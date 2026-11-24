package in.aicon.hisab;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.*;

public class BillScanTest {
 static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

 static List<OcrParse.El> bill(String[][] lines) {
  List<OcrParse.El> els = new ArrayList<>(); int top = 40;
  for (String[] segs : lines) { int left = 40; for (String seg : segs) { els.add(new OcrParse.El(seg, left, top, left + seg.length() * 20, top + 40)); left += seg.length() * 20 + 120; } top += 70; }
  return els;
 }

 @Test public void supermarketBill() {
  List<OcrParse.El> els = bill(new String[][]{
   {"FRESH MART SUPERSTORE"},
   {"24/11/2026", "18:42"},
   {"RICE 5KG", "349.00"},
   {"ATTA 10KG", "485.00"},
   {"MILK 1L", "62.00"},
   {"CGST 2.5%", "22.40"},
   {"SGST 2.5%", "22.40"},
   {"GRAND TOTAL", "940.80"},
  });
  BillScan.Bill b = BillScan.parse(els, 1200, TODAY);
  assertEquals(3, b.items.size());
  assertEquals("RICE 5KG", b.items.get(0).title);
  assertEquals(34900, b.items.get(0).amount);
  assertEquals(48500, b.items.get(1).amount);
  assertEquals(6200, b.items.get(2).amount);
  assertEquals(4480, b.gst);
  assertEquals("2026-11-24", b.date.toString());
  assertEquals("FRESH MART SUPERSTORE", b.merchant);
 }

 @Test public void gstSummaryLineWins() {
  List<OcrParse.El> els = bill(new String[][]{
   {"CITY MALL"},
   {"SHIRT", "999.00"},
   {"TOTAL GST", "49.95"},
   {"CGST", "24.98"},
   {"SGST", "24.97"},
   {"TOTAL", "1048.95"},
  });
  BillScan.Bill b = BillScan.parse(els, 1200, TODAY);
  assertEquals(1, b.items.size());
  assertEquals(4995, b.gst);
 }

 @Test public void skipsPaymentAndNoise() {
  List<OcrParse.El> els = bill(new String[][]{
   {"23-11-2026"},
   {"BREAD", "45.00"},
   {"CASH", "50.00"},
   {"CHANGE", "5.00"},
  });
  BillScan.Bill b = BillScan.parse(els, 1200, TODAY);
  assertEquals(1, b.items.size());
  assertEquals("BREAD", b.items.get(0).title);
  assertEquals("2026-11-23", b.date.toString());
 }
}
