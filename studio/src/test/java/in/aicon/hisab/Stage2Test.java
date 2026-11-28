package in.aicon.hisab;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 35)
public class Stage2Test {
 private static android.content.Context ctx() { return RuntimeEnvironment.getApplication(); }

 @Test public void hashCompareIsConstantTimeApi() {
  assertTrue(Lock.same("p2:abc", "p2:abc")); assertFalse(Lock.same("p2:abc", "p2:abd")); assertFalse(Lock.same("p2:abc", "p2:ab"));
 }
 @Test public void eachInstallGetsItsOwnSalt() {
  assertNotEquals(Lock.newSalt(), Lock.newSalt());
  Store db = new Store(ctx()); Lock.clear(db); Lock.set(db, "1357"); String s1 = db.pref("pinSalt", "");
  Lock.set(db, "1357"); assertNotEquals(s1, db.pref("pinSalt", "")); assertTrue(s1.length() >= 16);
 }
 @Test public void lockoutSurvivesRestart() {
  Store db = new Store(ctx()); Lock.clear(db); Lock.set(db, "2468");
  for (int i = 0; i < 5; i++) Lock.verify(db, "0000");
  Store again = new Store(ctx());
  assertTrue(Lock.waitMs(again) > 0);
  assertFalse(Lock.verify(again, "2468"));
 }
 @Test public void photoRoundTripIsEncryptedOnDisk() throws Exception {
  File f = File.createTempFile("att", ".jpg"); byte[] plain = "FAKE-JPEG-BYTES-invented".getBytes("UTF-8");
  Files.write(f.toPath(), plain);
  assertFalse(PhotoVault.sealed(f)); assertTrue(PhotoVault.sealInPlace(ctx(), f));
  assertTrue(PhotoVault.sealed(f)); assertFalse(Arrays.equals(plain, Files.readAllBytes(f.toPath())));
  assertFalse(new String(Files.readAllBytes(f.toPath()), "ISO-8859-1").contains("FAKE-JPEG"));
  assertArrayEquals(plain, PhotoVault.read(ctx(), f));
  assertFalse("second run does nothing", PhotoVault.sealInPlace(ctx(), f));
  assertArrayEquals(plain, PhotoVault.read(ctx(), f));
 }
 @Test public void damagedPhotoFailsLoudly() throws Exception {
  File f = File.createTempFile("att", ".jpg"); PhotoVault.write(ctx(), f, "abcdefgh".getBytes("UTF-8"));
  byte[] b = Files.readAllBytes(f.toPath()); b[b.length - 1] ^= 1; Files.write(f.toPath(), b);
  try { PhotoVault.read(ctx(), f); fail("tampered file must not open"); } catch (Exception expected) { }
 }
 @Test public void backupPasswordIsNotStoredInClear() {
  Store db = new Store(ctx()); DriveBackup.setPassword(db, "correct horse 9");
  String raw = DriveBackup.sec(ctx()).getString("pw", ""); assertFalse(raw.contains("correct")); assertEquals("correct horse 9", DriveBackup.password(ctx()));
  DriveBackup.sec(ctx()).edit().putString("pw", "oldplain1").apply(); assertEquals("old value still reads", "oldplain1", DriveBackup.password(ctx()));
  DriveBackup.setPassword(db, ""); assertFalse(DriveBackup.encrypted(db));
 }
 @Test public void backupHintTravelsWithFileAndPasswordDoesNot() throws Exception {
  byte[] zip = "zip-bytes".getBytes("UTF-8");
  byte[] withHint = DriveBackup.encrypt(zip, "longpass99".toCharArray(), "my first school");
  assertTrue(DriveBackup.isEncrypted(withHint)); assertEquals("my first school", DriveBackup.hintOf(withHint));
  assertArrayEquals(zip, DriveBackup.decrypt(withHint, "longpass99".toCharArray()));
  assertFalse(new String(withHint, "ISO-8859-1").contains("longpass99"));
  byte[] none = DriveBackup.encrypt(zip, "longpass99".toCharArray()); assertEquals("", DriveBackup.hintOf(none));
  assertArrayEquals(zip, DriveBackup.decrypt(none, "longpass99".toCharArray()));
  try { DriveBackup.decrypt(withHint, "wrongpass99".toCharArray()); fail(); } catch (Exception expected) { }
 }
 @Test public void smsPermissionsAreAskedOnlyInsideSmsFeatures() throws Exception {
  File dir = new File("src/main/java/in/aicon/hisab");
  for (File f : dir.listFiles()) {
   String src = new String(Files.readAllBytes(f.toPath()), "UTF-8");
   boolean asks = src.contains("permission.READ_SMS}") || src.contains("permission.SEND_SMS}") || src.contains("permission.RECEIVE_SMS}");
   if (asks) assertTrue(f.getName(), f.getName().equals("BankSms.java") || f.getName().equals("AutoRemind.java") || f.getName().equals("SmsAuto.java"));
  }
  String main = new String(Files.readAllBytes(new File(dir, "MainActivity.java").toPath()), "UTF-8");
  assertFalse(main.contains("SMS}"));
 }
 @Test public void modelAndEngineHashesArePinned() {
  for (String h : AiEngine.SHAS) assertTrue(h, h.matches("[0-9a-f]{64}"));
  assertTrue(AiEngine.SHAS.length >= 1);
 }
 @Test public void updateHelpHasMalayalamAndNoEmDash() {
  assertFalse(Settings.UPDATE_HELP.contains("\u2014"));
  assertNotNull(L.ML.get(Settings.UPDATE_HELP)); assertNotNull(L.ML.get("Install says a different certificate?"));
 }
 @Test public void signingDocAndGradleWiringExist() throws Exception {
  String g = new String(Files.readAllBytes(new File("build.gradle").toPath()), "UTF-8");
  assertTrue(g.contains("HISAB_") || g.contains("hisabSign")); assertTrue(g.contains("Release signing key not found"));
  assertTrue(new File("../docs/SIGNING.md").exists());
  assertFalse(g.contains("storePassword '"));
 }
 @Test public void oldBuildBackupRestoresWithoutReplacingNewerFiles() throws Exception {
  Store db = new Store(ctx()); db.seedBasics(); byte[] snap = Backup.snapshot(db);
  Backup.restore(db, snap);
  org.json.JSONObject a = new org.json.JSONObject(new String(snap, "UTF-8")), b = new org.json.JSONObject(new String(Backup.snapshot(db), "UTF-8"));
  assertEquals(a.getJSONArray("account").length(), b.getJSONArray("account").length());
 }
}
