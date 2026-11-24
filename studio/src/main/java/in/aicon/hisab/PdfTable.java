package in.aicon.hisab;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Fallback parser for statement-style tables (PDF exports): a date and an amount on one visual line. */
final class PdfTable {
 private PdfTable() {}

 private static final Pattern AMOUNT_CELL = Pattern.compile("^[₹$€£\\s]*-?((?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:dr|DR|Dr)?$");
 private static final Pattern DAY_MON = Pattern.compile("^(\\d{1,2})(?:st|nd|rd|th)?[\\s,.-]+([A-Za-z]{3,9})$");
 private static final Pattern HEADERISH = Pattern.compile("^(date|description|details|amount|debit|credit|balance|particulars|narration|title|category)$", Pattern.CASE_INSENSITIVE);

 static OcrParse.Result parse(List<OcrParse.El> els, int width, LocalDate today) {
  OcrParse.Result r = new OcrParse.Result();
  Map<String, OcrParse.Day> byDay = new LinkedHashMap<>();
  for (List<OcrParse.El> line : OcrParse.rows(els, width)) {
   String[] seg = new String[line.size()];
   for (int i = 0; i < line.size(); i++) seg[i] = line.get(i).text.trim();

   int headerHits = 0;
   for (String s : seg) if (HEADERISH.matcher(s).find()) headerHits++;
   if (headerHits >= 2) { r.ignored++; continue; }

   int dateIdx = -1; LocalDate date = null;
   for (int i = 0; i < seg.length; i++) {
    date = AnyCsv.parseDate(seg[i], today);
    if (date != null) { dateIdx = i; break; }
    Matcher dm = DAY_MON.matcher(seg[i]);
    if (dm.find() && OcrParse.monthOf(dm.group(2)) > 0) {
     try {
      date = LocalDate.of(today.getYear(), OcrParse.monthOf(dm.group(2)), Integer.parseInt(dm.group(1)));
      if (date.isAfter(today)) date = date.minusYears(1);
      dateIdx = i; break;
     } catch (Exception e) { date = null; }
    }
   }
   if (dateIdx < 0) { r.ignored++; continue; }

   int amtIdx = -1; long amount = 0;
   for (int i = seg.length - 1; i >= 0; i--) {
    if (i == dateIdx) continue;
    Matcher am = AMOUNT_CELL.matcher(seg[i]);
    if (am.find() && seg[i].matches(".*\\d.*")) {
     long v = OcrParse.amountMinor(am.group(1));
     if (v > 0) { amount = v; amtIdx = i; break; }
    }
   }
   if (amtIdx < 0) { r.ignored++; continue; }

   StringBuilder title = new StringBuilder();
   for (int i = 0; i < seg.length; i++) {
    if (i == dateIdx || i == amtIdx) continue;
    if (HEADERISH.matcher(seg[i]).find()) continue;
    if (title.length() > 0) title.append(' ');
    title.append(seg[i]);
   }
   String t = title.toString().trim();
   if (t.isEmpty() || !t.matches(".*\\p{L}.*")) { r.ignored++; continue; }

   OcrParse.Day d = byDay.get(date.toString());
   if (d == null) { d = new OcrParse.Day(); d.day = date.toString(); d.label = date.toString(); byDay.put(d.day, d); r.days.add(d); }
   d.entries.add(new OcrParse.Entry(t, amount, d.day));
  }
  return r;
 }
}
