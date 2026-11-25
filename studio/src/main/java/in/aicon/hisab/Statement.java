package in.aicon.hisab;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.time.YearMonth;
import java.util.*;

/** Monthly statement as a PDF, written into the app's private files and shared from there. No network. */
final class Statement {
 private Statement() {}

 static final class Data {
  YearMonth month; boolean allTime;
  long in, out, lent, borrowed;
  final List<String[]> rows = new ArrayList<>(); // day, title, account, signed amount text
  final List<String> catNames = new ArrayList<>(); // expense by category, for the donut
  final List<Long> catAmounts = new ArrayList<>();
  final List<Integer> catColors = new ArrayList<>();
  final List<String> barLabels = new ArrayList<>(); // spending over time, for the bar chart
  final List<Long> barAmounts = new ArrayList<>();
  long net() { return in - out; }
 }

 static Data collect(Store db, YearMonth ym) { return collect(db, ym, Fmt.from(ym), Fmt.to(ym), false); }
 static Data collectAll(Store db) { return collect(db, null, "0000-01-01", "9999-12-31", true); }

 private static Data collect(Store db, YearMonth ym, String from, String to, boolean allTime) {
  Data d = new Data(); d.month = ym; d.allTime = allTime;
  Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> ppl = Kit.peopleMap(db);
  Map<Long, Long> byCat = new HashMap<>(); Map<String, Long> byPeriod = new TreeMap<>();
  for (Store.Tx t : db.txs(from, to)) {
   if (t.kind.equals("income")) d.in += t.amount;
   else if (t.kind.equals("expense")) { d.out += t.amount; byCat.merge(t.cat, t.amount, Long::sum); byPeriod.merge(allTime ? t.day.substring(0, 7) : t.day, t.amount, Long::sum); }
   else if (t.kind.equals("lent")) d.lent += t.amount;
   else if (t.kind.equals("borrowed")) d.borrowed += t.amount;
   Store.Acc ac = accs.get(t.account);
   int s = t.kind.equals("transfer") ? 0 : Store.sign(t.kind);
   d.rows.add(new String[]{t.day, Kit.titleOf(t, cats, ppl), ac == null ? "" : ac.name, (s > 0 ? "+" : s < 0 ? "-" : "") + Fmt.money(t.amount)});
  }
  List<Map.Entry<Long, Long>> es = new ArrayList<>(byCat.entrySet()); es.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
  long rest = 0;
  for (int i = 0; i < es.size(); i++) {
   if (i < 6) { Store.Cat ct = cats.get(es.get(i).getKey()); d.catNames.add(ct == null ? "Other" : ct.name); d.catAmounts.add(es.get(i).getValue()); d.catColors.add(Kit.tone(es.get(i).getKey())); }
   else rest += es.get(i).getValue();
  }
  if (rest > 0) { d.catNames.add("Everything else"); d.catAmounts.add(rest); d.catColors.add(0xFFB9B2A4); }
  List<Map.Entry<String, Long>> periods = new ArrayList<>(byPeriod.entrySet());
  if (allTime && periods.size() > 12) periods = periods.subList(periods.size() - 12, periods.size());
  for (Map.Entry<String, Long> e : periods) {
   if (allTime) { String[] p = e.getKey().split("-"); d.barLabels.add(new String[]{"J","F","M","A","M","J","J","A","S","O","N","D"}[Integer.parseInt(p[1]) - 1] + p[0].substring(2)); }
   else d.barLabels.add("" + Integer.parseInt(e.getKey().substring(8)));
   d.barAmounts.add(e.getValue());
  }
  return d;
 }

 private static Paint paint(int color, float size, boolean bold) {
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  p.setColor(color); p.setTextSize(size);
  p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
  return p;
 }

 /** What the person chose in the options sheet: which charts to draw, and colour or black and white. */
 static final class Opts { boolean pie = true, bars = true, color = true; }

 /** Small popup before exporting: which charts to include, colour or black and white. Only the chosen parts are drawn. */
 static void optionsSheet(MainActivity a, YearMonth ym) {
  Opts o = new Opts();
  LinearLayout c = Sheets.sheetBody(a, ym == null ? "All-time statement" : "Statement for " + Fmt.month(ym));
  TextView hint = Ui.text(a, "Choose what goes into the PDF. Only what you pick is exported.", 13.5f, Ui.muted, false); hint.setPadding(0, 0, 0, Ui.dp(10)); c.addView(hint);
  String[][] rows = {{"Pie chart of spending by category", "pie"}, {"Bar chart of spending over time", "bars"}, {"Colours (off for black and white)", "color"}};
  for (String[] row : rows) {
   LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
   r.addView(Ui.text(a, row[0], 15, Ui.ink, true), Ui.weight(1));
   Switch sw = Ui.toggle(a); sw.setChecked(true); r.addView(sw);
   sw.setOnCheckedChangeListener((v, on) -> { if (row[1].equals("pie")) o.pie = on; else if (row[1].equals("bars")) o.bars = on; else o.color = on; });
   c.addView(r);
  }
  TextView make = Ui.button(a, "Create PDF", true); c.addView(make, Ui.lpm(-1, -2, 0, 16, 0, 0));
  Ui.tap(make, () -> { a.closeAll(); share(a, ym == null ? collectAll(a.db) : collect(a.db, ym), ym == null ? "Hisab statement - all time" : "Hisab statement " + Fmt.month(ym), o); });
  Sheets.show(a, c);
 }

