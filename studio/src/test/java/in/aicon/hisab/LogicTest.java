package in.aicon.hisab;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LogicTest {
 @Test public void expressionMath() { assertEquals(14450, EntrySheet.eval("120+30-5.5")); assertEquals(4000, EntrySheet.eval("40")); assertEquals(-1, EntrySheet.eval("")); assertEquals(1050, EntrySheet.eval("10.5")); }
 @Test public void formatting() { Fmt.grouping = "Indian"; Fmt.symbol = "₹"; assertEquals("₹12,34,567", Fmt.money(123456700)); assertEquals("₹1,219.37", Fmt.money(121937)); Fmt.grouping = "International"; assertEquals("₹1,234,567", Fmt.money(123456700)); Fmt.grouping = "Indian"; }
 @Test public void ledgerMath() {
  Store db = new Store(RuntimeEnvironment.getApplication()); db.seedBasics(); List<Store.Acc> a = db.accounts(); Store.Acc cash = a.get(0), bank = a.get(1); cash.opening = 100000; db.updateAccount(cash);
  Store.Tx t = HomePage.parse(db, "tea 40 yesterday"); assertNotNull(t); assertEquals(4000, t.amount); assertEquals("expense", t.kind); assertEquals("Tea", t.note); db.save(t);
  assertEquals(100000 - 4000, db.balance(db.account(cash.id)) + db.balance(db.account(bank.id)));
  Store.Tx tr = new Store.Tx(); tr.kind = "transfer"; tr.amount = 25000; tr.day = "2026-11-15"; tr.account = cash.id; tr.dest = bank.id; db.save(tr);
  long sum = db.balance(db.account(cash.id)) + db.balance(db.account(bank.id)); assertEquals(100000 - 4000, sum);
  long p = db.addPerson("Anu", ""); Store.Tx l = new Store.Tx(); l.kind = "lent"; l.amount = 50000; l.day = "2026-11-15"; l.account = cash.id; l.person = p; db.save(l); assertEquals(50000, db.net(p));
  Store.Tx r = new Store.Tx(); r.kind = "repay_in"; r.amount = 20000; r.day = "2026-11-15"; r.account = cash.id; r.person = p; db.save(r); assertEquals(30000, db.net(p));
  assertNull(HomePage.parse(db, "just words"));
  Store.Rec rec = new Store.Rec(); rec.title = "Rent"; rec.kind = "expense"; rec.amount = 100000; rec.account = bank.id; rec.period = "monthly"; rec.next = "2026-01-31"; rec.active = true; rec.id = db.saveRec(rec); db.confirm(rec); assertEquals("2026-02-28", rec.next);
 }
 @Test public void sampleAndUi() {
  MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get(); assertTrue(a.db.hasData()); a.showTab(1, false); a.showTab(3, false); a.showTab(4, false); a.showTab(0, false); a.openEntry("transfer", null); a.openEntry("borrowed", null); Settings.open(a); Pages.recurring(a); Pages.categories(a); Pages.accounts(a); Pages.split(a); a.closeAll();
  a.db.wipe(); a.db.seedBasics(); a.refresh(); a.showTab(3, false); a.showTab(4, false); a.showTab(1, false); a.showTab(0, false);
 }
}
