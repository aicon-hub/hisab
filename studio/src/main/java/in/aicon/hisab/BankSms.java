package in.aicon.hisab;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.*;
import java.util.*;
import java.util.regex.*;

/** Experimental on-device balance snapshots. Never sends SMS or imports transactions. */
public final class BankSms extends BroadcastReceiver {
 static final Map<String,String> BANKS=new LinkedHashMap<>();
 static {BANKS.put("SBI","SBI");BANKS.put("HDFCBK","HDFC");BANKS.put("ICICIB","ICICI");BANKS.put("AXISBK","Axis");BANKS.put("KOTAKB","Kotak");BANKS.put("PNBSMS","PNB");BANKS.put("CANBNK","Canara");BANKS.put("UNIONB","Union");BANKS.put("BOIIND","Bank of India");BANKS.put("BOBTXN","Bank of Baroda");BANKS.put("IOBCHN","Indian Overseas");BANKS.put("INDBNK","Indian Bank");BANKS.put("FEDBNK","Federal");BANKS.put("SIBSMS","South Indian");BANKS.put("IDFCFB","IDFC FIRST");BANKS.put("YESBNK","YES");BANKS.put("IDBIBK","IDBI");BANKS.put("AUBANK","AU");BANKS.put("RBLBNK","RBL");BANKS.put("BANDHN","Bandhan");}
 static final Pattern ACCOUNT=Pattern.compile("(?i)(?:a/c|a\\.?c(?:ct)?\\.?|account)(?:\\s*(?:no\\.?|number|ending(?:\\s+(?:in|with))?))?\\s*[:. -]?\\s*[xX*# -]*([0-9]{4,18})");
 static final String MONEY="([+-]?[0-9][0-9,]*(?:\\.[0-9]{1,2})?)";
 static final String LABEL="(?:(?:available|avl|avail|clear|closing|current|total\\s+available)\\.?\\s*)?(?:balance|bal)\\.?(?:\\s+is)?\\s*[:=]?\\s*";
 static final Pattern BALANCE=Pattern.compile("(?i)\\b"+LABEL+"(?:INR|Rs\\.?|₹)\\s*"+MONEY+"(?![0-9]|[.,][0-9])(?:\\s*(CR|DR)\\b)?");
 static final Pattern BALANCE_AFTER=Pattern.compile("(?i)\\b"+LABEL+MONEY+"\\s*(?:INR|Rs\\.?|₹)(?![a-z])(?:\\s*(CR|DR)\\b)?");
 static long parseMoney(String number,String sign){
  String abs=number.replaceFirst("^[+-]","");
  if(!abs.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3})(?:\\.[0-9]{1,2})?"))throw new IllegalArgumentException();
  long value=new java.math.BigDecimal(number.replace(",","")).movePointRight(2).longValueExact();
  if(sign!=null&&sign.equalsIgnoreCase("DR")){if(value<0)throw new IllegalArgumentException();value=-value;}
  if(sign!=null&&sign.equalsIgnoreCase("CR")&&value<0)throw new IllegalArgumentException();
  if(value < -999999999999L || value > 999999999999L)throw new IllegalArgumentException();return value;
 }
 static final class Result {String bank,last4;long balance;}
 static String senderKey(String sender){return sender==null?"":sender.toUpperCase(Locale.ROOT).replaceFirst("^[A-Z0-9]{2}-","").replaceFirst("-[A-Z]$","");}
 static Result parse(String sender,String body){return parse(sender,body,null);}
 static Result parse(String sender,String body,String customBank){
  if(sender==null||body==null||body.length()>5000)return null;String up=body.toUpperCase(Locale.ROOT);
  if(up.matches("(?s).*\\b(OTP|CREDIT CARD|ONE TIME|ONE-TIME|OFFER|LOAN|DUE DATE|MINIMUM DUE|MINIMUM BALANCE|MAINTAIN|LIEN|HOLD|LIMIT|PENDING|FAILED|DECLINED)\\b.*"))return null;
  String key=senderKey(sender);String bank=customBank==null?BANKS.get(key):customBank;if(bank==null)return null;
  Matcher am=ACCOUNT.matcher(body),bm=BALANCE.matcher(body),after=BALANCE_AFTER.matcher(body);
  if(!am.find())return null;boolean before=bm.find(),reverse=after.find();if(before==reverse)return null;
  Matcher chosen=before?bm:after;String digits=am.group(1);String tail=digits.substring(digits.length()-4);long amount;
  try{amount=parseMoney(chosen.group(1),chosen.group(2));}catch(Exception e){return null;}
  if(am.find()||chosen.find())return null;
  Result r=new Result();r.bank=bank;r.last4=tail;r.balance=amount;return r;
 }
 static String key(Result r){return r.bank+":"+r.last4;}
 static synchronized boolean apply(Store db,String sender,String body,long when){
  if(!db.flag("smsEnabled",false)||when>System.currentTimeMillis()+300000L)return false;String custom=db.pref("smsSender:"+senderKey(sender),"");Result r=parse(sender,body,custom.isEmpty()?null:custom);if(r==null)return false;String k=key(r);long account;
  try{account=Long.parseLong(db.pref("smsMap:"+k,"0"));}catch(Exception e){return false;}if(db.account(account)==null)return false;
  long last;try{last=Long.parseLong(db.pref("smsTime:"+account,"0"));}catch(Exception e){last=0;}if(when<=last)return false;
  db.getWritableDatabase().beginTransaction();try{db.setPref("smsBalance:"+account,""+r.balance);db.setPref("smsTime:"+account,""+when);db.setPref("smsSource:"+account,r.bank+" ••"+r.last4);db.getWritableDatabase().setTransactionSuccessful();return true;}finally{db.getWritableDatabase().endTransaction();}
 }
 @Override public void onReceive(Context c,Intent intent){if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;Store db=new Store(c);try{if(!db.flag("smsEnabled",false))return;SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);if(messages==null||messages.length==0)return;StringBuilder b=new StringBuilder();for(SmsMessage m:messages)b.append(m.getMessageBody());apply(db,messages[0].getOriginatingAddress(),b.toString(),messages[0].getTimestampMillis());}finally{db.close();}}
 static String inr(long amount){return (amount<0?"-":"")+"₹"+Fmt.num(amount);}
 static String display(Store db,long id){try{String value=db.pref("smsBalance:"+id,"");if(value.isEmpty())return null;Store.Acc account=db.account(id);long when=Long.parseLong(db.pref("smsTime:"+id,"0"));String date=java.time.Instant.ofEpochMilli(when).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm",L.locale()));String money=account!=null&&Privacy.hidden(db,account)?"••••":inr(Long.parseLong(value));String stale=System.currentTimeMillis()-when>7L*24*60*60*1000?(L.isMl()?" · പഴയ SMS":" · older SMS"):"";return L.t("Latest bank SMS")+" "+money+"\n"+date+stale;}catch(Exception e){return null;}}
 static void open(MainActivity a){
  Kit.page(a,"Bank SMS",null,null,c->{
   c.addView(Ui.text(a,"Experimental. Not a live bank connection.",18,Ui.ink,true));c.addView(Ui.text(a,"Reads supported balance messages on this phone only. No SMS is sent, uploaded or used for ads. Unknown senders, OTPs and ambiguous formats are ignored. Coverage is not guaranteed for every Indian bank. A forged sender is possible: always verify in your bank app.",14,Ui.muted,false),Ui.lpm(-1,-2,0,12,0,16));
   c.addView(Ui.text(a,"SMS balance is shown separately from your ledger balance, with its date. It never creates entries or changes your starting balance. This avoids counting an expense twice.",14,Ui.muted,false));
   Switch toggle=new Switch(a);toggle.setText("Read new bank SMS");toggle.setTextColor(Ui.ink);toggle.setChecked(a.db.flag("smsEnabled",false));c.addView(toggle,Ui.lpm(-1,-2,0,16,0,16));toggle.setOnCheckedChangeListener((v,on)->{if(!on){a.db.setPref("smsEnabled","0");return;}a.db.setPref("smsEnabled","0");toggle.setChecked(false);Sheets.confirm(a,"Allow SMS access?","Android will ask for SMS permission. Only incoming messages with a recognised bank sender, linked account ending and explicit INR balance are used. Other messages are ignored. You can turn this off any time.","Continue",()->{if(a.checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED){a.db.setPref("smsEnabled","1");a.closeAll();open(a);}else a.requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},71);});});
   TextView link=Ui.button(a,"Link a bank account",true);c.addView(link);Ui.tap(link,()->{List<String> names=new ArrayList<>(new LinkedHashSet<>(BANKS.values()));Sheets.pick(a,"Bank",names,null,-1,i->{List<Store.Acc> accounts=new ArrayList<>();for(Store.Acc candidate:a.db.accounts())if(candidate.kind.equals("bank"))accounts.add(candidate);if(accounts.isEmpty()){a.toast("Add a Bank account in Accounts first",null);return;}List<String> labels=new ArrayList<>();for(Store.Acc x:accounts)labels.add(x.name);Sheets.pick(a,"Hisab account",labels,null,-1,j->Sheets.form(a,"Account ending",new String[]{"Last 4 digits"},new String[]{""},new int[]{android.text.InputType.TYPE_CLASS_NUMBER},"Link",vals->{if(!vals[0].matches("[0-9]{4}")){a.toast("Enter exactly 4 digits",null);return;}String k=names.get(i)+":"+vals[0];a.db.setPref("smsMap:"+k,""+accounts.get(j).id);a.closeAll();open(a);},null));});});
   TextView other=Ui.button(a,"Other bank / sender",false);c.addView(other,Ui.lpm(-1,-2,0,12,0,0));Ui.tap(other,()->Sheets.form(a,"Other bank",new String[]{"Bank name","Sender ID (letters, not phone number)","Last 4 account digits"},new String[]{"","",""},new int[]{Sheets.TEXT,Sheets.TEXT,android.text.InputType.TYPE_CLASS_NUMBER},"Choose Hisab account",values->{String sender=senderKey(values[1]);if(values[0].isEmpty()||!sender.matches("[A-Z][A-Z0-9]{2,10}")||!values[2].matches("[0-9]{4}")){a.toast("Enter a bank name, bank sender ID and 4 digits",null);return;}List<Store.Acc> accounts=new ArrayList<>();for(Store.Acc candidate:a.db.accounts())if(candidate.kind.equals("bank"))accounts.add(candidate);if(accounts.isEmpty()){a.toast("Add a Bank account in Accounts first",null);return;}List<String> labels=new ArrayList<>();for(Store.Acc x:accounts)labels.add(x.name);Sheets.pick(a,"Hisab account",labels,null,-1,i->{a.db.setPref("smsSender:"+sender,values[0]);a.db.setPref("smsMap:"+values[0]+":"+values[2],""+accounts.get(i).id);a.closeAll();open(a);a.toast("Sender linked. Only explicit INR balances can be read",null);});},null));
   for(Store.Acc acc:a.db.accounts()){String bal=display(a.db,acc.id);if(bal!=null){LinearLayout card=Ui.card(a);card.addView(Ui.text(a,acc.name,17,Ui.ink,true));card.addView(Ui.text(a,bal,16,Ui.ink,true),Ui.lpm(-1,-2,0,10,0,6));card.addView(Ui.text(a,L.t("Ledger balance")+" "+Privacy.amount(a.db,acc,a.db.balance(acc)),13,Ui.muted,false));c.addView(card,Ui.lpm(-1,-2,0,12,0,0));}}

   try(Cursor links=a.db.getReadableDatabase().rawQuery("SELECT k,v FROM prefs WHERE k LIKE 'smsMap:%'",null)){while(links.moveToNext()){Store.Acc acc=null;try{acc=a.db.account(Long.parseLong(links.getString(1)));}catch(Exception ignored){}if(acc!=null)c.addView(Ui.text(a,links.getString(0).substring(7)+" → "+acc.name,13,Ui.muted,true),Ui.lpm(-1,-2,0,8,0,0));}}
   TextView scan=Ui.button(a,"Scan last 30 days",false);c.addView(scan,Ui.lpm(-1,-2,0,12,0,0));Ui.tap(scan,()->{if(!a.db.flag("smsEnabled",false)){a.toast("Enable bank SMS and link an account first",null);return;}Sheets.confirm(a,"Read recent messages?","Optionally reads up to 5,000 inbox messages from the last 30 days, locally. Only recognised balances for linked accounts are kept. No raw SMS is stored.","Continue",()->{if(a.checkSelfPermission(Manifest.permission.READ_SMS)!=PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{Manifest.permission.READ_SMS},72);else scan(a);});});
   TextView test=Ui.button(a,"Try a message locally",false);c.addView(test,Ui.lpm(-1,-2,0,12,0,0));Ui.tap(test,()->Sheets.form(a,"Test bank format",new String[]{"Sender ID", "Message (no OTP)"},new String[]{"VM-HDFCBK","A/c XX1234 available balance INR 1200.50"},new int[]{Sheets.TEXT,Sheets.TEXT},"Check",v->{String custom=a.db.pref("smsSender:"+senderKey(v[0]),"");Result r=parse(v[0],v[1],custom.isEmpty()?null:custom);a.toast(r==null?"Not recognised. No balance changed":r.bank+" ••"+r.last4+" balance "+((a.db.flag("hideAll",false)||a.db.flag("hide:bank",false))?"••••":inr(r.balance))+". Preview only",null);},null));
   TextView clear=Ui.button(a,"Unlink and clear SMS balances",false);c.addView(clear,Ui.lpm(-1,-2,0,12,0,0));Ui.tap(clear,()->{a.db.getWritableDatabase().delete("prefs","k LIKE 'sms%'",null);a.closeAll();a.refresh();open(a);});
   c.addView(Ui.text(a,"Sender families tested with synthetic examples: "+String.join(", ",new LinkedHashSet<>(BANKS.values()))+". Formats need an account ending and explicit INR/Rs balance. INR is kept even if your ledger uses another currency. Newer messages replace older snapshots; signed and DR balances are supported. A snapshot over seven days old is labelled older, and is never a live-balance guarantee. These are not bank-certified integrations.",12,Ui.muted,false),Ui.lpm(-1,-2,0,20,0,0));
  });
 }
 static void scan(MainActivity a){new Thread(()->{int count=0;try{long since=System.currentTimeMillis()-30L*24*60*60*1000;try(Cursor c=a.getContentResolver().query(Uri.parse("content://sms/inbox"),new String[]{"address","body","date"},"date>=?",new String[]{""+since},"date DESC")){int n=0;if(c!=null)while(c.moveToNext()&&n++<5000)if(apply(a.db,c.getString(0),c.getString(1),c.getLong(2)))count++;}int found=count;a.runOnUiThread(()->{a.closeAll();a.refresh();open(a);a.toast(found+" balance snapshots updated",null);});}catch(Exception e){a.runOnUiThread(()->a.toast("SMS could not be read. Check permission",null));}},"hisab-sms").start();}
}
