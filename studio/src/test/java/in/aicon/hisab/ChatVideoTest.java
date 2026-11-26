package in.aicon.hisab;

import android.graphics.*;
import android.view.View;
import java.io.FileOutputStream;
import java.time.LocalDate;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

/** Renders the real Ask Hisab screen frame by frame (virtual clock) for a preview video. Only runs when HISAB_VIDEO is set. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class ChatVideoTest {
 int n = 0; MainActivity a;
 void frame(int times) throws Exception {
  View v = a.getWindow().getDecorView(); int w = 1080, h = 2340;
  v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)); v.layout(0, 0, w, h);
  Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(new Canvas(b));
  for (int i = 0; i < times; i++) try (FileOutputStream o = new FileOutputStream("/tmp/vf/f" + String.format("%04d", n++) + ".png")) { b.compress(Bitmap.CompressFormat.PNG, 90, o); }
 }
 void scene(String q, boolean confirm) throws Exception {
  ChatPage.draft = q; ChatPage.build(a, ChatPage.lastC); frame(8);
  ChatPage.LOG.add(new String[]{"me", q, ""});
  ChatBrain.Reply r = ChatBrain.ask(a.db, q, LocalDate.now()); ChatPage.run = r; ChatPage.pending = r.act.isEmpty() ? null : r; ChatPage.draft = "";
  FlowView probe = new FlowView(a); long total = probe.schedule(ChatPage.STEP_MS);
  ChatPage.running = true;
  for (long ms = 0; ms <= total + 150; ms += 66) { ChatPage.clock = ms; ChatPage.build(a, ChatPage.lastC); frame(1); }
  ChatPage.running = false; if (r.act.isEmpty()) ChatPage.LOG.add(new String[]{"hisab", r.text, ChatPage.sources(a, r)});
  ChatPage.build(a, ChatPage.lastC); frame(18);
  if (confirm) { ChatPage.confirm(a, ChatPage.lastC); frame(18); }
 }
 @Test public void video() throws Exception {
  Assume.assumeNotNull(System.getenv("HISAB_VIDEO"));
  new java.io.File("/tmp/vf").mkdirs();
  a = ShotTest.start(true); ChatPage.LOG.clear(); ChatPage.open(a); a.closeAll(); a.layers.clear(); a.build(); ChatPage.open(a);
  scene("How much did I spend this month?", false);
  scene("ഇന്ന് ഭക്ഷണത്തിന് എത്ര", false);
  scene("tea 20", true);
 }
}
