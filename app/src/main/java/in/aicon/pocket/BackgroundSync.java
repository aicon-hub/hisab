package in.aicon.pocket;
import android.content.Context;
/** Checked before authorization and again before every remote request/effect. An
 * old queued job cannot upload a different selected account or local-only ledger.
 */
final class BackgroundSync {
 enum Outcome { DONE, RETRY, NEEDS_REVIEW, NO_LONGER_SELECTED }
 interface Authorize{VerifiedDriveAccess get(Context context,AccountDirectory.Entry account)throws Exception;}
 interface Run{void sync(LedgerSession session,LedgerSession.Ticket ticket,VerifiedDriveAccess access,SyncEngine.Guard selection)throws Exception;}
 static Outcome run(Context context,String subject,Authorize authorize,Run run,SyncEngine.Guard cancellation)throws Exception{
  AccountDirectory accounts=new AccountDirectory(context);AccountDirectory.Entry entry=accounts.find(subject);if(entry==null||accounts.disconnectPending(subject)||!subject.equals(accounts.selectedSubject()))return Outcome.NO_LONGER_SELECTED;
  SyncEngine.Guard selected=()->{cancellation.requireCurrent();try{if(accounts.disconnectPending(subject)||!subject.equals(accounts.selectedSubject())||accounts.find(subject)==null)throw new IllegalStateException("Queued account is no longer selected");}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Account selection cannot be verified",e);}};
  SyncStatusStore statuses=new SyncStatusStore(context,subject);VerifiedDriveAccess access=null;
  try(LedgerSession session=LedgerSession.remembered(context,accounts,subject)){
   selected.requireCurrent();if(session.resetPending()){statuses.set("RESET_PENDING",false);return Outcome.NEEDS_REVIEW;}if(session.conflict(session.ticket())!=null||session.repaymentReview(session.ticket())!=null){statuses.set("CONFLICT",false);return Outcome.NEEDS_REVIEW;}
   access=authorize.get(context,entry);selected.requireCurrent();if(!access.subject.equals(subject)||!entry.email.equalsIgnoreCase(access.email))throw new DriveAppData.NeedsSignIn();access.accessToken(subject);statuses.set("SYNCING",false);
   run.sync(session,session.ticket(),access,selected);selected.requireCurrent();statuses.set("READY",true);return Outcome.DONE;
  }catch(DriveAppData.NeedsSignIn e){statuses.set("NEEDS_SIGN_IN",false);return Outcome.NEEDS_REVIEW;}catch(SyncModel.Conflict|LedgerIntegrity.RepaymentConflict e){statuses.set("CONFLICT",false);return Outcome.NEEDS_REVIEW;}catch(BarrierRemote.ResetObserved e){statuses.set("RESET_DETECTED",false);return Outcome.NEEDS_REVIEW;}catch(DriveAppData.RetryLater|SyncEngine.ChangedDuringSync|java.net.SocketTimeoutException|java.net.UnknownHostException|java.net.ConnectException|java.net.NoRouteToHostException e){statuses.set("RETRY",false);return Outcome.RETRY;}catch(Exception e){try{selected.requireCurrent();}catch(Exception stopped){return Outcome.NO_LONGER_SELECTED;}statuses.set("ERROR",false);return Outcome.NEEDS_REVIEW;}finally{if(access!=null)access.clear();}
 }
}
