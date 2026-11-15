package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.graphics.Insets;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class CrashTest {
 MainActivity a;
 @Before public void start() {
  a=Robolectric.buildActivity(MainActivity.class,new Intent().putExtra("noLaunch",true)).setup().get();
 }
 static View text(View v,String text) {
  if (v instanceof TextView && ((TextView)v).getText().toString().equals(text)) return v;
  if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++){ View r=text(((ViewGroup)v).getChildAt(i),text);if(r!=null)return r; }
  return null;
 }
 static List<EditText> fields(View v){ List<EditText> out=new ArrayList<>(); if(v instanceof EditText)out.add((EditText)v); if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.addAll(fields(((ViewGroup)v).getChildAt(i)));return out; }
 View top(){return a.layers.get(a.layers.size()-1).content;}
 void click(String s){View v=text(top(),s); assertNotNull(s,v);while(!v.isClickable() && v.getParent() instanceof View)v=(View)v.getParent();assertTrue(s,v.performClick());}
 void ime(int bottom){
  WindowInsets in = new WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),Insets.of(0,70,0,50)).setInsets(WindowInsets.Type.ime(),Insets.of(0,0,0,bottom)).build();
  a.root.dispatchApplyWindowInsets(in);
 }
 @Test public void explicitRebuildWithOpenSheets(){
  Settings.open(a);Sheets.form(a,"Your name",new String[]{"Name"},new String[]{"You"},new int[]{Sheets.TEXT},"Save",v->{},null);
  a.build();a.build();assertEquals(2,a.layers.size());
 }
 @Test public void keyboardKeepsFieldTextFocusAndSelection(){
  Settings.open(a);click("You"); EditText field=fields(top()).get(0);field.setText("Taylor");field.requestFocus();field.setSelection(3);
  View sheet=top(),tab=a.tabHost.getChildAt(0);ime(900);ime(1100);ime(0);
  assertSame(sheet,top());assertSame(tab,a.tabHost.getChildAt(0));assertEquals("Taylor",field.getText().toString());assertEquals(3,field.getSelectionStart());assertTrue(field.hasFocus());
  click("Save");assertEquals("Taylor",a.db.pref("name",""));
 }
 @Test public void themeAccentFontCurrencyAndPureBlack(){
  Settings.open(a);click("Appearance");click("Dark");assertEquals("Dark",a.db.pref("appearance",""));
  click("Font");click("Lora");click("Currency");click("USD");
  a.db.setPref("pureBlack","1");a.applyTheme();a.build();ime(900);ime(0);
  assertEquals("Lora",a.db.pref("font",""));assertEquals("USD",a.db.pref("currency",""));
 }
 @Test public void allFormsSurviveImeAndRefresh(){
  for(Runnable page: Arrays.<Runnable>asList(()->Pages.accounts(a),()->Pages.categories(a),()->Pages.budgets(a),()->Pages.goals(a),()->Pages.recurring(a))){
   a.closeAll();page.run();a.refresh();ime(900);ime(0);a.build();a.closeAll();
  }
  for(String kind:Store.KINDS){a.openEntry(kind,null);EditText e=fields(top()).get(0);e.setText("A note");ime(950);ime(0);assertEquals("A note",e.getText().toString());a.closeAll();}
 }
 @Test public void peopleSplitAndContactCancel(){
  a.showTab(4,false);Pages.split(a);fields(top()).get(0).setText("100");ime(900);ime(0);click("Work it out");click("Add my share as spent");
  assertEquals(5000,a.db.recent(1).get(0).amount);
  a.pickContact((n,p)->fail("Cancelled picker called back"));assertEquals(1,a.results.size());int code=a.results.keySet().iterator().next();a.onActivityResult(code,0,null);assertTrue(a.results.isEmpty());
 }
 public static class NoApps extends MainActivity {
  @Override public void startActivity(Intent i) { throw new ActivityNotFoundException("No handler"); }
  @Override public void startActivityForResult(Intent i,int code) { throw new ActivityNotFoundException("No picker"); }
 }
 @Test public void missingExternalAppsDoNotCrash(){
  NoApps x=Robolectric.buildActivity(NoApps.class,new Intent().putExtra("noLaunch",true)).setup().get();
  x.launchExternal(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://example.com")));
  x.request(new Intent(Intent.ACTION_PICK),d->fail());assertTrue(x.results.isEmpty());
  Settings.exportCsv(x);
 }
 @Test public void emptyAndEmojiNames(){
  assertEquals("?",Ui.initial(""));assertEquals("?",Ui.initial(null));assertEquals("🥳",Ui.initial("🥳 hi"));
  a.db.setPref("name","");a.db.addPerson("","");a.refresh();a.showTab(4,false);Settings.open(a);
 }
 @Test public void configurationChangeWithLayers(){Settings.open(a);click("You");EditText field=fields(top()).get(0);field.setText("Keep me");a.onConfigurationChanged(new android.content.res.Configuration(a.getResources().getConfiguration()));a.build();assertEquals("Keep me",field.getText().toString());}
 @Test public void realFormSaves(){
  Pages.accounts(a);click("Cash");fields(top()).get(0).setText("Pocket");click("Save");assertEquals("Pocket",a.db.accounts().get(0).name);
  a.closeAll();Pages.budgets(a);click("Food");fields(top()).get(0).setText("2000");click("Save");
  a.closeAll();Pages.goals(a);click("New laptop");click("Add money");fields(top()).get(0).setText("100");click("Add");
  a.closeAll();Pages.recurring(a);click("Internet");click("Skip this one");a.refresh();
 }
}