 static File pdf(Context c, Data d) throws IOException { return pdf(c, d, new Opts()); }
 static File pdf(Context c, Data d, Opts o) throws IOException {
  int w = 595, h = 842, margin = 42, bottom = h - 48;
  PdfDocument doc = new PdfDocument();
  Paint title = paint(0xFF15161A, 22, true), head = paint(0xFF77736A, 11, true), body = paint(0xFF15161A, 11, false), small = paint(0xFF77736A, 9, false), line = new Paint();
  line.setColor(0xFFE3DDCF); line.setStrokeWidth(1f);
  int pageNum = 1, y;
  PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNum).create());
  Canvas cv = page.getCanvas();
  Wordmark wm = new Wordmark(c, 0xFF15161A); wm.measure(View.MeasureSpec.makeMeasureSpec(110, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(42, View.MeasureSpec.EXACTLY)); wm.layout(0, 0, 110, 42);
  cv.save(); cv.translate(margin, margin - 8); wm.draw(cv); cv.restore();
  y = margin + 58;
  cv.drawText(d.allTime ? "Statement - all time" : "Statement - " + Fmt.month(d.month), margin, y, paint(0xFF15161A, 15, true)); y += 20;
  cv.drawText((d.allTime ? "All time" : Fmt.month(d.month)) + "  ·  made on " + Fmt.today(), margin, y, small); y += 30;
  cv.drawText("Income " + Fmt.money(d.in), margin, y, paint(o.color ? 0xFF168A5C : 0xFF15161A, 13, true));
  cv.drawText("Spent " + Fmt.money(d.out), margin + 180, y, paint(o.color ? 0xFFD93C45 : 0xFF15161A, 13, true));
  cv.drawText("Kept " + Fmt.money(d.net()), margin + 340, y, paint(0xFF15161A, 13, true)); y += 18;
  if (d.lent > 0 || d.borrowed > 0) { cv.drawText("Lent " + Fmt.money(d.lent) + "    Borrowed " + Fmt.money(d.borrowed), margin, y, paint(0xFF77736A, 11, false)); y += 18; }
  y += 10;
  if (o.pie && !d.catAmounts.isEmpty()) y = donut(cv, d, margin, y, w - margin, bottom, o.color);
  if (o.bars && !d.barAmounts.isEmpty()) y = bars(cv, d, margin, y, w - margin, bottom, line, o.color);
  y += 6;
  cv.drawText("Date", margin, y, head); cv.drawText("Entry", margin + 78, y, head); cv.drawText("Account", margin + 330, y, head);
  Paint right = paint(0xFF77736A, 11, true); right.setTextAlign(Paint.Align.RIGHT);
  cv.drawText("Amount", w - margin, y, right); y += 8;
  cv.drawLine(margin, y, w - margin, y, line); y += 16;
  Paint amount = paint(0xFF15161A, 11, false); amount.setTextAlign(Paint.Align.RIGHT);
  for (String[] r : d.rows) {
   if (y > bottom) {
    footer(cv, w, h, margin, small);
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
  footer(cv, w, h, margin, small);
  doc.finishPage(page);
  File dir = new File(c.getExternalFilesDir(null), "statements"); dir.mkdirs();
  File f = new File(dir, d.allTime ? "Hisab-all-time.pdf" : "Hisab-" + d.month + ".pdf");
  FileOutputStream out = new FileOutputStream(f);
  doc.writeTo(out); out.close(); doc.close();
  return f;
 }

 /** Brand line at the foot of every page, with the public app link. */
 private static void footer(Canvas cv, int w, int h, int margin, Paint small) {
  Paint center = new Paint(small); center.setTextAlign(Paint.Align.CENTER);
  cv.drawText("Made with Hisab - github.com/aicon-hub/hisab/releases", w / 2f, h - 24, center);
 }

 /** Expense-by-category donut in full colour, with a legend. Returns the new y. */
 /** Grey ramp used when the person picks black and white. */
 static int grey(int i) { int v = Math.min(0xC8, 0x20 + i * 0x24); return 0xFF000000 | (v << 16) | (v << 8) | v; }

 private static int donut(Canvas cv, Data d, int left, int y, int right, int bottom, boolean color) {
  int size = 170, cx = left + size / 2, cy = y + 20 + size / 2;
  long total = 0; for (long v : d.catAmounts) total += v;
  Paint slice = new Paint(Paint.ANTI_ALIAS_FLAG); slice.setStyle(Paint.Style.STROKE); slice.setStrokeWidth(38);
  RectF box = new RectF(cx - size / 2 + 19, cy - size / 2 + 19, cx + size / 2 - 19, cy + size / 2 - 19);
  float start = -90;
  for (int i = 0; i < d.catAmounts.size(); i++) {
   float sweep = 360f * d.catAmounts.get(i) / total;
   slice.setColor(color ? d.catColors.get(i) : grey(i));
   cv.drawArc(box, start, Math.max(0.5f, sweep - 1.5f), false, slice);
   start += sweep;
  }
  Paint center = paint(0xFF15161A, 14, true); center.setTextAlign(Paint.Align.CENTER);
  cv.drawText(Fmt.compact(total), cx, cy + 5, center);
  Paint name = paint(0xFF15161A, 11, true), val = paint(0xFF77736A, 10, false);
  int ly = y + 28;
  for (int i = 0; i < d.catNames.size(); i++) {
   Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG); dot.setColor(color ? d.catColors.get(i) : grey(i));
   cv.drawCircle(left + size + 34, ly - 4, 5, dot);
   cv.drawText(clip(cv, d.catNames.get(i), name, 150), left + size + 48, ly, name);
   Paint pct = paint(0xFF77736A, 10, false); pct.setTextAlign(Paint.Align.RIGHT);
   cv.drawText(Math.round(100.0 * d.catAmounts.get(i) / total) + "%", right, ly, pct);
   ly += 20;
  }
  return Math.max(cy + size / 2, ly - 20 + 14) + 26;
 }

 /** Spending-over-time bar chart: per month for all-time statements, per day for a single month. Returns the new y. */
 private static int bars(Canvas cv, Data d, int left, int y, int right, int bottom, Paint line, boolean color) {
  int height = 110, top = y + 14, base = top + height;
  long max = 1; for (long v : d.barAmounts) max = Math.max(max, v);
  int n = d.barAmounts.size();
  float slot = (right - left) / (float) n, bw = Math.min(34, slot * 0.55f);
  Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
  int hi = 0; for (int i = 1; i < n; i++) if (d.barAmounts.get(i) > d.barAmounts.get(hi)) hi = i;
  for (int i = 0; i < n; i++) {
   float bh = Math.max(2, height * d.barAmounts.get(i) / (float) max);
   float cx = left + slot * i + slot / 2;
   bar.setColor(i == hi && color ? 0xFFC7F23A : 0xFF15161A);
   cv.drawRoundRect(new RectF(cx - bw / 2, base - bh, cx + bw / 2, base), bw / 3, bw / 3, bar);
  }
  cv.drawLine(left, base, right, base, line);
  Paint lab = paint(0xFF77736A, 8, false); lab.setTextAlign(Paint.Align.CENTER);
  int step = Math.max(1, (int) Math.ceil(n / 12.0));
  for (int i = 0; i < n; i += step) cv.drawText(d.barLabels.get(i), left + slot * i + slot / 2, base + 14, lab);
  Paint cap = paint(0xFF77736A, 10, false);
  cv.drawText(d.allTime ? "Spending by month (last 12 with entries)" : "Spending by day", left, top - 2, cap);
  return base + 30;
 }

 /** Truncates long text so columns never overlap. */
 static String clip(Canvas cv, String s, Paint p, float maxWidth) {
  if (p.measureText(s) <= maxWidth) return s;
  String e = "…";
  int n = s.length();
  while (n > 0 && p.measureText(s.substring(0, n) + e) > maxWidth) n--;
  return s.substring(0, Math.max(0, n)) + e;
 }

 static void share(MainActivity a, YearMonth ym) { optionsSheet(a, ym); }
 static void shareAll(MainActivity a) { optionsSheet(a, null); }

 private static void share(MainActivity a, Data d, String subject, Opts o) {
  try {
   File f = pdf(a, d, o);
   Uri u = androidx.core.content.FileProvider.getUriForFile(a, "in.aicon.hisab.studio2.fileprovider", f);
   Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, u).putExtra(Intent.EXTRA_SUBJECT, subject).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   a.launchExternal(Intent.createChooser(i, "Share statement"));
  } catch (Exception e) {
   a.toast("Statement could not be created", null);
  }
 }
}
