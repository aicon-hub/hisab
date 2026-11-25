package in.aicon.hisab;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Typed-in exchange rates for travel. Nothing is fetched; the rate is whatever you enter. */
final class TravelFx {
 private TravelFx() {}

 /** A rate such as 83.5, above 0 and at most 1,000,000; null when it is not usable. */
 static BigDecimal parseRate(String s) {
  if (s == null) return null;
  String t = s.trim().replace(",", "");
  if (!t.matches("\\d{1,7}(\\.\\d{1,6})?")) return null;
  BigDecimal r = new BigDecimal(t);
  return r.signum() > 0 && r.compareTo(new BigDecimal("1000000")) <= 0 ? r : null;
 }

 /** Three letters, upper-cased; null otherwise. */
 static String parseCode(String s) {
  if (s == null) return null;
  String t = s.trim().toUpperCase(java.util.Locale.ROOT);
  return t.matches("[A-Z]{3}") ? t : null;
 }

 /** Foreign minor units (cents) to home minor units (paise), rounded to the nearest unit; -1 when it is not usable. */
 static long convert(long foreignMinor, BigDecimal rate) {
  if (foreignMinor <= 0 || rate == null) return -1;
  try { return new BigDecimal(foreignMinor).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact(); } catch (ArithmeticException e) { return -1; }
 }

 /** The line stored in the entry note, e.g. "USD 12.50 @ 83.5". */
 static String noteText(String code, long foreignMinor, BigDecimal rate) { return code + " " + Ledger.plain(foreignMinor) + " @ " + rate.stripTrailingZeros().toPlainString(); }

 /** Sheet for the entry screen: foreign amount, currency and rate in, home amount out. */
 static void ask(MainActivity a, java.util.function.BiConsumer<Long, String> done) {
  Store db = a.db;
  Sheets.form(a, "Foreign amount", new String[]{"Amount in the other currency", "Currency code (like USD)", "Rate: 1 unit = how much of your currency"}, new String[]{"", db.pref("fxCode", ""), db.pref("fxRate", "")}, new int[]{Sheets.NUMBER, Sheets.TEXT, Sheets.NUMBER}, "Convert", v -> {
   long f = Ledger.parse(v[0].trim().isEmpty() ? "x" : v[0].trim()); String code = parseCode(v[1]); BigDecimal rate = parseRate(v[2]);
   long home = convert(f, rate);
   if (home <= 0 || code == null) { a.toast("Check the amount, currency code and rate", null); return; }
   db.setPref("fxCode", code); db.setPref("fxRate", rate.stripTrailingZeros().toPlainString());
   done.accept(home, noteText(code, f, rate));
  }, null);
 }
}
