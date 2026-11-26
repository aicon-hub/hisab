package in.aicon.hisab;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 35)
public class ChatBrainTest {
 MainActivity a; Store db; LocalDate today = LocalDate.now(); long food, travel;
 @Before public void start() {
  a = Robolectric.buildActivity(MainActivity.class, new android.content.Intent().putExtra("noLaunch", true)).setup().get(); db = a.db; db.wipe(); db.seedBasics();
  for (Store.Cat c : db.categories("expense")) { if (c.name.equals("Food")) food = c.id; if (c.name.equals("Transport")) travel = c.id; }
  assertTrue(food > 0); assertTrue(travel > 0);
  add("expense", 20000, today, food, "Tea"); add("expense", 45000, today, food, "Lunch"); add("expense", 6000, today, travel, "Auto"); add("income", 500000, today, 0, "Salary");
  long p = db.addPerson("Ravi", ""); Store.Tx t = new Store.Tx(); t.kind = "lent"; t.amount = 150000; t.day = today.toString(); t.account = db.accounts().get(0).id; t.person = p; t.note = "Lent"; db.save(t);
 }
 void add(String kind, long amt, LocalDate d, long cat, String note) { Store.Tx t = new Store.Tx(); t.kind = kind; t.amount = amt; t.day = d.toString(); t.account = db.accounts().get(0).id; t.cat = cat; t.note = note; db.save(t); }
 String q(String s) { String t = ChatBrain.ask(db, s, today).text; System.out.println("Q[" + s + "] => " + t.replace("\n", " | ")); return t; }

 @Test public void spendingAnswers() {
  assertTrue(q("How much did I spend this month?"), q("How much did I spend this month?").contains(Fmt.money(71000)));
  assertTrue(q("food spend").contains(Fmt.money(65000)));
  assertTrue(q("how much on food today?").contains(Fmt.money(65000)));
  assertTrue(q("tea").contains(Fmt.money(20000)));
  assertTrue(q("what is left this month").contains(Fmt.money(500000 - 71000)));
  assertTrue(q("top categories").contains("Food"));
  assertTrue(q("biggest expense").contains("Lunch"));
  assertTrue(q("last 5 entries").contains("Auto"));
  assertTrue(q("how many entries this month").contains("5"));
 }
 @Test public void manglishAndMalayalam() {
  assertTrue(q("ee maasam ethra chilavu").contains(Fmt.money(71000)));
  assertTrue(q("ഈ മാസം എത്ര ചെലവായി?").contains(Fmt.money(71000)));
  assertTrue(q("ഇന്ന് ഭക്ഷണത്തിന് എത്ര").contains(Fmt.money(65000)));
  assertTrue(q("innu food ethra").contains(Fmt.money(65000)));
  assertTrue(q("ആര് തരാനുണ്ട്").contains("Ravi"));
  assertTrue(q("ravi kadam undo").contains(Fmt.money(150000)));
  assertTrue(q("ee maasam baki ethra").contains(Fmt.money(429000)));
 }
 @Test public void owe() { assertTrue(q("who owes me?").contains("Ravi")); assertTrue(q("does Ravi owe me anything").contains(Fmt.money(150000))); }
 @Test public void addNeedsConfirmAndDoesNotWriteYet() {
  int before = db.recent(50).size();
  ChatBrain.Reply r = ChatBrain.ask(db, "add auto 60 yesterday", today);
  assertEquals("add", r.act); assertEquals(6000, r.tx.amount); assertEquals(today.minusDays(1).toString(), r.tx.day);
  assertEquals(before, db.recent(50).size());
  r = ChatBrain.ask(db, "chaya 20", today); assertEquals("add", r.act);
  r = ChatBrain.ask(db, "ശമ്പളം 5000", today); assertEquals("add", r.act); assertEquals("income", r.tx.kind);
 }
 @Test public void deleteAndEditLast() {
  ChatBrain.Reply r = ChatBrain.ask(db, "delete last entry", today); assertEquals("delete", r.act); assertNotNull(r.tx);
  r = ChatBrain.ask(db, "change last to 500", today); assertEquals("edit", r.act); assertEquals(50000, r.tx.amount);
  r = ChatBrain.ask(db, "അവസാനത്തേത് ഒഴിവാക്കൂ", today); assertEquals("delete", r.act);
 }
 @Test public void unknownGivesHelp() { assertTrue(q("blah blah").contains("You can ask")); assertTrue(q("help").contains("Delete: ")); }
 @Test public void chatPageBuildsAndConfirms() {
  ChatPage.LOG.clear(); ChatPage.open(a);
  android.widget.EditText in = a.root.findViewWithTag("chatInput"); assertNotNull(in);
  ChatBrain.Reply r = ChatBrain.ask(db, "tea 30", today); ChatPage.pending = r; assertEquals("add", r.act);
 }
}
