package in.aicon.pocket;
import java.util.*;import java.io.IOException;
/** Epoch-tagged Drive adapter, not exposed by the preview UI. Live tests required. */
public final class DriveBarrierTransport implements ResetCoordinator.Remote {
 private final DriveAppData drive;
 public DriveBarrierTransport(VerifiedDriveAccess access){this(access,()->{});}
 public DriveBarrierTransport(VerifiedDriveAccess access,SyncEngine.Guard guard){drive=new DriveAppData(subject->{guard.requireCurrent();return access.accessToken(subject);},access.subject);}
 @Override public Map<String,String> resetMarkers()throws Exception{Map<String,String> result=new LinkedHashMap<>();for(String id:drive.listIds("3","reset")){if(result.size()>=ResetBarrier.MAX_RESETS)throw new IOException("Too many reset markers");result.put(id,drive.fetch(id).body);}return result;}
 @Override public Map<String,String> operationFiles()throws Exception{if(!drive.listIds().isEmpty())throw new IOException("An older sync format is present. Explicit migration required; no files overwritten.");Map<String,String> result=new LinkedHashMap<>();for(String id:drive.listIds("3","operation"))result.put(id,drive.fetch(id).body);return result;}
 @Override public String reserveFileId()throws Exception{return drive.reserveFileId();}
 @Override public void appendFile(String fileId,String body)throws Exception{ResetWire.Operation op=ResetWire.parseOperation(fileId,body);drive.append(fileId,op.operation.operationId,body,"3","operation");}
 @Override public void deleteOperationFile(String fileId)throws Exception{drive.deleteContentOperation(fileId);}
 /** Caller must persist this stable file ID and marker UUID before this request. It
  * is deliberately not called by the preview or routine synchronize methods. */
 public void publishReset(String persistedFileId,String persistedResetId)throws Exception{drive.append(persistedFileId,persistedResetId,ResetWire.marker(persistedResetId),"3","reset");}
}
