package in.aicon.hisab;
import android.content.*;
import android.database.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Versioned, authenticated encrypted local backup. Password never persisted. */
final class Backup {
 static final byte[] MAGIC = {'H','I','S','A','B','2',0,1};
 static final int LIMIT=12*1024*1024, ITERATIONS=210000;
 static final String[] TABLES={"prefs","account","category","person","tx","budget","goal","recur"};
 static byte[] crypt(byte[] input,char[] password,boolean encrypt) throws Exception {
  byte[] salt=new byte[16],nonce=new byte[12],payload=input;
  if(encrypt){new SecureRandom().nextBytes(salt);new SecureRandom().nextBytes(nonce);}
  else { if(input.length<52 || input.length>LIMIT || !Arrays.equals(MAGIC,Arrays.copyOf(input,8)))throw new IOException("Not a Hisab encrypted backup");salt=Arrays.copyOfRange(input,8,24);nonce=Arrays.copyOfRange(input,24,36);payload=Arrays.copyOfRange(input,36,input.length); }
  PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
  byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();spec.clearPassword();
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(encrypt?Cipher.ENCRYPT_MODE:Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(MAGIC);
  try {byte[] result=cipher.doFinal(payload);if(!encrypt)return result;ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(MAGIC);out.write(salt);out.write(nonce);out.write(result);return out.toByteArray();}finally{Arrays.fill(key,(byte)0);}
 }
 static byte[] snapshot(Store db) throws Exception {
  JSONObject root=new JSONObject().put("format",2);Db d=db.getReadableDatabase();d.beginTransaction();
  try {for(String table:TABLES){JSONArray rows=new JSONArray();try(Cursor c=d.rawQuery("SELECT * FROM "+table,null)){while(c.moveToNext()){JSONObject row=new JSONObject();for(int i=0;i<c.getColumnCount();i++)row.put(c.getColumnName(i),c.getType(i)==Cursor.FIELD_TYPE_INTEGER?c.getLong(i):c.getString(i));rows.put(row);}}root.put(table,rows);}d.setTransactionSuccessful();}finally{d.endTransaction();}
  byte[] data=root.toString().getBytes(StandardCharsets.UTF_8);if(data.length>LIMIT-100)throw new IOException("Backup too large");return data;
 }
 static void restore(Store db,byte[] data) throws Exception {
  if(data.length>LIMIT)throw new IOException("Backup too large");JSONObject root=new JSONObject(new String(data,StandardCharsets.UTF_8));if(root.getInt("format")!=2)throw new IOException("Unsupported backup version");
  Db d=db.getWritableDatabase();d.beginTransaction();
  try {for(String table:TABLES){JSONArray rows=root.getJSONArray(table);if(rows.length()>50000)throw new IOException("Too many entries");Set<String> cols=new HashSet<>();try(Cursor c=d.rawQuery("SELECT * FROM "+table+" LIMIT 0",null)){Collections.addAll(cols,c.getColumnNames());}
    d.delete(table,null,null);for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(row.length()!=cols.size())throw new IOException("Incomplete record");ContentValues v=new ContentValues();for(String col:cols){Object value=row.get(col);if(value instanceof Number)v.put(col,((Number)value).longValue());else if(value instanceof String && ((String)value).length()<3000000)v.put(col,(String)value);else throw new IOException("Invalid record");}d.insertOrThrow(table,null,v);}}
   // Validate dates, kinds, amounts and references before committing any replacement.
   for(Store.Tx t:db.txs("", "~")){java.time.LocalDate.parse(t.day);if(!Arrays.asList("expense","income","transfer","lent","borrowed","repay_in","repay_out").contains(t.kind)||t.amount<=0||t.amount>999999999999L||db.account(t.account)==null|| (t.kind.equals("transfer")&&(db.account(t.dest)==null||t.dest==t.account)))throw new IOException("Invalid entry");}
   for(Store.Rec r:db.recurring()){java.time.LocalDate.parse(r.next);if(db.account(r.account)==null||r.amount<=0)throw new IOException("Invalid repeat");}
   if(db.accounts().isEmpty())throw new IOException("No accounts in backup");
   for(Store.Cat c:db.categories(null))if(!Arrays.asList("income","expense").contains(c.kind)||c.name.length()>250||!Icons.P.containsKey(c.icon))throw new IOException("Invalid category");
   for(Store.Acc ac:db.accounts())if(!Arrays.asList("cash","bank","card","wallet").contains(ac.kind)||ac.name.length()>250||Math.abs(ac.opening)>999999999999L)throw new IOException("Invalid account");
   for(Store.Person p:db.people())if(p.name.length()>250||p.phone.length()>40)throw new IOException("Invalid person");
   for(Store.Goal g:db.goals())if(g.target<=0||g.saved<0||g.target>999999999999L||g.saved>999999999999L||!Icons.P.containsKey(g.icon))throw new IOException("Invalid goal");
   String[][] enums={{"appearance","System","System,Light,Dark"},{"font","Inter","Inter,System,Lora,Poppins"},{"currency","INR",String.join(",",Fmt.CURRENCIES)},{"grouping","Indian","Indian,International,None"},{"accent","Lime",String.join(",",Ui.ACCENT_NAMES)}};
   for(String[] en:enums)if(!Arrays.asList(en[2].split(",")).contains(db.pref(en[0],en[1])))throw new IOException("Invalid setting");
   int minute=Integer.parseInt(db.pref("remindAt","1230"));if(minute<0||minute>1439)throw new IOException("Invalid reminder");Long.parseLong(db.pref("lastAcc","0"));
   db.setPref("lastBackupAt","0");db.setPref("smsEnabled","0");db.setPref("remind","0");d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
 static byte[] read(InputStream in) throws Exception {if(in==null)throw new IOException("File unavailable");try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=src.read(buf))!=-1){if(out.size()+n>LIMIT)throw new IOException("File too large");out.write(buf,0,n);}return out.toByteArray();}}
 static void password(MainActivity a,boolean exporting){
  LinearLayout c=Sheets.sheetBody(a,exporting?"Encrypted backup":"Restore backup");c.addView(Ui.text(a,exporting?"Choose a password of at least 8 characters. It is not saved and cannot be recovered. Backup includes entries, people, settings and profile photo.":"Select a Hisab .hisab file and enter its password. Restore replaces all local data only after the file is verified. Keep a backup first.",14,Ui.muted,false));
  EditText pass=Sheets.field(a,"Password","",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);c.addView(pass,Ui.lpm(-1,-2,0,16,0,0));EditText confirm=Sheets.field(a,"Repeat password","",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);if(exporting)c.addView(confirm,Ui.lpm(-1,-2,0,10,0,0));
  TextView go=Ui.button(a,exporting?"Choose where to save":"Choose backup file",true);c.addView(go,Ui.lpm(-1,-2,0,16,0,0));MainActivity.Layer[] layer=new MainActivity.Layer[1];
  Ui.tap(go,()->{String p=pass.getText().toString();if(p.length()<8||(exporting&&!p.equals(confirm.getText().toString()))){a.toast(exporting?"Use 8+ characters and matching passwords":"Enter the backup password",null);return;}char[] secret=p.toCharArray();pass.setText("");confirm.setText("");a.close(layer[0]);
   Intent i=new Intent(exporting?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).setType(exporting?"application/octet-stream":"*/*").addCategory(Intent.CATEGORY_OPENABLE);if(exporting)i.putExtra(Intent.EXTRA_TITLE,"Hisab-"+java.time.LocalDate.now()+".hisab");
   a.request(i,result->{if(result.getData()==null){Arrays.fill(secret,'\0');return;}a.toast(exporting?"Encrypting backup...":"Checking backup...",null);new Thread(()->{try{if(exporting){byte[] enc=crypt(snapshot(a.db),secret,true);try(OutputStream out=a.getContentResolver().openOutputStream(result.getData(),"wt")){if(out==null)throw new IOException();out.write(enc);}a.db.setPref("lastBackupAt",""+System.currentTimeMillis());a.runOnUiThread(()->a.toast("Encrypted backup saved",null));}else{byte[] plain=crypt(read(a.getContentResolver().openInputStream(result.getData())),secret,false);a.runOnUiThread(()->Sheets.confirm(a,"Replace local data?","Password and file verified. Your current entries, people and settings will be replaced. SMS and reminders will be turned off.","Restore",()->{try{restore(a.db,plain);Arrays.fill(plain,(byte)0);a.closeAll();a.applyTheme();a.build();Reminders.schedule(a,a.db);a.toast("Backup restored",null);}catch(Exception e){a.toast("Invalid backup. Your data was not changed",null);}}));}}catch(Exception e){a.runOnUiThread(()->a.toast(exporting?"Backup could not be saved":"Wrong password or damaged backup. Nothing changed",null));}finally{Arrays.fill(secret,'\0');}},"hisab-backup").start();});
  });layer[0]=Sheets.show(a,c);
 }
}
