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
 @Test public void wishlistRoundTripAndAfford() {
  Store db = a.db;
  java.util.List<Wishlist.Item> items = new java.util.ArrayList<>();
  Wishlist.Item it = new Wishlist.Item(); it.name = "zzcycle"; it.price = 500000; items.add(it); Wishlist.write(db, items);
  assertEquals(1, Wishlist.read(db).size()); assertEquals("zzcycle", Wishlist.read(db).get(0).name); assertEquals(500000, Wishlist.read(db).get(0).price);
  Wishlist.open(a);
  String text = texts(a.layers.get(a.layers.size() - 1).panel);
  assertTrue(text.contains("zzcycle")); assertTrue(text.contains(Fmt.money(500000)));
  Wishlist.write(db, new java.util.ArrayList<>()); assertEquals(0, Wishlist.read(db).size());
 }

 @Test public void tripTotalsFromTags() {
  Store db = a.db; long acc = db.accounts().get(0).id;
  java.util.List<Trips.Trip> trips = new java.util.ArrayList<>();
  Trips.Trip tr = new Trips.Trip(); tr.name = "zzGoa"; tr.from = ""; tr.to = ""; trips.add(tr); Trips.write(db, trips);
  Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = 45000; t.day = java.time.LocalDate.now().toString(); t.account = acc; t.cat = 0; t.note = "Shack"; t.person = 0; t.tags = "beach trip:zzgoa"; t.id = db.save(t);
  long[] tot = Trips.totals(db, "zzGoa"); assertEquals(45000, tot[0]); assertEquals(1, tot[1]);
  Trips.open(a);
  String text = texts(a.layers.get(a.layers.size() - 1).panel);
  assertTrue(text.contains("zzGoa")); assertTrue(text.contains(Fmt.money(45000)));
  db.deleteTx(t.id); Trips.write(db, new java.util.ArrayList<>());
 }

 @Test public void subscriptionFinderDetectsMonthly() {
  Store db = a.db; long acc = db.accounts().get(0).id; java.time.LocalDate base = java.time.LocalDate.now().minusDays(95);
  long[] ids = new long[3];
  for (int i = 0; i < 3; i++) { Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = 19900; t.day = base.plusDays(30L * i).toString(); t.account = acc; t.cat = 0; t.note = "zznetflix"; t.person = 0; ids[i] = db.save(t); }
  java.util.List<Subscriptions.Hit> hits = Subscriptions.detect(db);
  Subscriptions.Hit found = null; for (Subscriptions.Hit h : hits) if (h.note.equals("zznetflix")) found = h;
  assertNotNull(found); assertEquals("monthly", found.period); assertEquals(19900, found.amount);
  Subscriptions.open(a);
  assertTrue(texts(a.layers.get(a.layers.size() - 1).panel).contains("zznetflix"));
  for (long id : ids) db.deleteTx(id);
 }

 @Test public void roundUpPotCountsSpareChange() {
  Store db = a.db; long acc = db.accounts().get(0).id;
  db.setPref("roundUp", "100");
  long before = RoundUp.pot(db);
  Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = 12050; t.day = java.time.LocalDate.now().toString(); t.account = acc; t.cat = 0; t.note = "zzround"; t.person = 0; t.id = db.save(t);
  assertEquals(before + 7950, RoundUp.pot(db));
  db.setPref("roundUp", ""); assertEquals(0, RoundUp.pot(db));
  db.deleteTx(t.id);
 }

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

 @Test public void sharedLedgerRoundTripKeepsTags() {
  Store db = a.db;
  db.wipe(); db.seedBasics();
  Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = 4000; t.day = "2026-11-20"; t.account = db.accounts().get(0).id; t.note = "Chaya"; t.tags = "trip:kochi"; db.save(t);
  int total = db.txs("0000-01-01", "9999-12-31").size();
  String file = SharedLedger.export(db);
  assertTrue(SharedLedger.isSharedLedger(file));
  assertFalse(SharedLedger.isSharedLedger("{\"app\":\"other\"}"));
  assertFalse(SharedLedger.isSharedLedger("not json"));
  db.wipe(); db.seedBasics();
  int[] r = SharedLedger.importEntries(db, file);
  assertEquals(total, r[0]); assertEquals(0, r[1]);
  Store.Tx in = db.txs("2026-11-20", "2026-11-20").get(0);
  assertEquals("trip:kochi", in.tags); assertEquals(4000, in.amount); assertEquals("Chaya", in.note);
  int[] again = SharedLedger.importEntries(db, file);
  assertEquals(0, again[0]); assertEquals(total, again[1]);
 }

 @Test public void yearReviewMath() {
  Store db = a.db;
  db.wipe(); db.seedBasics();
  long cash = db.accounts().get(0).id;
  long food = 0, transport = 0, salary = 0;
  for (Store.Cat c : db.categories(null)) { if (c.name.equals("Food")) food = c.id; if (c.name.equals("Transport")) transport = c.id; if (c.name.equals("Salary")) salary = c.id; }
  Store.Tx e1 = new Store.Tx(); e1.kind = "expense"; e1.amount = 1000; e1.day = "2026-03-05"; e1.account = cash; e1.cat = food; e1.note = "Lunch"; db.save(e1);
  Store.Tx e2 = new Store.Tx(); e2.kind = "expense"; e2.amount = 500; e2.day = "2026-03-05"; e2.account = cash; e2.cat = food; e2.note = "Tea"; db.save(e2);
  Store.Tx e3 = new Store.Tx(); e3.kind = "expense"; e3.amount = 300; e3.day = "2026-03-07"; e3.account = cash; e3.cat = transport; e3.note = "Auto"; db.save(e3);
  Store.Tx i1 = new Store.Tx(); i1.kind = "income"; i1.amount = 9000; i1.day = "2026-03-01"; i1.account = cash; i1.cat = salary; i1.note = "Salary"; db.save(i1);
  long[] tt = YearReview.totals(db, 2026); assertEquals(9000, tt[0]); assertEquals(1800, tt[1]);
  String[] big = YearReview.biggestDay(db, 2026); assertNotNull(big); assertEquals("2026-03-05", big[0]); assertEquals("1500", big[1]);
  assertEquals(3, YearReview.activeDays(db, 2026));
  java.util.List<long[]> top = YearReview.topCategories(db, 2026);
  assertEquals(2, top.size()); assertEquals(food, top.get(0)[0]); assertEquals(1500, top.get(0)[1]);
  assertEquals(0, YearReview.totals(db, 2025)[0]);
  assertNull(YearReview.biggestDay(db, 2025));
 }

 @Test public void widgetAmountsAndForecast() {
  Store db = a.db;
  db.wipe(); db.seedBasics();
  long cash = db.accounts().get(0).id;
  String today = java.time.LocalDate.now().toString();
  Store.Tx e1 = new Store.Tx(); e1.kind = "expense"; e1.amount = 4000; e1.day = today; e1.account = cash; e1.note = "Tea"; db.save(e1);
  Store.Tx e2 = new Store.Tx(); e2.kind = "expense"; e2.amount = 1000; e2.day = "2020-01-10"; e2.account = cash; e2.note = "Old"; db.save(e2);
  Store.Tx i1 = new Store.Tx(); i1.kind = "income"; i1.amount = 9000; i1.day = today; i1.account = cash; i1.note = "Salary"; db.save(i1);
  assertEquals(4000, HisabWidget.todaySpend(db));
  assertEquals(5000, HisabWidget.totalExpense(db));
  java.time.YearMonth ym = java.time.YearMonth.now();
  Store.Tx m1 = new Store.Tx(); m1.kind = "expense"; m1.amount = 15000; m1.day = ym.atDay(1).toString(); m1.account = cash; m1.note = "Big"; db.save(m1);
  java.time.LocalDate mid = ym.atDay(Math.min(15, ym.lengthOfMonth()));
  long spent = 15000 + (today.startsWith(ym.toString()) ? 4000 : 0);
  assertEquals(Math.round(spent * (double) ym.lengthOfMonth() / mid.getDayOfMonth()), Forecast.monthEnd(db, mid));
  assertEquals(0, Forecast.monthEnd(db, ym.atDay(1)));
 }

 @Test public void amountSearchAndTagManager() {
  Store db = a.db; db.wipe(); db.seedBasics();
  long cash = db.accounts().get(0).id; String today = java.time.LocalDate.now().toString();
  String[][] rows = {{"49900", "Shoes", "gift, trip"}, {"12000", "Milk", "Trip"}, {"49900", "Bag", ""}};
  for (String[] r : rows) { Store.Tx x = new Store.Tx(); x.kind = "expense"; x.amount = Long.parseLong(r[0]); x.day = today; x.account = cash; x.note = r[1]; x.tags = r[2]; db.save(x); }
  assertEquals(49900, LedgerPage.amountQuery("499"));
  assertEquals(49950, LedgerPage.amountQuery("499.5"));
  assertEquals(123400, LedgerPage.amountQuery("1,234"));
  assertEquals(-1, LedgerPage.amountQuery("tea"));
  assertEquals(-1, LedgerPage.amountQuery(""));
  java.util.List<String[]> tc = db.tagCounts();
  assertEquals(2, tc.size()); assertEquals("gift", tc.get(0)[0]); assertEquals("1", tc.get(0)[1]); assertEquals("2", tc.get(1)[1]);
  db.renameTag("gift", "trip");
  tc = db.tagCounts(); assertEquals(1, tc.size()); assertEquals("2", tc.get(0)[1]);
  for (Store.Tx x : db.txs("2000-01-01", "2999-12-31")) if (x.note.equals("Shoes")) assertEquals("trip", x.tags);
  db.dropTag("trip");
  assertEquals(0, db.tagCounts().size());
  assertEquals(3, db.txs("2000-01-01", "2999-12-31").size());
  Tags.open(a); assertTrue(texts(a.root).contains("No tags yet")); a.closeAll();
 }

 @Test public void weeklyBudgetAndRollover() {
  Store db = a.db; db.wipe(); db.seedBasics();
  long cash = db.accounts().get(0).id; long cat = db.categories("expense").get(0).id;
  java.time.YearMonth ym = java.time.YearMonth.now(); java.time.YearMonth prev = ym.minusMonths(1);
  db.setBudget(cat, 100000);
  assertEquals(100000, db.effectiveLimit(cat, ym));
  Store.Tx old = new Store.Tx(); old.kind = "expense"; old.amount = 30000; old.day = prev.atDay(5).toString(); old.account = cash; old.cat = cat; old.note = "Last month"; db.save(old);
  assertEquals(0, db.carry(cat, ym));
  db.setRollover(cat, true);
  assertEquals(70000, db.carry(cat, ym));
  assertEquals(170000, db.effectiveLimit(cat, ym));
  Store.Tx over = new Store.Tx(); over.kind = "expense"; over.amount = 250000; over.day = prev.atDay(6).toString(); over.account = cash; over.cat = cat; over.note = "Big"; db.save(over);
  assertEquals(0, db.carry(cat, ym));
  assertEquals(100000, db.effectiveLimit(cat, ym));
  assertEquals(0, db.weeklyLimit(cat));
  db.setWeeklyLimit(cat, 25000); assertEquals(25000, db.weeklyLimit(cat));
  java.time.LocalDate today = java.time.LocalDate.now();
  Store.Tx now = new Store.Tx(); now.kind = "expense"; now.amount = 4000; now.day = today.toString(); now.account = cash; now.cat = cat; now.note = "Today"; db.save(now);
  Store.Tx lastWeek = new Store.Tx(); lastWeek.kind = "expense"; lastWeek.amount = 9000; lastWeek.day = today.minusDays(8).toString(); lastWeek.account = cash; lastWeek.cat = cat; lastWeek.note = "Earlier"; db.save(lastWeek);
  assertEquals(4000, db.weekSpent(cat, today));
  db.setWeeklyLimit(cat, 0); assertEquals(0, db.weeklyLimit(cat));
  Pages.budgets(a); assertTrue(texts(a.root).contains("Budgets")); a.closeAll();
 }

 @Test public void streakAndSavingsGoal() {
  Store db = a.db; db.wipe(); db.seedBasics();
  long cash = db.accounts().get(0).id; java.time.LocalDate today = java.time.LocalDate.now();
  assertEquals(0, Habits.streak(db, today));
  for (int back : new int[]{0, 1, 2, 4}) { Store.Tx x = new Store.Tx(); x.kind = "expense"; x.amount = 1000; x.day = today.minusDays(back).toString(); x.account = cash; x.note = "d" + back; db.save(x); }
  assertEquals(3, Habits.streak(db, today));
  assertEquals(2, Habits.streak(db, today.minusDays(1)));
  db.wipe(); db.seedBasics(); cash = db.accounts().get(0).id;
  Store.Tx y = new Store.Tx(); y.kind = "expense"; y.amount = 1000; y.day = today.minusDays(1).toString(); y.account = cash; y.note = "y"; db.save(y);
  assertEquals(1, Habits.streak(db, today));
  assertEquals(0, Habits.goalPct(db)); assertNull(Habits.goalLine(db, today));
  db.setPref("saveGoalPct", "20");
  assertNull(Habits.savedPct(db, today));
  Store.Tx inc = new Store.Tx(); inc.kind = "income"; inc.amount = 100000; inc.day = today.toString(); inc.account = cash; inc.note = "Pay"; db.save(inc);
  Store.Tx ex = new Store.Tx(); ex.kind = "expense"; ex.amount = 70000; ex.day = today.toString(); ex.account = cash; ex.note = "Rent"; db.save(ex);
  long yOut = today.minusDays(1).getMonth() == today.getMonth() ? 1000 : 0; assertEquals(Integer.valueOf((int) Math.round((100000 - 70000 - yOut) * 100.0 / 100000)), Habits.savedPct(db, today));
  assertTrue(Habits.goalLine(db, today).contains("on track"));
  db.setPref("saveGoalPct", "50");
  assertFalse(Habits.goalLine(db, today).contains("on track"));
  Settings.editSaveGoal(a); a.closeAll();
 }

 @Test public void transliteratedMalayalamQuickAdd() {
  Store.Tx t = HomePage.parse(a.db, "chaya 40"); assertNotNull(t); assertEquals(4000, t.amount); assertEquals("Food", a.db.categoryMap().get(t.cat).name);
  Store.Tx g = HomePage.parse(a.db, "paal 55"); assertNotNull(g); assertEquals("Groceries", a.db.categoryMap().get(g.cat).name);
  Store.Tx y = HomePage.parse(a.db, "innale auto 60"); assertNotNull(y); assertEquals(java.time.LocalDate.now().minusDays(1).toString(), y.day); assertEquals("Transport", a.db.categoryMap().get(y.cat).name);
  Store.Tx m = HomePage.parse(a.db, "marunnu 120"); assertNotNull(m); assertEquals("Health", a.db.categoryMap().get(m.cat).name);
  Store.Tx inc = HomePage.parse(a.db, "shambalam 50000"); assertNotNull(inc); assertEquals("income", inc.kind);
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

 @Test public void profilesAndTouchShowWithoutDrive(){a.db.setPref("driveBackup","0");a.settingsSection="data";Settings.open(a);String t=texts(a.root).toString();assertTrue(t.contains("Profiles"));assertTrue(t.contains("Touch and motion"));a.settingsSection="";a.closeAll();}
 @Test public void repeatDateIsPickedNotTyped(){android.widget.EditText e=Sheets.field(a,"d","2026-12-01",android.text.InputType.TYPE_CLASS_TEXT);Sheets.dateField(a,e);assertFalse(e.isFocusable());assertTrue(e.hasOnClickListeners());}
 @Test public void tagChipsToggleTheField(){a.db.setPref("trips","");android.widget.EditText f=Sheets.field(a,"t","food",Sheets.TEXT);android.view.View v=Tags.chooser(a,f);assertNotNull(v);assertEquals("food",String.join(",",Tags.parse(" Food , ,food")));}
 @Test public void bankSmsKeepsThreeStepsAndAdvancedHidden(){BankSms.open(a);String t=texts(a.root);assertTrue(t.contains("Step 1: switch it on"));assertTrue(t.contains("Step 2: pick your bank"));assertTrue(t.contains("More options"));a.closeAll();}
 @Test public void spokenMoneyHasNoCommasOrSymbol(){Fmt.currency("INR");assertEquals("1250 rupees",Speech.money(125000));assertFalse(Speech.money(12345600).contains(","));}
 @Test public void spokenFeedbackPageAndButtonsBuild(){Speech.settings(a);assertTrue(texts(a.root).contains("Test the voice"));assertTrue(texts(a.root).contains("Download (about 91 MB)"));assertFalse(MlVoice.installed(a));a.closeAll();a.settingsSection="";a.showTab(0,false);assertTrue(texts(a.root).contains("Read out today"));Speech.say(a,"hello");}
 @Test public void yearReportRenders(){YearReport.open(a);assertTrue(texts(a.root).contains("Year report"));a.closeAll();}

 @Test public void diaryAndMonthlySummary() {
  Store db = a.db; db.wipe(); db.seedBasics(); db.getWritableDatabase().delete("prefs", "k LIKE ? OR k LIKE ?", new String[]{"diary:%", "monthSummary%"});
  String d = java.time.LocalDate.now().toString();
  assertEquals("", Diary.get(db, d)); assertTrue(Diary.all(db).isEmpty());
  Diary.set(db, d, "  Chai with Anu  "); Diary.set(db, "2020-01-02", "older");
  assertEquals("Chai with Anu", Diary.get(db, d));
  assertEquals(2, Diary.all(db).size()); assertEquals(d, Diary.all(db).get(0).day);
  Diary.set(db, d, ""); assertEquals("", Diary.get(db, d)); assertEquals(1, Diary.all(db).size());
  Diary.open(a); assertTrue(texts(a.root).contains("older")); a.closeAll();
  java.time.YearMonth prev = java.time.YearMonth.now().minusMonths(1);
  assertNull(Diary.summary(db, prev));
  long cash = db.accounts().get(0).id; java.util.List<Store.Cat> cats = db.categories("expense");
  Store.Tx e = new Store.Tx(); e.kind = "expense"; e.amount = 50000; e.day = prev.atDay(5).toString(); e.account = cash; e.cat = cats.get(0).id; e.note = "x"; db.save(e);
  Store.Tx i = new Store.Tx(); i.kind = "income"; i.amount = 200000; i.day = prev.atDay(6).toString(); i.account = cash; i.note = "pay"; db.save(i);
  String sum = Diary.summary(db, prev); assertNotNull(sum); assertTrue(sum, sum.contains(cats.get(0).name));
  assertFalse(Diary.monthlyCheck(a, db, java.time.LocalDate.now()));
  db.setPref("monthSummary", "1");
  assertTrue(Diary.monthlyCheck(a, db, java.time.LocalDate.now()));
  assertEquals(prev.toString(), db.pref("monthSummaryDone", ""));
  assertFalse(Diary.monthlyCheck(a, db, java.time.LocalDate.now()));
 }

 @Test public void personStatementAndUpi() {
  Store db = a.db; db.wipe(); db.seedBasics();
  long pid = db.addPerson("Test Friend", ""); Store.Person q = null; for (Store.Person x : db.people()) if (x.id == pid) q = x;
  long cash = db.accounts().get(0).id;
  String[][] es = {{"lent", "100000", "2026-01-01"}, {"repay_in", "30000", "2026-01-05"}, {"borrowed", "90000", "2026-01-09"}};
  for (String[] e : es) { Store.Tx t = new Store.Tx(); t.kind = e[0]; t.amount = Long.parseLong(e[1]); t.day = e[2]; t.account = cash; t.person = q.id; t.note = ""; db.save(t); }
  java.util.List<PersonStatement.Row> rows = PersonStatement.rows(db, q.id);
  assertEquals(3, rows.size()); assertEquals(100000, rows.get(0).balance); assertEquals(70000, rows.get(1).balance); assertEquals(-20000, rows.get(2).balance);
  assertEquals(db.net(q.id), rows.get(2).balance);
  assertTrue(PersonStatement.summary("Test Friend", -20000).startsWith("You owe Test Friend"));
  assertTrue(PersonStatement.validUpi("friend.name@okbank")); assertFalse(PersonStatement.validUpi("friend")); assertFalse(PersonStatement.validUpi("a b@bank"));
  android.net.Uri u = PersonStatement.upiUri("friend@okbank", "Test Friend", 20050);
  assertEquals("upi", u.getScheme()); assertEquals("friend@okbank", u.getQueryParameter("pa")); assertEquals("200.50", u.getQueryParameter("am")); assertEquals("INR", u.getQueryParameter("cu")); assertEquals("Test Friend", u.getQueryParameter("pn"));
  assertNull(PersonStatement.upiUri("bad", "x", 100)); assertNull(PersonStatement.upiUri("friend@okbank", "x", 0));
  PeoplePage.person(a, q); assertTrue(texts(a.root).contains("Share statement")); assertTrue(texts(a.root).contains("Pay by UPI")); a.closeAll();
 }

 @Test public void incomeSourcesGroupAndShare() {
  Store db = a.db; db.wipe(); db.seedBasics();
  long cash = db.accounts().get(0).id; java.util.List<Store.Cat> inc = db.categories("income");
  java.time.LocalDate today = java.time.LocalDate.now();
  assertEquals(0, IncomeSources.collect(db, 3, today).total);
  long[][] es = {{300000, 0}, {100000, 0}, {50000, 1}};
  for (long[] e : es) { Store.Tx t = new Store.Tx(); t.kind = "income"; t.amount = e[0]; t.day = today.toString(); t.account = cash; t.cat = inc.get((int) e[1]).id; t.note = "x"; db.save(t); }
  Store.Tx old = new Store.Tx(); old.kind = "income"; old.amount = 70000; old.day = today.minusYears(2).toString(); old.account = cash; old.cat = inc.get(0).id; old.note = "old"; db.save(old);
  Store.Tx ex = new Store.Tx(); ex.kind = "expense"; ex.amount = 99999; ex.day = today.toString(); ex.account = cash; ex.note = "e"; db.save(ex);
  IncomeSources.Result all = IncomeSources.collect(db, 3, today);
  assertEquals(520000, all.total); assertEquals(2, all.sources.size()); assertEquals(inc.get(0).name, all.sources.get(0).name); assertEquals(470000, all.sources.get(0).amount);
  IncomeSources.Result month = IncomeSources.collect(db, 0, today);
  assertEquals(450000, month.total); assertEquals(1, month.byMonth.size());
  assertEquals(67, IncomeSources.share(300000, 450000)); assertEquals(0, IncomeSources.share(5, 0));
  assertTrue(IncomeSources.from(1, java.time.LocalDate.of(2026, 1, 15)).equals("2025-11-01"));
  IncomeSources.open(a); assertTrue(texts(a.root).contains("Income sources")); assertTrue(texts(a.root).contains(inc.get(0).name)); a.closeAll();
 }

 @Test public void travelFxConvertsWithTypedRate() {
  java.math.BigDecimal r = TravelFx.parseRate("83.5");
  assertNotNull(r); assertEquals(104375, TravelFx.convert(1250, r));
  assertEquals(1, TravelFx.convert(1, TravelFx.parseRate("0.5")));
  assertEquals(0, TravelFx.convert(1, new java.math.BigDecimal("0.4")) == 0 ? 0 : 1);
  for (String bad : new String[]{"0", "-3", "abc", "", "1e5", "9999999", "1.1234567"}) assertNull(bad, TravelFx.parseRate(bad));
  assertNull(TravelFx.parseRate(null)); assertNotNull(TravelFx.parseRate("1,234.5"));
  assertEquals(-1, TravelFx.convert(0, r)); assertEquals(-1, TravelFx.convert(100, null)); assertEquals(-1, TravelFx.convert(Long.MAX_VALUE, r));
  assertEquals("USD", TravelFx.parseCode(" usd ")); assertNull(TravelFx.parseCode("US")); assertNull(TravelFx.parseCode("US1"));
  assertEquals("USD 12.50 @ 83.5", TravelFx.noteText("USD", 1250, r));
  assertEquals("EUR 3 @ 90", TravelFx.noteText("EUR", 300, new java.math.BigDecimal("90.000")));
  a.db.wipe(); a.db.seedBasics(); EntrySheet.open(a, "expense", null);
  View tv = a.root.findViewWithTag("entryAmount"); assertNotNull(tv); assertNotNull(a.root.findViewWithTag("entryNote"));
  assertTrue(texts(a.root).contains("Foreign")); a.closeAll();
 }

 @Test public void secondProfileIsSeparateAndFirstIsUntouched() {
  assertEquals("hisab_studio.db", Profiles.dbName(0)); assertEquals("hisab_profile2.db", Profiles.dbName(1));
  assertEquals("Work", Profiles.cleanName("  Work   "));
  assertEquals("A B", Profiles.cleanName(" A \n  B ")); assertNull(Profiles.cleanName("   ")); assertNull(Profiles.cleanName(null));
  assertEquals(20, Profiles.cleanName("abcdefghijklmnopqrstuvwxyz").length());
  assertEquals(0, Profiles.active(a)); assertFalse(Profiles.hasSecond(a));
  Profiles.setActive(a, 1); assertEquals(0, Profiles.active(a));
  a.db.wipe(); a.db.seedBasics(); a.db.addPerson("ProfOneFriend", "");
  assertTrue(Profiles.addSecond(a)); assertFalse(Profiles.addSecond(a));
  assertEquals(1, Profiles.active(a)); assertEquals("hisab-backup-2.zip", Profiles.driveFile());
  Store two = new Store(a); assertEquals(0, two.people().size()); assertEquals("0", two.pref("sample", ""));
  two.addPerson("ProfTwoFriend", ""); assertEquals(1, two.people().size());
  assertTrue(Profiles.rename(a, 1, "  Shop ")); assertEquals("Shop", Profiles.name(a, 1)); assertEquals("Personal", Profiles.name(a, 0));
  Profiles.setActive(a, 0); assertEquals("hisab-backup.zip", Profiles.driveFile());
  Store one = new Store(a); boolean sawOne = false, sawTwo = false;
  for (Store.Person p : one.people()) { if (p.name.equals("ProfOneFriend")) sawOne = true; if (p.name.equals("ProfTwoFriend")) sawTwo = true; }
  assertTrue(sawOne); assertFalse(sawTwo);
  Profiles.setActive(a, 1); assertEquals(1, new Store(a).people().size());
  Profiles.setActive(a, 0); assertEquals(0, Profiles.active(a));
 }
}
