package in.aicon.pocket;
import android.content.Context;import android.content.SharedPreferences;
/** Private device status only, excluded from ledger backup. No token/error bodies. */
final class SyncStatusStore {
 static final class Status{final String state;final long success;Status(String state,long success){this.state=state;this.success=success;}}
 private final SharedPreferences preferences;
 SyncStatusStore(Context context,String subject){preferences=context.getSharedPreferences("hisab-sync-status-"+AccountScope.databaseFor(subject),Context.MODE_PRIVATE);}
 synchronized void set(String state,boolean succeeded){if(!java.util.Arrays.asList("SYNCING","READY","NEEDS_SIGN_IN","CONFLICT","RESET_PENDING","RESET_DETECTED","RETRY","ERROR").contains(state))throw new IllegalArgumentException("Invalid status");SharedPreferences.Editor edit=preferences.edit().putString("state",state);if(succeeded)edit.putLong("lastSuccess",System.currentTimeMillis());if(!edit.commit())throw new IllegalStateException("Sync status could not be saved");}
 SharedPreferences preferences(){return preferences;}
 synchronized Status get(){return new Status(preferences.getString("state","OFFLINE"),preferences.getLong("lastSuccess",0));}
}
