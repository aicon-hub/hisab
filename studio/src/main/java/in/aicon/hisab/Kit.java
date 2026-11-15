package in.aicon.hisab;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shared screen pieces. */
final class Kit {
 private Kit() {}
 static final int[] TONES = {0xFFA9B4FF, 0xFFFFA99B, 0xFF7FE6C3, 0xFFF3D98B, 0xFFC9B8FF, 0xFF9AD3FF, 0xFFFF9DBB, 0xFFD2F26B};
 static int tone(long id) { return TONES[(int) (Math.abs(id) % TONES.length)]; }

 /** Scrolling column with page padding. Bottom space clears the dock. */
 static LinearLayout column(MainActivity a, ScrollView[] out, boolean dockSpace) {
  ScrollView s = new ScrollView(a); s.setVerticalScrollBarEnabled(false); s.setOverScrollMode(View.OVER_SCROLL_NEVER); s.setFillViewport(false);
  LinearLayout c = Ui.col(a); c.setPadding(Ui.dp(20), a.topInset + Ui.dp(12), Ui.dp(20), a.bottomInset + Ui.dp(dockSpace ? 116 : 32)); s.addView(c, new FrameLayout.LayoutParams(-1, -2)); out[0] = s; return c;
 }

 static View title(MainActivity a, String sub, String title, boolean avatar) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(6), 0, Ui.dp(18));
  LinearLayout t = Ui.col(a); t.addView(Ui.text(a, sub, 13, Ui.muted, true)); TextView h = Ui.big(a, title, 32, Ui.ink); h.setPadding(0, Ui.dp(5), 0, 0); t.addView(h); r.addView(t, Ui.weight(1));
  if (avatar) {
   String nm = a.db.pref("name", "You"); FrameLayout av = new FrameLayout(a); av.setBackground(Ui.shape(Ui.accent, 22));
   TextView in = Ui.big(a, Ui.initial(nm), 18, Ui.onAccent); av.addView(in, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
   r.addView(av, Ui.lp(Ui.dp(44), Ui.dp(44))); av.setContentDescription("Settings"); Ui.tap(av, () -> Settings.open(a));
  }
  return r;
 }

 static View sectionHead(MainActivity a, String text, String action, Runnable onAction) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(26), 0, Ui.dp(12));
  r.addView(Ui.text(a, text, 17, Ui.ink, true), Ui.weight(1));
  if (action != null) { TextView x = Ui.text(a, action, 13, Ui.muted, true); x.setPadding(Ui.dp(8), Ui.dp(4), 0, Ui.dp(4)); r.addView(x); Ui.tap(x, onAction); }
  return r;
 }

 static String iconOf(Store.Tx t, Map<Long, Store.Cat> cats) {
  switch (t.kind) { case "transfer": return "swap"; case "lent": case "repay_out": return "lent"; case "borrowed": case "repay_in": return "borrowed"; default: Store.Cat c = cats.get(t.cat); return c == null ? (t.kind.equals("income") ? "coin" : "other") : c.icon; }
 }
 static String titleOf(Store.Tx t, Map<Long, Store.Cat> cats, Map<Long, Store.Person> people) {
  if (!t.note.isEmpty()) return t.note;
  Store.Person p = people.get(t.person); String who = p == null ? "" : p.name;
  switch (t.kind) { case "transfer": return "Transfer"; case "lent": return "Lent to " + who; case "borrowed": return "Borrowed from " + who; case "repay_in": return who + " paid back"; case "repay_out": return "Paid back " + who; default: Store.Cat c = cats.get(t.cat); return c == null ? Fmt.title(t.kind) : c.name; }
 }
 static String kindName(String k) { switch (k) { case "repay_in": return "Received back"; case "repay_out": return "Paid back"; default: return Fmt.title(k); } }

 static View txRow(MainActivity a, Store.Tx t, Map<Long, Store.Cat> cats, Map<Long, Store.Acc> accs, Map<Long, Store.Person> people, boolean showDate) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(9), 0, Ui.dp(9));
  Store.Cat c = cats.get(t.cat); int tone = t.kind.equals("expense") || t.kind.equals("income") ? tone(t.cat) : Ui.surface2;
  FrameLayout b = Ui.badge(a, iconOf(t, cats), 44, t.kind.equals("expense") || t.kind.equals("income") ? tone : Ui.surface2, t.kind.equals("expense") || t.kind.equals("income") ? 0xFF14151A : Ui.ink);
  r.addView(b, Ui.lpm(Ui.dp(44), Ui.dp(44), 0, 0, 14, 0));
  LinearLayout m = Ui.col(a); m.addView(Ui.text(a, titleOf(t, cats, people), 15, Ui.ink, true));
  Store.Acc ac = accs.get(t.account); String sub = (t.kind.equals("expense") || t.kind.equals("income") ? (c == null ? "" : c.name + " · ") : kindName(t.kind) + " · ") + (ac == null ? "" : ac.name) + (t.kind.equals("transfer") && accs.get(t.dest) != null ? " to " + accs.get(t.dest).name : "") + (showDate ? " · " + Fmt.shortDate(t.day) : "");
  TextView st = Ui.text(a, sub, 12.5f, Ui.muted, false); st.setPadding(0, Ui.dp(4), 0, 0); st.setSingleLine(true); st.setEllipsize(android.text.TextUtils.TruncateAt.END); m.addView(st);
  r.addView(m, Ui.weight(1));
  int sign = t.kind.equals("transfer") ? 0 : Store.sign(t.kind); String amt = (sign > 0 ? "+" : sign < 0 ? "-" : "") + Fmt.money(t.amount);
  TextView am = Ui.text(a, amt, 15, sign > 0 ? Ui.pos : Ui.ink, true); am.setPadding(Ui.dp(10), 0, 0, 0); r.addView(am);
  Ui.tap(r, () -> a.openTx(t)); return r;
 }

 static Map<Long, Store.Acc> accMap(Store db) { Map<Long, Store.Acc> m = new HashMap<>(); for (Store.Acc x : db.accounts()) m.put(x.id, x); return m; }
 static Map<Long, Store.Person> peopleMap(Store db) { Map<Long, Store.Person> m = new HashMap<>(); for (Store.Person x : db.people()) m.put(x.id, x); return m; }

 static View empty(MainActivity a, String icon, String head, String body) {
  LinearLayout c = Ui.col(a); c.setGravity(Gravity.CENTER_HORIZONTAL); c.setPadding(Ui.dp(24), Ui.dp(28), Ui.dp(24), Ui.dp(28));
  c.addView(Ui.badge(a, icon, 56, Ui.surface2, Ui.ink)); TextView h = Ui.text(a, head, 16, Ui.ink, true); h.setPadding(0, Ui.dp(14), 0, Ui.dp(4)); c.addView(h);
  TextView b = Ui.text(a, body, 13.5f, Ui.muted, false); b.setGravity(Gravity.CENTER); c.addView(b); return c;
 }

 /** Top bar for pushed pages. */
 static View bar(MainActivity a, String title, MainActivity.Layer[] ref, String actionIcon, Runnable action) {
  LinearLayout r = Ui.row(a); r.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(6));
  FrameLayout back = Ui.badge(a, "back", 42, Ui.surface, Ui.ink); r.addView(back, Ui.lp(Ui.dp(42), Ui.dp(42))); Ui.tap(back, () -> a.close(ref[0])); back.setContentDescription("Back");
  TextView t = Ui.big(a, title, 22, Ui.ink); t.setPadding(Ui.dp(14), 0, 0, 0); r.addView(t, Ui.weight(1));
  if (actionIcon != null) { FrameLayout f = Ui.badge(a, actionIcon, 42, Ui.accent, Ui.onAccent); r.addView(f, Ui.lp(Ui.dp(42), Ui.dp(42))); Ui.tap(f, action); f.setContentDescription("Add"); }
  return r;
 }

 /** Opens a full page with a back bar and scrolling body built by the filler. */
 static MainActivity.Layer page(MainActivity a, String title, String actionIcon, Runnable action, java.util.function.Consumer<LinearLayout> fill) {
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  ref[0] = a.open(() -> {
   LinearLayout outer = Ui.col(a); outer.addView(bar(a, title, ref, actionIcon, action));
   ScrollView s = new ScrollView(a); s.setVerticalScrollBarEnabled(false); LinearLayout c = Ui.col(a); c.setPadding(Ui.dp(20), Ui.dp(8), Ui.dp(20), Ui.dp(40)); s.addView(c); fill.accept(c); outer.addView(s, Ui.lp(-1, 0)); ((LinearLayout.LayoutParams) s.getLayoutParams()).weight = 1; return outer;
  }, false);
  return ref[0];
 }
}
