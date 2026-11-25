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
    card.addView(row(a, accIcon(x.kind), Ui.surface2, x.name, BankSms.display(db,x.id)==null?Fmt.title(x.kind):BankSms.display(db,x.id), Privacy.amount(db,x,bal), bal < 0 ? Ui.neg : Ui.ink, () -> Sheets.form(a, "Edit account", new String[]{"Name", "Starting balance"}, new String[]{x.name, Privacy.hidden(db,x)?"":plain(x.opening)}, new int[]{Sheets.TEXT, Sheets.NUMBER}, "Save", v -> { if (!v[0].isEmpty()) { x.name = v[0]; if(!v[1].isEmpty()) x.opening = amt(v[1]); db.updateAccount(x); a.refresh(); } }, () -> { if (!db.deleteAccount(x.id)) a.toast("Keep one account, and remove its entries before deleting", null); a.refresh(); }))); }
   c.addView(card); c.addView(Ui.text(a, "Ledger balance is the starting balance plus entries. SMS balance is a separate dated bank-message snapshot. Hidden opening balances are left unchanged when the field is empty.", 12.5f, Ui.muted, false), Ui.lpm(-2, -2, 4, 12, 0, 0));
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
  Runnable add = () -> Sheets.form(a, "New repeat", new String[]{"Name", "Amount", "Next date (yyyy-mm-dd)"}, new String[]{"", "", LocalDate.now().plusDays(7).toString()}, new int[]{Sheets.TEXT, Sheets.NUMBER, InputType.TYPE_CLASS_DATETIME}, "Add", v -> {
   LocalDate d; long m;
   try { d = LocalDate.parse(v[2]); m = amt(v[1]); } catch (Exception e) { a.toast("Use a date like 2026-12-01", null); return; }
   if (v[0].isEmpty() || m <= 0) return;
   Store.Rec r = new Store.Rec(); r.title = v[0]; r.kind = "expense"; r.amount = m; r.next = d.toString(); r.active = true;
   List<Store.Acc> as = db.accounts(); r.account = as.get(0).id; long last = Long.parseLong(db.pref("lastAcc", "0")); for (Store.Acc x : as) if (x.id == last) r.account = x.id;
   Sheets.pick(a, "Repeats", Arrays.asList("Weekly", "Monthly", "Yearly"), Arrays.asList("repeat", "calendar", "flame"), 1, pi -> {
    r.period = pi == 0 ? "weekly" : pi == 2 ? "yearly" : "monthly";
    List<String> an = new ArrayList<>(), ai = new ArrayList<>(); int sel = -1;
    for (int i = 0; i < as.size(); i++) { an.add(as.get(i).name); ai.add(accIcon(as.get(i).kind)); if (as.get(i).id == r.account) sel = i; }
    Sheets.pick(a, "From account", an, ai, sel, aj -> {
     r.account = as.get(aj).id;
     List<Store.Cat> cs = db.categories("expense"); List<String> cn = new ArrayList<>(), ci = new ArrayList<>(); int csel = -1;
     for (int i = 0; i < cs.size(); i++) { cn.add(cs.get(i).name); ci.add(cs.get(i).icon); if (cs.get(i).name.equals("Bills")) csel = i; }
     cn.add("No category"); ci.add("other");
     Sheets.pick(a, "Category", cn, ci, csel, ck -> { if (ck < cs.size()) r.cat = cs.get(ck).id; db.saveRec(r); a.refresh(); });
    });
   });
  }, null);
  Kit.page(a, "Bills and repeats", "plus", add, c -> {
   c.addView(Ui.text(a, "Nothing is added by itself. When a bill is due, you confirm it on the home screen.", 13.5f, Ui.muted, false), Ui.lpm(-2, -2, 2, 0, 0, 14));
   List<Store.Rec> rs = db.recurring(); if (rs.isEmpty()) { LinearLayout em = Ui.card(a); em.addView(Kit.empty(a, "repeat", "No repeats", "Add rent, a subscription or any monthly bill.")); c.addView(em); return; }
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); LocalDate today = LocalDate.now();
   for (int i = 0; i < rs.size(); i++) { Store.Rec r = rs.get(i); if (i > 0) card.addView(Ui.divider(a));
    String sub = Fmt.title(r.period) + " · next " + Fmt.dayLabel(r.next) + (r.active ? "" : " · paused");
    card.addView(row(a, "repeat", Ui.surface2, r.title, sub, Fmt.money(r.amount), r.active ? Ui.ink : Ui.muted, () -> Sheets.pick(a, r.title, Arrays.asList("Add it now", "Skip this one", r.active ? "Pause" : "Resume", "Delete"), Arrays.asList("check", "right", r.active ? "close" : "check", "trash"), -1, k -> {
     if (k == 0) { db.confirm(r); a.toast(r.title + " added", null); }
     else if (k == 1) db.skip(r);
     else if (k == 2) { r.active = !r.active; db.saveRec(r); }
     else db.deleteRec(r.id);
     a.refresh(); }))); }
   c.addView(card);
  });
 }

 static void iconPicker(MainActivity a,String current,java.util.function.Consumer<String> done){
  LinearLayout body=Sheets.sheetBody(a,"Choose an icon"); MainActivity.Layer[] layer=new MainActivity.Layer[1];
  TextView auto=Ui.button(a,L.t("Suggested icon")+": "+L.t(Fmt.title(current)),true);body.addView(auto,Ui.lpm(-1,-2,0,0,0,12));Ui.tap(auto,()->{a.close(layer[0]);done.accept(current);});
  String[] icons=Icons.CATEGORY_ICONS;
  for(int i=0;i<icons.length;i+=4){LinearLayout row=Ui.row(a);for(int j=i;j<Math.min(i+4,icons.length);j++){String icon=icons[j];LinearLayout cell=Ui.col(a);cell.setGravity(Gravity.CENTER);cell.setPadding(Ui.dp(6),Ui.dp(12),Ui.dp(6),Ui.dp(12));cell.addView(Ui.badge(a,icon,48,icon.equals(current)?Ui.accent:Ui.surface2,Ui.ink));cell.addView(Ui.text(a,Fmt.title(icon),11,Ui.muted,false));row.addView(cell,Ui.weight(1));Ui.tap(cell,()->{a.close(layer[0]);done.accept(icon);});}body.addView(row);}
  layer[0]=Sheets.show(a,body);
 }
 static void categories(MainActivity a) {
  Store db = a.db;
  Runnable add = () -> Sheets.pick(a, "New category for", Arrays.asList("Spending", "Income"), Arrays.asList("up", "download"), -1, i -> Sheets.form(a, "New category", new String[]{"Name"}, new String[]{""}, new int[]{Sheets.TEXT}, "Add", v -> {
   if (v[0].isEmpty()) return; List<String> ic = new ArrayList<>(Arrays.asList(Icons.CATEGORY_ICONS)); iconPicker(a,CategoryIcons.suggest(v[0]),icon -> { db.addCategory(v[0], i == 0 ? "expense" : "income", icon); a.refresh(); }); }, null));
  Kit.page(a, "Categories", "plus", add, c -> {
   for (String kind : new String[]{"expense", "income"}) { c.addView(Ui.label(a, kind.equals("expense") ? "Spending" : "Income")); ((LinearLayout.LayoutParams) c.getChildAt(c.getChildCount() - 1).getLayoutParams()).bottomMargin = Ui.dp(8);
    LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); List<Store.Cat> cs = db.categories(kind);
    for (int i = 0; i < cs.size(); i++) { Store.Cat x = cs.get(i); if (i > 0) card.addView(Ui.divider(a)); card.addView(row(a, x.icon, Kit.tone(x.id), x.name, null, null, 0, () -> Sheets.form(a, "Edit category", new String[]{"Name"}, new String[]{x.name}, new int[]{Sheets.TEXT}, "Save", v -> { if (!v[0].isEmpty()) { x.name = v[0]; db.updateCategory(x); a.refresh(); iconPicker(a,CategoryIcons.suggest(x.name),icon->{x.icon=icon;db.updateCategory(x);a.refresh();}); } }, () -> { db.deleteCategory(x.id); a.refresh(); }))); }
    c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 20)); }
  });
 }

 static void split(MainActivity a) {
  Store db = a.db;
  LinearLayout c = Sheets.sheetBody(a, "Split a bill");
  EditText total = Sheets.field(a, "Total bill", "", Sheets.NUMBER); c.addView(total, Ui.lpm(-1, -2, 0, 4, 0, 0));
  TextView cl = Ui.label(a, "Category"); cl.setPadding(Ui.dp(2), Ui.dp(14), 0, Ui.dp(6)); c.addView(cl);
  List<Store.Cat> cats = db.categories("expense");
  final long[] cat = {0}; for (Store.Cat ct : cats) if (ct.name.equals("Food")) cat[0] = ct.id;
  HorizontalScrollView chs = new HorizontalScrollView(a); chs.setHorizontalScrollBarEnabled(false); LinearLayout crow = Ui.row(a); chs.addView(crow); c.addView(chs);
  for (Store.Cat ct : cats) { TextView p = Ui.pill(a, ct.name, ct.id == cat[0]); p.setTag(ct.id); crow.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0));
   Ui.tap(p, () -> { cat[0] = ct.id; for (int i = 0; i < crow.getChildCount(); i++) { TextView q = (TextView) crow.getChildAt(i); boolean on = ((Long) q.getTag()) == ct.id; q.setTextColor(on ? Ui.onAccent : Ui.ink); q.setBackground(on ? Ui.shape(Ui.accent, 20) : Ui.outline(0, Ui.line, 20, 1)); } }); }
  TextView pl = Ui.label(a, "Who's in (besides you)"); pl.setPadding(Ui.dp(2), Ui.dp(14), 0, Ui.dp(6)); c.addView(pl);
  List<Store.Person> ps = db.people(); final Set<Long> sel = new LinkedHashSet<>();
  if (ps.isEmpty()) { c.addView(Ui.text(a, "No people yet - use the count below, or add people first.", 13f, Ui.muted, false)); }
  for (Store.Person person : ps) {
   LinearLayout r = Ui.row(a); r.setPadding(Ui.dp(4), Ui.dp(9), Ui.dp(4), Ui.dp(9));
   FrameLayout tick = Ui.badge(a, "check", 30, Ui.surface2, Ui.surface2);
   r.addView(tick, Ui.lpm(Ui.dp(30), Ui.dp(30), 0, 0, 12, 0));
   r.addView(Ui.text(a, person.name, 15f, Ui.ink, false), Ui.weight(1));
   Ui.tap(r, () -> { boolean on = !sel.contains(person.id); if (on) sel.add(person.id); else sel.remove(person.id); tick.setBackground(Ui.shape(on ? Ui.accent : Ui.surface2, 15)); ((FrameLayout) tick).removeAllViews(); tick.addView(Ui.icon(a, "check", 16, on ? Ui.onAccent : Ui.surface2), new FrameLayout.LayoutParams(Ui.dp(16), Ui.dp(16), Gravity.CENTER)); });
   c.addView(r);
  }
  EditText count = Sheets.field(a, "...or how many people (you included)", "2", InputType.TYPE_CLASS_NUMBER); c.addView(count, Ui.lpm(-1, -2, 0, 10, 0, 0));
  TextView dl = Ui.label(a, "Direction"); dl.setPadding(Ui.dp(2), Ui.dp(14), 0, Ui.dp(6)); c.addView(dl);
  LinearLayout dpills = Ui.row(a); final boolean[] owe = {true};
  TextView oweMe = Ui.pill(a, "They owe you", true), iOwe = Ui.pill(a, "You owe them", false);
  dpills.addView(oweMe, Ui.lpm(-2, -2, 0, 0, 8, 0)); dpills.addView(iOwe); c.addView(dpills);
  Runnable dir = () -> { oweMe.setTextColor(owe[0] ? Ui.onAccent : Ui.ink); oweMe.setBackground(owe[0] ? Ui.shape(Ui.accent, 20) : Ui.outline(0, Ui.line, 20, 1)); iOwe.setTextColor(!owe[0] ? Ui.onAccent : Ui.ink); iOwe.setBackground(!owe[0] ? Ui.shape(Ui.accent, 20) : Ui.outline(0, Ui.line, 20, 1)); };
  Ui.tap(oweMe, () -> { owe[0] = true; dir.run(); }); Ui.tap(iOwe, () -> { owe[0] = false; dir.run(); });
  TextView go = Ui.button(a, "Work it out", true); c.addView(go, Ui.lpm(-1, -2, 0, 18, 0, 0));
  Ui.tap(go, () -> {
   long t = amt(total.getText().toString());
   int n = sel.size() + 1;
   if (sel.isEmpty()) { try { n = Integer.parseInt(count.getText().toString().trim()); } catch (Exception e) { n = 0; } }
   if (t <= 0 || n < 2 || n > 50) { a.toast("Enter a total and pick people (or 2 to 50)", null); return; }
   long base = t / n, extra = t % n; final long mine = base + (extra > 0 ? 1 : 0); final int people = n; final long totalV = t;
   if (owe[0] && !sel.isEmpty()) {
    List<Long> ids = new ArrayList<>();
    List<Store.Acc> as = db.accounts(); long acc = as.isEmpty() ? 0 : as.get(0).id;
    for (long pid : sel) { Store.Tx x = new Store.Tx(); x.kind = "lent"; x.amount = base; x.day = LocalDate.now().toString(); x.account = acc; x.person = pid; x.cat = cat[0]; x.note = "Shared bill"; ids.add(db.save(x)); }
    a.refresh();
    LinearLayout r2 = Sheets.sheetBody(a, "Saved - each owes " + Fmt.money(base));
    r2.addView(Ui.text(a, sel.size() + " people owe you their share of " + Fmt.money(totalV) + ". It is on their page in People.", 14.5f, Ui.ink, false));
    TextView done = Ui.button(a, "Done", true); r2.addView(done, Ui.lpm(-1, -2, 0, 18, 0, 0));
    final MainActivity.Layer[] r2ref = new MainActivity.Layer[1];
    Ui.tap(done, () -> a.close(r2ref[0])); r2ref[0] = Sheets.show(a, r2);
    a.toast("Saved " + sel.size() + " shares", () -> { for (long id : ids) db.deleteTx(id); a.refresh(); });
    return;
   }
   LinearLayout r2 = Sheets.sheetBody(a, "Each pays " + Fmt.money(mine)); if (extra > 0) r2.addView(Ui.text(a, extra + " of " + people + " people pay one paisa less so the total matches exactly.", 13, Ui.muted, false), Ui.lpm(-2, -2, 0, 0, 0, 14));
   TextView t2 = Ui.text(a, "Total " + Fmt.money(totalV) + " across " + people + " people.", 14.5f, Ui.ink, false); r2.addView(t2);
   final MainActivity.Layer[] ref = new MainActivity.Layer[1]; TextView add = Ui.button(a, "Add my share as spent", true); r2.addView(add, Ui.lpm(-1, -2, 0, 18, 0, 0));
   Ui.tap(add, () -> { Store.Tx x = new Store.Tx(); x.kind = "expense"; x.amount = mine; x.day = LocalDate.now().toString(); List<Store.Acc> as2 = db.accounts(); x.account = as2.get(0).id; x.note = "Shared bill"; x.cat = cat[0]; long id = db.save(x); a.close(ref[0]); a.refresh(); a.toast("Added your share " + Fmt.money(mine), () -> { db.deleteTx(id); a.refresh(); }); });
   ref[0] = Sheets.show(a, r2);
  });
  Sheets.show(a, c);
 }
}
