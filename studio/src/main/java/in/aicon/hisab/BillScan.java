package in.aicon.hisab;

import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.widget.*;
import com.google.mlkit.vision.text.Text;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Scan a supermarket or mall bill: photo in, itemised expenses out, GST picked up. All on this phone. */
final class BillScan {
 private BillScan() {}

 private static final Pattern SKIP_WORD = Pattern.compile("\\b(cgst|sgst|igst|utgst|gst|tax|total|change|cash|card|upi|balance|discount|savings|tender|round ?(off)?|bill|invoice|order|token|date|time|cashier|counter|tel|phone|gstin|fssai|cin|thank|visit|welcome|address|mob|page)\\b", Pattern.CASE_INSENSITIVE);
 private static final Pattern TOTAL_WORD = Pattern.compile("\\b(grand total|total|net amount|amount payable|bill amount|to pay|payable|subtotal|sub total)\\b", Pattern.CASE_INSENSITIVE);
 private static final Pattern AMOUNT_CELL = Pattern.compile("^[₹$€£\\s]*(?:rs\\.?)?\\s*-?((?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)\\s*$", Pattern.CASE_INSENSITIVE);

 static final class Bill {
  LocalDate date; final List<OcrParse.Entry> items = new ArrayList<>(); long gst; String merchant = "";
 }

 static void open(MainActivity a) {
  LinearLayout c = Sheets.sheetBody(a, "Scan a bill");
  TextView t = Ui.text(a, "Take a photo of a supermarket or mall bill, or pick one from your gallery. Hisab reads the items on this phone, sorts each into a category and adds them as expenses. GST on the bill is picked up and totalled. Nothing is uploaded anywhere.", 14.5f, Ui.muted, false);
  c.addView(t);
  TextView cam = Ui.button(a, "Take a photo", true); c.addView(cam, Ui.lpm(-1, -2, 0, 16, 0, 0));
  TextView pick = Ui.button(a, "Pick from gallery", false); c.addView(pick, Ui.lpm(-1, -2, 0, 10, 0, 0));
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  Ui.tap(cam, () -> {
   File f = new File(a.getExternalFilesDir("statements"), "scan-" + System.currentTimeMillis() + ".jpg");
   Uri out = androidx.core.content.FileProvider.getUriForFile(a, "in.aicon.hisab.studio2.fileprovider", f);
   Intent cap = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT, out).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
   a.request(cap, d -> { a.close(ref[0]); scan(a, Uri.fromFile(f), f); });
  });
  Ui.tap(pick, () -> {
   Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
   a.request(i, d -> { if (d == null || d.getData() == null) return; a.close(ref[0]); scan(a, d.getData(), null); });
  });
  ref[0] = Sheets.show(a, c);
 }

 static void scan(MainActivity a, Uri u, File temp) {
  a.toast("Reading the bill…", null);
  com.google.mlkit.vision.text.TextRecognizer recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
  new java.util.concurrent.ThreadPoolExecutor(1, 1, 0, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>()).execute(() -> {
   OcrParse.Result done = new OcrParse.Result();
   int failed = 0; boolean modelPending = false;
   try {
    Bitmap bmp = OcrImport.decode(a, u);
    if (bmp == null) failed++;
    else {
     List<OcrParse.El> els = OcrImport.ocr(recognizer, bmp);
     Bill b = parse(els, bmp.getWidth(), LocalDate.now());
     if (b.items.isEmpty()) failed++;
     else {
      OcrParse.Day d = new OcrParse.Day(); d.day = b.date.toString(); d.label = Fmt.dayLabel(d.day) + (b.merchant.isEmpty() ? "" : " · " + b.merchant);
      for (OcrParse.Entry e : b.items) { e.day = d.day; d.entries.add(e); }
      done.days.add(d); done.gst = b.gst;
     }
    }
   } catch (Exception e) { failed++; if (OcrImport.modelBusy(e)) modelPending = true; }
   if (temp != null) temp.delete();
   OcrParse.Result r = done; int f = failed; boolean mp = modelPending;
   a.runOnUiThread(() -> {
    if (mp && r.count() == 0) { a.toast("The on-device reader is still downloading. Try again in a minute", null); return; }
    if (r.count() == 0) { a.toast("Could not find bill items. Try a sharper, flatter photo", null); return; }
    OcrImport.review(a, r, 1, f);
   });
  });
 }

 /** Pure parse of OCR lines from a printed bill. Unit-tested without Android. */
 static Bill parse(List<OcrParse.El> els, int width, LocalDate today) {
  Bill b = new Bill(); b.date = today; boolean foundDate = false;
  List<List<OcrParse.El>> rows = OcrParse.rows(els, width);
  int end = rows.size();
  for (int i = 0; i < rows.size(); i++) {
   if (TOTAL_WORD.matcher(join(rows.get(i))).find() && trailingAmount(rows.get(i)) > 0) { end = i; break; }
  }
  for (int i = 0; i < end; i++) {
   List<OcrParse.El> row = rows.get(i);
   String line = join(row);
   if (!foundDate) { LocalDate d = dateOn(row, today); if (d != null) { b.date = d; foundDate = true; } }
   if (i < 4 && b.merchant.isEmpty() && line.matches(".*\\p{L}{3}.*") && trailingAmount(row) <= 0 && !SKIP_WORD.matcher(line).find()) b.merchant = line.length() > 40 ? line.substring(0, 40) : line;
   if (SKIP_WORD.matcher(line).find()) continue;
   long amt = trailingAmount(row);
   if (amt <= 0) continue;
   String title = titleOf(row);
   if (title.isEmpty() || !title.matches(".*\\p{L}.*")) continue;
   if (title.length() > 60) title = title.substring(0, 60);
   b.items.add(new OcrParse.Entry(title, amt, ""));
   if (b.items.size() >= 80) break;
  }
  long split = 0, summary = 0, breakup = 0;
  for (List<OcrParse.El> row : rows) {
   long amt = trailingAmount(row);
   if (amt <= 0) continue;
   String low = join(row).toLowerCase(Locale.ROOT);
   if (low.matches(".*\\b(cgst|sgst|utgst|igst)\\b.*")) split += amt;
   else if (low.matches(".*\\b(total tax|total gst|gst total|tax total|gst amount|tax amount)\\b.*")) summary = Math.max(summary, amt);
   else if (low.matches(".*gst\\s*[@(].*")) breakup += amt;
  }
  b.gst = summary > 0 ? summary : (split > 0 ? split : breakup);
  return b;
 }

 static String join(List<OcrParse.El> row) {
  StringBuilder s = new StringBuilder();
  for (OcrParse.El e : row) { if (s.length() > 0) s.append(' '); s.append(e.text.trim()); }
  return s.toString();
 }

 static long trailingAmount(List<OcrParse.El> row) {
  if (row.isEmpty()) return 0;
  Matcher m = AMOUNT_CELL.matcher(row.get(row.size() - 1).text.trim());
  if (!m.find()) return 0;
  return OcrParse.amountMinor(m.group(1));
 }

 static String titleOf(List<OcrParse.El> row) {
  StringBuilder s = new StringBuilder();
  for (int i = 0; i + 1 < row.size(); i++) { String t = row.get(i).text.trim(); if (!t.isEmpty()) { if (s.length() > 0) s.append(' '); s.append(t); } }
  return s.toString().trim();
 }

 static LocalDate dateOn(List<OcrParse.El> row, LocalDate today) {
  for (OcrParse.El e : row) {
   LocalDate d = AnyCsv.parseDate(e.text.trim(), today);
   if (d != null) return d;
  }
  return null;
 }
}
