package in.aicon.hisab;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Hisab's own keyboard. It appears inside Hisab only, instead of the phone keyboard: English, Malayalam (InScript layout) and
 * Manglish (type Roman letters, get Malayalam), with word suggestions from the user's own Hisab data. Nothing is sent or stored.
 */
final class HisabKeys {
 static final String[] MODE_NAME = {"English", "മലയാളം", "Manglish"}, MODE_CODE = {"EN", "മ", "Mg"};
 private final MainActivity a; private final Handler h = new Handler(Looper.getMainLooper());
 private LinearLayout panel, strip, keys; private final TextView[] sug = new TextView[3];
 EditText target; int mode; private boolean shift, caps, symbols, numeric, secret, shown;
 private int compStart = -1; private final StringBuilder buf = new StringBuilder(); private String composed = "";
 private List<KbWords.W> words = new ArrayList<>();

 HisabKeys(MainActivity a) { this.a = a; }

 static final String[][] ML_ROWS = {
  {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}, {"ൺ", "ൻ", "ർ", "ൽ", "ൾ", "ൃ", "ക്ഷ", "ജ്ഞ", "ശ്ര", "ഋ"},
  {"ൗ", "ൈ", "ാ", "ീ", "ൂ", "ബ", "ഹ", "ഗ", "ദ", "ജ", "ഡ"}, {"ഔ", "ഐ", "ആ", "ഈ", "ഊ", "ഭ", "ങ", "ഘ", "ധ", "ഝ", "ഢ"},
  {"ോ", "േ", "്", "ി", "ു", "പ", "ര", "ക", "ത", "ച", "ട"}, {"ഓ", "ഏ", "അ", "ഇ", "ഉ", "ഫ", "റ", "ഖ", "ഥ", "ഛ", "ഠ"},
  {"െ", "ം", "മ", "ന", "വ", "ല", "സ", "യ"}, {"ഞ", "ഃ", "ണ", "ഩ", "ഴ", "ള", "ശ", "ഷ"}};

 boolean enabled() { return a.db != null && a.db.flag("hisabKeys", true); }
 boolean isShown() { return shown; }
 View view() { return panel; }

 void install() {
  a.root.getViewTreeObserver().addOnGlobalFocusChangeListener((o, n) -> { if (n instanceof EditText && n.isShown() && enabled()) attach((EditText) n); else h.post(this::sync); });
  a.root.getViewTreeObserver().addOnGlobalLayoutListener(this::sync);
 }
 private void sync() {
  if (!shown) return;
  View f = a.root.findFocus();
  if (!enabled() || !(f instanceof EditText) || !f.isShown()) hide();
 }
 /** Called for every touch: tapping a field whose keyboard was dismissed brings it back. */
 void onTouch(MotionEvent ev) {
  if (ev.getAction() != MotionEvent.ACTION_UP || shown || !enabled()) return;
  View f = a.root.findFocus();
  if (f instanceof EditText && f.isShown()) { Rect r = new Rect(); f.getGlobalVisibleRect(r); if (r.contains((int) ev.getRawX(), (int) ev.getRawY())) f.post(() -> attach((EditText) f)); }
 }

