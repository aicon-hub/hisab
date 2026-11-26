package in.aicon.hisab;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs a downloaded model on this phone. The engine library (llama.cpp, MIT licence) is an optional download so the app stays small.
 * Nothing leaves the phone: the model and your questions stay in this process.
 */
final class AiEngine {
 private AiEngine() {}
 static final String URL = "https://github.com/aicon-hub/hisab/releases/download/v1.0.27-pro-test/libhisabllm-arm64.so";
 static final long SIZE = 6948224L;
 static final String SHA = "d2f6183e55b19a01be0af104ecfc32b52ae026ec4d92c651ef67cb1f1a771503";
 static final int CTX = 4096;

 /** What the engine needs from native code, so tests can swap in a fake. */
 interface Backend {
  int load(String path, int nCtx, int threads); String chatPrompt(String[] roles, String[] texts); int begin(String prompt, float temp);
  byte[] next(); void stop(); void unload();
 }
 static final Backend JNI = new Backend() {
  public int load(String p, int n, int t) { return LlamaJni.load(p, n, t); }
  public String chatPrompt(String[] r, String[] t) { return LlamaJni.chatPrompt(r, t); }
  public int begin(String p, float t) { return LlamaJni.begin(p, t); }
  public byte[] next() { return LlamaJni.next(); }
  public void stop() { LlamaJni.stop(); }
  public void unload() { LlamaJni.unload(); }
 };
 static Backend backend = JNI; static boolean nativeReady;

 private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
 static File file(android.content.Context c) { File d = new File(c.getFilesDir(), "ai-engine"); d.mkdirs(); return new File(d, "libhisabllm.so"); }
 static boolean installed(android.content.Context c) { return backend != JNI || (file(c).isFile() && new File(file(c).getPath() + ".ok").isFile()); }

 // ---- engine download ----
 static volatile boolean dl; static volatile long done; static volatile String dlError;
 static void download(android.content.Context c, Runnable progress, java.util.function.Consumer<String> end) {
  if (dl) return; dl = true; dlError = null; done = 0;
  POOL.execute(() -> {
   String err = null;
   try {
    File out = file(c), part = new File(out.getPath() + ".part"); new File(out.getPath() + ".ok").delete(); out.delete(); part.delete();
    HttpURLConnection h = (HttpURLConnection) new URL(URL).openConnection(); h.setConnectTimeout(20000); h.setReadTimeout(30000); h.setInstanceFollowRedirects(true);
    if (h.getResponseCode() != 200) throw new IOException("Download failed (" + h.getResponseCode() + ")");
    try (InputStream in = h.getInputStream(); OutputStream o = new FileOutputStream(part)) {
     byte[] buf = new byte[1 << 16]; int n; long last = 0;
     while ((n = in.read(buf)) > 0) { o.write(buf, 0, n); done += n; if (done - last > 200000) { last = done; progress.run(); } }
    }
    if (part.length() != SIZE || !AiModels.sha256(part).equals(SHA)) { part.delete(); throw new IOException("The engine file did not match its checksum and was discarded."); }
    part.setReadOnly(); if (!part.renameTo(out)) throw new IOException("Could not save the engine."); out.setReadOnly(); new File(out.getPath() + ".ok").createNewFile();
   } catch (Exception e) { err = e.getMessage() == null ? "Download failed" : e.getMessage(); }
   dl = false; dlError = err; end.accept(err);
  });
 }
 static void delete(android.content.Context c) { POOL.execute(() -> { unloadNow(); File f = file(c); f.delete(); new File(f.getPath() + ".ok").delete(); }); }

 // ---- running ----
 static volatile boolean running, loading; static volatile String loadedId = "", stage = "", error;
 static volatile StringBuilder live = new StringBuilder(); static volatile int tokens; static volatile long tStart, tFirst, tEnd, loadMs, promptMs;
 static volatile boolean stopped;
 static String lastNote = "";

