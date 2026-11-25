package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

/** Year at a glance: twelve months of income and spending, the year's top categories, and how it compares with last year. */
final class YearReport {
 private YearReport() {}

 static void open(MainActivity a) {
  Kit.page(a, "Year report", null, null, c -> {
   Store db = a.db; int y = a.reportYear;
   LinearLayout nav = Ui.row(a); nav.setBackground(Ui.shape(Ui.surface, 26)); nav.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
   FrameLayout l = Ui.badge(a, "left", 38, Ui.surface2, Ui.ink), r = Ui.badge(a, "right", 38, Ui.surface2, Ui.ink);
   nav.addView(l, Ui.lp(Ui.dp(38), Ui.dp(38))); TextView yt = Ui.big(a, "" + y, 18, Ui.ink); yt.setGravity(Gravity.CENTER); nav.addView(yt, Ui.weight(1)); nav.addView(r, Ui.lp(Ui.dp(38), Ui.dp(38)));
   Ui.tap(l, () -> { a.reportYear = y - 1; a.closeAll(); open(a); }); Ui.tap(r, () -> { if (y < Year.now().getValue()) { a.reportYear = y + 1; a.closeAll(); open(a); } });
   c.addView(nav, Ui.lpm(-1, -2, 0, 8, 0, 12));

   long[] mi = new long[12], mo = new long[12]; Map<Long, Long> byCat = new HashMap<>();
   for (Store.Tx t : db.txs(y + "-01-01", y + "-12-31")) { int m = LocalDate.parse(t.day).getMonthValue() - 1; if (t.kind.equals("income")) mi[m] += t.amount; if (t.kind.equals("expense")) { mo[m] += t.amount; byCat.merge(t.cat, t.amount, Long::sum); } }
   long in = 0, out = 0; for (int i = 0; i < 12; i++) { in += mi[i]; out += mo[i]; }
   long pin = 0, pout = 0;
   for (Store.Tx t : db.txs((y - 1) + "-01-01", (y - 1) + "-12-31")) { if (t.kind.equals("income")) pin += t.amount; if (t.kind.equals("expense")) pout += t.amount; }

   LinearLayout sum = Ui.card(a); LinearLayout sr = Ui.row(a); sr.setPadding(0, Ui.dp(4), 0, 0);
   sr.addView(ycell(a, "Income", Fmt.money(in), Ui.pos), Ui.weight(1)); sr.addView(ycell(a, "Spent", Fmt.money(out), Ui.ink), Ui.weight(1)); sr.addView(ycell(a, "Kept", Fmt.money(in - out), in - out < 0 ? Ui.neg : Ui.ink), Ui.weight(1)); sum.addView(sr);
   if (pout > 0) { int d = (int) Math.round(100.0 * (out - pout) / pout); sum.addView(Ui.text(a, d == 0 ? "Same spending as " + (y - 1) : "Spending " + (d > 0 ? "up " + d + "%" : "down " + Math.abs(d) + "%") + " from " + (y - 1) + " (" + Fmt.money(pout) + ")", 13, Ui.muted, false), Ui.lpm(-2, -2, 0, 10, 0, 0)); }
   else sum.addView(Ui.text(a, "Nothing noted in " + (y - 1) + " to compare with.", 13, Ui.muted, false), Ui.lpm(-2, -2, 0, 10, 0, 0));
   c.addView(sum, Ui.lpm(-1, -2, 0, 4, 0, 12));

   c.addView(Kit.sectionHead(a, "Month by month", null, null));
   LinearLayout bars = Ui.card(a); long peak = 0; for (int i = 0; i < 12; i++) peak = Math.max(peak, Math.max(mi[i], mo[i]));
   if (peak == 0) bars.addView(Kit.empty(a, "chart", "No entries in " + y, "Nothing noted this year yet."));
   else for (int i = 0; i < 12; i++) {
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(5), 0, Ui.dp(5));
    TextView ml = Ui.text(a, Fmt.monthShort(YearMonth.of(y, i + 1)), 12.5f, Ui.muted, true); row.addView(ml, Ui.lpm(Ui.dp(42), -2, 0, 0, 8, 0));
    LinearLayout track = Ui.col(a);
    Charts.Pace ob = new Charts.Pace(a, mo[i] / (float) peak, -1, Ui.dark ? Ui.accent : Ui.ink, Ui.surface2); track.addView(ob, Ui.lpm(-1, Ui.dp(10), 0, 0, 0, 3));
    if (mi[i] > 0) { Charts.Pace ib = new Charts.Pace(a, mi[i] / (float) peak, -1, Ui.pos, Ui.surface2); track.addView(ib, Ui.lpm(-1, Ui.dp(10), 0, 3, 0, 0)); }
    row.addView(track, Ui.weight(1));
    row.addView(Ui.text(a, mo[i] > 0 ? Fmt.compact(mo[i]) : "-", 12, Ui.muted, true), Ui.lpm(-2, -2, 8, 0, 0, 0));
    bars.addView(row);
   }
   c.addView(bars, Ui.lpm(-1, -2, 0, 0, 0, 12));

   c.addView(Kit.sectionHead(a, "Top categories in " + y, null, null));
   LinearLayout tc = Ui.card(a); Map<Long, Store.Cat> cats = db.categoryMap();
   List<Map.Entry<Long, Long>> es = new ArrayList<>(byCat.entrySet()); es.sort((x, z) -> Long.compare(z.getValue(), x.getValue()));
   if (es.isEmpty()) tc.addView(Kit.empty(a, "chart", "No spending in " + y, "Nothing noted this year yet."));
   for (int i = 0; i < Math.min(5, es.size()); i++) { Store.Cat ct = cats.get(es.get(i).getKey());
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(9), 0, Ui.dp(9));
    row.addView(Ui.badge(a, ct == null ? "other" : ct.icon, 34, Kit.tone(es.get(i).getKey()), 0xFF14151A), Ui.lpm(Ui.dp(34), Ui.dp(34), 0, 0, 12, 0));
    row.addView(Ui.text(a, ct == null ? "Other" : ct.name, 14.5f, Ui.ink, true), Ui.weight(1));
    row.addView(Ui.text(a, Fmt.money(es.get(i).getValue()), 13, Ui.muted, true)); tc.addView(row);
    if (i < Math.min(5, es.size()) - 1) tc.addView(Ui.divider(a));
   }
   c.addView(tc);
  });
 }

 static View ycell(MainActivity a, String k, String v, int color) { LinearLayout l = Ui.col(a); l.addView(Ui.text(a, k, 12, Ui.muted, true)); TextView t = Ui.big(a, v, 17, color); t.setPadding(0, Ui.dp(4), 0, 0); l.addView(t); return l; }
}
