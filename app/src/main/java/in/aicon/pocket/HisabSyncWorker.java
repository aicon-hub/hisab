package in.aicon.pocket;
import android.content.Context;import androidx.annotation.NonNull;import androidx.work.*;
/** Scheduled only when the cloud feature gate is enabled and a Google ledger is
 * explicitly selected. WorkManager timing is best-effort, never a sync guarantee. */
public final class HisabSyncWorker extends Worker {
 public HisabSyncWorker(@NonNull Context context,@NonNull WorkerParameters parameters){super(context,parameters);}
 @NonNull @Override public Result doWork(){if(!CloudConfig.ACCOUNT_UI_ENABLED)return Result.success();String subject=getInputData().getString("subject");if(subject==null)return Result.failure();try{BackgroundSync.Outcome outcome=BackgroundSync.run(getApplicationContext(),subject,GoogleSilentAccess::obtain,(session,ticket,access,selected)->session.synchronize(ticket,access,selected),()->{if(isStopped())throw new IllegalStateException("Sync worker stopped");});return outcome==BackgroundSync.Outcome.RETRY&&getRunAttemptCount()<5?Result.retry():Result.success();}catch(Exception e){return Result.failure();}}
}
