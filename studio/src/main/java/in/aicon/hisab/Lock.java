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
   javax.crypto.SecretKeyFactory f = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
   byte[] d = f.generateSecret(new javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt.getBytes("UTF-8"), ROUNDS, 256)).getEncoded();
   StringBuilder b = new StringBuilder("p2:");
   for (byte x : d) b.append(String.format("%02x", x & 0xff));
   return b.toString();
  } catch (Exception e) { return ""; }
 }
 static final int ROUNDS = 120000;
 /** The old one-round check, kept only so a PIN set before 1.0.36 still opens once and is then upgraded. */
 static String legacyHash(String salt, String pin) {
  try {
   MessageDigest md = MessageDigest.getInstance("SHA-256");
   byte[] d = md.digest((salt + ":" + pin).getBytes("UTF-8"));
   StringBuilder b = new StringBuilder();
   for (byte x : d) b.append(String.format("%02x", x & 0xff));
   return b.toString();
  } catch (Exception e) { return ""; }
 }
 /** Milliseconds left in the wrong-PIN pause, set by the last verify. 0 when not paused. */
 static long lastWaitMs;
 /** After 5 wrong tries in a row: 30 s, then 1, 2, 4, 8 minutes, then 15 minutes each time. Resets on a right PIN. */
 static long waitMs(Store db) {
  long until; try { until = Long.parseLong(db.pref("pinLockUntil", "0")); } catch (Exception e) { until = 0; }
  return Math.max(0, until - System.currentTimeMillis());
 }
 static long pauseFor(int fails) { if (fails < 5) return 0; long s = 30L << Math.min(5, fails - 5); return Math.min(s, 900) * 1000L; }

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
  lastWaitMs = 0;
  if (!enabled(db)) return false;
  long w = waitMs(db); if (w > 0) { lastWaitMs = w; return false; }
  String salt = db.pref("pinSalt", ""), stored = db.pref("pinHash", "");
  boolean ok = valid(pin) && (stored.startsWith("p2:") ? hash(salt, pin).equals(stored) : legacyHash(salt, pin).equals(stored));
  if (ok) { db.setPref("pinFails", "0"); db.setPref("pinLockUntil", "0"); if (!stored.startsWith("p2:")) db.setPref("pinHash", hash(salt, pin)); return true; }
  int f; try { f = Integer.parseInt(db.pref("pinFails", "0")) + 1; } catch (Exception e) { f = 1; }
  db.setPref("pinFails", "" + f); long p = pauseFor(f);
  if (p > 0) { db.setPref("pinLockUntil", "" + (System.currentTimeMillis() + p)); lastWaitMs = p; }
  return false;
 }

 static boolean bio(Store db) { return db.flag("bioUnlock", false); }

 static void setBio(Store db, boolean on) { db.setPref("bioUnlock", on ? "1" : "0"); }

 static void clear(Store db) {
  db.setPref("pinHash", ""); db.setPref("pinSalt", ""); db.setPref("pinFails", "0"); db.setPref("pinLockUntil", "0");
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
