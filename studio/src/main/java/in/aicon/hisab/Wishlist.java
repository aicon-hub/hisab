package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

final class Wishlist {
 private Wishlist() {}

 static final class Item { String name; long price; }

 static List<Item> read(Store db) {
  List<Item> out = new ArrayList<>();
  for (String line : db.pref("wishlist", "").split("\n")) { int t = line.indexOf('\t'); if (t < 1) continue; try { Item it = new Item(); it.price = Long.parseLong(line.substring(0, t)); it.name = line.substring(t + 1); if (it.price > 0 && !it.name.isEmpty()) out.add(it); } catch (Exception e) {} }
  return out;
 }

 static void write(Store db, List<Item> items) {
  StringBuilder b = new StringBuilder();
  for (Item it : items) b.append(it.price).append('\t').append(it.name.replace('\t', ' ').replace('\n', ' ')).append('\n');
  db.setPref("wishlist", b.toString());
 }

 static void open(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Wishlist", "plus", () -> Sheets.form(a, "New wish", new String[]{"What do you want", "Price"}, new String[]{"", ""}, new int[]{Sheets.TEXT, Sheets.NUMBER}, "Add", v -> { long p = v[1].trim().isEmpty() ? 0 : Ledger.parse(v[1].trim()); if (!v[0].isEmpty() && p > 0) { List<Item> items = read(db); Item it = new Item(); it.name = v[0]; it.price = p; items.add(it); write(db, items); a.closeAll(); open(a); } }, null), c -> {
   List<Item> items = read(db);
   YearMonth ym = YearMonth.now(); long in = 0, out = 0; for (Store.Tx t : db.txs(Fmt.from(ym), Fmt.to(ym))) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) out += t.amount; }
   long kept = in - out;
   long totalBudget = 0; for (long v : db.budgets().values()) totalBudget += v;
   long budgeted = 0; if (totalBudget > 0) for (Store.Tx t : db.txs(Fmt.from(ym), Fmt.to(ym))) if (t.kind.equals("expense") && db.budgets().containsKey(t.cat)) budgeted += t.amount;
   LocalDate today = LocalDate.now(); int daysLeft = ym.lengthOfMonth() - today.getDayOfMonth() + 1;
   long safeToday = totalBudget > 0 ? Math.max(0, totalBudget - budgeted) / daysLeft : 0;

   long total = 0; for (Item it : items) total += it.price;
   LinearLayout hero = Ui.col(a); hero.setBackground(Ui.shape(Ui.accent, 30)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
   hero.addView(Ui.text(a, "Wishes", 13, Ui.onAccentSub, true));
   TextView big = Ui.big(a, Fmt.money(total), 36, Ui.onAccent); big.setPadding(0, Ui.dp(8), 0, Ui.dp(6)); hero.addView(big);
   hero.addView(Ui.text(a, items.isEmpty() ? "Save the things you want here and see them against what you can spend." : items.size() + (items.size() == 1 ? " wish" : " wishes") + " on the list", 13, Ui.onAccentSub, false));
   c.addView(hero);

   if (!items.isEmpty()) {
    LinearLayout card = Ui.card(a);
    for (int i = 0; i < items.size(); i++) { final int idx = i; Item it = items.get(i); if (i > 0) card.addView(Ui.divider(a));
     LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(11), 0, Ui.dp(11));
     r.addView(Ui.badge(a, "spark", 36, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(36), Ui.dp(36), 0, 0, 12, 0));
     LinearLayout m = Ui.col(a); m.addView(Ui.text(a, it.name, 15, Ui.ink, true));
     m.addView(Ui.text(a, afford(it.price, safeToday, kept), 12, Ui.muted, false)); r.addView(m, Ui.weight(1));
     r.addView(Ui.big(a, Fmt.money(it.price), 16, Ui.ink)); card.addView(r);
     Ui.tap(r, () -> Sheets.form(a, "Edit wish", new String[]{"Name", "Price"}, new String[]{it.name, Ledger.plain(it.price)}, new int[]{Sheets.TEXT, Sheets.NUMBER}, "Save", v -> { List<Item> now = read(db); if (idx < now.size()) { long p = v[1].trim().isEmpty() ? 0 : Ledger.parse(v[1].trim()); if (!v[0].isEmpty() && p > 0) { now.get(idx).name = v[0]; now.get(idx).price = p; } } write(db, now); a.closeAll(); open(a); }, () -> { List<Item> now = read(db); if (idx < now.size()) now.remove(idx); write(db, now); a.closeAll(); open(a); })); }
    c.addView(card);
   }
  });
 }

 static String afford(long price, long safeToday, long kept) {
  if (safeToday > 0) { if (price <= safeToday) return "Within today's safe amount"; long days = (price + safeToday - 1) / safeToday; return "About " + days + " days of safe spending"; }
  if (kept > 0) { if (price <= kept) return Math.round(100.0 * price / kept) + "% of what you kept this month"; return "More than you kept this month"; }
  return "Set budgets to see this against safe spending";
 }
}
