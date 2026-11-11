package in.aicon.pocket;
import android.content.Context;import android.content.SharedPreferences;import java.util.*;import org.json.*;

/** Private device account directory. No token/password and never included in ledger
 * backup/sync. Recording a verified identity does not select it or copy any data.
 * Offline selection opens only that previously verified account's local cache.
 */
public final class AccountDirectory {
 public static final class Entry {public final String subject,email;private Entry(String subject,String email){this.subject=subject;this.email=email;}}
 private final SharedPreferences preferences;
 public AccountDirectory(Context context){preferences=context.getSharedPreferences("hisab-account-directory",Context.MODE_PRIVATE);}
 private JSONObject read()throws Exception{String raw=preferences.getString("accounts","{}");if(raw==null||raw.length()>65536)throw new IllegalStateException("Account directory cannot be read");JSONObject accounts=new JSONObject(raw);if(accounts.length()>20)throw new IllegalStateException("Too many account entries");Iterator<String> ids=accounts.keys();while(ids.hasNext()){String id=ids.next();AccountScope.databaseFor(id);JSONObject entry=accounts.getJSONObject(id);LedgerWire.keys(entry,"email");email(LedgerWire.text(entry,"email",320,true));}return accounts;}
 private static void email(String value){if(value==null||value.length()>320||!value.contains("@")||value.trim().length()!=value.length())throw new IllegalArgumentException("Invalid verified account email");for(char c:value.toCharArray())if(Character.isISOControl(c))throw new IllegalArgumentException("Invalid email characters");}
 private void commit(SharedPreferences.Editor editor){if(!editor.commit())throw new IllegalStateException("Could not save selected ledger");}
 public synchronized void record(VerifiedDriveAccess access)throws Exception{if(disconnectPending(access.subject))throw new IllegalStateException("Finish disconnecting this account before connecting it again");access.accessToken(access.subject);AccountScope.databaseFor(access.subject);email(access.email);JSONObject accounts=read();if(accounts.length()>=20&&!accounts.has(access.subject))throw new IllegalArgumentException("Account limit reached on this device");accounts.put(access.subject,new JSONObject().put("email",access.email));commit(preferences.edit().putString("accounts",accounts.toString()));}
 public synchronized List<Entry> entries()throws Exception{JSONObject accounts=read();List<Entry> result=new ArrayList<>();Iterator<String> ids=accounts.keys();while(ids.hasNext()){String subject=ids.next();result.add(new Entry(subject,accounts.getJSONObject(subject).getString("email")));}result.sort(Comparator.comparing(a->a.email));return Collections.unmodifiableList(result);}
 public synchronized Entry find(String subject)throws Exception{AccountScope.databaseFor(subject);for(Entry entry:entries())if(entry.subject.equals(subject))return entry;return null;}
 public synchronized String selectedSubject()throws Exception{String subject=preferences.getString("selected",null);if(subject==null)return null;if(find(subject)==null)throw new IllegalStateException("Selected account was not verified on this device. Choose a ledger again.");return subject;}
 /** These are user-choice effects, never callback defaults after a sign-in. */
 public synchronized void selectLocal(){commit(preferences.edit().remove("selected"));}
 public synchronized void selectKnown(String subject)throws Exception{if(find(subject)==null)throw new IllegalArgumentException("Sign in to this account before selecting its ledger");commit(preferences.edit().putString("selected",subject));}
 public synchronized boolean disconnectPending(String subject){AccountScope.databaseFor(subject);return preferences.getBoolean("disconnect:"+subject,false);}
 public synchronized void beginDisconnect(String subject)throws Exception{if(find(subject)==null)throw new IllegalArgumentException("Unknown Google account");commit(preferences.edit().putBoolean("disconnect:"+subject,true));}
 /** Clear only after Google confirms revocation; a timeout is not confirmation. */
 public synchronized void disconnected(String subject)throws Exception{if(!disconnectPending(subject))throw new IllegalStateException("No disconnect was requested");JSONObject accounts=read();accounts.remove(subject);SharedPreferences.Editor edit=preferences.edit().putString("accounts",accounts.toString()).remove("disconnect:"+subject);if(subject.equals(preferences.getString("selected",null)))edit.remove("selected");commit(edit);}
 public synchronized void forget(String subject)throws Exception{if(disconnectPending(subject))throw new IllegalStateException("Finish the pending Google disconnect first");JSONObject accounts=read();AccountScope.databaseFor(subject);accounts.remove(subject);SharedPreferences.Editor edit=preferences.edit().putString("accounts",accounts.toString());if(subject.equals(preferences.getString("selected",null)))edit.remove("selected");commit(edit);}
}
