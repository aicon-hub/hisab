package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import static org.junit.Assert.*;

/** Drives the real screens the way a thumb does: tap rows, flip switches, press Back. Real (native) rendering. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class RealPathTest {
 MainActivity a;
 @Before public void start() throws Exception { a = ShotTest.start(true); }
 static void settle() throws Exception { Thread.sleep(250); org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(250); org.robolectric.shadows.ShadowLooper.idleMainLooper(); }
 static View byDesc(View v, String d) { if (d.contentEquals(String.valueOf(v.getContentDescription()))) return v; if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View r = byDesc(((ViewGroup) v).getChildAt(i), d); if (r != null) return r; } return null; }
 static View byText(View v, String t) { if (v instanceof TextView && ((TextView) v).getText().toString().equals(t)) return v; if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View r = byText(((ViewGroup) v).getChildAt(i), t); if (r != null) return r; } return null; }
 static Switch firstSwitch(View v) { if (v instanceof Switch) return (Switch) v; if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { Switch r = firstSwitch(((ViewGroup) v).getChildAt(i)); if (r != null) return r; } return null; }
 static void tapText(MainActivity a, String t) { View v = byText(a.root, t); assertNotNull("no row " + t, v); while (v != null && !v.isClickable()) v = (View) v.getParent(); assertNotNull(v); v.performClick(); }
 void homeShown() { assertNull("Home should be showing", a.layers.isEmpty() ? null : a.layers.get(0)); }

 @Test public void settingsSectionKeepsYouInPlaceAndBackGoesToTheHub() throws Exception {
  View av = byDesc(a.tabHost, "Settings"); assertNotNull(av); av.performClick(); settle();
  assertEquals(1, a.layers.size()); assertNotNull(byText(a.root, "Language and privacy"));
  tapText(a, "Look"); settle(); assertEquals("hub stays under the section", 2, a.layers.size());
  Switch sw = firstSwitch(a.layers.get(1).content); assertNotNull(sw); boolean was = sw.isChecked(); sw.setChecked(!was); settle();
  assertEquals("still in Look after a change", 2, a.layers.size()); assertNotNull(byText(a.layers.get(1).content, "Appearance"));
  a.onBackPressed(); settle(); assertEquals("Back from a section lands on the Settings list, not Home", 1, a.layers.size()); assertNotNull(byText(a.root, "Language and privacy"));
  a.onBackPressed(); settle(); assertEquals(0, a.layers.size());
 }
 @Test public void hisabAiSwitchesStayInTheSection() throws Exception {
  byDesc(a.tabHost, "Settings").performClick(); settle(); tapText(a, "Hisab AI"); settle(); assertEquals(2, a.layers.size());
  Switch sw = (Switch) a.layers.get(1).content.findViewWithTag("chatRead"); assertNotNull(sw); sw.setChecked(true); settle();
  assertEquals(2, a.layers.size()); assertTrue(a.db.flag("chatRead", false)); assertNotNull(byText(a.layers.get(1).content, "Ask Hisab AI"));
  a.onBackPressed(); settle(); assertEquals(1, a.layers.size());
 }
 @Test public void yourDataEncryptSwitchStaysPut() throws Exception {
  a.db.setPref("driveBackup", "1"); byDesc(a.tabHost, "Settings").performClick(); settle(); tapText(a, "Your data"); settle(); assertEquals(2, a.layers.size());
  DriveBackup.imagesSwitch(a); settle(); assertEquals(2, a.layers.size()); assertNotNull(byText(a.layers.get(1).content, "Include photos and PDFs"));
 }
 @Test public void micButtonRendersPremiumAndListening() throws Exception {
  new File(ShotTest.OUT).mkdirs();
  View home = a.tabHost.getChildAt(0); MicButton m = (MicButton) find(home, MicButton.class); assertNotNull(m);
  ShotTest.snap(a, "mic-home-idle");
  m.setListening(true); assertTrue(m.listening); m.frame(0.55f); ShotTest.snap(a, "mic-home-listening"); m.setListening(false);
 }
 @Test public void chatMicIsTheNewOneAndRingsAreNotClipped() throws Exception {
  new File(ShotTest.OUT).mkdirs(); ChatPage.open(a); settle();
  View top = a.layers.get(a.layers.size() - 1).content; MicButton m = (MicButton) find(top, MicButton.class); assertNotNull("chat uses MicButton", m);
  ShotTest.snap(a, "mic-chat-idle"); m.setListening(true); m.frame(0.55f); ShotTest.snap(a, "mic-chat-listening");
  int n = 0; for (android.view.ViewParent p = m.getParent(); p instanceof ViewGroup && n < 4; p = p.getParent(), n++) assertFalse("parent clips the pulse: " + p, ((ViewGroup) p).getClipChildren());
  m.setListening(false);
 }
 @Test public void pinAndAutoRemindStayOnTheirPage() throws Exception {
  byDesc(a.tabHost, "Settings").performClick(); settle(); Privacy.open(a); settle(); int n = a.layers.size(); assertTrue(n >= 2);
  Privacy.pinForm(a, "Set a PIN", false, ok -> { if (ok) { a.toast("App lock is on", null); a.refresh(); } }); settle();
  a.close(a.layers.get(a.layers.size() - 1)); a.refresh(); settle(); assertEquals(n, a.layers.size());
  a.closeAll(); byDesc(a.tabHost, "Settings").performClick(); settle(); int m = a.layers.size(); AutoRemind.open(a); settle(); assertEquals(m + 1, a.layers.size());
  a.refresh(); settle(); assertEquals(m + 1, a.layers.size());
 }
 static View find(View v, Class<?> c) { if (c.isInstance(v)) return v; if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View r = find(((ViewGroup) v).getChildAt(i), c); if (r != null) return r; } return null; }
}
