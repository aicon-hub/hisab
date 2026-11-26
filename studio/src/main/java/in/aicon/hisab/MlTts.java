package in.aicon.hisab;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Offline Malayalam text to speech: a Java port of the ONNX runtime of "Indic Tiny TTS" (Malayalam) by sulabhkatiyar.
 * Model weights CC-BY-4.0 (credit the model and the AI4Bharat Rasa dataset), vocoder Vocos MIT, code MIT.
 * Pure Java apart from ONNX Runtime and org.json, so it can be checked on a desktop JVM.
 */
final class MlTts implements AutoCloseable {
 private final OrtEnvironment env = OrtEnvironment.getEnvironment();
 private final OrtSession enc, flow, voc;
 private final Map<String, Integer> id = new HashMap<>();
 private final int lang, nSteps, nFft, hop, frameMultiple, maxTokens, sampleRate;
 private final float temperature, melMean, melStd, pauseS;
 private final Map<String, Integer> speakers = new HashMap<>(); private final Map<String, Float> lengthScale = new HashMap<>();
 private final Map<String, String> voices = new HashMap<>(); private final int style;

 MlTts(File dir, int threads) throws Exception {
  JSONObject cfg = new JSONObject(new String(Files.readAllBytes(new File(dir, "config.json").toPath()), StandardCharsets.UTF_8));
  JSONArray sym = cfg.getJSONObject("vocab").getJSONArray("symbols");
  for (int i = 0; i < sym.length(); i++) id.put(sym.getString(i), i);
  sampleRate = cfg.getInt("sample_rate");
  JSONObject ids = cfg.getJSONObject("ids"); lang = ids.getInt("language");
  JSONObject sp = ids.getJSONObject("speakers"); for (Iterator<String> it = sp.keys(); it.hasNext();) { String k = it.next(); speakers.put(k, sp.getInt(k)); }
  style = ids.getJSONObject("styles").getInt(cfg.getJSONObject("synthesis").optString("style", "read"));
  JSONObject v = cfg.getJSONObject("voices"); for (Iterator<String> it = v.keys(); it.hasNext();) { String k = it.next(); voices.put(k, v.getString(k)); }
  JSONObject syn = cfg.getJSONObject("synthesis"); nSteps = syn.optInt("n_timesteps", 4); temperature = (float) syn.optDouble("temperature", 0.667); pauseS = (float) syn.optDouble("sentence_pause_s", 0.15);
  JSONObject ls = syn.getJSONObject("length_scale"); for (Iterator<String> it = ls.keys(); it.hasNext();) { String k = it.next(); lengthScale.put(k, (float) ls.getDouble(k)); }
  JSONArray ms = cfg.getJSONArray("mel_stats"); melMean = (float) ms.getDouble(0); melStd = (float) ms.getDouble(1);
  JSONObject head = cfg.getJSONObject("vocoder").getJSONObject("head"); nFft = head.getInt("n_fft"); hop = head.getInt("hop_length");
  JSONObject man = new JSONObject(new String(Files.readAllBytes(new File(dir, "onnx/onnx_manifest.json").toPath()), StandardCharsets.UTF_8));
  frameMultiple = man.getInt("frame_multiple"); maxTokens = man.getInt("max_tokens");
  OrtSession.SessionOptions so = new OrtSession.SessionOptions(); so.setIntraOpNumThreads(threads); so.setInterOpNumThreads(1);
  enc = env.createSession(new File(dir, "onnx/text_encoder.onnx").getPath(), so);
  flow = env.createSession(new File(dir, "onnx/flow_decoder.onnx").getPath(), so);
  voc = env.createSession(new File(dir, "onnx/vocoder.onnx").getPath(), so);
 }

 int sampleRate() { return sampleRate; }

 /** Characters the model knows; anything else becomes a space, a few punctuation marks become a comma. */
 String normalize(String text) {
  String t = Normalizer.normalize(text, Normalizer.Form.NFC); StringBuilder b = new StringBuilder();
  for (int i = 0; i < t.length(); i++) {
   char ch = t.charAt(i); String s = String.valueOf(ch);
   if (ch == ' ' || id.containsKey("c:" + s)) b.append(ch);
   else if ((ch == ':' || ch == ';' || ch == '\u2013' || ch == '\u2014') && id.containsKey("c:,")) b.append(',');
   else if (ch == '\u2026') b.append('.');
   else b.append(' ');
  }
  return b.toString().replaceAll("\\s+", " ").trim();
 }

