package in.aicon.pocket;
import android.content.*;import android.app.*;import org.json.*;import java.time.*;
/** Device-private commitments. No contact, amount or name enters alarm extras. */
final class DuePlans {
 static final String ACTION="in.aicon.pocket.DUE";static final int PERMISSION=75;
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("hisab-device-due-plans",0);}
 static String key(String scope,String uuid){return ContactLinks.key(scope,uuid);}
 static JSONObject get(Context c,String key){try{JSONObject j=new JSONObject(prefs(c).getString(key,"{}"));boolean legacy=j.has("autoSms")||j.has("smsStatus")||j.has("attempt");if(legacy){for(String field:new String[]{"autoSms","smsStatus","attempt","phone","sim","uuid","person","kind","maximum","currency","contactKey","database","subject"})j.remove(field);prefs(c).edit().putString(key,j.toString()).commit();}return j;}catch(Exception e){return new JSONObject();}}
 static void put(Context c,String key,String scope,String date,boolean reminder)throws Exception{LocalDate due=LocalDate.parse(date);if(due.getYear()<1900||due.getYear()>2199)throw new IllegalArgumentException("Invalid date");JSONObject j=new JSONObject().put("scope",scope).put("date",date).put("reminder",reminder).put("notified",false);if(!prefs(c).edit().putString(key,j.toString()).commit())throw new IllegalStateException("Could not save due date");schedule(c);}
 static void remove(Context c,String key){prefs(c).edit().remove(key).apply();schedule(c);}
 static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,793,new Intent(c,DueReminderReceiver.class).setAction(ACTION),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static long when(String date){return LocalDate.parse(date).plusDays(1).atTime(8,30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}
 static void schedule(Context c){AlarmManager am=c.getSystemService(AlarmManager.class);try{am.cancel(alarm(c));if(!ReminderSchedule.allowed(c))return;long earliest=Long.MAX_VALUE;for(String key:prefs(c).getAll().keySet()){JSONObject j=get(c,key);if(j.optBoolean("reminder")&&!j.optBoolean("notified"))try{earliest=Math.min(earliest,when(j.getString("date")));}catch(Exception ignored){}}if(earliest!=Long.MAX_VALUE)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,Math.max(earliest,System.currentTimeMillis()+1000),alarm(c));}catch(SecurityException ignored){}}
 static boolean consumeDue(Context c,long now){boolean any=false;for(String key:prefs(c).getAll().keySet()){JSONObject j=get(c,key);if(j.optBoolean("reminder")&&!j.optBoolean("notified"))try{if(when(j.getString("date"))<=now){j.put("notified",true);if(!prefs(c).edit().putString(key,j.toString()).commit())continue;any=true;}}catch(Exception ignored){}}return any;}
}
