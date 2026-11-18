package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class SmsTxTest {
 MainActivity a; long bankId; long now;
 @Before public void start(){
  a=Robolectric.buildActivity(MainActivity.class,new android.content.Intent().putExtra("noLaunch",true)).setup().get();
  a.db.wipe();a.db.seedBasics();a.db.setPref("sample","0");
  for(Store.Acc x:a.db.accounts())if(x.kind.equals("bank"))bankId=x.id;
  a.db.setPref("smsEnabled","1");a.db.setPref("smsTxEnabled","1");a.db.setPref("smsMap:HDFC:1234",""+bankId);
  now=System.currentTimeMillis();
 }
 List<Store.Tx> all(){return a.db.txs("0000-01-01","9999-12-31");}
 Store.Tx only(){List<Store.Tx> l=all();assertEquals(1,l.size());return l.get(0);}

 @Test public void phoneNormalisation(){
  assertEquals("+919876543210",Phone.normalise("9876543210"));
  assertEquals("+919876543210",Phone.normalise("09876543210"));
  assertEquals("+919876543210",Phone.normalise("919876543210"));
  assertEquals("+919876543210",Phone.normalise("+91 98765 43210"));
  assertEquals("919876543210",Phone.wa("98765 43210"));
  assertEquals("+15551234567",Phone.normalise("+1 555 123 4567"));
  assertEquals("12345",Phone.normalise("12345"));
  assertEquals("",Phone.normalise(null));
  assertEquals("",Phone.normalise("abc"));
 }
 @Test public void storedNumbersAreNormalisedAndMigrated(){
  long id=a.db.addPerson("Rahul","9876543210");
  for(Store.Person p:a.db.people())if(p.id==id)assertEquals("+919876543210",p.phone);
  a.db.setPref("phoneFixV1","0");
  a.db.normalisePhones();
  for(Store.Person p:a.db.people())if(p.id==id)assertEquals("+919876543210",p.phone);
 }

 @Test public void p2pDebitBecomesPeopleExpense(){
  assertTrue(SmsTx.apply(a.db,"VM-HDFCBK","A/c XX1234 debited INR 500.00 to SALMAN K on 18-11-26. UTR 123456789012. Avl Bal INR 9,500.00",now));
  Store.Tx t=only();assertEquals("expense",t.kind);assertEquals(50000,t.amount);assertEquals(bankId,t.account);
  assertEquals("People",a.db.categoryMap().get(t.cat).name);assertEquals("Salman K",t.note);
 }
 @Test public void vpaTransferIsPersonToPerson(){
  SmsTx.T t=SmsTx.parse("VM-HDFCBK","A/c XX1234 debited. Rs 250.00 paid to rahul.nair@okicici. Avl Bal Rs 9,000.00",null);
  assertNotNull(t);assertTrue(t.p2p);assertEquals("rahul.nair@okicici",t.who);assertEquals(25000,t.amount);
  SmsTx.T m=SmsTx.parse("VM-HDFCBK","A/c XX1234 debited Rs 899.00 VPA amazonpay@axisb for order",null);
  assertNotNull(m);assertFalse(m.p2p);
 }
 @Test public void merchantDebitIsKeywordCategorised(){
  assertTrue(SmsTx.apply(a.db,"VM-HDFCBK","A/c XX1234 spent Rs 500.00 at INDIAN OIL PETROL PUMP on 18-11-26. Avl Bal INR 8,500.00",now));
  Store.Tx t=only();assertEquals("Fuel",a.db.categoryMap().get(t.cat).name);
 }
 @Test public void amountComesFromDebitNotBalance(){
  SmsTx.T t=SmsTx.parse("VM-HDFCBK","A/c XX1234 debited INR 500.00 to SALMAN K. Avl Bal INR 9,500.00",null);
  assertNotNull(t);assertEquals(50000,t.amount);
  assertNull(SmsTx.parse("VM-HDFCBK","A/c XX1234 debited INR 500.00. Charges Rs 5.00. Avl Bal INR 9,495.00",null)); // two amounts: ambiguous
  assertNull(SmsTx.parse("VM-HDFCBK","A/c XX1234 available balance INR 9,500.00",null)); // balance only, no debit
 }
 @Test public void creditOtpAndFutureDebitRejected(){
  assertNull(SmsTx.parse("VM-HDFCBK","A/c XX1234 credited INR 5,000.00 salary",null));
  assertNull(SmsTx.parse("VM-HDFCBK","OTP 482910 for A/c XX1234 debited INR 500.00",null));
  assertNull(SmsTx.parse("VM-HDFCBK","A/c XX1234 will be debited INR 1,299.00 for e-mandate on 20-11-26",null));
  assertNull(SmsTx.parse("VM-HDFCBK","A/c XX1234 debited INR 500.00 failed due to network",null));
 }
 @Test public void tripleAlertsCollapseToOneEntry(){
  String[] alerts={
   "A/c XX1234 debited INR 500.00 to SALMAN K on 18-11-26. UTR 123456789012.",
   "UPI: A/c XX1234 paid Rs 500.00 to SALMAN K. Ref No 123456789012.",
   "INR 500.00 sent to SALMAN K from A/c XX1234. Avl Bal INR 9,000.00."};
  assertTrue(SmsTx.apply(a.db,"VM-HDFCBK",alerts[0],now));
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK",alerts[1],now+30000)); // same UTR
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK",alerts[2],now+60000)); // same account/day/amount/person
  assertEquals(1,all().size());
 }
 @Test public void rescanNeverDuplicatesAndDeletionSticks(){
  String msg="A/c XX1234 debited INR 500.00 to SALMAN K on 18-11-26. UTR 123456789012.";
  assertTrue(SmsTx.apply(a.db,"VM-HDFCBK",msg,now));
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK",msg,now)); // rescan of the same SMS
  long id=only().id;
  a.db.deleteTx(id);
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK",msg,now)); // deleted import is not re-added
  assertEquals(0,all().size());
 }
 @Test public void unlinkedOrOptedOutIsIgnored(){
  a.db.setPref("smsTxEnabled","0");
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK","A/c XX1234 debited INR 500.00 to SALMAN K. UTR 123456789012",now));
  a.db.setPref("smsTxEnabled","1");a.db.setPref("smsEnabled","0");
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK","A/c XX1234 debited INR 500.00 to SALMAN K. UTR 123456789012",now));
  a.db.setPref("smsEnabled","1");
  assertFalse(SmsTx.apply(a.db,"VM-HDFCBK","A/c XX9999 debited INR 500.00 to SALMAN K. UTR 123456789012",now)); // account not linked
  assertEquals(0,all().size());
 }
 @Test public void missingSmsShowsAsBalanceMismatch(){
  assertNull(BankSms.mismatch(a.db,bankId)); // no snapshot yet
  a.db.setPref("smsBalance:"+bankId,"100000");a.db.setPref("smsTime:"+bankId,""+now);
  assertEquals(Long.valueOf(100000),BankSms.mismatch(a.db,bankId)); // ledger 0, bank says 1000: entries missing
  Store.Acc b=a.db.account(bankId);b.opening=200000;a.db.updateAccount(b);
  Store.Tx t=new Store.Tx();t.kind="expense";t.amount=100000;t.day=java.time.LocalDate.now().toString();t.account=bankId;t.note="Manual fix";a.db.save(t);
  assertEquals(Long.valueOf(0),BankSms.mismatch(a.db,bankId)); // reconciled manually
 }
 @Test public void autoRemindDueLogic(){
  long id=a.db.addPerson("Rahul","9876543210");
  Store.Tx t=new Store.Tx();t.kind="lent";t.amount=50000;t.day=java.time.LocalDate.now().toString();t.account=bankId;t.person=id;a.db.save(t);
  assertTrue(AutoRemind.due(a.db,now).isEmpty()); // feature off by default
  a.db.setPref("autoRemind","1");
  assertEquals(1,AutoRemind.due(a.db,now).size());
  a.db.setPref("autoReminded:"+id,""+now);
  assertTrue(AutoRemind.due(a.db,now).isEmpty()); // reminded just now
  assertEquals(1,AutoRemind.due(a.db,now+8L*86400000).size()); // due again after the 7 day interval
  a.db.setPref("autoRemindDays","3");
  assertTrue(AutoRemind.due(a.db,now+4L*86400000).size()==1);
  a.db.setPref("autoReminded:"+id,""+now);
  Store.Tx r=new Store.Tx();r.kind="repay_in";r.amount=50000;r.day=java.time.LocalDate.now().toString();r.account=bankId;r.person=id;a.db.save(r);
  assertTrue(AutoRemind.due(a.db,now+30L*86400000).isEmpty()); // settled: never reminded
 }
 @Test public void autoRemindSkipsPeopleWithoutPhone(){
  a.db.setPref("autoRemind","1");
  long id=a.db.addPerson("NoPhone","");
  Store.Tx t=new Store.Tx();t.kind="lent";t.amount=50000;t.day=java.time.LocalDate.now().toString();t.account=bankId;t.person=id;a.db.save(t);
  assertTrue(AutoRemind.due(a.db,now).isEmpty());
 }
 @Test public void autoRemindPageRenders(){
  AutoRemind.open(a);
  String s=PolishTest.texts(a.root);
  assertTrue(s.contains("Automatic reminders"));assertTrue(s.contains("Remind each person every"));assertTrue(s.contains("Send by"));
  BankSms.open(a);assertTrue(PolishTest.texts(a.root).contains("Auto-add expenses from bank SMS"));
 }
}
