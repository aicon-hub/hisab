package in.aicon.hisab;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.*;
import java.util.*;

/** One person's lent and borrowed history as a PDF, and a UPI payment request for what you owe them. No network. */
final class PersonStatement {
 private PersonStatement() {}

 static final class Row { String day, what; long delta, balance; }

 static long delta(String kind, long amount) {
  switch (kind) { case "lent": case "repay_out": return amount; case "repay_in": case "borrowed": return -amount; default: return 0; }
 }

 /** Oldest first, with the running balance after each entry. Positive means they owe you. */
 static List<Row> rows(Store db, long personId) {
  List<Store.Tx> hist = new ArrayList<>(db.txsOfPerson(personId)); Collections.reverse(hist);
  List<Row> out = new ArrayList<>(); long bal = 0;
  for (Store.Tx t : hist) {
   long d = delta(t.kind, t.amount); if (d == 0) continue;
   Row r = new Row(); r.day = t.day; r.delta = d; bal += d; r.balance = bal;
   String label = t.kind.equals("lent") ? "Lent" : t.kind.equals("borrowed") ? "Borrowed" : t.kind.equals("repay_in") ? "They paid" : "You paid";
   r.what = t.note == null || t.note.isEmpty() ? label : label + " - " + t.note;
   out.add(r);
  }
  return out;
 }

 /** Words for the balance line. */
 static String summary(String name, long net) {
  return net > 0 ? name + " owes you " + Fmt.money(net) : net < 0 ? "You owe " + name + " " + Fmt.money(-net) : "All settled with " + name;
 }

 /** A valid UPI id looks like name@handle. */
 static boolean validUpi(String s) { return s != null && s.trim().matches("[A-Za-z0-9._\\-]{2,64}@[A-Za-z][A-Za-z0-9.\\-]{1,40}"); }

 static String upiOf(Store db, long personId) { return db.pref("upi:" + personId, ""); }

 /** upi://pay link for an amount in paise; null when the id or amount is not usable. */
 static Uri upiUri(String upiId, String name, long amount) {
  if (!validUpi(upiId) || amount <= 0) return null;
  return Uri.parse("upi://pay?pa=" + Uri.encode(upiId.trim()) + "&pn=" + Uri.encode(name) + "&am=" + Ledger.plain(amount) + "&cu=INR&tn=" + Uri.encode("Settling up via Hisab"));
 }

 static File pdf(Context c, Store.Person p, long net, List<Row> rows) throws IOException {
  int w = 595, h = 842, margin = 42, bottom = h - 56;
  PdfDocument doc = new PdfDocument();
  Paint title = paint(0xFF15161A, 20, true), body = paint(0xFF15161A, 11, false), head = paint(0xFF77736A, 11, true), small = paint(0xFF77736A, 9, false), line = new Paint();
  line.setColor(0xFFE3DDCF); line.setStrokeWidth(1f);
  Paint right = paint(0xFF15161A, 11, false); right.setTextAlign(Paint.Align.RIGHT);
  Paint rightHead = paint(0xFF77736A, 11, true); rightHead.setTextAlign(Paint.Align.RIGHT);
  int pageNum = 1;
  PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNum).create());
  Canvas cv = page.getCanvas(); int y = margin + 18;
  cv.drawText("Statement - " + Statement.clip(cv, p.name, title, 380), margin, y, title); y += 22;
  cv.drawText("Made on " + Fmt.today(), margin, y, small); y += 28;
  cv.drawText(summary(p.name, net), margin, y, paint(0xFF15161A, 14, true)); y += 30;
  cv.drawText("Date", margin, y, head); cv.drawText("Entry", margin + 78, y, head); cv.drawText("Amount", w - margin - 110, y, rightHead); cv.drawText("Balance", w - margin, y, rightHead); y += 8;
  cv.drawLine(margin, y, w - margin, y, line); y += 16;
  for (Row r : rows) {
   if (y > bottom) { footer(cv, w, h, small); doc.finishPage(page); pageNum++; page = doc.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNum).create()); cv = page.getCanvas(); y = margin; }
   cv.drawText(r.day, margin, y, body);
   cv.drawText(Statement.clip(cv, r.what, body, 230), margin + 78, y, body);
   cv.drawText((r.delta > 0 ? "+" : "-") + Fmt.money(Math.abs(r.delta)), w - margin - 110, y, right);
   cv.drawText((r.balance < 0 ? "-" : "") + Fmt.money(Math.abs(r.balance)), w - margin, y, right);
   y += 18;
  }
  if (rows.isEmpty()) { cv.drawText("No entries yet.", margin, y, body); y += 18; }
  y += 6; cv.drawLine(margin, y, w - margin, y, line); y += 16;
  cv.drawText("Balance: positive means " + p.name + " owes you, negative means you owe " + p.name + ".", margin, y, small);
  footer(cv, w, h, small);
  doc.finishPage(page);
  File dir = new File(c.getExternalFilesDir(null), "statements"); dir.mkdirs();
  File f = new File(dir, "Hisab-person-" + p.id + ".pdf");
  FileOutputStream out = new FileOutputStream(f);
  doc.writeTo(out); out.close(); doc.close();
  return f;
 }

 private static Paint paint(int color, float size, boolean bold) {
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(color); p.setTextSize(size);
  p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); return p;
 }

 private static void footer(Canvas cv, int w, int h, Paint small) {
  Paint center = new Paint(small); center.setTextAlign(Paint.Align.CENTER);
  cv.drawText("Made with Hisab - github.com/aicon-hub/hisab/releases", w / 2f, h - 24, center);
 }

 static void share(MainActivity a, Store.Person p) {
  try {
   File f = pdf(a, p, a.db.net(p.id), rows(a.db, p.id));
   Uri u = androidx.core.content.FileProvider.getUriForFile(a, "in.aicon.hisab.studio2.fileprovider", f);
   Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, u).putExtra(Intent.EXTRA_SUBJECT, "Hisab statement - " + p.name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   a.launchExternal(Intent.createChooser(i, "Share statement"));
  } catch (Exception e) { a.toast("Statement could not be created", null); }
 }

 /** Asks for the UPI id once, remembers it for this person, then opens the payment link. Nothing is paid until you confirm in your UPI app. */
 static void payByUpi(MainActivity a, Store.Person p, long owed) {
  String saved = upiOf(a.db, p.id);
  Sheets.form(a, "Pay " + p.name + " by UPI", new String[]{"Their UPI id (like name@bank)"}, new String[]{saved}, new int[]{Sheets.TEXT}, "Open UPI app", v -> {
   String id = v[0].trim();
   Uri u = upiUri(id, p.name, owed);
   if (u == null) { a.toast("That does not look like a UPI id", null); return; }
   a.db.setPref("upi:" + p.id, id); a.closeAll();
   a.launchExternal(Intent.createChooser(new Intent(Intent.ACTION_VIEW, u), "Pay with"));
  }, null);
 }
}
