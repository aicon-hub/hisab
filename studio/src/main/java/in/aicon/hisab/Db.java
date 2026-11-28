package in.aicon.hisab;

import android.content.ContentValues;
import android.database.Cursor;

/** The few database calls Hisab uses. One backend is the encrypted SQLCipher file on a phone, the other is plain SQLite for tests on a computer. */
interface Db {
 int CONFLICT_REPLACE = 5;
 Cursor rawQuery(String sql, String[] args);
 long insert(String table, String nullHack, ContentValues v);
 long insertOrThrow(String table, String nullHack, ContentValues v);
 long insertWithOnConflict(String table, String nullHack, ContentValues v, int conflict);
 int update(String table, ContentValues v, String where, String[] args);
 int delete(String table, String where, String[] args);
 void execSQL(String sql);
 void execSQL(String sql, Object[] args);
 void beginTransaction();
 void setTransactionSuccessful();
 void endTransaction();
}
