package in.aicon.hisab;

import android.text.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

final class LedgerPage {
 private LedgerPage() {}
 static final String[][] FILTERS = {{"all", "All"}, {"expense", "Spent"}, {"income", "Income"}, {"transfer", "Moves"}, {"people", "People"}};

 static boolean match(String filter, Store.Tx t) {
  switch (filter) { case "all": return true; case "people": return t.kind.equals("lent") || t.kind.equals("borrowed") || t.kind.startsWith("repay"); default: return t.kind.equals(filter); }
 }

 static View build(MainActivity a) {
  Store db = a.db; ScrollView[] sv = new ScrollView[1]; LinearLayout c = Kit.column(a, sv, true);
  c.addView(Kit.title(a, "Everything you have noted", "Ledger", false));
  YearMonth ym = a.ledgerMonth;
  LinearLayout nav = Ui.row(a); nav.setBackground(Ui.shape(Ui.surface, 26)); nav.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
  FrameLayout l = Ui.badge(a, "left", 38, Ui.surface2, Ui.ink), r = Ui.badge(a, "right", 38, Ui.surface2, Ui.ink); l.setContentDescription("Previous month"); r.setContentDescription("Next month");
  nav.addView(l, Ui.lp(Ui.dp(38), Ui.dp(38))); TextView mt = Ui.big(a, Fmt.month(ym), 18, Ui.ink); mt.setGravity(Gravity.CENTER); nav.addView(mt, Ui.weight(1)); nav.addView(r, Ui.lp(Ui.dp(38), Ui.dp(38)));
  Ui.tap(l, () -> { a.ledgerMonth = ym.minusMonths(1); a.ledgerDay = 0; a.refresh(); }); Ui.tap(r, () -> { a.ledgerMonth = ym.plusMonths(1); a.ledgerDay = 0; a.refresh(); }); c.addView(nav);

  List<Store.Tx> month = db.txs(Fmt.from(ym), Fmt.to(ym)); long in = 0, out = 0; Map<Integer, Long> spend = new HashMap<>();
  for (Store.Tx t : month) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) { out += t.amount; spend.merge(LocalDate.parse(t.day).getDayOfMonth(), t.amount, Long::sum); } }
  LinearLayout sum = Ui.row(a); sum.setPadding(Ui.dp(4), Ui.dp(16), Ui.dp(4), Ui.dp(4));
  sum.addView(kv(a, "Income", Fmt.money(in), Ui.pos), Ui.weight(1)); sum.addView(kv(a, "Spent", Fmt.money(out), Ui.ink), Ui.weight(1)); sum.addView(kv(a, "Net", Fmt.money(in - out), in - out < 0 ? Ui.neg : Ui.ink), Ui.weight(1)); c.addView(sum);

  // search + calendar toggle
  LinearLayout sr = Ui.row(a); sr.setBackground(Ui.outline(Ui.surface, Ui.line, 26, 1)); sr.setPadding(Ui.dp(16), Ui.dp(2), Ui.dp(8), Ui.dp(2));
  sr.addView(Ui.icon(a, "search", 20, Ui.muted)); EditText e = new EditText(a); e.setHint("Search notes and categories"); e.setHintTextColor(Ui.muted); e.setTextColor(Ui.ink); e.setTextSize(15); e.setTypeface(Ui.body); e.setBackground(null); e.setSingleLine(true); e.setText(a.ledgerQuery); e.setPadding(Ui.dp(10), Ui.dp(12), Ui.dp(10), Ui.dp(12)); sr.addView(e, Ui.weight(1));
  FrameLayout cal = Ui.badge(a, "calendar", 38, a.ledgerCalendar ? Ui.accent : Ui.surface2, a.ledgerCalendar ? Ui.onAccent : Ui.ink); sr.addView(cal, Ui.lp(Ui.dp(38), Ui.dp(38))); cal.setContentDescription("Calendar view"); Ui.tap(cal, () -> { a.ledgerCalendar = !a.ledgerCalendar; a.refresh(); });
  c.addView(sr, Ui.lpm(-1, -2, 0, 14, 0, 0));
  final boolean[] ignore = {true};
  e.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence x, int i, int j, int k) {} public void afterTextChanged(Editable ed) {} public void onTextChanged(CharSequence x, int i, int j, int k) { if (ignore[0]) { ignore[0] = false; if (x.toString().equals(a.ledgerQuery)) return; } a.ledgerQuery = x.toString(); a.tabHost.removeAllViews(); View v = LedgerPage.build(a); a.tabHost.addView(v); EditText ne = v.findViewWithTag("q"); if (ne != null) { ne.requestFocus(); ne.setSelection(ne.getText().length()); } } });
  e.setTag("q");

  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout pr = Ui.row(a); pr.setPadding(0, Ui.dp(12), 0, 0);
  for (String[] f : FILTERS) { TextView p = Ui.pill(a, f[1], a.ledgerFilter.equals(f[0])); pr.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0)); Ui.tap(p, () -> { a.ledgerFilter = f[0]; a.refresh(); }); }
  hs.addView(pr); c.addView(hs);

  if (a.ledgerCalendar) { LinearLayout cc = Ui.card(a); Charts.Heat h = new Charts.Heat(a, ym, spend, a.ledgerDay, d -> { a.ledgerDay = a.ledgerDay == d ? 0 : d; a.refresh(); }); cc.addView(h, new LinearLayout.LayoutParams(-1, -2)); cc.addView(Ui.text(a, a.ledgerDay > 0 ? "Showing day " + a.ledgerDay + ". Tap it again to clear." : "Darker days are where you spent more. Tap a day.", 12, Ui.muted, false), Ui.lpm(-2, -2, 0, 10, 0, 0)); c.addView(cc, Ui.lpm(-1, -2, 0, 14, 0, 0)); }

  Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> people = Kit.peopleMap(db);
  String q = a.ledgerQuery.trim().toLowerCase(Locale.ROOT); List<Store.Tx> shown = new ArrayList<>();
  for (Store.Tx t : month) { if (!match(a.ledgerFilter, t)) continue; if (a.ledgerDay > 0 && LocalDate.parse(t.day).getDayOfMonth() != a.ledgerDay) continue; if (!q.isEmpty()) { Store.Cat ct = cats.get(t.cat); Store.Person p = people.get(t.person); String hay = (t.note + " " + (ct == null ? "" : ct.name) + " " + (p == null ? "" : p.name) + " " + Kit.kindName(t.kind)).toLowerCase(Locale.ROOT); if (!hay.contains(q)) continue; } shown.add(t); }
  if (shown.isEmpty()) { LinearLayout em = Ui.card(a); em.addView(Kit.empty(a, "search", "No entries here", q.isEmpty() ? "Nothing in this month for this filter." : "Nothing matches your search.")); c.addView(em, Ui.lpm(-1, -2, 0, 16, 0, 0)); }
  String day = ""; LinearLayout card = null;
  for (Store.Tx t : shown) {
   if (!t.day.equals(day)) { day = t.day; long dayOut = 0; for (Store.Tx x : shown) if (x.day.equals(t.day) && x.kind.equals("expense")) dayOut += x.amount;
    LinearLayout hd = Ui.row(a); hd.setPadding(Ui.dp(4), Ui.dp(22), Ui.dp(4), Ui.dp(10)); hd.addView(Ui.text(a, Fmt.dayLabel(t.day), 14, Ui.ink, true), Ui.weight(1)); if (dayOut > 0) hd.addView(Ui.text(a, "-" + Fmt.money(dayOut), 13, Ui.muted, true)); c.addView(hd);
    card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4)); c.addView(card); } else card.addView(Ui.divider(a));
   card.addView(Kit.txRow(a, t, cats, accs, people, false));
  }
  return sv[0];
 }
 static View kv(MainActivity a, String k, String v, int color) { LinearLayout l = Ui.col(a); l.addView(Ui.text(a, k, 12, Ui.muted, true)); TextView t = Ui.big(a, v, 18, color); t.setPadding(0, Ui.dp(5), 0, 0); l.addView(t); return l; }
}
