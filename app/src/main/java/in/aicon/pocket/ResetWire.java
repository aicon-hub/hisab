package in.aicon.pocket;
import org.json.*;import java.util.*;

/** Version 3 remote files carry an immutable reset-set tag. No transport is called here. */
public final class ResetWire {
 private ResetWire(){}
 public static JSONArray ids(Collection<String> input){JSONArray a=new JSONArray();for(String id:ResetBarrier.canonical(input))a.put(id);return a;}
 public static SortedSet<String> parseIds(JSONArray a)throws Exception{if(a.length()>ResetBarrier.MAX_RESETS)throw new IllegalArgumentException("Too many barriers");Set<String> ids=new HashSet<>();for(int n=0;n<a.length();n++){Object value=a.get(n);if(!(value instanceof String)||!ids.add((String)value))throw new IllegalArgumentException("Invalid or duplicate reset marker");}return ResetBarrier.canonical(ids);}
 public static String marker(String resetId)throws Exception{Identifiers.uuid(resetId);return new JSONObject().put("schema",3).put("type","reset").put("id",resetId).toString();}
 public static String parseMarker(String body)throws Exception{JSONObject j=new JSONObject(body);LedgerWire.keys(j,"schema","type","id");if(LedgerWire.integer(j,"schema")!=3||!LedgerWire.text(j,"type",10,true).equals("reset"))throw new IllegalArgumentException("Invalid reset marker");String id=LedgerWire.text(j,"id",36,true);Identifiers.uuid(id);return id;}
 public static String operation(SyncEngine.Pending operation,ResetBarrier.View view)throws Exception{return new JSONObject().put("schema",3).put("type","operation").put("resets",ids(view.known)).put("body",new JSONObject(SyncJson.operation(operation))).toString();}
 public static final class Operation {public final SortedSet<String> resets;public final SyncEngine.RemoteOperation operation;Operation(SortedSet<String> resets,SyncEngine.RemoteOperation operation){this.resets=resets;this.operation=operation;}}
 public static Operation parseOperation(String fileId,String body)throws Exception{JSONObject j=new JSONObject(body);LedgerWire.keys(j,"schema","type","resets","body");if(LedgerWire.integer(j,"schema")!=3||!LedgerWire.text(j,"type",10,true).equals("operation"))throw new IllegalArgumentException("Invalid epoch operation");return new Operation(parseIds(j.getJSONArray("resets")),SyncJson.parseOperation(fileId,j.getJSONObject("body").toString()));}
}
