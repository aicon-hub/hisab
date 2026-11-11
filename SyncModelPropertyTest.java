import in.aicon.pocket.*;import java.util.*;
public class SyncModelPropertyTest {
 static int count;static void check(boolean b){count++;if(!b)throw new AssertionError(count);}
 static boolean same(SyncModel.Record a,SyncModel.Record b){return a.id.equals(b.id)&&a.deleted==b.deleted&&Objects.equals(a.payload,b.payload)&&a.clock.equals(b.clock);}
 public static void main(String[] args)throws Exception {
 Random random=new Random(34321);for(int i=0;i<10000;i++){
 String id=UUID.randomUUID().toString();Map<String,Long>a=new TreeMap<>(),b=new TreeMap<>();for(String d:new String[]{"a","b","c"}){a.put(d,(long)random.nextInt(100));b.put(d,(long)random.nextInt(100));}
 SyncModel.Record x=new SyncModel.Record(id,"payload",false,a),y=new SyncModel.Record(id,"payload",false,b);
 check(same(SyncModel.merge(x,y),SyncModel.merge(y,x)));check(same(SyncModel.merge(x,x),x));check(SyncModel.union(a,b).equals(SyncModel.union(b,a)));
 SyncModel.Record del=x.edit("a",null,true);check(del.payload==null);try{del.edit("a","revive",false);throw new AssertionError();}catch(IllegalStateException expected){count++;}
 check(SyncModel.merge(x,del).deleted);check(SyncModel.merge(del,x).deleted);
 }
 System.out.println(count+" additional vector-clock/no-resurrection property assertions passed");
 }
}
