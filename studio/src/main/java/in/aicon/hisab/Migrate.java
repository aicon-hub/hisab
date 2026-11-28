package in.aicon.hisab;

import android.content.Context;
import android.database.Cursor;
import java.io.*;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

/** One-time move of an old plain ledger file into an encrypted one. The plain copy is kept until the encrypted file has opened once, then wiped. */
final class Migrate {
 private static final String[] TABLES = {"prefs", "account", "category", "person", "tx", "budget", "goal", "recur", "attach"};

 static boolean isPlain(File f) {
  if (!f.exists() || f.length() < 16) return false;
  try (FileInputStream in = new FileInputStream(f)) { byte[] h = new byte[16]; if (in.read(h) < 16) return false; return new String(h, "ISO-8859-1").startsWith("SQLite format 3"); } catch (IOException e) { return false; }
 }

 static void ifPlain(File f, String key, int version) throws Exception {
  if (!isPlain(f)) return;
  File enc = new File(f.getPath() + ".enc"), keep = new File(f.getPath() + ".plain");
  wipe(enc);
  SQLiteDatabase p = SQLiteDatabase.openDatabase(f.getPath(), "", null, SQLiteDatabase.OPEN_READWRITE, null);
  try {
   p.rawExecSQL("ATTACH DATABASE '" + enc.getPath().replace("'", "''") + "' AS enc KEY \"" + key + "\"");
   p.rawExecSQL("SELECT sqlcipher_export('enc')");
   p.rawExecSQL("PRAGMA enc.user_version = " + version);
   p.rawExecSQL("DETACH DATABASE enc");
  } finally { p.close(); }
  SQLiteDatabase e = SQLiteDatabase.openDatabase(enc.getPath(), key.getBytes("UTF-8"), null, SQLiteDatabase.OPEN_READONLY, null);
  SQLiteDatabase q = SQLiteDatabase.openDatabase(f.getPath(), "", null, SQLiteDatabase.OPEN_READONLY, null);
  try {
   for (String t : TABLES) if (count(q, t) != count(e, t)) throw new IOException("Encrypted copy does not match on " + t);
  } finally { e.close(); q.close(); }
  wipe(keep);
  if (!f.renameTo(keep)) throw new IOException("Could not set the old file aside");
  new File(f.getPath() + "-journal").delete();
  if (!enc.renameTo(f)) { keep.renameTo(f); throw new IOException("Could not move the encrypted file in place"); }
 }

 private static long count(SQLiteDatabase d, String t) {
  try (Cursor c = d.rawQuery("SELECT COUNT(*) FROM " + t, new String[0])) { c.moveToFirst(); return c.getLong(0); }
  catch (RuntimeException x) { return -1; }
 }

 /** Called after the encrypted file opened and answered a query. Wipes the old plain copy. */
 static void finishCleanup(Context c, File f) { File keep = new File(f.getPath() + ".plain"); if (keep.exists()) wipe(keep); }

 static void wipe(File f) {
  if (!f.exists()) return;
  try (RandomAccessFile r = new RandomAccessFile(f, "rw")) { byte[] z = new byte[8192]; for (long n = r.length(), i = 0; i < n; i += z.length) r.write(z, 0, (int) Math.min(z.length, n - i)); } catch (IOException ignored) { }
  f.delete();
}
}
