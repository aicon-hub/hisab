package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=26)
public class LegacyTest {
 @Test public void android8FormsAndIcons(){MainActivity a=Robolectric.buildActivity(MainActivity.class,new android.content.Intent().putExtra("noLaunch",true)).setup().get();Settings.open(a);a.updateInsets(40,50,0);a.build();a.closeAll();BankSms.open(a);a.closeAll();Backup.password(a,true);a.closeAll();Pages.iconPicker(a,"other",s->{});Privacy.open(a);a.db.setPref("language","മലയാളം");a.applyTheme();a.build();a.launchView();LauncherIcons.apply(a);assertEquals("Lime",LauncherIcons.variant(a.db));}
 @Test public void android8EncryptedDataRoundTrip() throws Exception {byte[] source="local test data".getBytes(java.nio.charset.StandardCharsets.UTF_8);assertArrayEquals(source,Backup.crypt(Backup.crypt(source,"test-only-password".toCharArray(),true),"test-only-password".toCharArray(),false));}
}
