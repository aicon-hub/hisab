package in.aicon.pocket;
import org.junit.*;import static org.junit.Assert.*;
public class VisualPreviewTest {
 @Test public void onlySeparateVisualVariantDisablesGoogle(){assertEquals(in.aicon.pocket.test.BuildConfig.APPLICATION_ID.equals("in.aicon.hisab.visualpreview"),in.aicon.pocket.test.BuildConfig.VISUAL_PREVIEW);assertEquals(!in.aicon.pocket.test.BuildConfig.VISUAL_PREVIEW,CloudConfig.ACCOUNT_UI_ENABLED);}
}
