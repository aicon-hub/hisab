package in.aicon.hisab;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.widget.*;
import java.util.*;

/**
 * Optional reminders to people who owe you money. One inexact alarm a day; nothing else runs.
 * WhatsApp: a notification opens a chat with the message ready - Android does not let apps send
 * WhatsApp messages by themselves. SMS: the phone sends the message itself when SMS is chosen and
 * the send permission is granted. Original code.
 */
public final class AutoRemind extends BroadcastReceiver {
 static final String CH = "due_remind_v1";
 static final int[] DAY_OPTIONS = {3, 7, 14, 30};
 static final int MAX_PER_RUN = 3;

 static void schedule(Context c, Store db) {
  AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE); PendingIntent pi = pending(c);
  if (am == null) return; am.cancel(pi);
  if (!db.flag("autoRemind", false)) return;
  Calendar t = Calendar.getInstance(); t.set(Calendar.HOUR_OF_DAY, 10); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0);
  if (t.getTimeInMillis() <= System.currentTimeMillis() + 60000) t.add(Calendar.DAY_OF_YEAR, 1);
  am.setAndAllowWhileIdle(AlarmManager.RTC, t.getTimeInMillis(), pi);
 }
 static PendingIntent pending(Context c) { return PendingIntent.getBroadcast(c, 41, new Intent(c, AutoRemind.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT); }

 static long intervalMillis(Store db) {
  long days; try { days = Long.parseLong(db.pref("autoRemindDays", "7")); } catch (Exception e) { days = 7; }
  return Math.max(1, days) * 86400000L;
 }

 /** People due a reminder right now: they owe you, have a phone number, and were not reminded within the chosen interval. */
 static List<Store.Person> due(Store db, long now) {
  List<Store.Person> out = new ArrayList<>();
  if (!db.flag("autoRemind", false)) return out;
  long gap = intervalMillis(db);
  for (Store.Person p : db.people()) {
   if (db.net(p.id) <= 0 || p.phone.isEmpty()) continue;
   long last; try { last = Long.parseLong(db.pref("autoReminded:" + p.id, "0")); } catch (Exception e) { last = 0; }
   if (now - last >= gap) out.add(p);
  }
  return out;
 }

 @Override public void onReceive(Context c, Intent i) {
  Store db = new Store(c);
  try {
   if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) { schedule(c, db); return; }
   run(c, db); schedule(c, db);
  } finally { db.close(); }
 }

 static void run(Context c, Store db) {
  List<Store.Person> list = due(db, System.currentTimeMillis());
  if (list.isEmpty()) return;
  String channel = db.pref("autoRemindChannel", "WhatsApp");
  NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
  if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CH, "Money due reminders", NotificationManager.IMPORTANCE_DEFAULT));
  int n = 0;
  for (Store.Person p : list) {
   if (n >= MAX_PER_RUN) break; // the rest are picked up on the next daily check
   long net = db.net(p.id);
   String text = PeoplePage.reminderText(db, p, net);
   boolean sms = channel.equals("SMS") && c.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
   if (sms && sendSms(c, p.phone, text)) db.setPref("autoReminded:" + p.id, "" + System.currentTimeMillis());
   else { notifyPerson(c, nm, p, net, text, channel.equals("SMS")); db.setPref("autoReminded:" + p.id, "" + System.currentTimeMillis()); }
   n++;
  }
 }

 static boolean sendSms(Context c, String phone, String text) {
  try {
   SmsManager sm = Build.VERSION.SDK_INT >= 31 ? c.getSystemService(SmsManager.class) : SmsManager.getDefault();
   ArrayList<String> parts = sm.divideMessage(text);
   sm.sendMultipartTextMessage(Phone.normalise(phone), null, parts, null, null);
   return true;
  } catch (Exception e) { return false; }
 }

 static void notifyPerson(Context c, NotificationManager nm, Store.Person p, long net, String text, boolean smsChannel) {
  Intent open;
  if (smsChannel) open = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Phone.normalise(p.phone))).putExtra("sms_body", text);
  else open = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + Phone.wa(p.phone) + "?text=" + Uri.encode(text)));
  Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
  b.setSmallIcon(R.drawable.notification_hisab).setContentTitle(p.name + " owes you " + Fmt.money(Math.abs(net)))
   .setContentText(smsChannel ? "Tap to open the SMS with the reminder ready." : "Tap to open WhatsApp with the reminder ready.")
   .setAutoCancel(true).setContentIntent(PendingIntent.getActivity(c, 900 + (int) (p.id % 1000), open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
  nm.notify(900 + (int) (p.id % 1000), b.build());
 }

 static void open(MainActivity a) {
  Kit.page(a, "Automatic reminders", null, null, c -> {
   Store db = a.db;
   c.addView(Ui.text(a, "Remind people who owe you", 18, Ui.ink, true));
   c.addView(Ui.text(a, "Once a day Hisab checks who still owes you and reminds each person at most once every few days. Only people with a phone number are included. Nothing runs in the background otherwise.", 14, Ui.muted, false), Ui.lpm(-1, -2, 0, 12, 0, 16));
   LinearLayout sw = Ui.row(a); sw.setPadding(0, Ui.dp(14), 0, Ui.dp(14)); sw.addView(Ui.text(a, "Automatic due reminders", 15, Ui.ink, true), Ui.weight(1)); Switch on = Ui.toggle(a); on.setChecked(db.flag("autoRemind", false)); sw.addView(on); c.addView(sw);
   on.setOnCheckedChangeListener((v, enabled) -> {
    if (!enabled) { db.setPref("autoRemind", "0"); schedule(a, db); return; }
    Sheets.confirm(a, "Turn on automatic reminders?", "WhatsApp reminders arrive as a daily notification that opens the chat with the message ready - you press send there. If you choose SMS, your phone sends the message itself and your carrier's SMS charges apply. You can turn this off any time.", "Turn on", () -> {
     db.setPref("autoRemind", "1"); schedule(a, db);
     if (Build.VERSION.SDK_INT >= 33) a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 74);
     if (db.pref("autoRemindChannel", "WhatsApp").equals("SMS")) a.requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 73);
     a.refresh();
    }); v.setOnCheckedChangeListener(null); v.setChecked(false);
   });
   long d0; try { d0 = Long.parseLong(db.pref("autoRemindDays", "7")); } catch (Exception e) { d0 = 7; }
   final long days = d0;
   c.addView(Settings.item(a, "calendar", "Remind each person every", days + " days", () -> {
    List<String> labels = new ArrayList<>(); for (int d : DAY_OPTIONS) labels.add(d + " days");
    List<String> cur = new ArrayList<>(); for (int d : DAY_OPTIONS) cur.add("" + d);
    Sheets.pick(a, "Remind every", labels, null, cur.indexOf("" + days), i -> { db.setPref("autoRemindDays", "" + DAY_OPTIONS[i]); a.refresh(); });
   }));
   String channel = db.pref("autoRemindChannel", "WhatsApp");
   c.addView(Settings.item(a, "sms", "Send by", channel, () -> Sheets.pick(a, "Send by", Arrays.asList("WhatsApp", "SMS"), null, channel.equals("SMS") ? 1 : 0, i -> {
    db.setPref("autoRemindChannel", i == 1 ? "SMS" : "WhatsApp");
    if (i == 1 && a.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) a.requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 73);
    a.refresh();
   })));
   c.addView(Ui.text(a, "WhatsApp opens the chat with the message written; Android does not allow apps to send WhatsApp messages automatically, so you press send there. SMS is sent by your phone without another tap when SMS is chosen. Standard carrier SMS charges apply to SMS. Reminders go only to people who owe you right now, never about what you owe others.", 13, Ui.muted, false), Ui.lpm(-1, -2, 0, 14, 0, 12));
   List<Store.Person> waiting = due(db, System.currentTimeMillis());
   c.addView(Ui.text(a, db.flag("autoRemind", false) ? (waiting.isEmpty() ? "Nobody is due a reminder right now." : waiting.size() + (waiting.size() == 1 ? " person is" : " people are") + " due at the next daily check.") : "Automatic reminders are off.", 13, Ui.muted, true), Ui.lpm(-1, -2, 0, 4, 0, 0));
  });
 }
}
