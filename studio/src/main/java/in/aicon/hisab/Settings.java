package in.aicon.hisab;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

final class Settings {
 private Settings() {}

 static View item(MainActivity a, String icon, String title, String value, Runnable tap) { return item(a, icon, title, value, null, tap); }
 /** Settings row: icon, title with the value as a small line under it (so long values never squeeze the title), and a chevron. */
 static View item(MainActivity a, String icon, String title, String value, String info, Runnable tap) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(13), 0, Ui.dp(13));
  r.addView(Ui.icon(a, icon, 22, Ui.ink), Ui.lpm(Ui.dp(22), Ui.dp(22), 0, 0, 16, 0));
  LinearLayout m = Ui.col(a);
  m.addView(Ui.text(a, title, 15.5f, Ui.ink, true));
  if (value != null) { TextView s = Ui.text(a, value, 13, Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s); }
  r.addView(m, Ui.weight(1));
  r.addView(Ui.icon(a, "right", 18, Ui.muted), Ui.lpm(Ui.dp(18), Ui.dp(18), 10, 0, 0, 0));
  Ui.tap(r, info == null ? tap : () -> infoFirst(a, title, info, tap));
  return r;
 }
 /** First tap on a section explains what it is for, with Skip now and Continue. Shown once per section; the "Section guides" row brings them back. */
 static void infoFirst(MainActivity a, String title, String info, Runnable tap) {
  String key = "infoSeen_" + title;
  if (a.db.flag(key, false)) { tap.run(); return; }
  LinearLayout c = Sheets.sheetBody(a, title);
  TextView t = Ui.text(a, info, 14.5f, Ui.muted, false); t.setPadding(0, 0, 0, Ui.dp(18)); c.addView(t);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  LinearLayout r = Ui.row(a);
  TextView skip = Ui.button(a, "Skip now", false), go = Ui.button(a, "Continue", true);
  r.addView(skip, Ui.lpm(0, -2, 0, 0, 6, 0)); ((LinearLayout.LayoutParams) skip.getLayoutParams()).weight = 1;
  r.addView(go, Ui.lpm(0, -2, 6, 0, 0, 0)); ((LinearLayout.LayoutParams) go.getLayoutParams()).weight = 1;
  c.addView(r);
  Ui.tap(skip, () -> { a.db.setPref(key, "1"); a.close(ref[0]); });
  Ui.tap(go, () -> { a.db.setPref(key, "1"); a.close(ref[0]); tap.run(); });
  ref[0] = Sheets.show(a, c);
 }
 static void choose(MainActivity a, String title, String key, String[] options, String current, boolean rebuild) {
  List<String> o = Arrays.asList(options); Sheets.pick(a, title, o, null, o.indexOf(current), i -> { a.db.setPref(key, o.get(i)); if (rebuild) { a.applyTheme(); a.build(); } else a.refresh(); a.closeAll(); open(a); });
 }

 static void open(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Settings", null, null, c -> {

   LinearLayout p = Ui.row(a); p.setBackground(Ui.shape(Ui.surface, 26)); p.setPadding(Ui.dp(18), Ui.dp(16), Ui.dp(18), Ui.dp(16));
   String nm = db.pref("name", "You"); View av = Profile.avatar(a,56); p.addView(av, Ui.lpm(Ui.dp(56), Ui.dp(56), 0, 0, 16, 0));
   LinearLayout m = Ui.col(a); m.addView(Ui.big(a, nm, 20, Ui.ink)); TextView s = Ui.text(a, "Stored only on this phone", 13, Ui.muted, false); s.setPadding(0, Ui.dp(5), 0, 0); m.addView(s); p.addView(m, Ui.weight(1)); p.addView(Ui.icon(a, "edit", 20, Ui.muted));
   Ui.tap(p, () -> Sheets.form(a, "Your name", new String[]{"Name"}, new String[]{nm}, new int[]{Sheets.TEXT}, "Save", v -> { if (!v[0].isEmpty()) { db.setPref("name", v[0]); a.refresh(); a.closeAll(); open(a); } }, null)); c.addView(p);
   TextView photo=Ui.button(a,"Add or change profile picture",false);c.addView(photo,Ui.lpm(-1,-2,0,10,0,0));Ui.tap(photo,()->Profile.choose(a));

   c.addView(Kit.sectionHead(a, "Look", null, null)); LinearLayout look = Ui.card(a); look.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   look.addView(item(a, "moon", "Appearance", db.pref("appearance", "System"), () -> choose(a, "Appearance", "appearance", new String[]{"System", "Light", "Dark"}, db.pref("appearance", "System"), true))); look.addView(Ui.divider(a));
   LinearLayout sw = Ui.row(a); sw.setPadding(0, Ui.dp(14), 0, Ui.dp(14)); sw.addView(Ui.text(a, "Accent", 15.5f, Ui.ink, true), Ui.weight(1)); String cur = db.pref("accent", "Lime");
   for (int i = 0; i < Ui.ACCENT_NAMES.length; i++) { final String nmx = Ui.ACCENT_NAMES[i]; View d = new View(a); boolean on = nmx.equals(cur); d.setBackground(on ? Ui.outline(Ui.accentFor(a, nmx), Ui.ink, 14, 2.2f) : Ui.shape(Ui.accentFor(a, nmx), 14)); sw.addView(d, Ui.lpm(Ui.dp(28), Ui.dp(28), 4, 0, 0, 0)); d.setContentDescription(nmx); Ui.tap(d, () -> { db.setPref("accent", nmx); a.applyTheme(); a.build(); a.closeAll(); open(a); }); }
   HorizontalScrollView colourScroll=new HorizontalScrollView(a);colourScroll.setHorizontalScrollBarEnabled(true);colourScroll.addView(sw);look.addView(colourScroll);
   look.addView(item(a,"settings","All accent colours",cur,()->choose(a,"Accent colour","accent",Ui.ACCENT_NAMES,db.pref("accent","Lime"),true)));
   LinearLayout launcher=Ui.row(a);launcher.setPadding(0,Ui.dp(12),0,Ui.dp(12));launcher.addView(Ui.text(a,"Match app icon to accent",14,Ui.ink,true),Ui.weight(1));Switch match=Ui.toggle(a);match.setChecked(db.flag("matchLauncher",true));launcher.addView(match);match.setOnCheckedChangeListener((v,on)->{db.setPref("matchLauncher",on?"1":"0");LauncherIcons.apply(a);a.toast("Launcher icon may take a moment to refresh",null);});look.addView(launcher);
   look.addView(Ui.text(a,"Your launcher may refresh the icon with a short delay. Android themed icons can override these colours.",12,Ui.muted,false),Ui.lpm(-1,-2,0,0,0,12)); look.addView(Ui.divider(a));
   LinearLayout pb = Ui.row(a); pb.setPadding(0, Ui.dp(13), 0, Ui.dp(13)); pb.addView(Ui.text(a, "Pure black in dark mode", 15.5f, Ui.ink, true), Ui.weight(1)); Switch sh = Ui.toggle(a); sh.setChecked(db.flag("pureBlack", false)); pb.addView(sh); sh.setOnCheckedChangeListener((v, on) -> { db.setPref("pureBlack", on ? "1" : "0"); a.applyTheme(); a.build(); a.closeAll(); open(a); }); look.addView(pb); look.addView(Ui.divider(a));
   look.addView(item(a, "edit", "Font", db.pref("font", "Inter"), () -> choose(a, "Font", "font", new String[]{"Inter", "System", "Lora", "Poppins"}, db.pref("font", "Inter"), true))); look.addView(Ui.divider(a));
   look.addView(item(a, "edit", "Text size", db.pref("textScale", "Standard"), () -> choose(a, "Text size", "textScale", new String[]{"Compact", "Standard", "Large"}, db.pref("textScale", "Standard"), true))); look.addView(Ui.divider(a));
   look.addView(item(a, "spark", "Custom accent colour", db.pref("accent", "Lime").equals("Custom") ? db.pref("accentHex", "#C7F23A") : "Off", () -> customAccent(a))); look.addView(Ui.divider(a));
   look.addView(item(a, "home", "Customise Home screen", null, () -> HomeCustom.open(a))); c.addView(look);

   LinearLayout gn = Ui.card(a); gn.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   gn.addView(item(a,"globe","Language / ഭാഷ",db.pref("language","English"),()->choose(a,"Language / ഭാഷ","language",new String[]{"English","മലയാളം"},db.pref("language","English"),true)));
   gn.addView(Ui.text(a,"Malayalam is a beta interface translation. Technical help may remain in English. Your entries and category names are not changed.",12,Ui.muted,false),Ui.lpm(-1,-2,0,0,0,10));
   gn.addView(Ui.divider(a));
   gn.addView(item(a,"lock","Privacy",null,()->Privacy.open(a))); c.addView(gn);

   c.addView(Kit.sectionHead(a, "Money", null, null)); LinearLayout mo = Ui.card(a); mo.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   mo.addView(item(a, "wallet", "Accounts", null, () -> Pages.accounts(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "other", "Categories", null, () -> Pages.categories(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "target", "Budgets", null, () -> Pages.budgets(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "flame", "Goals", null, () -> Pages.goals(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "repeat", "Bills and repeats", null, () -> Pages.recurring(a))); mo.addView(Ui.divider(a));
   mo.addView(item(a, "coin", "Currency", db.pref("currency", "INR"), () -> choose(a, "Currency", "currency", Fmt.CURRENCIES, db.pref("currency", "INR"), true))); mo.addView(Ui.divider(a));
   mo.addView(item(a, "ledger", "Number grouping", db.pref("grouping", "Indian"), () -> choose(a, "Number grouping", "grouping", new String[]{"Indian", "International", "None"}, db.pref("grouping", "Indian"), true))); mo.addView(Ui.divider(a));
   mo.addView(item(a, "calendar", "Date format", dateStyleName(db.pref("dateStyle", "DMY")), () -> { List<String> labels = Arrays.asList("24 Nov", "Nov 24", "24.11.2026"), codes = Arrays.asList("DMY", "MDY", "NUM"); Sheets.pick(a, "Date format", labels, null, codes.indexOf(db.pref("dateStyle", "DMY")), i -> { db.setPref("dateStyle", codes.get(i)); a.applyTheme(); a.build(); a.closeAll(); open(a); }); })); mo.addView(Ui.divider(a));
   mo.addView(item(a,"sms","Bank SMS balances","Experimental",()->BankSms.open(a))); c.addView(mo);

   c.addView(Kit.sectionHead(a, "Reminder", null, null)); LinearLayout rm = Ui.card(a); rm.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   LinearLayout rr = Ui.row(a); rr.setPadding(0, Ui.dp(14), 0, Ui.dp(14)); rr.addView(Ui.icon(a, "bell", 22, Ui.ink), Ui.lpm(Ui.dp(22), Ui.dp(22), 0, 0, 16, 0)); rr.addView(Ui.text(a, "Daily check", 15.5f, Ui.ink, true), Ui.weight(1)); Switch rs = Ui.toggle(a); rs.setChecked(db.flag("remind", false)); rr.addView(rs);
   rs.setOnCheckedChangeListener((v, on) -> { db.setPref("remind", on ? "1" : "0"); if (on && Build.VERSION.SDK_INT >= 33) a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5); Reminders.schedule(a, db); }); rm.addView(rr); rm.addView(Ui.divider(a));
   int mins = Integer.parseInt(db.pref("remindAt", "1230")); rm.addView(item(a, "calendar", "Time", String.format(Locale.US, "%d:%02d %s", (mins / 60 + 11) % 12 + 1, mins % 60, mins / 60 >= 12 ? "PM" : "AM"), () -> new android.app.TimePickerDialog(a, (v, h, mi) -> { db.setPref("remindAt", "" + (h * 60 + mi)); Reminders.schedule(a, db); a.closeAll(); open(a); }, mins / 60, mins % 60, false).show())); rm.addView(Ui.divider(a));
   rm.addView(item(a, "people", "Money due reminders", db.flag("autoRemind", false) ? "On" : "Off", () -> AutoRemind.open(a))); c.addView(rm);

   c.addView(Kit.sectionHead(a, "Your data", null, null)); LinearLayout da = Ui.card(a); da.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   da.addView(item(a,"lock","Encrypted backup",null,()->Backup.password(a,true)));da.addView(Ui.divider(a));
   da.addView(item(a,"download","Restore encrypted backup",null,()->Backup.password(a,false)));da.addView(Ui.divider(a));
   da.addView(item(a, "google", "Google Drive backup", DriveBackup.toggleValue(DriveBackup.enabled(db), DriveBackup.account(db)), "Keeps a copy of everything in the hidden app-data folder of your own Google Drive. After every change you make, the copy is updated to match this phone exactly, including deletions. Only you can see it.", () -> DriveBackup.toggle(a))); da.addView(Ui.divider(a));
   if (DriveBackup.enabled(db)) {
    da.addView(item(a, "upload", "Back up to Google Drive now", DriveBackup.backupNowValue(DriveBackup.lastBackupAt(db), DriveBackup.errorAt(db)), "Uploads a fresh copy of all your entries, accounts and attached bills to your own Google Drive right now. Backups also happen on their own after every change.", () -> DriveBackup.backupNow(a, false))); da.addView(Ui.divider(a));
    da.addView(item(a, "download", "Restore from Google Drive", "Replaces everything on this phone", "Puts the Google Drive copy back on this phone. Everything currently on this phone, including attached photos, is replaced by the backup.", () -> DriveBackup.restoreNow(a))); da.addView(Ui.divider(a));
   }
   da.addView(item(a, "share", "Export entries as CSV", "A spreadsheet of your entries to share or save", "Sends all your entries as a CSV file that spreadsheet apps like Excel and Google Sheets can open. Nothing is uploaded anywhere; you choose where it goes.", () -> exportCsv(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "share", "Export everything (CSV)", "Entries, accounts, categories, people, budgets, goals, repeats", "One CSV file with the whole app: entries plus your accounts, categories, people, budgets, goals and repeats.", () -> exportEverything(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "upload", "Import entries from CSV", "Bring entries in from a file", "Reads a CSV file you choose and adds the entries in it. Entries that already exist are skipped, so importing the same file twice adds nothing.", () -> importCsv(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "eye", "Import from screenshots or PDF", "Read another app's exports on this phone", "Reads bank or app statements saved as screenshots or PDF files and turns them into entries you can review. Everything is read on this phone.", () -> OcrImport.open(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "bills", "Scan a bill photo", "Items and GST read on this phone", "Take or pick a photo of a bill; Hisab reads the items and GST on this phone and helps you save them as an entry.", () -> BillScan.open(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "download", "Statement (PDF)", "Monthly or all-time, with charts", "Creates a PDF statement of your entries for a month or all time. You choose whether to include the pie chart and bar chart, and whether it uses colours or black and white.", () -> pickStatementMonth(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "trash", "Clear sample data", db.flag("sample", false) ? "Sample is on" : "Not in use", () -> Sheets.confirm(a, "Clear everything?", "This removes every entry, person, budget and goal on this phone, and starts you with empty cash and bank accounts.", "Clear", () -> { db.wipe(); db.seedBasics(); db.setPref("sample", "0"); a.closeAll(); a.refresh(); a.toast("Cleared", null); })));
   if (!db.flag("sample", false)) { da.addView(Ui.divider(a)); da.addView(item(a, "spark", "Load sample data", null, () -> Sheets.confirm(a, "Replace with sample data?", "Your current entries on this phone will be replaced.", "Replace", () -> { Sample.seed(db); a.closeAll(); a.refresh(); }))); }
   c.addView(da);

   c.addView(Kit.sectionHead(a, "About", null, null)); LinearLayout ab = Ui.card(a);
   ab.addView(Ui.text(a, "Hisab by AiCoN", 16, Ui.ink, true)); TextView t = Ui.text(a, "Free and open source under GPL-3.0. No ads, no tracking, no server. Your entries stay on this phone. Hisab never charges or adds interest.", 13.5f, Ui.muted, false); t.setPadding(0, Ui.dp(8), 0, Ui.dp(10)); ab.addView(t);
   ab.addView(Ui.text(a, "Version 0.9.7 test build", 12.5f, Ui.muted, true));
   ab.addView(Ui.divider(a), Ui.lpm(-1, -2, 0, 14, 0, 0));
   ab.addView(item(a, "info", "Section guides", "Short explanations, shown once per section · tap to see them again", () -> { a.db.getWritableDatabase().delete("prefs", "k LIKE 'infoSeen_%'", null); a.toast("Guides will show again when you open each section", null); a.closeAll(); open(a); }));
   c.addView(ab);
   LinearLayout note=Ui.card(a);note.addView(Ui.text(a,"A note from AiCoN",18,Ui.ink,true));note.addView(Ui.text(a,"Built to make everyday money easier to keep track of. Free to use, open source, and yours to shape. Your records stay on your phone. Thanks for trying Hisab and telling us what needs work.",14,Ui.muted,false),Ui.lpm(-1,-2,0,10,0,14));
   String[][] links={{"WhatsApp channel","https://whatsapp.com/channel/0029VawGIGTD38CRJde0fl1c","whatsapp"},{"Telegram community","https://t.me/AiCoN4tech","telegram"},{"AiCoN website","https://aicon-hub.github.io/","globe"}};
   for(int li=0;li<links.length;li++){String[] link=links[li];if(li>0)note.addView(Ui.divider(a));note.addView(item(a,link[2],link[0],null,()->a.launchExternal(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(link[1])))));}
   c.addView(note,Ui.lpm(-1,-2,0,18,0,20));
  });
 }

 static void exportCsv(MainActivity a) {
  Intent i = new Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_TEXT, CsvIo.export(a.db)).putExtra(Intent.EXTRA_SUBJECT, "Hisab entries");
  a.launchExternal(Intent.createChooser(i, "Export entries"));
 }

 static void exportEverything(MainActivity a) {
  Store db = a.db; StringBuilder b = new StringBuilder();
  b.append("# Hisab full export ").append(java.time.LocalDate.now()).append('\n');
  b.append("# accounts\nName,Kind,Opening balance\n");
  for (Store.Acc x : db.accounts()) b.append(Ledger.csv(x.name)).append(',').append(x.kind).append(',').append(Ledger.plain(x.opening)).append('\n');
  b.append("# categories\nName,Kind\n");
  for (Store.Cat x : db.categories(null)) b.append(Ledger.csv(x.name)).append(',').append(x.kind).append('\n');
  b.append("# people\nName,Phone\n");
  for (Store.Person x : db.people()) b.append(Ledger.csv(x.name)).append(',').append(Ledger.csv(x.phone)).append('\n');
  b.append("# budgets\nCategory,Monthly limit\n");
  Map<Long, Long> buds = db.budgets(); Map<Long, Store.Cat> cats = db.categoryMap();
  for (Map.Entry<Long, Long> e : buds.entrySet()) { Store.Cat ct = cats.get(e.getKey()); b.append(Ledger.csv(ct == null ? "" : ct.name)).append(',').append(Ledger.plain(e.getValue())).append('\n'); }
  b.append("# goals\nName,Target,Saved\n");
  for (Store.Goal g : db.goals()) b.append(Ledger.csv(g.name)).append(',').append(Ledger.plain(g.target)).append(',').append(Ledger.plain(g.saved)).append('\n');
  b.append("# recurring\nTitle,Kind,Amount,Account,Category,Period,Next,Active\n");
  Map<Long, Store.Acc> accs = Kit.accMap(db);
  for (Store.Rec r : db.recurring()) b.append(Ledger.csv(r.title)).append(',').append(r.kind).append(',').append(Ledger.plain(r.amount)).append(',').append(Ledger.csv(accs.get(r.account) == null ? "" : accs.get(r.account).name)).append(',').append(Ledger.csv(cats.get(r.cat) == null ? "" : cats.get(r.cat).name)).append(',').append(r.period).append(',').append(r.next).append(',').append(r.active ? "yes" : "no").append('\n');
  b.append("# entries\n").append(CsvIo.export(db));
  Intent i = new Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_TEXT, b.toString()).putExtra(Intent.EXTRA_SUBJECT, "Hisab full export");
  a.launchExternal(Intent.createChooser(i, "Export everything"));
 }

 static void importCsv(MainActivity a) {
  Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("text/*").addCategory(Intent.CATEGORY_OPENABLE);
  a.request(i, d -> {
   try (java.io.InputStream in = a.getContentResolver().openInputStream(d.getData())) {
    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(); byte[] chunk = new byte[8192]; int n;
    while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
    String text = buf.toString("UTF-8");
    if (AnyCsv.isHisabShape(CsvIo.parse(text))) {
     Sheets.confirm(a, "Import entries?", "Accounts, categories and people named in the file are created when missing. Entries that already exist are skipped, so importing the same file twice adds nothing. Everything stays on this phone.", "Import", () -> {
      int[] r = CsvIo.importEntries(a.db, text);
      a.closeAll(); a.refresh(); a.toast("Imported " + r[0] + (r[1] > 0 ? " · skipped " + r[1] : ""), null);
     });
    } else {
     OcrParse.Result found = AnyCsv.detect(CsvIo.parse(text), java.time.LocalDate.now());
     if (found.count() == 0) a.toast("Could not find dated entries in that file", null);
     else OcrImport.review(a, found, 1, 0);
    }
   } catch (Exception e) { a.toast("That file could not be read", null); }
  });
 }


 static void pickStatementMonth(MainActivity a) {
  List<String> names = new ArrayList<>(); List<java.time.YearMonth> months = new ArrayList<>();
  names.add("All time, every entry"); months.add(null);
  java.time.YearMonth ym = java.time.YearMonth.now();
  for (int i = 0; i < 6; i++) { months.add(ym.minusMonths(i)); names.add(Fmt.month(ym.minusMonths(i))); }
  Sheets.pick(a, "Statement for", names, null, 0, i -> Statement.optionsSheet(a, months.get(i)));
 }

 static String dateStyleName(String s) { return s.equals("MDY") ? "Nov 24" : s.equals("NUM") ? "24.11.2026" : "24 Nov"; }

 /** Custom accent from three colour sliders. */
 static void customAccent(MainActivity a) {
  LinearLayout c = Sheets.sheetBody(a, "Custom accent colour");
  final int[] rgb = new int[3]; int cur = MainActivity.parseHex(a.db.pref("accentHex", "#C7F23A"));
  rgb[0] = (cur >> 16) & 0xFF; rgb[1] = (cur >> 8) & 0xFF; rgb[2] = cur & 0xFF;
  View preview = new View(a); TextView hex = Ui.text(a, "", 14, Ui.muted, true); hex.setGravity(Gravity.CENTER);
  Runnable draw = () -> { int col = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]; preview.setBackground(Ui.shape(col, 26)); hex.setText(MainActivity.hexOf(col)); };
  preview.setLayoutParams(Ui.lp(-1, Ui.dp(56))); c.addView(preview); c.addView(hex, Ui.lpm(-1, -2, 0, 10, 0, 4));
  String[] names = {"Red", "Green", "Blue"};
  for (int i = 0; i < 3; i++) {
   final int idx = i; LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
   r.addView(Ui.text(a, names[i], 13.5f, Ui.ink, true), Ui.lp(Ui.dp(56), -2));
   android.widget.SeekBar sb = new android.widget.SeekBar(a); sb.setMax(255); sb.setProgress(rgb[i]); r.addView(sb, Ui.weight(1));
   sb.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
    public void onProgressChanged(android.widget.SeekBar s, int p, boolean u) { rgb[idx] = p; draw.run(); }
    public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {}
   });
   c.addView(r);
  }
  draw.run();
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  TextView use = Ui.button(a, "Use this colour", true); c.addView(use, Ui.lpm(-1, -2, 0, 18, 0, 0));
  Ui.tap(use, () -> { a.db.setPref("accentHex", MainActivity.hexOf(0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2])); a.db.setPref("accent", "Custom"); a.applyTheme(); a.build(); a.closeAll(); });
  TextView back = Ui.button(a, "Back to named colours", false); c.addView(back, Ui.lpm(-1, -2, 0, 10, 0, 0));
  Ui.tap(back, () -> { if (a.db.pref("accent", "Lime").equals("Custom")) { a.db.setPref("accent", "Lime"); a.applyTheme(); a.build(); } a.close(ref[0]); });
  ref[0] = Sheets.show(a, c);
 }
}