 static int threads() { int n = Runtime.getRuntime().availableProcessors(); return n >= 8 ? 4 : Math.max(2, n / 2); }
 static void unloadNow() { try { if (!loadedId.isEmpty()) backend.unload(); } catch (Throwable ignored) { } loadedId = ""; }

 static double tps() { long end = tEnd > 0 ? tEnd : System.currentTimeMillis(); return tFirst <= 0 || end <= tFirst || tokens < 2 ? 0 : (tokens - 1) * 1000.0 / (end - tFirst); }

 /** Starts one answer. update runs on the worker thread after every piece; end runs on the worker thread with null on success. */
 static void ask(android.content.Context c, AiModels.M m, String[] roles, String[] texts, float temp, Runnable update, java.util.function.Consumer<String> end) {
  if (running) return;
  running = true; stopped = false; error = null; live = new StringBuilder(); tokens = 0; tStart = System.currentTimeMillis(); tFirst = 0; tEnd = 0; loadMs = 0; promptMs = 0; stage = "Starting the engine";
  POOL.execute(() -> {
   String err = null;
   try {
    if (backend == JNI && !nativeReady) { System.load(file(c).getAbsolutePath()); nativeReady = true; }
    if (!m.id.equals(loadedId)) {
     unloadNow(); stage = "Loading " + m.name; update.run(); long t = System.currentTimeMillis();
     int r = backend.load(AiModels.file(c, m).getAbsolutePath(), CTX, threads());
     if (r != 0) throw new IOException(r == 2 ? "Not enough memory to run " + m.name + ". Close other apps or pick a smaller model." : "Could not open the model file.");
     loadedId = m.id; loadMs = System.currentTimeMillis() - t;
    }
    stage = "Reading your question"; update.run(); long t1 = System.currentTimeMillis();
    String prompt = backend.chatPrompt(roles, texts); if (prompt == null) throw new IOException("This model has no chat format Hisab can use.");
    int n = backend.begin(prompt, temp);
    if (n < 0) throw new IOException(n == -3 ? "That is too long for this model (about 4,000 words at most). Shorten it." : "The model could not read the question (" + n + ").");
    promptMs = System.currentTimeMillis() - t1; stage = "Answering"; update.run();
    ByteArrayOutputStream pend = new ByteArrayOutputStream(); int guard = 0;
    while (!stopped && guard++ < 1500) {
     byte[] b = backend.next(); if (b == null) break;
     if (tFirst == 0) tFirst = System.currentTimeMillis(); tokens++;
     pend.write(b, 0, b.length); byte[] all = pend.toByteArray(); int cut = utf8Complete(all);
     if (cut > 0) { live.append(new String(all, 0, cut, "UTF-8")); pend.reset(); pend.write(all, cut, all.length - cut); }
     update.run();
    }
   } catch (Throwable e) { err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); if (e instanceof UnsatisfiedLinkError) err = "The AI engine could not start on this phone (" + e.getMessage() + ")."; }
   tEnd = System.currentTimeMillis(); running = false; stage = ""; error = err;
   lastNote = m.name + ": " + tokens + " pieces, " + String.format(java.util.Locale.US, "%.1f", tps()) + " per second, first word after " + String.format(java.util.Locale.US, "%.1f", (tFirst > 0 ? tFirst - tStart : tEnd - tStart) / 1000.0) + " s" + (loadMs > 0 ? " (model load " + String.format(java.util.Locale.US, "%.1f", loadMs / 1000.0) + " s)" : "");
   end.accept(err);
  });
 }
 /** Number of leading bytes that form whole UTF-8 characters. */
 static int utf8Complete(byte[] b) {
  int n = b.length, i = n - 1, back = 0;
  while (i >= 0 && back < 4 && (b[i] & 0xC0) == 0x80) { i--; back++; }
  if (i < 0) return 0;
  int lead = b[i] & 0xFF, need = lead >= 0xF0 ? 4 : lead >= 0xE0 ? 3 : lead >= 0xC0 ? 2 : 1;
  return (n - i) >= need ? n : i;
 }
 static void stop() { stopped = true; try { backend.stop(); } catch (Throwable ignored) { } }
}
