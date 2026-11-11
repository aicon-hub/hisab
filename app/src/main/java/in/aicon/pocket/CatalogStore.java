package in.aicon.pocket;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;import org.json.*;

/** Named customization entities have permanent UUIDs. Renames update labels in the
 * legacy ledger in the same transaction. No amount, date, debt UUID or balance changes.
 * Removal is allowed only when unused. Used entities retain historical balances.
 */
public final class CatalogStore {
 public static final String[] TYPES={"category","account","person"};
 public static final String[] COLORS={"System","Teal","Blue","Violet","Rose","Graphite"};
 public static final class Item {
  public final String id,type,name,color;public final int icon;
  Item(String id,String type,String name,int icon,String color){this.id=id;this.type=type;this.name=name;this.icon=icon;this.color=color;}
 }
 private final SQLiteDatabase db;
 public CatalogStore(SQLiteDatabase db){this.db=db;db.execSQL("CREATE TABLE IF NOT EXISTS catalog_meta(name TEXT PRIMARY KEY,value TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS catalog(uuid TEXT PRIMARY KEY,type TEXT NOT NULL,name TEXT NOT NULL COLLATE NOCASE,icon INTEGER NOT NULL,color TEXT NOT NULL,UNIQUE(type,name))");}
 public void seed(String[] categories,String[] accounts)throws Exception{
  db.beginTransaction();try{boolean initial;try(Cursor c=db.rawQuery("SELECT value FROM catalog_meta WHERE name='seeded'",null)){initial=!c.moveToFirst();}if(initial&&(categories.length>0||accounts.length>0)){for(int i=0;i<categories.length;i++)seed("category",categories[i],i%28);for(String account:accounts)seed("account",account,22);db.execSQL("INSERT OR REPLACE INTO catalog_meta VALUES('seeded','1')");}
   try(Cursor c=db.rawQuery("SELECT DISTINCT category FROM tx WHERE category NOT IN ('Debt','Transfer') AND category<>''",null)){while(c.moveToNext())seed("category",c.getString(0),27);}
   try(Cursor c=db.rawQuery("SELECT DISTINCT account FROM tx WHERE account<>'' UNION SELECT DISTINCT dest FROM tx WHERE dest<>''",null)){while(c.moveToNext())seed("account",c.getString(0),22);}
   try(Cursor c=db.rawQuery("SELECT DISTINCT person FROM tx WHERE person<>''",null)){while(c.moveToNext())seed("person",c.getString(0),16);}
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
 }
 /** Only UI placeholders after an empty/reset ledger, not imported financial
  * records. They enter sync history only when a user makes an actual edit. */
 public void ensureUsable()throws Exception{db.beginTransaction();try{if(list("account").isEmpty())seed("account","Cash",22);if(list("category").isEmpty())seed("category","Other",27);db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public void ensureReference(String type,String name,int icon)throws Exception{if(!db.inTransaction())throw new IllegalStateException("Catalog migration must share the edit transaction");seed(type,name,icon);}
 private void seed(String type,String name,int icon)throws Exception{try(Cursor c=db.rawQuery("SELECT uuid FROM catalog WHERE type=? AND name=?",new String[]{type,name})){if(c.moveToFirst())return;}ContentValues v=new ContentValues();v.put("uuid",UUID.nameUUIDFromBytes(("hisab-catalog-v1:"+type+":"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());try(Cursor old=db.rawQuery("SELECT uuid FROM catalog WHERE uuid=?",new String[]{v.getAsString("uuid")})){if(old.moveToFirst())v.put("uuid",UUID.randomUUID().toString());}try(Cursor table=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='cloud_records'",null)){if(table.moveToFirst())try(Cursor history=db.rawQuery("SELECT body FROM cloud_records WHERE uuid=?",new String[]{v.getAsString("uuid")})){if(history.moveToFirst()&&SyncJson.parseRecord(history.getString(0)).deleted)v.put("uuid",UUID.randomUUID().toString());}}v.put("type",type);v.put("name",name);v.put("icon",icon);v.put("color","System");validate(type,name,icon,"System");db.insertOrThrow("catalog",null,v);}
 public List<Item> list(String type){if(!Arrays.asList(TYPES).contains(type))throw new IllegalArgumentException("Invalid entity type");List<Item> result=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT uuid,type,name,icon,color FROM catalog WHERE type=? ORDER BY rowid",new String[]{type})){while(c.moveToNext())result.add(new Item(c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4)));}return result;}
 public String[] names(String type){List<Item> all=list(type);String[] result=new String[all.size()];for(int i=0;i<all.size();i++)result[i]=all.get(i).name;return result;}
 public Item find(String type,String name){for(Item i:list(type))if(i.name.equals(name))return i;return null;}
 public Item put(String id,String type,String name,int icon,String color)throws Exception{
  name=name==null?"":name.trim();validate(type,name,icon,color);if(id!=null)Identifiers.uuid(id);
  db.beginTransaction();try{
   String previous=null;if(id!=null)try(Cursor c=db.rawQuery("SELECT type,name FROM catalog WHERE uuid=?",new String[]{id})){if(!c.moveToFirst()||!c.getString(0).equals(type))throw new IllegalArgumentException("Entity no longer exists");previous=c.getString(1);}
   try(Cursor c=db.rawQuery("SELECT uuid FROM catalog WHERE type=? AND name=? COLLATE NOCASE",new String[]{type,name})){if(c.moveToFirst()&&!c.getString(0).equals(id))throw new IllegalArgumentException("That name is already used");}
   String uuid=id==null?UUID.randomUUID().toString():id;ContentValues v=new ContentValues();v.put("uuid",uuid);v.put("type",type);v.put("name",name);v.put("icon",icon);v.put("color",color);
   if(id==null)db.insertOrThrow("catalog",null,v);else if(db.update("catalog",v,"uuid=?",new String[]{id})!=1)throw new IllegalArgumentException("Entity no longer exists");
   if(previous!=null&&!previous.equals(name)){
    ContentValues label=new ContentValues();label.put(type.equals("category")?"category":type.equals("account")?"account":"person",name);db.update("tx",label,(type.equals("category")?"category":type.equals("account")?"account":"person")+"=?",new String[]{previous});
    if(type.equals("account")){label=new ContentValues();label.put("dest",name);db.update("tx",label,"dest=?",new String[]{previous});}
    if(type.equals("category")){label=new ContentValues();label.put("category",name);db.update("budgets",label,"category=?",new String[]{previous});}
   }
   db.setTransactionSuccessful();return new Item(uuid,type,name,icon,color);
  }finally{db.endTransaction();}
 }
 public long references(Item item){if(item==null)throw new IllegalArgumentException("Entity not found");String column=item.type.equals("category")?"category":item.type.equals("account")?"account":"person";long total=count("SELECT count(*) FROM tx WHERE "+column+"=?",item.name);if(item.type.equals("account"))total+=count("SELECT count(*) FROM tx WHERE dest=?",item.name);if(item.type.equals("category"))total+=count("SELECT count(*) FROM budgets WHERE category=?",item.name);return total;}
 private long count(String query,String value){try(Cursor c=db.rawQuery(query,new String[]{value})){c.moveToFirst();return c.getLong(0);}}
 /** Remove only unused entities. A referenced account/person/category is never
  * cascaded: its historical balances, budgets and debt links must stay intact. */
 public void removeUnused(String id)throws Exception{Identifiers.uuid(id);db.beginTransaction();try{Item item=null;for(String type:TYPES)for(Item entry:list(type))if(entry.id.equals(id))item=entry;if(item==null)throw new IllegalArgumentException("Entity no longer exists");if(references(item)>0)throw new IllegalArgumentException("This is used by existing entries or budgets. Edit its name, icon or colour instead.");if(!item.type.equals("person")&&list(item.type).size()<=1)throw new IllegalArgumentException("Keep at least one account and category");if(db.delete("catalog","uuid=?",new String[]{id})!=1)throw new IllegalStateException("Entity changed. Reopen it.");SettingsStore preferences=new SettingsStore(db);for(String key:new String[]{"defaultAccount","defaultCategory"})if(preferences.get(key).equals(id))preferences.put(key,"");db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public static void validate(String type,String name,int icon,String color){
  if(!Arrays.asList(TYPES).contains(type)||name==null||name.trim().isEmpty()||name.length()>80||icon<0||icon>=28||!Arrays.asList(COLORS).contains(color))throw new IllegalArgumentException("Choose a name, icon and colour");
  for(char c:name.toCharArray())if(Character.isISOControl(c))throw new IllegalArgumentException("Names cannot contain control characters");
  if(type.equals("category")&&(name.equalsIgnoreCase("Debt")||name.equalsIgnoreCase("Transfer")))throw new IllegalArgumentException("That category is reserved for linked records");
 }
 public JSONArray export()throws Exception{JSONArray a=new JSONArray();try(Cursor c=db.rawQuery("SELECT uuid,type,name,icon,color FROM catalog ORDER BY rowid",null)){while(c.moveToNext())a.put(new JSONObject().put("uuid",c.getString(0)).put("type",c.getString(1)).put("name",c.getString(2)).put("icon",c.getInt(3)).put("color",c.getString(4)));}return a;}
 public static void validateExport(JSONArray a)throws Exception{if(a.length()>2000)throw new IllegalArgumentException("Too many customization entities");Set<String> ids=new HashSet<>(),names=new HashSet<>();for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);LedgerWire.keys(o,"uuid","type","name","icon","color");String id=LedgerWire.text(o,"uuid",36,true),type=LedgerWire.text(o,"type",20,true),name=LedgerWire.text(o,"name",80,true);Identifiers.uuid(id);if(id.equals(LedgerWire.PREFERENCES_ID))throw new IllegalArgumentException("Reserved preference identity");long icon=LedgerWire.integer(o,"icon");if(icon<0||icon>=28)throw new IllegalArgumentException("Invalid icon");validate(type,name,(int)icon,LedgerWire.text(o,"color",20,true));if(!name.equals(name.trim())||!ids.add(id)||!names.add(type+":"+name.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Duplicate customization entity");}}
 public void restore(JSONArray a)throws Exception{if(!db.inTransaction())throw new IllegalStateException("Restore must share the ledger transaction");validateExport(a);db.execSQL("INSERT OR REPLACE INTO catalog_meta VALUES('seeded','1')");db.delete("catalog",null,null);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);ContentValues v=new ContentValues();v.put("uuid",o.getString("uuid"));v.put("type",o.getString("type"));v.put("name",o.getString("name"));v.put("icon",o.getInt("icon"));v.put("color",o.getString("color"));db.insertOrThrow("catalog",null,v);}}
}
