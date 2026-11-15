package in.aicon.pocket;
import android.app.*;import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;import java.time.Duration;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35,qualifiers="en-rIN-w432dp-h960dp-xxhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PolishTest {
 QuickActivity a;
 @Before public void setup(){try{androidx.work.WorkManager.getInstance(RuntimeEnvironment.getApplication());}catch(IllegalStateException e){androidx.work.WorkManager.initialize(RuntimeEnvironment.getApplication(),new androidx.work.Configuration.Builder().setMinimumLoggingLevel(android.util.Log.ERROR).build());}RuntimeEnvironment.getApplication().getSharedPreferences("hisab-account-directory",0).edit().clear().commit();RuntimeEnvironment.getApplication().deleteDatabase("pocket.db");a=Robolectric.buildActivity(QuickActivity.class).setup().get();a.dismissLaunch();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));}
 void render(String name,View view)throws Exception{int w=1080,h=2400;view.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));view.layout(0,0,w,h);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600));view.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));view.layout(0,0,w,h);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));new File("/downloads/hisab-polish").mkdirs();try(FileOutputStream file=new FileOutputStream("/downloads/hisab-polish/"+name+".png")){bitmap.compress(Bitmap.CompressFormat.PNG,100,file);}bitmap.recycle();}
 void sample(String kind,long amount,String category,int days){ContentValues v=new ContentValues();v.put("uuid",java.util.UUID.randomUUID().toString());v.put("kind",kind);v.put("amount",amount);v.put("day",java.time.LocalDate.now().minusDays(days).toString());v.put("account","Cash");v.put("dest","");v.put("category",category);v.put("note","");v.put("person","");v.put("loan","");a.db.insertOrThrow("tx",null,v);}
 @Test public void polishedScreensAndPreservedTotals()throws Exception{
  sample("income",4200000,"Salary",5);sample("expense",18500,"Food",0);sample("expense",9500,"Transport",1);sample("expense",74000,"Groceries",2);
  long total=a.balance("Cash");a.settings.put("appearance","Dark");a.settings.put("surface","Liquid Glass");a.render();render("home-dark",a.root);assertEquals(total,a.balance("Cash"));
  a.settings.put("appearance","Light");a.render();render("home-light",a.root);
  a.page="reports";a.render();render("reports-light",a.root);
  a.page="settings";a.render();render("settings-light",a.root);
  a.settings.put("appearance","Dark");a.page="home";a.render();a.openEntry("expense");render("entry-dark",a.activeForm.getWindow().getDecorView());a.activeForm.dismiss();
  android.content.res.Configuration config=new android.content.res.Configuration(a.getResources().getConfiguration());config.fontScale=1.4f;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());a.ml=true;a.render();render("home-large-malayalam",a.root);assertEquals(total,a.balance("Cash"));
 }
 @Test public void launchIsShortAndNeverChangesMoney()throws Exception{
  long before=a.balance("Cash");a.showLaunch();HisabLaunch overlay=a.launchOverlay;assertNotNull(overlay.getParent());render("launch-logo",overlay);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertNull(overlay.getParent());assertEquals(before,a.balance("Cash"));assertTrue(HisabLaunch.HOLD_MS+HisabLaunch.EXIT_MS<=1000);
 }
 @Test public void restoredActivityDoesNotReplayLaunch(){android.os.Bundle state=new android.os.Bundle();a.onSaveInstanceState(state);QuickActivity restored=Robolectric.buildActivity(QuickActivity.class).create(state).start().resume().get();assertNull(restored.launchOverlay);}
 @Test public void reducedMotionSkipsLaunchAndPageStagger(){android.provider.Settings.Global.putFloat(a.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,0f);a.showLaunch();assertNull(a.launchOverlay.getParent());a.page="people";a.render();for(int i=0;i<a.body.getChildCount();i++)assertEquals(1f,a.body.getChildAt(i).getAlpha(),.001f);}
}
