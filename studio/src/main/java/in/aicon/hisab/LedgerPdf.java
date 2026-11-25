package in.aicon.hisab;

import java.util.*;
import java.util.regex.*;

/**
 * Reads a PDF or screenshot of Hisab's own export table (Date, Type, Amount, Account, To account, Category, Person, Note).
 * Text recognition often merges neighbouring cells into one line, so each row is read as a string, and the
 * account, category and person are matched against the names Hisab already knows.
 */
final class LedgerPdf {
 private LedgerPdf() {}

 private static final Pattern ROW = Pattern.compile("^[|\\s]*(\\d{4}-\\d{2}-\\d{2})\\s+(expense|income|transfer|lent|borrowed|repay_in|repay_out)\\s+[₹$€£]?\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s*(.*)$", Pattern.CASE_INSENSITIVE);

 static final class Out { String csv; int rows; }

 /** Returns the rows found as CSV in Hisab's import shape, or null when the file does not look like this table. */
 static Out parse(List<OcrParse.El> els, int width, Collection<String> accounts, Collection<String> expenseCats, Collection<String> incomeCats, Collection<String> people) {
  StringBuilder b = new StringBuilder(CsvIo.HEADER).append('\n'); int n = 0;
  for (List<OcrParse.El> line : OcrParse.rows(els, width)) {
   StringBuilder sb = new StringBuilder();
   for (OcrParse.El e : line) { if (sb.length() > 0) sb.append(' '); sb.append(e.text.trim()); }
   Matcher m = ROW.matcher(sb.toString().trim());
   if (!m.find()) continue;
   String day = m.group(1), kind = m.group(2).toLowerCase(Locale.ROOT), amt = m.group(3).replace(",", ""), rest = m.group(4).trim();
   String[] t = take(rest, accounts); String acc = t[0]; rest = t[1];
   String to = "", cat = "", person = "";
   if (kind.equals("transfer")) { t = take(rest, accounts); to = t[0]; rest = t[1]; }
   else if (kind.equals("expense") || kind.equals("income")) { t = take(rest, kind.equals("expense") ? expenseCats : incomeCats); cat = t[0]; rest = t[1]; }
   else { t = take(rest, people); person = t[0]; rest = t[1]; }
   if (acc.isEmpty()) continue;
   b.append(day).append(',').append(kind).append(',').append(amt).append(',').append(q(acc)).append(',').append(q(to)).append(',').append(q(cat)).append(',').append(q(person)).append(',').append(q(rest)).append('\n');
   n++;
  }
  if (n == 0) return null;
  Out o = new Out(); o.csv = b.toString(); o.rows = n; return o;
 }

 /** Longest known name that starts the text, else its first word. Returns {name, remainder}. */
 static String[] take(String text, Collection<String> known) {
  String best = null; String low = text.toLowerCase(Locale.ROOT);
  for (String k : known) {
   String kl = k.toLowerCase(Locale.ROOT).trim();
   if (kl.isEmpty() || !low.startsWith(kl)) continue;
   if (low.length() > kl.length() && Character.isLetterOrDigit(low.charAt(kl.length())) && Character.isLetterOrDigit(kl.charAt(kl.length() - 1))) continue;
   if (best == null || k.length() > best.length()) best = k;
  }
  if (best != null) return new String[]{best, text.substring(best.length()).trim()};
  if (text.isEmpty()) return new String[]{"", ""};
  int sp = text.indexOf(' ');
  return sp < 0 ? new String[]{text, ""} : new String[]{text.substring(0, sp), text.substring(sp + 1).trim()};
 }

 private static String q(String s) {
  if (s == null || s.isEmpty()) return "";
  return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
 }

 static Out parseDb(Store db, List<OcrParse.El> els, int width) {
  List<String> acc = new ArrayList<>(), ex = new ArrayList<>(), in = new ArrayList<>(), pe = new ArrayList<>();
  for (Store.Acc a : db.accounts()) acc.add(a.name);
  for (Store.Cat c : db.categories(null)) (c.kind.equals("income") ? in : ex).add(c.name);
  for (Store.Person p : db.people()) pe.add(p.name);
  return parse(els, width, acc, ex, in, pe);
 }
}
