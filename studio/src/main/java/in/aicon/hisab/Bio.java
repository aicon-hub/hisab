package in.aicon.hisab;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CancellationSignal;

/** Fingerprint or face unlock through the phone's own prompt. Nothing biometric is ever seen or stored by Hisab. */
final class Bio {
 private Bio() {}

 static boolean available(MainActivity a) {
  if (Build.VERSION.SDK_INT < 28) return false;
  PackageManager pm = a.getPackageManager();
  if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || pm.hasSystemFeature(PackageManager.FEATURE_FACE) || pm.hasSystemFeature(PackageManager.FEATURE_IRIS)) return true;
  if (Build.VERSION.SDK_INT >= 29) {
   android.hardware.biometrics.BiometricManager bm = a.getSystemService(android.hardware.biometrics.BiometricManager.class);
   return bm != null && bm.canAuthenticate() == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS;
  }
  return false;
 }

 static void unlock(MainActivity a, Runnable ok) {
  if (!available(a)) return;
  try {
   android.hardware.biometrics.BiometricPrompt p = new android.hardware.biometrics.BiometricPrompt.Builder(a)
    .setTitle("Unlock Hisab").setSubtitle("Use your fingerprint or face")
    .setNegativeButton("Use PIN", a.getMainExecutor(), (d, w) -> {})
    .build();
   p.authenticate(new CancellationSignal(), a.getMainExecutor(), new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
    @Override public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult r) { ok.run(); }
   });
  } catch (Exception ignored) {}
 }
}
