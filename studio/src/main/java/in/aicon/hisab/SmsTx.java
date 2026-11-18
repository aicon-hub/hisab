package in.aicon.hisab;

import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * Opt-in expense import from bank SMS. On-device only; raw SMS is never stored or uploaded.
 * Original code. Parsing ideas (reject e-mandates and future debits, treat "credited" as the only
 * certain incoming keyword, stop-delimited counterparty names, stable dedupe keys) were informed by
 * the pennywiseai-tracker project (AGPL-3.0, github.com/sarim2000/pennywiseai-tracker); no code was copied.
 */
final class SmsTx {
 private SmsTx() {}

 static final class T { String bank, last4, who = "", utr = ""; long amount; boolean p2p; }

 static final Pattern GUARD = Pattern.compile("(?is).*\\b(OTP|ONE[ -]TIME|E-?MANDATE|UPI-?MANDATE|WILL BE DEBITED|MANDATE SET|UPCOMING|OFFER|LOAN|CREDIT CARD|DUE DATE|MINIMUM DUE|REQUESTED|FAILED|DECLINED|EXPIRED|BLOCKED|KYC|LOTTERY|CASHBACK OFFER|REWARD)\\b.*");
 static final Pattern CREDIT = Pattern.compile("(?is).*\\b(credited|deposited|refund(?:ed)?|cashback|reversal|reversed)\\b.*");
 static final Pattern DEBIT = Pattern.compile("(?i)\\b(debited|spent|paid|sent|withdrawn|w/d|purchase(?:d)?|transferred|payment of|txn of)\\b");
 static final Pattern MONEY_BEFORE = Pattern.compile("(?i)(?:INR|Rs\\.?|₹)\\s*" + BankSms.MONEY + "(?![0-9]|[.,][0-9])");
 static final Pattern MONEY_AFTER = Pattern.compile("(?i)" + BankSms.MONEY + "\\s*(?:INR|Rs\\.?|₹)(?![a-z])");
 static final Pattern VPA = Pattern.compile("\\b([A-Za-z0-9._-]{2,}@[A-Za-z]{2,})\\b");
 static final Pattern TO_NAME = Pattern.compile("(?i)\\b(?:to|at|towards|favou?ring)\\s+([A-Za-z][A-Za-z0-9 .&'()-]{1,40})");
 static final Pattern UTR = Pattern.compile("(?i)\\b(?:UTR|UPI\\s*Ref(?:erence)?|Ref(?:erence)?\\s*(?:No\\.?|Number|ID)?)\\s*[:.]?\\s*([A-Z0-9]{8,22})\\b");
 static final String STOP = "(?i)\\s+(?:on|ref|utr|upi|avl|available|bal|balance|info|call|via|from|not you|if not|sms|thru|through)\\b.*";
 static final Pattern MERCHANT = Pattern.compile("(?i)\\b(pvt|ltd|llp|inc|corp|india|store?s?|mart|shop|supermarket|market|petrol|pump|hotel|restaurant|cafe|pharmacy|hospital|motors|enterprise?s?|traders?|agency|services?|solutions?|technologies|airlines?|railways?|irctc)\\b");
 static final Pattern BRAND = Pattern.compile("(?i).*(amazon|flipkart|zomato|swiggy|uber|ola|rapido|netflix|spotify|paytm|phonepe|googlepay|gpay|jio|airtel|vodafone|billdesk|razorpay|myntra|makemytrip|bookmyshow).*");

 /** Parse a debit SMS into a candidate expense, or null when it is not a clear single debit. */
 static T parse(String sender, String body, String customBank) {
  if (sender == null || body == null || body.length() > 5000) return null;
  if (GUARD.matcher(body).matches()) return null;
  if (CREDIT.matcher(body).matches() && !DEBIT.matcher(body).find()) return null;
  if (!DEBIT.matcher(body).find()) return null;
  String bank = customBank == null ? BankSms.BANKS.get(BankSms.senderKey(sender)) : customBank;
  if (bank == null) return null;
  Matcher am = BankSms.ACCOUNT.matcher(body);
  if (!am.find()) return null;
  String digits = am.group(1), last4 = digits.substring(digits.length() - 4);
  if (am.find()) return null; // two accounts mentioned: ambiguous
  String work = BankSms.BALANCE.matcher(body).replaceAll(" ");
  work = BankSms.BALANCE_AFTER.matcher(work).replaceAll(" ");
  Set<String> amounts = new LinkedHashSet<>();
  for (Matcher m = MONEY_BEFORE.matcher(work); m.find(); ) amounts.add(m.group(1));
  for (Matcher m = MONEY_AFTER.matcher(work); m.find(); ) amounts.add(m.group(1));
  if (amounts.size() != 1) return null; // none, or several different amounts: ambiguous
  long amount;
  try { amount = BankSms.parseMoney(amounts.iterator().next(), null); } catch (Exception e) { return null; }
  if (amount <= 0) return null;
  T t = new T(); t.bank = bank; t.last4 = last4; t.amount = amount;
  Matcher um = UTR.matcher(body); if (um.find()) t.utr = um.group(1).toUpperCase(Locale.ROOT);
  Matcher vm = VPA.matcher(work);
  if (vm.find()) { t.who = vm.group(1); t.p2p = !MERCHANT.matcher(t.who).find() && !BRAND.matcher(t.who).matches(); }
  else {
   Matcher nm = TO_NAME.matcher(work);
   if (nm.find()) {
    String name = nm.group(1).replaceAll(STOP, "").replaceAll("[^A-Za-z0-9 .&'()-]+$", "").trim().replaceAll("\\s+", " ");
    if (name.length() >= 2 && name.length() <= 30) { t.who = name; t.p2p = name.split(" ").length <= 3 && !MERCHANT.matcher(name).find() && !BRAND.matcher(name).matches(); }
   }
  }
  return t;
 }

