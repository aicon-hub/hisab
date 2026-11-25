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
 @Test public void notificationPromptShowsBothItems() {
  a.db.setPref("notifAsked", "1"); NotifPrompt.maybeShow(a); assertEquals(0, a.layers.size());
  a.db.setPref("notifAsked", "0"); NotifPrompt.show(a);
  assertEquals(1, a.layers.size());
  String text = org.robolectric.shadows.ShadowView.innerText(a.layers.get(0).panel);
  assertTrue(text, text.contains("Timely payment reminders") && text.contains("Instant updates") && text.contains("Turn on notifications") && text.contains("Not now"));
  a.closeAll();
 }
 @Test public void dateSheetShowsMonthGrid() {
  final java.time.LocalDate[] picked = {null};
  DateSheet.show(a, java.time.LocalDate.of(2026, 11, 10), d -> picked[0] = d);
  org.junit.Assert.assertEquals(1, a.layers.size());
  String text = org.robolectric.shadows.ShadowView.innerText(a.layers.get(0).panel);
  assertTrue(text, text.contains("November 2026") && text.contains("Jump to today"));
  a.closeAll();
 }
 @Test public void newCategorySheetOffersIcons() {
  EntrySheet.S s = new EntrySheet.S(); s.kind = "expense";
  EntrySheet.newCategory(a, s, Ui.col(a));
  org.junit.Assert.assertEquals(1, a.layers.size());
  String text = org.robolectric.shadows.ShadowView.innerText(a.layers.get(0).panel);
  assertTrue(text, text.contains("New category") && text.contains("Pick an icon") && text.contains("Add category"));
  a.closeAll();
 }
 @Test public void budgetAlertFiresOncePerCategoryPerMonth(){Store.Cat c=a.db.categories("expense").get(0);java.time.YearMonth m=java.time.YearMonth.now();long base=0;for(Store.Tx t:a.db.txs(Fmt.from(m),Fmt.to(m)))if(t.kind.equals("expense")&&t.cat==c.id)base+=t.amount;a.db.setBudget(c.id,base+5000);assertFalse(BudgetAlert.check(a,a.db,c.id));Store.Tx t=new Store.Tx();t.kind="expense";t.amount=6000;t.cat=c.id;t.day=Fmt.today().toString();a.db.save(t);assertTrue(BudgetAlert.check(a,a.db,c.id));assertFalse(BudgetAlert.check(a,a.db,c.id));a.db.setBudget(c.id,0);a.db.deleteTx(t.id);}
 @Test public void billsAndPhotosOnEveryEntryType(){Store.Tx t=new Store.Tx();t.kind="expense";t.amount=5000;t.day=Fmt.today().toString();t.cat=a.db.categories("expense").get(0).id;t.account=a.db.accounts().get(0).id;t.note="";t.person=0;t.dest=0;t.id=a.db.save(t);Sheets.txDetail(a,t);assertTrue(texts(a.root).contains("Bills and photos"));a.closeAll();a.db.deleteTx(t.id);}
 @Test public void biometricUnlockPreferenceRoundTrip(){assertFalse(Lock.bio(a.db));Lock.setBio(a.db,true);assertTrue(Lock.bio(a.db));Lock.setBio(a.db,false);assertFalse(Lock.bio(a.db));}
 @Test public void trashBinRoundTripAndPurge(){Store.Tx t=new Store.Tx();t.kind="expense";t.amount=4200;t.day=Fmt.today().toString();t.cat=a.db.categories("expense").get(0).id;t.account=a.db.accounts().get(0).id;t.note="bin-test";t.tags="";t.id=a.db.save(t);a.db.trashTx(t.id);for(Store.Tx x:a.db.txs("0000-01-01","9999-12-31"))assertNotEquals(t.id,x.id);assertEquals(1,a.db.trash().size());a.db.restoreTx(t.id);boolean back=false;for(Store.Tx x:a.db.txs("0000-01-01","9999-12-31"))if(x.id==t.id)back=true;assertTrue(back);a.db.trashTx(t.id);a.db.getWritableDatabase().execSQL("UPDATE tx SET deleted=? WHERE id=?",new Object[]{System.currentTimeMillis()-31L*24*60*60*1000,t.id});java.util.List<Long> purged=a.db.purgeTrash();assertTrue(purged.contains(t.id));assertTrue(a.db.trash().isEmpty());}
 @Test public void tagsSaveAndSearchRoundTrip(){Store.Tx t=new Store.Tx();t.kind="expense";t.amount=1000;t.day=Fmt.today().toString();t.cat=a.db.categories("expense").get(0).id;t.account=a.db.accounts().get(0).id;t.note="tagtest-note";t.tags="zztag";t.id=a.db.save(t);assertEquals("zztag",a.db.txById(t.id).tags);a.ledgerQuery="zztag";android.view.View v=LedgerPage.build(a);assertTrue(texts(v).contains("tagtest-note"));a.ledgerQuery="";a.db.deleteTx(t.id);}
 @Test public void ledgerCategoryAndAmountFilters(){Store.Cat c=a.db.categories("expense").get(0);Store.Tx t=new Store.Tx();t.kind="expense";t.amount=5000;t.day=Fmt.today().toString();t.cat=c.id;t.account=a.db.accounts().get(0).id;t.note="zzfilter-note";t.tags="";t.id=a.db.save(t);a.ledgerCat=c.id+9999;android.view.View v=LedgerPage.build(a);assertFalse(texts(v).contains("zzfilter-note"));a.ledgerCat=0;a.ledgerMin=900000;v=LedgerPage.build(a);assertFalse(texts(v).contains("zzfilter-note"));a.ledgerMin=0;a.ledgerMax=4900;v=LedgerPage.build(a);assertFalse(texts(v).contains("zzfilter-note"));a.ledgerMax=0;a.ledgerCat=c.id;v=LedgerPage.build(a);assertTrue(texts(v).contains("zzfilter-note"));a.ledgerCat=0;a.db.deleteTx(t.id);}
 @Test public void insightsCalendarWeekendAndCashSplit() {
  Store db = a.db; java.time.YearMonth ym = java.time.YearMonth.now();
  Store.Acc cash = null, bank = null; for (Store.Acc x : db.accounts()) { if (x.kind.equals("cash")) cash = x; if (x.kind.equals("bank")) bank = x; }
  assertNotNull(cash); assertNotNull(bank);
  Store.Tx t1 = new Store.Tx(); t1.kind = "expense"; t1.amount = 3000; t1.day = ym.atDay(2).toString(); t1.account = cash.id; t1.cat = 0; t1.note = "zzcash"; t1.person = 0; t1.id = db.save(t1);
  Store.Tx t2 = new Store.Tx(); t2.kind = "expense"; t2.amount = 7000; t2.day = ym.atDay(3).toString(); t2.account = bank.id; t2.cat = 0; t2.note = "zzdig"; t2.person = 0; t2.id = db.save(t2);
  a.insightMonth = ym;
  String tx = texts(InsightsPage.build(a));
  assertTrue(tx.contains("Spending calendar"));
  assertTrue(tx.contains("Cash or digital"));
  assertTrue(tx.contains("Weekdays"));
  db.deleteTx(t1.id); db.deleteTx(t2.id);
 }

 @Test public void leftTodayCardFollowsLimit() {
  a.db.setPref("dailyLimit", "50000");
  Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = 12000; t.day = java.time.LocalDate.now().toString(); t.account = a.db.accounts().get(0).id; t.cat = 0; t.note = "Lunch"; t.person = 0; a.db.save(t);
  a.refresh();
  String today = java.time.LocalDate.now().toString(); long spent = 0; for (Store.Tx x : a.db.txs(today, today)) if (x.kind.equals("expense")) spent += x.amount;
  assertTrue(texts(a.root).contains("Left today"));
  assertTrue(texts(a.root).contains("Spent " + Fmt.money(spent) + " of " + Fmt.money(50000)));
  a.db.setPref("dailyLimit", "0"); a.refresh();
  assertFalse(texts(a.root).contains("Left today"));
 }

 @Test public void netWorthAddsUp() {
  Store db = a.db;
  long inAccounts = 0; for (Store.Acc x : db.accounts()) inAccounts += db.balance(x);
  long owed = 0, owe = 0; for (Store.Person p : db.people()) { long n = db.net(p.id); if (n > 0) owed += n; else owe -= n; }
  NetWorth.open(a);
  String text = texts(a.layers.get(a.layers.size() - 1).panel);
  assertTrue(text.contains("Net worth"));
  assertTrue(text.contains(Fmt.money(inAccounts + owed - owe)));
  assertTrue(text.contains("Owed to you"));
 }

 @Test public void malayalamDigits() {
  Store.Tx t = HomePage.parse(a.db, "\u0D1A\u0D3E\u0D2F \u0D6A\u0D66");
  assertNotNull(t); assertEquals(4000, t.amount);
  assertEquals(4000, Ledger.parse("\u0D6A\u0D66"));
  assertEquals(-1, Ledger.parse("\u0D6A.\u0D66\u0D66\u0D66"));
 }

 @Test public void settingsHubListsSixSections() {
  a.settingsSection = ""; Settings.open(a);
  String hub = texts(a.layers.get(a.layers.size() - 1).panel);
  for (String need : new String[]{"Look", "Language and privacy", "Money", "Reminders", "Your data", "About"}) assertTrue(need, hub.contains(need));
  assertFalse(hub.contains("Bank SMS balances"));
  a.settingsSection = "money"; a.closeAll(); Settings.open(a);
  String money = texts(a.layers.get(a.layers.size() - 1).panel);
  assertTrue(money.contains("Bank SMS balances"));
  assertFalse(money.contains("Share Hisab with a friend"));
 }

 @Test public void yearReportRenders(){YearReport.open(a);assertTrue(texts(a.root).contains("Year report"));a.closeAll();}
}
