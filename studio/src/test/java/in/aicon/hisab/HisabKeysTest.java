package in.aicon.hisab;

import static org.junit.Assert.*;
import android.text.InputType;
import android.widget.EditText;
import java.util.List;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 35)
public class HisabKeysTest {
 MainActivity a;
 @Before public void start() { a = Robolectric.buildActivity(MainActivity.class, new android.content.Intent().putExtra("noLaunch", true)).setup().get(); a.db.wipe(); a.db.seedBasics(); }
 EditText field(int type) { EditText e = new EditText(a); e.setInputType(type); a.root.addView(e); return e; }
 void type(String s) { for (char c : s.toCharArray()) a.kb.typeKey(String.valueOf(c)); }

 @Test public void romanManglishWordsBecomeMalayalam() {
  String[][] t = {{"paisa", "പൈസ"}, {"avan", "അവൻ"}, {"hisaab", "ഹിസാബ്"}, {"chaaya", "ചായ"}, {"njaan", "ഞാൻ"}, {"maasam", "മാസം"}, {"roopa", "രൂപ"}, {"angane", "അങ്ങനെ"}, {"undu", "ഉണ്ടു"}, {"enthu", "എന്തു"}, {"yaathra", "യാത്ര"}, {"kuttam", "കുട്ടം"}};
  for (String[] x : t) assertEquals(x[0], x[1], MlRoman.translit(x[0]));
  assertEquals("പൈസ 12", MlRoman.translit("paisa 12"));
 }

 @Test public void englishTypingBackspaceAndShiftAfterSentence() {
  EditText e = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(e); assertTrue(a.kb.isShown());
  type("hi"); assertEquals("hi", e.getText().toString());
  a.kb.back(); assertEquals("h", e.getText().toString());
  type(" ok"); assertEquals("h ok", e.getText().toString());
 }

 @Test public void panelTakesSpaceAndHidesCleanly() {
  EditText e = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(e);
  assertTrue(a.kb.isShown()); assertNotNull(a.kb.target);
  a.kb.hide(); assertFalse(a.kb.isShown()); assertEquals(0, a.ownKb);
 }

 @Test public void manglishComposesLiveAndBackspaceEditsTheRomanLetters() {
  a.db.setPref("kbMode", "2"); EditText e = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(e);
  type("paisa"); assertEquals("പൈസ", e.getText().toString());
  a.kb.back(); assertEquals("പൈസ്", e.getText().toString());
  a.kb.typeKey("a"); a.kb.typeKey(" "); a.kb.typeKey("4"); a.kb.typeKey("0");
  assertEquals("പൈസ 40", e.getText().toString());
  type(" chaaya"); assertEquals("പൈസ 40 ചായ", e.getText().toString());
 }

 @Test public void malayalamLayoutTypesMalayalamLetters() {
  a.db.setPref("kbMode", "1"); EditText e = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(e);
  a.kb.typeKey("ക"); a.kb.typeKey("ാ"); a.kb.typeKey("ര"); a.kb.typeKey("്"); assertEquals("കാര്", e.getText().toString());
 }

 @Test public void suggestionsComeFromHisabOwnData() {
  String cat = a.db.categories("expense").get(0).name; String pre = cat.substring(0, 2);
  a.db.addPerson("Ravindran", ""); a.db.setPref("kbMode", "0");
  EditText e = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(e); type(pre);
  assertTrue(a.kb.suggestions().toString(), a.kb.suggestions().contains(cat));
  a.kb.pick(cat); assertEquals(cat + " ", e.getText().toString());
  EditText f = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(f); type("Ravi");
  assertTrue(a.kb.suggestions().contains("Ravindran"));
  a.db.setPref("kbMode", "2"); EditText g = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(g); type("shamb");
  assertTrue(a.kb.suggestions().toString(), a.kb.suggestions().contains("ശമ്പളം"));
  a.kb.pick("ശമ്പളം"); assertEquals("ശമ്പളം ", g.getText().toString());
 }

 @Test public void numberFieldsGetADigitPadThatRejectsLetters() {
  EditText e = field(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); a.kb.attach(e);
  a.kb.typeKey("1"); a.kb.typeKey("."); a.kb.typeKey("5"); a.kb.typeKey("x"); assertEquals("1.5", e.getText().toString());
  EditText w = field(InputType.TYPE_CLASS_NUMBER); a.kb.attach(w); a.kb.typeKey("1"); a.kb.typeKey("."); a.kb.typeKey("-"); assertEquals("1", w.getText().toString());
 }

 @Test public void passwordFieldsUseEnglishOnlyWithNoSuggestions() {
  a.db.setPref("kbMode", "2"); EditText e = field(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); a.kb.attach(e);
  type("pass"); assertEquals("pass", e.getText().toString()); assertTrue(a.kb.suggestions().isEmpty()); a.kb.nextMode(); assertEquals(0, a.kb.mode);
 }

 @Test public void enterRunsTheFieldAction() {
  EditText e = field(InputType.TYPE_CLASS_TEXT); e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
  boolean[] hit = {false}; e.setOnEditorActionListener((v, id, ev) -> { hit[0] = id == android.view.inputmethod.EditorInfo.IME_ACTION_DONE; return true; });
  a.kb.attach(e); a.kb.enter(); assertTrue(hit[0]);
 }

 @Test public void offSwitchLeavesThePhoneKeyboardAlone() {
  a.db.setPref("hisabKeys", "0"); EditText e = field(InputType.TYPE_CLASS_TEXT); a.kb.attach(e); assertFalse(a.kb.isShown());
 }
}
