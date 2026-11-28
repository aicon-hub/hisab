package in.aicon.hisab;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Holds the database key. A random 256-bit key is wrapped by an Android Keystore key that never leaves the phone's secure hardware or system store. */
final class SafeStore {
 private static final String ALIAS = "hisab_db_wrap", PREF = "hisab_dbkey", ENTRY = "k1";

 /** The SQLCipher raw-key string, x'<64 hex>'. Created on first use. Never logged or written in clear. */
 static synchronized String rawKey(Context c) throws Exception {
  SharedPreferences sp = c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
  String stored = sp.getString(ENTRY, null);
  SecretKey wrap = wrapKey(stored == null);
  if (stored != null) {
   byte[] all = Base64.decode(stored, Base64.NO_WRAP);
   Cipher ci = Cipher.getInstance("AES/GCM/NoPadding");
   ci.init(Cipher.DECRYPT_MODE, wrap, new GCMParameterSpec(128, all, 0, 12));
   return hex(ci.doFinal(all, 12, all.length - 12));
  }
  byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
  Cipher ci = Cipher.getInstance("AES/GCM/NoPadding"); ci.init(Cipher.ENCRYPT_MODE, wrap);
  byte[] iv = ci.getIV(), enc = ci.doFinal(key), all = new byte[iv.length + enc.length];
  System.arraycopy(iv, 0, all, 0, iv.length); System.arraycopy(enc, 0, all, iv.length, enc.length);
  if (!sp.edit().putString(ENTRY, Base64.encodeToString(all, Base64.NO_WRAP)).commit()) throw new IOException("key not saved");
  return hex(key);
 }

 private static SecretKey wrapKey(boolean create) throws Exception {
  KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
  if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
  if (!create) throw new IOException("database key is missing from this phone");
  KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
  return g.generateKey();
 }

 static String hex(byte[] b) { StringBuilder s = new StringBuilder("x'"); for (byte x : b) s.append(String.format("%02x", x)); return s.append("'").toString(); }
}
