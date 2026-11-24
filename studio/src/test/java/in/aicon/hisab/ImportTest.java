package in.aicon.hisab;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

/** Generic CSV auto-detection and PDF table fallback. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ImportTest {
 static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

 @Test public void hisabShapeDetected() {
  assertTrue(AnyCsv.isHisabShape(CsvIo.parse("Date,Type,Amount,Account,To account,Category,Person,Note\n2026-11-01,expense,50,Cash,,Food,,x\n")));
  assertFalse(AnyCsv.isHisabShape(CsvIo.parse("Date,Description,Amount\n01/11/2026,Tea,25\n")));
 }

 @Test public void genericColumns() {
  OcrParse.Result r = AnyCsv.detect(CsvIo.parse(
   "Date,Description,Category,Amount\n"
   + "01/11/2026,Tea at canteen,Food,25\n"
   + "02/11/2026,Petrol for bike,Fuel,300.50\n"
   + "02/11/2026,Salary,Income,5000\n"), TODAY);
  assertEquals(2, r.count());
  assertEquals("2026-11-01", r.days.get(0).day);
  assertEquals(2500, r.days.get(0).entries.get(0).amount);
  assertEquals("Tea at canteen · Food", r.days.get(0).entries.get(0).title);
  assertEquals(30050, r.days.get(1).entries.get(0).amount);
 }

 @Test public void debitCreditColumns() {
  OcrParse.Result r = AnyCsv.detect(CsvIo.parse(
   "Txn Date,Narration,Debit,Credit\n"
   + "05-09-2026,UPI-ZEPTO,88.00,\n"
   + "06-09-2026,SALARY,,10000.00\n"), TODAY);
  assertEquals(1, r.count());
  assertEquals("2026-09-05", r.days.get(0).day);
  assertEquals(8800, r.days.get(0).entries.get(0).amount);
 }

 @Test public void dateFormats() {
  assertEquals(LocalDate.of(2026, 8, 20), AnyCsv.parseDate("20/08/2026", TODAY));
  assertEquals(LocalDate.of(2026, 8, 20), AnyCsv.parseDate("20-08-26", TODAY));
  assertEquals(LocalDate.of(2026, 8, 20), AnyCsv.parseDate("2026-08-20", TODAY));
  assertEquals(LocalDate.of(2026, 8, 20), AnyCsv.parseDate("20 Aug 2026", TODAY));
  assertEquals(LocalDate.of(2026, 8, 20), AnyCsv.parseDate("Aug 20, 2026", TODAY));
  assertNull(AnyCsv.parseDate("not a date", TODAY));
 }

 @Test public void amountFormats() {
  assertEquals(100354, AnyCsv.parseAmount("1,003.54"));
  assertEquals(-100354, AnyCsv.parseAmount("-1,003.54"));
  assertEquals(-2500, AnyCsv.parseAmount("₹25.00 Dr"));
  assertEquals(-2500, AnyCsv.parseAmount("(25)"));
  assertEquals(0, AnyCsv.parseAmount(""));
  assertEquals(0, AnyCsv.parseAmount("abc"));
 }

 @Test public void pdfTableRows() {
  List<OcrParse.El> els = Arrays.asList(
   new OcrParse.El("Date", 40, 10, 120, 54), new OcrParse.El("Description", 300, 10, 500, 54), new OcrParse.El("Amount", 900, 10, 1010, 54),
   new OcrParse.El("10/11/2026", 40, 80, 220, 124), new OcrParse.El("RATION STORE", 300, 80, 560, 124), new OcrParse.El("13.00", 900, 80, 1010, 124),
   new OcrParse.El("09 Nov 2026", 40, 150, 240, 194), new OcrParse.El("inner wear", 300, 150, 520, 194), new OcrParse.El("-350", 920, 150, 1010, 194),
   new OcrParse.El("Page 1 of 1", 400, 900, 640, 944));
  OcrParse.Result r = PdfTable.parse(els, 1080, TODAY);
  assertEquals(2, r.count());
  assertEquals("2026-11-10", r.days.get(0).day);
  assertEquals("RATION STORE", r.days.get(0).entries.get(0).title);
  assertEquals(1300, r.days.get(0).entries.get(0).amount);
  assertEquals("2026-11-09", r.days.get(1).day);
  assertEquals(35000, r.days.get(1).entries.get(0).amount);
 }
}
