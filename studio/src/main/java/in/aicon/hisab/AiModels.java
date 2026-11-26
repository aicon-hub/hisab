package in.aicon.hisab;

import android.app.ActivityManager;
import android.content.Context;
import android.os.StatFs;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional on-device AI models (GGUF files from Hugging Face). Nothing is bundled, so the app stays small.
 * Files go straight from huggingface.co to this phone and each one is checked against its published SHA-256.
 * Downloading is never automatic. In this version a downloaded model is stored only; Hisab does not run it yet.
 */
final class AiModels {
 private AiModels() {}
 static final class M {
  final String id, name, repo, file, sha, tier, note; final long size; final int minRam;
  M(String id, String name, String repo, String file, long size, String sha, int minRam, String note) {
   this.id = id; this.name = name; this.repo = repo; this.file = file; this.size = size; this.sha = sha; this.minRam = minRam; this.note = note; this.tier = minRam + " GB";
  }
  String url() { if (testBase != null) return testBase + file; return "https://huggingface.co/" + repo + "/resolve/main/" + file; }
  String page() { return "https://huggingface.co/" + repo; }
 }
 /** Sizes and SHA-256 values read from the Hugging Face file listings on 27 Nov 2026. Q4_K_M quantisation. */
 static final M[] ALL = {
  new M("qwen05", "Qwen2.5 0.5B Instruct", "Qwen/Qwen2.5-0.5B-Instruct-GGUF", "qwen2.5-0.5b-instruct-q4_k_m.gguf", 491400032L, "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db", 4, "Smallest and fastest. Short answers, weak Malayalam."),
  new M("llama1", "Llama 3.2 1B Instruct", "bartowski/Llama-3.2-1B-Instruct-GGUF", "Llama-3.2-1B-Instruct-Q4_K_M.gguf", 807694464L, "6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83", 4, "Small and quick. English first."),
  new M("qwen15", "Qwen2.5 1.5B Instruct", "Qwen/Qwen2.5-1.5B-Instruct-GGUF", "qwen2.5-1.5b-instruct-q4_k_m.gguf", 1117320736L, "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e", 6, "Good balance for most phones."),
  new M("gemma2", "Gemma 2 2B Instruct", "bartowski/gemma-2-2b-it-GGUF", "gemma-2-2b-it-Q4_K_M.gguf", 1708582752L, "e0aee85060f168f0f2d8473d7ea41ce2f3230c1bc1374847505ea599288a7787", 6, "Clear writing. Uses more memory than its size suggests."),
  new M("qwen3b", "Qwen2.5 3B Instruct", "Qwen/Qwen2.5-3B-Instruct-GGUF", "qwen2.5-3b-instruct-q4_k_m.gguf", 2104932768L, "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d", 8, "Better understanding of messy sentences."),
  new M("llama3", "Llama 3.2 3B Instruct", "bartowski/Llama-3.2-3B-Instruct-GGUF", "Llama-3.2-3B-Instruct-Q4_K_M.gguf", 2019377696L, "6c1a2b41161032677be168d354123594c0e6e67d2b9227c84f296ad037c728ff", 8, "Strong English, mid-size."),
  new M("qwen7", "Qwen2.5 7B Instruct", "bartowski/Qwen2.5-7B-Instruct-GGUF", "Qwen2.5-7B-Instruct-Q4_K_M.gguf", 4683074240L, "65b8fcd92af6b4fefa935c625d1ac27ea29dcb6ee14589c55a8f115ceaaa1423", 12, "High-end phones. Slow on most."),
  new M("gemma9", "Gemma 2 9B Instruct", "bartowski/gemma-2-9b-it-GGUF", "gemma-2-9b-it-Q4_K_M.gguf", 5761057728L, "13b2a7b4115bbd0900162edcebe476da1ba1fc24e718e8b40d32f6e300f56dfe", 12, "High-end phones. Heavy."),
  new M("qwen14", "Qwen2.5 14B Instruct", "bartowski/Qwen2.5-14B-Instruct-GGUF", "Qwen2.5-14B-Instruct-Q4_K_M.gguf", 8988110976L, "e47ad95dad6ff848b431053b375adb5d39321290ea2c638682577dafca87c008", 16, "16 GB phones only. Very slow on a phone.")};
 static String testBase;
 static final int[] TIERS = {4, 6, 8, 12, 16};

