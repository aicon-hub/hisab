package in.aicon.hisab;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import java.util.*;
import java.util.concurrent.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class ModelChatTest {
 static class Fake implements AiEngine.Backend {
  List<byte[]> out = new ArrayList<>(); int i; int loadResult; String[] lastRoles, lastTexts; int loads;
  public int load(String p, int n, int t) { loads++; return loadResult; }
  public String chatPrompt(String[] r, String[] t) { lastRoles = r; lastTexts = t; return "<bos>p"; }
  public int begin(String p, float t) { i = 0; return 5; }
  public byte[] next() { return i < out.size() ? out.get(i++) : null; }
  public void stop() { }
  public void unload() { }
 }
 AiModels.M model;
 MainActivity setup(Fake f) throws Exception {
  MainActivity a = ShotTest.start(false); AiEngine.backend = f; AiEngine.loadedId = ""; model = AiModels.ALL[2];
  AiModels.delete(a, model); java.io.File mf = AiModels.file(a, model); mf.createNewFile(); new java.io.File(mf.getPath() + ".ok").createNewFile();
  a.db.setPref("chatMode", "model"); a.db.setPref("aiModel", model.id); ChatPage.LOG.clear(); return a;
 }
 @After public void tearDown() { AiEngine.backend = AiEngine.JNI; AiEngine.loadedId = ""; }

 @Test public void utf8Boundaries() {
  byte[] ok = "a\u0d2e".getBytes(java.nio.charset.StandardCharsets.UTF_8); assertEquals(ok.length, AiEngine.utf8Complete(ok));
  assertEquals(1, AiEngine.utf8Complete(new byte[]{'a', (byte) 0xE0, (byte) 0xB4}));
  assertEquals(0, AiEngine.utf8Complete(new byte[]{(byte) 0xE0}));
 }
 @Test public void answerStreamsAndKeepsTimingAndHistory() throws Exception {
  Fake f = new Fake(); byte[] ml = "\u0d2e".getBytes("UTF-8");
  f.out.add("Hel".getBytes()); f.out.add(new byte[]{ml[0], ml[1]}); f.out.add(new byte[]{ml[2]}); f.out.add("lo".getBytes());
  MainActivity a = setup(f); ChatPage.open(a);
  android.widget.LinearLayout c = ChatPage.lastC; assertNotNull(c);
  ChatPage.LOG.add(new String[]{"me", "hi there", ""});
  CountDownLatch l = new CountDownLatch(1);
  ModelChat.send(a, c, "hi there", ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  org.robolectric.shadows.ShadowLooper.idleMainLooper();
  assertFalse(AiEngine.running); String[] last = ChatPage.LOG.get(ChatPage.LOG.size() - 1);
  assertEquals("Hel\u0d2elo", last[1]); assertTrue(last[2].startsWith("AI model")); assertTrue(last[2].contains("4 pieces"));
  assertEquals("system", f.lastRoles[0]); assertEquals("user", f.lastRoles[f.lastRoles.length - 1]); assertEquals("hi there", f.lastTexts[f.lastTexts.length - 1]);
  assertEquals(1, f.loads);
  // second question: model stays loaded, previous answer goes in as history
  f.i = 0; ChatPage.LOG.add(new String[]{"me", "again", ""});
  ModelChat.send(a, c, "again", ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  assertEquals(1, f.loads); assertTrue(Arrays.asList(f.lastRoles).contains("assistant"));
 }
 @Test public void loadFailureIsSaidInPlainWords() throws Exception {
  Fake f = new Fake(); f.loadResult = 2; MainActivity a = setup(f); ChatPage.open(a);
  ChatPage.LOG.add(new String[]{"me", "q", ""});
  ModelChat.send(a, ChatPage.lastC, "q", ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  org.robolectric.shadows.ShadowLooper.idleMainLooper();
  String t = ChatPage.LOG.get(ChatPage.LOG.size() - 1)[1]; assertTrue(t, t.contains("Not enough memory"));
 }
 @Test public void switchShowsAndHisabModeStaysRuleBased() throws Exception {
  Fake f = new Fake(); MainActivity a = setup(f); a.db.setPref("chatMode", "hisab"); ChatPage.open(a);
  assertTrue(ChatPage.lastC.getChildCount() > 3); assertFalse(ModelChat.on(a));
  a.db.setPref("chatMode", "model"); ChatPage.build(a, ChatPage.lastC); assertTrue(ModelChat.on(a));
  assertEquals(model, ModelChat.current(a)); assertTrue(ModelChat.features(model).contains("chat"));
 }
 @Test public void engineMissingCardAppearsWithoutBackend() throws Exception {
  MainActivity a = ShotTest.start(false); AiEngine.backend = AiEngine.JNI; a.db.setPref("chatMode", "model"); ChatPage.LOG.clear(); ChatPage.open(a);
  assertFalse(AiEngine.installed(a)); assertTrue(ChatPage.lastC.getChildCount() > 3);
 }
}
