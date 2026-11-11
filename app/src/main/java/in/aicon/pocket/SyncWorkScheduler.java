package in.aicon.pocket;
import android.content.Context;import android.content.SharedPreferences;import androidx.work.*;import java.util.concurrent.TimeUnit;
/** One selected-ledger periodic job and one coalesced edit job. Durable selection
 * guards, not cancellation timing, prevent old jobs uploading the wrong account. */
final class SyncWorkScheduler {
 static final String PERIODIC="hisab-selected-periodic",NOW="hisab-selected-edits";
 private static Data data(String subject){AccountScope.databaseFor(subject);return new Data.Builder().putString("subject",subject).build();}
 static PeriodicWorkRequest periodic(String subject){return new PeriodicWorkRequest.Builder(HisabSyncWorker.class,30,TimeUnit.MINUTES).setInputData(data(subject)).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build();}
 static OneTimeWorkRequest edit(String subject){return new OneTimeWorkRequest.Builder(HisabSyncWorker.class).setInputData(data(subject)).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setInitialDelay(10,TimeUnit.SECONDS).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build();}
 static Operation selected(WorkManager work,String subject,boolean changed){if(changed||subject==null)work.cancelUniqueWork(NOW);if(subject==null){return work.cancelUniqueWork(PERIODIC);}return work.enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.UPDATE,periodic(subject));}
 static void selection(Context context,String subject){if(!CloudConfig.ACCOUNT_UI_ENABLED)return;SharedPreferences settings=context.getSharedPreferences("hisab-work-selection",Context.MODE_PRIVATE);String previous=settings.getString("subject",null);selected(WorkManager.getInstance(context),subject,!java.util.Objects.equals(previous,subject));SharedPreferences.Editor edit=settings.edit();if(subject==null)edit.remove("subject");else edit.putString("subject",subject);if(!edit.commit())throw new IllegalStateException("Could not record background selection");}
 static void edited(Context context,String subject){if(!CloudConfig.ACCOUNT_UI_ENABLED||subject==null)return;WorkManager.getInstance(context).enqueueUniqueWork(NOW,ExistingWorkPolicy.KEEP,edit(subject));}
}
