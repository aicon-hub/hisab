package in.aicon.hisab;
import android.content.Context;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Scope;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Optional backup to the hidden app-data folder inside the user's OWN Google Drive. Off by default, no server of ours.
    The backup is one zip: the data snapshot, attachment records, and the attached bill photos/PDFs themselves.
    Status is visible in Settings: the row shows the account, the last backup time, or a failed state with a retry. */
final class DriveBackup {
 static final String SCOPE = "https://www.googleapis.com/auth/drive.appdata";
 static String file() { return Profiles.driveFile(); }
 static final long AUTO_EVERY = 6L * 3600_000L;
 static final long ZIP_LIMIT = 300L * 1024 * 1024;

 static boolean playReady(Context c) { return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(c) == ConnectionResult.SUCCESS; }
 static boolean enabled(Store db) { return db.flag("driveBackup", false); }
 static String account(Store db) { return db.pref("driveBackupAcc", ""); }
 static long lastBackupAt(Store db) { return Long.parseLong(db.pref("driveBackupAt", "0")); }
 static long errorAt(Store db) { return Long.parseLong(db.pref("driveBackupErr", "0")); }
 static String when(long t) { return new java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.US).format(new java.util.Date(t)); }
 /** Value shown under "Back up to Google Drive now": failure with retry hint, last backup time, or the schedule. */
 static String backupNowValue(long lastAt, long errAt) {
  if (errAt > lastAt) return "Backup failed · tap to retry";
  if (lastAt > 0) return "Last backup " + when(lastAt);
  return "Live, right after every change";
 }
 /** Value shown on the main toggle row. */
 static String toggleValue(boolean on, String account) { return on ? (account.isEmpty() ? "On" : account) : "Off"; }


 // ---- 1.0.31: optional password encryption and photo switch ----
 static final byte[] DMAGIC = {'H','I','S','D','R','1',0,1};
 static android.content.SharedPreferences sec(Context c) { return c.getSharedPreferences("hisab_drive_secret", Context.MODE_PRIVATE); }
 static boolean images(Store db) { return db.flag("driveImages", true); }
 static boolean encrypted(Store db) { return !sec(db.ctx).getString("pw", "").isEmpty(); }
 static void setPassword(Store db, String pw) { sec(db.ctx).edit().putString("pw", pw == null ? "" : pw).apply(); }
 static boolean isEncrypted(byte[] b) { return b != null && b.length > 8 && java.util.Arrays.equals(DMAGIC, java.util.Arrays.copyOf(b, 8)); }
 private static javax.crypto.Cipher cipher(char[] pw, byte[] salt, byte[] nonce, boolean enc) throws Exception {
  javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(pw, salt, 210000, 256);
  byte[] key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); spec.clearPassword();
  javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
  c.init(enc ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"), new javax.crypto.spec.GCMParameterSpec(128, nonce)); c.updateAAD(DMAGIC); return c;
 }
 /** DMAGIC + salt(16) + nonce(12) + AES-256-GCM(zip). Key from PBKDF2-HMAC-SHA256, 210000 rounds. */
 static byte[] encrypt(byte[] zip, char[] pw) throws Exception {
  byte[] salt = new byte[16], nonce = new byte[12]; java.security.SecureRandom r = new java.security.SecureRandom(); r.nextBytes(salt); r.nextBytes(nonce);
  byte[] ct = cipher(pw, salt, nonce, true).doFinal(zip);
  ByteArrayOutputStream o = new ByteArrayOutputStream(); o.write(DMAGIC); o.write(salt); o.write(nonce); o.write(ct); return o.toByteArray();
 }
 static byte[] decrypt(byte[] b, char[] pw) throws Exception {
  if (!isEncrypted(b) || b.length < 37) throw new IOException("Not an encrypted Hisab backup");
  return cipher(pw, java.util.Arrays.copyOfRange(b, 8, 24), java.util.Arrays.copyOfRange(b, 24, 36), false).doFinal(b, 36, b.length - 36);
 }
 /** What goes to Drive: the zip, or the zip encrypted with the phone-held password when the switch is on. */
 static byte[] payload(Store db) throws Exception {
  byte[] zip = buildZip(db, images(db));
  String pw = sec(db.ctx).getString("pw", "");
  return pw.isEmpty() ? zip : encrypt(zip, pw.toCharArray());
 }
 /** Settings switch: turn encryption on (asks for a password twice) or off. Either way a fresh backup follows. */
 static void encryptSwitch(MainActivity a) {
  Store db = a.db;
  if (encrypted(db)) { Sheets.confirm(a, "Stop encrypting the backup?", "The Google Drive copy will be saved without your password from now on. Restoring will need only your Google sign-in.", "Turn off", () -> { setPassword(db, ""); backupNow(a, true); a.refresh(); }); return; }
  int pwType = android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
  Sheets.form(a, "Encrypt my backup", new String[]{"Password (8+ characters)", "Repeat password"}, new String[]{"", ""}, new int[]{pwType, pwType}, "Encrypt", v -> {
   if (v[0].length() < 8 || !v[0].equals(v[1])) { a.toast("Use 8+ characters and matching passwords", null); return; }
   setPassword(db, v[0]); backupNow(a, false); a.toast("Backup will be encrypted. Keep the password: Hisab cannot recover it", null);
  }, null);
 }
 static void imagesSwitch(MainActivity a) { Store db = a.db; db.setPref("driveImages", images(db) ? "0" : "1"); backupNow(a, true); a.refresh(); }
 /** Restore step for an encrypted file: the stored password first, then ask. */
 static void withPassword(MainActivity a, byte[] file, java.util.function.Consumer<byte[]> done) {
  String stored = sec(a).getString("pw", "");
  if (!stored.isEmpty()) { try { done.accept(decrypt(file, stored.toCharArray())); return; } catch (Exception ignored) {} }
  int pwType = android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
  Sheets.form(a, "Backup password", new String[]{"Password"}, new String[]{""}, new int[]{pwType}, "Restore", v -> {
   new Thread(() -> { try { byte[] zip = decrypt(file, v[0].toCharArray()); a.runOnUiThread(() -> { setPassword(a.db, v[0]); done.accept(zip); }); } catch (Exception e) { a.runOnUiThread(() -> a.toast("Wrong password. Nothing was changed", null)); } }, "hisab-drive-decrypt").start();
  }, null);
 }

 static GoogleSignInOptions options() { return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new Scope(SCOPE)).build(); }

 static void toggle(MainActivity a) {
  Store db = a.db;
  if (enabled(db)) {
   Sheets.confirm(a, "Turn off Google Drive backup?", "Automatic backups stop. The backup already in your Google Drive stays in your own Google account.", "Turn off", () -> {
    db.setPref("driveBackup", "0"); db.setPref("driveBackupAcc", ""); db.setPref("driveBackupErr", "0");
    GoogleSignIn.getClient(a, options()).signOut();
    a.closeAll(); Settings.open(a);
   });
   return;
  }
  if (!playReady(a)) { a.toast("Google Play services is needed for Drive backup", null); return; }
  GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(a);
  if (acc == null || !GoogleSignIn.hasPermissions(acc, new Scope(SCOPE))) {
   a.toast("Opening Google sign-in...", null);
   a.request(GoogleSignIn.getClient(a, options()).getSignInIntent(), result ->
    GoogleSignIn.getSignedInAccountFromIntent(result)
     .addOnSuccessListener(x -> { db.setPref("driveBackup", "1"); db.setPref("driveBackupAcc", x.getEmail() == null ? "" : x.getEmail()); db.setPref("driveBackupErr", "0"); backupNow(a, false); })
     .addOnFailureListener(e -> { db.setPref("driveBackupErr", "" + System.currentTimeMillis()); a.toast("Sign-in did not complete. Backup stays off", null); a.closeAll(); Settings.open(a); }),
    () -> a.toast("Sign-in cancelled. Backup stays off", null));
  } else {
   db.setPref("driveBackup", "1"); db.setPref("driveBackupAcc", acc.getEmail() == null ? "" : acc.getEmail()); db.setPref("driveBackupErr", "0");
   backupNow(a, false);
  }
 }

 static void backupNow(MainActivity a, boolean silent) {
  Store db = a.db;
  if (!silent) a.toast("Backing up to your Google Drive...", null);
  new Thread(() -> {
   try {
    byte[] zip = payload(db);
    String token = token(a, db);
    String id = findFile(token);
    upload(token, id, zip);
    db.setPref("driveBackupAt", "" + System.currentTimeMillis());
    db.setPref("driveBackupErr", "0");
    a.runOnUiThread(() -> { if (!silent) { a.toast("Backup saved to your Google Drive", null); a.closeAll(); Settings.open(a); } });
   } catch (com.google.android.gms.auth.UserRecoverableAuthException e) {
    db.setPref("driveBackupErr", "" + System.currentTimeMillis());
    a.runOnUiThread(() -> { if (!silent && e.getIntent() != null) { a.toast("Google needs you to sign in again", null); a.request(e.getIntent(), r -> backupNow(a, false)); } });
   } catch (Exception e) {
    db.setPref("driveBackupErr", "" + System.currentTimeMillis());
    a.runOnUiThread(() -> { if (!silent) { a.toast("Backup failed. Check your connection and sign-in", null); a.closeAll(); Settings.open(a); } });
   }
  }, "hisab-drive-backup").start();
 }

 static void auto(MainActivity a) {
  Store db = a.db;
  if (!enabled(db) || !playReady(a)) return;
  if (System.currentTimeMillis() - lastBackupAt(db) < AUTO_EVERY) return;
  backupNow(a, true);
 }

 /** Background backup with no Activity and no toasts. Failures are recorded in driveBackupErr so Settings shows them. */
 static void backupQuiet(Context c, Store db) {
  new Thread(() -> {
   try {
    byte[] zip = payload(db);
    String token = token(c, db);
    String id = findFile(token);
    upload(token, id, zip);
    db.setPref("driveBackupAt", "" + System.currentTimeMillis());
    db.setPref("driveBackupErr", "0");
   } catch (Exception e) { db.setPref("driveBackupErr", "" + System.currentTimeMillis()); notifyFailure(c); }
  }, "hisab-drive-backup").start();
 }

 private static android.os.Handler changeHandler;
 private static Runnable pendingChange;
 private static long lastChangeBackup;
 /** Data changed: back up shortly after, batching a burst of edits into one upload so the Drive copy mirrors the phone, deletions included.
     Uploads are at most one per minute; a change arriving right after an upload is delayed, never dropped. */
 static synchronized void dataChanged(Store db) {
  if (!enabled(db) || !playReady(db.ctx)) return;
  if (changeHandler == null) changeHandler = new android.os.Handler(android.os.Looper.getMainLooper());
  if (pendingChange != null) changeHandler.removeCallbacks(pendingChange);
  long wait = Math.max(4000, 60_000 - (System.currentTimeMillis() - lastChangeBackup));
  pendingChange = () -> { lastChangeBackup = System.currentTimeMillis(); backupQuiet(db.ctx, db); };
  changeHandler.postDelayed(pendingChange, wait);
 }

 static void restoreNow(MainActivity a) {
  Store db = a.db;
  if (!enabled(db)) { a.toast("Turn on Google Drive backup first", null); return; }
  a.toast("Checking your Google Drive...", null);
  new Thread(() -> {
   try {
    String token = token(a, db);
    JSONObject meta = findFileMeta(token);
    if (meta == null) { a.runOnUiThread(() -> a.toast("No Hisab backup found in your Google Drive", null)); return; }
    String id = meta.getString("id");
    final long modified = parseModified(meta.optString("modifiedTime", ""));
    byte[] file = download(token, id);
    if (isEncrypted(file)) { a.runOnUiThread(() -> withPassword(a, file, z -> restoreZip(a, z, modified))); return; }
    restoreZip(a, file, modified); return;
   } catch (Exception e) { a.runOnUiThread(() -> a.toast("Could not reach Google Drive. Check your connection", null)); }
  }, "hisab-drive-restore").start();
 }

 static void restoreZip(MainActivity a, byte[] zip, long modified) {
  Store db = a.db;
  new Thread(() -> {
   try {
    Map<String, byte[]> entries = zipRead(zip);
    byte[] snap = entries.get("snapshot.json");
    if (snap == null) { a.runOnUiThread(() -> a.toast("That Drive file is not a Hisab backup", null)); return; }
    String title = modified > 0 ? "Restore backup from " + when(modified) + "?" : "Replace local data?";
    a.runOnUiThread(() -> Sheets.confirm(a, title, "Everything on this phone, including attached photos, will be replaced by this Google Drive backup. This cannot be undone.", "Restore", () -> {
     try {
      Backup.restore(db, snap);
      File dir = a.getExternalFilesDir("attachments"); if (dir != null) dir.mkdirs();
      if (dir != null) { File[] old = dir.listFiles(); if (old != null) for (File f : old) f.delete(); }
      db.removeAttachAll();
      String list = entries.containsKey("attachments.json") ? new String(entries.get("attachments.json"), StandardCharsets.UTF_8) : "[]";
      JSONArray rows = new JSONArray(list);
      for (int i = 0; i < rows.length() && dir != null; i++) {
       JSONObject row = rows.getJSONObject(i);
       String name = new File(row.getString("name")).getName();
       byte[] bytes = entries.get("att/" + name);
       if (bytes == null) continue;
       File f = new File(dir, name);
       try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(bytes); }
       db.addAttach(row.getLong("tx"), f.getAbsolutePath());
      }
      db.setPref("driveBackup", "1"); db.setPref("driveBackupErr", "0");
      a.closeAll(); a.applyTheme(); a.build(); Reminders.schedule(a, db); a.toast("Backup restored", null);
     } catch (Exception e) { a.toast("Invalid backup. Your data was not changed", null); }
    }));
   } catch (Exception e) { a.runOnUiThread(() -> a.toast("Could not reach Google Drive. Check your connection", null)); }
  }, "hisab-drive-restore").start();
 }

 /** Quiet backup failed: say so with a notification ("Instant updates"), which is a no-op when notifications are off. */
 static void notifyFailure(Context c) {
  try {
   android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
   if (nm == null) return;
   if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new android.app.NotificationChannel("backup_v1", "Backup updates", android.app.NotificationManager.IMPORTANCE_DEFAULT));
   android.content.Intent open = new android.content.Intent(c, MainActivity.class).putExtra("noLaunch", true);
   android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, "backup_v1") : new android.app.Notification.Builder(c);
   b.setSmallIcon(R.drawable.notification_hisab).setContentTitle("Google backup failed").setContentText("Tap to open Hisab and retry from Settings.").setAutoCancel(true).setContentIntent(android.app.PendingIntent.getActivity(c, 42, open, android.app.PendingIntent.FLAG_IMMUTABLE));
   nm.notify(42, b.build());
  } catch (Exception ignored) {}
 }

 /** Drive's RFC3339 modifiedTime, with or without milliseconds. Returns 0 when absent or unparseable. */
 static long parseModified(String s) {
  if (s == null || s.isEmpty()) return 0;
  String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"};
  for (String p : patterns) {
   try { java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(p, java.util.Locale.US); f.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); java.util.Date d = f.parse(s); if (d != null) return d.getTime(); } catch (Exception ignored) {}
  }
  return 0;
 }

 /** snapshot.json + attachments.json + att/<name> entries. Photos and PDFs are stored uncompressed inside the zip (already-compressed formats), JSON deflated. */