 List<String> chunks(String norm) {
  List<String> out = new ArrayList<>();
  for (String sent : norm.split("(?<=[\u0964?!.])\\s+")) {
   while (sent.length() > 250) {
    int cut = sent.lastIndexOf(',', 249); cut = cut > 0 ? cut + 1 : sent.lastIndexOf(' ', 249); if (cut <= 0) cut = 250;
    out.add(sent.substring(0, cut).trim()); sent = sent.substring(cut).trim();
   }
   out.add(sent.trim());
  }
  List<String> keep = new ArrayList<>(); for (String c : out) { boolean al = false; for (int i = 0; i < c.length(); i++) if (Character.isLetter(c.charAt(i))) { al = true; break; } if (al) keep.add(c); }
  return keep;
 }

 long[] ids(String chunk) {
  long[] o = new long[2 * chunk.length() + 1];
  for (int i = 0; i < chunk.length(); i++) o[2 * i + 1] = id.get("c:" + chunk.charAt(i));
  return o;
 }

 /** Text (Malayalam script, numbers already spelled out) to a mono waveform in [-1, 1]. noise may be null (random). */
 float[] synthesize(String text, String voice, float speed, Random rnd, float[] noiseForFirstChunk) throws Exception {
  String spk = voices.get(voice == null ? "female" : voice);
  float ls = lengthScale.get(spk) / speed;
  List<float[]> pieces = new ArrayList<>(); int total = 0; boolean first = true;
  for (String chunk : chunks(normalize(text))) {
   float[] wav = chunk(ids(chunk), spk, ls, rnd, first ? noiseForFirstChunk : null); first = false;
   if (wav == null) continue;
   if (!pieces.isEmpty()) { float[] p = new float[(int) (pauseS * sampleRate)]; pieces.add(p); total += p.length; }
   pieces.add(wav); total += wav.length;
  }
  float[] out = new float[total]; int o = 0; for (float[] p : pieces) { System.arraycopy(p, 0, out, o, p.length); o += p.length; }
  return out;
 }

