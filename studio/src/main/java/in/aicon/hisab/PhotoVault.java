package in.aicon.hisab;

import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Encrypts attached photos and PDFs at rest. File layout: "HSE1", 12-byte nonce, AES-256-GCM data. The key comes from the same phone-held key as the database. */
final class PhotoVault {
 private PhotoVault() {}
 private static final byte[] MAGIC = {'H', 'S', 'E', '1'};
 private static final long MAX = 50L * 1024 * 1024;

 static boolean sealed(File f) {
  if (!f.isFile() || f.length() < 4) return false;
  try (InputStream in = new FileInputStream(f)) { byte[] h = new byte[4]; return in.read(h) == 4 && MessageDigest.isEqual(h, MAGIC); } catch (IOException e) { return false; }
 }

 static byte[] seal(Context c, byte[] plain) throws Exception {
  byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce);
  Cipher ci = cipher(c, Cipher.ENCRYPT_MODE, nonce); byte[] ct = ci.doFinal(plain);
  ByteArrayOutputStream o = new ByteArrayOutputStream(); o.write(MAGIC); o.write(nonce); o.write(ct); return o.toByteArray();
 }

 /** Plain bytes of a stored file. Files from before 1.0.39 are not sealed yet and are returned as they are. */
 static byte[] read(Context c, File f) throws Exception {
  if (f.length() > MAX) throw new IOException("File too large");
  return open(c, DriveBackup.readFile(f));
 }

 /** Plain bytes of stored bytes. Unsealed bytes are returned as they are. */
 static byte[] open(Context c, byte[] all) throws Exception {
  if (all.length < 4 || !MessageDigest.isEqual(java.util.Arrays.copyOf(all, 4), MAGIC)) return all;
  if (all.length < 32) throw new IOException("Damaged file");
  return cipher(c, Cipher.DECRYPT_MODE, java.util.Arrays.copyOfRange(all, 4, 16)).doFinal(all, 16, all.length - 16);
 }

 /** Writes sealed bytes through a temp file, then renames, so a crash never leaves half a photo. */
 static void write(Context c, File f, byte[] plain) throws Exception {
  File tmp = new File(f.getPath() + ".tmp");
  try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(seal(c, plain)); o.getFD().sync(); }
  if (!tmp.renameTo(f)) { tmp.delete(); throw new IOException("Could not save file"); }
 }

 /** Seals one existing plain file in place. Returns true when it changed. */
 static boolean sealInPlace(Context c, File f) throws Exception {
  if (!f.isFile() || f.length() == 0 || sealed(f)) return false;
  write(c, f, read(c, f)); return true;
 }

 /** Seals every attachment still stored in plain form. Safe to run again. Returns how many were sealed. */
 static int sealAll(Store db) {
  int n = 0;
  try (android.database.Cursor cur = db.getReadableDatabase().rawQuery("SELECT path FROM attach", null)) {
   while (cur.moveToNext()) { try { if (sealInPlace(db.ctx, new File(cur.getString(0)))) n++; } catch (Exception skip) { } }
  }
  return n;
 }

 private static Cipher cipher(Context c, int mode, byte[] nonce) throws Exception {
  byte[] key = MessageDigest.getInstance("SHA-256").digest((SafeStore.rawKey(c) + "|photos").getBytes("UTF-8"));
  Cipher ci = Cipher.getInstance("AES/GCM/NoPadding"); ci.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce)); return ci;
 }
}
