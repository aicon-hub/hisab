package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

final class Subscriptions {
 private Subscriptions() {}

 static final class Hit { String note; long amount; long monthly; String period; String last; int count; }

 /** Repeating charges: same note, steady amount, steady weekly/monthly/yearly rhythm, at least three times. */
 static List<Hit> detect(Store db) {
  Map<String, List<Store.Tx>> byNote = new HashMap<>();
  for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) if (t.kind.equals("expense") && !t.note.isEmpty()) { String k = t.note.trim().toLowerCase(Locale.US); List<Store.Tx> l = byNote.get(k); if (l == null) { l = new ArrayList<>(); byNote.put(k, l); } l.add(t); }
  List<Hit> out = new ArrayList<>();
  for (Map.Entry<String, List<Store.Tx>> e : byNote.entrySet()) {
   List<Store.Tx> l = e.getValue(); if (l.size() < 3) continue;
   l.sort((x, y) -> x.day.compareTo(y.day));
   long lo = Long.MAX_VALUE, hi = 0; for (Store.Tx t : l) { lo = Math.min(lo, t.amount); hi = Math.max(hi, t.amount); }
   if (hi > Math.max(100, lo * 1.15)) continue;
   long[] gaps = new long[l.size() - 1]; for (int i = 1; i < l.size(); i++) gaps[i - 1] = ChronoUnit.DAYS.between(LocalDate.parse(l.get(i - 1).day), LocalDate.parse(l.get(i).day));
   Arrays.sort(gaps); long g = gaps[gaps.length / 2]; if (g <= 0) continue;
   String period; if (g >= 5 && g <= 9) period = "weekly"; else if (g >= 25 && g <= 35) period = "monthly"; else if (g >= 350 && g <= 380) period = "yearly"; else continue;
   Store.Tx last = l.get(l.size() - 1); Hit h = new Hit(); h.note = last.note; h.amount = last.amount; h.period = period; h.last = last.day; h.count = l.size();
   h.monthly = Math.round(last.amount * 30.4 / g); out.add(h);
  }
  out.sort((x, y) -> Long.compare(y.monthly, x.monthly));
  return out;
 }

 static void open(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Subscriptions", null, null, c -> {
   List<Hit> hits = detect(db);
   long total = 0; for (Hit h : hits) total += h.monthly;
   LinearLayout hero = Ui.col(a); hero.setBackground(Ui.shape(Ui.accent, 30)); hero.setPadding(Ui.dp(22), Ui.dp(20), Ui.dp(22), Ui.dp(20));
   hero.addView(Ui.text(a, "Repeating charges", 13, Ui.onAccentSub, true));
   TextView big = Ui.big(a, Fmt.money(total) + " a month", 34, Ui.onAccent); big.setPadding(0, Ui.dp(8), 0, Ui.dp(6)); hero.addView(big);
   hero.addView(Ui.text(a, hits.isEmpty() ? "No repeating charges found yet. They appear once the same entry repeats on a steady rhythm." : hits.size() + (hits.size() == 1 ? " repeating charge" : " repeating charges") + " spotted in your entries", 13, Ui.onAccentSub, false));
   c.addView(hero);
   if (!hits.isEmpty()) {
    LinearLayout card = Ui.card(a);
    for (int i = 0; i < hits.size(); i++) { Hit h = hits.get(i); if (i > 0) card.addView(Ui.divider(a));
     LinearLayout r = Ui.row(a); r.setPadding(0, Ui.dp(11), 0, Ui.dp(11));
     r.addView(Ui.badge(a, "repeat", 36, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(36), Ui.dp(36), 0, 0, 12, 0));
     LinearLayout m = Ui.col(a); m.addView(Ui.text(a, h.note, 15, Ui.ink, true));
     m.addView(Ui.text(a, Fmt.money(h.amount) + " " + h.period + " - last " + Fmt.dayLabel(h.last) + " - " + h.count + " times", 12, Ui.muted, false)); r.addView(m, Ui.weight(1));
     r.addView(Ui.text(a, Fmt.money(h.monthly) + "/mo", 14, Ui.muted, true)); card.addView(r); }
    c.addView(card);
    c.addView(Ui.text(a, "Found by reading your entries on this phone - the same note, a steady amount and a steady rhythm. Nothing is sent anywhere.", 12, Ui.muted, false), Ui.lpm(-2, -2, 4, 12, 4, 0));
   }
  });
 }
}
