package in.aicon.hisab;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w360dp-h780dp-night-xxhdpi")
public class ModelsShotTest {
 @Test public void shot() throws Exception {
  MainActivity a = ShotTest.start(true); new java.io.File("/tmp/shots").mkdirs();
  AiModels.Phone p = new AiModels.Phone(); p.ramGb = 6; p.totalRam = (long) (5.6 * 1073741824L); p.cores = 8; p.arm64 = true; p.abi = "arm64-v8a"; p.soc = "Example SoC"; p.gpu = "Example GPU"; p.npu = "Android NN API present; whether it uses an NPU is not readable"; p.freeBytes = 20_000_000_000L;
  ModelsPage.phone = p;
  AiModels.M m = AiModels.ALL[2]; AiModels.active = m; AiModels.done = m.size * 62 / 100; AiModels.t0 = System.currentTimeMillis() - 4000; AiModels.d0 = 0; AiModels.phase = "Downloading";
  ModelsPage.open(a); android.widget.ScrollView sv = (android.widget.ScrollView) ModelsPage.col.getParent(); sv.scrollTo(0, Ui.dp(Float.parseFloat(System.getenv().getOrDefault("SCR", "700")))); ShotTest.snap(a, "models");
  AiModels.active = null; ModelsPage.phone = null;
 }
}
