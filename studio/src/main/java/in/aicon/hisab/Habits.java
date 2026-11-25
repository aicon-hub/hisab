package in.aicon.hisab;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Entry streaks and the monthly savings-rate goal. */
final class Habits {
 private Habits() {}

 /** Days in a row with at least one entry, counting back from today (or from yesterday if today has none yet). */
 static int streak(Store db, LocalDate today) {
  Set<String> days = new HashSet<>();
  for (Store.Tx t : db.txs(today.minusDays(400).toString(), today.toString())) days.add(t.day);
  LocalDate d = days.contains(today.toString()) ? today : today.minusDays(1);
  int n = 0;
  while (days.contains(d.toString())) { n++; d = d.minusDays(1); }
  return n;
 }

 /** Target share of income to keep each month, in whole percent; 0 when off. */
 static int goalPct(Store db) { try { return Math.max(0, Math.min(100, Integer.parseInt(db.pref("saveGoalPct", "0")))); } catch (Exception e) { return 0; } }

 /** Share of this month's income kept so far, in whole percent (may be negative); null with no income yet. */
 static Integer savedPct(Store db, LocalDate day) {
  java.time.YearMonth ym = java.time.YearMonth.from(day); long inc = 0, out = 0;
  for (Store.Tx t : db.txs(ym.atDay(1).toString(), ym.atEndOfMonth().toString())) { if (t.kind.equals("income")) inc += t.amount; else if (t.kind.equals("expense")) out += t.amount; }
  if (inc <= 0) return null;
  return (int) Math.round((inc - out) * 100.0 / inc);
 }

 /** One line for Home, or null when the goal is off or there is no income. */
 static String goalLine(Store db, LocalDate day) {
  int g = goalPct(db); if (g <= 0) return null;
  Integer s = savedPct(db, day); if (s == null) return null;
  return "Saved " + s + "% of income so far, goal " + g + "%" + (s >= g ? " - on track" : "");
 }
}
