package in.aicon.hisab;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Pure parser for OCR lines from expense-tracker screenshots. No Android or ML Kit types, so it is fully unit-testable. */
final class OcrParse {
 private OcrParse() {}

 /** One OCR line with its position in the image. */
 static final class El {
  final String text; final int left, top, right, bottom;
  El(String text, int left, int top, int right, int bottom) { this.text = text; this.left = left; this.top = top; this.right = right; this.bottom = bottom; }
  int cy() { return (top + bottom) / 2; }
 }

 static final class Entry {
  String title; long amount; String day;
  Entry(String title, long amount, String day) { this.title = title; this.amount = amount; this.day = day; }
 }

 static final class Day {
  String day = ""; String label = ""; long headerTotal = -1; final List<Entry> entries = new ArrayList<>();
  long sum() { long s = 0; for (Entry e : entries) s += e.amount; return s; }
  boolean mismatch() { return headerTotal >= 0 && headerTotal != sum(); }
 }

 static final class Result {
  final List<Day> days = new ArrayList<>();
  final List<Entry> noDate = new ArrayList<>();
  int ignored;
  long total() { long s = 0; for (Day d : days) s += d.sum(); return s; }
  int count() { int n = 0; for (Day d : days) n += d.entries.size(); return n; }
 }

 private static final String MONTHS = "jan|january|feb|february|mar|march|apr|april|may|jun|june|jul|july|aug|august|sep|sept|september|oct|october|nov|november|dec|december";
 private static final Pattern DAY_HEADER = Pattern.compile("^(\\d{1,2})(?:st|nd|rd|th)?[\\s,.-]+(" + MONTHS + ")(?:[\\s,.-]+(.+))?$", Pattern.CASE_INSENSITIVE);
 private static final Pattern DAY_TOTAL = Pattern.compile("^(?:expenses?|spent)[:\\s]+-?((?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE);
 private static final Pattern AMOUNT = Pattern.compile("^-?((?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)$");
 private static final Pattern ENTRY_LINE = Pattern.compile("^(.+?)\\s+(-(?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)$");
 private static final Pattern MONTH_BUTTON = Pattern.compile("^(" + MONTHS + ")\\s+\\d{4}$", Pattern.CASE_INSENSITIVE);
 private static final Pattern FULL_HEADER = Pattern.compile("^\\d{1,2}(?:st|nd|rd|th)?[\\s,.-]+(?:" + MONTHS + ")(?:[\\s,.-]+\\S+)?[\\s,.-]+(?:expenses?|spent)[:\\s]+-?((?:\\d{1,3},)*\\d{1,3}(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE);
 private static final Pattern SUMMARY_WORD = Pattern.compile("^(expenses?|income|balance)$", Pattern.CASE_INSENSITIVE);

 static int monthOf(String m) {
  String s = m.toLowerCase(Locale.ROOT);
  String[] names = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
  for (int i = 0; i < names.length; i++) if (s.startsWith(names[i])) return i + 1;
  return 0;
 }

 static long amountMinor(String s) {
  String clean = s.replace(",", "");
  return Ledger.parse(clean);
 }

 /** Groups raw OCR lines into visual rows (same vertical band), each row a list of segments left to right. */
 static List<List<El>> rows(List<El> els, int imageWidth) {
  List<El> sorted = new ArrayList<>(els);
  sorted.sort((x, y) -> x.top != y.top ? x.top - y.top : x.left - y.left);
  List<List<El>> lines = new ArrayList<>();
  for (El e : sorted) {
   List<El> best = null;
   for (int i = lines.size() - 1; i >= 0 && i >= lines.size() - 6; i--) {
    List<El> line = lines.get(i);
    int top = Integer.MAX_VALUE, bottom = 0;
    for (El x : line) { top = Math.min(top, x.top); bottom = Math.max(bottom, x.bottom); }
    int h = Math.max(1, bottom - top);
    if (e.top < bottom - h / 3 && e.bottom > top + h / 3) { best = line; break; }
    if (e.top > bottom) break;
   }
   if (best == null) { best = new ArrayList<>(); lines.add(best); }
   best.add(e);
  }
  List<List<El>> out = new ArrayList<>();
  for (List<El> line : lines) { line.sort((x, y) -> x.left - y.left); out.add(line); }
  return out;
 }

 /** Parses one screenshot's OCR lines. today anchors year inference for headers without a year. */
 static Result parse(List<El> els, int imageWidth, LocalDate today) {
  Result r = new Result();
  Day cur = null;
  for (List<El> row : rows(els, imageWidth)) {
   String[] seg = new String[row.size()];
   for (int i = 0; i < row.size(); i++) seg[i] = row.get(i).text.trim();
   String joined = join(seg);

   Matcher dh = DAY_HEADER.matcher(seg[0]);
   boolean isDate = dh.find() && monthOf(dh.group(2)) > 0;
   long tot = -1;
   if (isDate && seg.length > 1) { Matcher m = DAY_TOTAL.matcher(seg[seg.length - 1]); if (m.find()) tot = amountMinor(m.group(1)); }
   if (isDate && tot < 0 && seg.length == 1) { Matcher m = FULL_HEADER.matcher(joined); if (m.find()) tot = amountMinor(m.group(1)); }
   if (isDate && tot >= 0) {
    int day = Integer.parseInt(dh.group(1)), month = monthOf(dh.group(2));
    int year = today.getYear();
    LocalDate date;
    try { date = LocalDate.of(year, month, day); } catch (Exception e) { r.ignored++; continue; }
    if (date.isAfter(today)) date = date.minusYears(1);
    cur = new Day();
    cur.day = date.toString(); cur.label = seg[0];
    cur.headerTotal = tot;
    r.days.add(cur);
    continue;
   }

   if (seg.length == 1 && (MONTH_BUTTON.matcher(seg[0]).find() || SUMMARY_WORD.matcher(seg[0]).find())) { r.ignored++; continue; }
   if (seg.length >= 1 && SUMMARY_WORD.matcher(seg[0]).find()) { r.ignored++; continue; }

   String last = seg[seg.length - 1];
   Matcher am = AMOUNT.matcher(last);
   String title = null; long amount = 0;
   if (am.find() && last.startsWith("-") && seg.length >= 2) {
    amount = amountMinor(am.group(1));
    title = join(Arrays.copyOf(seg, seg.length - 1)).trim();
   } else if (seg.length == 1) {
    Matcher em = ENTRY_LINE.matcher(seg[0]);
    if (em.find()) { title = em.group(1).trim(); amount = amountMinor(em.group(2).substring(1)); }
   }
   if (title != null) {
    if (amount <= 0 || title.isEmpty() || !title.matches(".*\\p{L}.*") || SUMMARY_WORD.matcher(title).find() || DAY_HEADER.matcher(title).find()) { r.ignored++; continue; }
    Entry e = new Entry(title, amount, cur == null ? "" : cur.day);
    if (cur == null) r.noDate.add(e); else cur.entries.add(e);
    continue;
   }
   r.ignored++;
  }
  return r;
 }

 private static String join(String[] seg) {
  StringBuilder b = new StringBuilder();
  for (String s : seg) { if (b.length() > 0) b.append(' '); b.append(s); }
  return b.toString();
 }

 /** Merges several screenshot results, combining days that share a date. */
 static Result merge(List<Result> rs) {
  Result out = new Result();
  Map<String, Day> byDay = new LinkedHashMap<>();
  for (Result r : rs) {
   out.noDate.addAll(r.noDate); out.ignored += r.ignored;
   for (Day d : r.days) {
    Day ex = byDay.get(d.day);
    if (ex == null) { byDay.put(d.day, d); out.days.add(d); }
    else { ex.entries.addAll(d.entries); if (ex.headerTotal < 0) ex.headerTotal = d.headerTotal; }
   }
  }
  return out;
 }
}
