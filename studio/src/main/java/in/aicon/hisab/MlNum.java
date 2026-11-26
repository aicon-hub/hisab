package in.aicon.hisab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Numbers to Malayalam words, ported from the Malayalam tables of Indic Tiny TTS (MIT code, by sulabhkatiyar).
 * The voice model only knows Malayalam letters, so digits are spelled out before it sees the text.
 */
final class MlNum {
 private MlNum() {}
 private static final String[] BASE = ("പൂജ്യം ഒന്ന് രണ്ട് മൂന്ന് നാല് അഞ്ച് ആറ് ഏഴ് എട്ട് ഒമ്പത് "
   + "പത്ത് പതിനൊന്ന് പന്ത്രണ്ട് പതിമൂന്ന് പതിനാല് പതിനഞ്ച് പതിനാറ് പതിനേഴ് പതിനെട്ട് പത്തൊമ്പത്").split(" ");
 private static final String[] TENS = "ഇരുപത് മുപ്പത് നാല്പത് അമ്പത് അറുപത് എഴുപത് എൺപത് തൊണ്ണൂറ്".split(" ");
 private static final String[] OBL = "ഇരുപത്തി മുപ്പത്തി നാല്പത്തി അമ്പത്തി അറുപത്തി എഴുപത്തി എൺപത്തി തൊണ്ണൂറ്റി".split(" ");
 private static final String[] H_FINAL = "നൂറ് ഇരുനൂറ് മുന്നൂറ് നാനൂറ് അഞ്ഞൂറ് അറുനൂറ് എഴുനൂറ് എണ്ണൂറ് തൊള്ളായിരം".split(" ");
 private static final String[] H_OBL = "നൂറ്റി ഇരുനൂറ്റി മുന്നൂറ്റി നാനൂറ്റി അഞ്ഞൂറ്റി അറുനൂറ്റി എഴുനൂറ്റി എണ്ണൂറ്റി തൊള്ളായിരത്തി".split(" ");
 private static final String[] UNITS = new String[100];
 static {
  for (int i = 0; i < 20; i++) UNITS[i] = BASE[i];
  for (int t = 0; t < 8; t++) {
   UNITS[20 + 10 * t] = TENS[t];
   for (int u = 1; u < 10; u++) UNITS[20 + 10 * t + u] = join(OBL[t], BASE[u]);
  }
 }
 private static String join(String oblique, String unit) {
  char c = unit.charAt(0); String sign = c == 'അ' ? "" : c == 'ആ' ? "ാ" : c == 'എ' ? "െ" : c == 'ഏ' ? "േ" : c == 'ഒ' ? "ൊ" : null;
  return sign == null ? oblique + unit : oblique + "യ" + sign + unit.substring(1);
 }

 private static String thousands(int v, boolean last) {
  if (v == 1) return last ? "ആയിരം" : "ആയിരത്തി";
  if (v == 5) return last ? "അയ്യായിരം" : "അയ്യായിരത്തി";
  String w = UNITS[v]; w = w.substring(0, w.length() - 1);
  return w + (last ? "ായിരം" : "ായിരത്തി");
 }

 static String cardinal(long n) {
  if (n < 100) return UNITS[(int) n];
  long crore = n / 10000000L, rest = n % 10000000L, lakh = rest / 100000, r2 = rest % 100000, th = r2 / 1000, r3 = r2 % 1000, h = r3 / 100, u = r3 % 100;
  long[] v = {crore, lakh, th, h, u}; StringBuilder b = new StringBuilder();
  for (int i = 0; i < 5; i++) {
   if (v[i] == 0) continue;
   boolean last = true; for (int j = i + 1; j < 5; j++) if (v[j] != 0) last = false;
   String w;
   if (i == 0) w = (crore == 1 ? "ഒരു" : cardinal(crore)) + " കോടി";
   else if (i == 1) w = (lakh == 1 ? "ഒരു" : UNITS[(int) lakh]) + " " + (last ? "ലക്ഷം" : "ലക്ഷത്തി");
   else if (i == 2) w = thousands((int) th, last);
   else if (i == 3) w = last ? H_FINAL[(int) h - 1] : H_OBL[(int) h - 1];
   else w = UNITS[(int) u];
   if (b.length() > 0) b.append(' ');
   b.append(w);
  }
  return b.toString();
 }

 private static final Pattern NUM = Pattern.compile("(\\d+)(?:[.,](\\d+))?");
 /** Replaces every number in the text with Malayalam words. Decimals read the fraction digit by digit. */
 static String spell(String text) {
  Matcher m = NUM.matcher(text); StringBuffer out = new StringBuffer();
  while (m.find()) {
   String ip = m.group(1), fp = m.group(2); String w;
   if (ip.length() > 12) { StringBuilder d = new StringBuilder(); for (char ch : ip.toCharArray()) d.append(UNITS[ch - '0']).append(' '); w = d.toString().trim(); }
   else w = cardinal(Long.parseLong(ip));
   if (fp != null) { StringBuilder d = new StringBuilder(w).append(" പോയിന്റ്"); for (char ch : fp.toCharArray()) d.append(' ').append(UNITS[ch - '0']); w = d.toString(); }
   m.appendReplacement(out, Matcher.quoteReplacement(" " + w + " "));
  }
  m.appendTail(out); return out.toString().replaceAll("\\s+", " ").trim();
 }
}
