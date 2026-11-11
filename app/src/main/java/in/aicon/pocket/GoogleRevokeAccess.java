package in.aicon.pocket;
import android.accounts.Account;import android.content.Context;import com.google.android.gms.auth.api.identity.*;import com.google.android.gms.common.api.Scope;import com.google.android.gms.tasks.Tasks;import java.util.*;import java.util.concurrent.TimeUnit;
/** Explicit account, no authorization or account picker fallback. Google revokes
 * every scope granted to Hisab for this account, not merely the listed scopes. */
final class GoogleRevokeAccess {
 static void revoke(Context context,AccountDirectory.Entry account)throws Exception{RevokeAccessRequest request=RevokeAccessRequest.builder().setAccount(new Account(account.email,"com.google")).setScopes(Arrays.asList(new Scope(CloudConfig.DRIVE_SCOPE),new Scope("openid"),new Scope("https://www.googleapis.com/auth/userinfo.email"),new Scope("https://www.googleapis.com/auth/userinfo.profile"))).build();Tasks.await(Identity.getAuthorizationClient(context).revokeAccess(request),30,TimeUnit.SECONDS);}
}