 void attach(EditText et) {
  if (!enabled()) return;
  if (et != target) { commit(); }
  target = et; et.setShowSoftInputOnFocus(false);
  try { ((InputMethodManager) a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(et.getWindowToken(), 0); } catch (Exception ignored) { }
  int t = et.getInputType(), cls = t & InputType.TYPE_MASK_CLASS, var = t & InputType.TYPE_MASK_VARIATION;
  numeric = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME;
  secret = cls == InputType.TYPE_CLASS_TEXT && (var == InputType.TYPE_TEXT_VARIATION_PASSWORD || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) || cls == InputType.TYPE_CLASS_NUMBER && var == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
  mode = secret ? 0 : Math.max(0, Math.min(2, Integer.parseInt(a.db.pref("kbMode", "0"))));
  symbols = false; caps = false;
  shift = !secret && (t & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0 && et.getText().length() == 0 && mode != 1;
  if (!secret && !numeric) words = KbWords.load(a.db);
  build(); show();
 }

 private void show() {
  if (panel.getParent() != a.root) { if (panel.getParent() != null) ((android.view.ViewGroup) panel.getParent()).removeView(panel); a.root.addView(panel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)); }
  panel.setVisibility(View.VISIBLE); shown = true;
  if (a.dock != null) a.dock.setVisibility(View.GONE);
  panel.post(() -> { a.ownKb = panel.getHeight(); a.updateInsets(a.topInset, a.bottomInset, a.keyboardInset + a.bottomInset); reveal(); });
 }
 private void reveal() { EditText t = target; if (t != null) t.post(() -> t.requestRectangleOnScreen(new Rect(0, 0, t.getWidth(), t.getHeight()), false)); }

 void hide() {
  if (!shown) return; shown = false; commit();
  if (panel != null) { panel.setVisibility(View.GONE); if (panel.getParent() != null) ((android.view.ViewGroup) panel.getParent()).removeView(panel); }
  a.ownKb = 0; if (a.dock != null) a.dock.setVisibility(View.VISIBLE);
  a.updateInsets(a.topInset, a.bottomInset, a.keyboardInset + a.bottomInset);
 }

 // ---------------------------------------------------------------- layout
 private void build() {
  if (panel == null) {
   panel = Ui.col(a); strip = Ui.row(a); keys = Ui.col(a);
   panel.setBackground(Ui.shape(Ui.surface, 0)); panel.setElevation(Ui.dp(30)); panel.setClickable(true);
   View line = new View(a); line.setBackgroundColor(Ui.line); panel.addView(line, new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dp(0.6f))));
   for (int i = 0; i < 3; i++) {
    TextView s = Ui.text(a, "", 15, Ui.ink, true); s.setGravity(Gravity.CENTER); s.setSingleLine(true); s.setPadding(Ui.dp(6), 0, Ui.dp(6), 0);
    final int idx = i; s.setOnClickListener(v -> pickSuggestion(idx)); sug[i] = s; strip.addView(s, new LinearLayout.LayoutParams(0, Ui.dp(40), 1f));
   }
   panel.addView(strip); panel.addView(keys);
  }
  panel.setPadding(Ui.dp(3), 0, Ui.dp(3), a.bottomInset + Ui.dp(4));
  strip.setVisibility(secret || numeric ? View.GONE : View.VISIBLE);
  keys.removeAllViews();
  if (numeric) buildNumeric(); else if (symbols) buildSymbols(); else if (mode == 1) buildMl(); else buildQwerty();
  updateStrip();
 }
 private LinearLayout row() { LinearLayout r = new LinearLayout(a); r.setOrientation(LinearLayout.HORIZONTAL); keys.addView(r, new LinearLayout.LayoutParams(-1, Ui.dp(46))); return r; }
 private TextView key(LinearLayout r, String label, float weight, Runnable act, boolean repeat, boolean special) {
  TextView t = Ui.text(a, label, label.length() > 3 ? 14 : 18, Ui.ink, false); t.setGravity(Gravity.CENTER); t.setSingleLine(true);
  t.setBackground(Ui.shape(special ? Ui.surface2 : Ui.bg, 8)); t.setContentDescription(label);
  LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, weight); lp.setMargins(Ui.dp(2), Ui.dp(3), Ui.dp(2), Ui.dp(3)); r.addView(t, lp);
  Runnable rep = new Runnable() { @Override public void run() { act.run(); h.postDelayed(this, 55); } };
  t.setOnTouchListener((v, ev) -> {
   int ac = ev.getActionMasked();
   if (ac == MotionEvent.ACTION_DOWN) { v.setPressed(true); Ui.buzzTick(v); act.run(); if (repeat) h.postDelayed(rep, 380); return true; }
   if (ac == MotionEvent.ACTION_UP || ac == MotionEvent.ACTION_CANCEL) { v.setPressed(false); h.removeCallbacks(rep); return true; }
   return true;
  });
  return t;
 }
 private void chars(LinearLayout r, String[] cs, boolean upper) {
  for (String c : cs) { String out = upper ? c.toUpperCase() : c; String lab = mode == 1 && KbWords.hasMl(c) && isMark(c) ? "\u25CC" + c : out; key(r, lab, 1f, () -> typeKey(out), false, false); }
 }
 private static boolean isMark(String c) { if (c.length() != 1) return false; int ty = Character.getType(c.charAt(0)); return ty == Character.NON_SPACING_MARK || ty == Character.COMBINING_SPACING_MARK; }
 private void bottom(LinearLayout r) {
  key(r, symbols ? "ABC" : "?123", 1.4f, () -> { symbols = !symbols; build(); }, false, true);
  if (!secret) key(r, MODE_CODE[mode], 1.1f, this::nextMode, false, true);
  key(r, ",", 0.9f, () -> typeKey(","), false, false);
  key(r, secret ? "space" : MODE_NAME[mode], 4f, () -> typeKey(" "), false, false);
  key(r, ".", 0.9f, () -> typeKey("."), false, false);
  key(r, enterLabel(), 1.4f, this::enter, false, true);
 }
 private String enterLabel() { if (target == null) return "↵"; int ac = target.getImeOptions() & EditorInfo.IME_MASK_ACTION; boolean multi = (target.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0; return multi ? "↵" : ac == EditorInfo.IME_ACTION_SEARCH ? "🔍" : "✓"; }
 private void buildQwerty() {
  boolean up = shift || caps; String[] r1 = "q w e r t y u i o p".split(" "), r2 = "a s d f g h j k l".split(" "), r3 = "z x c v b n m".split(" ");
  chars(row(), r1, up);
  LinearLayout b = row(); b.setPadding(Ui.dp(14), 0, Ui.dp(14), 0); chars(b, r2, up);
  LinearLayout c = row();
  TextView sh = key(c, caps ? "⇪" : "⇧", 1.5f, () -> { if (shift && !caps) { caps = true; } else if (caps) { caps = false; shift = false; } else shift = true; build(); }, false, true);
  if (shift || caps) { sh.setBackground(Ui.shape(Ui.accent, 8)); sh.setTextColor(Ui.onAccent); }
  chars(c, r3, up); key(c, "⌫", 1.5f, this::back, true, true);
  bottom(row());
 }
 private void buildSymbols() {
  chars(row(), "1 2 3 4 5 6 7 8 9 0".split(" "), false);
  chars(row(), "@ # ₹ % & - + ( ) /".split(" "), false);
  LinearLayout c = row(); chars(c, "* \" ' : ; ! ? = _".split(" "), false); key(c, "⌫", 1.4f, this::back, true, true);
  bottom(row());
 }
 private void buildMl() {
  chars(row(), ML_ROWS[shift ? 1 : 0], false);
  chars(row(), ML_ROWS[shift ? 3 : 2], false);
  chars(row(), ML_ROWS[shift ? 5 : 4], false);
  LinearLayout c = row();
  TextView sh = key(c, "⇧", 1.4f, () -> { shift = !shift; build(); }, false, true);
  if (shift) { sh.setBackground(Ui.shape(Ui.accent, 8)); sh.setTextColor(Ui.onAccent); }
  chars(c, ML_ROWS[shift ? 7 : 6], false); key(c, "⌫", 1.4f, this::back, true, true);
  bottom(row());
 }
 private void buildNumeric() {
  int t = target == null ? 0 : target.getInputType(); boolean dec = (t & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0 || (t & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_NUMBER, signed = (t & InputType.TYPE_NUMBER_FLAG_SIGNED) != 0 || (t & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_NUMBER;
  String[][] g = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}};
  for (String[] rw : g) { LinearLayout r = row(); chars(r, rw, false); }
  LinearLayout r = row();
  key(r, dec ? "." : (signed ? "-" : ""), 1f, () -> { if (dec) typeKey("."); else if (signed) typeKey("-"); }, false, true);
  key(r, "0", 1f, () -> typeKey("0"), false, false);
  key(r, "⌫", 1f, this::back, true, true);
  LinearLayout d = row(); if ((t & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_NUMBER) { key(d, "-", 1f, () -> typeKey("-"), false, true); key(d, ":", 1f, () -> typeKey(":"), false, true); key(d, "/", 1f, () -> typeKey("/"), false, true); }
  key(d, enterLabel(), 2f, this::enter, false, true);
 }

 void nextMode() { if (secret || numeric) return; commit(); mode = (mode + 1) % 3; a.db.setPref("kbMode", "" + mode); shift = mode != 1 && target != null && (target.getInputType() & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0 && target.getText().length() == 0; caps = false; build(); }

 // ---------------------------------------------------------------- editing
 private boolean allowed(String s) {
  if (!numeric || target == null) return true;
  int t = target.getInputType(), cls = t & InputType.TYPE_MASK_CLASS;
  if (cls == InputType.TYPE_CLASS_NUMBER) { if (s.equals(".")) return (t & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0; if (s.equals("-")) return (t & InputType.TYPE_NUMBER_FLAG_SIGNED) != 0; return s.length() == 1 && Character.isDigit(s.charAt(0)); }
  return true;
 }
 private int selA() { return Math.max(0, Math.min(target.getSelectionStart(), target.getSelectionEnd())); }
 private int selB() { return Math.max(0, Math.max(target.getSelectionStart(), target.getSelectionEnd())); }
 private void insert(String s) { if (target == null) return; int st = selA(), en = selB(); Editable e = target.getText(); int before = e.length(); e.replace(st, en, s); int pos = Math.min(e.length(), st + s.length()); if (e.length() == before - (en - st)) pos = st; target.setSelection(pos); }
 private boolean composing() {
  if (compStart < 0 || target == null) return false;
  Editable e = target.getText(); int end = compStart + composed.length();
  return target.getSelectionStart() == end && target.getSelectionEnd() == end && end <= e.length() && e.subSequence(compStart, end).toString().equals(composed);
 }
 void commit() { compStart = -1; buf.setLength(0); composed = ""; }

 /** One character key (letters, digits, punctuation, space). */
 void typeKey(String s) {
  if (target == null || !allowed(s)) return;
  boolean letter = s.length() == 1 && ((s.charAt(0) >= 'a' && s.charAt(0) <= 'z') || (s.charAt(0) >= 'A' && s.charAt(0) <= 'Z'));
  if (mode == 2 && !secret && !numeric && !symbols && letter) {
   if (!composing()) { commit(); compStart = selA(); if (selB() != selA()) { target.getText().delete(selA(), selB()); } }
   buf.append(s); String m = MlRoman.translit(buf.toString());
   target.getText().replace(compStart, Math.min(target.getText().length(), compStart + composed.length()), m); composed = m;
   target.setSelection(Math.min(target.getText().length(), compStart + m.length()));
  } else { commit(); insert(s); }
  if (shift && !caps) { shift = false; build(); }
  else if (!numeric && !secret && (s.equals(" ") || s.equals(".")) && autoShift()) { shift = true; build(); }
  updateStrip();
 }
 private boolean autoShift() {
  if (target == null || mode == 1 || (target.getInputType() & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) == 0) return false;
  CharSequence t = target.getText(); int p = selA(); if (p < 2 || t.charAt(p - 1) != ' ') return false; char c = t.charAt(p - 2); return c == '.' || c == '!' || c == '?';
 }
 void back() {
  if (target == null) return;
  if (composing() && buf.length() > 0) {
   buf.setLength(buf.length() - 1); String m = MlRoman.translit(buf.toString());
   target.getText().replace(compStart, compStart + composed.length(), m); composed = m; target.setSelection(compStart + m.length());
   if (buf.length() == 0) commit(); updateStrip(); return;
  }
  commit(); int st = selA(), en = selB(); Editable e = target.getText();
  if (en > st) e.delete(st, en); else if (st > 0) { int n = Character.charCount(Character.codePointBefore(e, st)); e.delete(st - n, st); }
  updateStrip();
 }
 void enter() {
  if (target == null) return; commit();
  boolean multi = (target.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
  if (multi) { insert("\n"); return; }
  int ac = target.getImeOptions() & EditorInfo.IME_MASK_ACTION; if (ac == EditorInfo.IME_ACTION_UNSPECIFIED || ac == EditorInfo.IME_ACTION_NONE) ac = EditorInfo.IME_ACTION_DONE;
  EditText t = target; t.onEditorAction(ac); if (t == target && shown && ac != EditorInfo.IME_ACTION_NEXT) hide();
 }

 // ---------------------------------------------------------------- suggestions
 private String currentWord() {
  if (mode == 2 && composing() && buf.length() > 0) return buf.toString();
  if (target == null) return ""; CharSequence t = target.getText(); int p = selB(), s = p;
  while (s > 0 && p - s < 24 && (Character.isLetter(t.charAt(s - 1)) || Character.getType(t.charAt(s - 1)) == Character.NON_SPACING_MARK || Character.getType(t.charAt(s - 1)) == Character.COMBINING_SPACING_MARK)) s--;
  return t.subSequence(s, p).toString();
 }
 List<String> suggestions() {
  if (secret || numeric || target == null) return new ArrayList<>();
  return KbWords.suggest(words, currentWord(), mode, 3);
 }
 private void updateStrip() {
  if (strip == null) return; List<String> l = suggestions();
  for (int i = 0; i < 3; i++) { sug[i].setText(i < l.size() ? l.get(i) : ""); sug[i].setClickable(i < l.size()); }
 }
 private void pickSuggestion(int i) {
  List<String> l = suggestions(); if (i >= l.size() || target == null) return; pick(l.get(i));
 }
 void pick(String w) {
  Ui.buzzTick(panel);
  if (mode == 2 && composing()) { target.getText().replace(compStart, compStart + composed.length(), w + " "); target.setSelection(compStart + w.length() + 1); commit(); }
  else {
   String cur = currentWord(); int p = selB(); Editable e = target.getText(); int s = Math.max(0, p - cur.length());
   e.replace(s, p, w + " "); target.setSelection(Math.min(e.length(), s + w.length() + 1)); commit();
  }
  if (shift && !caps) { shift = false; build(); }
  updateStrip();
 }
}
