package in.aicon.hisab;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Keeps the last crash on the phone only. Nothing is sent anywhere; the user can share it by hand. */
final class CrashLog {
 private CrashLog() { }
 static File file(Context c) { return new File(c.getFilesDir(), "last_problem.txt"); }
 static void install(Context c) {
  final Context app = c.getApplicationContext();
  final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
  if (old instanceof Handler) return;
  Thread.setDefaultUncaughtExceptionHandler(new Handler(app, old));
 }
 static final class Handler implements Thread.UncaughtExceptionHandler {
  final Context app; final Thread.UncaughtExceptionHandler old;
  Handler(Context app, Thread.UncaughtExceptionHandler old) { this.app = app; this.old = old; }
  @Override public void uncaughtException(Thread t, Throwable e) {
   try { write(app, t, e); } catch (Throwable ignored) { }
   if (old != null) old.uncaughtException(t, e);
  }
 }
 static void write(Context c, Thread t, Throwable e) throws IOException {
  StringWriter sw = new StringWriter(); e.printStackTrace(new PrintWriter(sw));
  String s = "Hisab Studio " + "test build, " + new java.util.Date() + "\nAndroid " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + "), " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
   + "\nThread: " + t.getName() + "\n\n" + sw;
  if (s.length() > 12000) s = s.substring(0, 12000);
  try (FileOutputStream o = new FileOutputStream(file(c))) { o.write(s.getBytes(StandardCharsets.UTF_8)); }
 }
 static String read(Context c) {
  try (FileInputStream i = new FileInputStream(file(c))) { ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] x = new byte[4096]; int n; while ((n = i.read(x)) > 0) b.write(x, 0, n); return b.toString("UTF-8"); } catch (IOException e) { return null; }
 }
 static void show(final MainActivity a) {
  final String s = read(a);
  Kit.page(a, "Last problem", null, null, c -> {
   c.addView(Ui.text(a, s == null ? "No problem recorded. If Hisab closes by itself, open this page again afterwards." : "The last time Hisab closed because of an error, this was recorded. It has no entries or amounts, only the error. It never leaves the phone unless you share it.", 14, Ui.ink, false));
   if (s != null) {
    c.addView(Ui.text(a, s, 11, Ui.muted, false), Ui.lpm(-1, -2, 0, 10, 0, 0));
    TextView share = Ui.button(a, "Share", true); Ui.tap(share, () -> { Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s); a.startActivity(Intent.createChooser(i, "Share")); });
    c.addView(share, Ui.lpm(-1, -2, 0, 10, 0, 0));
    TextView clr = Ui.button(a, "Clear", false); Ui.tap(clr, () -> { file(a).delete(); show(a); });
    c.addView(clr, Ui.lpm(-1, -2, 0, 8, 0, 0));
   }
  });
 }
}
