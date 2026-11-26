package in.aicon.hisab;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Rule-based "Ask Hisab" brain. No model, no network. It reads English, Malayalam and Manglish,
 * answers from the user's own entries and prepares changes that the user must confirm.
 */
final class ChatBrain {
 private ChatBrain() { }

 static final class Reply {
  String text; Store.Tx tx; String act = ""; // act: "", "add", "delete", "edit"
  /** Real run record: each step is {node, text, microseconds since start}. Nodes: 0 question, 1 language, 2 intent, 3 period, 4 match, 5 read entries, 6 read lists, 7 filter, 8 check, 9 sources, 10 answer. */
  final List<Object[]> steps = new ArrayList<>();
  /** The entries the answer was computed from. */
  List<Store.Tx> sources = new ArrayList<>();
  String intent = "", period = ""; int read; long t0 = System.nanoTime();
 }

 private static final String[][] MONTHS = {
  {"january", "jan", "ജനുവരി"}, {"february", "feb", "ഫെബ്രുവരി"}, {"march", "mar", "മാർച്ച്"}, {"april", "apr", "ഏപ്രിൽ"},
  {"may", "may", "മെയ്"}, {"june", "jun", "ജൂൺ"}, {"july", "jul", "ജൂലൈ"}, {"august", "aug", "ഓഗസ്റ്റ്"},
  {"september", "sep", "സെപ്റ്റംബർ"}, {"october", "oct", "ഒക്ടോബർ"}, {"november", "nov", "നവംബർ"}, {"december", "dec", "ഡിസംബർ"}};
 // group key (matched against category names) then spoken variants
 private static final String[][] SYN = {
  {"food", "food", "eat", "ate", "meal", "lunch", "dinner", "breakfast", "hotel", "restaurant", "snack", "tea", "coffee", "chaya", "chaaya", "bhakshanam", "bhakshan", "khana", "ഭക്ഷണ", "ചായ", "ഹോട്ടൽ", "കാപ്പി"},
  {"travel", "travel", "trip", "yatra", "യാത്ര"},
  {"transport", "transport", "auto", "bus", "taxi", "cab", "fuel", "petrol", "diesel", "ഓട്ടോ", "ബസ്", "പെട്രോൾ"},
  {"grocer", "grocery", "groceries", "vegetable", "milk", "supermarket", "palachaka", "പലചരക്ക്", "പാൽ"},
  {"shop", "shopping", "clothes", "dress", "ഷോപ്പിംഗ്", "തുണി"},
  {"bill", "bill", "bills", "electricity", "current", "water", "recharge", "wifi", "internet", "ബിൽ", "കറണ്ട്", "റീചാർജ്"},
  {"rent", "rent", "vaadaka", "vadaka", "വാടക"},
  {"health", "health", "medicine", "medical", "doctor", "hospital", "marunnu", "മരുന്ന്", "ആശുപത്രി"},
  {"entertain", "entertainment", "movie", "cinema", "film", "സിനിമ"},
  {"salary", "salary", "shambalam", "sambalam", "ശമ്പള"}};

