package in.aicon.hisab;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.io.*;

/** Opens the ledger. On a phone it is always an encrypted SQLCipher file. Plain SQLite is used only when running on a computer (unit tests), never on Android. */
abstract class Sql {
 static final boolean PHONE = "Dalvik".equals(System.getProperty("java.vm.name"));
 private final Context ctx; private final String name; private final int version;
 private Object helper; private Db db;

 Sql(Context c, String name, int version) { ctx = c.getApplicationContext(); this.name = name; this.version = version; }
 abstract void onCreate(Db d);
 abstract void onUpgrade(Db d, int a, int b);

 synchronized Db getWritableDatabase() { return open(); }
 synchronized Db getReadableDatabase() { return open(); }
 synchronized void close() { try { if (helper instanceof net.zetetic.database.sqlcipher.SQLiteOpenHelper) ((net.zetetic.database.sqlcipher.SQLiteOpenHelper) helper).close(); else if (helper != null) ((android.database.sqlite.SQLiteOpenHelper) helper).close(); } finally { helper = null; db = null; } }

 private Db open() {
  if (db != null) return db;
  if (!PHONE) {
   android.database.sqlite.SQLiteOpenHelper h = new android.database.sqlite.SQLiteOpenHelper(ctx, name, null, version) {
    @Override public void onCreate(android.database.sqlite.SQLiteDatabase d) { Sql.this.onCreate(plain(d)); }
    @Override public void onUpgrade(android.database.sqlite.SQLiteDatabase d, int a, int b) { Sql.this.onUpgrade(plain(d), a, b); }
   };
   helper = h; return db = plain(h.getWritableDatabase());
  }
  try {
   System.loadLibrary("sqlcipher");
   String key = SafeStore.rawKey(ctx);
   File f = ctx.getDatabasePath(name);
   Migrate.ifPlain(f, key, version);
   net.zetetic.database.sqlcipher.SQLiteOpenHelper h = new net.zetetic.database.sqlcipher.SQLiteOpenHelper(ctx, name, key.getBytes("UTF-8"), null, version, 1, null, null, false) {
    @Override public void onCreate(net.zetetic.database.sqlcipher.SQLiteDatabase d) { Sql.this.onCreate(cipher(d)); }
    @Override public void onUpgrade(net.zetetic.database.sqlcipher.SQLiteDatabase d, int a, int b) { Sql.this.onUpgrade(cipher(d), a, b); }
   };
   helper = h; Db r = cipher(h.getWritableDatabase());
   Migrate.finishCleanup(ctx, f);
   return db = r;
  } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new IllegalStateException("Cannot open the encrypted ledger: " + e, e); }
 }

 static Db plain(final android.database.sqlite.SQLiteDatabase d) {
  return new Db() {
   public Cursor rawQuery(String s, String[] a) { return d.rawQuery(s, a); }
   public long insert(String t, String n, ContentValues v) { return d.insert(t, n, v); }
   public long insertOrThrow(String t, String n, ContentValues v) { return d.insertOrThrow(t, n, v); }
   public long insertWithOnConflict(String t, String n, ContentValues v, int c) { return d.insertWithOnConflict(t, n, v, c); }
   public int update(String t, ContentValues v, String w, String[] a) { return d.update(t, v, w, a); }
   public int delete(String t, String w, String[] a) { return d.delete(t, w, a); }
   public void execSQL(String s) { d.execSQL(s); }
   public void execSQL(String s, Object[] a) { d.execSQL(s, a); }
   public void beginTransaction() { d.beginTransaction(); }
   public void setTransactionSuccessful() { d.setTransactionSuccessful(); }
   public void endTransaction() { d.endTransaction(); }
  };
 }

 static Db cipher(final net.zetetic.database.sqlcipher.SQLiteDatabase d) {
  return new Db() {
   public Cursor rawQuery(String s, String[] a) { return a == null ? d.rawQuery(s, new String[0]) : d.rawQuery(s, a); }
   public long insert(String t, String n, ContentValues v) { return d.insert(t, n, v); }
   public long insertOrThrow(String t, String n, ContentValues v) { return d.insertOrThrow(t, n, v); }
   public long insertWithOnConflict(String t, String n, ContentValues v, int c) { return d.insertWithOnConflict(t, n, v, c); }
   public int update(String t, ContentValues v, String w, String[] a) { return d.update(t, v, w, a); }
   public int delete(String t, String w, String[] a) { return d.delete(t, w, a); }
   public void execSQL(String s) { d.execSQL(s); }
   public void execSQL(String s, Object[] a) { d.execSQL(s, a); }
   public void beginTransaction() { d.beginTransaction(); }
   public void setTransactionSuccessful() { d.setTransactionSuccessful(); }
   public void endTransaction() { d.endTransaction(); }
  };
 }
}
