package in.aicon.hisab;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Home screen widget: today's spend, total expense, and a plus that opens a new expense sheet. Refreshes after every change. */
public class HisabWidget extends AppWidgetProvider {

 static long todaySpend(Store db) {
  String today = java.time.LocalDate.now().toString(); long sum = 0;
  for (Store.Tx t : db.txs(today, today)) if (t.kind.equals("expense")) sum += t.amount;
  return sum;
 }

 static long totalExpense(Store db) {
  long sum = 0;
  for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) if (t.kind.equals("expense")) sum += t.amount;
  return sum;
 }

 @Override public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
  Store db = new Store(c);
  String today = Fmt.money(todaySpend(db)), total = Fmt.money(totalExpense(db));
  db.close();
  for (int id : ids) {
   RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_quick);
   v.setTextViewText(R.id.widgetToday, today);
   v.setTextViewText(R.id.widgetTotal, total);
   Intent tap = new Intent(c, MainActivity.class).putExtra("quick", "expense").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
   v.setOnClickPendingIntent(R.id.widgetPlus, PendingIntent.getActivity(c, 11, tap, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
   Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   v.setOnClickPendingIntent(R.id.widgetRoot, PendingIntent.getActivity(c, 12, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
   mgr.updateAppWidget(id, v);
  }
 }

 /** Called after any ledger change so the numbers never lag. */
 static void refresh(Context c) {
  try {
   AppWidgetManager mgr = AppWidgetManager.getInstance(c);
   int[] ids = mgr.getAppWidgetIds(new ComponentName(c, HisabWidget.class));
   if (ids.length > 0) new HisabWidget().onUpdate(c, mgr, ids);
  } catch (Exception ignored) {}
 }
}
