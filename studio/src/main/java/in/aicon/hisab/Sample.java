package in.aicon.hisab;

import java.time.*;
import java.util.*;

/** Synthetic sample ledger so a fresh install shows every screen. Removable from Settings. */
final class Sample {
 private Sample() {}
 static void seed(Store db) {
  db.wipe(); db.seedBasics();
  List<Store.Acc> acc = db.accounts(); long cash = acc.get(0).id, bank = acc.get(1).id;
  Store.Acc a = acc.get(0); a.opening = 2500000; db.updateAccount(a); a = acc.get(1); a.opening = 6200000; db.updateAccount(a);
  long upi = db.addAccount("Wallet", "wallet", 150000);
  Map<String, Long> cat = new HashMap<>(); for (Store.Cat c : db.categories(null)) cat.put(c.name, c.id);
  Random r = new Random(11); LocalDate today = LocalDate.now();
  String[][] pool = {{"Food", "Tea", "40", "90"}, {"Food", "Lunch", "120", "260"}, {"Food", "Dinner out", "280", "900"}, {"Groceries", "Groceries", "250", "1800"}, {"Transport", "Auto", "60", "220"}, {"Transport", "Metro card", "300", "600"}, {"Fuel", "Petrol", "400", "1200"}, {"Shopping", "Clothes", "700", "3200"}, {"Fun", "Movie", "250", "700"}, {"Bills", "Electricity", "600", "1500"}, {"Health", "Pharmacy", "120", "800"}, {"Education", "Books", "200", "1500"}, {"Other", "Misc", "50", "400"}};
  double[] w = {9, 7, 2, 4, 6, 1, 2, 1.2, 1.2, .5, 1, .5, 1.5}; double tw = 0; for (double x : w) tw += x;
  for (int back = 92; back >= 0; back--) {
   LocalDate d = today.minusDays(back); int n = r.nextInt(3) + (d.getDayOfWeek().getValue() >= 6 ? 1 : 0);
   for (int i = 0; i < n; i++) {
    double x = r.nextDouble() * tw; int k = 0; while (k < w.length - 1 && x > w[k]) { x -= w[k]; k++; }
    String[] p = pool[k]; long lo = Long.parseLong(p[2]), hi = Long.parseLong(p[3]); long amt = (lo + (long) (r.nextDouble() * (hi - lo))) / 5 * 5 * 100;
    Store.Tx t = new Store.Tx(); t.kind = "expense"; t.amount = amt; t.day = d.toString(); t.account = r.nextInt(3) == 0 ? cash : (r.nextInt(2) == 0 ? upi : bank); t.cat = cat.get(p[0]); t.note = p[1]; db.save(t);
   }
   if (d.getDayOfMonth() == 1) { Store.Tx t = new Store.Tx(); t.kind = "income"; t.amount = 5200000; t.day = d.toString(); t.account = bank; t.cat = cat.get("Salary"); t.note = "Salary"; db.save(t); }
   if (d.getDayOfMonth() == 9 && back < 80) { Store.Tx t = new Store.Tx(); t.kind = "income"; t.amount = 850000; t.day = d.toString(); t.account = bank; t.cat = cat.get("Other income"); t.note = "Side work"; db.save(t); }
  }
  long anu = db.addPerson("Anu", ""), rahul = db.addPerson("Rahul", ""), sana = db.addPerson("Sana", "");
  person(db, "lent", 150000, today.minusDays(12), cash, anu, "Lent for books");
  person(db, "borrowed", 80000, today.minusDays(6), cash, rahul, "Borrowed for fuel");
  person(db, "lent", 300000, today.minusDays(30), bank, sana, "Lent for rent");
  person(db, "repay_in", 100000, today.minusDays(9), bank, sana, "Part payment");
  for (String[] b : new String[][]{{"Food", "6000"}, {"Groceries", "8000"}, {"Transport", "3500"}, {"Shopping", "5000"}, {"Fun", "2500"}, {"Fuel", "3000"}}) db.setBudget(cat.get(b[0]), Long.parseLong(b[1]) * 100);
  Store.Goal g = new Store.Goal(); g.name = "New laptop"; g.target = 6000000; g.saved = 2250000; g.icon = "phone"; db.saveGoal(g);
  g = new Store.Goal(); g.name = "Trip fund"; g.target = 2500000; g.saved = 900000; g.icon = "travel"; db.saveGoal(g);
  rec(db, "Rent", 1200000, bank, cat.get("Bills"), today.plusDays(8));
  rec(db, "Internet", 59900, bank, cat.get("Bills"), today.plusDays(2));
  rec(db, "Phone plan", 29900, upi, cat.get("Bills"), today.plusDays(4));
  db.setPref("sample", "1"); db.setPref("name", "You");
 }
 static void person(Store db, String kind, long amt, LocalDate d, long acc, long person, String note) { Store.Tx t = new Store.Tx(); t.kind = kind; t.amount = amt; t.day = d.toString(); t.account = acc; t.person = person; t.note = note; db.save(t); }
 static void rec(Store db, String title, long amt, long acc, long cat, LocalDate next) { Store.Rec r = new Store.Rec(); r.title = title; r.kind = "expense"; r.amount = amt; r.account = acc; r.cat = cat; r.period = "monthly"; r.next = next.toString(); r.active = true; db.saveRec(r); }
}
