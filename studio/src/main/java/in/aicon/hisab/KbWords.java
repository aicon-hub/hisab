package in.aicon.hisab;

import android.database.Cursor;
import java.util.*;

/** Word suggestions for Hisab's own keyboard: the user's own categories, people, accounts, tags, trips and note words, plus common money words. */
final class KbWords {
 private KbWords() {}
 static final class W { final String text, key; final int weight; final boolean ml; W(String t, String k, int w, boolean m) { text = t; key = k; weight = w; ml = m; } }

 static final String[] EN = {"Salary", "Rent", "Groceries", "Petrol", "Recharge", "Electricity", "Water", "Auto", "Taxi", "Bus", "Tea", "Coffee", "Breakfast", "Lunch", "Dinner", "Snacks",
  "Medicine", "Hospital", "School fee", "Gift", "Shopping", "Mobile bill", "Internet", "Milk", "Vegetables", "Fruits", "Travel", "Hotel", "Movie", "Bill", "Savings", "Lent", "Borrowed", "Fuel", "Parking", "Transfer"};
 /** roman key, Malayalam word */
 static final String[][] ML = {
  {"ente", "എന്റെ"}, {"ninte", "നിന്റെ"}, {"njaan", "ഞാൻ"}, {"nee", "നീ"}, {"avan", "അവൻ"}, {"aval", "അവൾ"}, {"aanu", "ആണ്"}, {"alla", "അല്ല"}, {"undu", "ഉണ്ട്"}, {"illa", "ഇല്ല"},
  {"enthu", "എന്ത്"}, {"ethra", "എത്ര"}, {"evide", "എവിടെ"}, {"ivide", "ഇവിടെ"}, {"kadam", "കടം"}, {"paisa", "പൈസ"}, {"panam", "പണം"}, {"roopa", "രൂപ"}, {"chaya", "ചായ"}, {"kaapi", "കാപ്പി"},
  {"bhakshanam", "ഭക്ഷണം"}, {"chelavu", "ചെലവ്"}, {"varavu", "വരവ്"}, {"shambalam", "ശമ്പളം"}, {"vaadaka", "വാടക"}, {"vila", "വില"}, {"saadhanam", "സാധനം"}, {"pachakkari", "പച്ചക്കറി"},
  {"pazham", "പഴം"}, {"paal", "പാൽ"}, {"ari", "അരി"}, {"mutta", "മുട്ട"}, {"meen", "മീൻ"}, {"erachi", "ഇറച്ചി"}, {"petrol", "പെട്രോൾ"}, {"bus", "ബസ്"}, {"auto", "ഓട്ടോ"}, {"yaathra", "യാത്ര"},
  {"ticket", "ടിക്കറ്റ്"}, {"hotel", "ഹോട്ടൽ"}, {"marunnu", "മരുന്ന്"}, {"aashupathri", "ആശുപത്രി"}, {"school", "സ്കൂൾ"}, {"fees", "ഫീസ്"}, {"vaidyuthi", "വൈദ്യുതി"}, {"vellam", "വെള്ളം"},
  {"recharge", "റീചാർജ്"}, {"phone", "ഫോൺ"}, {"kadayil", "കടയിൽ"}, {"innu", "ഇന്ന്"}, {"innale", "ഇന്നലെ"}, {"naale", "നാളെ"}, {"maasam", "മാസം"}, {"aazhcha", "ആഴ്ച"}, {"nalla", "നല്ല"},
  {"sammaanam", "സമ്മാനം"}, {"koduthu", "കൊടുത്തു"}, {"vaangi", "വാങ്ങി"}, {"kittiyathu", "കിട്ടിയത്"}, {"thirichu", "തിരിച്ച്"}, {"nanni", "നന്ദി"}, {"sheri", "ശരി"}, {"mathi", "മതി"}};

 static boolean hasMl(String s) { for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c >= 0x0D00 && c <= 0x0D7F) return true; } return false; }

 static List<W> load(Store db) {
  Map<String, W> m = new LinkedHashMap<>();
  try {
   for (Store.Cat c : db.categories(null)) add(m, c.name, 50);
   for (Store.Person p : db.people()) add(m, p.name, 40);
   for (Store.Acc x : db.accounts()) add(m, x.name, 30);
   for (String[] t : db.tagCounts()) add(m, t[0], 30 + Math.min(20, Integer.parseInt(t[1])));
   for (Trips.Trip t : Trips.read(db)) add(m, t.name, 35);
   Map<String, Integer> freq = new HashMap<>();
   try (Cursor c = db.getReadableDatabase().rawQuery("SELECT note FROM tx WHERE note <> '' ORDER BY id DESC LIMIT 400", null)) {
    while (c.moveToNext()) for (String w : c.getString(0).split("[^\\p{L}\\p{M}]+")) if (w.length() >= 3) freq.merge(w, 1, Integer::sum);
   }
   for (Map.Entry<String, Integer> e : freq.entrySet()) add(m, e.getKey(), 5 + Math.min(20, e.getValue()));
  } catch (Exception ignored) { }
  for (String s : EN) add(m, s, 10);
  for (String[] p : ML) m.put("ml:" + p[0], new W(p[1], p[0], 10, true));
  return new ArrayList<>(m.values());
 }
 private static void add(Map<String, W> m, String text, int w) {
  if (text == null) return; String t = text.trim(); if (t.isEmpty()) return;
  String k = t.toLowerCase(Locale.ROOT); W old = m.get(k);
  if (old == null || old.weight < w) m.put(k, new W(t, k, w, hasMl(t)));
 }

 /** mode 0 English, 1 Malayalam script, 2 Manglish (word is the roman letters typed so far). */
 static List<String> suggest(List<W> all, String word, int mode, int max) {
  String q = word.toLowerCase(Locale.ROOT); List<W> hit = new ArrayList<>();
  for (W w : all) {
   boolean ok;
   if (mode == 0) ok = !w.ml && w.key.startsWith(q);
   else if (mode == 1) ok = w.ml && (q.isEmpty() || w.text.startsWith(word));
   else ok = q.length() > 0 ? w.key.startsWith(q) : false;
   if (q.isEmpty() && mode != 2 && mode != 1) ok = !w.ml && w.weight >= 30;
   if (q.isEmpty() && mode == 1) ok = w.ml && w.weight >= 10;
   if (ok && !(mode == 0 && w.key.equals(q))) hit.add(w);
  }
  hit.sort((x, y) -> y.weight != x.weight ? y.weight - x.weight : x.text.length() - y.text.length());
  List<String> out = new ArrayList<>();
  for (W w : hit) { if (!out.contains(w.text)) out.add(w.text); if (out.size() >= max) break; }
  return out;
 }
}
