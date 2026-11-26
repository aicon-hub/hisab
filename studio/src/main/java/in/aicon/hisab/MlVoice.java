package in.aicon.hisab;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional offline Malayalam voice (Indic Tiny TTS by sulabhkatiyar, CC-BY-4.0; Rasa dataset by AI4Bharat; Vocos vocoder MIT).
 * The user downloads about 91 MB once, straight from Hugging Face; every file is checked against a fixed SHA-256 before it is used.
 * Speaking happens on the phone. Nothing about the user's entries is ever sent.
 */
final class MlVoice {
 private MlVoice() {}
 static final String BASE = "https://huggingface.co/sulabhkatiyar/indic-tiny-tts-malayalam/resolve/720f4f4a26c7133438b9e92223204b12475f456d/";
 /** path, size in bytes, sha256 */
 static final String[][] FILES = {
  {"config.json", "0", "c661ac4ffe1d13628e771adb67c24badc20d64b92257e48e90683908bf766b6a"},
  {"onnx/onnx_manifest.json", "0", "8f8a480de44225047715ea56f8201faac6773497c5d095a26daae5eb3a504cd4"},
  {"onnx/text_encoder.onnx", "17697886", "0effe5e255503c531b4fdc53d7f77ce1e8d03055f5d99e1b8db9d98a335b2c75"},
  {"onnx/flow_decoder.onnx", "19169253", "b8bed09b9c34d9dc8495e05a3b85ca1f8ad1fa0c8828cd8d6c0269a4d4b86ae8"},
  {"onnx/vocoder.onnx", "54150036", "44d36238a761750447038895ab194e2378f6f4ba2c2d089a4766fa52810ef353"}};
 static final long TOTAL = 17697886L + 19169253L + 54150036L;
 static final String CREDIT = "Malayalam voice: Indic Tiny TTS by sulabhkatiyar (CC BY 4.0), trained on the Rasa dataset by AI4Bharat. Vocoder: Vocos (MIT).";

 private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
 private static MlTts engine; private static AudioTrack track; private static volatile boolean downloading; private static volatile long done;
 private static volatile int gen;

 static File dir(MainActivity a) { return new File(a.getFilesDir(), "voice-ml"); }
 static boolean installed(MainActivity a) {
  for (String[] f : FILES) if (!new File(dir(a), f[0]).isFile()) return false;
  return new File(dir(a), "ok").isFile();
 }
 static boolean ready(MainActivity a) { return a.db.flag("mlVoice", true) && installed(a) && !downloading; }
 static boolean busy() { return downloading; }
 static int percent() { return (int) (done * 100 / TOTAL); }

 static String sha256(File f) throws Exception {
  MessageDigest md = MessageDigest.getInstance("SHA-256"); byte[] buf = new byte[1 << 16];
  try (InputStream in = new FileInputStream(f)) { int n; while ((n = in.read(buf)) > 0) md.update(buf, 0, n); }
  StringBuilder b = new StringBuilder(); for (byte x : md.digest()) b.append(String.format("%02x", x)); return b.toString();
 }

 /** Downloads and verifies every file. progress runs on the worker thread; end runs on the UI thread with null on success. */
 static void download(MainActivity a, Runnable progress, java.util.function.Consumer<String> end) {
  if (downloading) return; downloading = true; done = 0;
  POOL.execute(() -> {
   String err = null;
   try {
    File d = dir(a); new File(d, "ok").delete(); new File(d, "onnx").mkdirs();
    for (String[] f : FILES) {
     File out = new File(d, f[0]);
     if (out.isFile() && sha256(out).equals(f[2])) { done += Long.parseLong(f[1]); progress.run(); continue; }
     File part = new File(d, f[0] + ".part");
     HttpURLConnection c = (HttpURLConnection) new URL(BASE + f[0]).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(true);
     if (c.getResponseCode() != 200) throw new IOException("Download failed (" + c.getResponseCode() + ")");
     try (InputStream in = c.getInputStream(); OutputStream o = new FileOutputStream(part)) {
      byte[] buf = new byte[1 << 16]; int n; long last = 0;
      while ((n = in.read(buf)) > 0) { o.write(buf, 0, n); done += n; if (done - last > 400000) { last = done; progress.run(); } }
     }
     if (!sha256(part).equals(f[2])) { part.delete(); throw new IOException("A downloaded file did not match its checksum and was discarded."); }
     if (!part.renameTo(out)) throw new IOException("Could not save the file.");
    }
    new File(d, "ok").createNewFile();
   } catch (Exception e) { err = e.getMessage() == null ? "Download failed" : e.getMessage(); }
   downloading = false; String fe = err; a.runOnUiThread(() -> end.accept(fe));
  });
 }

 static void delete(MainActivity a) {
  POOL.execute(() -> { stop(); if (engine != null) { engine.close(); engine = null; } rm(dir(a)); });
 }
 private static void rm(File f) { File[] k = f.listFiles(); if (k != null) for (File x : k) rm(x); f.delete(); }

 static synchronized void stop() { gen++; try { if (track != null) { track.stop(); track.release(); } } catch (Exception e) { } track = null; }

 /** Speaks Malayalam text with the downloaded voice. onFail runs on the worker thread if anything goes wrong, so the caller can fall back. */
 static void speak(MainActivity a, String text, Runnable onFail) {
  final int my; synchronized (MlVoice.class) { stop(); my = gen; }
  POOL.execute(() -> {
   try {
    if (my != gen) return;
    if (engine == null) engine = new MlTts(dir(a), 2);
    float[] w = engine.synthesize(MlNum.spell(text), "female", 1f, new java.util.Random(), null);
    if (w.length == 0) { onFail.run(); return; }
    short[] pcm = new short[w.length]; for (int i = 0; i < w.length; i++) pcm[i] = (short) Math.round(w[i] * 32767f);
    synchronized (MlVoice.class) {
     if (my != gen) return;
     AudioTrack t = new AudioTrack(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
       new AudioFormat.Builder().setSampleRate(engine.sampleRate()).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
       pcm.length * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
     t.write(pcm, 0, pcm.length); track = t; t.play();
    }
   } catch (Throwable e) { onFail.run(); }
  });
 }
}
