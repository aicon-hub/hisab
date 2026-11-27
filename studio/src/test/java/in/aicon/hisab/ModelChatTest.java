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
  a.db.setPref("aiModel", model.id); ChatPage.LOG.clear(); return a;
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
  ChatPage.LOG.add(new String[]{"me", "hi there", ""});
  CountDownLatch l = new CountDownLatch(1);
  ModelChat.send(a, "hi there", null, ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper();
  assertFalse(AiEngine.running); String[] last = ChatPage.LOG.get(ChatPage.LOG.size() - 1);
  assertEquals("Hel\u0d2elo", last[1]); assertTrue(last[2].startsWith("AI model")); assertTrue(last[2].contains("4 pieces"));
  assertEquals("system", f.lastRoles[0]); assertEquals("user", f.lastRoles[f.lastRoles.length - 1]); assertEquals("hi there", f.lastTexts[f.lastTexts.length - 1]);
  assertEquals(1, f.loads);
  // second question: model stays loaded, previous answer goes in as history
  f.i = 0; ChatPage.LOG.add(new String[]{"me", "again", ""});
  ModelChat.send(a, "again", null, ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  assertEquals(1, f.loads); assertTrue(Arrays.asList(f.lastRoles).contains("assistant"));
 }
 @Test public void loadFailureIsSaidInPlainWords() throws Exception {
  Fake f = new Fake(); f.loadResult = 2; MainActivity a = setup(f); ChatPage.open(a);
  ChatPage.LOG.add(new String[]{"me", "q", ""});
  ModelChat.send(a, "q", null, ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper();
  String t = ChatPage.LOG.get(ChatPage.LOG.size() - 1)[1]; assertTrue(t, t.contains("Not enough memory"));
 }
 @Test public void engineMissingCardAppearsWithoutBackend() throws Exception {
  MainActivity a = ShotTest.start(false); AiEngine.backend = AiEngine.JNI; ChatPage.LOG.clear(); ChatPage.open(a);
  assertFalse(AiEngine.installed(a)); assertNotNull(a.root.findViewWithTag("chatInput"));
 }
 @Test public void variantPick() {
  assertEquals(0, AiEngine.pick(" fp asimd")); assertEquals(1, AiEngine.pick(" fp asimd asimddp")); assertEquals(2, AiEngine.pick(" fp asimd asimddp i8mm"));
  assertTrue(AiEngine.size() > 6_000_000L);
 }
 @Test public void readOffMeansNoDataReachesModel() throws Exception {
  Fake f = new Fake(); f.out.add("ok".getBytes()); MainActivity a = setup(f); ChatPage.open(a);
  assertFalse(ChatPage.read(a)); assertFalse(ChatPage.write(a));
  ChatPage.LOG.add(new String[]{"me", "how much did I spend", ""});
  ModelChat.send(a, "how much did I spend", null, ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  assertEquals(ModelChat.SYSTEM, f.lastTexts[0]);
 }
 @Test public void readOnGivesExactFacts() throws Exception {
  Fake f = new Fake(); f.out.add("ok".getBytes()); MainActivity a = setup(f); ChatPage.open(a);
  String facts = ModelChat.facts(a, "food spend this month", ChatBrain.ask(a.db, "food spend this month", java.time.LocalDate.now()));
  assertTrue(facts.length() > 20 && facts.length() <= 2400);
  ChatPage.LOG.add(new String[]{"me", "q", ""});
  ModelChat.send(a, "q", facts, ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  assertTrue(f.lastTexts[0].startsWith(ModelChat.SYSTEM_DATA)); assertTrue(ChatPage.LOG.get(ChatPage.LOG.size() - 1)[2].contains("Hisab data"));
 }
 @Test public void writeOffNeverChangesEntriesAndWriteOnNeedsConfirm() throws Exception {
  Fake f = new Fake(); MainActivity a = setup(f); ChatPage.open(a); int n = a.db.recent(100000).size();
  a.db.setPref("chatAsked", "1"); ChatPage.submit(a, "tea 20");
  assertNull(ChatPage.pending); assertEquals(n, a.db.recent(100000).size());
  a.db.setPref("chatWrite", "1"); ChatPage.submit(a, "tea 20");
  assertNotNull(ChatPage.pending); assertEquals(n, a.db.recent(100000).size());
  ChatPage.confirm(a); assertEquals(n + 1, a.db.recent(100000).size());
 }
 @Test public void historySavedOnlyWhenAllowed() throws Exception {
  Fake f = new Fake(); MainActivity a = setup(f); ChatPage.open(a);
  ChatPage.LOG.add(new String[]{"me", "secret", ""}); a.db.setPref("chatSave", "0"); ChatPage.save(a); assertEquals("", a.db.pref("chatLog", ""));
  a.db.setPref("chatSave", "1"); ChatPage.save(a); assertTrue(a.db.pref("chatLog", "").contains("secret"));
  ChatPage.LOG.clear(); ChatPage.load(a); assertEquals(1, ChatPage.LOG.size());
 }
 @Test public void loopingModelIsStoppedAndTrimmed() throws Exception {
  Fake f = new Fake(); for (int k = 0; k < 400; k++) f.out.add("Total spent on fun: 10,025. ".getBytes());
  MainActivity a = setup(f); ChatPage.open(a); ChatPage.LOG.add(new String[]{"me", "q", ""});
  ModelChat.send(a, "q", null, ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper();
  assertTrue(AiEngine.tokens < 100); String[] last = ChatPage.LOG.get(ChatPage.LOG.size() - 1); assertTrue(last[2], last[2].contains("repeating"));
  assertTrue(last[1], last[1].split("10,025", -1).length - 1 <= 7);
 }
 @Test public void lengthCapStopsLongAnswers() throws Exception {
  Fake f = new Fake(); for (int k = 0; k < 900; k++) f.out.add(("w" + k + " ").getBytes());
  MainActivity a = setup(f); ChatPage.open(a); ChatPage.LOG.add(new String[]{"me", "q", ""});
  ModelChat.send(a, "q", null, ChatPage.LOG, () -> { });
  for (int k = 0; k < 200 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  assertEquals(600, AiEngine.tokens); assertTrue(ChatPage.LOG.get(ChatPage.LOG.size() - 1)[2].contains("length limit"));
 }
 @Test public void numberCheck() {
  assertTrue(ModelChat.numbersOk("You spent 10,025 this month, 3 times", "Spent 10025.00 total"));
  assertTrue(ModelChat.numbersOk("You spent 10,025.50", "Spent 10,025.50"));
  assertFalse(ModelChat.numbersOk("You spent 10,026", "Spent 10,025"));
 }
 @Test public void groundedAnswerReplacesInventedNumbers() throws Exception {
  Fake f = new Fake(); f.out.add("You spent 99,999 on food.".getBytes());
  MainActivity a = setup(f); ChatPage.open(a); ChatPage.LOG.add(new String[]{"me", "food?", ""});
  ModelChat.send(a, "food?", "facts", "Food this month: 2,295", ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper();
  String[] last = ChatPage.LOG.get(ChatPage.LOG.size() - 1); assertEquals("Food this month: 2,295", last[1]); assertEquals(ModelChat.SYSTEM_GROUND, f.lastTexts[0]);
  f.i = 0; f.out.clear(); f.out.add("Food was 2,295 this month.".getBytes()); ChatPage.LOG.add(new String[]{"me", "food?", ""});
  ModelChat.send(a, "food?", "facts", "Food this month: 2,295", ChatPage.LOG, () -> { });
  for (int k = 0; k < 100 && AiEngine.running; k++) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50); }
  Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper();
  assertEquals("Food was 2,295 this month.", ChatPage.LOG.get(ChatPage.LOG.size() - 1)[1]);
 }
 @Test public void phoneLockRecoveryNeedsAPhoneLock() throws Exception {
  MainActivity a = ShotTest.start(false);
  org.robolectric.shadows.ShadowKeyguardManager k = org.robolectric.Shadows.shadowOf((android.app.KeyguardManager) a.getSystemService(android.content.Context.KEYGUARD_SERVICE));
  k.setIsDeviceSecure(false); assertFalse(Lock.phoneLockSet(a)); assertNull(Lock.phoneLockIntent(a));
  k.setIsDeviceSecure(true); assertTrue(Lock.phoneLockSet(a)); assertNotNull(Lock.phoneLockIntent(a));
  Lock.set(a.db, "1234"); assertTrue(Lock.enabled(a.db)); Lock.clear(a.db); assertFalse(Lock.enabled(a.db));
 }
 @Test public void ledgerMonthSwipeDirection() throws Exception {
  MainActivity a = ShotTest.start(false); java.time.YearMonth m0 = a.ledgerMonth;
  android.widget.TextView t = new android.widget.TextView(a); int[] c = {0, 0};
  LedgerPage.swipeMonth(a, t, () -> c[0]++, () -> c[1]++);
  java.util.function.BiConsumer<Float, Float> swipe = (x0, x1) -> {
   long now = android.os.SystemClock.uptimeMillis();
   t.dispatchTouchEvent(android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, x0, 10, 0));
   t.dispatchTouchEvent(android.view.MotionEvent.obtain(now, now + 50, android.view.MotionEvent.ACTION_MOVE, (x0 + x1) / 2, 10, 0));
   t.dispatchTouchEvent(android.view.MotionEvent.obtain(now, now + 100, android.view.MotionEvent.ACTION_MOVE, x1, 10, 0));
   t.dispatchTouchEvent(android.view.MotionEvent.obtain(now, now + 120, android.view.MotionEvent.ACTION_UP, x1, 10, 0));
  };
  swipe.accept(400f, 100f); assertEquals(1, c[0]); assertEquals(0, c[1]);
  swipe.accept(100f, 400f); assertEquals(1, c[1]);
 }
 @Test public void ledgerSearchHasClearButton() throws Exception {
  MainActivity a = ShotTest.start(false); a.ledgerQuery = ""; android.view.View v = LedgerPage.build(a);
  assertNull(v.findViewWithTag("clearSearch"));
  a.ledgerQuery = "tea"; v = LedgerPage.build(a); android.view.View x = v.findViewWithTag("clearSearch"); assertNotNull(x);
  x.performClick(); assertEquals("", a.ledgerQuery);
 }
}
