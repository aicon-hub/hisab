package in.aicon.pocket;
import org.json.*;import java.util.*;import java.nio.charset.StandardCharsets;

/** Strict app-only payloads. Current ledger records use permanent catalog UUID
 * references, so a concurrent rename cannot detach an account, category or debt. */
public final class LedgerWire {
 private LedgerWire(){}
 public static final String PREFERENCES_ID=UUID.nameUUIDFromBytes("hisab-ledger-preferences-v1".getBytes(StandardCharsets.UTF_8)).toString();
 public static JSONObject catalog(CatalogStore.Item item)throws Exception{return new JSONObject().put("type","catalog").put("uuid",item.id).put("entityType",item.type).put("name",item.name).put("icon",item.icon).put("color",item.color);}
 public static JSONObject preferences(JSONObject values)throws Exception{return new JSONObject().put("type","preferences").put("uuid",PREFERENCES_ID).put("values",values);}
 public static JSONObject linkedTransaction(String uuid,String kind,long amount,String day,String accountId,String destId,String categoryId,String note,String personId,String loan)throws Exception{return new JSONObject().put("type","transaction").put("uuid",uuid).put("kind",kind).put("amount",amount).put("day",day).put("accountId",accountId).put("destId",destId).put("categoryId",categoryId).put("note",note).put("personId",personId).put("loan",loan).put("currency","INR");}
 public static JSONObject linkedBudget(String uuid,String categoryId,long amount)throws Exception{return new JSONObject().put("type","budget").put("uuid",uuid).put("categoryId",categoryId).put("amount",amount).put("currency","INR");}
 // Legacy pure-fixture codecs. No account identity or local row ID in any payload.
 public static JSONObject transaction(String uuid,String kind,long amount,String day,String account,String dest,String category,String note,String person,String loan)throws Exception{return new JSONObject().put("type","transaction").put("uuid",uuid).put("kind",kind).put("amount",amount).put("day",day).put("account",account).put("dest",dest).put("category",category).put("note",note).put("person",person).put("loan",loan);}
 public static JSONObject budget(String uuid,String category,long amount)throws Exception{Identifiers.uuid(uuid);return new JSONObject().put("type","budget").put("uuid",uuid).put("category",category).put("amount",amount);}
 public static JSONObject goal(String uuid,String name,long target,long saved)throws Exception{return new JSONObject().put("type","goal").put("uuid",uuid).put("name",name).put("target",target).put("saved",saved);}
 static long integer(JSONObject j,String key)throws Exception{Object value=j.get(key);if(!(value instanceof Integer)&&!(value instanceof Long))throw new IllegalArgumentException("Invalid numeric field");return ((Number)value).longValue();}
 static String text(JSONObject j,String key,int max,boolean required)throws Exception{Object value=j.get(key);if(!(value instanceof String))throw new IllegalArgumentException("Invalid text field");String s=(String)value;if(s.length()>max||(required&&s.trim().isEmpty())||s.indexOf('\u0000')>=0)throw new IllegalArgumentException("Invalid text field");return s;}
 static long amount(JSONObject j,String key,boolean zero)throws Exception{long value=integer(j,key);if(value<(zero?0:1)||value>999999999999L)throw new IllegalArgumentException("Invalid amount");return value;}
 static void keys(JSONObject j,String... keys)throws Exception{Set<String> allowed=new HashSet<>(Arrays.asList(keys));if(j.length()!=allowed.size())throw new IllegalArgumentException("Unexpected financial fields");Iterator<String> k=j.keys();while(k.hasNext())if(!allowed.contains(k.next()))throw new IllegalArgumentException("Unexpected financial field");}
 static String reference(Map<String,JSONObject> catalog,String id,String type,boolean optional)throws Exception{if(optional&&id.isEmpty())return "";JSONObject item=catalog.get(id);if(item==null||!item.getString("entityType").equals(type))throw new IllegalArgumentException("Missing or wrong catalog reference");return item.getString("name");}
 public static Map<String,JSONObject> validate(Map<String,SyncModel.Record> records)throws Exception{
  Map<String,JSONObject> decoded=new TreeMap<>(),catalog=new TreeMap<>();Set<String> names=new HashSet<>(),accounts=new HashSet<>(Arrays.asList("Cash","Bank","Wallet"));int catalogs=0;
  for(Map.Entry<String,SyncModel.Record> entry:records.entrySet()){
   SyncModel.Record r=entry.getValue();if(!r.id.equals(entry.getKey()))throw new IllegalArgumentException("Record key mismatch");if(r.deleted){if(r.id.equals(PREFERENCES_ID))throw new IllegalArgumentException("Ledger preferences cannot be deleted individually. A reviewed cloud reset is required.");continue;}
   JSONObject j=new JSONObject(r.payload);String uuid=text(j,"uuid",36,true);Identifiers.uuid(uuid);if(!uuid.equals(r.id))throw new IllegalArgumentException("Record ID mismatch");String type=text(j,"type",20,true);decoded.put(uuid,j);
   if(type.equals("catalog")){keys(j,"type","uuid","entityType","name","icon","color");String entity=text(j,"entityType",20,true),name=text(j,"name",80,true),color=text(j,"color",20,true);long icon=integer(j,"icon");if(icon<0||icon>=28)throw new IllegalArgumentException("Invalid icon");CatalogStore.validate(entity,name,(int)icon,color);if(!name.equals(name.trim())||!names.add(entity+":"+name.toLowerCase(Locale.ROOT))||++catalogs>2000)throw new IllegalArgumentException("Duplicate or too many catalog entries");catalog.put(uuid,j);}
   else if(type.equals("preferences")){keys(j,"type","uuid","values");if(!uuid.equals(PREFERENCES_ID))throw new IllegalArgumentException("Invalid preferences identity");SettingsStore.validateExport(j.getJSONObject("values"));}
  }
  if(!catalog.isEmpty()){accounts.clear();for(JSONObject item:catalog.values())if(item.getString("entityType").equals("account"))accounts.add(item.getString("name"));}
  Map<String,String> entityTypes=new HashMap<>();for(Map.Entry<String,JSONObject> item:catalog.entrySet())entityTypes.put(item.getKey(),item.getValue().getString("entityType"));if(decoded.containsKey(PREFERENCES_ID))SettingsStore.validateReferences(decoded.get(PREFERENCES_ID).getJSONObject("values"),entityTypes);
  String ledgerCurrency="INR";if(decoded.containsKey(PREFERENCES_ID))ledgerCurrency=decoded.get(PREFERENCES_ID).getJSONObject("values").optString("currency","INR");
  List<LedgerIntegrity.Entry> transactions=new ArrayList<>();Set<String> budgetCategories=new HashSet<>();int budgets=0,goals=0;
  for(JSONObject j:decoded.values()){
   String type=j.getString("type"),uuid=j.getString("uuid");if(Arrays.asList("transaction","budget","goal").contains(type)){String unit=j.has("currency")?text(j,"currency",3,true):"INR";if(!unit.equals(ledgerCurrency))throw new IllegalArgumentException("Records have different currencies. Review before merging.");}
   switch(type){
    case "catalog":case "preferences":break;
    case "transaction":{
     String account,dest,category,person,kind=text(j,"kind",20,true);
     if(j.has("accountId")){keys(j,"type","uuid","kind","amount","day","accountId","destId","categoryId","note","personId","loan","currency");account=reference(catalog,text(j,"accountId",36,true),"account",false);dest=reference(catalog,text(j,"destId",36,false),"account",true);String categoryId=text(j,"categoryId",36,true);category=categoryId.equals("Debt")||categoryId.equals("Transfer")?categoryId:reference(catalog,categoryId,"category",false);person=reference(catalog,text(j,"personId",36,false),"person",true);j.put("account",account).put("dest",dest).put("category",category).put("person",person);}
     else{keys(j,"type","uuid","kind","amount","day","account","dest","category","note","person","loan");account=text(j,"account",80,true);dest=text(j,"dest",80,false);category=text(j,"category",80,true);person=text(j,"person",80,false);}
     boolean debt=kind.equals("lent")||kind.equals("borrowed")||kind.startsWith("repay_");if(debt&&!category.equals("Debt")||kind.equals("transfer")&&!category.equals("Transfer")||!debt&&!kind.equals("transfer")&&(category.equals("Debt")||category.equals("Transfer")))throw new IllegalArgumentException("Invalid linked category");text(j,"note",160,false);transactions.add(new LedgerIntegrity.Entry(uuid,kind,amount(j,"amount",false),text(j,"day",10,true),account,dest,person,text(j,"loan",36,false)));break;
    }
    case "budget":{String category;if(j.has("categoryId")){keys(j,"type","uuid","categoryId","amount","currency");category=reference(catalog,text(j,"categoryId",36,true),"category",false);j.put("category",category);}else{keys(j,"type","uuid","category","amount");category=text(j,"category",80,true);}amount(j,"amount",false);if(!budgetCategories.add(category)||++budgets>100)throw new IllegalArgumentException("Invalid budget identity or count");break;}
    case "goal":if(j.has("currency"))keys(j,"type","uuid","name","target","saved","currency");else keys(j,"type","uuid","name","target","saved");text(j,"name",80,true);amount(j,"target",false);amount(j,"saved",true);if(++goals>1000)throw new IllegalArgumentException("Too many goals");break;
    default:throw new IllegalArgumentException("Unsupported financial record");
   }
  }
  if(transactions.size()>20000)throw new IllegalArgumentException("Too many transactions");LedgerIntegrity.validate(transactions,accounts);return decoded;
 }
}
