package in.aicon.pocket;
import android.app.*;import android.content.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import android.telephony.*;import org.json.*;
/** Experimental sideload-only, explicit per-payment SMS grant. Never WhatsApp automation. */
final class AutoSms {
 static final int PERMISSION=76;
 static int subscription(){return SubscriptionManager.getDefaultSmsSubscriptionId();}
 static String message(String currency,long remaining,String kind,String date){return "Hisab reminder: "+currency+" "+Ledger.number(remaining)+" remains unpaid "+(kind.equals("lent")?"from money I lent you":"from money I borrowed from you")+". Recorded pay-by date: "+date+". Please confirm or correct.";}
 static void run(Context c,String key,JSONObject j){if(!j.optBoolean("autoSms"))return;String status="Not sent: permission, account, contact or payment changed. Review manually.";try{
  if(c.checkSelfPermission(android.Manifest.permission.SEND_SMS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new IllegalStateException();
  int sim=j.getInt("sim");if(!SubscriptionManager.isValidSubscriptionId(sim)||sim!=subscription())throw new IllegalStateException();
  if(!j.getString("phone").equals(ContactLinks.get(c,j.getString("contactKey")).optString("phone")))throw new IllegalStateException();
  String selected=new AccountDirectory(c).selectedSubject();if(!java.util.Objects.equals(j.optString("subject",""),selected==null?"":selected))throw new IllegalStateException();if(selected!=null)throw new IllegalStateException();String file=j.getString("database");if(!file.equals("pocket.db")&&!file.matches("hisab-google-[a-f0-9]{64}\\.db"))throw new IllegalStateException();
  String kind,person;long remaining;try(SQLiteDatabase db=SQLiteDatabase.openDatabase(c.getDatabasePath(file).getPath(),null,SQLiteDatabase.OPEN_READONLY);Cursor row=db.rawQuery("SELECT kind,person,amount-coalesce((SELECT sum(p.amount) FROM tx p WHERE p.loan=t.uuid),0) FROM tx t WHERE uuid=? AND kind IN ('lent','borrowed')",new String[]{j.getString("uuid")})){if(!row.moveToFirst())throw new IllegalStateException();kind=row.getString(0);person=row.getString(1);remaining=row.getLong(2);}
  if(remaining<=0){status="Not sent: payment is settled.";return;}if(remaining>j.getLong("maximum")||!kind.equals(j.getString("kind"))||!person.equals(j.getString("person")))throw new IllegalStateException();
  String text=message(j.getString("currency"),remaining,kind,j.getString("date"));SmsManager manager=android.os.Build.VERSION.SDK_INT>=31?c.getSystemService(SmsManager.class).createForSubscriptionId(sim):SmsManager.getSmsManagerForSubscriptionId(sim);if(manager.divideMessage(text).size()!=1){status="Not sent: message would cost more than one SMS segment.";return;}
  // Commit before attempting, so a process restart never repeats an uncertain send.
  String attempt=java.util.UUID.randomUUID().toString();j.put("attempt",attempt).put("autoSms",false).put("smsStatus","Attempt reserved; outcome unknown. No automatic retry.");if(!DuePlans.prefs(c).edit().putString(key,j.toString()).commit())return;
  Intent result=new Intent(c,SmsResultReceiver.class).setAction("in.aicon.pocket.SMS_RESULT").putExtra("key",key).putExtra("attempt",attempt);PendingIntent sent=PendingIntent.getBroadcast(c,key.hashCode(),result,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);manager.sendTextMessage(j.getString("phone"),null,text,sent,null);status="Handed to Android. Delivery is not confirmed; no automatic retry.";
 }catch(Exception ignored){}finally{try{j.put("autoSms",false).put("smsStatus",status);DuePlans.prefs(c).edit().putString(key,j.toString()).commit();}catch(Exception ignored){}}}
}
