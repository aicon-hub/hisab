package in.aicon.hisab;

import java.security.MessageDigest;
import java.security.SecureRandom;

/** Optional PIN lock. Only a salted SHA-256 hash is stored, never the PIN. The unlocked state lives in this process, so a fresh app start asks again. */
final class Lock {
 private Lock() {}
 /** True while this process has passed the PIN screen. Not persisted. */
 static boolean unlocked;

 static boolean enabled(Store db) { return !db.pref("pinHash", "").isEmpty(); }

 static String hash(String salt, String pin) {
  try {
   MessageDigest md = MessageDigest.getInstance("SHA-256");
   byte[] d = md.digest((salt + ":" + pin).getBytes("UTF-8"));
   StringBuilder b = new StringBuilder();
   for (byte x : d) b.append(String.format("%02x", x & 0xff));
   return b.toString();
  } catch (Exception e) { return ""; }
 }

 static String newSalt() {
  byte[] s = new byte[16]; new SecureRandom().nextBytes(s);
  StringBuilder b = new StringBuilder(); for (byte x : s) b.append(String.format("%02x", x & 0xff));
  return b.toString();
 }

 /** 4 to 8 digits. */
 static boolean valid(String pin) { return pin != null && pin.matches("[0-9]{4,8}"); }

 static void set(Store db, String pin) {
  String salt = newSalt();
  db.setPref("pinSalt", salt); db.setPref("pinHash", hash(salt, pin));
  unlocked = true;
 }

 static boolean verify(Store db, String pin) {
  return enabled(db) && valid(pin) && hash(db.pref("pinSalt", ""), pin).equals(db.pref("pinHash", ""));
 }

 static boolean bio(Store db) { return db.flag("bioUnlock", false); }

 static void setBio(Store db, boolean on) { db.setPref("bioUnlock", on ? "1" : "0"); }

 static void clear(Store db) {
  db.setPref("pinHash", ""); db.setPref("pinSalt", "");
  unlocked = true;
 }
 /** True when the phone itself has a PIN, pattern, password or fingerprint screen lock. */
 static boolean phoneLockSet(android.content.Context c) {
  android.app.KeyguardManager km = (android.app.KeyguardManager) c.getSystemService(android.content.Context.KEYGUARD_SERVICE);
  return km != null && km.isDeviceSecure();
 }
 /** The phone's own "confirm your screen lock" screen. Hisab never sees the phone PIN. */
 static android.content.Intent phoneLockIntent(android.content.Context c) {
  android.app.KeyguardManager km = (android.app.KeyguardManager) c.getSystemService(android.content.Context.KEYGUARD_SERVICE);
  return km == null || !km.isDeviceSecure() ? null : km.createConfirmDeviceCredentialIntent("Hisab", "Confirm your phone lock to reset Hisab's app lock");
 }
}
