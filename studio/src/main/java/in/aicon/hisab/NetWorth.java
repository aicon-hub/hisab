package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.util.*;

final class NetWorth {
 private NetWorth() {}

 static void open(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Net worth", null, null, c -> {
   List<Store.Acc> accs = db.accounts();
   long inAccounts = 0; for (Store.Acc x : accs) inAccounts += db.balance(x);
   long owedToYou = 0, youOwe = 0;
   for (Store.Person p : db.people()) { long n = db.net(p.id); if (n > 0) owedToYou += n; else youOwe -= n; }
   long total = inAccounts + owedToYou - youOwe;

   LinearLayout hero = Ui.col(a); hero.setBackground(Ui.shape(Ui.accent, 30)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
   hero.addView(Ui.text(a, "Net worth", 13, Ui.onAccentSub, true));
   TextView big = Ui.big(a, Fmt.money(total), 40, Ui.onAccent); big.setPadding(0, Ui.dp(8), 0, Ui.dp(6)); hero.addView(big);
   hero.addView(Ui.text(a, "Accounts " + Fmt.money(inAccounts) + " + owed to you " + Fmt.money(owedToYou) + (youOwe > 0 ? " - you owe " + Fmt.money(youOwe) : ""), 13, Ui.onAccentSub, false));
   c.addView(hero);

   c.addView(Kit.sectionHead(a, "Accounts", "Manage", () -> Pages.accounts(a)));
   LinearLayout ac = Ui.card(a);
   for (int i = 0; i < accs.size(); i++) { Store.Acc x = accs.get(i); long bal = db.balance(x); if (i > 0) ac.addView(Ui.divider(a)); ac.addView(netRow(a, x.name, Fmt.title(x.kind), Privacy.amount(db, x, bal), bal)); }
   c.addView(ac);

   c.addView(Kit.sectionHead(a, "People", null, null));
   LinearLayout pc = Ui.card(a);
   pc.addView(netRow(a, "Owed to you", "Lent minus repaid", Fmt.money(owedToYou), 1));
   if (youOwe > 0) { pc.addView(Ui.divider(a)); pc.addView(netRow(a, "You owe", "Borrowed minus repaid", Fmt.money(youOwe), -1)); }
   c.addView(pc);
  });
 }

 static View netRow(MainActivity a, String title, String sub, String value, long sign) {
  LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
  LinearLayout t = Ui.col(a); t.addView(Ui.text(a, title, 15, Ui.ink, true));
  TextView ts = Ui.text(a, sub, 12, Ui.muted, false); ts.setPadding(0, Ui.dp(2), 0, 0); t.addView(ts); r.addView(t, Ui.weight(1));
  r.addView(Ui.big(a, value, 16, sign < 0 ? Ui.neg : Ui.ink)); return r;
 }
}
