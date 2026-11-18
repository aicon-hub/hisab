package in.aicon.hisab;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Live, opt-in handling of one incoming bank SMS: finds or creates the Hisab account, saves the
 * balance snapshot and any clear debit (with the balance in the note) and can post a local notification.
 * Original code. Runs entirely on this phone; the message text is never stored or uploaded.
 */
final class SmsAuto {
 private SmsAuto() {}
 static final String CH = "hisab_bank_sms";

 /** Result of handling one message. */
 static final class Out { boolean balance, expense; long account; long amount; long bal; boolean hasBal; String bank = "", last4 = ""; boolean created; }

 static boolean live(Store db) { return db.flag("smsEnabled", false) && db.flag("smsLive", false); }

 /** Handle one SMS. Bank senders only: unknown senders parse to nothing and are ignored. */
 static synchronized Out handle(Store db, String sender, String body, long when) {
  Out out = new Out();
  if (!db.flag("smsEnabled", false) || sender == null || body == null) return out;
  if (when > System.currentTimeMillis() + 300000L) return out;
  String custom = db.pref("smsSender:" + BankSms.senderKey(sender), "");
  String c = custom.isEmpty() ? null : custom;
  BankSms.Result r = BankSms.parse(sender, body, c);
  SmsTx.T t = db.flag("smsTxEnabled", false) ? SmsTx.parse(sender, body, c) : null;
  String bank = r != null ? r.bank : t != null ? t.bank : null;
  String last4 = r != null ? r.last4 : t != null ? t.last4 : null;
  if (bank == null) return out;
  if (r != null && t != null && !r.last4.equals(t.last4)) t = null;
  String key = bank + ":" + last4;
  long account = 0;
  try { account = Long.parseLong(db.pref("smsMap:" + key, "0")); } catch (Exception ignored) {}
  if (db.account(account) == null) {
   if (!db.flag("smsAutoLink", false)) return out;
   // Opening balance is chosen so the ledger equals the bank balance once this message is saved.
   long opening = r != null ? r.balance + (t != null ? t.amount : 0) : 0;
   account = db.addAccount(bank + " ••" + last4, "bank", opening);
   db.setPref("smsMap:" + key, "" + account);
   out.created = true;
  }
  out.account = account; out.bank = bank; out.last4 = last4;
  if (r != null) { out.balance = BankSms.apply(db, sender, body, when); out.hasBal = true; out.bal = r.balance; }
  if (t != null) { out.amount = t.amount; out.expense = SmsTx.apply(db, sender, body, when, r != null ? r.balance : null); }
  return out;
 }

 static void notifyOut(Context c, Store db, Out o) {
  if (!db.flag("smsNotify", false) || !(o.expense || o.balance || o.created)) return;
  try {
   Store.Acc acc = db.account(o.account);
   boolean hide = acc != null && Privacy.hidden(db, acc);
   String label = o.bank + " ••" + o.last4;
   String title = o.expense ? "Expense added: " + (hide ? "••••" : BankSms.inr(o.amount)) : "Bank balance updated";
   String text = label + (o.hasBal ? " · balance " + (hide ? "••••" : BankSms.inr(o.bal)) : "") + (o.created ? " · account created" : "");
   NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
   if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CH, "Bank SMS", NotificationManager.IMPORTANCE_LOW));
   Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
   Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
   b.setSmallIcon(R.drawable.notification_hisab).setContentTitle(title).setContentText(text).setAutoCancel(true);
   if (open != null) b.setContentIntent(PendingIntent.getActivity(c, 950, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
   nm.notify(950 + (int) (System.currentTimeMillis() % 40), b.build());
  } catch (Exception ignored) {}
 }
}
