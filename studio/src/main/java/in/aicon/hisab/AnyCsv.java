package in.aicon.hisab;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Reads CSV exports from other expense apps: finds the date, amount and title columns on its own. On-device only. */
final class AnyCsv {
 private AnyCsv() {}

 private static final String[] DATE_NAMES = {"date", "day", "transaction date", "posted date", "posted", "value date", "txn date"};
 private static final String[] AMOUNT_NAMES = {"amount", "amt", "value", "sum", "spent", "expense", "debit", "withdrawal", "debit amount", "dr", "price", "cost"};
 private static final String[] CREDIT_NAMES = {"credit", "deposit", "cr", "income", "credit amount"};
 private static final String[] TITLE_NAMES = {"title", "note", "notes", "description", "name", "narration", "particulars", "merchant", "payee", "details", "item", "memo"};
 private static final String[] CATEGORY_NAMES = {"category", "cat", "group", "tag", "tags"};
 private static final String[] TYPE_NAMES = {"type", "kind", "dr/cr", "transaction type"};

 static boolean isHisabShape(List<String[]> rows) {
  if (rows.isEmpty()) return false;
  String[] h = rows.get(0);
  return h.length >= 4 && h[0].trim().equalsIgnoreCase("Date") && h[1].trim().equalsIgnoreCase("Type") && h[2].trim().equalsIgnoreCase("Amount") && h[3].trim().equalsIgnoreCase("Account");
 }

 static int col(String[] header, String[] names) {
  for (int i = 0; i < header.length; i++) {
   String c = header[i].trim().toLowerCase(Locale.ROOT);
   for (String n : names) if (c.equals(n) || c.contains(n)) return i;
  }
  return -1;
 }

 static boolean looksLikeHeader(String[] row) {
  int hits = 0;
  for (String cell : row) {
   String c = cell.trim().toLowerCase(Locale.ROOT);
   if (c.isEmpty()) continue;
   if (!c.matches(".*\\d.*")) hits++;
  }
  return hits >= Math.max(2, row.length - 1) && col(row, DATE_NAMES) >= 0;
 }

 private static final Pattern ISO = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
 private static final Pattern DMY = Pattern.compile("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})$");
 private static final Pattern DMONY = Pattern.compile("^(\\d{1,2})(?:st|nd|rd|th)?[\\s-]+([A-Za-z]{3,9})[\\s,.-]+(\\d{2,4})$");
 private static final Pattern MONDY = Pattern.compile("^([A-Za-z]{3,9})[\\s-]+(\\d{1,2})(?:st|nd|rd|th)?[\\s,.-]+(\\d{2,4})$");

 static LocalDate parseDate(String s, LocalDate today) {
  s = s.trim();
  Matcher m = ISO.matcher(s);
  if (m.find()) return safe(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
  m = DMY.matcher(s);
  if (m.find()) {
   int a = Integer.parseInt(m.group(1)), b = Integer.parseInt(m.group(2)), y = Integer.parseInt(m.group(3));
   if (y < 100) y += 2000;
   if (a > 12 && b <= 12) return safe(y, b, a);
   if (b > 12 && a <= 12) return safe(y, a, b);
   return safe(y, b, a); // ambiguous: assume day first, the common export order in India
  }
  m = DMONY.matcher(s);
  if (m.find()) {
   int mon = OcrParse.monthOf(m.group(2)), y = Integer.parseInt(m.group(3));
   if (mon > 0) return safe(y < 100 ? y + 2000 : y, mon, Integer.parseInt(m.group(1)));
  }
  m = MONDY.matcher(s);
  if (m.find()) {
   int mon = OcrParse.monthOf(m.group(1)), y = Integer.parseInt(m.group(3));
   if (mon > 0) return safe(y < 100 ? y + 2000 : y, mon, Integer.parseInt(m.group(2)));
  }
  return null;
 }

 private static LocalDate safe(int y, int m, int d) {
  try { return LocalDate.of(y, m, d); } catch (Exception e) { return null; }
 }

 private static final Pattern NUM = Pattern.compile("^[₹$€£\\s]*\\(?-?((?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)\\)?\\s*(?:dr|cr|DR|CR|Dr|Cr)?$");

 /** Parses an amount cell. Negative when the cell marks money out. */
 static long parseAmount(String s) {
  s = s.trim();
  if (s.isEmpty()) return 0;
  boolean neg = s.contains("-") || s.contains("(") || s.toLowerCase(Locale.ROOT).endsWith("dr");
  Matcher m = NUM.matcher(s);
  if (!m.find()) return 0;
  long v = OcrParse.amountMinor(m.group(1));
  return neg ? -v : v;
 }

 /** Maps rows from another app's CSV onto review-screen days. Expenses only; income rows are counted and left out. */
 static OcrParse.Result detect(List<String[]> rows, LocalDate today) {
  OcrParse.Result r = new OcrParse.Result();
  if (rows.size() < 2) return r;
  int head = looksLikeHeader(rows.get(0)) ? 0 : -1;
  if (head < 0) { r.ignored = rows.size(); return r; }
  String[] h = rows.get(0);
  int cDate = col(h, DATE_NAMES), cAmt = col(h, AMOUNT_NAMES), cCr = col(h, CREDIT_NAMES), cTitle = col(h, TITLE_NAMES), cCat = col(h, CATEGORY_NAMES), cType = col(h, TYPE_NAMES);
  if (cDate < 0) { r.ignored = rows.size(); return r; }
  Map<String, OcrParse.Day> byDay = new LinkedHashMap<>();
  for (int i = 1; i < rows.size(); i++) {
   String[] row = rows.get(i);
   String dcell = cell(row, cDate);
   if (dcell.isEmpty()) { r.ignored++; continue; }
   LocalDate date = parseDate(dcell, today);
   if (date == null) { r.ignored++; continue; }
   long amt = cAmt >= 0 ? parseAmount(cell(row, cAmt)) : 0;
   if (amt == 0 && cCr >= 0) { long cr = parseAmount(cell(row, cCr)); if (cr != 0) { r.ignored++; continue; } } // income row
   String type = cType >= 0 ? cell(row, cType).toLowerCase(Locale.ROOT) : "";
   if (type.contains("credit") || type.equals("cr") || type.contains("income")) { r.ignored++; continue; }
   if (amt < 0) amt = -amt;
   if (amt <= 0) { r.ignored++; continue; }
   String title = cTitle >= 0 ? cell(row, cTitle).trim() : "";
   String catName = cCat >= 0 ? cell(row, cCat).trim() : "";
   if (title.isEmpty()) title = catName.isEmpty() ? "Entry" : catName;
   if (!catName.isEmpty() && !title.toLowerCase(Locale.ROOT).contains(catName.toLowerCase(Locale.ROOT))) title = title + " · " + catName;
   OcrParse.Day d = byDay.get(date.toString());
   if (d == null) { d = new OcrParse.Day(); d.day = date.toString(); d.label = date.toString(); byDay.put(d.day, d); r.days.add(d); }
   d.entries.add(new OcrParse.Entry(title, amt, d.day));
  }
  return r;
 }

 private static String cell(String[] row, int i) { return i >= 0 && i < row.length ? row[i] : ""; }
}
