package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class FixedFactsTest {
 MainActivity a;
 @Before public void start()throws Exception{a=ShotTest.start(false);}
 @Test public void creatorQuestionsGetTheBuiltInAnswer(){
  for(String q:new String[]{"Who is hisab app's creator","who made hisab?","Who developed this app","hisab developer name"}){String r=FixedFacts.answer(a,q);assertNotNull(q,r);assertTrue(r.contains("Muhammed Salman CC"));assertTrue(r.contains("AiCoN"));assertTrue(r.contains("GPL-3.0"));assertTrue(r.contains("Creator:"));assertTrue(r.contains("github.com/salman-cc"));assertTrue(r.contains("salman-cc.github.io"));assertFalse(r.contains("whatsapp"));assertTrue(r.contains("github.com/aicon-hub/hisab"));}
 }
 @Test public void capabilitiesFollowTheToggles(){
  a.db.setPref("chatRead","0");a.db.setPref("chatWrite","0");String off=FixedFacts.answer(a,"What are your capabilities");
  assertTrue(off.contains("It is OFF"));assertTrue(off.contains("Write. It is OFF"));
  a.db.setPref("chatRead","1");a.db.setPref("chatWrite","1");String on=FixedFacts.answer(a,"what can you do");
  assertFalse(on.contains("It is OFF"));assertTrue(on.contains("Confirm card"));
 }
 @Test public void normalMoneyQuestionsAreNotIntercepted(){
  for(String q:new String[]{"how much did I spend on food","add tea 40","what is the capital of France","balance in cash"})assertNull(q,FixedFacts.answer(a,q));
 }
 @Test public void chatUsesItWithoutTheModel(){
  ChatPage.LOG.clear();ChatPage.run(a,"Who is hisab app's creator");
  String[] last=ChatPage.LOG.get(ChatPage.LOG.size()-1);assertTrue(last[1].contains("Muhammed Salman CC"));assertEquals(FixedFacts.NOTE,last[2]);
 }
}
