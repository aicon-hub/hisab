package in.aicon.hisab;

import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

/** Add or edit an entry: big amount, custom keypad, one-tap category. */
final class EntrySheet {
 private EntrySheet() {}

 static final class S { String kind; StringBuilder expr = new StringBuilder(); long account, dest, cat, person; LocalDate day = LocalDate.now(); String note = ""; long id; }

 static long eval(String e) {
  if (e.isEmpty()) return -1; long total = 0; int sign = 1; StringBuilder cur = new StringBuilder();
  for (int i = 0; i <= e.length(); i++) {
   char ch = i < e.length() ? e.charAt(i) : '+';
   if (ch == '+' || ch == '-') { if (cur.length() > 0) { long v = Ledger.parse(cur.toString().endsWith(".") ? cur + "0" : cur.toString()); if (v < 0) return -1; total += sign * v; cur.setLength(0); } else if (i < e.length() && i > 0) return -1; sign = ch == '-' ? -1 : 1; }
   else cur.append(ch);
  }
  return total;
 }

 static void open(MainActivity a, String kind, Store.Tx edit) {
  S s = new S(); Store db = a.db; List<Store.Acc> accs = db.accounts(); if (accs.isEmpty()) { a.toast("Add an account first", null); return; }
  s.kind = kind; s.account = accs.get(0).id; long last = Long.parseLong(db.pref("lastAcc", "0")); for (Store.Acc x : accs) if (x.id == last) s.account = x.id;
  if (edit != null) { s.id = edit.id; s.kind = edit.kind; s.expr.append(Ledger_plain(edit.amount)); s.account = edit.account; s.dest = edit.dest; s.cat = edit.cat; s.person = edit.person; s.day = LocalDate.parse(edit.day); s.note = edit.note; }
  else if (accs.size() > 1) s.dest = accs.get(1).id == s.account ? accs.get(0).id : accs.get(1).id;
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  ref[0] = a.open(() -> view(a, s, ref), true); ref[0].build = null;
 }
 static void openPrefilled(MainActivity a, Store.Tx t) {
  S s = new S(); s.kind = t.kind; if (t.amount > 0) s.expr.append(Ledger_plain(t.amount)); s.account = t.account; s.person = t.person; s.day = LocalDate.parse(t.day);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1]; ref[0] = a.open(() -> view(a, s, ref), true); ref[0].build = null;
 }
 static String Ledger_plain(long minor) { return minor % 100 == 0 ? "" + minor / 100 : String.format(Locale.US, "%d.%02d", minor / 100, minor % 100); }

 static View view(MainActivity a, S s, MainActivity.Layer[] ref) {
  Store db = a.db; LinearLayout c = Sheets.sheetBody(a, null); c.setPadding(Ui.dp(18), Ui.dp(10), Ui.dp(18), Ui.dp(16));
  LinearLayout meta = Ui.col(a), cats = Ui.col(a); TextView[] amountTv = new TextView[1], subTv = new TextView[1]; EditText note;
  Runnable[] renderAll = new Runnable[1];
  // type pills
  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout pills = Ui.row(a);
  String[][] ks = {{"expense", "Spent"}, {"income", "Income"}, {"transfer", "Move"}, {"lent", "Lent"}, {"borrowed", "Borrowed"}};
  for (String[] k : ks) { TextView p = Ui.pill(a, k[1], s.kind.equals(k[0])); p.setTag(k[0]); pills.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0)); Ui.tap(p, () -> { if (s.id > 0 && (s.kind.startsWith("repay"))) return; s.kind = k[0]; if (s.cat != 0 && db.categoryMap().get(s.cat) != null && !db.categoryMap().get(s.cat).kind.equals(k[0])) s.cat = 0; for (int i = 0; i < pills.getChildCount(); i++) { TextView q = (TextView) pills.getChildAt(i); boolean on = q.getTag().equals(s.kind); q.setTextColor(on ? Ui.onAccent : Ui.ink); q.setBackground(on ? Ui.shape(Ui.accent, 20) : Ui.outline(0, Ui.line, 20, 1)); } renderAll[0].run(); }); }
  if (s.kind.startsWith("repay")) { TextView p = Ui.pill(a, Kit.kindName(s.kind), true); pills.addView(p); }
  hs.addView(pills); c.addView(hs, Ui.lpm(-1, -2, 0, 14, 0, 6));
  // amount
  LinearLayout ar = Ui.row(a); ar.setPadding(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(2));
  TextView sym = Ui.big(a, Fmt.symbol.trim(), 26, Ui.muted); ar.addView(sym, Ui.lpm(-2, -2, 0, 0, 10, 0));
  amountTv[0] = Ui.big(a, "", 54, Ui.ink); amountTv[0].setSingleLine(true); ar.addView(amountTv[0], Ui.weight(1)); c.addView(ar);
  subTv[0] = Ui.text(a, " ", 13, Ui.muted, true); subTv[0].setPadding(Ui.dp(6), 0, 0, Ui.dp(8)); c.addView(subTv[0]);
  c.addView(meta); c.addView(cats);
  note = Sheets.field(a, "Add a note", s.note, Sheets.TEXT); note.setTextSize(15); c.addView(note, Ui.lpm(-1, -2, 0, 10, 0, 12));
  note.addTextChangedListener(new android.text.TextWatcher() { public void beforeTextChanged(CharSequence x, int i, int j, int k) {} public void onTextChanged(CharSequence x, int i, int j, int k) { s.note = x.toString().trim(); } public void afterTextChanged(android.text.Editable e) {} });
  Runnable show = () -> { String e = s.expr.toString(); amountTv[0].setText(e.isEmpty() ? "0" : e.replace("-", " - ").replace("+", " + ")); amountTv[0].setTextColor(e.isEmpty() ? Ui.muted : Ui.ink); amountTv[0].setTextSize(e.length() > 11 ? 30 : e.length() > 8 ? 40 : 54); boolean ops = e.contains("+") || e.contains("-"); long v = eval(e); subTv[0].setText(ops && v > 0 ? "= " + Fmt.money(v) : " "); };
  renderAll[0] = () -> { renderMeta(a, s, meta, ref); renderCats(a, s, cats); show.run(); };
  // keypad
  String[][] keys = {{"7", "8", "9", "<"}, {"4", "5", "6", "+"}, {"1", "2", "3", "-"}, {".", "0", "00", "ok"}};
  LinearLayout pad = Ui.col(a);
  for (String[] rowKeys : keys) {
   LinearLayout r = Ui.row(a);
   for (String k : rowKeys) {
    View key; boolean okKey = k.equals("ok"), op = k.equals("+") || k.equals("-");
    if (k.equals("<")) key = Ui.badge(a, "back", 56, 0, Ui.ink); else if (okKey) key = Ui.badge(a, "check", 56, Ui.dark ? Ui.accent : Ui.ink, Ui.dark ? Ui.onAccent : 0xFFFFFFFF); else { TextView t = Ui.big(a, k.equals("-") ? "−" : k, 24, Ui.ink); t.setGravity(Gravity.CENTER); key = t; }
    if (!okKey) key.setBackground(Ui.shape(op || k.equals("<") ? Ui.surface2 : Ui.bg == Ui.surface ? Ui.surface2 : (Ui.dark ? Ui.surface2 : 0xFFF6F2EA), 20));
    if (key instanceof FrameLayout && !okKey) key.setBackground(Ui.shape(Ui.surface2, 20)); else if (okKey) key.setBackground(Ui.shape(Ui.dark ? Ui.accent : Ui.ink, 20));
    key.setContentDescription(k.equals("<") ? "Delete digit" : okKey ? "Save" : k);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(56), 1); lp.setMargins(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4)); r.addView(key, lp);
    Ui.tap(key, () -> {
     String e = s.expr.toString();
     if (k.equals("<")) { if (e.length() > 0) s.expr.setLength(e.length() - 1); }
     else if (okKey) { if (e.contains("+") || e.contains("-")) { long v = eval(e); if (v > 0) { s.expr.setLength(0); s.expr.append(Ledger_plain(v)); } } else save(a, s, ref); }
     else if (op) { if (e.length() > 0 && !e.endsWith("+") && !e.endsWith("-")) s.expr.append(k); }
     else if (k.equals(".")) { String last = e.substring(Math.max(e.lastIndexOf('+'), e.lastIndexOf('-')) + 1); if (!last.contains(".")) s.expr.append(last.isEmpty() ? "0." : "."); }
     else { String last = e.substring(Math.max(e.lastIndexOf('+'), e.lastIndexOf('-')) + 1); int dot = last.indexOf('.'); if (dot >= 0 && last.length() - dot > 2) return; if (last.equals("0") && !k.equals("00")) s.expr.setLength(e.length() - 1); if (last.isEmpty() && k.equals("00")) return; if (s.expr.length() < 18) s.expr.append(k); }
     show.run(); okIcon(key, s);
    });
   }
   pad.addView(r);
  }
  c.addView(pad);
  renderAll[0].run(); return Sheets.scroll(a, c);
 }
 static void okIcon(View key, S s) {}

 static void renderMeta(MainActivity a, S s, LinearLayout meta, MainActivity.Layer[] ref) {
  meta.removeAllViews(); Store db = a.db; List<Store.Acc> accs = db.accounts(); Map<Long, Store.Acc> am = Kit.accMap(db); Map<Long, Store.Person> pm = Kit.peopleMap(db);
  LinearLayout row = Ui.row(a); HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); hs.addView(row);
  boolean transfer = s.kind.equals("transfer"), people = s.kind.equals("lent") || s.kind.equals("borrowed") || s.kind.startsWith("repay");
  row.addView(chip(a, "wallet", (transfer ? "From " : "") + (am.get(s.account) == null ? "Account" : am.get(s.account).name), () -> { List<String> n = new ArrayList<>(), ic = new ArrayList<>(); int sel = -1; for (int i = 0; i < accs.size(); i++) { n.add(accs.get(i).name + "   " + Privacy.amount(db,accs.get(i),db.balance(accs.get(i)))); ic.add(accs.get(i).kind.equals("cash") ? "cash" : accs.get(i).kind.equals("bank") ? "bank" : accs.get(i).kind.equals("card") ? "card" : "wallet"); if (accs.get(i).id == s.account) sel = i; } Sheets.pick(a, "Account", n, ic, sel, i -> { s.account = accs.get(i).id; renderMeta(a, s, meta, ref); }); }));
  if (transfer) row.addView(chip(a, "right", "To " + (am.get(s.dest) == null ? "Account" : am.get(s.dest).name), () -> { List<String> n = new ArrayList<>(), ic = new ArrayList<>(); int sel = -1; for (int i = 0; i < accs.size(); i++) { n.add(accs.get(i).name); ic.add("wallet"); if (accs.get(i).id == s.dest) sel = i; } Sheets.pick(a, "Move to", n, ic, sel, i -> { s.dest = accs.get(i).id; renderMeta(a, s, meta, ref); }); }));
  row.addView(chip(a, "calendar", Fmt.dayLabel(s.day.toString()), () -> { android.app.DatePickerDialog d = new android.app.DatePickerDialog(a, (v, y, m, dd) -> { s.day = LocalDate.of(y, m + 1, dd); renderMeta(a, s, meta, ref); }, s.day.getYear(), s.day.getMonthValue() - 1, s.day.getDayOfMonth()); d.show(); }));
  if (people) row.addView(chip(a, "user", pm.get(s.person) == null ? "Choose person" : pm.get(s.person).name, () -> { List<Store.Person> ps = db.people(); List<String> n = new ArrayList<>(), ic = new ArrayList<>(); int sel = -1; for (int i = 0; i < ps.size(); i++) { n.add(ps.get(i).name); ic.add("user"); if (ps.get(i).id == s.person) sel = i; } n.add("New from contacts"); ic.add("people"); n.add("New by name"); ic.add("plus"); final int cnt = ps.size();
   Sheets.pick(a, "Person", n, ic, sel, i -> { if (i < cnt) { s.person = ps.get(i).id; renderMeta(a, s, meta, ref); } else if (i == cnt) a.pickContact((nm, ph) -> { s.person = db.addPerson(nm, ph); renderMeta(a, s, meta, ref); }); else Sheets.form(a, "New person", new String[]{"Name"}, new String[]{""}, new int[]{Sheets.TEXT}, "Add", v -> { if (!v[0].isEmpty()) { s.person = db.addPerson(v[0], ""); renderMeta(a, s, meta, ref); } }, null); }); }));
  meta.addView(hs);
 }
 static View chip(MainActivity a, String icon, String text, Runnable r) {
  LinearLayout l = Ui.row(a); l.setBackground(Ui.outline(0, Ui.line, 20, 1)); l.setPadding(Ui.dp(12), Ui.dp(9), Ui.dp(14), Ui.dp(9)); l.addView(Ui.icon(a, icon, 18, Ui.ink)); TextView t = Ui.text(a, text, 13.5f, Ui.ink, true); t.setPadding(Ui.dp(7), 0, 0, 0); l.addView(t); l.setLayoutParams(Ui.lpm(-2, -2, 0, 0, 8, 0)); Ui.tap(l, r); return l;
 }

 static void renderCats(MainActivity a, S s, LinearLayout box) {
  box.removeAllViews(); if (!(s.kind.equals("expense") || s.kind.equals("income"))) return; List<Store.Cat> cs = a.db.categories(s.kind);
  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout row = Ui.row(a); hs.addView(row); row.setPadding(0, Ui.dp(12), 0, 0);
  for (Store.Cat c : cs) { boolean on = c.id == s.cat; LinearLayout cell = Ui.col(a); cell.setGravity(Gravity.CENTER_HORIZONTAL); cell.setPadding(Ui.dp(4), Ui.dp(2), Ui.dp(4), Ui.dp(2));
   FrameLayout b = Ui.badge(a, c.icon, 52, on ? Kit.tone(c.id) : Ui.surface2, on ? 0xFF14151A : Ui.ink); if (on) b.setBackground(Ui.outline(Kit.tone(c.id), Ui.ink, 26, 2)); cell.addView(b, Ui.lp(Ui.dp(52), Ui.dp(52))); TextView t = Ui.text(a, c.name, 11.5f, on ? Ui.ink : Ui.muted, on); t.setPadding(0, Ui.dp(6), 0, 0); cell.addView(t); row.addView(cell, Ui.lpm(Ui.dp(66), -2, 0, 0, 4, 0)); Ui.tap(cell, () -> { s.cat = c.id; renderCats(a, s, box); }); }
  box.addView(hs);
 }

 static void save(MainActivity a, S s, MainActivity.Layer[] ref) {
  long v = eval(s.expr.toString()); if (v <= 0) { a.toast("Enter an amount first", null); return; }
  boolean people = s.kind.equals("lent") || s.kind.equals("borrowed") || s.kind.startsWith("repay");
  if (people && s.person == 0) { a.toast("Choose a person first", null); return; }
  if (s.kind.equals("transfer") && (s.dest == 0 || s.dest == s.account)) { a.toast("Pick two different accounts", null); return; }
  Store.Tx t = new Store.Tx(); t.id = s.id; t.kind = s.kind; t.amount = v; t.day = s.day.toString(); t.account = s.account; t.dest = s.kind.equals("transfer") ? s.dest : 0; t.person = people ? s.person : 0; t.note = s.note;
  if (s.kind.equals("expense") || s.kind.equals("income")) { t.cat = s.cat; if (t.cat == 0) { List<Store.Cat> cs = a.db.categories(s.kind); if (!cs.isEmpty()) t.cat = cs.get(cs.size() - 1).id; } }
  long id = a.db.save(t); a.db.setPref("lastAcc", "" + s.account); a.close(ref[0]); a.refresh();
  final boolean fresh = s.id == 0; a.toast((fresh ? "Added " : "Updated ") + Fmt.money(v), fresh ? () -> { a.db.deleteTx(id); a.refresh(); } : null);
 }
}