static byte[] buildZip(Store db) throws Exception { return buildZip(db, true); }
 static byte[] buildZip(Store db, boolean withImages) throws Exception {
  LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
  entries.put("snapshot.json", Backup.snapshot(db));
  JSONArray rows = new JSONArray();
  try (android.database.Cursor c = db.getReadableDatabase().rawQuery("SELECT tx,path FROM attach ORDER BY id", null)) {
   while (withImages && c.moveToNext()) {
    File f = new File(c.getString(1));
    if (!f.exists() || f.length() == 0 || f.length() > 50L * 1024 * 1024) continue;
    String name = f.getName();
    if (name.contains("/") || name.contains("..")) continue;
    byte[] bytes = readFile(f);
    rows.put(new JSONObject().put("tx", c.getLong(0)).put("name", name));
    entries.put("att/" + name, bytes);
   }
  }
  entries.put("attachments.json", rows.toString().getBytes(StandardCharsets.UTF_8));
  return zipBuild(entries);
 }

 static byte[] readFile(File f) throws IOException {
  try (InputStream in = new FileInputStream(f); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
   byte[] buf = new byte[8192]; int n;
   while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
   return out.toByteArray();
  }
 }

 static boolean stored(String name) { String n = name.toLowerCase(java.util.Locale.US); return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".pdf") || n.endsWith(".webp"); }

 static byte[] zipBuild(LinkedHashMap<String, byte[]> entries) throws IOException {
  ByteArrayOutputStream bos = new ByteArrayOutputStream();
  try (ZipOutputStream z = new ZipOutputStream(bos)) {
   for (Map.Entry<String, byte[]> e : entries.entrySet()) {
    byte[] data = e.getValue();
    ZipEntry ze = new ZipEntry(e.getKey());
    if (stored(e.getKey())) {
     ze.setMethod(ZipEntry.STORED);
     ze.setSize(data.length); ze.setCompressedSize(data.length);
     CRC32 crc = new CRC32(); crc.update(data); ze.setCrc(crc.getValue());
    } else ze.setMethod(ZipEntry.DEFLATED);
    z.putNextEntry(ze);
    z.write(data);
    z.closeEntry();
    if (bos.size() > ZIP_LIMIT) throw new IOException("Backup too large");
   }
  }
  return bos.toByteArray();
 }

 static Map<String, byte[]> zipRead(byte[] zip) throws IOException {
  LinkedHashMap<String, byte[]> out = new LinkedHashMap<>();
  try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
   ZipEntry ze; byte[] buf = new byte[8192];
   while ((ze = z.getNextEntry()) != null) {
    String name = ze.getName();
    if (name.contains("..") || name.startsWith("/")) continue;
    ByteArrayOutputStream entry = new ByteArrayOutputStream(); int n;
    while ((n = z.read(buf)) != -1) { if (entry.size() + n > ZIP_LIMIT) throw new IOException("Backup too large"); entry.write(buf, 0, n); }
    out.put(name, entry.toByteArray());
   }
  }
  return out;
 }

 /** Cached tokens can expire or be revoked server-side; clear once and retry so a stale token does not break auto backup. */
 static String token(Context c, Store db) throws Exception {
  String acc = account(db);
  String scope = "oauth2:" + SCOPE;
  try { return GoogleAuthUtil.getToken(c, acc, scope); }
  catch (GoogleAuthException e) {
   try { GoogleAuthUtil.clearToken(c, GoogleAuthUtil.getToken(c, acc, scope)); } catch (Exception ignored) {}
   return GoogleAuthUtil.getToken(c, acc, scope);
  }
 }

 static JSONObject findFileMeta(String token) throws Exception {
  HttpURLConnection con = get("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,modifiedTime)&q=" + URLEncoder.encode("name='" + file() + "'", "UTF-8"), token);
  String body = read(con);
  JSONArray files = new JSONObject(body).optJSONArray("files");
  return files == null || files.length() == 0 ? null : files.getJSONObject(0);
 }

 static String findFile(String token) throws Exception {
  JSONObject meta = findFileMeta(token);
  return meta == null ? null : meta.getString("id");
 }

 static byte[] download(String token, String id) throws Exception {
  HttpURLConnection con = get("https://www.googleapis.com/drive/v3/files/" + id + "?alt=media", token);
  try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
   byte[] buf = new byte[8192]; int n;
   while ((n = in.read(buf)) != -1) { if (out.size() + n > ZIP_LIMIT) throw new IOException("File too large"); out.write(buf, 0, n); }
   return out.toByteArray();
  }
 }

 static void upload(String token, String id, byte[] data) throws Exception {
  HttpURLConnection con;
  if (id == null) {
   con = (HttpURLConnection) new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart").openConnection();
   con.setRequestMethod("POST");
   byte[] body = createBody(data);
   con.setRequestProperty("Content-Type", "multipart/related; boundary=hisab_boundary");
   send(con, token, body);
  } else {
   con = (HttpURLConnection) new URL("https://www.googleapis.com/upload/drive/v3/files/" + id + "?uploadType=media").openConnection();
   con.setRequestMethod("PATCH");
   con.setRequestProperty("Content-Type", "application/zip");
   send(con, token, data);
  }
 }

 static byte[] createBody(byte[] data) throws IOException {
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  out.write(("--hisab_boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n{\"name\":\"" + file() + "\",\"parents\":[\"appDataFolder\"],\"mimeType\":\"application/zip\"}\r\n--hisab_boundary\r\nContent-Type: application/zip\r\n\r\n").getBytes(StandardCharsets.UTF_8));
  out.write(data);
  out.write("\r\n--hisab_boundary--\r\n".getBytes(StandardCharsets.UTF_8));
  return out.toByteArray();
 }

 static HttpURLConnection get(String url, String token) throws Exception {
  HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
  con.setRequestProperty("Authorization", "Bearer " + token);
  con.setConnectTimeout(20000); con.setReadTimeout(60000);
  if (con.getResponseCode() >= 400) throw new IOException("Drive error " + con.getResponseCode());
  return con;
 }

 static void send(HttpURLConnection con, String token, byte[] body) throws Exception {
  con.setRequestProperty("Authorization", "Bearer " + token);
  con.setConnectTimeout(20000); con.setReadTimeout(60000);
  con.setDoOutput(true);
  try (OutputStream out = con.getOutputStream()) { out.write(body); }
  if (con.getResponseCode() >= 400) throw new IOException("Drive error " + con.getResponseCode());
 }

 static String read(HttpURLConnection con) throws Exception {
  try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
   byte[] buf = new byte[8192]; int n;
   while ((n = in.read(buf)) != -1) { if (out.size() > 1000000) throw new IOException(); out.write(buf, 0, n); }
   return out.toString("UTF-8");
  }
 }
}
