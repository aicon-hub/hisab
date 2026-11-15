package in.aicon.hisab;

import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

/** Secondary pages: accounts, budgets, goals, recurring, categories, split. */
final class Pages {
 private Pages() {}
 static String accIcon(String kind) { return kind.equals("cash") ? "cash" : kind.equals("bank") ? "bank" : kind.equals("card") ? "card" : "wallet"; }
 static long amt(String s) { return s.isEmpty() ? 0 : Ledger.parse(s.replace(",", "")); }
 static String plain(long m) { return EntrySheet.Ledger_plain(m); }
 static View row(MainActivity a, String icon, int tone, String title, String sub, String right, int rightColor, Runnable tap) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(11), 0, Ui.dp(11)); r.addView(Ui.badge(a, icon, 42, tone, tone == Ui.surface2 ? Ui.ink : 0xFF14151A), Ui.lpm(Ui.dp(42), Ui.dp(42), 0, 0, 14, 0));
  LinearLayout m = Ui.col(a); m.addView(Ui.text(a, title, 15.5f, Ui.ink, true)); if (sub != null) { TextView s = Ui.text(a, sub, 12.5f, Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s); } r.addView(m, Ui.weight(1));
  if (right != null) r.addView(Ui.text(a, right, 14.5f, rightColor, true)); if (tap != null) Ui.tap(r, tap); return r;
 }

 static void accounts(MainActivity a) {
  Store db = a.db; String[] kinds = {"cash", "bank", "card", "wallet"};
  Runnable add = () -> Sheets.pick(a, "Account type", Arrays.asList("Cash", "Bank", "Card", "Wallet"), Arrays.asList("cash", "bank", "card", "wallet"), -1, i -> Sheets.form(a, "New account", new String[]{"Name", "Starting balance"}, new String[]{"", ""}, new int[]{Sheets.TEXT, Sheets.NUMBER}, "Add", v -> { if (!v[0].isEmpty()) { db.addAccount(v[0], kinds[i], amt(v[1])); a.refresh(); } }, null));
  Kit.page(a, "Accounts", "plus", add, c -> {
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); List<Store.Acc> as = db.accounts();
   for (int i = 0; i < as.size(); i++) { Store.Acc x = as.get(i); if (i > 0) card.addView(Ui.divider(a)); long bal = db.balance(x);
    card.addView(row(a, accIcon(x.kind), Ui.surface2, x.name, BankSms.display(db,x.id)==null?Fmt.title(x.kind):BankSms.display(db,x.id), Fmt.money(bal), bal < 0 ? Ui.neg : Ui.ink, () -> Sheets.form(a, "Edit account", new String[]{"Name", "Starting balance"}, new String[]{x.name, plain(x.opening)}, new int[]{Sheets.TEXT, Sheets.NUMBER}, "Save", v -> { if (!v[0].isEmpty()) { x.name = v[0]; x.opening = amt(v[1]); db.updateAccount(x); a.refresh(); } }, () -> { if (!db.deleteAccount(x.id)) a.toast("Keep one account, and remove its entries before deleting", null); a.refresh(); }))); }
   c.addView(card); c.addView(Ui.text(a, "Balance is the starting balance plus every entry on the account.", 12.5f, Ui.muted, false), Ui.lpm(-2, -2, 4, 12, 0, 0));
  });
 }

 static void budgets(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Budgets", null, null, c -> {
   YearMonth_ ym = new YearMonth_(); Map<Long, Long> b = db.budgets(), spent = new HashMap<>(); for (Store.Tx t : db.txs(Fmt.from(ym.v), Fmt.to(ym.v))) if (t.kind.equals("expense")) spent.merge(t.cat, t.amount, Long::sum);
   c.addView(Ui.text(a, "Monthly limits. The home screen turns them into a safe amount for today.", 13.5f, Ui.muted, false), Ui.lpm(-2, -2, 2, 0, 0, 14));
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); List<Store.Cat> cs = db.categories("expense");
   for (int i = 0; i < cs.size(); i++) { Store.Cat x = cs.get(i); if (i > 0) card.addView(Ui.divider(a)); long lim = b.getOrDefault(x.id, 0L), used = spent.getOrDefault(x.id, 0L);
    card.addView(row(a, x.icon, Kit.tone(x.id), x.name, lim > 0 ? Fmt.money(used) + " of " + Fmt.money(lim) : "No limit", lim > 0 ? (used > lim ? "Over" : Fmt.compact(lim - used) + " left") : "Set", lim > 0 && used > lim ? Ui.neg : Ui.muted, () -> Sheets.form(a, x.name + " budget", new String[]{"Monthly limit (0 removes)"}, new String[]{plain(lim)}, new int[]{Sheets.NUMBER}, "Save", v -> { db.setBudget(x.id, amt(v[0])); a.refresh(); }, null))); }
   c.addView(card);
  });
 }
 static final class YearMonth_ { final java.time.YearMonth v = java.time.YearMonth.now(); }

 static void goals(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Goals", "plus", () -> Sheets.form(a, "New goal", new String[]{"What are you saving for", "Target amount"}, new String[]{"", ""}, new int[]{Sheets.TEXT, Sheets.NUMBER}, "Add", v -> { if (!v[0].isEmpty() && amt(v[1]) > 0) { Store.Goal g = new Store.Goal(); g.name = v[0]; g.target = amt(v[1]); g.icon = "target"; db.saveGoal(g); a.refresh(); } }, null), c -> {
   List<Store.Goal> gs = db.goals(); if (gs.isEmpty()) { LinearLayout em = Ui.card(a); em.addView(Kit.empty(a, "target", "No goals yet", "Add one and move money towards it.")); c.addView(em); }
   for (Store.Goal g : gs) { LinearLayout card = Ui.card(a); float f = g.target == 0 ? 0 : Math.min(1f, g.saved / (float) g.target); LinearLayout top = Ui.row(a); top.addView(Ui.badge(a, g.icon, 44, Ui.accent, Ui.onAccent), Ui.lpm(Ui.dp(44), Ui.dp(44), 0, 0, 14, 0)); LinearLayout m = Ui.col(a); m.addView(Ui.text(a, g.name, 16, Ui.ink, true)); TextView s = Ui.text(a, Fmt.money(g.saved) + " of " + Fmt.money(g.target), 13, Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s); top.addView(m, Ui.weight(1)); top.addView(Ui.big(a, Math.round(f * 100) + "%", 20, Ui.ink)); card.addView(top);
    card.addView(new Charts.Pace(a, f, -1, Ui.dark ? Ui.accent : Ui.ink, Ui.surface2), Ui.lpm(-1, Ui.dp(24), 0, 12, 0, 0));
    Ui.tap(card, () -> Sheets.pick(a, g.name, Arrays.asList("Add money", "Edit goal", "Delete goal"), Arrays.asList("plus", "edit", "trash"), -1, i -> {
     if (i == 0) Sheets.form(a, "Add to " + g.name, new String[]{"Amount"}, new String[]{""}, new int[]{Sheets.NUMBER}, "Add", v -> { long x = amt(v[0]); if (x > 0) { g.saved += x; db.saveGoal(g); a.refresh(); } }, null);
     else if (i == 1) Sheets.form(a, "Edit goal", new String[]{"Name", "Target", "Saved so far"}, new String[]{g.name, plain(g.target), plain(g.saved)}, new int[]{Sheets.TEXT, Sheets.NUMBER, Sheets.NUMBER}, "Save", v -> { if (!v[0].isEmpty() && amt(v[1]) > 0) { g.name = v[0]; g.target = amt(v[1]); g.saved = amt(v[2]); db.saveGoal(g); a.refresh(); } }, null);
     else Sheets.confirm(a, "Delete " + g.name + "?", "The goal is removed. Your entries are not touched.", "Delete", () -> { db.deleteGoal(g.id); a.refresh(); }); }));
    c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 12)); }
  });
 }

 static void recurring(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Bills and repeats", "plus", () -> Sheets.form(a, "New repeat", new String[]{"Name", "Amount", "Next date (yyyy-mm-dd)"}, new String[]{"", "", LocalDate.now().plusDays(7).toString()}, new int[]{Sheets.TEXT, Sheets.NUMBER, InputType.TYPE_CLASS_DATETIME}, "Add", v -> {
   try { LocalDate d = LocalDate.parse(v[2]); long m = amt(v[1]); if (v[0].isEmpty() || m <= 0) return; Store.Rec r = new Store.Rec(); r.title = v[0]; r.kind = "expense"; r.amount = m; List<Store.Acc> as = db.accounts(); r.account = as.get(0).id; long last = Long.parseLong(db.pref("lastAcc", "0")); for (Store.Acc x : as) if (x.id == last) r.account = x.id; for (Store.Cat ct : db.categories("expense")) if (ct.name.equals("Bills")) r.cat = ct.id; r.period = "monthly"; r.next = d.toString(); r.active = true; db.saveRec(r); a.refresh(); } catch (Exception e) { a.toast("Use a date like 2026-12-01", null); } }, null), c -> {
   c.addView(Ui.text(a, "Nothing is added by itself. When a bill is due, you confirm it on the home screen.", 13.5f, Ui.muted, false), Ui.lpm(-2, -2, 2, 0, 0, 14));
   List<Store.Rec> rs = db.recurring(); if (rs.isEmpty()) { LinearLayout em = Ui.card(a); em.addView(Kit.empty(a, "repeat", "No repeats", "Add rent, a subscription or any monthly bill.")); c.addView(em); return; }
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); LocalDate today = LocalDate.now();
   for (int i = 0; i < rs.size(); i++) { Store.Rec r = rs.get(i); if (i > 0) card.addView(Ui.divider(a)); card.addView(row(a, "repeat", Ui.surface2, r.title, "Monthly · next " + Fmt.dayLabel(r.next), Fmt.money(r.amount), Ui.ink, () -> Sheets.pick(a, r.title, Arrays.asList("Add it now", "Skip this one", "Delete"), Arrays.asList("check", "right", "trash"), -1, k -> { if (k == 0) { db.confirm(r); a.toast(r.title + " added", null); } else if (k == 1) db.skip(r); else db.deleteRec(r.id); a.refresh(); }))); }
   c.addView(card);
  });
 }

 static void iconPicker(MainActivity a,String current,java.util.function.Consumer<String> done){
  LinearLayout body=Sheets.sheetBody(a,"Choose an icon"); MainActivity.Layer[] layer=new MainActivity.Layer[1];
  String[] icons=Icons.CATEGORY_ICONS;
  for(int i=0;i<icons.length;i+=4){LinearLayout row=Ui.row(a);for(int j=i;j<Math.min(i+4,icons.length);j++){String icon=icons[j];LinearLayout cell=Ui.col(a);cell.setGravity(Gravity.CENTER);cell.setPadding(Ui.dp(6),Ui.dp(12),Ui.dp(6),Ui.dp(12));cell.addView(Ui.badge(a,icon,48,icon.equals(current)?Ui.accent:Ui.surface2,Ui.ink));cell.addView(Ui.text(a,Fmt.title(icon),11,Ui.muted,false));row.addView(cell,Ui.weight(1));Ui.tap(cell,()->{a.close(layer[0]);done.accept(icon);});}body.addView(row);}
  layer[0]=Sheets.show(a,body);
 }
 static void categories(MainActivity a) {
  Store db = a.db;
  Runnable add = () -> Sheets.pick(a, "New category for", Arrays.asList("Spending", "Income"), Arrays.asList("up", "download"), -1, i -> Sheets.form(a, "New category", new String[]{"Name"}, new String[]{""}, new int[]{Sheets.TEXT}, "Add", v -> {
   if (v[0].isEmpty()) return; List<String> ic = new ArrayList<>(Arrays.asList(Icons.CATEGORY_ICONS)); iconPicker(a,"other",icon -> { db.addCategory(v[0], i == 0 ? "expense" : "income", icon); a.refresh(); }); }, null));
  Kit.page(a, "Categories", "plus", add, c -> {
   for (String kind : new String[]{"expense", "income"}) { c.addView(Ui.label(a, kind.equals("expense") ? "Spending" : "Income")); ((LinearLayout.LayoutParams) c.getChildAt(c.getChildCount() - 1).getLayoutParams()).bottomMargin = Ui.dp(8);
    LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); List<Store.Cat> cs = db.categories(kind);
    for (int i = 0; i < cs.size(); i++) { Store.Cat x = cs.get(i); if (i > 0) card.addView(Ui.divider(a)); card.addView(row(a, x.icon, Kit.tone(x.id), x.name, null, null, 0, () -> Sheets.form(a, "Edit category", new String[]{"Name"}, new String[]{x.name}, new int[]{Sheets.TEXT}, "Save", v -> { if (!v[0].isEmpty()) { x.name = v[0]; db.updateCategory(x); a.refresh(); iconPicker(a,x.icon,icon->{x.icon=icon;db.updateCategory(x);a.refresh();}); } }, () -> { db.deleteCategory(x.id); a.refresh(); }))); }
    c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 20)); }
  });
 }

 static void split(MainActivity a) {
  Store db = a.db;
  Sheets.form(a, "Split a bill", new String[]{"Total bill", "How many people (you included)"}, new String[]{"", "2"}, new int[]{Sheets.NUMBER, InputType.TYPE_CLASS_NUMBER}, "Work it out", v -> {
   long total = amt(v[0]); int n; try { n = Integer.parseInt(v[1]); } catch (Exception e) { n = 0; } if (total <= 0 || n < 2 || n > 50) { a.toast("Enter a total and 2 to 50 people", null); return; }
   long base = total / n, extra = total % n; final long mine = base + (extra > 0 ? 1 : 0); final int people = n;
   LinearLayout c = Sheets.sheetBody(a, "Each pays " + Fmt.money(base + (extra > 0 ? 1 : 0))); if (extra > 0) c.addView(Ui.text(a, extra + " of " + n + " people pay one paisa less so the total matches exactly.", 13, Ui.muted, false), Ui.lpm(-2, -2, 0, 0, 0, 14));
   TextView t = Ui.text(a, "Total " + Fmt.money(total) + " across " + n + " people.", 14.5f, Ui.ink, false); c.addView(t);
   final MainActivity.Layer[] ref = new MainActivity.Layer[1]; TextView add = Ui.button(a, "Add my share as spent", true); c.addView(add, Ui.lpm(-1, -2, 0, 18, 0, 0));
   Ui.tap(add, () -> { Store.Tx x = new Store.Tx(); x.kind = "expense"; x.amount = mine; x.day = LocalDate.now().toString(); List<Store.Acc> as = db.accounts(); x.account = as.get(0).id; x.note = "Shared bill"; for (Store.Cat ct : db.categories("expense")) if (ct.name.equals("Food")) x.cat = ct.id; long id = db.save(x); a.close(ref[0]); a.refresh(); a.toast("Added your share " + Fmt.money(mine), () -> { db.deleteTx(id); a.refresh(); }); });
   ref[0] = Sheets.show(a, c); }, null);
 }
}
