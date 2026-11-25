package in.aicon.hisab;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
import java.util.*;

/** On-device ledger. One SQLite file, no network, no server. Amounts are minor units (paise). */
final class Store extends SQLiteOpenHelper {
 static final class Acc { long id; String name, kind; long opening; }
 static final class Cat { long id; String name, kind, icon; }
 static final class Person { long id; String name, phone; }
 static final class Tx { long id; String kind, day, note; long amount, account, dest, cat, person; long deleted; String tags = ""; }
 static final class Goal { long id; String name, icon; long target, saved; }
 static final class Rec { long id; String title, kind, period, next; long amount, account, cat; boolean active; }
 static final class Att { long id; long tx; String path; }

 static final String[] KINDS = {"expense", "income", "transfer", "lent", "borrowed"};
 final Context ctx;

 Store(Context c) { super(c.getApplicationContext(), "hisab_studio.db", null, 3); ctx = c.getApplicationContext(); }

 @Override public void onCreate(SQLiteDatabase d) {
  d.execSQL("CREATE TABLE prefs(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
  d.execSQL("CREATE TABLE account(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, kind TEXT NOT NULL, opening INTEGER NOT NULL DEFAULT 0)");
  d.execSQL("CREATE TABLE category(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, kind TEXT NOT NULL, icon TEXT NOT NULL)");
  d.execSQL("CREATE TABLE person(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '')");
  d.execSQL("CREATE TABLE tx(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, amount INTEGER NOT NULL, day TEXT NOT NULL, account INTEGER NOT NULL, dest INTEGER NOT NULL DEFAULT 0, cat INTEGER NOT NULL DEFAULT 0, note TEXT NOT NULL DEFAULT '', person INTEGER NOT NULL DEFAULT 0, deleted INTEGER NOT NULL DEFAULT 0, tags TEXT NOT NULL DEFAULT '')");
  d.execSQL("CREATE INDEX tx_day ON tx(day)");
  d.execSQL("CREATE TABLE budget(cat INTEGER PRIMARY KEY, amount INTEGER NOT NULL)");
  d.execSQL("CREATE TABLE goal(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, target INTEGER NOT NULL, saved INTEGER NOT NULL DEFAULT 0, icon TEXT NOT NULL DEFAULT 'target')");
  d.execSQL("CREATE TABLE recur(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, kind TEXT NOT NULL, amount INTEGER NOT NULL, account INTEGER NOT NULL, cat INTEGER NOT NULL, period TEXT NOT NULL, next TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
  d.execSQL("CREATE TABLE attach(id INTEGER PRIMARY KEY AUTOINCREMENT, tx INTEGER NOT NULL, path TEXT NOT NULL)");
 }
 @Override public void onUpgrade(SQLiteDatabase d, int a, int b) {
  if (a < 2) d.execSQL("CREATE TABLE IF NOT EXISTS attach(id INTEGER PRIMARY KEY AUTOINCREMENT, tx INTEGER NOT NULL, path TEXT NOT NULL)");
  if (a < 3) { d.execSQL("ALTER TABLE tx ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0"); d.execSQL("ALTER TABLE tx ADD COLUMN tags TEXT NOT NULL DEFAULT ''"); }
 }

 String pref(String k, String def) { try (Cursor c = getReadableDatabase().rawQuery("SELECT v FROM prefs WHERE k=?", new String[]{k})) { return c.moveToFirst() ? c.getString(0) : def; } }
 void setPref(String k, String v) { ContentValues cv = new ContentValues(); cv.put("k", k); cv.put("v", v); getWritableDatabase().insertWithOnConflict("prefs", null, cv, SQLiteDatabase.CONFLICT_REPLACE); }
 boolean flag(String k, boolean def) { return pref(k, def ? "1" : "0").equals("1"); }
 /** Any stored data changed: ask DriveBackup to mirror it (no-op when backup is off). */
 private void changed() { try { DriveBackup.dataChanged(this); } catch (Exception ignored) {} try { HisabWidget.refresh(ctx); } catch (Exception ignored) {} }

 List<Acc> accounts() {
  List<Acc> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,kind,opening FROM account ORDER BY id", null)) { while (c.moveToNext()) { Acc a = new Acc(); a.id = c.getLong(0); a.name = c.getString(1); a.kind = c.getString(2); a.opening = c.getLong(3); l.add(a); } }
  return l;
 }
 long addAccount(String name, String kind, long opening) { ContentValues v = new ContentValues(); v.put("name", name); v.put("kind", kind); v.put("opening", opening); long id = getWritableDatabase().insert("account", null, v); changed(); return id; }
 void updateAccount(Acc a) { ContentValues v = new ContentValues(); v.put("name", a.name); v.put("kind", a.kind); v.put("opening", a.opening); getWritableDatabase().update("account", v, "id=?", new String[]{"" + a.id}); changed(); }
 boolean deleteAccount(long id) {
  if(accounts().size()<=1)return false;
  try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tx WHERE deleted=0 AND (account=? OR dest=?)", new String[]{"" + id, "" + id})) { c.moveToFirst(); if (c.getLong(0) > 0) return false; }
  getWritableDatabase().delete("account", "id=?", new String[]{"" + id}); changed(); return true;
 }
 Acc account(long id) { for (Acc a : accounts()) if (a.id == id) return a; return null; }
 long balance(Acc a) {
  long b = a.opening;
  try (Cursor c = getReadableDatabase().rawQuery("SELECT kind,amount,account,dest FROM tx WHERE deleted=0 AND (account=? OR dest=?)", new String[]{"" + a.id, "" + a.id})) {
   while (c.moveToNext()) {
    String k = c.getString(0); long amt = c.getLong(1); boolean from = c.getLong(2) == a.id, to = c.getLong(3) == a.id;
    if (k.equals("transfer")) { if (from) b -= amt; if (to) b += amt; }
    else if (from) b += sign(k) * amt;
   }
  }
  return b;
 }
 static int sign(String k) { return (k.equals("income") || k.equals("borrowed") || k.equals("repay_in")) ? 1 : -1; }

 List<Cat> categories(String kind) {
  List<Cat> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,kind,icon FROM category" + (kind == null ? "" : " WHERE kind=?") + " ORDER BY id", kind == null ? null : new String[]{kind})) { while (c.moveToNext()) { Cat x = new Cat(); x.id = c.getLong(0); x.name = c.getString(1); x.kind = c.getString(2); x.icon = c.getString(3); l.add(x); } }
  return l;
 }
 long addCategory(String name, String kind, String icon) { ContentValues v = new ContentValues(); v.put("name", name); v.put("kind", kind); v.put("icon", icon); long id = getWritableDatabase().insert("category", null, v); changed(); return id; }
 void updateCategory(Cat x) { ContentValues v = new ContentValues(); v.put("name", x.name); v.put("icon", x.icon); getWritableDatabase().update("category", v, "id=?", new String[]{"" + x.id}); changed(); }
 void deleteCategory(long id) { SQLiteDatabase d = getWritableDatabase(); d.update("tx", cv("cat", 0), "cat=?", new String[]{"" + id}); d.delete("budget", "cat=?", new String[]{"" + id}); d.delete("category", "id=?", new String[]{"" + id}); changed(); }
 Map<Long, Cat> categoryMap() { Map<Long, Cat> m = new HashMap<>(); for (Cat c : categories(null)) m.put(c.id, c); return m; }
 private static ContentValues cv(String k, long v) { ContentValues c = new ContentValues(); c.put(k, v); return c; }

 List<Person> people() {
  List<Person> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,phone FROM person ORDER BY name COLLATE NOCASE", null)) { while (c.moveToNext()) { Person p = new Person(); p.id = c.getLong(0); p.name = c.getString(1); p.phone = c.getString(2); l.add(p); } }
  return l;
 }
 long addPerson(String name, String phone) {
  phone = Phone.normalise(phone);
  for (Person p : people()) if (p.name.equalsIgnoreCase(name) && (phone.isEmpty() || p.phone.equals(phone) || p.phone.isEmpty())) { if (p.phone.isEmpty() && !phone.isEmpty()) { ContentValues v = new ContentValues(); v.put("phone", phone); getWritableDatabase().update("person", v, "id=?", new String[]{"" + p.id}); } return p.id; }
  ContentValues v = new ContentValues(); v.put("name", name); v.put("phone", phone); return getWritableDatabase().insert("person", null, v);
 }
 void updatePerson(Person p) { ContentValues v = new ContentValues(); v.put("name", p.name); v.put("phone", Phone.normalise(p.phone)); getWritableDatabase().update("person", v, "id=?", new String[]{"" + p.id}); changed(); }
 void deletePerson(long id) { SQLiteDatabase d = getWritableDatabase(); d.delete("tx", "person=?", new String[]{"" + id}); d.delete("person", "id=?", new String[]{"" + id}); changed(); }
 /** Positive: they owe you. Negative: you owe them. */
 long net(long personId) {
  long n = 0;
  try (Cursor c = getReadableDatabase().rawQuery("SELECT kind,amount FROM tx WHERE deleted=0 AND person=?", new String[]{"" + personId})) {
   while (c.moveToNext()) { String k = c.getString(0); long a = c.getLong(1); switch (k) { case "lent": n += a; break; case "repay_in": n -= a; break; case "borrowed": n -= a; break; case "repay_out": n += a; break; default: } }
  }
  return n;
 }

 private static Tx tx(Cursor c) { Tx t = new Tx(); t.id = c.getLong(0); t.kind = c.getString(1); t.amount = c.getLong(2); t.day = c.getString(3); t.account = c.getLong(4); t.dest = c.getLong(5); t.cat = c.getLong(6); t.note = c.getString(7); t.person = c.getLong(8); t.deleted = c.getLong(9); t.tags = c.getString(10); return t; }
 static final String COLS = "id,kind,amount,day,account,dest,cat,note,person,deleted,tags";
 List<Tx> txs(String from, String to) {
  List<Tx> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM tx WHERE deleted=0 AND day>=? AND day<=? ORDER BY day DESC, id DESC", new String[]{from, to})) { while (c.moveToNext()) l.add(tx(c)); }
  return l;
 }
 List<Tx> txsOfPerson(long person) {
  List<Tx> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM tx WHERE deleted=0 AND person=? ORDER BY day DESC, id DESC", new String[]{"" + person})) { while (c.moveToNext()) l.add(tx(c)); }
  return l;
 }
 /** Distinct tags across every entry (bin included), each with its use count, sorted A to Z. */
 List<String[]> tagCounts() {
  Map<String, Integer> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  try (Cursor c = getReadableDatabase().rawQuery("SELECT tags FROM tx WHERE tags <> ''", null)) {
   while (c.moveToNext()) for (String t : c.getString(0).split(",")) { String n = t.trim(); if (!n.isEmpty()) m.merge(n, 1, Integer::sum); }
  }
  List<String[]> out = new ArrayList<>(); for (Map.Entry<String, Integer> e : m.entrySet()) out.add(new String[]{e.getKey(), "" + e.getValue()});
  return out;
 }

 /** Renames a tag on every entry that uses it; naming an existing tag merges them. */
 void renameTag(String from, String to) { retag(from.trim(), to.trim()); }

 /** Takes a tag off every entry that uses it; the entries themselves stay. */
 void dropTag(String tag) { retag(tag.trim(), null); }

 private void retag(String from, String to) {
  if (from.isEmpty() || (to != null && (to.isEmpty() || to.equalsIgnoreCase(from)))) return;
  List<Long> ids = new ArrayList<>(); List<String> vals = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT id, tags FROM tx WHERE tags <> ''", null)) {
   while (c.moveToNext()) {
    List<String> parts = new ArrayList<>(); boolean hit = false;
    for (String t : c.getString(1).split(",")) {
     String n = t.trim(); if (n.isEmpty()) continue;
     if (n.equalsIgnoreCase(from)) { hit = true; if (to != null) n = to; else continue; }
     boolean dup = false; for (String x : parts) if (x.equalsIgnoreCase(n)) { dup = true; break; }
     if (!dup) parts.add(n);
    }
    if (hit) { ids.add(c.getLong(0)); vals.add(android.text.TextUtils.join(", ", parts)); }
   }
  }
  for (int i = 0; i < ids.size(); i++) { ContentValues v = new ContentValues(); v.put("tags", vals.get(i)); getWritableDatabase().update("tx", v, "id=?", new String[]{"" + ids.get(i)}); }
  if (!ids.isEmpty()) changed();
 }

 List<Tx> recent(int n) {
  List<Tx> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM tx WHERE deleted=0 ORDER BY day DESC, id DESC LIMIT " + n, null)) { while (c.moveToNext()) l.add(tx(c)); }
  return l;
 }
 Tx txById(long id) { try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM tx WHERE id=?", new String[]{"" + id})) { return c.moveToFirst() ? tx(c) : null; } }
 long save(Tx t) {
  ContentValues v = new ContentValues(); v.put("kind", t.kind); v.put("amount", t.amount); v.put("day", t.day); v.put("account", t.account); v.put("dest", t.dest); v.put("cat", t.cat); v.put("note", t.note == null ? "" : t.note); v.put("person", t.person); v.put("tags", t.tags == null ? "" : t.tags);
  long id;
  if (t.id > 0) { getWritableDatabase().update("tx", v, "id=?", new String[]{"" + t.id}); id = t.id; }
  else id = getWritableDatabase().insert("tx", null, v);
  changed();
  return id;
 }
 void deleteTx(long id) { getWritableDatabase().delete("tx", "id=?", new String[]{"" + id}); changed(); }
 void trashTx(long id) { getWritableDatabase().update("tx", cv("deleted", System.currentTimeMillis()), "id=?", new String[]{"" + id}); changed(); }
 void restoreTx(long id) { getWritableDatabase().update("tx", cv("deleted", 0), "id=?", new String[]{"" + id}); changed(); }
 List<Tx> trash() { List<Tx> l = new ArrayList<>(); try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM tx WHERE deleted>0 ORDER BY deleted DESC", null)) { while (c.moveToNext()) l.add(tx(c)); } return l; }
 /** Hard-deletes entries trashed more than 30 days ago. Returns their ids so callers can clean attachment files. */
 List<Long> purgeTrash() {
  List<Long> ids = new ArrayList<>(); long cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
  try (Cursor c = getReadableDatabase().rawQuery("SELECT id FROM tx WHERE deleted>0 AND deleted<?", new String[]{"" + cutoff})) { while (c.moveToNext()) ids.add(c.getLong(0)); }
  for (long id : ids) { getWritableDatabase().delete("attach", "tx=?", new String[]{"" + id}); getWritableDatabase().delete("tx", "id=?", new String[]{"" + id}); }
  if (!ids.isEmpty()) changed();
  return ids;
 }
 List<Att> attachments(long tx) { List<Att> l = new ArrayList<>(); try (Cursor c = getReadableDatabase().rawQuery("SELECT id,path FROM attach WHERE tx=? ORDER BY id", new String[]{"" + tx})) { while (c.moveToNext()) { Att x = new Att(); x.id = c.getLong(0); x.tx = tx; x.path = c.getString(1); l.add(x); } } return l; }
 long addAttach(long tx, String path) { ContentValues v = new ContentValues(); v.put("tx", tx); v.put("path", path); long id = getWritableDatabase().insert("attach", null, v); changed(); return id; }
 void removeAttach(long id) { getWritableDatabase().delete("attach", "id=?", new String[]{"" + id}); changed(); }
 void removeAttachFor(long tx) { getWritableDatabase().delete("attach", "tx=?", new String[]{"" + tx}); changed(); }
 void removeAttachAll() { getWritableDatabase().delete("attach", null, null); }
 boolean hasData() { try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tx WHERE deleted=0", null)) { c.moveToFirst(); return c.getLong(0) > 0; } }

 Map<Long, Long> budgets() { Map<Long, Long> m = new LinkedHashMap<>(); try (Cursor c = getReadableDatabase().rawQuery("SELECT cat,amount FROM budget ORDER BY amount DESC", null)) { while (c.moveToNext()) m.put(c.getLong(0), c.getLong(1)); } return m; }
 void setBudget(long cat, long amount) { if (amount <= 0) { getWritableDatabase().delete("budget", "cat=?", new String[]{"" + cat}); changed(); return; } ContentValues v = new ContentValues(); v.put("cat", cat); v.put("amount", amount); getWritableDatabase().insertWithOnConflict("budget", null, v, SQLiteDatabase.CONFLICT_REPLACE); changed(); }

 List<Goal> goals() { List<Goal> l = new ArrayList<>(); try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,target,saved,icon FROM goal ORDER BY id", null)) { while (c.moveToNext()) { Goal g = new Goal(); g.id = c.getLong(0); g.name = c.getString(1); g.target = c.getLong(2); g.saved = c.getLong(3); g.icon = c.getString(4); l.add(g); } } return l; }
 long saveGoal(Goal g) { ContentValues v = new ContentValues(); v.put("name", g.name); v.put("target", g.target); v.put("saved", g.saved); v.put("icon", g.icon == null ? "target" : g.icon); long id; if (g.id > 0) { getWritableDatabase().update("goal", v, "id=?", new String[]{"" + g.id}); id = g.id; } else id = getWritableDatabase().insert("goal", null, v); changed(); return id; }
 void deleteGoal(long id) { getWritableDatabase().delete("goal", "id=?", new String[]{"" + id}); changed(); }

 List<Rec> recurring() {
  List<Rec> l = new ArrayList<>();
  try (Cursor c = getReadableDatabase().rawQuery("SELECT id,title,kind,amount,account,cat,period,next,active FROM recur ORDER BY next", null)) { while (c.moveToNext()) { Rec r = new Rec(); r.id = c.getLong(0); r.title = c.getString(1); r.kind = c.getString(2); r.amount = c.getLong(3); r.account = c.getLong(4); r.cat = c.getLong(5); r.period = c.getString(6); r.next = c.getString(7); r.active = c.getInt(8) == 1; l.add(r); } }
  return l;
 }
 long saveRec(Rec r) { ContentValues v = new ContentValues(); v.put("title", r.title); v.put("kind", r.kind); v.put("amount", r.amount); v.put("account", r.account); v.put("cat", r.cat); v.put("period", r.period); v.put("next", r.next); v.put("active", r.active ? 1 : 0); long id; if (r.id > 0) { getWritableDatabase().update("recur", v, "id=?", new String[]{"" + r.id}); id = r.id; } else id = getWritableDatabase().insert("recur", null, v); changed(); return id; }
 void deleteRec(long id) { getWritableDatabase().delete("recur", "id=?", new String[]{"" + id}); changed(); }
 static String advance(String day, String period) { LocalDate d = LocalDate.parse(day); return (period.equals("weekly") ? d.plusWeeks(1) : period.equals("yearly") ? d.plusYears(1) : d.plusMonths(1)).toString(); }
 /** Adds the entry for the due date and moves the schedule forward. */
 void confirm(Rec r) {
  Tx t = new Tx(); t.kind = r.kind; t.amount = r.amount; t.day = r.next; t.account = r.account; t.cat = r.cat; t.note = r.title; save(t);
  r.next = advance(r.next, r.period); saveRec(r);
 }
 void skip(Rec r) { r.next = advance(r.next, r.period); saveRec(r); }

 /** One-time pass: normalise phone numbers saved before the +91 fix. */
 void normalisePhones() {
  if (flag("phoneFixV1", false)) return;
  for (Person p : people()) { String n = Phone.normalise(p.phone); if (!n.equals(p.phone)) { ContentValues v = new ContentValues(); v.put("phone", n); getWritableDatabase().update("person", v, "id=?", new String[]{"" + p.id}); } }
  setPref("phoneFixV1", "1");
 }

 void wipe() { SQLiteDatabase d = getWritableDatabase(); for (String t : new String[]{"tx", "budget", "goal", "recur", "person", "category", "account"}) d.delete(t, null, null); setPref("sample", "0"); }
 void seedBasics() {
  if (!categories(null).isEmpty()) return;
  String[][] ex = {{"Food", "food"}, {"Groceries", "groceries"}, {"Transport", "transport"}, {"Fuel", "fuel"}, {"Shopping", "shopping"}, {"Bills", "bills"}, {"Health", "health"}, {"Fun", "fun"}, {"Education", "education"}, {"Travel", "travel"}, {"Other", "other"}};
  for (String[] e : ex) addCategory(e[0], "expense", e[1]);
  String[][] in = {{"Salary", "coin"}, {"Gift", "gift"}, {"Other income", "other"}};
  for (String[] e : in) addCategory(e[0], "income", e[1]);
  if (accounts().isEmpty()) { addAccount("Cash", "cash", 0); addAccount("Bank", "bank", 0); }
 }
}
