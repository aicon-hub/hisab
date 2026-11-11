package in.aicon.pocket;
import java.util.*;import org.json.*;
/** Strict on-device wire format, versioned separately from exports. */
public final class SyncJson {
 private SyncJson(){}
 public static String record(SyncModel.Record r)throws JSONException {
  JSONObject j=new JSONObject().put("schema",2).put("id",r.id).put("deleted",r.deleted);JSONObject clock=new JSONObject();
  for(Map.Entry<String,Long> e:r.clock.entrySet())clock.put(e.getKey(),e.getValue());j.put("clock",clock);
  if(!r.deleted)j.put("payload",r.payload);return j.toString();
 }
 public static SyncModel.Record parseRecord(String body)throws Exception {
  if(body==null||body.length()>8*1024*1024)throw new IllegalArgumentException("Invalid sync record size");
  JSONObject j=new JSONObject(body);if(integer(j,"schema")!=2)throw new IllegalArgumentException("Unsupported sync record");
  if(!(j.get("deleted") instanceof Boolean))throw new IllegalArgumentException("Invalid deletion flag");boolean deleted=j.getBoolean("deleted");
  if(deleted&&j.has("payload"))throw new IllegalArgumentException("A deletion marker must contain no financial content");
  JSONObject raw=j.getJSONObject("clock");if(raw.length()>256)throw new IllegalArgumentException("Too many replica clocks");SortedMap<String,Long> clock=new TreeMap<>();Iterator<String> keys=raw.keys();
  while(keys.hasNext()){String key=keys.next();if(!key.matches("[A-Za-z0-9_-]{1,128}"))throw new IllegalArgumentException("Invalid replica ID");long value=integer(raw,key);if(value<0)throw new IllegalArgumentException("Invalid replica clock");clock.put(key,value);}
  if(!deleted&&!(j.get("payload") instanceof String))throw new IllegalArgumentException("Invalid financial record");
  // Reject unknown fields, so a tombstone cannot covertly retain old content.
  Set<String> allowed=new HashSet<>(Arrays.asList("schema","id","deleted","clock","payload"));keys=j.keys();while(keys.hasNext())if(!allowed.contains(keys.next()))throw new IllegalArgumentException("Unknown record field");
  return new SyncModel.Record(j.getString("id"),deleted?null:j.getString("payload"),deleted,clock);
 }
 public static String operation(SyncEngine.Pending op)throws Exception {return new JSONObject().put("schema",2).put("operation",op.operationId).put("record",new JSONObject(record(op.record))).toString();}
 public static SyncEngine.RemoteOperation parseOperation(String fileId,String body)throws Exception {
  if(body==null||body.length()>8*1024*1024)throw new IllegalArgumentException("Invalid sync operation size");JSONObject j=new JSONObject(body);
  if(integer(j,"schema")!=2||j.length()!=3)throw new IllegalArgumentException("Unsupported sync operation");
  return new SyncEngine.RemoteOperation(fileId,j.getString("operation"),parseRecord(j.getJSONObject("record").toString()));
 }
 private static long integer(JSONObject j,String name)throws Exception {Object o=j.get(name);if(!(o instanceof Integer)&&!(o instanceof Long))throw new IllegalArgumentException("Integer required");return ((Number)o).longValue();}
}
