package in.aicon.hisab;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class ShotTest {
 static final String OUT = System.getProperty("shots", "/tmp/shots");

 static void snap(MainActivity a, String name) throws Exception {
  View v = a.getWindow().getDecorView(); int w = 1080, h = 2340;
  v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)); v.layout(0, 0, w, h);
  Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(new Canvas(b));
  try (FileOutputStream o = new FileOutputStream(OUT + "/" + name + ".png")) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
 }
 static MainActivity start(boolean dark) throws Exception {
  try { java.lang.reflect.Method m = android.animation.ValueAnimator.class.getMethod("setDurationScale", float.class); m.invoke(null, 0f); } catch (Throwable t) { System.out.println("scale " + t); }
  MainActivity a = Robolectric.buildActivity(MainActivity.class, new android.content.Intent().putExtra("noLaunch", true)).setup().get();
  a.db.setPref("appearance", dark ? "Dark" : "Light"); a.applyTheme(); a.topInset = Ui.dp(30); a.bottomInset = Ui.dp(20); a.build(); return a;
 }
 static void scroll(MainActivity a, int y) { View v = a.tabHost.getChildAt(0); if (v instanceof ScrollView) { v.measure(View.MeasureSpec.makeMeasureSpec(1080, 1073741824), View.MeasureSpec.makeMeasureSpec(2340, 1073741824)); v.layout(0, 0, 1080, 2340); ((ScrollView) v).scrollTo(0, y); } }

 @Test public void light() throws Exception {
  MainActivity a = start(false);
  snap(a, "01-home-light"); scroll(a, 1500); snap(a, "02-home-light-b"); scroll(a, 3000); snap(a, "03-home-light-c");
  a.showTab(1, false); snap(a, "04-ledger"); a.ledgerCalendar = true; a.refresh(); snap(a, "05-ledger-calendar");
  a.showTab(3, false); snap(a, "06-insights"); scroll(a, 1700); snap(a, "07-insights-b");
  a.showTab(4, false); snap(a, "08-people");
  a.showTab(0, false); a.openEntry("expense", null); snap(a, "09-entry");
  a.closeAll(); a.layers.clear(); a.build(); Settings.open(a); snap(a, "10-settings");
  a.closeAll(); a.layers.clear(); a.build(); Pages.budgets(a); snap(a, "11-budgets");
  a.closeAll(); a.layers.clear(); a.build(); Pages.goals(a); snap(a, "12-goals");
 }
 @Test public void dark() throws Exception {
  MainActivity a = start(true);
  snap(a, "20-home-dark"); scroll(a, 1500); snap(a, "21-home-dark-b");
  a.showTab(3, false); snap(a, "22-insights-dark"); a.showTab(1, false); snap(a, "23-ledger-dark");
  a.showTab(0, false); a.openEntry("lent", null); snap(a, "24-entry-dark");
 }

 @Test public void keyboardRegression() throws Exception {
  MainActivity a=start(true); Settings.open(a);
  CrashTest helper=new CrashTest();helper.a=a;helper.click("You");
  EditText field=CrashTest.fields(helper.top()).get(0);field.setText("Taylor");field.requestFocus();field.setSelection(6);
  helper.ime(900);snap(a,"40-name-with-ime-inset");helper.ime(0);snap(a,"41-name-after-ime");
  helper.click("Save");snap(a,"42-settings-after-save");
 }
 @Test public void logo() throws Exception {
  MainActivity a = start(false); FrameLayout f = new FrameLayout(a); f.setBackgroundColor(Ui.bg); Charts.Logo l = new Charts.Logo(a); l.t = 1f; Wordmark w = new Wordmark(a, Ui.ink);
  LinearLayout m = Ui.col(a); m.setGravity(Gravity.CENTER_HORIZONTAL); m.addView(l, Ui.lp(Ui.dp(116), Ui.dp(116))); m.addView(w, Ui.lpm(Ui.dp(112), Ui.dp(44), 0, 18, 0, 0)); f.addView(m, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
  f.measure(View.MeasureSpec.makeMeasureSpec(1080, 1073741824), View.MeasureSpec.makeMeasureSpec(2340, 1073741824)); f.layout(0, 0, 1080, 2340); Bitmap b = Bitmap.createBitmap(1080, 2340, Bitmap.Config.ARGB_8888); f.draw(new Canvas(b));
  try (FileOutputStream o = new FileOutputStream(OUT + "/30-launch.png")) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
 }
}
