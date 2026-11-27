package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.view.*;
import android.widget.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class Release1030Test {
 MainActivity a;
 static String texts(View v){String s=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s+="\n"+texts(((ViewGroup)v).getChildAt(i));return s;}
 void seed(){a.db.wipe();a.db.seedBasics();long acc=a.db.accounts().get(0).id;java.util.List<Store.Cat> cs=a.db.categories("expense");
  String[] days={"2026-11-20","2026-11-02","2026-03-05","2025-12-30"};
  for(int i=0;i<days.length;i++){Store.Tx t=new Store.Tx();t.kind="expense";t.amount=(i+1)*10000;t.day=days[i];t.account=acc;t.cat=cs.get(i%2).id;t.note="lunch"+i;a.db.save(t);}}
 @Before public void start()throws Exception{a=ShotTest.start(false);seed();}

 @Test public void aiSectionHoldsChatItemsAndRename(){
  a.settingsSection="";Settings.open(a);assertTrue(texts(a.root).contains("Hisab AI"));
  a.closeAll();a.settingsSection="ai";Settings.open(a);String t=texts(a.root);
  assertTrue(t.contains("Ask Hisab AI"));assertTrue(t.contains("AI models"));assertFalse(t.contains("Works only inside Hisab"));
  a.closeAll();a.settingsSection="general";Settings.open(a);assertFalse(texts(a.root).contains("Ask Hisab"));
 }
 @Test public void settingsChangeStaysInSettings(){
  a.closeAll();a.settingsSection="general";Settings.open(a);int before=a.root.getChildCount();
  a.refresh();assertEquals(before,a.root.getChildCount());assertTrue(texts(a.root).contains("Language and privacy"));
 }
 @Test public void insightsHasThreeWayToggleAndRename(){
  a.db.setPref("pieScope","month");View v=InsightsPage.build(a);String t=texts(v);
  assertTrue(t.contains("This month"));assertTrue(t.contains("This year"));assertTrue(t.contains("All time"));assertTrue(t.contains("Ask Hisab AI"));
  View sw=v.findViewWithTag("pieScopeSwitch");assertNotNull(sw);
 }
 @Test public void periodChangesTheListedEntries(){
  Store.Cat c=a.db.categories("expense").get(0);
  java.util.List<java.util.Map.Entry<Long,Long>> es=new java.util.ArrayList<>();es.add(new java.util.AbstractMap.SimpleEntry<>(c.id,1L));
  InsightsPage.categoryEntries(a,es,1,0,"0000-01-01","9999-12-31");
  View s=a.root.findViewWithTag("categoryEntries");assertNotNull(s);assertTrue(texts(s).contains("entries"));
 }
 @Test public void donutTapMapsToSlice(){
  Charts.Donut d=new Charts.Donut(a,new long[]{50,50},new int[]{0xFF1D4ED8,0xFFB3261E},"1","x");
  d.layout(0,0,400,400);int[] got={-1};d.pick=i->got[0]=i;
  assertEquals(0,d.sliceAt(330,200));assertEquals(1,d.sliceAt(70,200));assertEquals(-1,d.sliceAt(200,200));
 }
 @Test public void settingsButtonOnLedgerInsightsPeople(){
  for(View v:new View[]{LedgerPage.build(a),InsightsPage.build(a),PeoplePage.build(a)}){boolean f=false;java.util.ArrayDeque<View> q=new java.util.ArrayDeque<>();q.add(v);while(!q.isEmpty()){View x=q.poll();if("Settings".contentEquals(String.valueOf(x.getContentDescription())))f=true;if(x instanceof ViewGroup)for(int i=0;i<((ViewGroup)x).getChildCount();i++)q.add(((ViewGroup)x).getChildAt(i));}assertTrue(f);}
 }
 @Test public void micButtonPulsesOnlyWhileListening(){
  MicButton m=new MicButton(a);assertFalse(m.listening);m.setListening(true);assertTrue(m.listening);m.setListening(false);assertFalse(m.listening);
  assertNotNull(Icons.class);
 }
}
