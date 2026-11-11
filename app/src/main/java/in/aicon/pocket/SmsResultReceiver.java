package in.aicon.pocket;
import android.content.*;import android.app.Activity;import org.json.*;
public final class SmsResultReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(!"in.aicon.pocket.SMS_RESULT".equals(i.getAction()))return;String key=i.getStringExtra("key");if(key==null)return;JSONObject j=DuePlans.get(c,key);if(!j.has("date")||!j.optString("attempt").equals(i.getStringExtra("attempt")))return;try{j.put("autoSms",false).put("smsStatus",getResultCode()==Activity.RESULT_OK?"Android reports SMS sent, not delivered. No automatic retry.":"Android reports SMS send failed. Review manually; no automatic retry.");DuePlans.prefs(c).edit().putString(key,j.toString()).commit();}catch(Exception ignored){}}
}
