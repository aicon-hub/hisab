package in.aicon.hisab;

import java.time.*;
import java.time.format.*;
import java.util.*;

final class Fmt {
 private Fmt() {}
 static String symbol = "₹", grouping = "Indian", dateStyle = "DMY";
 static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP", "AED", "SAR", "SGD", "MYR", "CAD", "AUD"};
 static final String[] SYMBOLS = {"₹", "$", "€", "£", "AED ", "SAR ", "S$", "RM ", "C$", "A$"};
 static void currency(String code) { int i = Arrays.asList(CURRENCIES).indexOf(code); symbol = SYMBOLS[i < 0 ? 0 : i]; }

 static String group(long whole) {
  String s = Long.toString(Math.abs(whole)); if (grouping.equals("None") || s.length() <= 3) return s;
  StringBuilder b = new StringBuilder(); String tail = s.substring(s.length() - 3), head = s.substring(0, s.length() - 3);
  if (grouping.equals("Indian")) { int n = head.length(); for (int i = 0; i < n; i++) { b.append(head.charAt(i)); int left = n - i - 1; if (left > 0 && left % 2 == 0) b.append(','); } }
  else { int n = head.length(); for (int i = 0; i < n; i++) { b.append(head.charAt(i)); int left = n - i - 1; if (left > 0 && left % 3 == 0) b.append(','); } }
  return b + "," + tail;
 }
 /** Plain number: whole units when there are no paise. */
 static String num(long minor) {
  long a = Math.abs(minor), w = a / 100, p = a % 100;
  return group(w) + (p == 0 ? "" : String.format(Locale.US, ".%02d", p));
 }
 static String money(long minor) { return (minor < 0 ? "-" : "") + symbol + num(minor); }
 static String signed(long minor) { return (minor < 0 ? "-" : "+") + symbol + num(minor); }
 /** Short for tight spaces: 1.2k, 4.5L. */
 static String compact(long minor) {
  double v = Math.abs(minor) / 100.0; String s;
  if (v >= 10000000) s = String.format(Locale.US, "%.1fCr", v / 10000000); else if (v >= 100000) s = String.format(Locale.US, "%.1fL", v / 100000); else if (v >= 1000) s = String.format(Locale.US, "%.1fk", v / 1000); else s = String.format(Locale.US, "%.0f", v);
  return symbol + s.replace(".0", "");
 }

 static LocalDate today() { return LocalDate.now(); }
 static String month(YearMonth m) { return m.getMonth().getDisplayName(TextStyle.FULL, L.locale()) + " " + m.getYear(); }
 static String monthShort(YearMonth m) { return m.getMonth().getDisplayName(TextStyle.SHORT, L.locale()); }
 static String dayLabel(String iso) {
  LocalDate d = LocalDate.parse(iso), t = today();
  if (d.equals(t)) return "Today"; if (d.equals(t.minusDays(1))) return "Yesterday";
  return d.getDayOfWeek().getDisplayName(TextStyle.SHORT, L.locale()) + ", " + d.getDayOfMonth() + " " + d.getMonth().getDisplayName(TextStyle.SHORT, L.locale());
 }
 static String shortDate(String iso) {
  LocalDate d = LocalDate.parse(iso);
  String mon = d.getMonth().getDisplayName(TextStyle.SHORT, L.locale());
  if (dateStyle.equals("MDY")) return mon + " " + d.getDayOfMonth();
  if (dateStyle.equals("NUM")) return String.format(Locale.US, "%02d.%02d.%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
  return d.getDayOfMonth() + " " + mon;
 }
 static String from(YearMonth m) { return m.atDay(1).toString(); }
 static String to(YearMonth m) { return m.atEndOfMonth().toString(); }
 static String title(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
 
}
