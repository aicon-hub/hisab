package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.view.*;
import android.widget.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class PolishTest {
 MainActivity a;
 @Before public void start(){a=Robolectric.buildActivity(MainActivity.class,new android.content.Intent().putExtra("noLaunch",true)).setup().get();}
 static String texts(View v){String s=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s+="\n"+texts(((ViewGroup)v).getChildAt(i));return s;}
 @Test public void englishDefaultAndMalayalamSelection(){assertEquals("English",a.db.pref("language","English"));a.db.setPref("language","മലയാളം");a.applyTheme();a.build();assertTrue(texts(a.root).contains("നമസ്കാരം"));assertTrue(texts(a.root).contains("അക്കൗണ്ടുകൾ"));Settings.open(a);assertTrue(texts(a.root).contains("ക്രമീകരണങ്ങൾ"));a.db.setPref("language","English");a.applyTheme();a.build();assertTrue(texts(a.root).contains("Hello"));}
 @Test public void languageDoesNotChangeRecords(){byte[] old=tableSnapshot();a.db.setPref("language","മലയാളം");a.applyTheme();a.build();Pages.categories(a);assertArrayEquals(old,tableSnapshot());}
 byte[] tableSnapshot(){StringBuilder s=new StringBuilder();for(Store.Cat c:a.db.categories("expense"))s.append(c.id).append(c.name).append(c.icon);for(Store.Tx t:a.db.txs("","~"))s.append(t.note).append(t.amount);return s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);}
 @Test public void privacyPerKindAndGlobalPersist(){Store.Acc ac=a.db.accounts().get(0);long value=a.db.balance(ac);a.db.setPref("hide:"+ac.kind,"1");assertEquals("••••",Privacy.amount(a.db,ac,value));assertTrue(Privacy.anyHidden(a.db));a.db.setPref("hide:"+ac.kind,"0");assertEquals(Fmt.money(value),Privacy.amount(a.db,ac,value));a.db.setPref("hideAll","1");a.refresh();assertEquals("••••",Privacy.amount(a.db,ac,value));assertTrue(texts(a.root).contains("••••"));assertEquals(value,a.db.balance(ac));Store reopened=new Store(a);assertTrue(reopened.flag("hideAll",false));reopened.close();}
 @Test public void homeToggleActuallyHidesAndShows(){View toggle=a.root.findViewWithTag("balanceToggle");toggle.performClick();assertTrue(a.db.flag("hideAll",false));a.root.findViewWithTag("balanceToggle").performClick();assertFalse(a.db.flag("hideAll",true));}
 @Test public void screenshotProtection(){assertEquals(0,a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE);a.db.setPref("secureScreen","1");Privacy.apply(a);assertTrue((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);a.db.setPref("secureScreen","0");Privacy.apply(a);assertEquals(0,a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE);}
 @Test public void privacySettingsSwitchesAreLive(){Privacy.open(a);Switch sw=a.root.findViewWithTag("hide:bank");sw.setChecked(true);assertTrue(a.db.flag("hide:bank",false));assertNotNull(a.root.findViewWithTag("hide:bank"));}
 @Test public void smsCurrencyTimestampAndMask(){long id=a.db.accounts().get(0).id;a.db.setPref("smsEnabled","1");a.db.setPref("smsMap:HDFC:1234",""+id);assertTrue(BankSms.apply(a.db,"VM-HDFCBK","A/c XX1234 available balance INR 1200.50.",System.currentTimeMillis()));a.db.setPref("currency","USD");a.applyTheme();String display=BankSms.display(a.db,id);assertTrue(display,display.contains("₹1,200.50"));assertFalse(display,display.contains("$"));a.db.setPref("hideAll","1");display=BankSms.display(a.db,id);assertFalse(display.contains("1,200.50"));assertTrue(display.contains("••••"));}
 @Test public void moreSmsFormatsAreExplicitNotInferred(){
  String[] samples={"Avl. Bal is Rs. 1,20,000.00", "Available balance: INR 120000", "Current Balance 120000.00 INR", "Bal INR +120000.00 CR", "Balance Rs 120000.00."};
  for(String s:samples){BankSms.Result r=BankSms.parse("VM-HDFCBK","Your A/c XX1234. "+s);assertNotNull(s,r);assertEquals(12000000,r.balance);}
  assertEquals(-120000,BankSms.parse("VM-HDFCBK","A/c XX1234 balance INR -1200.00").balance);
  assertEquals(-120000,BankSms.parse("VM-HDFCBK","A/c XX1234 balance INR 1200.00 DR").balance);
  for(String s:new String[]{"balance INR 1,2,00","balance INR 1200.001","balance USD 100","balance INR -100 CR","balance INR -92233720368547758.08","balance INR 50 balance INR 60","balance INR 50 balance 60 INR","debited INR 50. Thank you"})assertNull(s,BankSms.parse("VM-HDFCBK","A/c XX1234 "+s));
 }
 @Test public void automaticCategoryIconsAndMalayalamQuickEntry(){assertTrue(Icons.CATEGORY_ICONS.length>=30);for(String[] pair:new String[][]{{"Gym","fitness"},{"Pet food","food"},{"WiFi","wifi"},{"ചായ","food"},{"വാടക","rent"},{"Laptop","electronics"},{"Sadaqah","charity"}}){assertEquals(pair[0],pair[1],CategoryIcons.suggest(pair[0]));}for(String icon:Icons.CATEGORY_ICONS)assertNotNull(Icons.path(icon));Store.Tx t=HomePage.parse(a.db,"ചായ 40");assertNotNull(t);assertEquals(4000,t.amount);assertEquals("Food",a.db.categoryMap().get(t.cat).name);}
 @Test public void backupNudgeIsLocalAndOnlyAfterAWeek(){a.db.setPref("sample","0");a.db.setPref("lastBackupAt","0");assertTrue(Privacy.backupDue(a.db));a.db.setPref("lastBackupAt",""+System.currentTimeMillis());assertFalse(Privacy.backupDue(a.db));a.db.setPref("lastBackupAt","0");a.db.setPref("backupNudge","0");assertFalse(Privacy.backupDue(a.db));}
 @Test public void revealContainsSelectedAccent(){a.db.setPref("accent","Rose");a.applyTheme();FrameLayout reveal=a.launchView();assertEquals(Ui.accent,((android.graphics.drawable.GradientDrawable)((ViewGroup)reveal.getChildAt(0)).getChildAt(2).getBackground()).getColor().getDefaultColor());assertTrue(texts(reveal).contains("Rose"));}

 @Test public void onAccentFollowsAccentBrightness() {
  assertEquals(0xFF14151A, Ui.onAccentFor(0xFFC7F23A));
  assertEquals(0xFF14151A, Ui.onAccentFor(0xFFF2F0EA));
  assertEquals(0xFFF2F0EA, Ui.onAccentFor(0xFF15161A));
  assertEquals(0xFFF2F0EA, Ui.onAccentFor(0xFF1A237E));
 }
 @Test public void accentOnDarkSurfacesStaysVisible() {
  assertEquals(0xFFC7F23A, Ui.accentOnDarkFor(0xFFC7F23A));
  assertEquals(0xFFF2F0EA, Ui.accentOnDarkFor(0xFF15161A));
  assertEquals(0xFFF2F0EA, Ui.accentOnDarkFor(0xFF000000));
 }
 @Test public void blackAndWhiteAccentFollowsMode() {
  Ui.dark = true; assertEquals(0xFFF2F0EA, Ui.accentFor(null, "Black and White"));
  Ui.dark = false; assertEquals(0xFF15161A, Ui.accentFor(null, "Black and White"));
  assertTrue(java.util.Arrays.asList(Ui.ACCENT_NAMES).contains("Black and White"));
 }

 @Test public void allTimeStatementCollectsCharts() {
  Store db = a.db; db.wipe(); db.seedBasics();
  java.util.List<Store.Acc> accs = db.accounts();
  Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = 4500; t.day = "2026-11-20"; t.account = accs.get(0).id; db.save(t);
  Store.Tx t2 = new Store.Tx(); t2.kind = "expense"; t2.amount = 9000; t2.day = "2026-10-05"; t2.account = accs.get(0).id; db.save(t2);
  Store.Tx t3 = new Store.Tx(); t3.kind = "income"; t3.amount = 20000; t3.day = "2026-11-01"; t3.account = accs.get(0).id; db.save(t3);
  Statement.Data d = Statement.collectAll(db);
  assertTrue(d.allTime);
  assertEquals(13500, d.out); assertEquals(20000, d.in);
  assertEquals(3, d.rows.size());
  assertFalse(d.catNames.isEmpty()); assertFalse(d.catColors.isEmpty());
  assertEquals(2, d.barAmounts.size());
  Statement.Data m = Statement.collect(db, java.time.YearMonth.of(2026, 11));
  assertFalse(m.allTime); assertEquals(4500, m.out); assertEquals(2, m.rows.size());
 }
 @Test public void greetingAndStatementOptions() {
  assertEquals("Good morning", HomePage.greet(8));
  assertEquals("Good afternoon", HomePage.greet(13));
  assertEquals("Good evening", HomePage.greet(19));
  assertEquals("Good night", HomePage.greet(2));
  Statement.Opts o = new Statement.Opts();
  assertTrue(o.pie && o.bars && o.color);
  assertEquals(0xFF202020, Statement.grey(0));
  assertTrue(Statement.grey(3) != Statement.grey(0));
 }
}
