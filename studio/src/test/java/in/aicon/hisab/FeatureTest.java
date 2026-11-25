package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.widget.*;
import android.view.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class FeatureTest {
 MainActivity a;
 @Before public void start(){a=Robolectric.buildActivity(MainActivity.class,new android.content.Intent().putExtra("noLaunch",true)).setup().get();}
 @Test public void encryptedRoundTripAndAuthentication() throws Exception {
  byte[] source=Backup.snapshot(a.db);byte[] encrypted=Backup.crypt(source,"test-only-passphrase".toCharArray(),true);assertFalse(new String(encrypted,StandardCharsets.UTF_8).contains("Salary"));
  byte[] decrypted=Backup.crypt(encrypted,"test-only-passphrase".toCharArray(),false);assertArrayEquals(source,decrypted);
  a.db.wipe();a.db.seedBasics();Backup.restore(a.db,decrypted);assertTrue(a.db.hasData());assertEquals("0",a.db.pref("smsEnabled",""));
  try{Backup.crypt(encrypted,"wrong-password".toCharArray(),false);fail();}catch(javax.crypto.AEADBadTagException expected){}
  encrypted[40]^=1;try{Backup.crypt(encrypted,"test-only-passphrase".toCharArray(),false);fail();}catch(javax.crypto.AEADBadTagException expected){}
 }
 @Test public void failedRestoreRollsBackAllTables() throws Exception {
  byte[] before=Backup.snapshot(a.db);org.json.JSONObject bad=new org.json.JSONObject(new String(before,StandardCharsets.UTF_8));bad.getJSONArray("tx").getJSONObject(0).put("day","not-a-date");
  try{Backup.restore(a.db,bad.toString().getBytes(StandardCharsets.UTF_8));fail();}catch(Exception expected){}
  assertArrayEquals(before,Backup.snapshot(a.db));
 }
 @Test public void allBankSenderFamiliesSyntheticFormats(){
  int i=0;for(Map.Entry<String,String> bank:BankSms.BANKS.entrySet()){
   for(String balance:new String[]{"Avl Bal: Rs. 1,23,456.78","Available balance INR 123456.78","Bal. Rs 123456.78","Clear Balance: INR 123456.78"}){
    BankSms.Result r=BankSms.parse("VM-"+bank.getKey(),"Your A/c XX1234 debited Rs. 50. "+balance+" on 15-11-26");assertNotNull(bank.getKey()+balance,r);assertEquals(bank.getValue(),r.bank);assertEquals("1234",r.last4);assertEquals(12345678L,r.balance);i++;
   }
  }assertEquals(80,i);
 }
 @Test public void rejectAmbiguousOtpUnknownAndNoBalance(){
  for(String s:new String[]{"OTP 123456 for a/c XX1234. Avl bal INR 5000","A/c XX1234 debited INR 10","Credit card XX1234 available balance INR 3000","A/c XX1234 balance INR 30 A/c XX5678 balance INR 50","Account XX1234 balance INR 50.123","Offer for account XX1234 balance INR 50","Account XX1234 minimum balance INR 1000","Account XX1234 transaction failed. Balance INR 1000"})assertNull(s,BankSms.parse("VM-HDFCBK",s));
  assertNull(BankSms.parse("+919999999999","A/c XX1234 bal INR 1000"));assertNull(BankSms.parse("FAKEBK","A/c XX1234 bal INR 1000"));
 }
 @Test public void smsRequiresOptInLinkAndNewerTimestamp(){
  long id=a.db.accounts().get(0).id;long ledger=a.db.balance(a.db.account(id));String sms="A/c XX1234 available balance INR 1200.50";
  assertFalse(BankSms.apply(a.db,"VM-HDFCBK",sms,100));a.db.setPref("smsEnabled","1");assertFalse(BankSms.apply(a.db,"VM-HDFCBK",sms,100));a.db.setPref("smsMap:HDFC:1234",""+id);
  assertTrue(BankSms.apply(a.db,"VM-HDFCBK",sms,100));assertFalse(BankSms.apply(a.db,"VM-HDFCBK",sms,100));assertFalse(BankSms.apply(a.db,"VM-HDFCBK",sms,99));assertEquals("120050",a.db.pref("smsBalance:"+id,""));assertEquals(ledger,a.db.balance(a.db.account(id)));
 }
 @Test public void quickTextPersistsOnceThroughActualButton(){
  EditText input=a.root.findViewWithTag("quickEntry");assertNotNull(input);input.setText("auto 60 yesterday");ViewGroup row=(ViewGroup)input.getParent();View go=row.getChildAt(row.getChildCount()-1);int before=a.db.txs("","~").size();go.performClick();assertEquals(before+1,a.db.txs("","~").size());Store.Tx t=a.db.recent(1).get(0); // recent can sort future sample; query by note instead
  boolean found=false;for(Store.Tx x:a.db.txs("","~"))if(x.note.equals("Auto")&&x.amount==6000&&x.day.equals(java.time.LocalDate.now().minusDays(1).toString()))found=true;assertTrue(found);
 }
 @Test public void quickDatesCurrencyAndAmbiguity(){
  for(String s:new String[]{"tea 40","tea Rs. 40","INR 40 tea","₹40 tea"})assertEquals(4000,HomePage.parse(a.db,s).amount);
  assertEquals("2026-11-10",HomePage.parse(a.db,"tea 40 2026-11-10").day);assertNull(HomePage.parse(a.db,"tea 40 50"));assertNull(HomePage.parse(a.db,"tea -40"));assertNull(HomePage.parse(a.db,"tea 40 2026-02-31"));assertNull(HomePage.parse(a.db,"tea 40.123"));
 }
 @Test public void customSenderMappingAndZeroBalance(){
  long id=a.db.accounts().get(0).id;a.db.setPref("smsEnabled","1");a.db.setPref("smsSender:MYBANK","Regional Bank");a.db.setPref("smsMap:Regional Bank:4321",""+id);assertTrue(BankSms.apply(a.db,"VM-MYBANK","Account XX4321 available balance INR 0.00",100));assertEquals("0",a.db.pref("smsBalance:"+id,""));
 }
 @Test public void profileBitmapAndIconCategoryPersistence() throws Exception {
  android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(40,40,android.graphics.Bitmap.Config.ARGB_8888);b.eraseColor(0xFF0099AA);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);a.db.setPref("profilePhoto",android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP));assertTrue(Profile.avatar(a,44) instanceof android.widget.ImageView);
  long id=a.db.addCategory("Pets","expense","health");Store.Cat cat=a.db.categoryMap().get(id);cat.icon="gift";a.db.updateCategory(cat);assertEquals("gift",a.db.categoryMap().get(id).icon);byte[] snap=Backup.snapshot(a.db);a.db.wipe();a.db.seedBasics();Backup.restore(a.db,snap);assertTrue(Profile.avatar(a,44) instanceof android.widget.ImageView);assertEquals("gift",a.db.categoryMap().get(id).icon);
 }
 @Test public void launcherAliasMatchesEveryAccent(){
  for(String accent:Ui.ACCENT_NAMES){a.db.setPref("accent",accent);LauncherIcons.apply(a);assertEquals(accent,LauncherIcons.variant(a.db));for(String other:Ui.ACCENT_NAMES)assertEquals(accent.equals(other)?android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED:android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,a.getPackageManager().getComponentEnabledSetting(new android.content.ComponentName(a,"in.aicon.hisab.Icon"+other.replace(" ",""))));}
  a.db.setPref("matchLauncher","0");LauncherIcons.apply(a);assertEquals("Original",LauncherIcons.variant(a.db));
 }
 @Test public void lastAccountCannotBeDeleted(){a.db.wipe();a.db.seedBasics();java.util.List<Store.Acc> accounts=a.db.accounts();assertTrue(a.db.deleteAccount(accounts.get(0).id));assertFalse(a.db.deleteAccount(accounts.get(1).id));}
 @Test public void iconPickerAndFeaturePagesBuild(){Pages.iconPicker(a,"other",s->{});a.closeAll();BankSms.open(a);a.closeAll();Backup.password(a,true);a.closeAll();Backup.password(a,false);a.closeAll();Profile.choose(a);a.closeAll();for(String accent:Ui.ACCENT_NAMES){a.db.setPref("accent",accent);a.applyTheme();a.build();}assertEquals(18,Ui.ACCENT_NAMES.length);}
}
