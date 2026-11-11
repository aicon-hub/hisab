package in.aicon.pocket;
import android.app.*;import android.content.*;import android.os.*;import java.util.*;
/** Local, one-shot wall-clock reminders. Inexact idle-aware alarms need no
 * exact-alarm special access and may be delayed by Android battery controls. */
final class ReminderSchedule {
 static final String CHANNEL="hisab-ledger-reminders-v1";static final int PERMISSION=73;
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("hisab-reminders",0);}
 static int time(Context c,int slot){return prefs(c).getInt("time"+slot,slot==0?510:1230);}
 static long next(long now,int minute,TimeZone zone){if(minute<0||minute>=1440)throw new IllegalArgumentException("Invalid reminder time");Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(now);c.set(Calendar.HOUR_OF_DAY,minute/60);c.set(Calendar.MINUTE,minute%60);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=now)c.add(Calendar.DATE,1);return c.getTimeInMillis();}
 static PendingIntent intent(Context c,int slot){return PendingIntent.getBroadcast(c,790+slot,new Intent(c,ReminderReceiver.class).setAction("in.aicon.pocket.REMINDER").putExtra("slot",slot),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static boolean allowed(Context c){return (Build.VERSION.SDK_INT<33||c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)&&c.getSystemService(NotificationManager.class).areNotificationsEnabled();}
 static void schedule(Context c){AlarmManager am=c.getSystemService(AlarmManager.class);try{for(int slot=0;slot<2;slot++){PendingIntent pi=intent(c,slot);am.cancel(pi);if(prefs(c).getBoolean("enabled",false)&&allowed(c))am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next(System.currentTimeMillis(),time(c,slot),TimeZone.getDefault()),pi);}}catch(SecurityException blocked){prefs(c).edit().putBoolean("enabled",false).apply();for(int slot=0;slot<2;slot++)try{am.cancel(intent(c,slot));}catch(SecurityException ignored){}}}
 static void enabled(Context c,boolean on){prefs(c).edit().putBoolean("enabled",on).apply();schedule(c);if(!on)c.getSystemService(NotificationManager.class).cancel(790);}
 static void channel(Context c){NotificationManager nm=c.getSystemService(NotificationManager.class);NotificationChannel channel=new NotificationChannel(CHANNEL,"Hisab daily reminders",NotificationManager.IMPORTANCE_DEFAULT);channel.setDescription("Optional morning and evening reminders to update your ledger");android.net.Uri tone=android.net.Uri.parse("android.resource://"+c.getPackageName()+"/"+in.aicon.pocket.test.R.raw.hisab_note);channel.setSound(tone,new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());nm.createNotificationChannel(channel);}
}
