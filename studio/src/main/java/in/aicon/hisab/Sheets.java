package in.aicon.hisab;

import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.*;

/** Small reusable sheets: pick one, fill a form, confirm, entry detail. */
final class Sheets {
 private Sheets() {}

 static LinearLayout sheetBody(MainActivity a, String title) {
  LinearLayout c = Ui.col(a); c.setPadding(Ui.dp(22), Ui.dp(10), Ui.dp(22), Ui.dp(22));
  View grab = new View(a); grab.setBackground(Ui.shape(Ui.line, 3)); LinearLayout.LayoutParams gp = Ui.lp(Ui.dp(40), Ui.dp(5)); gp.gravity = Gravity.CENTER_HORIZONTAL; c.addView(grab, gp);
  if (title != null) { TextView t = Ui.big(a, title, 22, Ui.ink); t.setPadding(0, Ui.dp(18), 0, Ui.dp(12)); c.addView(t); }
  return c;
 }

 static MainActivity.Layer show(MainActivity a, View v) { ScrollView scroll = scroll(a, v); MainActivity.Layer l = a.open(() -> scroll, true); l.build = null; return l; }

 static ScrollView scroll(MainActivity a, View content) {
  ScrollView s = new ScrollView(a); s.setFillViewport(false); s.setVerticalScrollBarEnabled(false);
  s.addView(content, new FrameLayout.LayoutParams(-1, -2)); return s;
 }

 /** Choose one option. Calls back with the index and closes. */
 static void pick(MainActivity a, String title, List<String> labels, List<String> icons, int selected, IntConsumer done) {
  LinearLayout c = sheetBody(a, title); ScrollView sv = new ScrollView(a); LinearLayout list = Ui.col(a); sv.addView(list);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  for (int i = 0; i < labels.size(); i++) {
   final int idx = i; LinearLayout r = Ui.row(a); r.setPadding(Ui.dp(14), Ui.dp(13), Ui.dp(14), Ui.dp(13)); if (i == selected) r.setBackground(Ui.shape(Ui.surface2, 18));
   if (icons != null) r.addView(Ui.badge(a, icons.get(i), 38, i == selected ? Ui.accent : Ui.surface2, i == selected ? Ui.onAccent : Ui.ink), Ui.lpm(Ui.dp(38), Ui.dp(38), 0, 0, 14, 0));
   r.addView(Ui.text(a, labels.get(i), 16, Ui.ink, i == selected), Ui.weight(1)); if (i == selected) r.addView(Ui.icon(a, "check", 20, Ui.ink));
   Ui.tap(r, () -> { a.close(ref[0]); done.accept(idx); }); list.addView(r);
  }
  c.addView(sv, new LinearLayout.LayoutParams(-1, Math.min(Ui.dp(60) * labels.size() + Ui.dp(4), Ui.dp(420))));
  ref[0] = show(a, c);
 }

