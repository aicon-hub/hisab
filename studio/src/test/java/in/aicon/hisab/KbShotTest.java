package in.aicon.hisab;

import android.text.InputType;
import android.widget.EditText;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class KbShotTest {
 @Test public void shots() throws Exception {
  MainActivity a = ShotTest.start(false); new java.io.File("/tmp/shots").mkdirs();
  EditText q = a.root.findViewWithTag("quickEntry"); Assert.assertNotNull(q); q.requestFocus();
  String[] names = {"en", "ml", "mg"};
  for (int m = 0; m < 3; m++) {
   a.db.setPref("kbMode", "" + m); a.kb.attach(q); q.setText(""); a.kb.commit();
   for (char c : (m == 1 ? "" : m == 0 ? "Fo" : "paisa").toCharArray()) a.kb.typeKey("" + c);
   a.root.post(() -> {}); org.robolectric.shadows.ShadowLooper.idleMainLooper();
   ShotTest.snap(a, "kb-" + names[m]); panel(a, "kp-" + names[m]);
  }
  a.db.setPref("kbMode", "1"); a.kb.attach(q); a.kb.typeKey("ക"); a.kb.nextMode(); a.kb.nextMode(); a.kb.nextMode(); a.kb.typeKey("N"); a.kb.typeKey("N"); a.kb.nextMode(); a.kb.nextMode(); a.kb.nextMode();
  a.kb.attach(q); a.kb.typeKey("ക"); org.robolectric.shadows.ShadowLooper.idleMainLooper(); ShotTest.snap(a, "kb-ml2");
 }
 static void panel(MainActivity a, String name) throws Exception {
  android.view.View v = a.kb.view(); v.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, 1073741824), android.view.View.MeasureSpec.makeMeasureSpec(0, 0)); v.layout(0, 0, 1080, v.getMeasuredHeight());
  android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(1080, v.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888); v.draw(new android.graphics.Canvas(b));
  try (java.io.FileOutputStream o = new java.io.FileOutputStream("/tmp/shots/" + name + ".png")) { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, o); }
 }
}
