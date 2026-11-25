package in.aicon.hisab;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One-time first-launch screen: optional Google sign-in for Drive backup, with a clear skip.
    Shown once; the choice is stored in the driveOnboard pref and never asked again. Backup stays opt-in. */
final class Onboard {
 private Onboard() {}

 static void maybeShow(MainActivity a) {
  Store db = a.db;
  if (!db.pref("driveOnboard", "").isEmpty()) return;
  if (a.lockLayer != null) { a.root.postDelayed(() -> maybeShow(a), 1200); return; }

  FrameLayout layer = new FrameLayout(a); layer.setBackgroundColor(Ui.bg); layer.setElevation(Ui.dp(80));
  LinearLayout col = Ui.col(a); col.setGravity(Gravity.CENTER_HORIZONTAL); col.setPadding(Ui.dp(32), 0, Ui.dp(32), 0);
  col.addView(Ui.badge(a, "upload", 64, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(64), Ui.dp(64), 0, 0, 0, 0));
  TextView h = Ui.big(a, "Back up your Hisab", 24, Ui.ink); h.setPadding(0, Ui.dp(20), 0, Ui.dp(6)); col.addView(h);
  TextView body = Ui.text(a, "Your entries stay on this phone. If you want, Hisab can also keep a private backup in the hidden app folder of your own Google Drive, so a new phone can restore everything.", 14, Ui.muted, false);
  body.setGravity(Gravity.CENTER); col.addView(body);

  Runnable dismiss = () -> { db.setPref("driveOnboard", "1"); a.root.removeView(layer); };

  TextView signIn = Ui.button(a, "Sign in with Google", true); col.addView(signIn, Ui.lpm(-1, -2, 0, 26, 0, 0));
  Ui.tap(signIn, () -> {
   if (!DriveBackup.playReady(a)) { a.toast("Google Play services is needed for Drive backup. You can skip for now", null); return; }
   com.google.android.gms.auth.api.signin.GoogleSignInAccount acc = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(a);
   if (acc == null || !com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(acc, new com.google.android.gms.common.api.Scope(DriveBackup.SCOPE))) {
    a.toast("Opening Google sign-in...", null);
    a.request(com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(a, DriveBackup.options()).getSignInIntent(), result ->
     com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result)
      .addOnSuccessListener(x -> {
       db.setPref("driveBackup", "1"); db.setPref("driveBackupAcc", x.getEmail() == null ? "" : x.getEmail()); db.setPref("driveBackupErr", "0");
       dismiss.run(); a.refresh(); DriveBackup.backupNow(a, false);
      })
      .addOnFailureListener(e -> a.toast("Sign-in did not complete. You can retry or skip", null)),
     () -> a.toast("Sign-in cancelled. You can retry or skip", null));
   } else {
    db.setPref("driveBackup", "1"); db.setPref("driveBackupAcc", acc.getEmail() == null ? "" : acc.getEmail()); db.setPref("driveBackupErr", "0");
    dismiss.run(); a.refresh(); DriveBackup.backupNow(a, false);
   }
  });

  TextView skip = Ui.button(a, "Skip for now", false); col.addView(skip, Ui.lpm(-1, -2, 0, 12, 0, 0));
  Ui.tap(skip, dismiss::run);
  TextView note = Ui.text(a, "You can turn backup on or off any time in Settings.", 12.5f, Ui.muted, false); note.setGravity(Gravity.CENTER); note.setPadding(0, Ui.dp(16), 0, 0); col.addView(note);

  layer.addView(col, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
  a.root.addView(layer, new FrameLayout.LayoutParams(-1, -1));
 }
}
