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

 static View item(MainActivity a, String icon, String title, String value, Runnable tap) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(13), 0, Ui.dp(13)); r.addView(Ui.icon(a, icon, 22, Ui.ink), Ui.lpm(Ui.dp(22), Ui.dp(22), 0, 0, 16, 0)); r.addView(Ui.text(a, title, 15.5f, Ui.ink, true), Ui.weight(1));
  if (value != null) { TextView v = Ui.text(a, value, 13.5f, Ui.muted, false); v.setPadding(0, 0, Ui.dp(6), 0); r.addView(v); } r.addView(Ui.icon(a, "right", 18, Ui.muted)); Ui.tap(r, tap); return r;
 }
 static void choose(MainActivity a, String title, String key, String[] options, String current, boolean rebuild) {
  List<String> o = Arrays.asList(options); Sheets.pick(a, title, o, null, o.indexOf(current), i -> { a.db.setPref(key, o.get(i)); if (rebuild) { a.applyTheme(); a.build(); } else a.refresh(); a.closeAll(); open(a); });
 }

 static void open(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Settings", null, null, c -> {
   // profile
   LinearLayout p = Ui.row(a); p.setBackground(Ui.shape(Ui.surface, 26)); p.setPadding(Ui.dp(18), Ui.dp(16), Ui.dp(18), Ui.dp(16));
   String nm = db.pref("name", "You"); FrameLayout av = new FrameLayout(a); av.setBackground(Ui.shape(Ui.accent, 28)); av.addView(Ui.big(a, nm.substring(0, 1).toUpperCase(Locale.ROOT), 24, Ui.onAccent), new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)); p.addView(av, Ui.lpm(Ui.dp(56), Ui.dp(56), 0, 0, 16, 0));
   LinearLayout m = Ui.col(a); m.addView(Ui.big(a, nm, 20, Ui.ink)); TextView s = Ui.text(a, "Stored only on this phone", 13, Ui.muted, false); s.setPadding(0, Ui.dp(5), 0, 0); m.addView(s); p.addView(m, Ui.weight(1)); p.addView(Ui.icon(a, "edit", 20, Ui.muted));
   Ui.tap(p, () -> Sheets.form(a, "Your name", new String[]{"Name"}, new String[]{nm}, new int[]{Sheets.TEXT}, "Save", v -> { if (!v[0].isEmpty()) { db.setPref("name", v[0]); a.refresh(); a.closeAll(); open(a); } }, null)); c.addView(p);

   c.addView(Kit.sectionHead(a, "Look", null, null)); LinearLayout look = Ui.card(a); look.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   look.addView(item(a, "moon", "Appearance", db.pref("appearance", "System"), () -> choose(a, "Appearance", "appearance", new String[]{"System", "Light", "Dark"}, db.pref("appearance", "System"), true))); look.addView(Ui.divider(a));
   LinearLayout sw = Ui.row(a); sw.setPadding(0, Ui.dp(14), 0, Ui.dp(14)); sw.addView(Ui.text(a, "Accent", 15.5f, Ui.ink, true), Ui.weight(1)); String cur = db.pref("accent", "Lime");
   for (int i = 0; i < Ui.ACCENT_NAMES.length; i++) { final String nmx = Ui.ACCENT_NAMES[i]; View d = new View(a); boolean on = nmx.equals(cur); d.setBackground(on ? Ui.outline(Ui.accentFor(a, nmx), Ui.ink, 14, 2.2f) : Ui.shape(Ui.accentFor(a, nmx), 14)); sw.addView(d, Ui.lpm(Ui.dp(28), Ui.dp(28), 4, 0, 0, 0)); d.setContentDescription(nmx); Ui.tap(d, () -> { db.setPref("accent", nmx); a.applyTheme(); a.build(); a.closeAll(); open(a); }); }
   look.addView(sw); look.addView(Ui.divider(a));
   LinearLayout pb = Ui.row(a); pb.setPadding(0, Ui.dp(13), 0, Ui.dp(13)); pb.addView(Ui.text(a, "Pure black in dark mode", 15.5f, Ui.ink, true), Ui.weight(1)); Switch sh = new Switch(a); sh.setChecked(db.flag("pureBlack", false)); pb.addView(sh); sh.setOnCheckedChangeListener((v, on) -> { db.setPref("pureBlack", on ? "1" : "0"); a.applyTheme(); a.build(); a.closeAll(); open(a); }); look.addView(pb); look.addView(Ui.divider(a));
   look.addView(item(a, "edit", "Font", db.pref("font", "Inter"), () -> choose(a, "Font", "font", new String[]{"Inter", "System", "Lora", "Poppins"}, db.pref("font", "Inter"), true))); c.addView(look);

   c.addView(Kit.sectionHead(a, "Money", null, null)); LinearLayout mo = Ui.card(a); mo.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   mo.addView(item(a, "wallet", "Accounts", null, () -> Pages.accounts(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "other", "Categories", null, () -> Pages.categories(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "target", "Budgets", null, () -> Pages.budgets(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "flame", "Goals", null, () -> Pages.goals(a))); mo.addView(Ui.divider(a)); mo.addView(item(a, "repeat", "Bills and repeats", null, () -> Pages.recurring(a))); mo.addView(Ui.divider(a));
   mo.addView(item(a, "coin", "Currency", db.pref("currency", "INR"), () -> choose(a, "Currency", "currency", Fmt.CURRENCIES, db.pref("currency", "INR"), true))); mo.addView(Ui.divider(a));
   mo.addView(item(a, "ledger", "Number grouping", db.pref("grouping", "Indian"), () -> choose(a, "Number grouping", "grouping", new String[]{"Indian", "International", "None"}, db.pref("grouping", "Indian"), true))); c.addView(mo);

   c.addView(Kit.sectionHead(a, "Reminder", null, null)); LinearLayout rm = Ui.card(a); rm.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   LinearLayout rr = Ui.row(a); rr.setPadding(0, Ui.dp(14), 0, Ui.dp(14)); rr.addView(Ui.icon(a, "bell", 22, Ui.ink), Ui.lpm(Ui.dp(22), Ui.dp(22), 0, 0, 16, 0)); rr.addView(Ui.text(a, "Daily check", 15.5f, Ui.ink, true), Ui.weight(1)); Switch rs = new Switch(a); rs.setChecked(db.flag("remind", false)); rr.addView(rs);
   rs.setOnCheckedChangeListener((v, on) -> { db.setPref("remind", on ? "1" : "0"); if (on && Build.VERSION.SDK_INT >= 33) a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5); Reminders.schedule(a, db); }); rm.addView(rr); rm.addView(Ui.divider(a));
   int mins = Integer.parseInt(db.pref("remindAt", "1230")); rm.addView(item(a, "calendar", "Time", String.format(Locale.US, "%d:%02d %s", (mins / 60 + 11) % 12 + 1, mins % 60, mins / 60 >= 12 ? "PM" : "AM"), () -> new android.app.TimePickerDialog(a, (v, h, mi) -> { db.setPref("remindAt", "" + (h * 60 + mi)); Reminders.schedule(a, db); a.closeAll(); open(a); }, mins / 60, mins % 60, false).show())); c.addView(rm);

   c.addView(Kit.sectionHead(a, "Your data", null, null)); LinearLayout da = Ui.card(a); da.setPadding(Ui.dp(18), Ui.dp(6), Ui.dp(18), Ui.dp(6));
   da.addView(item(a, "share", "Export entries as CSV", null, () -> exportCsv(a))); da.addView(Ui.divider(a));
   da.addView(item(a, "trash", "Clear sample data", db.flag("sample", false) ? "Sample is on" : "Not in use", () -> Sheets.confirm(a, "Clear everything?", "This removes every entry, person, budget and goal on this phone, and starts you with empty cash and bank accounts.", "Clear", () -> { db.wipe(); db.seedBasics(); db.setPref("sample", "0"); a.closeAll(); a.refresh(); a.toast("Cleared", null); })));
   if (!db.flag("sample", false)) { da.addView(Ui.divider(a)); da.addView(item(a, "spark", "Load sample data", null, () -> Sheets.confirm(a, "Replace with sample data?", "Your current entries on this phone will be replaced.", "Replace", () -> { Sample.seed(db); a.closeAll(); a.refresh(); }))); }
   c.addView(da);

   c.addView(Kit.sectionHead(a, "About", null, null)); LinearLayout ab = Ui.card(a);
   ab.addView(Ui.text(a, "Hisab by AiCoN", 16, Ui.ink, true)); TextView t = Ui.text(a, "Free and open source under GPL-3.0. No ads, no tracking, no server. Your entries stay on this phone. Hisab never charges or adds interest.", 13.5f, Ui.muted, false); t.setPadding(0, Ui.dp(8), 0, Ui.dp(10)); ab.addView(t);
   ab.addView(Ui.text(a, "Version 0.4.0 test build", 12.5f, Ui.muted, true)); c.addView(ab);
  });
 }

 static void exportCsv(MainActivity a) {
  Store db = a.db; Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> ppl = Kit.peopleMap(db);
  StringBuilder b = new StringBuilder("Date,Type,Amount,Account,To account,Category,Person,Note\n");
  for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) { b.append(t.day).append(',').append(t.kind).append(',').append(Ledger.plain(t.amount)).append(',').append(Ledger.csv(accs.get(t.account) == null ? "" : accs.get(t.account).name)).append(',').append(Ledger.csv(accs.get(t.dest) == null ? "" : accs.get(t.dest).name)).append(',').append(Ledger.csv(cats.get(t.cat) == null ? "" : cats.get(t.cat).name)).append(',').append(Ledger.csv(ppl.get(t.person) == null ? "" : ppl.get(t.person).name)).append(',').append(Ledger.csv(t.note)).append('\n'); }
  Intent i = new Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_TEXT, b.toString()).putExtra(Intent.EXTRA_SUBJECT, "Hisab entries");
  a.startActivity(Intent.createChooser(i, "Export entries"));
 }
}
