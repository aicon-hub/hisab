package in.aicon.hisab;

/**
 * Manglish: Roman letters to Malayalam script, the way people type Malayalam on a phone ("paisa" gives പൈസ).
 * Case matters: T D N L R S A E I O U M H are the heavier or longer sounds. Pure Java, no Android types.
 */
final class MlRoman {
 private MlRoman() {}
 private static final String[][] VOW = {
  {"au", "ഔ", "ൗ"}, {"ai", "ഐ", "ൈ"}, {"aa", "ആ", "ാ"}, {"ee", "ഈ", "ീ"}, {"ii", "ഈ", "ീ"}, {"oo", "ഊ", "ൂ"}, {"uu", "ഊ", "ൂ"},
  {"a", "അ", ""}, {"e", "എ", "െ"}, {"i", "ഇ", "ി"}, {"o", "ഒ", "ൊ"}, {"u", "ഉ", "ു"},
  {"A", "ആ", "ാ"}, {"E", "ഏ", "േ"}, {"I", "ഈ", "ീ"}, {"O", "ഓ", "ോ"}, {"U", "ഊ", "ൂ"}};
 private static final String[][] CON = {
  {"ksh", "ക്ഷ"}, {"chh", "ഛ"}, {"ng", "ങ്ങ"}, {"nj", "ഞ"}, {"zh", "ഴ"}, {"kh", "ഖ"}, {"gh", "ഘ"}, {"ch", "ച"}, {"jh", "ഝ"}, {"Th", "ഥ"}, {"th", "ത"}, {"dh", "ധ"}, {"Dh", "ഢ"},
  {"ph", "ഫ"}, {"bh", "ഭ"}, {"sh", "ഷ"}, {"rr", "റ്റ"},
  {"k", "ക"}, {"g", "ഗ"}, {"c", "ച"}, {"j", "ജ"}, {"t", "ട"}, {"T", "ട"}, {"d", "ഡ"}, {"D", "ഡ"}, {"n", "ന"}, {"N", "ണ"}, {"p", "പ"}, {"f", "ഫ"}, {"b", "ബ"}, {"m", "മ"},
  {"y", "യ"}, {"r", "ര"}, {"R", "റ"}, {"l", "ല"}, {"L", "ള"}, {"v", "വ"}, {"w", "വ"}, {"s", "സ"}, {"S", "ശ"}, {"h", "ഹ"}, {"q", "ക"}, {"x", "ക്സ"}, {"z", "സ"}};
 private static final String VIR = "്";

 private static String[] vow(String s, int i) { for (String[] v : VOW) if (s.startsWith(v[0], i)) return v; return null; }
 private static String[] con(String s, int i) { for (String[] c : CON) if (s.startsWith(c[0], i)) return c; return null; }

 static String translit(String s) {
  StringBuilder out = new StringBuilder();
  boolean pending = false;       // output ends with a virama we may still remove
  int kind = 0;                  // 0 start/other, 1 consonant, 2 vowel, 3 mark
  boolean lastA = false;         // last vowel was the plain "a"
  int consStart = -1; boolean consSingle = false; String consKey = ""; int prevKindOfCons = 0; boolean prevA = false;
  int i = 0; boolean forceTa = false;
  while (i < s.length()) {
   String[] v = vow(s, i);
   if (v != null) {
    if (pending) { out.setLength(out.length() - 1); pending = false; }
    if (kind == 1) out.append(v[2]); else out.append(v[1]);
    kind = 2; lastA = v[0].equals("a"); i += v[0].length(); continue;
   }
   char ch = s.charAt(i);
   if (ch == 'M' || ch == 'H') {
    if (pending) { out.setLength(out.length() - 1); pending = false; }
    out.append(ch == 'M' ? "ം" : "ഃ"); kind = 3; i++; continue;
   }
   String[] c = con(s, i);
   if (c != null) {
    String letter = c[1]; if (forceTa && (c[0].equals("d") || c[0].equals("D"))) letter = "ട"; forceTa = false;
    if (c[0].equals("n")) {
     String[] nx = con(s, i + 1);
     if (nx != null) { String f = nx[0]; if (f.equals("k") || f.equals("g")) letter = "ങ"; else if (f.equals("c") || f.equals("ch") || f.equals("j")) letter = "ഞ"; else if (f.equals("t") || f.equals("T") || f.equals("d") || f.equals("D")) { letter = "ണ"; forceTa = true; } }
    }
    prevKindOfCons = kind; prevA = lastA; consStart = out.length(); consSingle = c[0].length() == 1; consKey = c[0];
    out.append(letter).append(VIR); pending = true; kind = 1; i += c[0].length(); continue;
   }
   // any other character ends the word
   if (pending) { finish(out, consStart, consKey, consSingle, prevKindOfCons, prevA); pending = false; }
   out.append(ch); kind = 0; lastA = false; i++;
  }
  if (pending) finish(out, consStart, consKey, consSingle, prevKindOfCons, prevA);
  return out.toString();
 }

 private static void finish(StringBuilder out, int start, String key, boolean single, int prevKind, boolean prevA) {
  if (!single) return;
  String rep = null;
  if (prevKind != 1) { // not the tail of a conjunct
   switch (key) { case "n": rep = "ൻ"; break; case "l": rep = "ൽ"; break; case "L": rep = "ൾ"; break; case "r": rep = "ർ"; break; case "N": rep = "ൺ"; break; default: break; }
  }
  if (rep == null && key.equals("m") && prevKind == 2 && prevA) rep = "ം";
  if (rep != null) { out.setLength(start); out.append(rep); }
 }
}
