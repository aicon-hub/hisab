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
 int tab = 0, topInset = 0, bottomInset = 0;
 YearMonth ledgerMonth = YearMonth.now(), insightMonth = YearMonth.now();
 String ledgerFilter = "all", ledgerQuery = ""; int ledgerDay = 0; boolean ledgerCalendar = false;
 final List<Layer> layers = new ArrayList<>();
 final Map<Integer, Consumer<Intent>> results = new HashMap<>();
 View toastView; boolean launching;

 static final class Layer { FrameLayout holder; FrameLayout panel; Supplier<View> build; boolean sheet; Runnable onClose; View content; }

 @Override protected void onCreate(Bundle b) {
  super.onCreate(b);
  db = new Store(this); db.seedBasics();
  if (!db.hasData() && db.pref("sample", "").isEmpty()) { Sample.seed(db); }
  if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
  else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  applyTheme();
  root = new FrameLayout(this);
  setContentView(root);
  root.setOnApplyWindowInsetsListener((v, ins) -> { topInset = ins.getSystemWindowInsetTop(); bottomInset = ins.getSystemWindowInsetBottom(); build(); return ins; });
  launching = b == null && Ui.motion(root) && !getIntent().getBooleanExtra("noLaunch", false);
  build();
  if (launching) showLaunch();
  Reminders.schedule(this, db);
 }

 void applyTheme() {
  Fmt.currency(db.pref("currency", "INR")); Fmt.grouping = db.pref("grouping", "Indian");
  String mode = db.pref("appearance", "System");
  boolean sysDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  boolean dark = mode.equals("Dark") || (mode.equals("System") && sysDark);
  Ui.theme(this, dark, db.flag("pureBlack", false), db.pref("accent", "Lime"), db.pref("font", "Inter"));
  Window w = getWindow(); w.getDecorView().setBackgroundColor(Ui.bg);
  int flags = w.getDecorView().getSystemUiVisibility();
  w.getDecorView().setSystemUiVisibility(dark ? flags & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 16) : flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 16);
 }

 /** Full rebuild of the shell, used for theme and inset changes. */
 void build() {
  List<Layer> keep = new ArrayList<>(layers);
  root.removeAllViews(); root.setBackgroundColor(Ui.bg);
  tabHost = new FrameLayout(this); root.addView(tabHost, new FrameLayout.LayoutParams(-1, -1));
  buildDock();
  layerHost = new FrameLayout(this); layerHost.setElevation(Ui.dp(24)); root.addView(layerHost, new FrameLayout.LayoutParams(-1, -1));
  for (Layer l : keep) layerHost.addView(l.holder);
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
    FrameLayout plus = Ui.badge(this, "plus", 52, Ui.accent, Ui.onAccent); LinearLayout.LayoutParams p = Ui.lpm(Ui.dp(52), Ui.dp(52), 6, 0, 6, 0); dock.addView(plus, p);
    Ui.tap(plus, () -> openEntry(Store.KINDS[0], null)); plus.setContentDescription("Add entry"); continue;
   }
   LinearLayout item = Ui.row(this); item.setGravity(Gravity.CENTER); boolean on = tab == i;
   item.setPadding(Ui.dp(on ? 14 : 12), Ui.dp(12), Ui.dp(on ? 16 : 12), Ui.dp(12));
   if (on) item.setBackground(Ui.shape(Ui.dark ? 0x33FFFFFF & 0x00FFFFFF | Ui.accent & 0xFF000000 : 0x24FFFFFF, 24));
   if (on) item.setBackground(Ui.shape(Ui.accent, 24));
   item.addView(Ui.icon(this, icons[i], 22, on ? Ui.onAccent : idle));
   if (on) { TextView t = Ui.text(this, names[i], 13, Ui.onAccent, true); t.setPadding(Ui.dp(7), 0, 0, 0); item.addView(t); }
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
  if (animate && change) { v.setAlpha(0); v.animate().alpha(1).setDuration(180).start(); }
  // dock refresh
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

 // ---- layers
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
  View.OnClickListener none = null;
  if (Ui.motion(root)) { l.holder.setAlpha(1); l.panel.animate().translationY(l.sheet ? Ui.dp(500) : 0).translationX(l.sheet ? 0 : Ui.dp(120)).alpha(l.sheet ? 1 : 0).setDuration(200).withEndAction(end).start(); l.holder.animate().alpha(0).setDuration(200).start(); }
  else end.run();
  View f = getCurrentFocus(); if (f != null) ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(), 0);
 }
 void closeTop() { if (!layers.isEmpty()) close(layers.get(layers.size() - 1)); }
 void closeAll() { for (Layer l : new ArrayList<>(layers)) close(l); }

 @Override public void onBackPressed() {
  if (!layers.isEmpty()) { closeTop(); return; }
  if (tab != 0) { showTab(0, true); return; }
  super.onBackPressed();
 }

 // ---- shared actions
 void openEntry(String kind, Store.Tx edit) { EntrySheet.open(this, kind, edit); }
 void openEntryPrefilled(Store.Tx t) { EntrySheet.openPrefilled(this, t); }
 void openTx(Store.Tx t) { Sheets.txDetail(this, t); }

 void toast(String msg, Runnable undo) {
  if (toastView != null) layerHostRoot().removeView(toastView);
  LinearLayout t = Ui.row(this); t.setBackground(Ui.shape(Ui.dark ? Ui.surface2 : Ui.ink, 20)); t.setPadding(Ui.dp(18), Ui.dp(12), Ui.dp(10), Ui.dp(12)); t.setElevation(Ui.dp(32));
  int fg = Ui.dark ? Ui.ink : 0xFFFFFFFF; t.addView(Ui.text(this, msg, 14, fg, false), Ui.weight(1));
  if (undo != null) { TextView u = Ui.text(this, "Undo", 14, Ui.dark ? Ui.accent : 0xFFC7F23A, true); u.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(10), Ui.dp(6)); t.addView(u); Ui.tap(u, () -> { undo.run(); dismissToast(); }); }
  FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); lp.setMargins(Ui.dp(20), 0, Ui.dp(20), Ui.dp(94) + bottomInset);
  toastView = t; layerHostRoot().addView(t, lp);
  if (Ui.motion(root)) { t.setTranslationY(Ui.dp(30)); t.setAlpha(0); t.animate().translationY(0).alpha(1).setDuration(220).start(); }
  final View mine = t; t.postDelayed(() -> { if (toastView == mine) dismissToast(); }, 4200);
 }
 FrameLayout layerHostRoot() { return root; }
 void dismissToast() { if (toastView != null) { View v = toastView; toastView = null; v.animate().alpha(0).setDuration(160).withEndAction(() -> root.removeView(v)).start(); } }

 // ---- launch reveal (cold start only, about 1.6 seconds)
 void showLaunch() {
  FrameLayout f = new FrameLayout(this); f.setBackgroundColor(Ui.dark ? 0xFF0D0E11 : 0xFFF4F0E8); f.setClickable(true);
  LinearLayout m = Ui.col(this); m.setGravity(Gravity.CENTER_HORIZONTAL);
  Charts.Logo logo = new Charts.Logo(this); m.addView(logo, Ui.lp(Ui.dp(116), Ui.dp(116)));
  Wordmark w = new Wordmark(this, Ui.ink); w.setAlpha(0); m.addView(w, Ui.lpm(Ui.dp(112), Ui.dp(44), 0, 18, 0, 0));
  f.addView(m, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)); root.addView(f, new FrameLayout.LayoutParams(-1, -1));
  logo.play(); w.animate().alpha(1).setStartDelay(650).setDuration(350).start();
  f.postDelayed(() -> f.animate().alpha(0).setDuration(280).withEndAction(() -> root.removeView(f)).start(), 1450);
 }

 // ---- activity results
 void request(Intent i, Consumer<Intent> cb) { int code = 100 + results.size() % 50; results.put(code, cb); try { startActivityForResult(i, code); } catch (Exception e) { toast("Not available on this phone", null); } }
 @Override protected void onActivityResult(int code, int res, Intent data) { Consumer<Intent> cb = results.remove(code); if (cb != null && res == RESULT_OK && data != null) cb.accept(data); }
 /** Contact picker. No contacts permission is needed or requested. */
 void pickContact(BiConsumer<String, String> done) {
  request(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), d -> {
   try (Cursor c = getContentResolver().query(d.getData(), new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
    if (c != null && c.moveToFirst()) done.accept(c.getString(0), c.getString(1).replaceAll("[^+0-9]", ""));
   } catch (Exception e) { toast("Could not read that contact", null); }
  });
 }
 @Override protected void onResume() { super.onResume(); if (db != null && root != null && !layers.isEmpty() == false) { /* keep state */ } }
}
