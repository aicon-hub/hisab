package in.aicon.hisab;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

/** Screenshot OCR import: pure parser tests on synthetic layouts matching the old app's home list. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class OcrTest {
 static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
 static final int W = 1080;

 static List<OcrParse.El> els(Object... items) {
  // items: alternating y (int) and rows: each row is String[] of segments laid out left to right
  List<OcrParse.El> out = new ArrayList<>();
  for (int i = 0; i < items.length; i += 2) {
   int y = (Integer) items[i];
   String[] segs = (String[]) items[i + 1];
   int x = 40;
   for (int s = 0; s < segs.length; s++) {
    int w = segs[s].length() * 22;
    int left = s == segs.length - 1 && segs.length > 1 ? W - 40 - w : x;
    out.add(new OcrParse.El(segs[s], left, y, left + w, y + 44));
    x += w + (segs.length > 1 && s == segs.length - 2 ? 300 : 12);
   }
  }
  return out;
 }

 @Test public void moneyTrackerLayout() {
  List<OcrParse.El> in = els(
   10, new String[]{"June 2026"},
   60, new String[]{"Expenses", "Income", "Balance"},
   110, new String[]{"493", "0", "-493"},
   160, new String[]{"29 Jun Monday", "Expenses: 25"},
   220, new String[]{"synthetic vinegar", "-25"},
   280, new String[]{"27 Jun Saturday", "Expenses: 255"},
   340, new String[]{"ksrtc+rapido", "-255"});
  OcrParse.Result r = OcrParse.parse(in, W, TODAY);
  assertEquals(2, r.days.size());
  assertEquals("2026-06-29", r.days.get(0).day);
  assertEquals(2500, r.days.get(0).headerTotal);
  assertEquals(1, r.days.get(0).entries.size());
  assertEquals("synthetic vinegar", r.days.get(0).entries.get(0).title);
  assertEquals(2500, r.days.get(0).entries.get(0).amount);
  assertEquals("2026-06-27", r.days.get(1).day);
  assertEquals(25500, r.days.get(1).sum());
  assertTrue(r.noDate.isEmpty());
 }

 @Test public void thousandsDecimalsAndMismatch() {
  List<OcrParse.El> in = els(
   10, new String[]{"20 Aug Thursday", "Expenses: 1,054.54"},
   70, new String[]{"NPTEL", "-1,003.54"},
   130, new String[]{"Recharge", "-51"});
  OcrParse.Result r = OcrParse.parse(in, W, TODAY);
  assertEquals(1, r.days.size());
  OcrParse.Day d = r.days.get(0);
  assertEquals(105454, d.headerTotal);
  assertEquals(105454, d.sum());
  assertFalse(d.mismatch());
  assertEquals(100354, d.entries.get(0).amount);
  assertEquals(5100, d.entries.get(1).amount);
 }

 @Test public void headerOnOneLineAndYearRoll() {
  List<OcrParse.El> in = els(
   10, new String[]{"26 Sep Saturday Expenses: 2,000"},
   70, new String[]{"gate exam", "-2,000"});
  OcrParse.Result r = OcrParse.parse(in, W, TODAY);
  assertEquals("2026-09-26", r.days.get(0).day);
  assertEquals(200000, r.days.get(0).sum());
  // a December date when "today" is November belongs to last year
  List<OcrParse.El> dec = els(10, new String[]{"15 Dec Monday", "Expenses: 60"}, 70, new String[]{"gift wrap", "-60"});
  OcrParse.Result r2 = OcrParse.parse(dec, W, TODAY);
  assertEquals("2025-12-15", r2.days.get(0).day);
 }

 @Test public void noiseLinesIgnored() {
  List<OcrParse.El> in = els(
   10, new String[]{"Money Tracker"},
   60, new String[]{"Balance", "-493"},
   110, new String[]{"Jul 2026"},
   160, new String[]{"Aug 2026"},
   210, new String[]{"2 Jul Thursday", "Expenses: 400"},
   270, new String[]{"baniyan", "-200"},
   330, new String[]{"discover 125", "-200"});
  OcrParse.Result r = OcrParse.parse(in, W, TODAY);
  assertEquals(1, r.days.size());
  assertEquals(2, r.days.get(0).entries.size());
  assertEquals(40000, r.days.get(0).sum());
  assertFalse(r.days.get(0).mismatch());
 }

 @Test public void entriesBeforeAnyHeaderHaveNoDate() {
  List<OcrParse.El> in = els(10, new String[]{"mystery row", "-99"}, 70, new String[]{"3 Sep Thursday", "Expenses: 10"}, 130, new String[]{"printing", "-10"});
  OcrParse.Result r = OcrParse.parse(in, W, TODAY);
  assertEquals(1, r.noDate.size());
  assertEquals(9900, r.noDate.get(0).amount);
  assertEquals(1, r.count());
 }

 @Test public void mergeCombinesSameDayAcrossScreenshots() {
  List<OcrParse.El> a = els(10, new String[]{"2 Jul Thursday", "Expenses: 400"}, 70, new String[]{"baniyan", "-200"});
  List<OcrParse.El> b = els(10, new String[]{"2 Jul Thursday", "Expenses: 400"}, 70, new String[]{"discover 125", "-200"});
  OcrParse.Result m = OcrParse.merge(Arrays.asList(OcrParse.parse(a, W, TODAY), OcrParse.parse(b, W, TODAY)));
  assertEquals(1, m.days.size());
  assertEquals(2, m.days.get(0).entries.size());
 }

 @Test public void guessedCategoriesExist() {
  Store db = new Store(RuntimeEnvironment.getApplication());
  db.wipe(); db.seedBasics();
  assertEquals("Fuel", OcrImport.guessCategory("discover 125 petrol", db));
  assertEquals("Food", OcrImport.guessCategory("biryani", db));
  assertEquals("Other", OcrImport.guessCategory("zzz unknown thing", db));
 }
}
