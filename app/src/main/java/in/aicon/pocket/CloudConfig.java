package in.aicon.pocket;
/** Public OAuth identifier, not a client secret. Cloud integration is not wired yet. */
public final class CloudConfig {
 private CloudConfig(){}
 // Keep private preview local-only until device/Google and background QA is complete.
 public static final boolean ACCOUNT_UI_ENABLED=false;
 public static final String WEB_CLIENT_ID="859517752237-v5g6jihopv1a0nm3ap6tupon28lboubs.apps.googleusercontent.com";
 public static final String DRIVE_SCOPE="https://www.googleapis.com/auth/drive.appdata";
}
