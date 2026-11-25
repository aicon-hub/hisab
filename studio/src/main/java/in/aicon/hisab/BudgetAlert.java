package in.aicon.hisab;

import android.content.Context;
import java.time.YearMonth;

/** Local notification the first time a category crosses its monthly budget, at most once per category per month. */
final class BudgetAlert {
 private BudgetAlert() {}

 static boolean check(Context c, Store db, long catId) {
  if (c == null || db == null || catId == 0) return false;
  Long limit = db.budgets().get(catId);
  if (limit == null || limit <= 0) return false;
  YearMonth m = YearMonth.now();
  long used = 0; for (Store.Tx t : db.txs(Fmt.from(m), Fmt.to(m))) if (t.kind.equals("expense") && t.cat == catId) used += t.amount;
  if (used <= limit) return false;
  String key = "budgetAlert:" + catId + ":" + m;
  if (!db.pref(key, "").isEmpty()) return false;
  db.setPref(key, "1");
  Store.Cat cat = db.categoryMap().get(catId);
  String name = cat == null ? "a category" : cat.name;
  try {
   android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
   if (nm == null) return true;
   if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new android.app.NotificationChannel("budget_v1", "Budget alerts", android.app.NotificationManager.IMPORTANCE_DEFAULT));
   android.content.Intent open = new android.content.Intent(c, MainActivity.class).putExtra("noLaunch", true);
   android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, "budget_v1") : new android.app.Notification.Builder(c);
   b.setSmallIcon(R.drawable.notification_hisab).setContentTitle("Over budget in " + name).setContentText(Fmt.money(used) + " spent against the " + Fmt.money(limit) + " " + Fmt.month(m) + " limit.").setAutoCancel(true).setContentIntent(android.app.PendingIntent.getActivity(c, 60, open, android.app.PendingIntent.FLAG_IMMUTABLE));
   nm.notify(60 + (int) (catId % 1000), b.build());
  } catch (Exception ignored) {}
  return true;
 }
}
