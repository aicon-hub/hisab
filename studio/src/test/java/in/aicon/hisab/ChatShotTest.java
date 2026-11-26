package in.aicon.hisab;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-notnight-xxhdpi")
public class ChatShotTest {
 @Test public void shot() throws Exception {
  MainActivity a = ShotTest.start(false); new java.io.File("/tmp/shots").mkdirs();
  ChatPage.LOG.clear(); ChatPage.open(a);
  ChatPage.LOG.add(new String[]{"me", "How much did I spend this month?"});
  ChatPage.LOG.add(new String[]{"hisab", ChatBrain.ask(a.db, "How much did I spend this month?", java.time.LocalDate.now()).text});
  ChatPage.pending = ChatBrain.ask(a.db, "tea 20", java.time.LocalDate.now());
  a.closeAll(); a.layers.clear(); a.build(); ChatPage.open(a); ShotTest.snap(a, "chat");
 }
}