 float[] chunk(long[] ids, String spk, float lengthScale, Random rnd, float[] noiseIn) throws Exception {
  if (ids.length > maxTokens) return null;
  int T = ids.length; float[][][] muX; float[] logw; float[][] cond;
  Map<String, OnnxTensor> in = new HashMap<>();
  in.put("tokens", OnnxTensor.createTensor(env, LongBuffer.wrap(ids), new long[]{1, T}));
  in.put("token_lengths", OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{T}), new long[]{1}));
  in.put("language", OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{lang}), new long[]{1}));
  in.put("speaker", OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{speakers.get(spk)}), new long[]{1}));
  in.put("style", OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{style}), new long[]{1}));
  try (OrtSession.Result r = enc.run(in)) {
   muX = (float[][][]) r.get("mu_x").get().getValue(); logw = ((float[][][]) r.get("logw").get().getValue())[0][0]; cond = (float[][]) r.get("cond").get().getValue();
  } finally { for (OnnxTensor t : in.values()) t.close(); }
  float[] wceil = new float[T]; double sum = 0;
  for (int i = 0; i < T; i++) { wceil[i] = (float) Math.ceil((float) Math.exp(logw[i])) * lengthScale; sum += wceil[i]; }
  int yLen = (int) Math.max((float) sum, 1f); int yPad = (yLen + frameMultiple - 1) / frameMultiple * frameMultiple;
  int C = muX[0].length;
  double acc = 0; float[] cum = new float[T]; for (int i = 0; i < T; i++) { acc += wceil[i]; cum[i] = (float) acc; }
  float[] mu = new float[C * yPad]; float[] mask = new float[yPad];
  for (int j = 0; j < yPad; j++) {
   int lo = 0, hi = T; while (lo < hi) { int m = (lo + hi) >>> 1; if (cum[m] > j) hi = m; else lo = m + 1; }
   if (lo < T && j < yLen) { mask[j] = 1f; for (int c = 0; c < C; c++) mu[c * yPad + j] = muX[0][c][lo]; }
  }
  float[] x = new float[C * yPad];
  if (noiseIn != null) System.arraycopy(noiseIn, 0, x, 0, Math.min(noiseIn.length, x.length));
  else for (int i = 0; i < x.length; i++) x[i] = (float) rnd.nextGaussian();
  for (int i = 0; i < x.length; i++) x[i] *= temperature;
  float[] tSpan = new float[nSteps + 1]; for (int i = 0; i <= nSteps; i++) tSpan[i] = (float) i / nSteps;
  float t = tSpan[0], dt = tSpan[1] - tSpan[0];
  float[] condFlat = cond[0];
  for (int step = 1; step <= nSteps; step++) {
   Map<String, OnnxTensor> fin = new HashMap<>();
   fin.put("x", OnnxTensor.createTensor(env, FloatBuffer.wrap(x), new long[]{1, C, yPad}));
   fin.put("mask", OnnxTensor.createTensor(env, FloatBuffer.wrap(mask), new long[]{1, 1, yPad}));
   fin.put("mu", OnnxTensor.createTensor(env, FloatBuffer.wrap(mu), new long[]{1, C, yPad}));
   fin.put("t", OnnxTensor.createTensor(env, FloatBuffer.wrap(new float[]{t}), new long[]{1}));
   fin.put("cond", OnnxTensor.createTensor(env, FloatBuffer.wrap(condFlat), new long[]{1, condFlat.length}));
   try (OrtSession.Result r = flow.run(fin)) {
    float[][][] v = (float[][][]) r.get(0).getValue();
    for (int c = 0; c < C; c++) for (int j = 0; j < yPad; j++) x[c * yPad + j] += dt * v[0][c][j];
   } finally { for (OnnxTensor q : fin.values()) q.close(); }
   t += dt; if (step < nSteps) dt = tSpan[step + 1] - t;
  }
  if (yLen < 2) return null;
  float[] mel = new float[C * yLen];
  for (int c = 0; c < C; c++) for (int j = 0; j < yLen; j++) mel[c * yLen + j] = x[c * yPad + j] * melStd + melMean;
  float[][] re, im;
  try (OnnxTensor mt = OnnxTensor.createTensor(env, FloatBuffer.wrap(mel), new long[]{1, C, yLen}); OrtSession.Result r = voc.run(Collections.singletonMap("mel", mt))) {
   re = ((float[][][]) r.get(0).getValue())[0]; im = ((float[][][]) r.get(1).getValue())[0];
  }
  float[] wav = istft(re, im);
  for (int i = 0; i < wav.length; i++) wav[i] = Math.max(-1f, Math.min(1f, wav[i]));
  return wav;
 }

 /** Inverse STFT with a periodic Hann window and "center" padding, like torch.istft. */
 float[] istft(float[][] re, float[][] im) {
  int F = re.length, T = re[0].length, N = nFft; double[] win = new double[N]; for (int i = 0; i < N; i++) win[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / N);
  int total = N + hop * (T - 1); double[] y = new double[total], env = new double[total];
  double[] xr = new double[N], xi = new double[N];
  for (int t = 0; t < T; t++) {
   Arrays.fill(xr, 0); Arrays.fill(xi, 0);
   for (int k = 0; k < F; k++) { double a = re[k][t], b = (k == 0 || k == N / 2) ? 0 : im[k][t]; xr[k] = a; xi[k] = b; if (k > 0 && k < N / 2) { xr[N - k] = a; xi[N - k] = -b; } }
   fft(xr, xi);
   for (int n = 0; n < N; n++) { y[t * hop + n] += xr[n] / N * win[n]; env[t * hop + n] += win[n] * win[n]; }
  }
  int pad = N / 2; float[] out = new float[total - 2 * pad];
  for (int i = 0; i < out.length; i++) { double e = env[i + pad]; out[i] = (float) (y[i + pad] / (e < 1e-11 ? 1 : e)); }
  return out;
 }
 /** In-place forward complex FFT (size must be a power of two); applied to a Hermitian spectrum it gives N times the inverse real FFT. */
 private static void fft(double[] re, double[] im) {
  int n = re.length;
  for (int i = 1, j = 0; i < n; i++) { int bit = n >> 1; for (; (j & bit) != 0; bit >>= 1) j ^= bit; j ^= bit; if (i < j) { double a = re[i]; re[i] = re[j]; re[j] = a; a = im[i]; im[i] = im[j]; im[j] = a; } }
  for (int len = 2; len <= n; len <<= 1) {
   double ang = 2 * Math.PI / len, wr = Math.cos(ang), wi = Math.sin(ang);
   for (int i = 0; i < n; i += len) { double cr = 1, ci = 0; for (int k = 0; k < len / 2; k++) {
    int u = i + k, v = i + k + len / 2; double tr = re[v] * cr - im[v] * ci, ti = re[v] * ci + im[v] * cr;
    re[v] = re[u] - tr; im[v] = im[u] - ti; re[u] += tr; im[u] += ti; double nr = cr * wr - ci * wi; ci = cr * wi + ci * wr; cr = nr; } }
  }
 }

 @Override public void close() { try { enc.close(); flow.close(); voc.close(); } catch (Exception e) { } }
}
