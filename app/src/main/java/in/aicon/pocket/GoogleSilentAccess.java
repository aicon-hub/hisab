package in.aicon.pocket;
import android.accounts.Account;import android.content.Context;import com.google.android.gms.auth.api.identity.*;import com.google.android.gms.common.api.Scope;import com.google.android.gms.tasks.Tasks;import java.util.*;import java.util.concurrent.TimeUnit;

/** Worker-only reconnect for previously granted permission. Never opens a screen,
 * launches a resolution, chooses a fallback account or persists an access token.
 */
final class GoogleSilentAccess {
 static VerifiedDriveAccess obtain(Context context,AccountDirectory.Entry account)throws Exception{
  AuthorizationRequest request=AuthorizationRequest.builder().setAccount(new Account(account.email,"com.google")).setRequestedScopes(Arrays.asList(new Scope(CloudConfig.DRIVE_SCOPE),new Scope("openid"),new Scope("https://www.googleapis.com/auth/userinfo.email"),new Scope("https://www.googleapis.com/auth/userinfo.profile"))).build();
  AuthorizationResult result;try{result=Tasks.await(Identity.getAuthorizationClient(context).authorize(request),30,TimeUnit.SECONDS);}catch(java.util.concurrent.TimeoutException e){throw new DriveAppData.RetryLater();}catch(java.util.concurrent.ExecutionException e){Throwable cause=e.getCause();if(cause instanceof com.google.android.gms.common.api.ApiException){int code=((com.google.android.gms.common.api.ApiException)cause).getStatusCode();if(code==4||cause instanceof com.google.android.gms.common.api.ResolvableApiException)throw new DriveAppData.NeedsSignIn();if(code==7)throw new DriveAppData.RetryLater();}throw e;}
  if(result.hasResolution()||!result.getGrantedScopes().contains(CloudConfig.DRIVE_SCOPE)||!result.getGrantedScopes().contains("openid")||!result.getGrantedScopes().contains("https://www.googleapis.com/auth/userinfo.email"))throw new DriveAppData.NeedsSignIn();
  VerifiedDriveAccess access=VerifiedDriveAccess.verify(result.getAccessToken(),account.subject);if(!account.email.equalsIgnoreCase(access.email)){access.clear();throw new DriveAppData.NeedsSignIn();}return access;
 }
}
