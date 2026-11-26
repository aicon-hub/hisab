package in.aicon.hisab;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class AiModelsTest {
 static AiModels.Phone ph(int gb, int cores, boolean arm, long free) { AiModels.Phone p = new AiModels.Phone(); p.ramGb = gb; p.cores = cores; p.arm64 = arm; p.freeBytes = free; return p; }
 static AiModels.M m(String id) { for (AiModels.M x : AiModels.ALL) if (x.id.equals(id)) return x; throw new AssertionError(id); }
 static final long G = 1073741824L;

 @Test public void nominalRamRoundsToBoxSize() {
  assertEquals(4, AiModels.nominalGb((long) (3.7 * G))); assertEquals(6, AiModels.nominalGb((long) (5.5 * G)));
  assertEquals(8, AiModels.nominalGb((long) (7.4 * G))); assertEquals(12, AiModels.nominalGb((long) (11.2 * G))); assertEquals(16, AiModels.nominalGb((long) (15.1 * G)));
  assertEquals(2, AiModels.nominalGb((long) (1.8 * G)));
 }
 @Test public void tiersMatchRamAndNothingIsHiddenAboveSixteen() {
  AiModels.Phone p4 = ph(4, 8, true, 20_000_000_000L);
  assertEquals(AiModels.OK, AiModels.rate(p4, m("qwen05"))); assertEquals(AiModels.OK, AiModels.rate(p4, m("llama1")));
  assertEquals(AiModels.BORDER, AiModels.rate(p4, m("qwen15"))); assertEquals(AiModels.NO, AiModels.rate(p4, m("qwen7"))); assertEquals(AiModels.NO, AiModels.rate(p4, m("qwen14")));
  AiModels.Phone p8 = ph(8, 8, true, 20_000_000_000L);
  assertEquals(AiModels.OK, AiModels.rate(p8, m("qwen3b"))); assertEquals(AiModels.NO, AiModels.rate(p8, m("qwen14")));
  AiModels.Phone p16 = ph(16, 8, true, 40_000_000_000L);
  assertEquals(AiModels.OK, AiModels.rate(p16, m("gemma9"))); assertEquals(AiModels.OK, AiModels.rate(p16, m("qwen7"))); assertEquals(AiModels.BORDER, AiModels.rate(p16, m("qwen14")));
  for (AiModels.M x : AiModels.ALL) assertTrue(x.minRam <= 16 && x.sha.length() == 64 && x.size > 0);
 }
 @Test public void unfitPhonesAreMarkedNo() {
  assertEquals(AiModels.NO, AiModels.rate(ph(16, 8, false, 40_000_000_000L), m("qwen05")));
  assertEquals(AiModels.NO, AiModels.rate(ph(8, 8, true, 100_000_000L), m("qwen05")));
  assertEquals(AiModels.BORDER, AiModels.rate(ph(8, 4, true, 20_000_000_000L), m("qwen15")));
  assertTrue(AiModels.why(ph(4, 8, true, 20_000_000_000L), m("qwen14")).contains("too big"));
 }
 @Test public void phoneCheckReadsWithoutCrashing() throws Exception {
  MainActivity a = ShotTest.start(false); AiModels.Phone p = AiModels.check(a);
  assertTrue(p.cores >= 1 && p.ramGb >= 1); assertNotNull(p.gpu); assertNotNull(p.npu);
 }
 @Test public void pageShowsTiersCheckAndModels() throws Exception {
  MainActivity a = ShotTest.start(false); ModelsPage.phone = null; ModelsPage.open(a);
  android.widget.LinearLayout c = ModelsPage.col; assertNotNull(c);
  assertNotNull(c.findViewWithTag("qwen05")); assertNotNull(c.findViewWithTag("qwen14"));
  ModelsPage.phone = ph(4, 8, true, 20_000_000_000L); ModelsPage.build(a, c);
  assertNotNull(c.findViewWithTag("qwen15")); ModelsPage.phone = null;
 }
 @Test public void downloadVerifiesResumesAndDeletes() throws Exception {
  byte[] data = new byte[700_000]; new java.util.Random(7).nextBytes(data);
  String sha = AiModelsTestUtil.sha(data);
  java.net.ServerSocket srv = new java.net.ServerSocket(0, 5, java.net.InetAddress.getByName("127.0.0.1"));
  Thread th = new Thread(() -> {
   try { while (true) { java.net.Socket so = srv.accept(); java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(so.getInputStream()));
    String line, range = null; while ((line = in.readLine()) != null && !line.isEmpty()) if (line.toLowerCase().startsWith("range:")) range = line.substring(6).trim();
    int from = 0; boolean part = range != null; if (part) from = Integer.parseInt(range.replace("bytes=", "").replace("-", ""));
    java.io.OutputStream o = so.getOutputStream(); o.write(((part ? "HTTP/1.1 206 Partial Content" : "HTTP/1.1 200 OK") + "\r\nContent-Length: " + (data.length - from) + "\r\nConnection: close\r\n\r\n").getBytes()); o.write(data, from, data.length - from); o.flush(); so.close(); } } catch (Exception e) { }
  }); th.setDaemon(true);
  th.start(); AiModels.testBase = "http://127.0.0.1:" + srv.getLocalPort() + "/";
  try {
   MainActivity a = ShotTest.start(false);
   AiModels.M mm = new AiModels.M("t", "Test", "x/y", "t.gguf", data.length, sha, 4, "n");
   AiModels.delete(a, mm);
   // resume: pre-seed the first 300000 bytes
   java.io.File part = new java.io.File(AiModels.dir(a), "t.gguf.part"); try (java.io.FileOutputStream o = new java.io.FileOutputStream(part)) { o.write(data, 0, 300_000); }
   assertEquals(300_000, AiModels.partial(a, mm));
   CountDownLatch l = new CountDownLatch(1); String[] res = new String[1];
   AiModels.start(a, mm, () -> { }, e -> { res[0] = e; l.countDown(); });
   assertTrue(l.await(30, TimeUnit.SECONDS)); assertNull(res[0]);
   assertTrue(AiModels.installed(a, mm)); assertEquals(data.length, AiModels.file(a, mm).length()); assertNull(AiModels.active);
   AiModels.delete(a, mm); assertFalse(AiModels.installed(a, mm)); assertFalse(AiModels.file(a, mm).exists());
   // wrong checksum is discarded
   AiModels.M bad = new AiModels.M("b", "Bad", "x/y", "b.gguf", data.length, "0".repeat(64), 4, "n");
   CountDownLatch l2 = new CountDownLatch(1); AiModels.start(a, bad, () -> { }, e -> { res[0] = e; l2.countDown(); });
   assertTrue(l2.await(30, TimeUnit.SECONDS)); assertTrue(res[0].contains("checksum")); assertFalse(AiModels.installed(a, bad)); assertFalse(AiModels.file(a, bad).exists());
  } finally { AiModels.testBase = null; srv.close(); }
 }
}
