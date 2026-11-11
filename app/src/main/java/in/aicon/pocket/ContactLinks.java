package in.aicon.pocket;
import android.content.*;import org.json.*;
/** Selected contacts only. Kept outside the ledger, Drive sync and backup payload. */
final class ContactLinks {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("hisab-device-contact-links",0);}
 static String key(String scope,String id){try{byte[] b=java.security.MessageDigest.getInstance("SHA-256").digest((scope+":"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 static void save(Context c,String key,String name,String phone)throws Exception{if(name==null||name.length()>160||phone==null||phone.length()>80)throw new IllegalArgumentException("Contact is not usable");String clean=phone.replaceAll("[ ()\\-.]","");if(!clean.matches("\\+?[0-9]{5,20}"))throw new IllegalArgumentException("Choose a contact with a normal mobile number");JSONObject j=new JSONObject().put("name",name).put("phone",clean);if(!prefs(c).edit().putString(key,j.toString()).commit())throw new IllegalStateException("Contact could not be saved");}
 static JSONObject get(Context c,String key){try{return new JSONObject(prefs(c).getString(key,"{}"));}catch(Exception e){return new JSONObject();}}
 static void remove(Context c,String key){prefs(c).edit().remove(key).apply();}
}
