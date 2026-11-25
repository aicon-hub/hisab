package in.aicon.hisab;

import android.Manifest;
import android.os.Build;
import android.view.*;
import android.widget.*;

/** One-time ask, in the style Salman liked from Khatabook: what notifications do here, then the system permission dialog. Shown once; Settings -> Reminder covers it afterwards. */
final class NotifPrompt {
 private NotifPrompt() {}

 static void maybeShow(MainActivity a) {
  if (a.getIntent() != null && a.getIntent().getBooleanExtra("noLaunch", false)) return;
  if (Build.VERSION.SDK_INT < 33) return;
  if (a.db.flag("notifAsked", false)) return;
  if (a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) { a.db.setPref("notifAsked", "1"); return; }
  if (!a.layers.isEmpty()) return; // never stack on top of another sheet or page
  a.db.setPref("notifAsked", "1");
  show(a);
 }

 static void show(MainActivity a) {
  LinearLayout c = Sheets.sheetBody(a, null);
  LinearLayout top = Ui.col(a); top.setGravity(Gravity.CENTER_HORIZONTAL); top.setPadding(0, Ui.dp(16), 0, Ui.dp(4));
  top.addView(Ui.badge(a, "bell", 68, Ui.accent, Ui.onAccent));
  TextView t = Ui.big(a, "Allow Hisab notifications & alerts", 21, Ui.ink); t.setGravity(Gravity.CENTER); t.setPadding(0, Ui.dp(16), 0, Ui.dp(6)); top.addView(t);
  TextView s = Ui.text(a, "Stay updated on your entries, dues and backups", 13.5f, Ui.muted, false); s.setGravity(Gravity.CENTER); top.addView(s);
  c.addView(top);
  c.addView(item(a, "bell", "Timely payment reminders", "Alerts for pending dues, bills and your daily check-in"), Ui.lpm(-1, -2, 0, 14, 0, 0));
  c.addView(item(a, "spark", "Instant updates", "Backup results and reminders as they happen"), Ui.lpm(-1, -2, 0, 10, 0, 0));
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  TextView on = Ui.button(a, "Turn on notifications", true); c.addView(on, Ui.lpm(-1, -2, 0, 20, 0, 0));
  Ui.tap(on, () -> { a.close(ref[0]); if (Build.VERSION.SDK_INT >= 33) a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5); });
  TextView later = Ui.button(a, "Not now", false); c.addView(later, Ui.lpm(-1, -2, 0, 10, 0, 0));
  Ui.tap(later, () -> a.close(ref[0]));
  ref[0] = Sheets.show(a, c);
 }

 private static View item(MainActivity a, String icon, String title, String sub) {
  LinearLayout r = Ui.row(a); r.setPadding(Ui.dp(4), Ui.dp(6), Ui.dp(4), Ui.dp(6));
  r.addView(Ui.badge(a, icon, 42, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(42), Ui.dp(42), 0, 0, 14, 0));
  LinearLayout m = Ui.col(a); m.addView(Ui.text(a, title, 15.5f, Ui.ink, true));
  TextView s = Ui.text(a, sub, 12.5f, Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s);
  r.addView(m, Ui.weight(1));
  return r;
 }
}
