package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

/** Year in review: one card with the year's totals, top categories and biggest day, ready to look back on. */
final class YearReview {
 private YearReview() {}

 /** {income, spent} for the year. */
 static long[] totals(Store db, int year) {
  long in = 0, out = 0;
  for (Store.Tx t : db.txs(year + "-01-01", year + "-12-31")) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) out += t.amount; }
  return new long[]{in, out};
 }

 /** Top spending categories of the year as {catId, amount}, most first, at most five. */
 static List<long[]> topCategories(Store db, int year) {
  Map<Long, Long> byCat = new HashMap<>();
  for (Store.Tx t : db.txs(year + "-01-01", year + "-12-31")) if (t.kind.equals("expense")) byCat.merge(t.cat, t.amount, Long::sum);
  List<Map.Entry<Long, Long>> es = new ArrayList<>(byCat.entrySet()); es.sort((x, z) -> Long.compare(z.getValue(), x.getValue()));
  List<long[]> out = new ArrayList<>();
  for (int i = 0; i < Math.min(5, es.size()); i++) out.add(new long[]{es.get(i).getKey(), es.get(i).getValue()});
  return out;
 }

 /** The single day with the most spending this year, or null when nothing was spent. */
 static String[] biggestDay(Store db, int year) {
  Map<String, Long> byDay = new HashMap<>();
  for (Store.Tx t : db.txs(year + "-01-01", year + "-12-31")) if (t.kind.equals("expense")) byDay.merge(t.day, t.amount, Long::sum);
  String best = null; long sum = 0;
  for (Map.Entry<String, Long> e : byDay.entrySet()) if (e.getValue() > sum || (e.getValue() == sum && best != null && e.getKey().compareTo(best) < 0)) { best = e.getKey(); sum = e.getValue(); }
  return best == null ? null : new String[]{best, "" + sum};
 }

 /** Days of the year with at least one entry, for the "kept notes on N days" line. */
 static int activeDays(Store db, int year) {
  Set<String> days = new HashSet<>();
  for (Store.Tx t : db.txs(year + "-01-01", year + "-12-31")) days.add(t.day);
  return days.size();
 }

 static void open(MainActivity a) {
  Store db = a.db; int y = Year.now().getValue();
  Kit.page(a, "Year in review", null, null, c -> {
   long[] tt = totals(db, y); long in = tt[0], out = tt[1];

   LinearLayout hero = Ui.card(a); hero.setGravity(Gravity.CENTER_HORIZONTAL);
   hero.addView(Ui.badge(a, "spark", 64, Ui.accent, Ui.onAccent), Ui.lpm(Ui.dp(64), Ui.dp(64), 0, 8, 0, 0));
   TextView yv = Ui.big(a, "" + y, 34, Ui.ink); yv.setPadding(0, Ui.dp(14), 0, Ui.dp(2)); hero.addView(yv);
   hero.addView(Ui.text(a, "Your year in money", 13.5f, Ui.muted, false), Ui.lpm(-2, -2, 0, 0, 0, 8));
   LinearLayout sr = Ui.row(a); sr.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
   sr.addView(YearReport.ycell(a, "Income", Fmt.money(in), Ui.pos), Ui.weight(1));
   sr.addView(YearReport.ycell(a, "Spent", Fmt.money(out), Ui.ink), Ui.weight(1));
   sr.addView(YearReport.ycell(a, "Kept", Fmt.money(in - out), in - out < 0 ? Ui.neg : Ui.ink), Ui.weight(1));
   hero.addView(sr, Ui.lpm(-1, -2, 0, 6, 0, 0));
   c.addView(hero, Ui.lpm(-1, -2, 0, 4, 0, 12));

   if (out == 0 && in == 0) {
    LinearLayout none = Ui.card(a); none.addView(Kit.empty(a, "chart", "Nothing to review yet", "Entries you add this year will build this card."));
    c.addView(none); return;
   }

   String[] big = biggestDay(db, y);
   LinearLayout facts = Ui.card(a);
   facts.addView(Ui.text(a, "Notes kept on " + activeDays(db, y) + " days of " + y, 14.5f, Ui.ink, true), Ui.lpm(-2, -2, 0, 8, 0, 0));
   if (big != null) {
    facts.addView(Ui.divider(a));
    LinearLayout bd = Ui.row(a); bd.setPadding(0, Ui.dp(12), 0, Ui.dp(10));
    bd.addView(Ui.badge(a, "flame", 38, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(38), Ui.dp(38), 0, 0, 14, 0));
    LinearLayout m = Ui.col(a);
    m.addView(Ui.text(a, "Biggest day: " + Fmt.dayLabel(big[0]), 14.5f, Ui.ink, true));
    TextView s = Ui.text(a, Fmt.money(Long.parseLong(big[1])) + " spent that day", 12.5f, Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s);
    bd.addView(m, Ui.weight(1));
    facts.addView(bd);
   }
   c.addView(facts, Ui.lpm(-1, -2, 0, 0, 0, 12));

   List<long[]> top = topCategories(db, y);
   if (!top.isEmpty()) {
    c.addView(Kit.sectionHead(a, "Where it went in " + y, null, null));
    LinearLayout tc = Ui.card(a); Map<Long, Store.Cat> cats = db.categoryMap();
    for (int i = 0; i < top.size(); i++) {
     Store.Cat ct = cats.get(top.get(i)[0]);
     LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(9), 0, Ui.dp(9));
     row.addView(Ui.badge(a, ct == null ? "other" : ct.icon, 34, Kit.tone(top.get(i)[0]), 0xFF14151A), Ui.lpm(Ui.dp(34), Ui.dp(34), 0, 0, 12, 0));
     row.addView(Ui.text(a, ct == null ? "Other" : ct.name, 14.5f, Ui.ink, true), Ui.weight(1));
     long share = out > 0 ? Math.round(100.0 * top.get(i)[1] / out) : 0;
     row.addView(Ui.text(a, Fmt.money(top.get(i)[1]) + " · " + share + "%", 13, Ui.muted, true));
     tc.addView(row);
     if (i < top.size() - 1) tc.addView(Ui.divider(a));
    }
    c.addView(tc);
   }
  });
 }
}
