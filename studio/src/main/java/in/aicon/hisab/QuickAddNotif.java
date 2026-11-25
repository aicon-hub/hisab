package in.aicon.hisab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Persistent quick-add notification: always in the shade, one tap opens the new-entry sheet. */
final class QuickAddNotif {
 private QuickAddNotif() {}

 static final String CH = "quick_add_v1";
 static final int ID = 71;

 static boolean on(Store db) { return db.flag("quickAddNotif", false); }

 static void apply(Context c, Store db) {
  NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
  if (nm == null) return;
  if (!on(db)) { nm.cancel(ID); return; }
  if (Build.VERSION.SDK_INT >= 26) {
   NotificationChannel ch = new NotificationChannel(CH, "Quick add", NotificationManager.IMPORTANCE_LOW);
   ch.setSound(null, null);
   nm.createNotificationChannel(ch);
  }
  Intent tap = new Intent(c, MainActivity.class).putExtra("quick", "expense").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
  PendingIntent pi = PendingIntent.getActivity(c, 7, tap, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
  b.setSmallIcon(R.drawable.notification_hisab)
   .setContentTitle("Quick add")
   .setContentText("Tap to note an expense")
   .setContentIntent(pi)
   .setOngoing(true)
   .setShowWhen(false);
  nm.notify(ID, b.build());
 }
}