 private static Reply cur;
 private static void step(int node, String text) { Reply r = cur; if (r != null) r.steps.add(new Object[]{node, text, (System.nanoTime() - r.t0) / 1000}); }
 private static void intent(Reply r, String what) { r.intent = what; step(2, "Understood: " + what); }
 // ------------------------------------------------------------------ helpers
 private static boolean ml(String s) { for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 0x0D00 && s.charAt(i) <= 0x0D7F) return true; return false; }
 private static boolean has(String low, String... vs) {
  for (String v : vs) {
   if (v.isEmpty()) continue;
   if (ml(v)) { if (low.contains(v)) return true; }
   else if (Pattern.compile("(?<![a-z0-9])" + Pattern.quote(v)).matcher(low).find()) return true;
  }
  return false;
 }
 private static String digits(String s) {
  StringBuilder b = new StringBuilder(s);
  for (int i = 0; i < b.length(); i++) { char ch = b.charAt(i); if (ch >= '\u0D66' && ch <= '\u0D6F') b.setCharAt(i, (char) ('0' + ch - '\u0D66')); }
  return b.toString();
 }
 private static long amountIn(String s) {
  java.util.regex.Matcher m = Pattern.compile("(?<![a-zA-Z0-9.-])([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?![0-9])").matcher(s);
  return m.find() ? Ledger.parse(m.group(1).replace(",", "")) : 0;
 }
 private static String t(boolean m, String en, String mal) { return m ? mal : en; }

 private static final class Span { LocalDate from, to; String en, ml; boolean explicit; }

 private static Span span(String low, LocalDate today) {
  Span s = new Span(); YearMonth ym = YearMonth.from(today);
  if (has(low, "yesterday", "innale", "ഇന്നലെ")) { s.from = s.to = today.minusDays(1); s.en = "yesterday"; s.ml = "ഇന്നലെ"; s.explicit = true; }
  else if (has(low, "today", "innu", "ഇന്ന്") && !has(low, "ഇന്ന് വരെ")) { s.from = s.to = today; s.en = "today"; s.ml = "ഇന്ന്"; s.explicit = true; }
  else if (has(low, "last week", "previous week", "kazhinja aazhcha", "kazhinja week", "കഴിഞ്ഞ ആഴ്ച")) { LocalDate mon = today.minusDays(today.getDayOfWeek().getValue() - 1).minusDays(7); s.from = mon; s.to = mon.plusDays(6); s.en = "last week"; s.ml = "കഴിഞ്ഞ ആഴ്ച"; s.explicit = true; }
  else if (has(low, "this week", "week", "ee aazhcha", "aazhcha", "ഈ ആഴ്ച", "ആഴ്ച")) { LocalDate mon = today.minusDays(today.getDayOfWeek().getValue() - 1); s.from = mon; s.to = today; s.en = "this week"; s.ml = "ഈ ആഴ്ച"; s.explicit = true; }
  else if (has(low, "last month", "previous month", "kazhinja maasam", "kazhinja masam", "kazhinja month", "കഴിഞ്ഞ മാസം")) { YearMonth p = ym.minusMonths(1); s.from = p.atDay(1); s.to = p.atEndOfMonth(); s.en = "last month (" + Fmt.month(p) + ")"; s.ml = "കഴിഞ്ഞ മാസം"; s.explicit = true; }
  else if (has(low, "last year", "previous year", "kazhinja varsham", "കഴിഞ്ഞ വർഷം")) { int y = today.getYear() - 1; s.from = LocalDate.of(y, 1, 1); s.to = LocalDate.of(y, 12, 31); s.en = "last year"; s.ml = "കഴിഞ്ഞ വർഷം"; s.explicit = true; }
  else if (has(low, "this year", "year", "varsham", "ee varsham", "വർഷം")) { s.from = LocalDate.of(today.getYear(), 1, 1); s.to = today; s.en = "this year"; s.ml = "ഈ വർഷം"; s.explicit = true; }
  else if (has(low, "all time", "overall", "ever", "total till now", "ഇതുവരെ", "ithuvare", "aake")) { s.from = LocalDate.of(2000, 1, 1); s.to = today; s.en = "so far"; s.ml = "ഇതുവരെ"; s.explicit = true; }
  else if (has(low, "last 7 days", "past week", "last seven days")) { s.from = today.minusDays(6); s.to = today; s.en = "the last 7 days"; s.ml = "കഴിഞ്ഞ 7 ദിവസം"; s.explicit = true; }
  else if (has(low, "last 30 days", "past month")) { s.from = today.minusDays(29); s.to = today; s.en = "the last 30 days"; s.ml = "കഴിഞ്ഞ 30 ദിവസം"; s.explicit = true; }
  else {
   for (int i = 0; i < 12; i++) {
    if (has(low, MONTHS[i][0], MONTHS[i][1].length() > 3 ? "" : (MONTHS[i][1].equals("may") ? "" : MONTHS[i][1] + " "), MONTHS[i][2]) || (low.endsWith(" " + MONTHS[i][1]) && !MONTHS[i][1].equals("may"))) {
     int y = i + 1 > today.getMonthValue() ? today.getYear() - 1 : today.getYear(); YearMonth p = YearMonth.of(y, i + 1);
     s.from = p.atDay(1); s.to = p.atEndOfMonth(); s.en = Fmt.month(p); s.ml = MONTHS[i][2] + " " + y; s.explicit = true; return s;
    }
   }
   if (has(low, "this month", "ee maasam", "ee masam", "month", "maasam", "masam", "ഈ മാസം", "മാസം")) s.explicit = true;
   s.from = ym.atDay(1); s.to = today; s.en = "this month"; s.ml = "ഈ മാസം";
  }
  return s;
 }

 private static Store.Cat category(Store db, String low, String kind) { return category(db, low, kind, true); }
 private static Store.Cat category(Store db, String low, String kind, boolean syn) {
  List<Store.Cat> cats = db.categories(kind);
  // direct name
  Store.Cat best = null;
  for (Store.Cat c : cats) {
   String n = c.name == null ? "" : c.name.toLowerCase(Locale.ROOT);
   if (n.length() < 2) continue;
   if (has(low, n) || (ml(n) && low.contains(n))) { if (best == null || n.length() > best.name.length()) best = c; }
  }
  if (best != null) return best;
  // transliterated words
  for (String w : low.split("[^\\p{L}0-9]+")) {
   if (w.length() < 3 || ml(w)) continue;
   String m = MlRoman.translit(w);
   for (Store.Cat c : cats) if (c.name != null && ml(c.name) && (c.name.equals(m) || c.name.startsWith(m) && m.length() >= 2)) return c;
  }
  if (!syn) return null;
  // synonyms
  for (String[] g : SYN) {
   String[] vs = Arrays.copyOfRange(g, 1, g.length);
   if (has(low, vs)) for (Store.Cat c : cats) if (c.name != null && c.name.toLowerCase(Locale.ROOT).contains(g[0])) return c;
  }
  return null;
 }

 private static Store.Person person(Store db, String low) {
  Store.Person best = null;
  for (Store.Person p : db.people()) {
   String n = p.name == null ? "" : p.name.toLowerCase(Locale.ROOT).trim();
   if (n.length() < 2) continue;
   String first = n.split("\\s+")[0];
   boolean hit = has(low, n) || (first.length() >= 3 && has(low, first));
   if (!hit && !ml(n)) { for (String w : low.split("[^\\p{L}0-9]+")) if (w.length() >= 3 && ml(MlRoman.translit(w)) && false) hit = true; }
   if (!hit && ml(n)) { for (String w : low.split("[^\\p{L}0-9]+")) if (w.length() >= 3 && !ml(w) && MlRoman.translit(w).equals(n.split("\\s+")[0])) hit = true; }
   if (hit && (best == null || n.length() > best.name.length())) best = p;
  }
  return best;
 }

 private static long sum(List<Store.Tx> l, String kind, long cat, String noteKey) {
  long s = 0;
  for (Store.Tx x : l) if (kind.equals(x.kind) && (cat == 0 || x.cat == cat) && (noteKey == null || (x.note != null && x.note.toLowerCase(Locale.ROOT).contains(noteKey)))) s += x.amount;
  return s;
 }

 private static final Set<String> STOP = new HashSet<>(Arrays.asList(
  "how", "much", "many", "did", "i", "my", "me", "the", "a", "an", "on", "for", "in", "of", "is", "are", "was", "were", "to", "and", "this", "last", "month", "week", "year", "today",
  "yesterday", "spend", "spent", "expense", "expenses", "income", "total", "show", "tell", "what", "whats", "have", "had", "all", "by", "at", "with", "from", "do", "does", "so", "far",
  "ethra", "aayi", "aayirunnu", "ente", "njan", "ee", "kazhinja", "innu", "innale", "maasam", "masam", "chilavu", "selavu", "kharch", "varavu", "ennu", "paranju", "tharu", "kaanikku",
  "november", "december", "october", "september", "august", "july", "june", "may", "april", "march", "february", "january", "category", "money", "rupees", "rs", "inr", "please", "pls", "can", "you", "it", "that", "than"));

 private static String noteKey(String low, Store db, List<Store.Tx> pool) {
  for (String w : low.split("[^\\p{L}0-9]+")) {
   if (w.length() < 3 || STOP.contains(w) || w.matches("[0-9]+")) continue;
   String m = ml(w) ? w : null;
   for (Store.Tx x : pool) {
    if (x.note == null) continue; String n = x.note.toLowerCase(Locale.ROOT);
    if (n.contains(w) || (m == null && n.contains(MlRoman.translit(w)) && MlRoman.translit(w).length() >= 2)) return n.contains(w) ? w : MlRoman.translit(w);
   }
  }
  return null;
 }

 static String line(Store db, Store.Tx x, Map<Long, Store.Cat> cm) {
  Store.Cat c = cm.get(x.cat);
  String sign = "income".equals(x.kind) ? "+" : "expense".equals(x.kind) ? "-" : "";
  return Fmt.shortDate(x.day) + "  " + sign + Fmt.money(x.amount) + "  " + (x.note == null || x.note.isEmpty() ? (c == null ? x.kind : c.name) : x.note) + (c != null && x.note != null && !x.note.isEmpty() ? " (" + c.name + ")" : "");
 }

 // ------------------------------------------------------------------ main entry
 static Reply ask(Store db, String raw, LocalDate today) {
  Reply r = new Reply(); cur = r;
  String shown = raw == null ? "" : raw.trim(); if (shown.length() > 40) shown = shown.substring(0, 40) + "...";
  step(0, "Question read: \"" + shown + "\"");
  try { run(db, raw, today, r);
  if (r.intent.isEmpty()) step(2, "Understood: no known question");
  boolean mlq = ml(raw == null ? "" : raw);
  boolean roman = !mlq && raw != null && raw.matches("(?s).*\\b(ethra|chilavu|maasam|innu|innale|kadam|baki|selavu|paisa|ente|aayi)\\b.*");
  step(1, "Language: " + (mlq ? "Malayalam" : roman ? "Manglish" : "English"));
  step(3, "Period: " + (r.period.isEmpty() ? "none needed" : r.period));
  step(6, "Read lists: " + db.categories(null).size() + " categories, " + db.people().size() + " people, " + db.accounts().size() + " accounts");
  step(7, "Filter: kept " + r.sources.size() + (r.read > 0 ? " of " + r.read : "") + " entries");
  step(8, r.act.isEmpty() ? "Checked the totals over " + r.sources.size() + " entries" : "Checked: nothing is saved until you confirm");
  step(9, r.sources.isEmpty() ? "Sources: none needed" : "Sources: " + r.sources.size() + " entries listed");
  step(10, r.act.isEmpty() ? "Answer ready" : "Confirm card ready");
  } finally { cur = null; }
  return r;
 }
 private static Reply run(Store db, String raw, LocalDate today, Reply r) {
  String text = raw == null ? "" : digits(raw.trim());
  boolean m = ml(text) || L.isMl();
  String low = text.toLowerCase(Locale.ROOT).replaceAll("[\\u200c\\u200d]", "");
  if (low.isEmpty()) { r.text = help(m); return r; }
  Map<Long, Store.Cat> cm = db.categoryMap();
  boolean question = low.contains("?") || has(low, "how", "what", "who", "which", "when", "where", "show", "tell", "list", "total", "much", "many", "ethra", "entha", "enthu", "aaru", "aaranu", "evide", "kaanikku", "kaanikkam", "parayu", "paranju", "tharu", "എത്ര", "എന്ത", "ആര", "എവിടെ", "കാണിക്ക", "പറയ", "ഏതാണ്", "ഏത്");
  boolean last = has(low, "last", "latest", "previous", "recent", "avasana", "avasaanathe", "അവസാന", "അവസാനത്തെ", "ഒടുവില");
  long num = amountIn(low);

  if (has(low, "help", "what can you do", "commands", "examples", "sahayam", "സഹായം") && !has(low, "spent", "spend")) { r.text = help(m); return r; }
  if (low.matches("^(hi|hello|hey|hai|namaste|namaskaram|നമസ്കാരം|ഹായ്|ഹലോ)\\W*$")) { r.text = t(m, "Hi! Ask me about your money, or type an entry like \"tea 20\". Say \"help\" for examples.", "ഹായ്! നിങ്ങളുടെ പണത്തെക്കുറിച്ച് ചോദിക്കൂ, അല്ലെങ്കിൽ \"ചായ 20\" പോലെ എഴുതൂ. ഉദാഹരണങ്ങൾക്ക് \"help\"."); return r; }

  // ---- delete the last entry
  if (has(low, "delete", "remove", "undo", "cancel", "erase", "kalayu", "kalayuka", "ozhivakku", "ozhivaakku", "ഒഴിവാക്ക", "കളയ", "ഡിലീറ്റ്", "നീക്ക") && last) {
   List<Store.Tx> rc = db.recent(1);
   if (rc.isEmpty()) { r.text = t(m, "There are no entries to delete.", "ഇല്ലാതാക്കാൻ എൻട്രികളൊന്നുമില്ല."); return r; }
   intent(r, "delete the last entry"); step(5, "Read the last entry"); r.sources = new ArrayList<>(rc); r.tx = rc.get(0); r.act = "delete";
   r.text = t(m, "Delete this entry?\n", "ഈ എൻട്രി ഇല്ലാതാക്കട്ടെ?\n") + line(db, r.tx, cm); return r;
  }
  // ---- change the last entry's amount
  if (has(low, "change", "edit", "update", "correct", "fix", "make it", "maattu", "maattuka", "thiruthu", "മാറ്റ", "തിരുത്ത") && last && num > 0) {
   List<Store.Tx> rc = db.recent(1);
   if (rc.isEmpty()) { r.text = t(m, "There is no entry to change.", "മാറ്റാൻ എൻട്രിയില്ല."); return r; }
   intent(r, "change the last entry"); step(5, "Read the last entry"); r.sources = new ArrayList<>(rc); Store.Tx x = rc.get(0); Store.Tx n = db.txById(x.id); n.amount = num; r.tx = n; r.act = "edit";
   r.text = t(m, "Change the amount of the last entry?\n", "അവസാന എൻട്രിയുടെ തുക മാറ്റട്ടെ?\n") + line(db, x, cm) + "\n→ " + Fmt.money(num); return r;
  }

  // ---- add an entry: an amount without a question
  boolean addWord = has(low, "add", "enter", "save", "record", "log", "note down", "chertho", "cherkku", "cherkkuka", "ezhuthu", "ചേർക്ക", "എഴുത", "രേഖപ്പെടുത്ത");
  if (num > 0 && (addWord || (!question && !last && !has(low, "entries", "history", "transactions", "top", "biggest"))) && !has(low, "owe", "balance")) {
   String entry = text.replaceAll("(?i)^(please |pls )?(add|enter|save|record|log|note down|chertho|cherkku|cherkkuka|ezhuthu)\\b[: ]*", "").replaceAll("^(ചേർക്കുക|ചേർക്കൂ|എഴുതുക|എഴുതൂ)[: ]*", "").replaceAll("(?i)\\b(an? )?(entry|expense)( of| for)?\\b", " ").trim();
   intent(r, "add an entry");
   Store.Tx x = HomePage.parse(db, entry);
   if (x == null) x = HomePage.parse(db, text);
   if (x != null) {
    step(4, "Read amount " + Fmt.money(x.amount) + ", date " + x.day + ", note \"" + x.note + "\""); step(5, "Matched category and account");
    // Manglish words in the note: show them in Malayalam when the app is in Malayalam
    if ("expense".equals(x.kind) && has(low, "salary", "shambalam", "sambalam", "income", "received", "ശമ്പള", "വരുമാന", "കിട്ടി")) { x.kind = "income"; x.cat = 0; for (Store.Cat ic : db.categories("income")) if (x.cat == 0 || ic.name.toLowerCase(Locale.ROOT).contains("salary") && (has(low, "salary", "shambalam", "sambalam", "ശമ്പള"))) x.cat = ic.id; }
    r.tx = x; r.act = "add";
    Store.Cat c = cm.get(x.cat);
    r.text = t(m, "Add this?\n", "ഇത് ചേർക്കട്ടെ?\n") + ("income".equals(x.kind) ? "+" : "-") + Fmt.money(x.amount) + "  " + x.note + (c == null ? "" : " (" + c.name + ")") + "\n" + Fmt.shortDate(x.day);
    return r;
   }
  }

  Span sp = span(low, today);
  String from = sp.from.toString(), to = sp.to.toString();
  String when = m ? sp.ml : sp.en;
  List<Store.Tx> tx = db.txs(from, to);
  step(5, "Read " + tx.size() + " entries, " + from + " to " + to); r.read = tx.size(); r.period = sp.en;
  Store.Person pr = person(db, low);
  if (pr != null) step(4, "Found person: " + pr.name);

  // ---- who owes / lent / borrowed
  if (has(low, "owe", "owes", "owed", "lent", "borrow", "kadam", "kadamayi", "tharanund", "tharaanund", "kittanund", "kittaanund", "baki tharan", "കടം", "തരാനുണ്ട്", "കിട്ടാനുണ്ട്", "വായ്പ")) {
   intent(r, "who owes whom");
   if (pr != null) {
    long n = db.net(pr.id);
    r.text = n > 0 ? t(m, pr.name + " owes you " + Fmt.money(n) + ".", pr.name + " നിങ്ങൾക്ക് " + Fmt.money(n) + " തരാനുണ്ട്.")
     : n < 0 ? t(m, "You owe " + pr.name + " " + Fmt.money(-n) + ".", "നിങ്ങൾ " + pr.name + "ന് " + Fmt.money(-n) + " കൊടുക്കാനുണ്ട്.") : t(m, "You and " + pr.name + " are settled.", pr.name + "മായി കണക്ക് തീർന്നു.");
    return r;
   }
   StringBuilder b = new StringBuilder(); long toGet = 0, toGive = 0;
   for (Store.Person p : db.people()) { long n = db.net(p.id); if (n == 0) continue; if (n > 0) toGet += n; else toGive -= n; b.append("\n").append(p.name).append(": ").append(n > 0 ? t(m, "owes you ", "തരാനുണ്ട് ") : t(m, "you owe ", "കൊടുക്കാനുണ്ട് ")).append(Fmt.money(Math.abs(n))); }
   r.text = b.length() == 0 ? t(m, "Nobody owes anything right now.", "ഇപ്പോൾ ആർക്കും ആരോടും കടമില്ല.")
    : t(m, "To get back " + Fmt.money(toGet) + ", to give " + Fmt.money(toGive) + ".", "തിരികെ കിട്ടാൻ " + Fmt.money(toGet) + ", കൊടുക്കാൻ " + Fmt.money(toGive) + ".") + b;
   return r;
  }
  // ---- budgets
  if (has(low, "budget", "limit", "ബജറ്റ്")) {
   intent(r, "budgets");
   YearMonth ym = YearMonth.from(today); StringBuilder b = new StringBuilder();
   for (Map.Entry<Long, Long> e : db.budgets().entrySet()) {
    Store.Cat c = cm.get(e.getKey()); if (c == null) continue;
    long lim = db.effectiveLimit(e.getKey(), ym), sp2 = db.spentIn(e.getKey(), ym.atDay(1).toString(), ym.atEndOfMonth().toString());
    b.append("\n").append(c.name).append(": ").append(Fmt.money(sp2)).append(" / ").append(Fmt.money(lim)).append(sp2 > lim ? t(m, "  over", "  കവിഞ്ഞു") : "");
   }
   r.text = b.length() == 0 ? t(m, "No budgets set yet.", "ബജറ്റുകളൊന്നും സജ്ജമാക്കിയിട്ടില്ല.") : t(m, "Budgets this month:", "ഈ മാസത്തെ ബജറ്റ്:") + b; return r;
  }
  // ---- compare this vs last month
  if (has(low, "compare", "versus", "vs", "than last", "more than", "less than", "thaarathamyam", "താരതമ്യം")) {
   intent(r, "compare with last month");
   YearMonth ym = YearMonth.from(today), pm = ym.minusMonths(1);
   long a1 = sum(db.txs(ym.atDay(1).toString(), today.toString()), "expense", 0, null), a0 = sum(db.txs(pm.atDay(1).toString(), pm.atEndOfMonth().toString()), "expense", 0, null);
   long d = a1 - a0;
   r.text = t(m, "Spent " + Fmt.money(a1) + " this month and " + Fmt.money(a0) + " in " + Fmt.month(pm) + ". That is " + Fmt.money(Math.abs(d)) + (d > 0 ? " more." : d < 0 ? " less." : ", the same."),
    "ഈ മാസം " + Fmt.money(a1) + ", കഴിഞ്ഞ മാസം " + Fmt.money(a0) + ". വ്യത്യാസം " + Fmt.money(Math.abs(d)) + (d > 0 ? " കൂടുതൽ." : d < 0 ? " കുറവ്." : "."));
   return r;
  }
  // ---- average
  if (has(low, "average", "avg", "per day", "daily", "a day", "ശരാശരി", "oru divasam", "divasam")) {
   intent(r, "daily average");
   long tot = sum(tx, "expense", 0, null); for (Store.Tx x : tx) if ("expense".equals(x.kind)) r.sources.add(x); long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(sp.from, sp.to) + 1);
   r.text = t(m, "About " + Fmt.money(tot / days) + " a day " + (sp.explicit ? when : "this month") + " (" + Fmt.money(tot) + " over " + days + " days).", "ഒരു ദിവസം ഏകദേശം " + Fmt.money(tot / days) + " (" + days + " ദിവസത്തിൽ " + Fmt.money(tot) + ")."); return r;
  }
  // ---- top categories / biggest expenses
  boolean top = has(low, "top", "biggest", "largest", "highest", "most", "maximum", "max", "ettavum", "ettavum kooduthal", "ഏറ്റവും", "കൂടുതൽ", "kooduthal");
  if (top || has(low, "where did", "where is", "breakdown", "evide poyi", "എവിടെ")) {
   intent(r, "top categories or biggest expenses");
   if (has(low, "categor", "where", "breakdown", "evide", "വിഭാഗ", "എവിടെ") || !has(low, "expense", "entry", "spend")) {
    Map<Long, Long> by = new HashMap<>(); long tot = 0;
    for (Store.Tx x : tx) if ("expense".equals(x.kind)) { by.merge(x.cat, x.amount, Long::sum); tot += x.amount; r.sources.add(x); }
    List<Map.Entry<Long, Long>> es = new ArrayList<>(by.entrySet()); es.sort((p, q) -> Long.compare(q.getValue(), p.getValue()));
    if (es.isEmpty()) { r.text = t(m, "No expenses " + when + ".", when + " ചെലവുകളൊന്നുമില്ല."); return r; }
    StringBuilder b = new StringBuilder(t(m, "Where the money went " + when + " (" + Fmt.money(tot) + "):", when + " പണം പോയത് (" + Fmt.money(tot) + "):"));
    for (int i = 0; i < Math.min(5, es.size()); i++) { Store.Cat c = cm.get(es.get(i).getKey()); b.append("\n").append(c == null ? t(m, "No category", "വിഭാഗമില്ല") : c.name).append(": ").append(Fmt.money(es.get(i).getValue())).append("  ").append(tot == 0 ? 0 : es.get(i).getValue() * 100 / tot).append("%"); }
    r.text = b.toString(); return r;
   }
   List<Store.Tx> ex = new ArrayList<>(); for (Store.Tx x : tx) if ("expense".equals(x.kind)) ex.add(x);
   ex.sort((p, q) -> Long.compare(q.amount, p.amount)); r.sources.addAll(ex.subList(0, Math.min(5, ex.size())));
   if (ex.isEmpty()) { r.text = t(m, "No expenses " + when + ".", when + " ചെലവുകളൊന്നുമില്ല."); return r; }
   StringBuilder b = new StringBuilder(t(m, "Biggest expenses " + when + ":", when + " ഏറ്റവും വലിയ ചെലവുകൾ:"));
   for (int i = 0; i < Math.min(5, ex.size()); i++) b.append("\n").append(line(db, ex.get(i), cm));
   r.text = b.toString(); return r;
  }
  // ---- recent / last entries
  if (last && !has(low, "month", "week", "year", "maasam", "aazhcha", "മാസം", "ആഴ്ച") || has(low, "recent", "latest", "history", "entries", "transactions", "പുതിയ")) {
   intent(r, "recent entries");
   int n = num > 0 && num <= 20 ? (int) (num / 100) : 5; if (n <= 0 || n > 20) n = 5;
   String ds = digits(low); java.util.regex.Matcher mm = Pattern.compile("\\b([0-9]{1,2})\\b").matcher(ds); if (mm.find()) n = Math.max(1, Math.min(20, Integer.parseInt(mm.group(1))));
   List<Store.Tx> rc = db.recent(n); r.sources.addAll(rc); step(5, "Read the last " + rc.size() + " entries");
   if (rc.isEmpty()) { r.text = t(m, "No entries yet.", "എൻട്രികളൊന്നുമില്ല."); return r; }
   StringBuilder b = new StringBuilder(t(m, "Last " + rc.size() + " entries:", "അവസാന " + rc.size() + " എൻട്രികൾ:"));
   for (Store.Tx x : rc) b.append("\n").append(line(db, x, cm)); r.text = b.toString(); return r;
  }
  // ---- how many entries
  if (has(low, "how many", "count", "number of", "ethra ennam", "എത്ര എണ്ണം", "എണ്ണം")) {
   intent(r, "count entries");
   r.text = t(m, tx.size() + " entries " + when + ".", when + " " + tx.size() + " എൻട്രികൾ."); return r;
  }
  // ---- account balances
  if (has(low, "balance", "account", "accounts", "cash", "wallet", "bank", "how much money", "net worth", "ബാലൻസ്", "അക്കൗണ്ട്", "ബാങ്ക്", "കയ്യിൽ", "kayyil", "kaiyil") && !has(low, "spent", "spend", "left", "baki", "ബാക്കി")) {
   intent(r, "account balances");
   long total = 0; StringBuilder b = new StringBuilder(); List<Store.Acc> hit = new ArrayList<>();
   for (Store.Acc a : db.accounts()) { long bal = db.balance(a); total += bal; String n = a.name == null ? "" : a.name.toLowerCase(Locale.ROOT); if (n.length() >= 2 && has(low, n)) hit.add(a); b.append("\n").append(a.name).append(": ").append(Fmt.money(bal)); }
   if (hit.size() == 1) { r.text = t(m, hit.get(0).name + " has " + Fmt.money(db.balance(hit.get(0))) + ".", hit.get(0).name + "ൽ " + Fmt.money(db.balance(hit.get(0))) + " ഉണ്ട്."); return r; }
   r.text = t(m, "You have " + Fmt.money(total) + " across your accounts:", "എല്ലാ അക്കൗണ്ടിലുമായി " + Fmt.money(total) + " ഉണ്ട്:") + b; return r;
  }
  // ---- net / left / savings
  if (has(low, "left", "remaining", "net", "saving", "savings", "saved", "surplus", "baki", "baaki", "migham", "ബാക്കി", "മിച്ചം", "സമ്പാദ്യം")) {
   intent(r, "money left");
   long inc = sum(tx, "income", 0, null), exp = sum(tx, "expense", 0, null); for (Store.Tx x : tx) if ("income".equals(x.kind) || "expense".equals(x.kind)) r.sources.add(x);
   r.text = t(m, "Income " + Fmt.money(inc) + ", spent " + Fmt.money(exp) + " " + when + ". Left: " + Fmt.money(inc - exp) + ".", when + " വരവ് " + Fmt.money(inc) + ", ചെലവ് " + Fmt.money(exp) + ". ബാക്കി: " + Fmt.money(inc - exp) + "."); return r;
  }
  // ---- income
  if (has(low, "income", "earn", "earned", "salary", "received", "paid me", "varavu", "varumanam", "sambalam", "shambalam", "വരവ്", "വരുമാന", "ശമ്പള", "കിട്ടി")) {
   intent(r, "income");
   Store.Cat c = category(db, low, "income"); long v = sum(tx, "income", c == null ? 0 : c.id, null);
   for (Store.Tx x : tx) if ("income".equals(x.kind) && (c == null || x.cat == c.id)) r.sources.add(x);
   r.text = t(m, "Income " + when + (c == null ? "" : " from " + c.name) + ": " + Fmt.money(v) + ".", when + " വരവ്" + (c == null ? "" : " (" + c.name + ")") + ": " + Fmt.money(v) + "."); return r;
  }
  // ---- spending (default), with optional category / person / note filter
  boolean spendWord = has(low, "spend", "spent", "spending", "expense", "expenses", "cost", "paid", "pay", "bought", "kharch", "chilavu", "chelavu", "selavu", "chilav", "ചെലവ്", "ചിലവ്", "ചെലവായ", "ചിലവായ", "ചെലവഴി", "kodutthu", "koduthu", "കൊടുത്ത");
  Store.Cat c = category(db, low, "expense", false);
  String key = c == null ? noteKey(low, db, tx) : null;
  if (c == null && key == null) c = category(db, low, "expense", true);
  if (spendWord || c != null || key != null || sp.explicit || question) {
   intent(r, "spending" + (c != null ? " on " + c.name : key != null ? " on \"" + key + "\"" : "") + ", " + sp.en); if (c != null || key != null) step(4, "Matched " + (c != null ? "category " + c.name : "note word \"" + key + "\""));
   long v = sum(tx, "expense", c == null ? 0 : c.id, key); int cnt = 0;
   for (Store.Tx x : tx) if ("expense".equals(x.kind) && (c == null || x.cat == c.id) && (key == null || (x.note != null && x.note.toLowerCase(Locale.ROOT).contains(key)))) cnt++;
   String what = c != null ? c.name : key != null ? "\"" + key + "\"" : "";
   for (Store.Tx x : tx) if ("expense".equals(x.kind) && (c == null || x.cat == c.id) && (key == null || (x.note != null && x.note.toLowerCase(Locale.ROOT).contains(key)))) r.sources.add(x);
   if (v == 0 && cnt == 0) { r.text = t(m, "Nothing spent" + (what.isEmpty() ? "" : " on " + what) + " " + when + ".", when + (what.isEmpty() ? "" : " " + what + " ന്") + " ചെലവൊന്നുമില്ല."); return r; }
   r.text = t(m, "Spent " + Fmt.money(v) + (what.isEmpty() ? "" : " on " + what) + " " + when + " (" + cnt + (cnt == 1 ? " entry" : " entries") + ").", when + (what.isEmpty() ? "" : " " + what) + " ചെലവ് " + Fmt.money(v) + " (" + cnt + " എൻട്രി).");
   if (c == null && key == null && v > 0) { r.text += t(m, " Ask \"top categories\" to see where it went.", " എവിടെയാണ് പോയതെന്നറിയാൻ \"top categories\" ചോദിക്കൂ."); }
   return r;
  }
  r.text = t(m, "I did not understand that. ", "എനിക്ക് മനസ്സിലായില്ല. ") + help(m); return r;
 }

 static String help(boolean m) {
  return m ? "ഇങ്ങനെ ചോദിക്കാം:\n• ഈ മാസം എത്ര ചെലവായി?\n• ഭക്ഷണത്തിന് എത്ര?\n• ഈ മാസം ബാക്കി എത്ര?\n• ആര് തരാനുണ്ട്? / രവി തരാനുണ്ടോ?\n• അക്കൗണ്ട് ബാലൻസ്\n• ഏറ്റവും കൂടുതൽ ചെലവ്\n• അവസാന 5 എൻട്രി\n• ബജറ്റ്\n\nചേർക്കാൻ: \"ചായ 20\" അല്ലെങ്കിൽ \"chaya 20\". ഒഴിവാക്കാൻ: \"അവസാനത്തേത് ഒഴിവാക്കൂ\". മാറ്റാൻ: \"അവസാനത്തേത് 500 ആക്കൂ\". ഓരോ മാറ്റത്തിനും മുമ്പ് ഞാൻ ചോദിക്കും."
   : "You can ask:\n• How much did I spend this month / last month / yesterday?\n• How much on food?\n• What is left this month?\n• Who owes me? / Does Ravi owe me?\n• Account balance\n• Top categories / biggest expenses\n• Last 5 entries\n• Budgets, compare with last month, daily average\n\nAdd: \"tea 20\" or \"add auto 60 yesterday\". Delete: \"delete last\". Change: \"change last to 500\". Malayalam and Manglish work too (\"ee maasam ethra chilavu\"). I always ask before changing anything.";
 }
}