 static EditText field(MainActivity a, String hint, String initial, int type) {
  EditText e = new EditText(a); e.setHint(L.t(hint)); e.setHintTextColor(Ui.muted); e.setTextColor(Ui.ink); e.setTextSize(16); e.setTypeface(Ui.body); e.setInputType(type); e.setSingleLine(true); e.setText(initial);
  e.setBackground(Ui.outline(Ui.surface2, Ui.surface2, 16, 1)); e.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14)); return e;
 }
 static final int TEXT = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, NUMBER = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;

 /** Form with labelled fields. Calls back with the values, in order. */
 static void form(MainActivity a, String title, String[] labels, String[] initial, int[] types, String action, Consumer<String[]> done, Runnable delete) {
  LinearLayout c = sheetBody(a, title); EditText[] es = new EditText[labels.length];
  for (int i = 0; i < labels.length; i++) { TextView l = Ui.label(a, labels[i]); l.setPadding(Ui.dp(4), Ui.dp(10), 0, Ui.dp(6)); c.addView(l); es[i] = field(a, labels[i], initial[i], types[i]); c.addView(es[i]); }
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  TextView ok = Ui.button(a, action, true); c.addView(ok, Ui.lpm(-1, -2, 0, 20, 0, 0));
  Ui.tap(ok, () -> { String[] v = new String[es.length]; for (int i = 0; i < es.length; i++) v[i] = es[i].getText().toString().trim(); done.accept(v); a.close(ref[0]); });
  if (delete != null) { TextView d = Ui.button(a, "Delete", false); d.setTextColor(Ui.neg); c.addView(d, Ui.lpm(-1, -2, 0, 10, 0, 0)); Ui.tap(d, () -> confirm(a, "Delete this?", "This cannot be undone.", "Delete", () -> { delete.run(); a.close(ref[0]); })); }
  ref[0] = show(a, c);
 }

 static void confirm(MainActivity a, String title, String body, String action, Runnable yes) {
  LinearLayout c = sheetBody(a, title); TextView t = Ui.text(a, body, 14.5f, Ui.muted, false); t.setPadding(0, 0, 0, Ui.dp(18)); c.addView(t);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  LinearLayout r = Ui.row(a); TextView no = Ui.button(a, "Cancel", false), ok = Ui.button(a, action, true); r.addView(no, Ui.lpm(0, -2, 0, 0, 6, 0)); ((LinearLayout.LayoutParams) no.getLayoutParams()).weight = 1; r.addView(ok, Ui.lpm(0, -2, 6, 0, 0, 0)); ((LinearLayout.LayoutParams) ok.getLayoutParams()).weight = 1; c.addView(r);
  Ui.tap(no, () -> a.close(ref[0])); Ui.tap(ok, () -> { a.close(ref[0]); yes.run(); }); ref[0] = show(a, c);
 }

 static void txDetail(MainActivity a, Store.Tx t) {
  Store db = a.db; Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> people = Kit.peopleMap(db);
  LinearLayout c = sheetBody(a, null); final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  LinearLayout top = Ui.col(a); top.setGravity(Gravity.CENTER_HORIZONTAL); top.setPadding(0, Ui.dp(22), 0, Ui.dp(10));
  boolean tone = t.kind.equals("expense") || t.kind.equals("income"); top.addView(Ui.badge(a, Kit.iconOf(t, cats), 60, tone ? Kit.tone(t.cat) : Ui.surface2, tone ? 0xFF14151A : Ui.ink));
  int sign = t.kind.equals("transfer") ? 0 : Store.sign(t.kind); TextView am = Ui.big(a, (sign > 0 ? "+" : sign < 0 ? "-" : "") + Fmt.money(t.amount), 38, sign > 0 ? Ui.pos : Ui.ink); am.setPadding(0, Ui.dp(14), 0, Ui.dp(4)); top.addView(am);
  top.addView(Ui.text(a, Kit.titleOf(t, cats, people), 15, Ui.muted, true)); c.addView(top);
  LinearLayout card = Ui.col(a); card.setBackground(Ui.shape(Ui.surface2, 22)); card.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
  card.addView(kv(a, "Type", Kit.kindName(t.kind))); if (tone) { Store.Cat ct = cats.get(t.cat); card.addView(kv(a, "Category", ct == null ? "None" : ct.name)); }
  card.addView(kv(a, t.kind.equals("transfer") ? "From" : "Account", accs.get(t.account) == null ? "None" : accs.get(t.account).name)); if (t.kind.equals("transfer")) card.addView(kv(a, "To", accs.get(t.dest) == null ? "None" : accs.get(t.dest).name));
  if (t.person != 0 && people.get(t.person) != null) card.addView(kv(a, "Person", people.get(t.person).name)); card.addView(kv(a, "Date", Fmt.dayLabel(t.day) + ", " + t.day)); if (!t.note.isEmpty()) card.addView(kv(a, "Note", t.note));
  c.addView(card);
  LinearLayout r = Ui.row(a); TextView e = Ui.button(a, "Edit", true), d = Ui.button(a, "Delete", false); d.setTextColor(Ui.neg);
  r.addView(e, Ui.lpm(0, -2, 0, 0, 6, 0)); ((LinearLayout.LayoutParams) e.getLayoutParams()).weight = 1; r.addView(d, Ui.lpm(0, -2, 6, 0, 0, 0)); ((LinearLayout.LayoutParams) d.getLayoutParams()).weight = 1; c.addView(r, Ui.lpm(-1, -2, 0, 16, 0, 0));
  Ui.tap(e, () -> { a.close(ref[0]); a.openEntry(t.kind, t); });
  Ui.tap(d, () -> { a.close(ref[0]); db.deleteTx(t.id); a.refresh(); a.toast("Entry deleted", () -> { t.id = 0; db.save(t); a.refresh(); }); });
  ref[0] = show(a, c);
 }
 static View kv(MainActivity a, String k, String v) { LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(13), 0, Ui.dp(13)); r.addView(Ui.text(a, k, 14, Ui.muted, false), Ui.weight(1)); TextView t = Ui.text(a, v, 14.5f, Ui.ink, true); t.setGravity(Gravity.END); t.setMaxWidth(Ui.dp(220)); r.addView(t); return r; }
}
