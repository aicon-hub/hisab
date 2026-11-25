package in.aicon.hisab;

import android.content.Context;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.YearMonth;
import java.util.*;
import static org.junit.Assert.*;

/** 0.9.0 production features: PIN lock, CSV round-trip, statements, repeats, summaries, customisation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ProTest {
 Store fresh() {
  Store db = new Store(RuntimeEnvironment.getApplication());
  db.wipe(); db.seedBasics();
  db.setPref("pinHash", ""); db.setPref("pinSalt", "");
  for (String[] s : HomeCustom.SECTIONS) db.setPref("home:" + s[0], "1");
  return db;
 }

 @Test public void pinLock() {
  Store db = fresh();
  assertFalse(Lock.enabled(db));
  assertFalse(Lock.valid("")); assertFalse(Lock.valid("12")); assertFalse(Lock.valid("123456789")); assertFalse(Lock.valid("12a4"));
  assertTrue(Lock.valid("4712")); assertTrue(Lock.valid("12345678"));
  Lock.set(db, "4712");
  assertTrue(Lock.enabled(db));
  assertTrue(Lock.verify(db, "4712"));
  assertFalse(Lock.verify(db, "4713"));
  assertFalse(Lock.verify(db, ""));
  // hash, never the pin
  assertFalse(db.pref("pinHash", "").contains("4712"));
  // two different salts hash differently
  String h1 = Lock.hash("aa", "4712"), h2 = Lock.hash("bb", "4712");
  assertNotEquals(h1, h2); assertEquals(64, h1.length());
  Lock.clear(db);
  assertFalse(Lock.enabled(db));
  assertFalse(Lock.verify(db, "4712"));
 }

 @Test public void csvRoundTrip() {
  Store db = fresh();
  List<Store.Acc> as = db.accounts(); Store.Acc cash = as.get(0), bank = as.get(1);
  cash.opening = 100000; db.updateAccount(cash);
  Store.Tx t1 = new Store.Tx(); t1.kind = "expense"; t1.amount = 4550; t1.day = "2026-11-20"; t1.account = cash.id; t1.note = "Tea, \"special\" one"; db.save(t1);
  Store.Tx t2 = new Store.Tx(); t2.kind = "income"; t2.amount = 2500000; t2.day = "2026-11-01"; t2.account = bank.id; db.save(t2);
  Store.Tx t3 = new Store.Tx(); t3.kind = "transfer"; t3.amount = 50000; t3.day = "2026-11-10"; t3.account = bank.id; t3.dest = cash.id; db.save(t3);
  long pid = db.addPerson("Anu", "+91 97467 07833");
  Store.Tx t4 = new Store.Tx(); t4.kind = "lent"; t4.amount = 100000; t4.day = "2026-11-12"; t4.account = cash.id; t4.person = pid; t4.note = "for books"; db.save(t4);
  Map<String, Long> before = new HashMap<>(); for (Store.Acc x : db.accounts()) before.put(x.name, db.balance(x));
  int beforeCount = db.txs("0000-01-01", "9999-12-31").size();

  String csv = CsvIo.export(db);
  assertTrue(csv.startsWith("Date,Type,Amount,Account,To account,Category,Person,Note\n"));
  assertTrue(csv.contains("\"Tea, \"\"special\"\" one\""));

  db.wipe(); db.seedBasics();
  int[] r = CsvIo.importEntries(db, csv);
  assertEquals(4, r[0]); assertEquals(0, r[1]);
  assertEquals(beforeCount, db.txs("0000-01-01", "9999-12-31").size());
  // CSV carries entries, not starting balances: restore the opening before comparing
  for (Store.Acc x : db.accounts()) if (x.name.equals("Cash")) { x.opening = 100000; db.updateAccount(x); }
  for (Store.Acc x : db.accounts()) assertEquals(x.name, before.get(x.name), (Long) db.balance(x));
  assertEquals(100000, db.net(db.people().get(0).id));

  // re-importing the same file adds nothing
  int[] again = CsvIo.importEntries(db, csv);
  assertEquals(0, again[0]); assertEquals(4, again[1]);

  // bad rows are skipped, good ones kept
  int[] mixed = CsvIo.importEntries(db, "Date,Type,Amount,Account,To account,Category,Person,Note\n"
   + "2026-11-21,expense,99,Cash,,Food,,ticket\n"
   + "not-a-date,expense,50,Cash,,Food,,x\n"
   + "2026-11-21,lottery,50,Cash,,Food,,x\n"
   + "2026-11-21,transfer,50,Cash,,,,x\n");
  assertEquals(1, mixed[0]); assertEquals(3, mixed[1]);
 }

 @Test public void csvParserEdgeCases() {
  List<String[]> rows = CsvIo.parse("a,\"b,c\",\"d\"\"e\"\r\nf,g,\n\n\"multi\nline\",x,");
  assertEquals(3, rows.size());
  assertArrayEquals(new String[]{"a", "b,c", "d\"e"}, rows.get(0));
  assertArrayEquals(new String[]{"f", "g", ""}, rows.get(1));
  assertEquals("multi\nline", rows.get(2)[0]);
  assertEquals(0, CsvIo.parse("").size());
 }

 @Test public void statementData() {
  Store db = fresh();
  List<Store.Acc> as = db.accounts();
  Store.Tx i = new Store.Tx(); i.kind = "income"; i.amount = 300000; i.day = "2026-11-05"; i.account = as.get(1).id; db.save(i);
  Store.Tx e = new Store.Tx(); e.kind = "expense"; e.amount = 7500; e.day = "2026-11-06"; e.account = as.get(0).id; e.note = "Metro card"; db.save(e);
  Store.Tx old = new Store.Tx(); old.kind = "expense"; old.amount = 100; old.day = "2026-10-31"; old.account = as.get(0).id; db.save(old);
  Statement.Data d = Statement.collect(db, YearMonth.of(2026, 11));
  assertEquals(300000, d.in); assertEquals(7500, d.out); assertEquals(292500, d.net());
  assertEquals(2, d.rows.size());
  assertEquals("Metro card", d.rows.get(0)[1]);
  assertEquals("-₹75", d.rows.get(0)[3]);
 }

 @Test public void recurringPeriods() {
  assertEquals("2026-02-07", Store.advance("2026-01-31", "weekly"));
  assertEquals("2026-02-28", Store.advance("2026-01-31", "monthly"));
  assertEquals("2027-01-31", Store.advance("2026-01-31", "yearly"));
  assertEquals("2025-03-01", Store.advance("2024-03-01", "yearly"));
  Store db = fresh();
  Store.Rec r = new Store.Rec(); r.title = "Gym"; r.kind = "expense"; r.amount = 50000; r.account = db.accounts().get(0).id; r.period = "weekly"; r.next = "2026-11-24"; r.active = true;
  r.id = db.saveRec(r);
  r.active = false; db.saveRec(r);
  Store.Rec loaded = null; for (Store.Rec x : db.recurring()) if (x.id == r.id) loaded = x;
  assertNotNull(loaded); assertFalse(loaded.active); assertEquals("weekly", loaded.period);
  db.confirm(loaded);
  assertEquals("2026-12-01", loaded.next);
  List<Store.Tx> day = db.txs("2026-11-24", "2026-11-24");
  assertEquals(1, day.size()); assertEquals(50000, day.get(0).amount); assertEquals("Gym", day.get(0).note);
 }

 @Test public void monthSummary() {
  Store db = fresh();
  Store.Tx i = new Store.Tx(); i.kind = "income"; i.amount = 100000; i.day = "2026-11-03"; i.account = db.accounts().get(1).id; db.save(i);
  Store.Tx e = new Store.Tx(); e.kind = "expense"; e.amount = 4000; e.day = "2026-11-04"; e.account = db.accounts().get(0).id; for (Store.Cat c : db.categories("expense")) if (c.name.equals("Food")) e.cat = c.id; db.save(e);
  String s = InsightsPage.summary(db, YearMonth.of(2026, 11));
  assertTrue(s.contains("Hisab · November 2026"));
  assertTrue(s.contains("Income ₹1,000")); assertTrue(s.contains("Spent ₹40")); assertTrue(s.contains("Kept ₹960"));
  assertTrue(s.contains("Food ₹40"));
 }

 @Test public void dateStyles() {
  String old = Fmt.dateStyle;
  Fmt.dateStyle = "DMY"; assertEquals("5 Nov", Fmt.shortDate("2026-11-05"));
  Fmt.dateStyle = "MDY"; assertEquals("Nov 5", Fmt.shortDate("2026-11-05"));
  Fmt.dateStyle = "NUM"; assertEquals("05.11.2026", Fmt.shortDate("2026-11-05"));
  Fmt.dateStyle = old;
 }

 @Test public void themeScaleAndCustomAccent() {
  Context c = RuntimeEnvironment.getApplication();
  Ui.theme(c, false, false, "Lime", "Inter", 1.14f, 0xFFC7F23A);
  TextView t = Ui.text(c, "x", 10, 0, false);
  assertEquals(11.4f, t.getTextSize() / c.getResources().getDisplayMetrics().density, 0.05f);
  Ui.theme(c, false, false, "Custom", "Inter", 1f, 0xFF112233);
  assertEquals(0xFF112233, Ui.accent);
  Ui.theme(c, false, false, "Lime", "Inter");
  assertEquals(0xFFC7F23A, Ui.accent);
  TextView t2 = Ui.text(c, "x", 10, 0, false);
  assertEquals(10f, t2.getTextSize() / c.getResources().getDisplayMetrics().density, 0.05f);
 }

 @Test public void homeSectionFlags() {
  Store db = fresh();
  for (String[] s : HomeCustom.SECTIONS) assertTrue(s[0], HomeCustom.on(db, s[0]));
  db.setPref("home:recent", "0");
  assertFalse(HomeCustom.on(db, "recent"));
  assertTrue(HomeCustom.on(db, "hero"));
  db.setPref("home:recent", "1");
 }

 @Test public void hexColours() {
  assertEquals(0xFF112233, MainActivity.parseHex("#112233"));
  assertEquals(0xFF112233, MainActivity.parseHex("112233"));
  assertEquals(0xFFC7F23A, MainActivity.parseHex("junk"));
  assertEquals("#112233", MainActivity.hexOf(0xFF112233));
 }

 @Test public void smokeAllPages() {
  MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
  Settings.open(a); Privacy.open(a); HomeCustom.open(a); Pages.recurring(a); Pages.goals(a);
  a.showTab(1, false); a.showTab(3, false); a.closeAll();
  // locked flow: set a pin, restart the activity, gate must appear and open
  Lock.set(a.db, "4712"); Lock.unlocked = false;
  MainActivity locked = Robolectric.buildActivity(MainActivity.class).setup().get();
  assertNotNull(locked.lockLayer);
  assertNull(locked.lockLayer.findViewWithTag("pinField"));
  assertEquals(0, countEdits(locked.lockLayer));
  for (String k : new String[]{"4", "0", "0", "0"}) locked.lockLayer.findViewWithTag("pinKey" + k).performClick();
  locked.lockLayer.findViewWithTag("pinKeyOk").performClick();
  assertNotNull(locked.lockLayer);
  for (String k : new String[]{"4", "7", "1", "2"}) locked.lockLayer.findViewWithTag("pinKey" + k).performClick();
  assertNull(locked.lockLayer); assertTrue(Lock.unlocked);
  Lock.clear(locked.db);
 }

 static int countEdits(android.view.View v) {
  if (v instanceof android.widget.EditText) return 1;
  int n = 0;
  if (v instanceof android.view.ViewGroup) { android.view.ViewGroup g = (android.view.ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) n += countEdits(g.getChildAt(i)); }
  return n;
 }

 @Test public void csvImport() throws Exception {
  Store db = fresh();
  String csv = "Date,Kind,Amount,Account,To,Category,Person,Note\n"
   + "2026-06-03,expense,450,Cash,,Groceries,,synthetic row\n"
   + "2026-06-15,income,25000,Bank,,Salary,,synthetic row\n"
   + "2026-07-01,expense,1200,Cash,,Transport,,synthetic row\n"
   + "2026-07-20,transfer,5000,Bank,Cash,,,synthetic row\n"
   + "2026-08-11,lent,800,Cash,,,Test Person,synthetic row\n";
  int[] r = CsvIo.importEntries(db, csv);
  assertEquals(5, r[0]); assertEquals(0, r[1]);
  long jun = 0;
  for (Store.Tx t : db.txs("2026-06-01", "2026-06-30")) jun += t.amount;
  assertEquals(2545000, jun);
  for (Store.Tx t : db.txs("2026-06-01", "2026-08-31")) assertTrue(t.account > 0);
  // re-import is a no-op
  int[] again = CsvIo.importEntries(db, csv);
  assertEquals(0, again[0]); assertEquals(5, again[1]);
 }
}

