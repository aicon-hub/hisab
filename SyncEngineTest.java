import in.aicon.pocket.*;import java.util.*;import java.io.*;
public class SyncEngineTest {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError(n);}
 static boolean equal(SyncModel.Record a,SyncModel.Record b){return a!=null&&b!=null&&a.deleted==b.deleted&&Objects.equals(a.payload,b.payload)&&a.clock.equals(b.clock);}
 static class DB implements SyncEngine.Store {
  long revision;Map<String,SyncModel.Record> state=new TreeMap<>();Map<String,SyncEngine.Pending> out=new LinkedHashMap<>();boolean reject;
  void edit(SyncModel.Record r){state.put(r.id,r);queue(r);revision++;}
  void queue(SyncModel.Record r){String op=UUID.randomUUID().toString();out.put(op,new SyncEngine.Pending(op,null,r));}
  public SyncEngine.Snapshot snapshot(){return new SyncEngine.Snapshot(revision,state);}
  public boolean commitMerge(long rev,Map<String,SyncModel.Record> values){if(reject||rev!=revision)return false;for(String id:values.keySet())if(!equal(state.get(id),values.get(id)))queue(values.get(id));state=new TreeMap<>(values);revision++;return true;}
  public List<SyncEngine.Pending> pending(){return new ArrayList<>(out.values());}
  public SyncEngine.Pending reserve(String op,String id){SyncEngine.Pending old=out.get(op);if(old==null)return null;if(old.fileId==null){old=new SyncEngine.Pending(op,id,old.record);out.put(op,old);}return old;}
  public void acknowledge(String op,String id){if(out.containsKey(op)&&id.equals(out.get(op).fileId))out.remove(op);}
 }
 static class Drive implements SyncEngine.Remote {
  Map<String,SyncEngine.RemoteOperation> files=new TreeMap<>();int next;boolean fail;
  public List<SyncEngine.RemoteOperation> pull(){return new ArrayList<>(files.values());}
  public String reserveFileId(){return "drive_"+(++next);}
  public void append(SyncEngine.Pending p)throws Exception{if(fail)throw new IOException("offline");SyncEngine.RemoteOperation old=files.get(p.fileId);if(old!=null&&!equal(old.record,p.record))throw new IOException("collision");files.put(p.fileId,new SyncEngine.RemoteOperation(p.fileId,p.operationId,p.record));}
  public void deleteContentFile(String id){files.remove(id);}
 }
 static void run(DB db,Drive d)throws Exception{new SyncEngine(db,d,()->{},records->{}).sync();}
 static SyncModel.Record base(){return new SyncModel.Record(UUID.randomUUID().toString(),"original",false,new TreeMap<>());}
 public static void main(String[] args)throws Exception {
  for(int i=0;i<500;i++){
   DB a=new DB(),b=new DB();Drive d=new Drive();SyncModel.Record original=base().edit("a","first",false);a.edit(original);run(a,d);run(b,d);ok(equal(a.state.get(original.id),b.state.get(original.id)));
   b.edit(original.edit("b","offline edit",false));a.edit(original.edit("a",null,true));run(a,d);run(b,d);run(a,d);ok(a.state.get(original.id).deleted);ok(b.state.get(original.id).deleted);ok(d.files.values().stream().allMatch(x->x.record.deleted));
  }
  DB a=new DB();Drive d=new Drive();SyncModel.Record r=base().edit("a","text",false);a.edit(r);d.fail=true;try{run(a,d);throw new AssertionError();}catch(IOException expected){n++;}ok(a.pending().size()==1);String reserved=a.pending().get(0).fileId;ok(reserved!=null);d.fail=false;run(a,d);ok(d.files.containsKey(reserved));ok(a.pending().isEmpty());
  DB b=new DB();b.edit(base());b.reject=true;int count=d.files.size();try{run(b,d);throw new AssertionError();}catch(SyncEngine.ChangedDuringSync expected){n++;}ok(d.files.size()==count);
  SyncModel.Record x=base(),left=x.edit("a","one",false),right=x.edit("b","two",false),resolved;
  try{SyncModel.merge(left,right);throw new AssertionError();}catch(SyncModel.Conflict c){resolved=SyncModel.resolve(c,"a","reviewed",false);}
  List<SyncModel.Record> history=Arrays.asList(x,left,right,resolved);
  for(int i=0;i<1000;i++){Collections.shuffle(history,new Random(i));ok(SyncModel.mergeRecords(history).payload.equals("reviewed"));}
  SyncModel.Record dead=x.edit("a",null,true);SyncModel.Record malformed=new SyncModel.Record(x.id,"resurrection",false,Collections.singletonMap("a",100L));ok(SyncModel.merge(dead,malformed).deleted);
  System.out.println(n+" multi-device/offline/retry/conflict-resolution/deletion cases passed");
 }
}
