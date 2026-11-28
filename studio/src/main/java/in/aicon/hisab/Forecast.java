package in.aicon.hisab;

import java.time.LocalDate;
import java.time.YearMonth;

/** Month-end forecast: where this month's spending lands at the current pace. */
final class Forecast {
 private Forecast() {}

 /** Projected month spend, or 0 when it is too early or nothing is spent yet. */
 static long monthEnd(Store db, LocalDate day) {
  YearMonth ym = YearMonth.from(day);
  long spent = 0;
  for (Store.Tx t : db.txs(ym.atDay(1).toString(), ym.atEndOfMonth().toString())) if (t.kind.equals("expense")) spent += t.amount;
  if (spent <= 0 || day.getDayOfMonth() < 3) return 0;
  return (spent * ym.lengthOfMonth() + day.getDayOfMonth() / 2) / day.getDayOfMonth();
 }
}