 // ---- phone check ----
 static final class Phone {
  int ramGb; long totalRam; int cores; boolean arm64; String abi = "", soc = "", gpu = "unknown", npu = "not readable by apps"; long freeBytes;
 }
 static Phone check(Context c) {
  Phone p = new Phone();
  try { ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo(); ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi); p.totalRam = mi.totalMem; } catch (Exception e) { }
  p.ramGb = nominalGb(p.totalRam);
  p.cores = Runtime.getRuntime().availableProcessors();
  String[] abis = android.os.Build.SUPPORTED_ABIS; p.abi = abis != null && abis.length > 0 ? abis[0] : ""; p.arm64 = p.abi.contains("arm64");
  try { p.soc = (android.os.Build.VERSION.SDK_INT >= 31 ? android.os.Build.SOC_MANUFACTURER + " " + android.os.Build.SOC_MODEL : android.os.Build.HARDWARE).trim(); } catch (Throwable e) { p.soc = String.valueOf(android.os.Build.HARDWARE); }
  try { StatFs s = new StatFs(c.getFilesDir().getAbsolutePath()); p.freeBytes = s.getAvailableBytes(); } catch (Exception e) { }
  p.gpu = gpuName();
  if (android.os.Build.VERSION.SDK_INT >= 27) p.npu = "Android NN API present; whether it uses an NPU is not readable";
  return p;
 }
 /** totalMem is a little under the label on the box (3.7 GB for a 4 GB phone), so round to the usual sizes. */
 static int nominalGb(long totalMem) {
  double g = totalMem / 1073741824.0 * 1.07; int[] std = {1, 2, 3, 4, 6, 8, 10, 12, 16, 24};
  int best = 1; for (int s : std) if (g >= s - 0.35) best = s; return best;
 }
 /** Reads the GPU name from a throwaway GL context. Returns "unknown" if the phone will not give one. */
 static String gpuName() {
  try {
   android.opengl.EGLDisplay d = android.opengl.EGL14.eglGetDisplay(android.opengl.EGL14.EGL_DEFAULT_DISPLAY);
   int[] v = new int[2]; if (!android.opengl.EGL14.eglInitialize(d, v, 0, v, 1)) return "unknown";
   int[] cfgAttr = {android.opengl.EGL14.EGL_RENDERABLE_TYPE, 4, android.opengl.EGL14.EGL_NONE};
   android.opengl.EGLConfig[] cfg = new android.opengl.EGLConfig[1]; int[] n = new int[1];
   android.opengl.EGL14.eglChooseConfig(d, cfgAttr, 0, cfg, 0, 1, n, 0); if (n[0] == 0) return "unknown";
   android.opengl.EGLContext ctx = android.opengl.EGL14.eglCreateContext(d, cfg[0], android.opengl.EGL14.EGL_NO_CONTEXT, new int[]{0x3098, 2, android.opengl.EGL14.EGL_NONE}, 0);
   android.opengl.EGLSurface sf = android.opengl.EGL14.eglCreatePbufferSurface(d, cfg[0], new int[]{android.opengl.EGL14.EGL_WIDTH, 1, android.opengl.EGL14.EGL_HEIGHT, 1, android.opengl.EGL14.EGL_NONE}, 0);
   android.opengl.EGL14.eglMakeCurrent(d, sf, sf, ctx);
   String r = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER);
   android.opengl.EGL14.eglMakeCurrent(d, android.opengl.EGL14.EGL_NO_SURFACE, android.opengl.EGL14.EGL_NO_SURFACE, android.opengl.EGL14.EGL_NO_CONTEXT);
   android.opengl.EGL14.eglDestroySurface(d, sf); android.opengl.EGL14.eglDestroyContext(d, ctx); android.opengl.EGL14.eglTerminate(d);
   return r == null || r.isEmpty() ? "unknown" : r;
  } catch (Throwable e) { return "unknown"; }
 }

 static final int OK = 0, BORDER = 1, NO = 2;
 /** Rule of thumb, not a benchmark: the file plus working memory has to fit in about half of the RAM. */
 static int rate(Phone p, M m) {
  if (!p.arm64 || p.freeBytes > 0 && p.freeBytes < m.size + 200_000_000L) return NO;
  double need = m.size / 1e9 * 1.2 + 0.4; double ram = p.ramGb;
  int r = need <= ram * 0.5 ? OK : need <= ram * 0.8 ? BORDER : NO; if (r == OK && ram < m.minRam) r = BORDER;
  if (p.cores < 6 && r == OK) r = BORDER; else if (p.cores < 4 && r == BORDER) r = NO;
  return r;
 }
 static String why(Phone p, M m) {
  if (!p.arm64) return "needs a 64-bit ARM phone";
  if (p.freeBytes > 0 && p.freeBytes < m.size + 200_000_000L) return "not enough free storage (" + mb(p.freeBytes) + " free)";
  int r = rate(p, m);
  return r == OK ? "fits in your " + p.ramGb + " GB of memory" : r == BORDER ? "may be slow or get closed in the background" : "too big for " + p.ramGb + " GB of memory";
 }
 static String mb(long b) { return b >= 1_000_000_000L ? String.format(java.util.Locale.US, "%.1f GB", b / 1e9) : Math.round(b / 1e6) + " MB"; }

 // ---- files ----
 static File dir(Context c) { File d = new File(c.getFilesDir(), "ai-models"); d.mkdirs(); return d; }
 static File file(Context c, M m) { return new File(dir(c), m.file); }
 static boolean installed(Context c, M m) { return file(c, m).isFile() && new File(dir(c), m.file + ".ok").isFile(); }
 static long partial(Context c, M m) { File f = new File(dir(c), m.file + ".part"); return f.isFile() ? f.length() : 0; }
 static void delete(Context c, M m) { file(c, m).delete(); new File(dir(c), m.file + ".ok").delete(); new File(dir(c), m.file + ".part").delete(); }
 static String sha256(File f) throws Exception {
  MessageDigest md = MessageDigest.getInstance("SHA-256"); byte[] buf = new byte[1 << 16];
  try (InputStream in = new FileInputStream(f)) { int n; while ((n = in.read(buf)) > 0) md.update(buf, 0, n); }
  StringBuilder b = new StringBuilder(); for (byte x : md.digest()) b.append(String.format("%02x", x)); return b.toString();
 }

 // ---- download (one at a time, resumable) ----
 private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
 static volatile M active; static volatile long done; static volatile boolean pause; static volatile String phase = ""; static volatile String error; static volatile long t0, d0;
 static boolean busy() { return active != null; }
 static int pct(Context c, M m) { long d = active == m ? done : partial(c, m); return (int) Math.min(100, d * 100 / m.size); }
 static long speed() { long dt = System.currentTimeMillis() - t0; return dt < 800 ? 0 : (done - d0) * 1000 / dt; }

 /** Starts or resumes a download. end runs on the worker thread with null on success, "paused" if paused, else the message. */
 static void start(Context c, M m, Runnable progress, java.util.function.Consumer<String> end) {
  if (active != null) return; active = m; pause = false; error = null; phase = "Downloading";
  POOL.execute(() -> {
   String err = null;
   try {
    File out = file(c, m), part = new File(dir(c), m.file + ".part"); new File(dir(c), m.file + ".ok").delete();
    long have = part.isFile() ? part.length() : 0;
    if (c.getFilesDir().getUsableSpace() < m.size - have + 100_000_000L) throw new IOException("Not enough free storage. It needs " + mb(m.size - have + 100_000_000L) + ".");
    done = have; d0 = have; t0 = System.currentTimeMillis();
    if (have < m.size) {
     HttpURLConnection h = (HttpURLConnection) new URL(m.url()).openConnection(); h.setConnectTimeout(20000); h.setReadTimeout(30000); h.setInstanceFollowRedirects(true);
     if (have > 0) h.setRequestProperty("Range", "bytes=" + have + "-");
     int code = h.getResponseCode();
     if (code == 200) have = 0; else if (code != 206) throw new IOException("Download failed (" + code + ")");
     done = have; d0 = have; t0 = System.currentTimeMillis();
     try (InputStream in = h.getInputStream(); OutputStream o = new FileOutputStream(part, have > 0)) {
      byte[] buf = new byte[1 << 16]; int n; long last = 0;
      while ((n = in.read(buf)) > 0) {
       o.write(buf, 0, n); done += n; if (done - last > 300000) { last = done; progress.run(); }
       if (pause) { h.disconnect(); throw new IOException("paused"); }
      }
     }
    }
    phase = "Checking"; progress.run();
    if (part.length() != m.size || !sha256(part).equals(m.sha)) { part.delete(); throw new IOException("The file did not match its checksum and was discarded. Try again."); }
    if (!part.renameTo(out)) throw new IOException("Could not save the file.");
    new File(dir(c), m.file + ".ok").createNewFile();
   } catch (Exception e) { err = e.getMessage() == null ? "Download failed" : e.getMessage(); }
   active = null; phase = ""; error = "paused".equals(err) ? null : err; String fe = err; end.accept(fe);
  });
 }
 static void pause() { pause = true; }
}
