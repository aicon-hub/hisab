package in.aicon.hisab;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.*;
import android.os.*;
import android.provider.ContactsContract;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

public class MainActivity extends Activity {
 Store db;
 FrameLayout root, tabHost, layerHost;
 LinearLayout dock;
 int tab = 0, topInset = 0, bottomInset = 0, keyboardInset = 0, ownKb = 0;
 int nextRequestCode = 100;
 YearMonth ledgerMonth = YearMonth.now(), insightMonth = YearMonth.now();
 String ledgerFilter = "all", ledgerQuery = "", ledgerFrom = "", ledgerTo = ""; int ledgerDay = 0; long ledgerCat = 0, ledgerMin = 0, ledgerMax = 0; boolean ledgerCalendar = false; String settingsSection = ""; int reportYear = java.time.Year.now().getValue();
 final List<Layer> layers = new ArrayList<>();
 final Map<Integer, Consumer<Intent>> results = new HashMap<>();
 final Map<Integer, Runnable> cancels = new HashMap<>();
 View toastView; boolean launching;

 static final class Layer { FrameLayout holder; FrameLayout panel; Supplier<View> build; boolean sheet; Runnable onClose; View content; }

 @Override protected void onCreate(Bundle b) {
  super.onCreate(b); CrashLog.install(this);
  db = new Store(this); db.seedBasics(); Ui.haptics = db.flag("haptics", true); Ui.initVib(this);
  if (!db.hasData() && db.pref("sample", "").isEmpty()) { Sample.seed(db); }
  if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
  else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  applyTheme();
  root = new FrameLayout(this);
  setContentView(root);
  root.setOnApplyWindowInsetsListener((v, ins) -> {
   if (Build.VERSION.SDK_INT >= 30) {
    android.graphics.Insets bars = ins.getInsets(WindowInsets.Type.systemBars());
    updateInsets(bars.top, bars.bottom, ins.getInsets(WindowInsets.Type.ime()).bottom);
   } else {
    // On older Android adjustResize shrinks the window above the keyboard.
    updateInsets(ins.getStableInsetTop(), ins.getStableInsetBottom(), 0);
   }
   return ins;
  });
  if (getIntent().getBooleanExtra("profileFade", false)) {
   getIntent().removeExtra("profileFade");
   if (Ui.motion(root)) { root.setAlpha(0f); root.setScaleX(1.04f); root.setScaleY(1.04f); root.post(() -> root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(360).setInterpolator(new DecelerateInterpolator(2f)).start()); }
  }
  launching = b == null && Ui.motion(root) && !getIntent().getBooleanExtra("noLaunch", false);
  build();
  if (launching) showLaunch();
  LauncherIcons.apply(this);
  Reminders.schedule(this, db);
  AutoRemind.schedule(this, db);
  Diary.monthlyCheck(this, db, java.time.LocalDate.now());
  QuickAddNotif.apply(this, db);
  db.normalisePhones();
  BankSms.autoCatchUp(this);
  Trash.purge(this); if (Lock.enabled(db) && !Lock.unlocked) showLock();
  Onboard.maybeShow(this);
  handleQuick(getIntent());
 }

 /** Launcher shortcuts and the quick-settings tile land here. */
 void handleQuick(Intent i) {
  if (i == null) return;
  final String quick = i.getStringExtra("quick");
  if (quick == null) return;
  root.post(() -> { if (lockLayer == null && !launching) openEntry(quick, null); else root.postDelayed(() -> { if (lockLayer == null) openEntry(quick, null); }, 1000); });
 }

