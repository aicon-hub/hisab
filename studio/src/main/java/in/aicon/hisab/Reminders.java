package in.aicon.hisab;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import java.util.Calendar;

/** Optional daily nudge at a time you choose. Inexact alarm, nothing runs in the background otherwise. */
public final class Reminders extends BroadcastReceiver {
 static final String CH = "daily_v1";
 static void schedule(Context c, Store db) {
  AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE); PendingIntent pi = pending(c);
  if (am == null) return; am.cancel(pi);
  if (!db.flag("remind", false)) return;
  int mins = Integer.parseInt(db.pref("remindAt", "1230")); Calendar t = Calendar.getInstance(); t.set(Calendar.HOUR_OF_DAY, mins / 60); t.set(Calendar.MINUTE, mins % 60); t.set(Calendar.SECOND, 0);
  if (t.getTimeInMillis() <= System.currentTimeMillis() + 60000) t.add(Calendar.DAY_OF_YEAR, 1);
  am.setAndAllowWhileIdle(AlarmManager.RTC, t.getTimeInMillis(), pi);
 }
 static PendingIntent pending(Context c) { return PendingIntent.getBroadcast(c, 7, new Intent(c, Reminders.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT); }
 @Override public void onReceive(Context c, Intent i) {
  Store db = new Store(c);
  if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) { schedule(c, db); QuickAddNotif.apply(c, db); return; }
  if (!db.flag("remind", false)) return;
  NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
  if (Build.VERSION.SDK_INT >= 26) {
   NotificationChannel ch = new NotificationChannel(CH, "Daily check", NotificationManager.IMPORTANCE_DEFAULT);
   ch.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.hisab_note), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()); nm.createNotificationChannel(ch);
  }
  Intent open = new Intent(c, MainActivity.class).putExtra("noLaunch", true);
  Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
  b.setSmallIcon(R.drawable.notification_hisab).setContentTitle("Anything to note today?").setContentText("Add what you spent or received in a few seconds.").setAutoCancel(true).setContentIntent(PendingIntent.getActivity(c, 8, open, PendingIntent.FLAG_IMMUTABLE));
  nm.notify(8, b.build()); schedule(c, db);
 }
}
