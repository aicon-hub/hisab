package in.aicon.pocket;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;import org.json.*;

/** Copy an explicitly chosen local-only ledger to an empty account ledger. This is
 * not called by sign-in. Source stays untouched. No replace/merge shortcut exists.
 */
public final class LedgerTransfer {
 private LedgerTransfer(){}
 public static Map<String,SyncModel.Record> localSnapshot(SQLiteDatabase local)throws Exception{
  if(!new java.io.File(local.getPath()).getName().equals(AccountScope.LOCAL_DATABASE))throw new IllegalArgumentException("Only the local-only ledger can be adopted");
  local.beginTransaction();try{CatalogStore catalog=new CatalogStore(local);SettingsStore settings=new SettingsStore(local);Map<String,SyncModel.Record> result=new TreeMap<>();for(Map.Entry<String,String> entry:LedgerRecords.capture(local,catalog,settings).entrySet())result.put(entry.getKey(),new SyncModel.Record(entry.getKey(),entry.getValue(),false,Collections.emptyMap()));LedgerWire.validate(result);local.setTransactionSuccessful();return Collections.unmodifiableMap(result);}finally{local.endTransaction();}
 }
 public static void intoEmptyAccount(SQLiteDatabase target,LedgerSyncStore store,Map<String,SyncModel.Record> source)throws Exception{
  if(!store.owns(target))throw new IllegalArgumentException("Sync state belongs to a different ledger");if(!target.inTransaction())throw new IllegalStateException("Adoption and initial sync capture must share a transaction");
  if(new java.io.File(target.getPath()).getName().equals(AccountScope.LOCAL_DATABASE))throw new IllegalArgumentException("Target must be an account ledger");
  if(!store.snapshot().records.isEmpty())throw new IllegalStateException("This account ledger already has sync history. No records replaced.");for(String table:new String[]{"tx","budgets","goals"})try(Cursor c=target.rawQuery("SELECT count(*) FROM "+table,null)){c.moveToFirst();if(c.getLong(0)!=0)throw new IllegalStateException("This account ledger is not empty. No records replaced.");}
  for(SyncModel.Record record:source.values())if(record.deleted)throw new IllegalArgumentException("Local adoption cannot contain deleted history");Map<String,JSONObject> decoded=LedgerWire.validate(source);JSONArray catalogRows=new JSONArray();JSONObject preferences=null;
  for(JSONObject j:decoded.values())if(j.getString("type").equals("catalog"))catalogRows.put(new JSONObject().put("uuid",j.getString("uuid")).put("type",j.getString("entityType")).put("name",j.getString("name")).put("icon",j.getInt("icon")).put("color",j.getString("color")));else if(j.getString("type").equals("preferences"))preferences=j.getJSONObject("values");
  if(preferences==null)throw new IllegalArgumentException("Local preferences missing");new CatalogStore(target).restore(catalogRows);new SettingsStore(target).restore(preferences);
  for(JSONObject j:decoded.values()){
   String type=j.getString("type");if(type.equals("catalog")||type.equals("preferences"))continue;ContentValues v=new ContentValues();v.put("uuid",j.getString("uuid"));String table;
   if(type.equals("transaction")){table="tx";for(String key:new String[]{"kind","day","account","dest","category","note","person","loan"})v.put(key,j.getString(key));v.put("amount",j.getLong("amount"));}
   else if(type.equals("budget")){table="budgets";v.put("category",j.getString("category"));v.put("amount",j.getLong("amount"));}
   else if(type.equals("goal")){table="goals";v.put("name",j.getString("name"));v.put("target",j.getLong("target"));v.put("saved",j.getLong("saved"));}else throw new IllegalArgumentException("Unsupported local record");target.insertOrThrow(table,null,v);
  }
  store.captureLocalEdits();
 }
}
