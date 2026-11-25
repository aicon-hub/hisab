package in.aicon.hisab;

import java.math.BigDecimal;

final class Ledger {
 private Ledger() {}
 /** Parses a plain number such as 40 or 40.5 into minor units. Returns -1 when invalid. */
 static long parse(String s) {
  try { if (s == null) return -1; StringBuilder nb = new StringBuilder(s); for (int i = 0; i < nb.length(); i++) { char ch = nb.charAt(i); if (ch >= '\u0D66' && ch <= '\u0D6F') nb.setCharAt(i, (char) ('0' + ch - '\u0D66')); } s = nb.toString(); if (!s.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")) return -1; long n = new BigDecimal(s).movePointRight(2).longValueExact(); return n > 0 && n < 999999999999L ? n : -1; } catch (Exception e) { return -1; }
 }
 static String plain(long m) { return m % 100 == 0 ? "" + m / 100 : String.format(java.util.Locale.US, "%d.%02d", m / 100, m % 100); }
 static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
}
