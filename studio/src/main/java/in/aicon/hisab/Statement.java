package in.aicon.hisab;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.*;
import java.time.YearMonth;
import java.util.*;

/** Monthly statement as a PDF, written into the app's private files and shared from there. No network. */
final class Statement {
 private Statement() {}

 static final class Data {
  YearMonth month;
  long in, out, lent, borrowed;
  final List<String[]> rows = new ArrayList<>(); // day, title, account, signed amount text
  long net() { return in - out; }
 }

 static Data collect(Store db, YearMonth ym) {
  Data d = new Data(); d.month = ym;
  Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> ppl = Kit.peopleMap(db);
  for (Store.Tx t : db.txs(Fmt.from(ym), Fmt.to(ym))) {
   if (t.kind.equals("income")) d.in += t.amount;
   else if (t.kind.equals("expense")) d.out += t.amount;
   else if (t.kind.equals("lent")) d.lent += t.amount;
   else if (t.kind.equals("borrowed")) d.borrowed += t.amount;
   Store.Acc ac = accs.get(t.account);
   int s = t.kind.equals("transfer") ? 0 : Store.sign(t.kind);
   d.rows.add(new String[]{t.day, Kit.titleOf(t, cats, ppl), ac == null ? "" : ac.name, (s > 0 ? "+" : s < 0 ? "-" : "") + Fmt.money(t.amount)});
  }
  return d;
 }

 private static Paint paint(int color, float size, boolean bold) {
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  p.setColor(color); p.setTextSize(size);
  p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
  return p;
 }

 static File pdf(Context c, Data d) throws IOException {
  int w = 595, h = 842, margin = 42, bottom = h - 48;
  PdfDocument doc = new PdfDocument();
  Paint title = paint(0xFF15161A, 22, true), head = paint(0xFF77736A, 11, true), body = paint(0xFF15161A, 11, false), small = paint(0xFF77736A, 9, false), line = new Paint();
  line.setColor(0xFFE3DDCF); line.setStrokeWidth(1f);
  int pageNum = 1, y;
  PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNum).create());
  Canvas cv = page.getCanvas();
  y = margin + 24;
  cv.drawText("Hisab statement", margin, y, title); y += 26;
  cv.drawText(Fmt.month(d.month) + "  ·  made on " + Fmt.today(), margin, y, small); y += 30;
  cv.drawText("Income " + Fmt.money(d.in) + "    Spent " + Fmt.money(d.out) + "    Kept " + Fmt.money(d.net()), margin, y, paint(d.net() < 0 ? 0xFFD93C45 : 0xFF15161A, 13, true)); y += 18;
  if (d.lent > 0 || d.borrowed > 0) { cv.drawText("Lent " + Fmt.money(d.lent) + "    Borrowed " + Fmt.money(d.borrowed), margin, y, paint(0xFF77736A, 11, false)); y += 18; }
  y += 10;
  cv.drawText("Date", margin, y, head); cv.drawText("Entry", margin + 78, y, head); cv.drawText("Account", margin + 330, y, head);
  Paint right = paint(0xFF77736A, 11, true); right.setTextAlign(Paint.Align.RIGHT);
  cv.drawText("Amount", w - margin, y, right); y += 8;
  cv.drawLine(margin, y, w - margin, y, line); y += 16;
  Paint amount = paint(0xFF15161A, 11, false); amount.setTextAlign(Paint.Align.RIGHT);
  for (String[] r : d.rows) {
   if (y > bottom) {
    doc.finishPage(page);
    pageNum++;
    page = doc.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNum).create());
    cv = page.getCanvas(); y = margin;
   }
   cv.drawText(r[0].substring(5), margin, y, body);
   cv.drawText(clip(cv, r[1], body, 240), margin + 78, y, body);
   cv.drawText(clip(cv, r[2], body, 110), margin + 330, y, body);
   cv.drawText(r[3], w - margin, y, amount);
   y += 18;
  }
  if (d.rows.isEmpty()) { cv.drawText("No entries this month.", margin, y, body); y += 18; }
  y += 6; cv.drawLine(margin, y, w - margin, y, line); y += 16;
  cv.drawText("Hisab keeps your entries on this phone only. Free and open source, GPL-3.0.", margin, y, small);
  doc.finishPage(page);
  File dir = new File(c.getExternalFilesDir(null), "statements"); dir.mkdirs();
  File f = new File(dir, "Hisab-" + d.month + ".pdf");
  FileOutputStream out = new FileOutputStream(f);
  doc.writeTo(out); out.close(); doc.close();
  return f;
 }

 /** Truncates long text so columns never overlap. */
 static String clip(Canvas cv, String s, Paint p, float maxWidth) {
  if (p.measureText(s) <= maxWidth) return s;
  String e = "…";
  int n = s.length();
  while (n > 0 && p.measureText(s.substring(0, n) + e) > maxWidth) n--;
  return s.substring(0, Math.max(0, n)) + e;
 }

 static void share(MainActivity a, YearMonth ym) {
  try {
   File f = pdf(a, collect(a.db, ym));
   Uri u = androidx.core.content.FileProvider.getUriForFile(a, "in.aicon.hisab.studio2.fileprovider", f);
   Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, u).putExtra(Intent.EXTRA_SUBJECT, "Hisab statement " + Fmt.month(ym)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   a.launchExternal(Intent.createChooser(i, "Share statement"));
  } catch (Exception e) {
   a.toast("Statement could not be created", null);
  }
 }
}
