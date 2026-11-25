package in.aicon.hisab;

import android.content.Context;
import android.database.Cursor;
import android.widget.LinearLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A short note per day kept in the same local database, and a once-a-month summary notification. */
final class Diary {
 private Diary() {}

 static final class Note { String day, text; }

 static String get(Store db, String day) { return db.pref("diary:" + day, ""); }

 static void set(Store db, String day, String text) {
  String t = text == null ? "" : text.trim();
  if (t.length() > 500) t = t.substring(0, 500);
  if (t.isEmpty()) { db.getWritableDatabase().delete("prefs", "k=?", new String[]{"diary:" + day}); return; }
  db.setPref("diary:" + day, t);
 }

 /** Notes newest first. */
 static List<Note> all(Store db) {
  List<Note> out = new ArrayList<>();
  try (Cursor c = db.getReadableDatabase().rawQuery("SELECT k, v FROM prefs WHERE k LIKE 'diary:%' ORDER BY k DESC", null)) {
   while (c.moveToNext()) { Note n = new Note(); n.day = c.getString(0).substring(6); n.text = c.getString(1); out.add(n); }
  }
  return out;
 }

 static void open(MainActivity a) {
  Store db = a.db; String today = LocalDate.now().toString();
  Kit.page(a, "Money diary", "plus", () -> edit(a, today), c -> {
   List<Note> notes = all(db);
   if (notes.isEmpty()) { c.addView(Ui.text(a, "Write a line about your day with money. Tap + to add today's note.", 14, Ui.muted, false)); return; }
   LinearLayout card = Ui.card(a);
   for (int i = 0; i < notes.size(); i++) { final Note n = notes.get(i); if (i > 0) card.addView(Ui.divider(a));
    LinearLayout m = Ui.col(a); m.setPadding(0, Ui.dp(11), 0, Ui.dp(11));
    m.addView(Ui.text(a, n.day, 12, Ui.muted, true)); m.addView(Ui.text(a, n.text, 15, Ui.ink, false));
    Ui.tap(m, () -> edit(a, n.day)); card.addView(m);
   }
   c.addView(card);
  });
 }

 static void edit(MainActivity a, String day) {
  Store db = a.db;
  Sheets.form(a, "Diary for " + day, new String[]{"Note"}, new String[]{get(db, day)}, new int[]{Sheets.TEXT}, "Save", v -> { set(db, day, v[0]); a.closeAll(); open(a); }, get(db, day).isEmpty() ? null : () -> { set(db, day, ""); a.closeAll(); open(a); });
 }

 /** Text of last month's summary, or null when there was nothing recorded. */
 static String summary(Store db, YearMonth m) {
  long inc = 0, out = 0; java.util.Map<Long, Long> by = new java.util.HashMap<>();
  for (Store.Tx t : db.txs(Fmt.from(m), Fmt.to(m))) {
   if (t.kind.equals("income")) inc += t.amount;
   else if (t.kind.equals("expense")) { out += t.amount; by.merge(t.cat, t.amount, Long::sum); }
  }
  if (inc == 0 && out == 0) return null;
  long best = 0, bestCat = 0; for (java.util.Map.Entry<Long, Long> e : by.entrySet()) if (e.getValue() > best) { best = e.getValue(); bestCat = e.getKey(); }
  Store.Cat cat = db.categoryMap().get(bestCat);
  String s = "Spent " + Fmt.money(out) + ", received " + Fmt.money(inc) + ".";
  if (cat != null && best > 0) s += " Most went on " + cat.name + " (" + Fmt.money(best) + ").";
  return s;
 }

 /** Shows last month's summary once per month, when enabled. Runs when the app opens, nothing in the background. */
 static boolean monthlyCheck(Context c, Store db, LocalDate today) {
  if (c == null || !db.flag("monthSummary", false)) return false;
  YearMonth prev = YearMonth.from(today).minusMonths(1);
  String key = "monthSummaryDone"; if (db.pref(key, "").equals(prev.toString())) return false;
  db.setPref(key, prev.toString());
  String body = summary(db, prev); if (body == null) return false;
  try {
   android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
   if (nm == null) return true;
   if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new android.app.NotificationChannel("monthly_v1", "Monthly summary", android.app.NotificationManager.IMPORTANCE_DEFAULT));
   android.content.Intent open = new android.content.Intent(c, MainActivity.class).putExtra("noLaunch", true);
   android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, "monthly_v1") : new android.app.Notification.Builder(c);
   b.setSmallIcon(R.drawable.notification_hisab).setContentTitle(Fmt.month(prev) + " in review").setContentText(body).setStyle(new android.app.Notification.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(android.app.PendingIntent.getActivity(c, 61, open, android.app.PendingIntent.FLAG_IMMUTABLE));
   nm.notify(61, b.build());
  } catch (Exception ignored) {}
  return true;
 }
}
