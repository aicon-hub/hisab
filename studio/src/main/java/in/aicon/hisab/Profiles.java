package in.aicon.hisab;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Up to two separate ledgers on one phone. The first keeps its original database file, untouched. */
final class Profiles {
 private Profiles() {}

 static final int MAX = 2;
 private static final String FILE_PREFS = "hisab_profiles";
 private static volatile int current = 0;

 static SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences(FILE_PREFS, Context.MODE_PRIVATE); }

 /** Database file for a profile index. Index 0 is the original file name, never changed. */
 static String dbName(int index) { return index == 1 ? "hisab_profile2.db" : "hisab_studio.db"; }

 /** Name of the Drive backup file for the active profile, so profile 2 never overwrites profile 1's backup. */
 static String driveFile() { return current == 1 ? "hisab-backup-2.zip" : "hisab-backup.zip"; }

 static boolean hasSecond(Context c) { return prefs(c).getBoolean("has2", false); }

 static int active(Context c) {
  int i = hasSecond(c) && prefs(c).getInt("active", 0) == 1 ? 1 : 0;
  current = i; return i;
 }

 /** The database file name for the active profile; used by Store. */
 static String dbName(Context c) { return dbName(active(c)); }

 static String defaultName(int index) { return index == 1 ? "Work" : "Personal"; }

 static String name(Context c, int index) {
  String n = prefs(c).getString("name" + index, "");
  return n.isEmpty() ? defaultName(index) : n;
 }

 /** Trimmed, single-spaced, at most 20 characters; null when empty. */
 static String cleanName(String s) {
  if (s == null) return null;
  String t = s.replaceAll("\\s+", " ").trim();
  if (t.length() > 20) t = t.substring(0, 20).trim();
  return t.isEmpty() ? null : t;
 }

 static boolean rename(Context c, int index, String raw) {
  String n = cleanName(raw); if (n == null || index < 0 || index >= MAX || (index == 1 && !hasSecond(c))) return false;
  prefs(c).edit().putString("name" + index, n).apply(); return true;
 }

 static void setActive(Context c, int index) {
  if (index == 1 && !hasSecond(c)) return;
  prefs(c).edit().putInt("active", index == 1 ? 1 : 0).commit(); current = index == 1 ? 1 : 0;
 }

 /** Adds the second profile (once), makes it active and gives it an empty ledger without demo data. Returns false if it already exists. */
 static boolean addSecond(Context c) {
  if (hasSecond(c)) return false;
  prefs(c).edit().putBoolean("has2", true).putInt("active", 1).commit(); current = 1;
  Store s = new Store(c); s.setPref("sample", "0"); s.seedBasics();
  return true;
 }

 static void open(MainActivity a) {
  Kit.page(a, "Profiles", null, null, c -> {
   TextView hint = Ui.text(a, "Each profile is a separate ledger on this phone. Entries, accounts and settings are not shared between them. Backups and exports cover the active profile only.", 13.5f, Ui.muted, false); hint.setPadding(0, 0, 0, Ui.dp(12)); c.addView(hint);
   LinearLayout card = Ui.card(a); int act = active(a); int n = hasSecond(a) ? 2 : 1;
   for (int i = 0; i < n; i++) { final int idx = i; if (i > 0) card.addView(Ui.divider(a));
    LinearLayout row = Ui.col(a); row.setPadding(0, Ui.dp(12), 0, Ui.dp(12));
    row.addView(Ui.text(a, name(a, idx), 15.5f, Ui.ink, true)); row.addView(Ui.text(a, idx == act ? "Active. Tap to rename." : "Tap to switch to this profile. You can also tap the profile name on Home.", 12.5f, Ui.muted, false));
    Ui.tap(row, () -> { if (idx == act) Sheets.form(a, "Rename profile", new String[]{"Name"}, new String[]{name(a, idx)}, new int[]{Sheets.TEXT}, "Save", v -> { if (rename(a, idx, v[0])) { a.closeAll(); a.refresh(); open(a); } }, null); else { a.switchProfile(idx); } });
    card.addView(row);
   }
   c.addView(card);
   if (!hasSecond(a)) {
    TextView add = Ui.button(a, "Add a second profile", true); c.addView(add, Ui.lpm(-1, -2, 0, 16, 0, 0));
    Ui.tap(add, () -> Sheets.confirm(a, "Add a second profile?", "It starts empty. Your current profile and its entries are not touched, and you can switch back any time.", "Add", () -> { if (addSecond(a)) { a.getIntent().putExtra("profileFade", true); a.closeAll(); a.recreate(); } }));
   }
  });
 }
}
