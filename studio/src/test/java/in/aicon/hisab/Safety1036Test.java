package in.aicon.hisab;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 35)
public class Safety1036Test {
 @Test public void monthlyKeepsTheDay() {
  assertEquals("2027-02-28", Store.advance("2027-01-31", "monthly", 31));
  assertEquals("2027-03-31", Store.advance("2027-02-28", "monthly", 31));
  assertEquals("2028-02-29", Store.advance("2027-02-28", "yearly", 29));
  assertEquals("2027-04-30", Store.advance("2027-03-31", "monthly", 31));
  assertEquals("2027-02-08", Store.advance("2027-02-01", "weekly", 1));
 }
 @Test public void scheduleWalksBackToThe31st() {
  Store db = new Store(RuntimeEnvironment.getApplication()); db.getWritableDatabase().delete("recur", null, null);
  Store.Rec r = new Store.Rec(); r.title = "Rent"; r.kind = "expense"; r.amount = 100; r.account = 1; r.cat = 1; r.period = "monthly"; r.next = "2027-01-31"; r.active = true; r.id = db.saveRec(r);
  String[] want = {"2027-02-28", "2027-03-31", "2027-04-30", "2027-05-31"};
  for (String w : want) { db.skip(r); assertEquals(w, r.next); }
  r.next = "2027-06-10"; db.skip(r); assertEquals("a hand-edited date restarts the anchor", "2027-07-10", r.next);
 }
 @Test public void pinIsSlowHashedAndLimited() {
  Store db = new Store(RuntimeEnvironment.getApplication()); Lock.clear(db); Lock.set(db, "4712");
  assertTrue(db.pref("pinHash", "").startsWith("p2:"));
  for (int i = 0; i < 4; i++) { assertFalse(Lock.verify(db, "0000")); assertEquals(0, Lock.lastWaitMs); }
  assertFalse(Lock.verify(db, "0000")); assertTrue(Lock.lastWaitMs >= 29000);
  assertFalse("right PIN is refused during the pause", Lock.verify(db, "4712")); assertTrue(Lock.lastWaitMs > 0);
  db.setPref("pinLockUntil", "0"); assertTrue(Lock.verify(db, "4712")); assertEquals("0", db.pref("pinFails", "x"));
  assertEquals(30000L, Lock.pauseFor(5)); assertEquals(60000L, Lock.pauseFor(6)); assertEquals(900000L, Lock.pauseFor(40));
 }
 @Test public void oldPinStillOpensAndIsUpgraded() {
  Store db = new Store(RuntimeEnvironment.getApplication()); Lock.clear(db);
  db.setPref("pinSalt", "ab12"); db.setPref("pinHash", Lock.legacyHash("ab12", "2468"));
  assertTrue(Lock.verify(db, "2468")); assertTrue(db.pref("pinHash", "").startsWith("p2:")); assertTrue(Lock.verify(db, "2468"));
 }
}
