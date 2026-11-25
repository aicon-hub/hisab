package in.aicon.hisab;

import java.time.LocalDate;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Shared ledger: a portable file one person can hand to another so both keep the same entries.
 * Unlike the CSV export this keeps tags (trips, shared markers) and entry kinds exactly.
 * Everything stays on the phones; the file travels however the people choose to send it.
 */
final class SharedLedger {
 private SharedLedger() {}

 static final String MARK = "hisab-shared-ledger";

 /** The full ledger as shared-ledger text. */
 static String export(Store db) {
  Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> ppl = Kit.peopleMap(db);
  JSONObject root = new JSONObject();
  try {
   root.put("app", MARK); root.put("version", 1); root.put("exported", LocalDate.now().toString());
   JSONArray list = new JSONArray();
   for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) {
    Store.Cat ct = cats.get(t.cat); Store.Acc ac = accs.get(t.account); Store.Acc de = accs.get(t.dest); Store.Person pe = ppl.get(t.person);
    list.put(new JSONObject()
     .put("day", t.day).put("kind", t.kind).put("amount", t.amount)
     .put("account", ac == null ? "" : ac.name).put("dest", de == null ? "" : de.name)
     .put("category", ct == null ? "" : ct.name).put("person", pe == null ? "" : pe.name)
     .put("note", t.note == null ? "" : t.note).put("tags", t.tags == null ? "" : t.tags));
   }
   root.put("entries", list);
   return root.toString(1);
  } catch (Exception ignored) {}
  return "{}";
 }

 static boolean isSharedLedger(String text) {
  if (text == null) return false;
  try { return MARK.equals(new JSONObject(text.trim()).optString("app")); } catch (Exception e) { return false; }
 }

 /**
  * Imports a shared-ledger file, creating missing accounts, categories and people by name.
  * An entry identical to an existing one (every field, including tags) is skipped, so sharing
  * back and forth between two phones never duplicates. Returns {added, skipped}.
  */
 static int[] importEntries(Store db, String text) {
  int added = 0, skipped = 0;
  JSONArray list;
  try { list = new JSONObject(text.trim()).getJSONArray("entries"); } catch (Exception e) { return new int[]{0, 0}; }
  Map<String, Long> accIds = new HashMap<>(); for (Store.Acc x : db.accounts()) accIds.put(x.name.toLowerCase(Locale.ROOT), x.id);
  Map<String, Long> catIds = new HashMap<>(); for (Store.Cat x : db.categories(null)) catIds.put(x.kind + "|" + x.name.toLowerCase(Locale.ROOT), x.id);
  Map<String, Long> people = new HashMap<>(); for (Store.Person x : db.people()) people.put(x.name.toLowerCase(Locale.ROOT), x.id);
  for (int i = 0; i < list.length(); i++) {
   Store.Tx t = map(db, list.optJSONObject(i), accIds, catIds, people);
   if (t == null || exists(db, t)) { skipped++; continue; }
   db.save(t); added++;
  }
  return new int[]{added, skipped};
 }

 private static long account(Store db, String name, Map<String, Long> accIds) {
  name = name.trim(); if (name.isEmpty()) return 0;
  Long id = accIds.get(name.toLowerCase(Locale.ROOT));
  if (id != null) return id;
  long nid = db.addAccount(name, CsvIo.guessAccountKind(name), 0);
  accIds.put(name.toLowerCase(Locale.ROOT), nid);
  return nid;
 }

 private static Store.Tx map(Store db, JSONObject o, Map<String, Long> accIds, Map<String, Long> catIds, Map<String, Long> people) {
  if (o == null) return null;
  String day = o.optString("day", "").trim(), kind = o.optString("kind", "").trim();
  if (!CsvIo.isKind(kind)) return null;
  try { LocalDate.parse(day); } catch (Exception e) { return null; }
  long amount = o.optLong("amount", 0);
  if (amount <= 0) return null;
  long account = account(db, o.optString("account", ""), accIds);
  if (account == 0) return null;
  Store.Tx t = new Store.Tx();
  t.kind = kind; t.amount = amount; t.day = day; t.account = account;
  t.note = o.optString("note", "").trim();
  t.tags = o.optString("tags", "").trim();
  if (kind.equals("transfer")) {
   long dest = account(db, o.optString("dest", ""), accIds);
   if (dest == 0 || dest == account) return null;
   t.dest = dest;
  }
  String cn = o.optString("category", "").trim();
  if ((kind.equals("expense") || kind.equals("income")) && !cn.isEmpty()) {
   String key = kind + "|" + cn.toLowerCase(Locale.ROOT);
   Long cid = catIds.get(key);
   if (cid == null) { cid = db.addCategory(cn, kind, CategoryIcons.suggest(cn)); catIds.put(key, cid); }
   t.cat = cid;
  }
  boolean peopleKind = kind.equals("lent") || kind.equals("borrowed") || kind.startsWith("repay");
  if (peopleKind) {
   String pn = o.optString("person", "").trim();
   if (pn.isEmpty()) return null;
   Long pid = people.get(pn.toLowerCase(Locale.ROOT));
   if (pid == null) { pid = db.addPerson(pn, ""); people.put(pn.toLowerCase(Locale.ROOT), pid); }
   t.person = pid;
  }
  return t;
 }

 private static boolean exists(Store db, Store.Tx t) {
  String tags = t.tags == null ? "" : t.tags;
  for (Store.Tx x : db.txs(t.day, t.day)) {
   if (x.kind.equals(t.kind) && x.amount == t.amount && x.account == t.account && x.dest == t.dest && x.cat == t.cat && x.person == t.person && x.note.equals(t.note) && (x.tags == null ? "" : x.tags).equals(tags)) return true;
  }
  return false;
 }
}