 @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleQuick(i); }

 FrameLayout lockLayer;

 /** Full-screen PIN gate. Nothing behind it can be reached until unlocked. */
 void showLock() {
  if (lockLayer != null) return;
  lockLayer = new FrameLayout(this); lockLayer.setBackgroundColor(Ui.bg); lockLayer.setElevation(Ui.dp(96));
  LinearLayout col = Ui.col(this); col.setGravity(Gravity.CENTER_HORIZONTAL); col.setPadding(Ui.dp(32), 0, Ui.dp(32), 0);
  col.addView(Ui.badge(this, "lock", 64, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(64), Ui.dp(64), 0, 0, 0, 0));
  TextView h = Ui.big(this, "Hisab is locked", 24, Ui.ink); h.setPadding(0, Ui.dp(20), 0, Ui.dp(6)); h.setGravity(Gravity.CENTER); col.addView(h, new LinearLayout.LayoutParams(-1, -2));
  TextView sub = Ui.text(this, "Enter your PIN to open your ledger", 13.5f, Ui.muted, false); sub.setGravity(Gravity.CENTER); col.addView(sub, new LinearLayout.LayoutParams(-1, -2));
  PinPad pad = new PinPad(this, pin -> {
   if (!Lock.verify(db, pin)) return false;
   Lock.unlocked = true; Ui.buzzOk(root); root.removeView(lockLayer); lockLayer = null; refresh(); return true;
  }, true);
  col.addView(pad.view, Ui.lpm(PinPad.width(this), -2, 0, 18, 0, 0));
  lockLayer.addView(col, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
  root.addView(lockLayer, new FrameLayout.LayoutParams(-1, -1));
  if (Lock.bio(db) && Bio.available(this)) root.postDelayed(() -> Bio.unlock(this, () -> { if (lockLayer != null) { Lock.unlocked = true; root.removeView(lockLayer); lockLayer = null; refresh(); } }), 300);
 }

 float textScale() { String s = db.pref("textScale", "Standard"); return s.equals("Compact") ? 0.9f : s.equals("Large") ? 1.14f : 1f; }
 static int parseHex(String s) { try { return (int) (Long.parseLong(s.replace("#", ""), 16) & 0xFFFFFF) | 0xFF000000; } catch (Exception e) { return 0xFFC7F23A; } }
 static String hexOf(int color) { return String.format(java.util.Locale.US, "#%06X", color & 0xFFFFFF); }

 void applyTheme() {
  L.language = db.pref("language", "English");
  Privacy.apply(this);
  Fmt.currency(db.pref("currency", "INR")); Fmt.grouping = db.pref("grouping", "Indian"); Fmt.dateStyle = db.pref("dateStyle", "DMY");
  String mode = db.pref("appearance", "System");
  boolean sysDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  boolean dark = mode.equals("Dark") || (mode.equals("System") && sysDark);
  Ui.theme(this, dark, db.flag("pureBlack", false), db.pref("accent", "Lime"), db.pref("font", "Inter"), textScale(), parseHex(db.pref("accentHex", "")));
  if(root!=null)LauncherIcons.apply(this);
  Window w = getWindow(); w.getDecorView().setBackgroundColor(Ui.bg);
  int flags = w.getDecorView().getSystemUiVisibility();
  w.getDecorView().setSystemUiVisibility(dark ? flags & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 16) : flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 16);
 }

 /** Update geometry in place. Never replace a focused field when the IME opens. */
 void updateInsets(int top, int bottom, int ime) {
  int dt = top - topInset, dbottom = bottom - bottomInset;
  topInset = top; bottomInset = bottom; keyboardInset = Math.max(0, ime - bottom);
  if (tabHost == null || layerHost == null) return;
  if (tabHost.getChildCount() > 0) {
   View page = tabHost.getChildAt(0);
   if (page instanceof ScrollView && ((ScrollView) page).getChildCount() > 0) {
    View col = ((ScrollView) page).getChildAt(0);
    col.setPadding(col.getPaddingLeft(), col.getPaddingTop() + dt, col.getPaddingRight(), col.getPaddingBottom() + dbottom);
   }
  }
  if (dock != null) {
   FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) dock.getLayoutParams();
   lp.bottomMargin = Ui.dp(14) + bottomInset; dock.setLayoutParams(lp);
  }
  FrameLayout.LayoutParams hp = (FrameLayout.LayoutParams) layerHost.getLayoutParams();
  int kbm = Math.max(keyboardInset, Math.max(0, ownKb - bottomInset));
  hp.bottomMargin = kbm; layerHost.setLayoutParams(hp);
  FrameLayout.LayoutParams tp = (FrameLayout.LayoutParams) tabHost.getLayoutParams(); tp.bottomMargin = Math.max(0, ownKb); tabHost.setLayoutParams(tp);
  for (Layer l : layers) l.panel.setPadding(0, l.sheet ? 0 : topInset, 0, bottomInset);
 }

 /** Full rebuild only for an explicit theme change. */
 void build() {
  List<Layer> keep = new ArrayList<>(layers);
  root.removeAllViews(); root.setBackgroundColor(Ui.bg);
  tabHost = new FrameLayout(this); root.addView(tabHost, new FrameLayout.LayoutParams(-1, -1));
  buildDock();
  layerHost = new FrameLayout(this); layerHost.setElevation(Ui.dp(24)); root.addView(layerHost, new FrameLayout.LayoutParams(-1, -1));
  for (Layer l : keep) {
   ViewParent parent = l.holder.getParent();
   if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(l.holder);
   layerHost.addView(l.holder);
  }
  updateInsets(topInset, bottomInset, keyboardInset + bottomInset);
  showTab(tab, false);
 }

 void buildDock() {
  dock = Ui.row(this); dock.setGravity(Gravity.CENTER);
  dock.setBackground(Ui.outline(Ui.dark ? Ui.surface2 : Ui.ink, Ui.dark ? Ui.line : Ui.ink, 34, 1));
  dock.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8)); dock.setElevation(Ui.dp(10));
  String[] icons = {"home", "ledger", "plus", "chart", "people"}, names = {"Home", "Ledger", "", "Insights", "People"};
  int idle = Ui.dark ? Ui.muted : 0xB3FFFFFF;
  for (int i = 0; i < 5; i++) {
   final int idx = i;
   if (i == 2) {
    FrameLayout plus = Ui.badge(this, "plus", 52, Ui.accentOnDark, Ui.onAccentOnDark); LinearLayout.LayoutParams p = Ui.lpm(Ui.dp(52), Ui.dp(52), 6, 0, 6, 0); dock.addView(plus, p);
    Ui.tap(plus, () -> openEntry(Store.KINDS[0], null)); plus.setContentDescription("Add entry"); continue;
   }
   LinearLayout item = Ui.row(this); item.setGravity(Gravity.CENTER); boolean on = tab == i;
   item.setPadding(Ui.dp(on ? 14 : 12), Ui.dp(12), Ui.dp(on ? 16 : 12), Ui.dp(12));
   if (on) item.setBackground(Ui.shape(Ui.accentOnDark, 24));
   item.addView(Ui.icon(this, icons[i], 22, on ? Ui.onAccentOnDark : idle));
   if (on) { TextView t = Ui.text(this, names[i], 13, Ui.onAccentOnDark, true); t.setPadding(Ui.dp(7), 0, 0, 0); item.addView(t); }
   item.setContentDescription(names[i]);
   Ui.tap(item, () -> { if (tab != idx) showTab(idx, true); });
   dock.addView(item, Ui.lp(-2, -2));
  }
  FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL); lp.bottomMargin = Ui.dp(14) + bottomInset;
  root.addView(dock, lp);
  if (layerHost != null) layerHost.bringToFront();
 }

 void showTab(int i, boolean animate) {
  boolean change = tab != i; tab = i;
  tabHost.removeAllViews();
  View v;
  switch (i) { case 1: v = LedgerPage.build(this); break; case 3: v = InsightsPage.build(this); break; case 4: v = PeoplePage.build(this); break; default: v = HomePage.build(this); }
  tabHost.addView(v, new FrameLayout.LayoutParams(-1, -1));
  if (animate && change && Ui.motion(root)) { v.setAlpha(0); v.animate().alpha(1).setDuration(180).start(); }

  root.removeView(dock); buildDock();
 }

 /** Rebuild the visible tab and any open layers after data changes. */
 void refresh() {
  int sy = 0; View cur = tabHost.getChildCount() > 0 ? tabHost.getChildAt(0) : null; if (cur instanceof ScrollView) sy = cur.getScrollY();
  tabHost.removeAllViews(); View v;
  switch (tab) { case 1: v = LedgerPage.build(this); break; case 3: v = InsightsPage.build(this); break; case 4: v = PeoplePage.build(this); break; default: v = HomePage.build(this); }
  tabHost.addView(v, new FrameLayout.LayoutParams(-1, -1)); final int fy = sy; if (v instanceof ScrollView) v.post(() -> v.scrollTo(0, fy));
  for (Layer l : new ArrayList<>(layers)) if (l.build != null && l.content != null) {
   int ly = l.content instanceof ScrollView ? l.content.getScrollY() : 0; View n = l.build.get(); int idx = l.panel.indexOfChild(l.content); l.panel.removeView(l.content); l.panel.addView(n, idx, l.sheet ? new FrameLayout.LayoutParams(-1, -2) : new FrameLayout.LayoutParams(-1, -1)); l.content = n; final int fly = ly; if (n instanceof ScrollView) n.post(() -> n.scrollTo(0, fly));
  }
 }

 Layer open(Supplier<View> build, boolean sheet) {
  Layer l = new Layer(); l.build = build; l.sheet = sheet;
  l.holder = new FrameLayout(this); l.holder.setClickable(true);
  View scrim = new View(this); scrim.setBackgroundColor(Ui.scrim); l.holder.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
  l.panel = new FrameLayout(this);
  FrameLayout.LayoutParams pp;
  if (sheet) { pp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); l.panel.setBackground(topRounded(Ui.surface, 30)); scrim.setOnClickListener(x -> close(l)); l.panel.setClickable(true); l.panel.setPadding(0, 0, 0, bottomInset); }
  else { pp = new FrameLayout.LayoutParams(-1, -1); l.panel.setBackgroundColor(Ui.bg); l.panel.setPadding(0, topInset, 0, bottomInset); scrim.setVisibility(View.GONE); }
  l.holder.addView(l.panel, pp);
  l.content = build.get(); l.panel.addView(l.content, sheet ? new FrameLayout.LayoutParams(-1, -2) : new FrameLayout.LayoutParams(-1, -1));
  layerHost.addView(l.holder); layers.add(l);
  if (Ui.motion(root)) {
   if (sheet) { scrim.setAlpha(0); scrim.animate().alpha(1).setDuration(200).start(); l.panel.setTranslationY(Ui.dp(500)); l.panel.animate().translationY(0).setDuration(320).setInterpolator(new DecelerateInterpolator(2f)).start(); }
   else { l.panel.setTranslationX(Ui.dp(120)); l.panel.setAlpha(0); l.panel.animate().translationX(0).alpha(1).setDuration(260).setInterpolator(new DecelerateInterpolator(2f)).start(); }
  }
  return l;
 }
 static GradientDrawable_ topRounded(int color, float r) { GradientDrawable_ g = new GradientDrawable_(); g.setColor(color); float x = Ui.dp(r); g.setCornerRadii(new float[]{x, x, x, x, 0, 0, 0, 0}); return g; }
 static final class GradientDrawable_ extends android.graphics.drawable.GradientDrawable {}

 void close(Layer l) {
  if (!layers.remove(l)) return;
  Runnable end = () -> { layerHost.removeView(l.holder); if (l.onClose != null) l.onClose.run(); };
  if (Ui.motion(root)) { l.holder.setAlpha(1); l.panel.animate().translationY(l.sheet ? Ui.dp(500) : 0).translationX(l.sheet ? 0 : Ui.dp(120)).alpha(l.sheet ? 1 : 0).setDuration(200).withEndAction(end).start(); l.holder.animate().alpha(0).setDuration(200).start(); }
  else end.run();
  View f = getCurrentFocus(); if (f != null) ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(), 0);
 }
 void closeTop() { if (!layers.isEmpty()) close(layers.get(layers.size() - 1)); }
 /** Switches to the other profile with a fade out and a fade in, so there is no flash or restart feel. */
 void switchProfile(int idx) {
  if (idx == Profiles.active(this) || (idx == 1 && !Profiles.hasSecond(this))) return;
  Ui.buzzOk(root);
  Runnable go = () -> { Profiles.setActive(this, idx); getIntent().putExtra("profileFade", true); closeAll(); recreate(); };
  if (Ui.motion(root)) root.animate().alpha(0f).scaleX(.96f).scaleY(.96f).setDuration(190).setInterpolator(new AccelerateInterpolator(1.4f)).withEndAction(go).start(); else go.run();
 }

 void closeAll() { for (Layer l : new ArrayList<>(layers)) close(l); }

 @Override public void onBackPressed() {

  if (lockLayer != null) { moveTaskToBack(true); return; }
  if (!layers.isEmpty()) { closeTop(); return; }
  if (tab != 0) { showTab(0, true); return; }
  super.onBackPressed();
 }

 void launchExternal(Intent intent) {
  try { startActivity(intent); }
  catch (android.content.ActivityNotFoundException | SecurityException e) { toast("No app available to open this", null); }
 }

 void openEntry(String kind, Store.Tx edit) { EntrySheet.open(this, kind, edit); }
 void openEntryPrefilled(Store.Tx t) { EntrySheet.openPrefilled(this, t); }
 void openTx(Store.Tx t) { Sheets.txDetail(this, t); }

 void toast(String msg, Runnable undo) {
  Ui.buzzOk(root);
  if (toastView != null) layerHostRoot().removeView(toastView);
  LinearLayout t = Ui.row(this); t.setBackground(Ui.shape(Ui.dark ? Ui.surface2 : Ui.ink, 20)); t.setPadding(Ui.dp(18), Ui.dp(12), Ui.dp(10), Ui.dp(12)); t.setElevation(Ui.dp(32));
  int fg = Ui.dark ? Ui.ink : 0xFFFFFFFF; t.addView(Ui.text(this, msg, 14, fg, false), Ui.weight(1));
  if (undo != null) { TextView u = Ui.text(this, "Undo", 14, Ui.accentOnDark, true); u.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(10), Ui.dp(6)); t.addView(u); Ui.tap(u, () -> { undo.run(); dismissToast(); }); }
  FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); lp.setMargins(Ui.dp(20), 0, Ui.dp(20), Ui.dp(94) + bottomInset);
  toastView = t; layerHostRoot().addView(t, lp);
  if (Ui.motion(root)) { t.setTranslationY(Ui.dp(30)); t.setAlpha(0); t.animate().translationY(0).alpha(1).setDuration(220).start(); }
  final View mine = t; t.postDelayed(() -> { if (toastView == mine) dismissToast(); }, 4200);
 }
 FrameLayout layerHostRoot() { return root; }
 void dismissToast() { if (toastView != null) { View v = toastView; toastView = null; v.animate().alpha(0).setDuration(160).withEndAction(() -> root.removeView(v)).start(); } }

 FrameLayout launchView() {
  FrameLayout f = new FrameLayout(this); f.setTag("launchReveal"); f.setElevation(Ui.dp(64)); f.setBackgroundColor(Ui.bg);
  LinearLayout m = Ui.col(this); m.setGravity(Gravity.CENTER_HORIZONTAL);
  Charts.Logo logo = new Charts.Logo(this); logo.setTag("launchLogo"); m.addView(logo, Ui.lp(Ui.dp(112), Ui.dp(112)));
  Wordmark word = new Wordmark(this, Ui.ink); m.addView(word, Ui.lpm(Ui.dp(112), Ui.dp(44), 0, 20, 0, 0));
  TextView colour = Ui.text(this, "● " + L.t(db.pref("accent", "Lime")), 13, Ui.ink, true);
  colour.setBackground(Ui.shape(Ui.accent, 20)); colour.setPadding(Ui.dp(16),Ui.dp(9),Ui.dp(16),Ui.dp(9)); colour.setTextColor(Ui.onAccent);
  m.addView(colour, Ui.lpm(-2,-2,0,16,0,0));
  f.addView(m, new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER)); return f;
 }
 void showLaunch() {
  FrameLayout f=launchView(); root.addView(f,new FrameLayout.LayoutParams(-1,-1));
  View content=f.getChildAt(0); content.setAlpha(0); content.setScaleX(.96f);content.setScaleY(.96f);
  content.animate().alpha(1).scaleX(1).scaleY(1).setDuration(420).setInterpolator(new DecelerateInterpolator(1.6f)).start();
  ((Charts.Logo)f.findViewWithTag("launchLogo")).play();
  f.setClickable(false); f.postDelayed(()->{
   tabHost.setAlpha(0);tabHost.setTranslationY(Ui.dp(8));
   tabHost.animate().alpha(1).translationY(0).setDuration(300).start();
   f.animate().alpha(0).setDuration(300).withEndAction(()->{root.removeView(f);launching=false;}).start();
  },850);
 }

 @Override public void onConfigurationChanged(Configuration c) {
  super.onConfigurationChanged(c); applyTheme(); build();
 }

 void request(Intent i, Consumer<Intent> cb) { request(i, cb, null); }
 /** onCancel runs when the activity returns without an OK result, so callers can tell the user instead of staying silent. */
 void request(Intent i, Consumer<Intent> cb, Runnable onCancel) {
  while (results.containsKey(nextRequestCode)) nextRequestCode = nextRequestCode >= 65000 ? 100 : nextRequestCode + 1;
  int code = nextRequestCode++; results.put(code, cb); cancels.put(code, onCancel);
  try { startActivityForResult(i, code); }
  catch (android.content.ActivityNotFoundException | SecurityException e) { results.remove(code); cancels.remove(code); toast("Not available on this phone", null); }
 }
 @Override protected void onStop() { super.onStop(); try { DriveBackup.auto(this); } catch (Exception ignored) {} }
 @Override protected void onActivityResult(int code, int res, Intent data) { super.onActivityResult(code, res, data); Consumer<Intent> cb = results.remove(code); Runnable onCancel = cancels.remove(code); if (res == RESULT_OK) { if (cb != null) cb.accept(data); } else if (onCancel != null) onCancel.run(); }
 /** Contact picker. No contacts permission is needed or requested. */
 void pickContact(BiConsumer<String, String> done) {
  request(new Intent(Intent.ACTION_PICK).setDataAndType(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE), d -> {
   try (Cursor c = getContentResolver().query(d.getData(), new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
    if (c != null && c.moveToFirst()) {
     String name = c.getString(0), phone = c.getString(1);
     phone = phone == null ? "" : phone.replaceAll("[^+0-9]", "");
     done.accept(name == null || name.trim().isEmpty() ? (phone.isEmpty() ? "Contact" : phone) : name.trim(), phone);
    }
   } catch (Exception e) { toast("Could not read that contact", null); }
  });
 }
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);boolean yes=grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;if(code==71){db.setPref("smsEnabled",yes?"1":"0");closeAll();if(yes)BankSms.finishSetup(this);else BankSms.open(this);toast(yes?"Bank SMS enabled. Link an account":"SMS permission not granted",null);}else if(code==72){if(yes)BankSms.scan(this);else toast("Inbox access not granted",null);}else if(code==75){boolean sms=false;for(int i=0;i<permissions.length;i++)if(permissions[i].equals(android.Manifest.permission.RECEIVE_SMS)&&grants[i]==android.content.pm.PackageManager.PERMISSION_GRANTED)sms=true;if(sms){db.setPref("smsEnabled","1");toast("Live bank SMS is on",null);}else{db.setPref("smsLive","0");db.setPref("smsAutoLink","0");db.setPref("smsNotify","0");toast("SMS permission not granted. Live mode stays off",null);}closeAll();BankSms.open(this);}else if(code==73){toast(yes?"SMS reminders will be sent by your phone":"SMS permission not granted. Reminders arrive as notifications instead",null);}else if(code==6){toast(yes?"Mic is on. Tap the mic to speak":"Mic permission not granted",null);}}
 @Override protected void onResume() { super.onResume(); if(db!=null && root!=null){Privacy.apply(this); if(tabHost!=null && !launching){refresh(); root.postDelayed(() -> { if (!launching) NotifPrompt.maybeShow(this); }, 900);}} }
}
