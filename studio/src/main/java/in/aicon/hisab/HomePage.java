package in.aicon.hisab;

import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.time.*;
import java.util.*;

final class HomePage {
 private HomePage() {}

 static View build(MainActivity a) {
  Store db = a.db; ScrollView[] sv = new ScrollView[1]; LinearLayout c = Kit.column(a, sv, true);
  LocalDate today = LocalDate.now(); YearMonth ym = YearMonth.from(today);
  List<Store.Tx> month = db.txs(Fmt.from(ym), Fmt.to(ym)); Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> people = Kit.peopleMap(db);
  long spent = 0, income = 0, budgeted = 0; Map<Long, Long> budget = db.budgets(), byCat = new HashMap<>();
  for (Store.Tx t : month) { if (t.kind.equals("expense")) { spent += t.amount; byCat.merge(t.cat, t.amount, Long::sum); if (budget.containsKey(t.cat)) budgeted += t.amount; } else if (t.kind.equals("income")) income += t.amount; }
  long totalBudget = 0; for (long v : budget.values()) totalBudget += v;
  String name = db.pref("name", "You");
  c.addView(Kit.title(a, today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, L.locale()) + ", " + Fmt.shortDate(today.toString()), "Hello, " + name, true));

  boolean secHero = HomeCustom.on(db, "hero");
  LinearLayout hero = Ui.col(a); hero.setBackground(Ui.shape(Ui.accent, 30)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
  int daysLeft = ym.lengthOfMonth() - today.getDayOfMonth() + 1; long left = totalBudget - budgeted;
  String lab; long big; String sub;
  if (totalBudget > 0) {
   if (left >= 0) { lab = "Safe to spend today"; big = left / daysLeft / 100 * 100; sub = L.isMl()?Fmt.money(totalBudget)+" ൽ "+Fmt.money(left)+" ബാക്കി · "+daysLeft+" ദിവസം":Fmt.money(left) + " left of " + Fmt.money(totalBudget) + " · " + daysLeft + (daysLeft == 1 ? " day" : " days") + " to go"; }
   else { lab = "Over budget by"; big = -left; sub = "Budgets total " + Fmt.money(totalBudget) + " this month"; }
  } else { lab = "Spent this month"; big = spent; sub = "Set budgets to see a daily safe amount"; }
  TextView l1 = Ui.text(a, lab, 13, 0xCC14151A, true); hero.addView(l1);
  TextView amt = Ui.big(a, Fmt.money(big), 46, Ui.onAccent); amt.setPadding(0, Ui.dp(8), 0, Ui.dp(6)); hero.addView(amt);
  hero.addView(Ui.text(a, sub, 13.5f, 0xCC14151A, false));
  if (totalBudget > 0) { Charts.Pace p = new Charts.Pace(a, Math.max(0f, budgeted / (float) totalBudget), (today.getDayOfMonth() - 1f) / ym.lengthOfMonth(), Ui.onAccent, 0x2E14151A); hero.addView(p, Ui.lpm(-1, Ui.dp(26), 0, 12, 0, 0)); hero.addView(Ui.text(a, "Thin mark shows where the month is today", 11.5f, 0x9914151A, false)); }
  LinearLayout stats = Ui.row(a); stats.setPadding(0, Ui.dp(16), 0, 0);
  stats.addView(stat(a, "Income", Fmt.money(income)), Ui.weight(1)); stats.addView(stat(a, "Spent", Fmt.money(spent)), Ui.weight(1)); stats.addView(stat(a, "Net", Fmt.money(income - spent)), Ui.weight(1));
  hero.addView(stats); if (secHero) c.addView(hero);

  if(Privacy.backupDue(db)){TextView backup=Ui.button(a,"Keep a backup of your entries",false);c.addView(backup,Ui.lpm(-1,-2,0,12,0,0));Ui.tap(backup,()->Backup.password(a,true));}
  if (HomeCustom.on(db, "quickAdd")) c.addView(quickAdd(a), Ui.lpm(-1, -2, 0, 14, 0, 0));
  LinearLayout kinds = Ui.row(a); kinds.setPadding(0, Ui.dp(12), 0, 0);
  String[][] ks = {{"expense", "up", "Spent"}, {"income", "download", "Income"}, {"lent", "lent", "Lent"}, {"borrowed", "borrowed", "Borrowed"}, {"transfer", "swap", "Move"}};
  for (String[] k : ks) { LinearLayout cell = Ui.col(a); cell.setGravity(Gravity.CENTER_HORIZONTAL); cell.addView(Ui.badge(a, k[1], 54, Ui.surface, Ui.ink)); TextView t = Ui.text(a, k[2], 12, Ui.muted, true); t.setPadding(0, Ui.dp(7), 0, 0); t.setGravity(Gravity.CENTER);cell.addView(t); kinds.addView(cell, Ui.weight(1)); Ui.tap(cell, () -> a.openEntry(k[0], null)); ((LinearLayout) cell).setContentDescription(k[2]); }
  if (HomeCustom.on(db, "kinds")) c.addView(kinds);

  List<Store.Rec> due = new ArrayList<>(); for (Store.Rec r : db.recurring()) if (r.active && LocalDate.parse(r.next).isBefore(today.plusDays(8))) due.add(r);
  if (!due.isEmpty() && HomeCustom.on(db, "comingUp")) {
   c.addView(Kit.sectionHead(a, "Coming up", "All", () -> Pages.recurring(a)));
   LinearLayout card = Ui.card(a);
   for (int i = 0; i < due.size() && i < 3; i++) { Store.Rec r = due.get(i); if (i > 0) card.addView(Ui.divider(a)); card.addView(dueRow(a, r, today)); }
   c.addView(card);
  }

  boolean secAcc = HomeCustom.on(db, "accounts");
  if (secAcc) c.addView(Kit.sectionHead(a, "Accounts", "Manage", () -> Pages.accounts(a)));
  if (secAcc) {
  TextView hide=Ui.pill(a,db.flag("hideAll",false)?"Show balances":"Hide balances",db.flag("hideAll",false));hide.setTag("balanceToggle");c.addView(hide,Ui.lpm(-2,-2,0,0,0,12));Ui.tap(hide,()->{db.setPref("hideAll",db.flag("hideAll",false)?"0":"1");a.refresh();});
  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout row = Ui.row(a); long total = 0;
  for (Store.Acc x : db.accounts()) { long bal = db.balance(x); total += bal; LinearLayout card = Ui.col(a); card.setBackground(Ui.shape(Ui.surface, 22)); card.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(18), Ui.dp(14)); card.addView(Ui.badge(a, x.kind.equals("cash") ? "cash" : x.kind.equals("bank") ? "bank" : x.kind.equals("card") ? "card" : "wallet", 36, Ui.surface2, Ui.ink)); TextView n = Ui.text(a, x.name, 13, Ui.muted, true); n.setPadding(0, Ui.dp(12), 0, Ui.dp(4)); card.addView(n); card.addView(Ui.big(a, Privacy.amount(db,x,bal), 19, bal < 0 ? Ui.neg : Ui.ink)); String sms=BankSms.display(db,x.id);if(sms!=null)card.addView(Ui.text(a,sms,11,Ui.muted,false),Ui.lpm(-1,-2,0,6,0,0)); row.addView(card, Ui.lpm(Ui.dp(170), -2, 0, 0, 10, 0)); }
  hs.addView(row); c.addView(hs);
  c.addView(Ui.text(a, L.t("Total across accounts")+"  " + (Privacy.anyHidden(db)?"••••":Fmt.money(total)), 12.5f, Ui.muted, true), Ui.lpm(-2, -2, 2, 12, 0, 0));
  }

