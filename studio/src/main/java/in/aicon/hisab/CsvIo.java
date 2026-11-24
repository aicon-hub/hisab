package in.aicon.hisab;

import java.time.LocalDate;
import java.util.*;

/** CSV export and import of entries, in the exact shape of the app's own export. Everything stays on the phone. */
final class CsvIo {
 private CsvIo() {}

 static final String HEADER = "Date,Type,Amount,Account,To account,Category,Person,Note";

 /** The full ledger as CSV text. Text fields are quoted. */
 static String export(Store db) {
  Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> ppl = Kit.peopleMap(db);
  StringBuilder b = new StringBuilder(HEADER + "\n");
  for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) {
   b.append(t.day).append(',').append(t.kind).append(',').append(Ledger.plain(t.amount)).append(',')
    .append(Ledger.csv(accs.get(t.account) == null ? "" : accs.get(t.account).name)).append(',')
    .append(Ledger.csv(accs.get(t.dest) == null ? "" : accs.get(t.dest).name)).append(',')
    .append(Ledger.csv(cats.get(t.cat) == null ? "" : cats.get(t.cat).name)).append(',')
    .append(Ledger.csv(ppl.get(t.person) == null ? "" : ppl.get(t.person).name)).append(',')
    .append(Ledger.csv(t.note)).append('\n');
  }
  return b.toString();
 }

 /** Parses CSV text into rows. Handles quoted fields, "" escapes and CRLF. */
 static List<String[]> parse(String text) {
  List<String[]> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cur = new StringBuilder();
  boolean q = false;
  for (int i = 0; i < text.length(); i++) {
   char ch = text.charAt(i);
   if (q) {
    if (ch == '"') { if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cur.append('"'); i++; } else q = false; }
    else cur.append(ch);
   } else if (ch == '"') q = true;
   else if (ch == ',') { row.add(cur.toString()); cur.setLength(0); }
   else if (ch == '\n' || ch == '\r') {
    if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
    row.add(cur.toString()); cur.setLength(0);
    if (row.size() > 1 || !row.get(0).isEmpty()) rows.add(row.toArray(new String[0]));
    row.clear();
   } else cur.append(ch);
  }
  if (cur.length() > 0 || !row.isEmpty()) {
   row.add(cur.toString());
   if (row.size() > 1 || !row.get(0).isEmpty()) rows.add(row.toArray(new String[0]));
  }
  return rows;
 }

 static String guessAccountKind(String name) {
  String n = name.toLowerCase(Locale.ROOT);
  if (n.contains("cash")) return "cash";
  if (n.contains("card")) return "card";
  if (n.contains("wallet") || n.contains("upi")) return "wallet";
  return "bank";
 }

 static boolean isKind(String k) {
  for (String x : new String[]{"expense", "income", "transfer", "lent", "borrowed", "repay_in", "repay_out"}) if (x.equals(k)) return true;
  return false;
 }

 /**
  * Imports rows in the export shape, creating missing accounts, categories and people by name.
  * A row identical to an existing entry is skipped, so importing the same file twice adds nothing.
  * Returns {added, skipped}.
  */
 static int[] importEntries(Store db, String text) {
  List<String[]> rows = parse(text);
  int added = 0, skipped = 0;
  if (rows.isEmpty()) return new int[]{0, 0};
  int start = rows.get(0).length > 0 && rows.get(0)[0].trim().equalsIgnoreCase("Date") ? 1 : 0;
  Map<String, Long> accIds = new HashMap<>(); for (Store.Acc x : db.accounts()) accIds.put(x.name.toLowerCase(Locale.ROOT), x.id);
  Map<String, Long> catIds = new HashMap<>(); for (Store.Cat x : db.categories(null)) catIds.put(x.kind + "|" + x.name.toLowerCase(Locale.ROOT), x.id);
  Map<String, Long> people = new HashMap<>(); for (Store.Person x : db.people()) people.put(x.name.toLowerCase(Locale.ROOT), x.id);
  for (int i = start; i < rows.size(); i++) {
   Store.Tx t = mapRow(db, rows.get(i), accIds, catIds, people);
   if (t == null || exists(db, t)) { skipped++; continue; }
   db.save(t); added++;
  }
  return new int[]{added, skipped};
 }

 private static long account(Store db, String name, Map<String, Long> accIds) {
  name = name.trim(); if (name.isEmpty()) return 0;
  Long id = accIds.get(name.toLowerCase(Locale.ROOT));
  if (id != null) return id;
  long nid = db.addAccount(name, guessAccountKind(name), 0);
  accIds.put(name.toLowerCase(Locale.ROOT), nid);
  return nid;
 }

 private static Store.Tx mapRow(Store db, String[] r, Map<String, Long> accIds, Map<String, Long> catIds, Map<String, Long> people) {
  if (r.length < 3) return null;
  String day = r[0].trim(), kind = r[1].trim();
  if (!isKind(kind)) return null;
  try { LocalDate.parse(day); } catch (Exception e) { return null; }
  long amount = Ledger.parse(r[2].trim().replace(",", ""));
  if (amount <= 0) return null;
  long account = account(db, r.length > 3 ? r[3] : "", accIds);
  if (account == 0) return null;
  Store.Tx t = new Store.Tx();
  t.kind = kind; t.amount = amount; t.day = day; t.account = account;
  t.note = r.length > 7 ? r[7].trim() : "";
  if (kind.equals("transfer")) {
   long dest = account(db, r.length > 4 ? r[4] : "", accIds);
   if (dest == 0 || dest == account) return null;
   t.dest = dest;
  }
  if ((kind.equals("expense") || kind.equals("income")) && r.length > 5 && !r[5].trim().isEmpty()) {
   String cn = r[5].trim(); String key = kind + "|" + cn.toLowerCase(Locale.ROOT);
   Long cid = catIds.get(key);
   if (cid == null) { cid = db.addCategory(cn, kind, CategoryIcons.suggest(cn)); catIds.put(key, cid); }
   t.cat = cid;
  }
  boolean peopleKind = kind.equals("lent") || kind.equals("borrowed") || kind.startsWith("repay");
  if (peopleKind) {
   String pn = r.length > 6 ? r[6].trim() : "";
   if (pn.isEmpty()) return null;
   Long pid = people.get(pn.toLowerCase(Locale.ROOT));
   if (pid == null) { pid = db.addPerson(pn, ""); people.put(pn.toLowerCase(Locale.ROOT), pid); }
   t.person = pid;
  }
  return t;
 }

 private static boolean exists(Store db, Store.Tx t) {
  for (Store.Tx x : db.txs(t.day, t.day)) {
   if (x.kind.equals(t.kind) && x.amount == t.amount && x.account == t.account && x.dest == t.dest && x.cat == t.cat && x.person == t.person && x.note.equals(t.note)) return true;
  }
  return false;
 }
}
