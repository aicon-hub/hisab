package in.aicon.hisab;

import static org.junit.Assert.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** Stage 1 screen items U3, U4, U7, U8, U9. */
@RunWith(RobolectricTestRunner.class) @Config(sdk = 35)
public class Stage1UiTest {
 MainActivity a;
 @Before public void start() { a = Robolectric.buildActivity(MainActivity.class, new android.content.Intent().putExtra("noLaunch", true)).setup().get(); }

 static TextView find(View v, String text) {
  if (v instanceof TextView && ((TextView) v).getText().toString().equals(text)) return (TextView) v;
  if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { TextView t = find(((ViewGroup) v).getChildAt(i), text); if (t != null) return t; }
  return null;
 }
 static String texts(View v) { String s = v instanceof TextView ? ((TextView) v).getText().toString() : ""; if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) s += "\n" + texts(((ViewGroup) v).getChildAt(i)); return s; }

 @Test public void importantErrorStaysUntilDismissed() {
  a.alert("Backup failed. Check your connection and sign-in");
  ShadowLooper.idleMainLooper(30, java.util.concurrent.TimeUnit.SECONDS);
  assertNotNull("still on screen after 30 s", a.toastView);
  TextView d = find(a.toastView, "Dismiss"); assertNotNull(d); d.performClick();
  assertNull(a.toastView);
 }
 @Test public void ordinaryToastStillGoesAway() {
  a.toast("Saved", null); assertNotNull(a.toastView);
  ShadowLooper.idleMainLooper(6, java.util.concurrent.TimeUnit.SECONDS); assertNull(a.toastView);
 }
 @Test public void backupFailureTextsUseTheStickyAlert() throws Exception {
  for (String f : new String[]{"Backup.java", "DriveBackup.java"}) {
   String s = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/in/aicon/hisab/" + f)), "UTF-8");
   assertFalse(f, s.matches("(?s).*toast\\([^;]*(Backup failed|Invalid backup|Could not reach Google Drive)[^;]*\\).*"));
  }
 }

 @Test public void scrollScreensClearTheDock() {
  a.dock.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
  int need = a.dock.getMeasuredHeight() + Ui.dp(14) + a.bottomInset;
  android.widget.ScrollView[] sv = new android.widget.ScrollView[1];
  android.widget.LinearLayout c = Kit.column(a, sv, true);
  assertTrue(c.getPaddingBottom() + " vs " + need, c.getPaddingBottom() >= need);
  assertTrue(a.dockClearance() >= a.dock.getMeasuredHeight() + Ui.dp(14));
 }

 @Test public void emptyScreensOfferOneNextStep() {
  a.db.wipe(); a.db.seedBasics();
  for (int tab : new int[]{0, 1, 3}) { a.showTab(tab, false); assertNotNull("tab " + tab, find(a.root, "Add an entry")); }
  a.showTab(0, false); TextView b = find(a.root, "Add an entry"); b.performClick();
  assertTrue(texts(a.root).length() > 0);
  Pages.goals(a); assertNotNull(find(a.root, "Add a goal"));
  Pages.recurring(a); assertNotNull(find(a.root, "Add a repeat"));
 }

 @Test public void missingVoiceServiceExplainsWhatToDo() {
  Voice.listen(a, s -> { });
  TextView t = null; if (a.toastView != null) t = find(a.toastView, "No voice typing on this phone. Turn on Google voice typing in Android settings, or type instead.");
  assertNotNull(t);
 }

 @Test public void shortHintAndMalayalamForNewText() {
  String[] keys = {"Dismiss", "Add an entry", "Add a goal", "Add a repeat", "Type it: tea 40", "No voice typing on this phone. Turn on Google voice typing in Android settings, or type instead."};
  for (String k : keys) { String ml = L.ML.get(k); assertNotNull(k, ml); assertTrue(k, ml.matches(".*[\\u0D00-\\u0D7F].*")); assertFalse(k.contains("\u2014") || ml.contains("\u2014")); }
  assertTrue("Type it: tea 40".length() <= 20);
 }
 @Test public void lastProblemScreenSharesPlainText() throws Exception {
  CrashLog.write(a, Thread.currentThread(), new IllegalStateException("invented failure"));
  CrashLog.show(a);
  assertNotNull(find(a.root, "Last problem"));
  TextView share = find(a.root, "Share"); assertNotNull(share); share.performClick();
  android.content.Intent chooser = org.robolectric.Shadows.shadowOf(a).getNextStartedActivity();
  android.content.Intent send = (android.content.Intent) chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT);
  assertEquals("text/plain", send.getType()); assertTrue(send.getStringExtra(android.content.Intent.EXTRA_TEXT).contains("invented failure"));
  assertFalse(CrashLog.file(a).length() == 0);
 }
}
