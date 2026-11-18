package in.aicon.hisab;

import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Who owes whom. No interest, no fees, only the amounts you note. */
final class PeoplePage {
 private PeoplePage() {}

 static View build(MainActivity a) {
  Store db = a.db; ScrollView[] sv = new ScrollView[1]; LinearLayout c = Kit.column(a, sv, true);
  c.addView(Kit.title(a, "Lent and borrowed", "People", false));
  List<Store.Person> ps = db.people(); long owed = 0, owe = 0; Map<Long, Long> nets = new HashMap<>(); for (Store.Person p : ps) { long n = db.net(p.id); nets.put(p.id, n); if (n > 0) owed += n; else owe -= n; }
  LinearLayout hero = Ui.row(a); hero.setBackground(Ui.shape(Ui.accent, 28)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
  LinearLayout l = Ui.col(a); l.addView(Ui.text(a, "They owe you", 13, 0xCC14151A, true)); TextView x = Ui.big(a, Fmt.money(owed), 28, Ui.onAccent); x.setPadding(0, Ui.dp(8), 0, 0); l.addView(x); hero.addView(l, Ui.weight(1));
  LinearLayout r = Ui.col(a); r.addView(Ui.text(a, "You owe", 13, 0xCC14151A, true)); TextView y = Ui.big(a, Fmt.money(owe), 28, Ui.onAccent); y.setPadding(0, Ui.dp(8), 0, 0); r.addView(y); hero.addView(r, Ui.weight(1)); c.addView(hero);
  LinearLayout btns = Ui.row(a); TextView add = Ui.button(a, "Add person", true), split = Ui.button(a, "Split a bill", false);
  btns.addView(add, Ui.lpm(0, -2, 0, 0, 6, 0)); ((LinearLayout.LayoutParams) add.getLayoutParams()).weight = 1; btns.addView(split, Ui.lpm(0, -2, 6, 0, 0, 0)); ((LinearLayout.LayoutParams) split.getLayoutParams()).weight = 1; c.addView(btns, Ui.lpm(-1, -2, 0, 14, 0, 0));
  Ui.tap(add, () -> Sheets.pick(a, "Add a person", Arrays.asList("From contacts", "Type a name"), Arrays.asList("people", "edit"), -1, i -> { if (i == 0) a.pickContact((n, ph) -> { db.addPerson(n, ph); a.refresh(); }); else Sheets.form(a, "New person", new String[]{"Name", "Phone (optional)"}, new String[]{"", ""}, new int[]{Sheets.TEXT, android.text.InputType.TYPE_CLASS_PHONE}, "Add", v -> { if (!v[0].isEmpty()) { db.addPerson(v[0], v[1].replaceAll("[^+0-9]", "")); a.refresh(); } }, null); }));
  Ui.tap(split, () -> Pages.split(a));
  c.addView(Settings.item(a, "bell", "Automatic reminders", db.flag("autoRemind", false) ? "On" : "Off", () -> AutoRemind.open(a)), Ui.lpm(-1, -2, 0, 12, 0, 0));
  c.addView(Kit.sectionHead(a, "Everyone", null, null));
  if (ps.isEmpty()) { LinearLayout em = Ui.card(a); em.addView(Kit.empty(a, "people", "No one yet", "Add a person, then note what you lent or borrowed.")); c.addView(em); }
  ps.sort((p, q) -> Long.compare(Math.abs(nets.get(q.id)), Math.abs(nets.get(p.id))));
  LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6));
  for (int i = 0; i < ps.size(); i++) { Store.Person p = ps.get(i); long n = nets.get(p.id); if (i > 0) card.addView(Ui.divider(a)); LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(12), 0, Ui.dp(12));
   FrameLayout av = new FrameLayout(a); av.setBackground(Ui.shape(Kit.tone(p.id), 22)); av.addView(Ui.big(a, Ui.initial(p.name), 17, 0xFF14151A), new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)); row.addView(av, Ui.lpm(Ui.dp(44), Ui.dp(44), 0, 0, 14, 0));
   LinearLayout m = Ui.col(a); m.addView(Ui.text(a, p.name, 15.5f, Ui.ink, true)); TextView s = Ui.text(a, n > 0 ? "owes you" : n < 0 ? "you owe" : "settled", 12.5f, Ui.muted, false); s.setPadding(0, Ui.dp(3), 0, 0); m.addView(s); row.addView(m, Ui.weight(1));
   row.addView(Ui.big(a, n == 0 ? Fmt.money(0) : Fmt.money(Math.abs(n)), 17, n > 0 ? Ui.pos : n < 0 ? Ui.neg : Ui.muted)); Ui.tap(row, () -> person(a, p)); card.addView(row); }
  if (!ps.isEmpty()) c.addView(card);
  Ui.enter(c); return sv[0];
 }

 static String reminderText(Store db, Store.Person p, long net) {
  String amt = Fmt.money(Math.abs(net));
  if (net > 0) return "Hi " + p.name + ", a gentle reminder about the " + amt + " I lent you. Please send it when you can. Thank you!";
  return "Hi " + p.name + ", I still owe you " + amt + ". I will pay it back soon. Thank you for waiting.";
 }

 static void person(MainActivity a, Store.Person p) {
  Store db = a.db; final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  ref[0] = a.open(() -> {
   LinearLayout outer = Ui.col(a); outer.addView(Kit.bar(a, p.name, ref, null, null)); ScrollView s = new ScrollView(a); s.setVerticalScrollBarEnabled(false); LinearLayout c = Ui.col(a); c.setPadding(Ui.dp(20), Ui.dp(8), Ui.dp(20), Ui.dp(40)); s.addView(c);
   long n = db.net(p.id); LinearLayout hero = Ui.col(a); hero.setBackground(Ui.shape(Ui.surface, 28)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
   hero.addView(Ui.text(a, n > 0 ? p.name + " owes you" : n < 0 ? "You owe " + p.name : "All settled", 13, Ui.muted, true)); TextView big = Ui.big(a, Fmt.money(Math.abs(n)), 40, n > 0 ? Ui.pos : n < 0 ? Ui.neg : Ui.ink); big.setPadding(0, Ui.dp(8), 0, 0); hero.addView(big); c.addView(hero);
   LinearLayout r = Ui.row(a); TextView got = Ui.button(a, n >= 0 ? "They paid me" : "I paid them", true); TextView more = Ui.button(a, n >= 0 ? "Lent more" : "Borrowed more", false); r.addView(got, Ui.lpm(0, -2, 0, 0, 6, 0)); ((LinearLayout.LayoutParams) got.getLayoutParams()).weight = 1; r.addView(more, Ui.lpm(0, -2, 6, 0, 0, 0)); ((LinearLayout.LayoutParams) more.getLayoutParams()).weight = 1; c.addView(r, Ui.lpm(-1, -2, 0, 12, 0, 0));
   Ui.tap(got, () -> entryFor(a, p, n >= 0 ? "repay_in" : "repay_out", Math.abs(n))); Ui.tap(more, () -> entryFor(a, p, n >= 0 ? "lent" : "borrowed", 0));
   if (n != 0) { LinearLayout rr = Ui.row(a); TextView wa = Ui.button(a, "Remind on WhatsApp", false), sms = Ui.button(a, "SMS", false); rr.addView(wa, Ui.lpm(0, -2, 0, 0, 6, 0)); ((LinearLayout.LayoutParams) wa.getLayoutParams()).weight = 2; rr.addView(sms, Ui.lpm(0, -2, 6, 0, 0, 0)); ((LinearLayout.LayoutParams) sms.getLayoutParams()).weight = 1; c.addView(rr, Ui.lpm(-1, -2, 0, 10, 0, 0));
    String text = reminderText(db, p, n);
    wa.setCompoundDrawablesWithIntrinsicBounds(Icons.brand(a,"whatsapp"),null,null,null);wa.setCompoundDrawablePadding(Ui.dp(6));sms.setCompoundDrawablesWithIntrinsicBounds(new Icons.Draw("sms",Ui.ink),null,null,null);sms.setCompoundDrawablePadding(Ui.dp(6));
    Ui.tap(wa, () -> { String ph = Phone.wa(p.phone); Uri u = ph.isEmpty() ? Uri.parse("https://wa.me/?text=" + Uri.encode(text)) : Uri.parse("https://wa.me/" + ph + "?text=" + Uri.encode(text)); Sheets.confirm(a, "Open WhatsApp?", "Your message is prefilled. Nothing is sent until you press send there.\n\n" + text, "Open", () -> a.launchExternal(new Intent(Intent.ACTION_VIEW, u))); });
    Ui.tap(sms, () -> Sheets.confirm(a, "Open messages?", "Your message is prefilled. Nothing is sent until you press send there.\n\n" + text, "Open", () -> a.launchExternal(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Phone.normalise(p.phone))).putExtra("sms_body", text)))); }
   c.addView(Kit.sectionHead(a, "History", null, null)); Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> pm = Kit.peopleMap(db); List<Store.Tx> hist = db.txsOfPerson(p.id);
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6)); for (int i = 0; i < hist.size(); i++) { if (i > 0) card.addView(Ui.divider(a)); card.addView(Kit.txRow(a, hist.get(i), cats, accs, pm, true)); } if (hist.isEmpty()) card.addView(Kit.empty(a, "ledger", "No entries", "Add what you lent or borrowed.")); c.addView(card);
   TextView del = Ui.button(a, "Remove person and their entries", false); del.setTextColor(Ui.neg); c.addView(del, Ui.lpm(-1, -2, 0, 24, 0, 0)); Ui.tap(del, () -> Sheets.confirm(a, "Remove " + p.name + "?", "Their entries will be deleted too. This cannot be undone.", "Remove", () -> { db.deletePerson(p.id); a.close(ref[0]); a.refresh(); }));
   outer.addView(s, Ui.lp(-1, 0)); ((LinearLayout.LayoutParams) s.getLayoutParams()).weight = 1; return outer; }, false);
 }
 static void entryFor(MainActivity a, Store.Person p, String kind, long amount) {
  Store.Tx t = new Store.Tx(); t.kind = kind; t.amount = amount; t.person = p.id; t.day = java.time.LocalDate.now().toString(); List<Store.Acc> accs = a.db.accounts(); t.account = accs.get(0).id; t.note = "";
  if (amount == 0) { t.id = 0; }
  EntrySheet.S s = null; Store.Tx e = t; e.id = 0; a.openEntryPrefilled(e);
 }
}
