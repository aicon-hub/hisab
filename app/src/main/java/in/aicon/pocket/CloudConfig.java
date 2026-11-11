package in.aicon.pocket;
/** Public OAuth identifier, not a client secret. Test-only cloud connection is opt-in. */
public final class CloudConfig {
 private CloudConfig(){}
 // Next candidate exposes opt-in account screens. Real Google/Drive device QA is still required.
 public static final boolean ACCOUNT_UI_ENABLED=true;
 public static final String WEB_CLIENT_ID="859517752237-v5g6jihopv1a0nm3ap6tupon28lboubs.apps.googleusercontent.com";
 public static final String DRIVE_SCOPE="https://www.googleapis.com/auth/drive.appdata";
}
