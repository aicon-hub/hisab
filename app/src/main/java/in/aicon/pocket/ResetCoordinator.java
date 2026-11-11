package in.aicon.pocket;
import android.database.sqlite.SQLiteDatabase;import java.util.*;

/** Durable cloud-reset retry flow. UI must disclose retained content-free markers.
 * Database work shares the session lock; slow network never holds that lock.
 */
public final class ResetCoordinator {
 public interface Remote extends BarrierRemote.Transport {void publishReset(String fileId,String resetId)throws Exception;}
 private final SQLiteDatabase db;private final ResetIntentStore intents;private final ResetStore barriers;private final Remote remote;private final SyncEngine.Guard guard;private final Object lock;
 private interface Read<T>{T run()throws Exception;}
 private <T>T stored(Read<T> action)throws Exception{synchronized(lock){guard.requireCurrent();return action.run();}}
 public ResetCoordinator(SQLiteDatabase db,String subject,Remote remote,SyncEngine.Guard guard){this(db,subject,remote,guard,db);}
 public ResetCoordinator(SQLiteDatabase db,String subject,Remote remote,SyncEngine.Guard guard,Object lock){this.db=db;this.intents=new ResetIntentStore(db,subject);barriers=new ResetStore(db,subject);this.remote=remote;this.guard=guard;this.lock=lock;}
 private ResetBarrier.View remoteView()throws Exception{guard.requireCurrent();Map<String,String> markers=remote.resetMarkers();guard.requireCurrent();if(markers.size()>ResetBarrier.MAX_RESETS)throw new IllegalArgumentException("Too many reset markers");Set<String> ids=new HashSet<>();for(String body:markers.values())ids.add(ResetWire.parseMarker(body));return new ResetBarrier.View(ids);}
 public int resume()throws Exception{
  ResetIntentStore.Intent intent=stored(()->intents.current());if(intent==null)throw new IllegalStateException("No explicit reset is pending");
  if(intent.phase.equals("publish")){
   ResetBarrier.View before=stored(()->barriers.view());ResetBarrier.View observed=remoteView();before.inspect(observed.known);
   if(intent.fileId==null){String reserved=remote.reserveFileId();String id=intent.id;intent=stored(()->intents.reserve(id,reserved));}
   guard.requireCurrent();remote.publishReset(intent.fileId,intent.id);guard.requireCurrent();observed=remoteView();if(!observed.known.contains(intent.id))throw new IllegalStateException("Reset marker not visible yet. Retry without uploading ledger data.");ResetBarrier.Check check=before.inspect(observed.known);String id=intent.id;
   stored(()->{db.beginTransaction();try{barriers.applyObserved(check);intents.markCleanup(id);db.setTransactionSuccessful();return null;}finally{db.endTransaction();}});
  }
  intent=stored(()->intents.current());if(intent==null||!intent.phase.equals("cleanup"))throw new IllegalStateException("Reset phase changed");ResetBarrier.View local=stored(()->barriers.view()),observed=remoteView();local.inspect(observed.known);if(!observed.known.contains(intent.id))throw new IllegalStateException("Reset history incomplete; cleanup paused");
  Map<String,String> files=remote.operationFiles();guard.requireCurrent();if(files.size()>100000)throw new IllegalArgumentException("Too many operation files");int removed=0;ResetBarrier.Check current=observed.inspect(observed.known);
  for(Map.Entry<String,String> file:files.entrySet()){ResetWire.Operation operation=ResetWire.parseOperation(file.getKey(),file.getValue());if(current.accepts(operation.resets))continue;guard.requireCurrent();remote.deleteOperationFile(file.getKey());removed++;}
  ResetBarrier.View finalView=remoteView();String id=intent.id;stored(()->{ResetBarrier.Check last=barriers.view().inspect(finalView.known);db.beginTransaction();try{if(last.resetObserved)barriers.applyObserved(last);intents.complete(id);db.setTransactionSuccessful();return null;}finally{db.endTransaction();}});return removed;
 }
}