 static String pairKey(long account, String day, long amount, String who) { return account + ":" + day + ":" + amount + ":" + who.toLowerCase(Locale.ROOT); }

 static long categoryFor(Store db, T t) {
  if (t.p2p) return ensure(db, "People", "people");
  String name = CategoryIcons.existingCategory(CategoryIcons.suggest(t.who));
  if (name == null) name = "Other";
  for (Store.Cat c : db.categories("expense")) if (c.name.equalsIgnoreCase(name)) return c.id;
  return db.addCategory(name, "expense", CategoryIcons.suggest(t.who));
 }
 static long ensure(Store db, String name, String icon) {
  for (Store.Cat c : db.categories("expense")) if (c.name.equalsIgnoreCase(name)) return c.id;
  return db.addCategory(name, "expense", icon);
 }

 /** Record one SMS as an expense when it is a clear debit for a linked account. Returns true when an entry was created. */
 static boolean apply(Store db, String sender, String body, long when) { return apply(db, sender, body, when, null); }
 static synchronized boolean apply(Store db, String sender, String body, long when, Long balance) {
  if (!db.flag("smsEnabled", false) || !db.flag("smsTxEnabled", false)) return false;
  if (when > System.currentTimeMillis() + 300000L) return false;
  String custom = db.pref("smsSender:" + BankSms.senderKey(sender), "");
  T t = parse(sender, body, custom.isEmpty() ? null : custom);
  if (t == null) return false;
  long account;
  try { account = Long.parseLong(db.pref("smsMap:" + t.bank + ":" + t.last4, "0")); } catch (Exception e) { return false; }
  if (db.account(account) == null) return false;
  String day = Instant.ofEpochMilli(when).atZone(ZoneId.systemDefault()).toLocalDate().toString();
  String pair = pairKey(account, day, t.amount, t.who);
  if (!t.utr.isEmpty() && !db.pref("smsTxUtr:" + t.utr, "").isEmpty()) return false;
  if (!db.pref("smsTx:" + pair, "").isEmpty()) return false; // seen, or imported then deleted: never re-add
  Store.Tx x = new Store.Tx(); x.kind = "expense"; x.amount = t.amount; x.day = day; x.account = account; x.cat = categoryFor(db, t); x.note = (t.who.isEmpty() ? "Bank SMS expense" : pretty(t.who)) + (balance == null ? "" : " · bal " + BankSms.inr(balance));
  long id = db.save(x);
  db.setPref("smsTx:" + pair, "" + id);
  if (!t.utr.isEmpty()) db.setPref("smsTxUtr:" + t.utr, "" + id);
  return true;
 }

 static String pretty(String who) {
  if (who.contains("@")) return who;
  StringBuilder b = new StringBuilder();
  for (String w : who.split(" ")) { if (w.isEmpty()) continue; if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(w.charAt(0))); if (w.length() > 1) b.append(w.substring(1).toLowerCase(Locale.ROOT)); }
  return b.toString();
 }

 /** Imported entries, newest first. */
 static List<Store.Tx> imported(Store db) {
  List<Long> ids = new ArrayList<>();
  try (android.database.Cursor c = db.getReadableDatabase().rawQuery("SELECT v FROM prefs WHERE k LIKE 'smsTx:%'", null)) { while (c.moveToNext()) { try { ids.add(Long.parseLong(c.getString(0))); } catch (Exception ignored) {} } }
  List<Store.Tx> out = new ArrayList<>();
  for (long id : ids) { Store.Tx t = db.txById(id); if (t != null) out.add(t); }
  out.sort((p, q) -> { int d = q.day.compareTo(p.day); return d != 0 ? d : Long.compare(q.id, p.id); });
  return out;
 }

 static void clearAll(Store db) {
  db.getWritableDatabase().delete("prefs", "k LIKE 'smsTx:%' OR k LIKE 'smsTxUtr:%'", null);
 }
}
