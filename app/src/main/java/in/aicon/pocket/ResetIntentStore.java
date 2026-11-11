package in.aicon.pocket;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;

/** Persistent reset intention. UUID and remote file reservation never change on retry.
 * No financial data is stored here. Begin only after an explicit cloud-reset choice.
 */
public final class ResetIntentStore {
 private final SQLiteDatabase db;
 public static final class Intent {public final String id,fileId,phase;Intent(String id,String fileId,String phase){this.id=id;this.fileId=fileId;this.phase=phase;}}
 public ResetIntentStore(SQLiteDatabase db,String subject){if(!new java.io.File(db.getPath()).getName().equals(AccountScope.databaseFor(subject)))throw new IllegalArgumentException("Reset intent requires account ledger");this.db=db;db.execSQL("CREATE TABLE IF NOT EXISTS reset_intent(singleton INTEGER PRIMARY KEY CHECK(singleton=1),uuid TEXT NOT NULL,file_id TEXT,phase TEXT NOT NULL)");}
 public Intent current(){try(Cursor c=db.rawQuery("SELECT uuid,file_id,phase FROM reset_intent WHERE singleton=1",null)){if(!c.moveToFirst())return null;String id=c.getString(0),file=c.isNull(1)?null:c.getString(1),phase=c.getString(2);Identifiers.uuid(id);if(!phase.equals("publish")&&!phase.equals("cleanup"))throw new IllegalStateException("Invalid reset state");return new Intent(id,file,phase);}}
 public Intent begin(String newId){Identifiers.uuid(newId);db.beginTransaction();try{Intent previous=current();if(previous!=null)throw new IllegalStateException("A cloud reset is already pending. Resume it instead.");ContentValues v=new ContentValues();v.put("singleton",1);v.put("uuid",newId);v.put("phase","publish");db.insertOrThrow("reset_intent",null,v);db.setTransactionSuccessful();return current();}finally{db.endTransaction();}}
 public Intent reserve(String id,String fileId){if(fileId==null||!fileId.matches("[A-Za-z0-9_-]{1,200}"))throw new IllegalArgumentException("Invalid reserved file ID");db.beginTransaction();try{Intent pending=require(id);if(pending.fileId==null){ContentValues v=new ContentValues();v.put("file_id",fileId);db.update("reset_intent",v,"singleton=1 AND uuid=? AND file_id IS NULL",new String[]{id});}db.setTransactionSuccessful();return current();}finally{db.endTransaction();}}
 private Intent require(String id){Intent i=current();if(i==null||!i.id.equals(id))throw new IllegalStateException("Reset intent changed");return i;}
 public void markCleanup(String id){if(!db.inTransaction())throw new IllegalStateException("Clearing ledger and advancing reset phase must be atomic");require(id);ContentValues v=new ContentValues();v.put("phase","cleanup");if(db.update("reset_intent",v,"singleton=1 AND uuid=?",new String[]{id})!=1)throw new IllegalStateException("Reset intent changed");}
 public void complete(String id){db.beginTransaction();try{if(!require(id).phase.equals("cleanup"))throw new IllegalStateException("Reset publication is not committed");db.delete("reset_intent","singleton=1 AND uuid=?",new String[]{id});db.setTransactionSuccessful();}finally{db.endTransaction();}}
}