  if (!budget.isEmpty() && HomeCustom.on(db, "budgets")) {
   c.addView(Kit.sectionHead(a, "Budgets", "Edit", () -> Pages.budgets(a)));
   HorizontalScrollView bs = new HorizontalScrollView(a); bs.setHorizontalScrollBarEnabled(false); LinearLayout br = Ui.row(a);
   int n = 0; for (Map.Entry<Long, Long> e : budget.entrySet()) { if (n++ >= 6) break; Store.Cat ct = cats.get(e.getKey()); if (ct == null) continue; long used = byCat.getOrDefault(e.getKey(), 0L); float f = used / (float) e.getValue(); LinearLayout cell = Ui.col(a); cell.setGravity(Gravity.CENTER_HORIZONTAL); cell.setBackground(Ui.shape(Ui.surface, 22)); cell.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(12));
    cell.addView(new Charts.Ring(a, Math.min(f, 1f), f > 1f ? Ui.neg : (Ui.dark ? Ui.accent : Ui.ink), ct.icon), Ui.lp(Ui.dp(72), Ui.dp(72))); TextView nm = Ui.text(a, ct.name, 13, Ui.ink, true); nm.setPadding(0, Ui.dp(10), 0, Ui.dp(3)); cell.addView(nm); cell.addView(Ui.text(a, f > 1f ? "Over by " + Fmt.compact(used - e.getValue()) : Fmt.compact(Math.max(0, e.getValue() - used)) + " left", 12, f > 1f ? Ui.neg : Ui.muted, false));
    br.addView(cell, Ui.lpm(Ui.dp(116), -2, 0, 0, 10, 0)); Ui.tap(cell, () -> Pages.budgets(a)); }
   bs.addView(br); c.addView(bs);
  }

  if (HomeCustom.on(db, "week")) {
  c.addView(Kit.sectionHead(a, "Last 7 days", null, null));
  long[] v = new long[7]; String[] lb = new String[7]; List<Store.Tx> wk = db.txs(today.minusDays(6).toString(), today.toString());
  for (int i = 0; i < 7; i++) { LocalDate d = today.minusDays(6 - i); lb[i] = d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.NARROW, L.locale()); for (Store.Tx t : wk) if (t.kind.equals("expense") && t.day.equals(d.toString())) v[i] += t.amount; }
  long sum = 0; for (long x : v) sum += x;
  LinearLayout wc = Ui.card(a); LinearLayout hd = Ui.row(a); hd.addView(Ui.big(a, Fmt.money(sum), 24, Ui.ink), Ui.weight(1)); hd.addView(Ui.text(a, "avg " + Fmt.money(sum / 7 / 100 * 100) + " a day", 12.5f, Ui.muted, true)); wc.addView(hd);
  wc.addView(new Charts.Bars(a, v, lb, 6, true), Ui.lpm(-1, Ui.dp(150), 0, 12, 0, 0)); c.addView(wc);
  }

  long owed = 0, owe = 0; for (Store.Person p : db.people()) { long nn = db.net(p.id); if (nn > 0) owed += nn; else owe -= nn; }
  if ((owed > 0 || owe > 0) && HomeCustom.on(db, "people")) {
   c.addView(Kit.sectionHead(a, "People", "Open", () -> a.showTab(4, true)));
   LinearLayout pr = Ui.row(a); pr.addView(pill(a, "They owe you", Fmt.money(owed), Ui.pos), Ui.lpm(0, -2, 0, 0, 5, 0)); ((LinearLayout.LayoutParams) pr.getChildAt(0).getLayoutParams()).weight = 1; pr.addView(pill(a, "You owe", Fmt.money(owe), Ui.neg), Ui.lpm(0, -2, 5, 0, 0, 0)); ((LinearLayout.LayoutParams) pr.getChildAt(1).getLayoutParams()).weight = 1; c.addView(pr);
  }

  if (HomeCustom.on(db, "recent")) {
  c.addView(Kit.sectionHead(a, "Recent", "See all", () -> a.showTab(1, true)));
  List<Store.Tx> rec = db.recent(6); LinearLayout rc = Ui.card(a); rc.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(8));
  if (rec.isEmpty()) rc.addView(Kit.empty(a, "ledger", "Nothing yet", "Tap the plus button to add your first entry."));
  for (int i = 0; i < rec.size(); i++) { if (i > 0) rc.addView(Ui.divider(a)); rc.addView(Kit.txRow(a, rec.get(i), cats, accs, people, true)); }
  c.addView(rc);
  }
  Ui.enter(c);
  return sv[0];
 }

 static View stat(MainActivity a, String k, String v) { LinearLayout l = Ui.col(a); l.addView(Ui.text(a, k, 12, 0xB314151A, true)); TextView t = Ui.text(a, v, 14.5f, Ui.onAccent, true); t.setPadding(0, Ui.dp(4), 0, 0); l.addView(t); return l; }
 static View pill(MainActivity a, String k, String v, int color) { LinearLayout l = Ui.col(a); l.setBackground(Ui.shape(Ui.surface, 22)); l.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14)); l.addView(Ui.text(a, k, 12.5f, Ui.muted, true)); TextView t = Ui.big(a, v, 21, color); t.setPadding(0, Ui.dp(6), 0, 0); l.addView(t); return l; }

 static View dueRow(MainActivity a, Store.Rec r, LocalDate today) {
  LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(9), 0, Ui.dp(9)); row.addView(Ui.badge(a, "repeat", 40, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(40), Ui.dp(40), 0, 0, 12, 0));
  long days = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(r.next)); String when = days < 0 ? "Overdue" : days == 0 ? "Today" : days == 1 ? "Tomorrow" : L.isMl()?days+" ദിവസത്തിൽ":"In " + days + " days";
  LinearLayout m = Ui.col(a); m.addView(Ui.text(a, r.title, 15, Ui.ink, true)); TextView s = Ui.text(a, L.t(when) + " · " + Fmt.money(r.amount), 12.5f, days < 0 ? Ui.neg : Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s); row.addView(m, Ui.weight(1));
  TextView add = Ui.text(a, "Add", 13, Ui.onAccent, true); add.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(8)); add.setBackground(Ui.shape(Ui.accent, 18)); row.addView(add); Ui.tap(add, () -> { a.db.confirm(r); a.refresh(); a.toast(r.title + " added", null); });
  return row;
 }

 static View quickAdd(MainActivity a) {
  LinearLayout r = Ui.row(a); r.setBackground(Ui.outline(Ui.surface, Ui.line, 28, 1)); r.setPadding(Ui.dp(18), Ui.dp(4), Ui.dp(6), Ui.dp(4));
  EditText e = new EditText(a); e.setHint(L.t("Type it: tea 40, auto 60 yesterday")); e.setHintTextColor(Ui.muted); e.setTextColor(Ui.ink); e.setTextSize(15); e.setTypeface(Ui.body); e.setBackground(null); e.setSingleLine(true); e.setImeOptions(EditorInfo.IME_ACTION_DONE); e.setInputType(InputType.TYPE_CLASS_TEXT); e.setTag("quickEntry");
  r.addView(e, Ui.weight(1)); FrameLayout go = Ui.badge(a, "check", 42, Ui.dark ? Ui.accent : Ui.ink, Ui.dark ? Ui.onAccent : 0xFFFFFFFF); r.addView(go, Ui.lp(Ui.dp(42), Ui.dp(42))); go.setContentDescription("Add typed entry");
  Runnable submit = () -> { String s = e.getText().toString().trim(); if (s.isEmpty()) return; Store.Tx t = parse(a.db, s); if (t == null) { a.toast("Include an amount, like tea 40", null); return; } long id = a.db.save(t); if(id<0){a.toast("Entry could not be saved",null);return;} e.setText(""); View focus=a.getCurrentFocus();if(focus!=null)((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0); a.refresh(); a.toast("Added " + Fmt.money(t.amount) + (a.db.categoryMap().get(t.cat) != null ? " · " + a.db.categoryMap().get(t.cat).name : ""), () -> { a.db.deleteTx(id); a.refresh(); }); };
  Ui.tap(go, submit); e.setOnEditorActionListener((v, id, ev) -> { if(id==EditorInfo.IME_ACTION_DONE || (ev!=null && ev.getKeyCode()==android.view.KeyEvent.KEYCODE_ENTER && ev.getAction()==android.view.KeyEvent.ACTION_DOWN)){submit.run();return true;}return false; });
  return r;
 }

 static final String[][] WORDS = {
  {"Food", "tea coffee lunch dinner breakfast snack snacks food biryani juice meal chai"}, {"Groceries", "milk vegetables fruits groceries grocery rice eggs"}, {"Transport", "auto bus metro cab taxi uber ola train ticket parking"},
  {"Fuel", "petrol diesel fuel"}, {"Bills", "recharge bill electricity wifi internet rent water gas"}, {"Health", "medicine pharmacy doctor hospital"}, {"Fun", "movie game netflix show"}, {"Education", "book books fees course exam"}, {"Shopping", "shirt shoes clothes dress shopping"}};
 static Store.Tx parse(Store db, String s) {
  if(s==null||s.trim().isEmpty())return null;
  String value=s.trim();LocalDate day=LocalDate.now();
  java.util.regex.Matcher date=java.util.regex.Pattern.compile("\\b(20[0-9]{2}-[0-9]{2}-[0-9]{2})\\b").matcher(value);
  if(date.find()){try{day=LocalDate.parse(date.group(1));}catch(Exception e){return null;}value=value.replace(date.group(1),"");}
  else if(value.toLowerCase(Locale.ROOT).matches("(?s).*\\byesterday\\b.*"))day=day.minusDays(1);
  value=value.replaceAll("(?i)\\b(yesterday|today)\\b", " ").replaceAll("(?i)\\b(?:rs\\.?|inr)\\s*", "").replace("₹","");
  java.util.regex.Matcher amount=java.util.regex.Pattern.compile("(?<![a-zA-Z0-9.-])([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?![0-9.])").matcher(value);
  if(!amount.find())return null;String token=amount.group(1);long amt=Ledger.parse(token.replace(",",""));int st=amount.start(),en=amount.end();if(amt<=0||amount.find())return null;
  String note=(value.substring(0,st)+" "+value.substring(en)).trim().replaceAll("\\s+"," ");String lower=note.toLowerCase(Locale.ROOT);boolean income=lower.matches("(?s).*\\b(salary|received|got|income|ശമ്പളം|വരുമാനം)\\b.*");String catName=null;
  for(String w:lower.split("\\s+"))for(String[] group:WORDS)if(Arrays.asList(group[1].split(" ")).contains(w))catName=group[0];
  if(catName==null)catName=CategoryIcons.existingCategory(CategoryIcons.suggest(note));
  List<Store.Acc> accs=db.accounts();if(accs.isEmpty())return null;Store.Tx t=new Store.Tx();t.kind=income?"income":"expense";t.amount=amt;t.day=day.toString();t.note=note.isEmpty()?(income?"Income":"Expense"):Fmt.title(note);t.account=accs.get(0).id;
  for(Store.Acc ac:accs)if((""+ac.id).equals(db.pref("lastAcc","0")))t.account=ac.id;
  for(Store.Cat c:db.categories(t.kind))if((income&&c.name.equals("Salary"))||(!income&&c.name.equals(catName)))t.cat=c.id;
  for(Store.Cat c:db.categories(t.kind))if(!c.name.isEmpty() && lower.equals(c.name.toLowerCase(Locale.ROOT)))t.cat=c.id;
  if(t.cat==0)for(Store.Cat c:db.categories(t.kind))if(c.name.toLowerCase(Locale.ROOT).startsWith("other"))t.cat=c.id;
  return t;
 }
}
