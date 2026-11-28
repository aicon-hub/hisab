package in.aicon.hisab;

import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Stage 1 review items C2, C4, C5, C7, C8 and S5. Invented rows only. */
@RunWith(RobolectricTestRunner.class) @Config(sdk = 35)
public class Stage1Test {
 private Store fresh() { Store db = new Store(RuntimeEnvironment.getApplication()); db.wipe(); db.seedBasics(); return db; }
 private long add(Store db, String kind, long amount, String day) {
  Store.Tx t = new Store.Tx(); t.kind = kind; t.amount = amount; t.day = day; t.account = db.accounts().get(0).id; t.note = "x"; return db.save(t);
 }

 @Test public void splitPartsAlwaysAddUp() {
  long[][] cases = {{100, 3}, {101, 3}, {102, 3}, {1, 2}, {1, 5}, {999999999999L, 7}, {10000, 50}, {5, 2}};
  for (long[] c : cases) {
   long[] s = SplitMath.shares(c[0], (int) c[1]); long sum = 0, lo = Long.MAX_VALUE, hi = 0;
   for (long v : s) { sum += v; lo = Math.min(lo, v); hi = Math.max(hi, v); }
   assertEquals(c[0] + " over " + c[1], c[0], sum); assertTrue(hi - lo <= 1);
  }
  assertArrayEquals(new long[]{34, 34, 33}, SplitMath.shares(101, 3));
  assertArrayEquals(new long[]{100}, SplitMath.shares(100, 1));
 }

 @Test public void monthlyClampsAndComesBack() {
  assertEquals("2028-02-29", Store.advance("2028-01-31", "monthly", 31));
  assertEquals("2027-02-28", Store.advance("2027-01-29", "monthly", 29));
  assertEquals("2028-02-29", Store.advance("2028-01-29", "monthly", 29));
  assertEquals("2027-02-28", Store.advance("2027-01-30", "monthly", 30));
  assertEquals("2027-03-30", Store.advance("2027-02-28", "monthly", 30));
  assertEquals("2027-04-30", Store.advance("2027-03-30", "monthly", 30));
  assertEquals("2027-12-31", Store.advance("2027-11-30", "monthly", 31));
 }
 @Test public void yearlyFeb29SurvivesLeapCycle() {
  String d = "2028-02-29"; String[] want = {"2029-02-28", "2030-02-28", "2031-02-28", "2032-02-29"};
  for (String w : want) { d = Store.advance(d, "yearly", 29); assertEquals(w, d); }
 }

 @Test public void receiversAreNotExported() throws Exception {
  String m = new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")), "UTF-8");
  for (String name : new String[]{"AutoRemind", "Reminders"}) {
   int at = m.indexOf("in.aicon.hisab." + name + "\""); assertTrue(name, at > 0);
   String tag = m.substring(at, m.indexOf('>', at)); assertTrue(name + " " + tag, tag.contains("android:exported=\"false\""));
  }
 }

 @Test public void forecastEdges() {
  Store db = fresh(); LocalDate d1 = LocalDate.of(2027, 3, 1), mid = LocalDate.of(2027, 3, 10), last = LocalDate.of(2027, 3, 31);
  assertEquals("nothing spent", 0, Forecast.monthEnd(db, mid));
  add(db, "expense", 31000, "2027-03-01");
  assertEquals("first days are too early", 0, Forecast.monthEnd(db, d1));
  assertEquals("last day equals the spend", 31000, Forecast.monthEnd(db, last));
  assertEquals(96100, Forecast.monthEnd(db, mid));
 }
 @Test public void noBudgetMeansNoLimit() {
  Store db = fresh(); long cat = db.categories("expense").get(0).id;
  assertEquals(0, db.effectiveLimit(cat, YearMonth.of(2027, 3)));
  db.setBudget(cat, 500000); assertEquals(500000, db.effectiveLimit(cat, YearMonth.of(2027, 3)));
  db.setBudget(cat, 0); assertEquals(0, db.effectiveLimit(cat, YearMonth.of(2027, 3)));
 }

 @Test public void rateEditDoesNotChangeSavedEntries() {
  Store db = fresh(); long a = TravelFx.convert(1000, new BigDecimal("83.5")); assertEquals(83500, a);
  add(db, "expense", a, "2027-03-02"); long before = db.balance(db.accounts().get(0));
  assertEquals(90000, TravelFx.convert(1000, new BigDecimal("90")));
  assertEquals(before, db.balance(db.accounts().get(0)));
  long sum = 0; for (Store.Tx t : db.txs("2027-03-01", "2027-03-31")) sum += t.amount; assertEquals(83500, sum);
 }
 @Test public void roundUpAndGoalsNeverTouchTheLedger() {
  Store db = fresh(); add(db, "expense", 4230, "2027-03-02"); add(db, "expense", 1000, "2027-03-03");
  long bal = db.balance(db.accounts().get(0)); int n = db.txs("0000-01-01", "9999-12-31").size();
  db.setPref("roundUp", "10"); assertEquals(770, RoundUp.pot(db));
  Store.Goal g = new Store.Goal(); g.name = "Trip"; g.icon = "target"; g.target = 100000; g.saved = 5000; db.saveGoal(g);
  assertEquals(bal, db.balance(db.accounts().get(0))); assertEquals(n, db.txs("0000-01-01", "9999-12-31").size());
  db.setPref("roundUp", ""); assertEquals(0, RoundUp.pot(db)); assertEquals(bal, db.balance(db.accounts().get(0)));
 }
}
