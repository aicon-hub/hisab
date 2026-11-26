package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

final class InsightsPage {
 private InsightsPage() {}

 static View build(MainActivity a) {
  Store db = a.db; ScrollView[] sv = new ScrollView[1]; LinearLayout c = Kit.column(a, sv, true);
  c.addView(Kit.title(a, "Where it went", "Insights", false));
  YearMonth ym = a.insightMonth; boolean pieAll = db.pref("pieScope", "month").equals("all");
  LinearLayout nav = Ui.row(a); nav.setBackground(Ui.shape(Ui.surface, 26)); nav.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
  FrameLayout l = Ui.badge(a, "left", 38, Ui.surface2, Ui.ink), r = Ui.badge(a, "right", 38, Ui.surface2, Ui.ink);
  nav.addView(l, Ui.lp(Ui.dp(38), Ui.dp(38))); TextView mt = Ui.big(a, Fmt.month(ym), 18, Ui.ink); mt.setGravity(Gravity.CENTER); nav.addView(mt, Ui.weight(1)); nav.addView(r, Ui.lp(Ui.dp(38), Ui.dp(38)));
  Ui.tap(l, () -> { a.insightMonth = ym.minusMonths(1); a.refresh(); }); Ui.tap(r, () -> { a.insightMonth = ym.plusMonths(1); a.refresh(); }); c.addView(nav);

  List<Store.Tx> month = db.txs(Fmt.from(ym), Fmt.to(ym)); Map<Long, Store.Cat> cats = db.categoryMap(); long in = 0, out = 0; Map<Long, Long> byCat = new HashMap<>(); Map<String, Long> byNote = new HashMap<>(); long[] week = new long[7];
  for (Store.Tx t : month) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) { out += t.amount; byCat.merge(t.cat, t.amount, Long::sum); if (!t.note.isEmpty()) byNote.merge(t.note, t.amount, Long::sum); week[LocalDate.parse(t.day).getDayOfWeek().getValue() - 1] += t.amount; } }
  // saved card
  LinearLayout top = Ui.card(a); long saved = in - out; int rate = in > 0 ? (int) Math.round(100.0 * saved / in) : 0;
  top.addView(Ui.label(a, "Kept this month")); TextView big = Ui.big(a, Fmt.money(saved), 36, saved < 0 ? Ui.neg : Ui.ink); big.setPadding(0, Ui.dp(8), 0, Ui.dp(6)); top.addView(big);
  top.addView(Ui.text(a, in > 0 ? (saved >= 0 ? "You kept " + rate + "% of what came in" : "Spending ran " + Math.abs(rate) + "% above income") : "No income noted this month", 13.5f, Ui.muted, false));
  long gstM = 0; try { gstM = Long.parseLong(db.pref("gstBills:" + ym.toString(), "0")); } catch (Exception e) {}
  if (gstM > 0) top.addView(Ui.text(a, "GST from scanned bills this month: " + Fmt.money(gstM), 13, Ui.muted, false));
  Store.Tx dummy = null; LinearLayout pr = Ui.row(a); pr.setPadding(0, Ui.dp(16), 0, 0); pr.addView(HomePage.pill(a, "Income", Fmt.money(in), Ui.pos), Ui.weight(1)); ((LinearLayout.LayoutParams) pr.getChildAt(0).getLayoutParams()).rightMargin = Ui.dp(6); pr.addView(HomePage.pill(a, "Spent", Fmt.money(out), Ui.ink), Ui.weight(1)); ((LinearLayout.LayoutParams) pr.getChildAt(1).getLayoutParams()).leftMargin = Ui.dp(6);
  for (int i = 0; i < 2; i++) pr.getChildAt(i).setBackground(Ui.shape(Ui.surface2, 20)); top.addView(pr); c.addView(top, Ui.lpm(-1, -2, 0, 14, 0, 0));

  LinearLayout more = Ui.card(a); more.setPadding(Ui.dp(18), Ui.dp(2), Ui.dp(18), Ui.dp(2));
  more.addView(Settings.item(a, "chart", "Year report", "Months, totals, compare", () -> YearReport.open(a))); more.addView(Ui.divider(a));
  more.addView(Settings.item(a, "wallet", "Net worth", "Accounts and dues", () -> NetWorth.open(a))); more.addView(Ui.divider(a));
  more.addView(Settings.item(a, "repeat", "Subscriptions", "Repeating charges", () -> Subscriptions.open(a))); more.addView(Ui.divider(a));
  more.addView(Settings.item(a, "upload", "Share month summary", null, () -> {
   android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, summary(db, ym));
   a.launchExternal(android.content.Intent.createChooser(i, "Share month summary"));
  }));
  more.addView(Ui.divider(a)); more.addView(Settings.item(a, "bell", "Read out this month", "Spoken summary", () -> Speech.say(a, Speech.monthLine(db, ym))));
  c.addView(more, Ui.lpm(-1, -2, 0, 0, 0, 14));
  if (RoundUp.step(db) > 0) { LinearLayout ru = Ui.card(a); ru.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(14));
   LinearLayout rr = Ui.row(a); rr.addView(Ui.text(a, "Round-up pot", 13, Ui.muted, true), Ui.weight(1)); rr.addView(Ui.big(a, Fmt.money(RoundUp.pot(db)), 20, Ui.ink)); ru.addView(rr);
   ru.addView(Ui.text(a, "Spare change if every expense rounded up - counted, not moved.", 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 4, 0, 0));
   c.addView(ru, Ui.lpm(-1, -2, 0, 0, 0, 10)); }

  // by category
  c.addView(Kit.sectionHead(a, "By category", pieAll ? "All time" : "This month", () -> { db.setPref("pieScope", pieAll ? "month" : "all"); a.refresh(); }));
  Map<Long, Long> pieCat = byCat; long pieOut = out;
  if (pieAll) { pieCat = new HashMap<>(); pieOut = 0; for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) if (t.kind.equals("expense")) { pieOut += t.amount; pieCat.merge(t.cat, t.amount, Long::sum); } }
  List<Map.Entry<Long, Long>> es = new ArrayList<>(pieCat.entrySet()); es.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
  LinearLayout cc = Ui.card(a);
  if (es.isEmpty()) cc.addView(Kit.empty(a, "chart", "No spending", "Nothing noted in this month."));
  else {
   int n = Math.min(es.size(), 6); long[] vals = new long[n + (es.size() > n ? 1 : 0)]; int[] cols = new int[vals.length]; long rest = 0;
   for (int i = 0; i < es.size(); i++) { if (i < n) { vals[i] = es.get(i).getValue(); cols[i] = Kit.tone(es.get(i).getKey()); } else rest += es.get(i).getValue(); } if (rest > 0) { vals[n] = rest; cols[n] = Ui.dark ? 0xFF3A3E49 : 0xFFD9D2C2; }
   Charts.Donut d = new Charts.Donut(a, vals, cols, Fmt.compact(pieOut), pieAll ? "all time" : "spent"); cc.addView(d, Ui.lpm(Ui.dp(190), Ui.dp(190), 0, 4, 0, 14)); ((LinearLayout.LayoutParams) d.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
   for (int i = 0; i < Math.min(es.size(), 7); i++) { Store.Cat ct = cats.get(es.get(i).getKey()); long v = es.get(i).getValue(); int pct = pieOut > 0 ? (int) Math.round(100.0 * v / pieOut) : 0;
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(8), 0, Ui.dp(8)); row.addView(Ui.badge(a, ct == null ? "other" : ct.icon, 34, Kit.tone(es.get(i).getKey()), 0xFF14151A), Ui.lpm(Ui.dp(34), Ui.dp(34), 0, 0, 12, 0));
    LinearLayout m = Ui.col(a); LinearLayout h = Ui.row(a); h.addView(Ui.text(a, ct == null ? "Other" : ct.name, 14.5f, Ui.ink, true), Ui.weight(1)); h.addView(Ui.text(a, Fmt.money(v) + "  " + pct + "%", 13, Ui.muted, true)); m.addView(h);
    Charts.Pace bar = new Charts.Pace(a, v / (float) es.get(0).getValue(), -1, Kit.tone(es.get(i).getKey()), Ui.surface2); m.addView(bar, Ui.lp(-1, Ui.dp(16))); row.addView(m, Ui.weight(1)); cc.addView(row); }
  }
  c.addView(cc);

  // six months
  c.addView(Kit.sectionHead(a, "Six months", null, null));
  long[] mi = new long[6], mo = new long[6]; String[] ml = new String[6]; YearMonth first = ym.minusMonths(5);
  List<Store.Tx> six = db.txs(Fmt.from(first), Fmt.to(ym));
  for (int i = 0; i < 6; i++) ml[i] = Fmt.monthShort(first.plusMonths(i));
  for (Store.Tx t : six) { int idx = (int) java.time.temporal.ChronoUnit.MONTHS.between(first, YearMonth.from(LocalDate.parse(t.day))); if (idx < 0 || idx > 5) continue; if (t.kind.equals("income")) mi[idx] += t.amount; if (t.kind.equals("expense")) mo[idx] += t.amount; }
  LinearLayout tc = Ui.card(a); LinearLayout lg = Ui.row(a); lg.addView(dot(a, Ui.dark ? 0xFF3A3E49 : 0xFFD9D2C2), Ui.lp(Ui.dp(10), Ui.dp(10))); lg.addView(Ui.text(a, "  Income      ", 12, Ui.muted, true)); lg.addView(dot(a, Ui.dark ? Ui.accent : Ui.ink), Ui.lp(Ui.dp(10), Ui.dp(10))); lg.addView(Ui.text(a, "  Spent", 12, Ui.muted, true)); tc.addView(lg);
  tc.addView(new Charts.Pairs(a, mi, mo, ml), Ui.lpm(-1, Ui.dp(170), 0, 12, 0, 0)); c.addView(tc);

  // compare with last month
  Map<Long, Long> prev = new HashMap<>(); YearMonth pm = ym.minusMonths(1); for (Store.Tx t : db.txs(Fmt.from(pm), Fmt.to(pm))) if (t.kind.equals("expense")) prev.merge(t.cat, t.amount, Long::sum);
  List<long[]> diffs = new ArrayList<>(); for (long k : byCat.keySet()) { long d = byCat.get(k) - prev.getOrDefault(k, 0L); if (Math.abs(d) >= 10000) diffs.add(new long[]{k, d}); } diffs.sort((x, y) -> Long.compare(Math.abs(y[1]), Math.abs(x[1])));
  if (!diffs.isEmpty()) { c.addView(Kit.sectionHead(a, "Versus " + Fmt.month(pm).split(" ")[0], null, null)); LinearLayout dc = Ui.card(a);
   for (int i = 0; i < Math.min(4, diffs.size()); i++) { Store.Cat ct = cats.get(diffs.get(i)[0]); long d = diffs.get(i)[1]; if (i > 0) dc.addView(Ui.divider(a)); LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(11), 0, Ui.dp(11)); row.addView(Ui.icon(a, d > 0 ? "up" : "download", 20, d > 0 ? Ui.neg : Ui.pos), Ui.lpm(Ui.dp(20), Ui.dp(20), 0, 0, 12, 0)); row.addView(Ui.text(a, ct == null ? "Other" : ct.name, 15, Ui.ink, true), Ui.weight(1)); long pv = prev.getOrDefault(diffs.get(i)[0], 0L); String pct = pv > 0 ? "  " + Math.round(100.0 * d / pv) + "%" : ""; row.addView(Ui.text(a, (d > 0 ? "+" : "-") + Fmt.money(Math.abs(d)) + pct, 14, d > 0 ? Ui.neg : Ui.pos, true)); dc.addView(row); }
   c.addView(dc); }

  // weekday pattern
  c.addView(Kit.sectionHead(a, "Busiest days", null, null)); int hi = 0; for (int i = 1; i < 7; i++) if (week[i] > week[hi]) hi = i;
  LinearLayout wc = Ui.card(a); wc.addView(new Charts.Bars(a, week, new String[]{"M", "T", "W", "T", "F", "S", "S"}, out > 0 ? hi : -1, false), Ui.lp(-1, Ui.dp(130)));
  wc.addView(Ui.text(a, out > 0 ? "You spend the most on " + java.time.DayOfWeek.of(hi + 1).getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + "s." : "No spending to compare yet.", 13, Ui.muted, false), Ui.lpm(-2, -2, 0, 8, 0, 0));
  if (out > 0) { long wdSum = week[0] + week[1] + week[2] + week[3] + week[4], weSum = week[5] + week[6]; int wdN = 0, weN = 0; for (int dd = 1; dd <= ym.lengthOfMonth(); dd++) { if (ym.atDay(dd).getDayOfWeek().getValue() >= 6) weN++; else wdN++; } wc.addView(Ui.text(a, "Weekdays " + Fmt.money(wdSum / Math.max(1, wdN)) + " a day on average - weekends " + Fmt.money(weSum / Math.max(1, weN)) + ".", 13, Ui.muted, false), Ui.lpm(-2, -2, 0, 6, 0, 0)); }
  c.addView(wc);

  // spending calendar heatmap
  c.addView(Kit.sectionHead(a, "Spending calendar", null, null));
  Map<String, Long> byDay = new HashMap<>(); long maxDay = 0; for (Store.Tx t : month) if (t.kind.equals("expense")) byDay.merge(t.day, t.amount, Long::sum); for (long v : byDay.values()) maxDay = Math.max(maxDay, v);
  LinearLayout hc = Ui.card(a);
  LinearLayout dh = Ui.row(a); for (String dw : new String[]{"M","T","W","T","F","S","S"}) { TextView hv = Ui.text(a, dw, 11, Ui.muted, true); hv.setGravity(Gravity.CENTER); dh.addView(hv, Ui.weight(1)); } hc.addView(dh, Ui.lpm(-1, -2, 0, 0, 0, 4));
  int lead = ym.atDay(1).getDayOfWeek().getValue() - 1, days = ym.lengthOfMonth(), day = 1;
  for (int rr = 0; rr < 6 && day <= days; rr++) { LinearLayout gRow = Ui.row(a); gRow.setPadding(0, Ui.dp(2), 0, Ui.dp(2));
   for (int col = 0; col < 7; col++) { TextView cell;
    if ((rr == 0 && col < lead) || day > days) { cell = Ui.text(a, "", 12, Ui.muted, false); }
    else { long v = byDay.getOrDefault(ym.atDay(day).toString(), 0L); int alpha = v > 0 ? 40 + (int) (160.0 * v / Math.max(1, maxDay)) : 0;
     cell = Ui.text(a, "" + day, 12, alpha > 120 ? Ui.onAccent : Ui.ink, v > 0); cell.setGravity(Gravity.CENTER);
     if (v > 0) cell.setBackground(Ui.shape((alpha << 24) | (Ui.accent & 0xFFFFFF), 10)); day++; }
    gRow.addView(cell, Ui.weight(1)); cell.getLayoutParams().height = Ui.dp(34); }
   hc.addView(gRow); }
  if (maxDay > 0) hc.addView(Ui.text(a, "Darker days saw more spending. Deepest: " + Fmt.money(maxDay) + " in one day.", 12.5f, Ui.muted, false), Ui.lpm(-2, -2, 0, 8, 0, 0));
  c.addView(hc);

  // cash vs digital split
  Map<Long, Store.Acc> accMap = Kit.accMap(db); long cashOut = 0, digOut = 0;
  for (Store.Tx t : month) if (t.kind.equals("expense")) { Store.Acc ac = accMap.get(t.account); if (ac != null && ac.kind.equals("cash")) cashOut += t.amount; else digOut += t.amount; }
  if (out > 0) { c.addView(Kit.sectionHead(a, "Cash or digital", null, null)); LinearLayout kc = Ui.card(a);
   LinearLayout kr = Ui.row(a); kr.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
   LinearLayout k1 = Ui.col(a); k1.addView(Ui.text(a, "Cash", 13, Ui.muted, true)); k1.addView(Ui.big(a, Fmt.money(cashOut), 20, Ui.ink)); kr.addView(k1, Ui.weight(1));
   LinearLayout k2 = Ui.col(a); k2.addView(Ui.text(a, "Digital (bank, card, wallet)", 13, Ui.muted, true)); k2.addView(Ui.big(a, Fmt.money(digOut), 20, Ui.ink)); kr.addView(k2, Ui.weight(1)); kc.addView(kr);
   kc.addView(new Charts.Pace(a, out > 0 ? cashOut / (float) out : 0, -1, Ui.accent, Ui.surface2), Ui.lpm(-1, Ui.dp(16), 0, 4, 0, 0));
   kc.addView(Ui.text(a, "Read from the account each entry uses - UPI itself is not tracked.", 12, Ui.muted, false), Ui.lpm(-2, -2, 0, 8, 0, 0));
   c.addView(kc); }

  // frequent
  List<Map.Entry<String, Long>> ns = new ArrayList<>(byNote.entrySet()); ns.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
  if (!ns.isEmpty()) { c.addView(Kit.sectionHead(a, "Top notes", null, null)); LinearLayout nc = Ui.card(a); for (int i = 0; i < Math.min(4, ns.size()); i++) { if (i > 0) nc.addView(Ui.divider(a)); LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(11), 0, Ui.dp(11)); row.addView(Ui.text(a, ns.get(i).getKey(), 15, Ui.ink, true), Ui.weight(1)); row.addView(Ui.text(a, Fmt.money(ns.get(i).getValue()), 14, Ui.muted, true)); nc.addView(row); } c.addView(nc); }
  Ui.enter(c); return sv[0];
 }
 static View dot(MainActivity a, int color) { View v = new View(a); v.setBackground(Ui.shape(color, 5)); return v; }

 /** Plain-text month recap for sharing anywhere. Amounts use the user's currency. */
 static String summary(Store db, YearMonth ym) {
  long in = 0, out = 0; Map<Long, Long> byCat = new HashMap<>();
  for (Store.Tx t : db.txs(Fmt.from(ym), Fmt.to(ym))) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) { out += t.amount; byCat.merge(t.cat, t.amount, Long::sum); } }
  Map<Long, Store.Cat> cats = db.categoryMap();
  StringBuilder b = new StringBuilder();
  b.append("Hisab · ").append(Fmt.month(ym)).append('\n');
  b.append("Income ").append(Fmt.money(in)).append(" · Spent ").append(Fmt.money(out)).append(" · Kept ").append(Fmt.money(in - out));
  List<Map.Entry<Long, Long>> es = new ArrayList<>(byCat.entrySet()); es.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
  if (!es.isEmpty()) {
   b.append("\nTop: ");
   for (int i = 0; i < Math.min(3, es.size()); i++) {
    Store.Cat ct = cats.get(es.get(i).getKey());
    if (i > 0) b.append(", ");
    b.append(ct == null ? "Other" : ct.name).append(' ').append(Fmt.money(es.get(i).getValue()));
   }
  }
  return b.toString();
 }
}
