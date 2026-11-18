package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class SmsLiveTest {
 MainActivity a; long now;
 @Before public void start(){
  a=Robolectric.buildActivity(MainActivity.class,new android.content.Intent().putExtra("noLaunch",true)).setup().get();
  a.db.wipe();a.db.seedBasics();a.db.setPref("sample","0");
  a.db.setPref("smsEnabled","1");a.db.setPref("smsTxEnabled","1");a.db.setPref("smsLive","1");a.db.setPref("smsAutoLink","1");a.db.setPref("smsNotify","1");
  now=System.currentTimeMillis();
 }
 List<Store.Tx> all(){return a.db.txs("0000-01-01","9999-12-31");}
 Store.Acc acc(String n){for(Store.Acc x:a.db.accounts())if(x.name.equals(n))return x;return null;}

 @Test public void firstDebitCreatesAccountWithMatchingLedger(){
  SmsAuto.Out o=SmsAuto.handle(a.db,"VM-HDFCBK","Rs.500.00 debited from A/c XX1234 on 18-11-26 to VPA shop@okhdfc. Avl bal INR 4,500.00",now);
  assertTrue(o.created);assertTrue(o.expense);assertTrue(o.balance);
  Store.Acc x=acc("HDFC ••1234");assertNotNull(x);
  assertEquals(450000L,a.db.balance(x));
  assertTrue(all().get(0).note.contains("bal"));
  assertEquals(0L,(long)BankSms.mismatch(a.db,x.id));
 }
 @Test public void secondMessageKeepsLedgerInSync(){
  SmsAuto.handle(a.db,"VM-HDFCBK","Rs.500.00 debited from A/c XX1234 on 18-11-26 to VPA shop@okhdfc. Avl bal INR 4,500.00",now-5000);
  SmsAuto.handle(a.db,"VM-HDFCBK","Rs.200.00 debited from A/c XX1234 on 18-11-26 to VPA tea@okhdfc. Avl bal INR 4,300.00",now);
  Store.Acc x=acc("HDFC ••1234");assertEquals(1,a.db.accounts().stream().filter(y->y.name.equals("HDFC ••1234")).count());
  assertEquals(2,all().size());assertEquals(430000L,a.db.balance(x));assertEquals(0L,(long)BankSms.mismatch(a.db,x.id));
 }
 @Test public void duplicateSmsDoesNotDouble(){
  String m="Rs.500.00 debited from A/c XX1234 to VPA shop@okhdfc. Avl bal INR 4,500.00";
  SmsAuto.handle(a.db,"VM-HDFCBK",m,now);SmsAuto.Out o=SmsAuto.handle(a.db,"AX-HDFCBK",m,now+1000);
  assertEquals(1,all().size());assertFalse(o.expense);
 }
 @Test public void nonBankSenderIgnored(){
  SmsAuto.Out o=SmsAuto.handle(a.db,"+919876543210","Rs.500.00 debited from A/c XX1234. Avl bal INR 4,500.00",now);
  assertFalse(o.created||o.expense||o.balance);assertNull(acc("HDFC ••1234"));
  o=SmsAuto.handle(a.db,"VM-SHOPXY","Rs.500.00 debited from A/c XX1234. Avl bal INR 4,500.00",now);
  assertFalse(o.created);assertEquals(0,all().size());
 }
 @Test public void otpAndOffAreIgnored(){
  assertFalse(SmsAuto.handle(a.db,"VM-HDFCBK","Your OTP is 123456 for A/c XX1234 debit of Rs.500. Avl bal INR 4,500.00",now).created);
  a.db.setPref("smsEnabled","0");
  assertFalse(SmsAuto.handle(a.db,"VM-HDFCBK","Rs.500.00 debited from A/c XX1234. Avl bal INR 4,500.00",now).created);
 }
 @Test public void withoutAutoLinkNothingIsCreated(){
  a.db.setPref("smsAutoLink","0");
  SmsAuto.Out o=SmsAuto.handle(a.db,"VM-HDFCBK","Rs.500.00 debited from A/c XX1234. Avl bal INR 4,500.00",now);
  assertFalse(o.created);assertNull(acc("HDFC ••1234"));assertEquals(0,all().size());
 }
 @Test public void creditOnlyUpdatesBalance(){
  SmsAuto.Out o=SmsAuto.handle(a.db,"VM-HDFCBK","Rs.1,000.00 credited to A/c XX1234. Avl bal INR 6,000.00",now);
  assertTrue(o.balance);assertFalse(o.expense);assertEquals(600000L,a.db.balance(acc("HDFC ••1234")));
 }
 @Test public void receiverRunsInBackgroundPath(){
  android.content.Intent i=new android.content.Intent(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
  assertNotNull(a.db);new BankSms().onReceive(a,i);
 }
}
