package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;

/** In-app date picker: a proper month grid with arrows, replacing the plain system dialog. */
final class DateSheet {
 private DateSheet() {}
 interface Pick { void on(LocalDate d); }

 static void show(MainActivity a, LocalDate sel, Pick done) {
  LinearLayout c = Sheets.sheetBody(a, null);
  final YearMonth[] ym = {YearMonth.from(sel)}; final LocalDate[] cur = {sel};
  LinearLayout head = Ui.row(a);
  FrameLayout prev = Ui.badge(a, "left", 40, Ui.surface2, Ui.ink), next = Ui.badge(a, "right", 40, Ui.surface2, Ui.ink);
  TextView m = Ui.big(a, "", 19, Ui.ink); m.setGravity(Gravity.CENTER);
  head.addView(prev, Ui.lp(Ui.dp(40), Ui.dp(40))); head.addView(m, Ui.weight(1)); head.addView(next, Ui.lp(Ui.dp(40), Ui.dp(40)));
  c.addView(head, Ui.lpm(-1, -2, 0, 6, 0, 12));
  LinearLayout wk = Ui.row(a);
  for (String w : new String[]{"M", "T", "W", "T", "F", "S", "S"}) { TextView t = Ui.text(a, w, 11.5f, Ui.muted, false); t.setGravity(Gravity.CENTER); wk.addView(t, Ui.weight(1)); }
  c.addView(wk, Ui.lpm(-1, -2, 0, 0, 0, 4));
  LinearLayout grid = Ui.col(a); c.addView(grid);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  final Runnable[] build = new Runnable[1];
  build[0] = () -> {
   m.setText(cap(ym[0].getMonth().name()) + " " + ym[0].getYear());
   grid.removeAllViews();
   int first = ym[0].atDay(1).getDayOfWeek().getValue() - 1, days = ym[0].lengthOfMonth();
   final LocalDate today = LocalDate.now();
   for (int i = 0; i < first + days; i++) {
    LinearLayout r;
    if (i % 7 == 0) { r = Ui.row(a); grid.addView(r); } else r = (LinearLayout) grid.getChildAt(grid.getChildCount() - 1);
    if (i < first) { r.addView(Ui.text(a, "", 15f, Ui.ink, false), cellLp()); continue; }
    final LocalDate ld = ym[0].atDay(i - first + 1);
    boolean on = ld.equals(cur[0]), isToday = ld.equals(today);
    TextView t = Ui.text(a, "" + ld.getDayOfMonth(), 15f, on ? Ui.onAccent : isToday ? Ui.accent : Ui.ink, on || isToday);
    t.setGravity(Gravity.CENTER);
    if (on) t.setBackground(Ui.shape(Ui.accent, 22)); else if (isToday) t.setBackground(Ui.outline(0, Ui.accent, 22, 1.2f));
    Ui.tap(t, () -> { done.on(ld); a.close(ref[0]); });
    r.addView(t, cellLp());
   }
  };
  Ui.tap(prev, () -> { ym[0] = ym[0].minusMonths(1); build[0].run(); });
  Ui.tap(next, () -> { ym[0] = ym[0].plusMonths(1); build[0].run(); });
  TextView today = Ui.button(a, "Jump to today", false); c.addView(today, Ui.lpm(-1, -2, 0, 16, 0, 0));
  Ui.tap(today, () -> { done.on(LocalDate.now()); a.close(ref[0]); });
  ref[0] = Sheets.show(a, c);
  build[0].run();
 }

 private static LinearLayout.LayoutParams cellLp() { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(46), 1); lp.setMargins(Ui.dp(2), Ui.dp(2), Ui.dp(2), Ui.dp(2)); return lp; }
 private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(); }
}
