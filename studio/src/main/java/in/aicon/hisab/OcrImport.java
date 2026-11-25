package in.aicon.hisab;

import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Import entries from screenshots of another expense app. Text is recognised on the phone; nothing is uploaded. */
final class OcrImport {
 private OcrImport() {}

 static final class Row {
  OcrParse.Entry entry; String cat; boolean include = true;
  Row(OcrParse.Entry e, String cat) { entry = e; this.cat = cat; }
 }

 static void open(MainActivity a) {
  LinearLayout c = Sheets.sheetBody(a, "Import from screenshots or PDF");
  TextView t = Ui.text(a, "Switching from another expense app? Pick its screenshots or a PDF export and Hisab reads the entries on this phone. Nothing is uploaded anywhere. You review every entry before anything is saved.", 14.5f, Ui.muted, false);
  t.setPadding(0, 0, 0, Ui.dp(16)); c.addView(t);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  TextView go = Ui.button(a, "Choose files", true); c.addView(go, Ui.lpm(-1, -2, 0, 4, 0, 0));
  Ui.tap(go, () -> { a.close(ref[0]); pick(a); });
  ref[0] = Sheets.show(a, c);
 }

 static void pick(MainActivity a) {
  Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true).putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
  a.request(i, d -> {
   List<Uri> uris = new ArrayList<>();
   if (d.getClipData() != null) for (int k = 0; k < d.getClipData().getItemCount(); k++) uris.add(d.getClipData().getItemAt(k).getUri());
   else if (d.getData() != null) uris.add(d.getData());
   if (uris.isEmpty()) return;
   process(a, uris);
  });
 }

 static void process(MainActivity a, List<Uri> uris) {
  a.toast(uris.size() == 1 ? "Reading the file…" : "Reading " + uris.size() + " files…", null);
  TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  ExecutorService pool = Executors.newSingleThreadExecutor();
  pool.execute(() -> {
   List<OcrParse.Result> results = new ArrayList<>();
   boolean[] modelPending = {false};
   List<List<OcrParse.El>> tableFallback = new ArrayList<>();
   final List<LedgerPdf.Out> ledgers = new ArrayList<>();
   List<Integer> tableWidths = new ArrayList<>();
   int failed = 0;
   for (Uri u : uris) {
    try {
     String type = a.getContentResolver().getType(u);
     if (looksLikePdf(type, displayName(a, u))) {
      int before = 0; for (OcrParse.Result x : results) before += x.count();
      int sizeBefore = results.size();
      List<OcrParse.El> docEls = new ArrayList<>(); final int[] docWidth = {1};
      boolean ok = readPdf(a, recognizer, u, (els, w) -> {
       docEls.addAll(els); if (w > docWidth[0]) docWidth[0] = w;
       OcrParse.Result one = OcrParse.parse(els, w, LocalDate.now());
       if (one.count() > 0) results.add(one);
      });
      if (!ok) { failed++; continue; }
      LedgerPdf.Out led = LedgerPdf.parseDb(a.db, docEls, docWidth[0]);
      if (led != null) { while (results.size() > sizeBefore) results.remove(results.size() - 1); ledgers.add(led); continue; }
      int after = 0; for (OcrParse.Result x : results) after += x.count();
      if (after == before) { tableFallback.add(docEls); tableWidths.add(docWidth[0]); }
     } else {
      Bitmap bmp = decode(a, u);
      if (bmp == null) { failed++; continue; }
      results.add(OcrParse.parse(ocr(recognizer, bmp), bmp.getWidth(), LocalDate.now()));
     }
    } catch (Exception e) { failed++; if (modelBusy(e)) modelPending[0] = true; }
   }
   pool.shutdown();
   if (modelPending[0] && results.isEmpty() && tableFallback.isEmpty()) {
    a.runOnUiThread(() -> a.toast("The on-device reader is still downloading. Try again in a minute", null));
    return;
   }
   OcrParse.Result merged = OcrParse.merge(results);
   if (merged.count() == 0 && !tableFallback.isEmpty()) {
    List<OcrParse.Result> tables = new ArrayList<>();
    for (int i = 0; i < tableFallback.size(); i++) tables.add(PdfTable.parse(tableFallback.get(i), tableWidths.get(i), LocalDate.now()));
    merged = OcrParse.merge(tables);
   }
   OcrParse.Result done = merged;
   int f = failed;
   if (!ledgers.isEmpty()) {
    a.runOnUiThread(() -> confirmLedgers(a, ledgers, done.count() > 0 ? () -> review(a, done, uris.size(), f) : null));
    return;
   }
   a.runOnUiThread(() -> review(a, done, uris.size(), f));
  });
 }

 /** The file is Hisab's own export table: import it with its account, category, person and note, skipping rows already in the ledger. */
 static void confirmLedgers(MainActivity a, List<LedgerPdf.Out> ledgers, Runnable then) {
  int rows = 0; for (LedgerPdf.Out o : ledgers) rows += o.rows;
  final int total = rows;
  Sheets.confirm(a, "Hisab export found", "This looks like a table exported from Hisab. " + total + " entries were read with their type, account, category and note. Rows already in your ledger are skipped, so importing twice adds nothing. Check the entries afterwards, because reading a PDF can misread a digit.", "Import " + total, () -> {
   int added = 0, skipped = 0;
   for (LedgerPdf.Out o : ledgers) { int[] r = CsvIo.importEntries(a.db, o.csv); added += r[0]; skipped += r[1]; }
   a.refresh();
   a.toast("Imported " + added + (skipped > 0 ? ", skipped " + skipped : ""), null);
   if (then != null) then.run();
  });
  if (total == 0 && then != null) then.run();
 }

 static boolean modelBusy(Throwable e) {
  while (e != null) {
   if (e instanceof com.google.mlkit.common.MlKitException && ((com.google.mlkit.common.MlKitException) e).getErrorCode() == com.google.mlkit.common.MlKitException.UNAVAILABLE) return true;
   e = e.getCause();
  }
  return false;
 }

 static List<OcrParse.El> ocr(TextRecognizer recognizer, Bitmap bmp) throws Exception {
  Text text = Tasks.await(recognizer.process(InputImage.fromBitmap(bmp, 0)));
  List<OcrParse.El> els = new ArrayList<>();
  for (Text.TextBlock b : text.getTextBlocks()) for (Text.Line l : b.getLines()) {
   Rect box = l.getBoundingBox();
   if (box != null && l.getText() != null) els.add(new OcrParse.El(l.getText(), box.left, box.top, box.right, box.bottom));
  }
  return els;
 }

 interface PageSink { void onPage(List<OcrParse.El> els, int width) throws Exception; }

 /** True when the picked file is a PDF, by MIME type or by file name. Some providers report PDFs as application/octet-stream or null, so the name check matters. */
 static boolean looksLikePdf(String mime, String name) {
  if (mime != null && mime.toLowerCase(java.util.Locale.ROOT).contains("pdf")) return true;
  return name != null && name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf");
 }

 static String displayName(MainActivity a, Uri u) {
  try (android.database.Cursor c = a.getContentResolver().query(u, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
   if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
  } catch (Exception e) {}
  return null;
 }

 /** Renders each page, OCRs it, then frees the bitmap before the next page, so a long statement does not run out of memory. */
 static boolean readPdf(MainActivity a, TextRecognizer recognizer, Uri u, PageSink sink) {
  try (android.os.ParcelFileDescriptor pfd = a.getContentResolver().openFileDescriptor(u, "r");
       android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(pfd)) {
   int n = Math.min(renderer.getPageCount(), 30);
   for (int i = 0; i < n; i++) {
    android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(i);
    int scale = 3;
    while (scale > 2 && Math.max(page.getWidth(), page.getHeight()) * scale > 3200) scale--;
    Bitmap bmp = Bitmap.createBitmap(page.getWidth() * scale, page.getHeight() * scale, Bitmap.Config.ARGB_8888);
    bmp.eraseColor(Color.WHITE);
    page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
    int w = bmp.getWidth();
    page.close();
    try { sink.onPage(ocr(recognizer, bmp), w); } finally { bmp.recycle(); }
   }
   return true;
  } catch (Exception e) {
   if (modelBusy(e)) throw new RuntimeException(e);
   return false;
  }
 }

 static Bitmap decode(MainActivity a, Uri u) {
  try {
   byte[] bytes = Backup.read(a.getContentResolver().openInputStream(u));
   BitmapFactory.Options opt = new BitmapFactory.Options(); opt.inJustDecodeBounds = true;
   BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
   if (opt.outWidth <= 0 || opt.outHeight <= 0) return null;
   opt.inSampleSize = 1;
   while (Math.max(opt.outWidth, opt.outHeight) / opt.inSampleSize > 2400) opt.inSampleSize *= 2;
   opt.inJustDecodeBounds = false;
   return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
  } catch (Exception | OutOfMemoryError e) { return null; }
 }

 /** Guesses a category name from the entry title, matching the app's own suggestion rules. */
 static String guessCategory(String title, Store db) {
  String icon = CategoryIcons.suggest(title);
  String name = CategoryIcons.existingCategory(icon);
  if (name == null) name = "Other";
  for (Store.Cat c : db.categories("expense")) if (c.name.equalsIgnoreCase(name)) return c.name;
  return name;
 }

 static void review(MainActivity a, OcrParse.Result r, int images, int failed) {
  if (r.count() == 0) {
   a.toast(failed > 0 ? "Could not read that file" : "No entries found. Try a clearer photo or a different PDF", null);
   return;
  }
  List<DayRows> groups = new ArrayList<>();
  for (OcrParse.Day d : r.days) {
   DayRows g = new DayRows(); g.day = d;
   for (OcrParse.Entry e : d.entries) g.rows.add(new Row(e, guessCategory(e.title, a.db)));
   groups.add(g);
  }
  for (OcrParse.Entry e : r.noDate) {
   DayRows g = new DayRows(); g.day = new OcrParse.Day(); g.day.label = "No date found"; g.day.day = "";
   g.rows.add(new Row(e, guessCategory(e.title, a.db))); g.rows.get(0).include = false;
   groups.add(g);
  }

  LinearLayout c = Sheets.sheetBody(a, "Review import");
  long total = 0; int n = 0;
  for (DayRows g : groups) for (Row row : g.rows) if (row.include) { total += row.entry.amount; n++; }
  String head = n + (n == 1 ? " entry" : " entries") + " · " + Fmt.money(total) + (images > 1 ? " · from " + images + " files" : "");
  if (failed > 0) head += " · " + failed + " unreadable";
  if (r.gst > 0) head += " · GST " + Fmt.money(r.gst) + " on this bill";
  TextView h = Ui.text(a, head, 14, Ui.muted, false); h.setPadding(0, 0, 0, Ui.dp(8)); c.addView(h);
  boolean anyMismatch = false;
  for (DayRows g : groups) if (g.day.mismatch()) anyMismatch = true;
  if (anyMismatch) {
   TextView warn = Ui.text(a, "Some day totals differ from the app's own header totals. Check those rows before saving.", 13, Ui.neg, false);
   warn.setPadding(0, 0, 0, Ui.dp(10)); c.addView(warn);
  }

  LinearLayout list = Ui.col(a);
  rebuild(a, list, groups);
  int rowCount = 0;
  for (DayRows g : groups) rowCount += 1 + g.rows.size();
  ScrollView sv = new ScrollView(a); sv.setVerticalScrollBarEnabled(false); sv.addView(list);
  c.addView(sv, new LinearLayout.LayoutParams(-1, Math.min(Ui.dp(56) * rowCount + Ui.dp(8), Ui.dp(430))));

  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  TextView save = Ui.button(a, "Add entries", true); c.addView(save, Ui.lpm(-1, -2, 0, 14, 0, 0));
  Ui.tap(save, () -> { int[] res = save(a, groups); Store db = a.db;
   if (r.gst > 0 && res[0] > 0) {
    db.setPref("gstBills", "" + (Long.parseLong(db.pref("gstBills", "0")) + r.gst));
    String day = ""; for (DayRows g : groups) if (!g.day.day.isEmpty()) { day = g.day.day; break; }
    if (!day.isEmpty()) { String mk = "gstBills:" + day.substring(0, 7); db.setPref(mk, "" + (Long.parseLong(db.pref(mk, "0")) + r.gst)); }
   }
   a.close(ref[0]); a.refresh(); a.toast("Imported " + res[0] + (res[1] > 0 ? " · skipped " + res[1] + " already there" : "") + (r.gst > 0 && res[0] > 0 ? " · GST " + Fmt.money(r.gst) + " noted" : ""), null); });
  ref[0] = Sheets.show(a, c);
 }

 static final class DayRows { OcrParse.Day day; final List<Row> rows = new ArrayList<>(); }

 static void rebuild(MainActivity a, LinearLayout list, List<DayRows> groups) {
  list.removeAllViews();
  for (DayRows g : groups) {
   LinearLayout dh = Ui.row(a); dh.setPadding(0, Ui.dp(12), 0, Ui.dp(4));
   dh.addView(Ui.text(a, g.day.label, 14, Ui.ink, true), Ui.weight(1));
   if (g.day.headerTotal >= 0) {
    String s = "Expenses: " + Fmt.num(g.day.headerTotal);
    TextView st = Ui.text(a, s + (g.day.mismatch() ? " · found " + Fmt.num(g.day.sum()) : " · matches"), 12.5f, g.day.mismatch() ? Ui.neg : Ui.muted, false);
    dh.addView(st);
   }
   list.addView(dh);
   for (Row row : g.rows) {
    LinearLayout er = Ui.row(a); er.setPadding(0, Ui.dp(7), 0, Ui.dp(7));
    er.setAlpha(row.include ? 1f : 0.4f);
    er.addView(Ui.badge(a, CategoryIcons.suggest(row.entry.title), 34, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(34), Ui.dp(34), 0, 0, 12, 0));
    LinearLayout mid = Ui.col(a);
    mid.addView(Ui.text(a, row.entry.title, 15, Ui.ink, false));
    mid.addView(Ui.text(a, row.cat + (row.include ? "" : " · not imported"), 12.5f, Ui.muted, false));
    er.addView(mid, Ui.weight(1));
    er.addView(Ui.text(a, Fmt.money(row.entry.amount), 15, Ui.ink, true));
    Ui.tap(er, () -> rowOptions(a, list, groups, g, row));
    list.addView(er);
   }
  }
 }

 static void rowOptions(MainActivity a, LinearLayout list, List<DayRows> groups, DayRows g, Row row) {
  List<String> labels = new ArrayList<>(); List<String> icons = new ArrayList<>();
  for (Store.Cat cat : a.db.categories("expense")) { labels.add(cat.name); icons.add(cat.icon); }
  labels.add(row.include ? "Leave out" : "Include again"); icons.add(row.include ? "close" : "check");
  int sel = 0;
  for (int i = 0; i < labels.size(); i++) if (labels.get(i).equals(row.cat)) sel = i;
  Sheets.pick(a, row.entry.title, labels, icons, sel, i -> {
   if (i == labels.size() - 1) row.include = !row.include;
   else row.cat = labels.get(i);
   rebuild(a, list, groups);
  });
 }

 /** Saves included rows with a date, skipping entries that already exist. Returns {added, skipped}. */
 static int[] save(MainActivity a, List<DayRows> groups) {
  Store db = a.db;
  long account = 0;
  for (Store.Acc x : db.accounts()) if (x.name.equalsIgnoreCase("cash")) account = x.id;
  if (account == 0 && !db.accounts().isEmpty()) account = db.accounts().get(0).id;
  if (account == 0) account = db.addAccount("Cash", "cash", 0);
  Map<String, Long> cats = new HashMap<>();
  for (Store.Cat x : db.categories("expense")) cats.put(x.name.toLowerCase(Locale.ROOT), x.id);
  int added = 0, skipped = 0;
  for (DayRows g : groups) {
   if (g.day.day.isEmpty()) continue;
   for (Row row : g.rows) {
    if (!row.include) continue;
    Long cid = cats.get(row.cat.toLowerCase(Locale.ROOT));
    if (cid == null) { cid = db.addCategory(row.cat, "expense", CategoryIcons.suggest(row.cat)); cats.put(row.cat.toLowerCase(Locale.ROOT), cid); }
    Store.Tx t = new Store.Tx();
    t.kind = "expense"; t.amount = row.entry.amount; t.day = g.day.day; t.account = account; t.cat = cid; t.note = row.entry.title;
    boolean dup = false;
    for (Store.Tx x : db.txs(t.day, t.day)) if (x.kind.equals("expense") && x.amount == t.amount && x.note.equals(t.note)) { dup = true; break; }
    if (dup) { skipped++; continue; }
    db.save(t); added++;
   }
  }
  return new int[]{added, skipped};
 }
}
