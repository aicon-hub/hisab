package in.aicon.hisab;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Where your income comes from: totals per source and per month over a chosen period. Local only. */
final class IncomeSources {
 private IncomeSources() {}

 static final String[] PERIODS = {"This month", "Last 3 months", "This year", "All time"};

 static final class Source { String name; long amount; }
 static final class Result { long total; final List<Source> sources = new ArrayList<>(); final TreeMap<String, Long> byMonth = new TreeMap<>(); }

 static String from(int period, LocalDate today) {
  switch (period) {
   case 0: return YearMonth.from(today).atDay(1).toString();
   case 1: return YearMonth.from(today).minusMonths(2).atDay(1).toString();
   case 2: return today.getYear() + "-01-01";
   default: return "0000-01-01";
  }
 }

 /** Income entries in the period, grouped by income category (the note when there is none), biggest first. */
 static Result collect(Store db, int period, LocalDate today) {
  Result r = new Result(); Map<Long, Store.Cat> cats = db.categoryMap(); Map<String, Long> by = new HashMap<>();
  for (Store.Tx t : db.txs(from(period, today), today.toString())) {
   if (!t.kind.equals("income")) continue;
   Store.Cat c = cats.get(t.cat);
   String name = c != null ? c.name : (t.note != null && !t.note.trim().isEmpty() ? t.note.trim() : "Other income");
   by.merge(name, t.amount, Long::sum); r.total += t.amount; r.byMonth.merge(t.day.substring(0, 7), t.amount, Long::sum);
  }
  for (Map.Entry<String, Long> e : by.entrySet()) { Source s = new Source(); s.name = e.getKey(); s.amount = e.getValue(); r.sources.add(s); }
  r.sources.sort((x, y) -> { int c = Long.compare(y.amount, x.amount); return c != 0 ? c : x.name.compareTo(y.name); });
  return r;
 }

 static int share(long part, long total) { return total <= 0 ? 0 : (int) Math.round(part * 100.0 / total); }

 static void open(MainActivity a) { open(a, 2); }

 static void open(MainActivity a, int period) {
  Store db = a.db;
  Kit.page(a, "Income sources", "calendar", () -> Sheets.pick(a, "Period", Arrays.asList(PERIODS), null, period, i -> { a.closeAll(); open(a, i); }), c -> {
   Result r = collect(db, period, LocalDate.now());
   LinearLayout hero = Ui.col(a); hero.setBackground(Ui.shape(Ui.accent, 30)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
   hero.addView(Ui.text(a, "Income, " + PERIODS[period].toLowerCase(Locale.ROOT), 13, Ui.onAccentSub, true));
   TextView big = Ui.big(a, Fmt.money(r.total), 36, Ui.onAccent); big.setPadding(0, Ui.dp(8), 0, Ui.dp(6)); hero.addView(big);
   hero.addView(Ui.text(a, r.sources.isEmpty() ? "No income recorded in this period." : r.sources.size() + (r.sources.size() == 1 ? " source" : " sources"), 13, Ui.onAccentSub, false));
   c.addView(hero);
   if (r.sources.isEmpty()) return;
   LinearLayout card = Ui.card(a);
   for (int i = 0; i < r.sources.size(); i++) { Source s = r.sources.get(i); if (i > 0) card.addView(Ui.divider(a));
    LinearLayout row = Ui.col(a); row.setPadding(0, Ui.dp(11), 0, Ui.dp(11));
    LinearLayout top = Ui.row(a); top.addView(Ui.text(a, s.name, 15, Ui.ink, true), Ui.weight(1)); top.addView(Ui.big(a, Fmt.money(s.amount), 16, Ui.ink)); row.addView(top);
    int pct = share(s.amount, r.total);
    LinearLayout track = Ui.row(a); track.setBackground(Ui.shape(Ui.surface2, 4));
    View fill = new View(a); fill.setBackground(Ui.shape(Ui.accent, 4)); track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(6), Math.max(1, pct)));
    track.addView(new View(a), new LinearLayout.LayoutParams(0, Ui.dp(6), Math.max(0, 100 - pct)));
    row.addView(track, Ui.lpm(-1, Ui.dp(6), 0, 8, 0, 0));
    row.addView(Ui.text(a, pct + "% of income", 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 4, 0, 0));
    card.addView(row);
   }
   c.addView(card, Ui.lpm(-1, -2, 0, 14, 0, 0));
   c.addView(Kit.sectionHead(a, "By month", null, null));
   LinearLayout mc = Ui.card(a);
   int n = 0; for (Map.Entry<String, Long> e : r.byMonth.descendingMap().entrySet()) { if (n++ > 0) mc.addView(Ui.divider(a));
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
    row.addView(Ui.text(a, Fmt.month(YearMonth.parse(e.getKey())), 14, Ui.ink, false), Ui.weight(1)); row.addView(Ui.big(a, Fmt.money(e.getValue()), 15, Ui.ink)); mc.addView(row);
   }
   c.addView(mc);
  });
 }
}
