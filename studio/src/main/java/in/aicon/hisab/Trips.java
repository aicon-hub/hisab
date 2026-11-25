package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

final class Trips {
 private Trips() {}

 static final class Trip { String name, from, to; }

 static List<Trip> read(Store db) {
  List<Trip> out = new ArrayList<>();
  for (String line : db.pref("trips", "").split("\n")) { String[] p = line.split("\t", -1); if (p.length == 3 && !p[0].isEmpty()) { Trip t = new Trip(); t.name = p[0]; t.from = p[1]; t.to = p[2]; out.add(t); } }
  return out;
 }

 static void write(Store db, List<Trip> trips) {
  StringBuilder b = new StringBuilder();
  for (Trip t : trips) b.append(t.name.replace('\t', ' ').replace('\n', ' ')).append('\t').append(t.from).append('\t').append(t.to).append('\n');
  db.setPref("trips", b.toString());
 }

 /** Entries tagged trip:<name> (case-insensitive), expenses only. */
 static long[] totals(Store db, String name) {
  String tag = "trip:" + name.toLowerCase(Locale.US);
  long sum = 0, count = 0;
  for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) if (t.kind.equals("expense") && t.tags != null && t.tags.toLowerCase(Locale.US).contains(tag)) { sum += t.amount; count++; }
  return new long[]{sum, count};
 }

 static void open(MainActivity a) {
  Store db = a.db;
  Kit.page(a, "Trips and events", "plus", () -> Sheets.form(a, "New trip or event", new String[]{"Name", "Starts (optional, hold to clear)", "Ends (optional)"}, new String[]{"", "", ""}, new int[]{Sheets.TEXT, Sheets.DATE, Sheets.DATE}, "Add", v -> { if (!v[0].isEmpty()) { List<Trip> trips = read(db); Trip t = new Trip(); t.name = v[0]; t.from = v[1].trim(); t.to = v[2].trim(); trips.add(t); write(db, trips); a.closeAll(); open(a); } }, null), c -> {
   List<Trip> trips = read(db);
   if (trips.isEmpty()) { c.addView(Kit.empty(a, "spark", "No trips yet", "Add a trip, then tap its tag chip under Tags when you save an entry.")); return; }
   for (int i = 0; i < trips.size(); i++) { final int idx = i; Trip t = trips.get(i); long[] tot = totals(db, t.name);
    LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(14));
    LinearLayout h = Ui.row(a); h.addView(Ui.text(a, t.name, 16, Ui.ink, true), Ui.weight(1)); h.addView(Ui.big(a, Fmt.money(tot[0]), 20, Ui.ink)); card.addView(h);
    StringBuilder sub = new StringBuilder(); sub.append(tot[1]).append(tot[1] == 1 ? " entry" : " entries");
    if (!t.from.isEmpty()) { sub.append(" - ").append(Fmt.dayLabel(t.from)); if (!t.to.isEmpty()) { sub.append(" to ").append(Fmt.dayLabel(t.to)); long days = Math.max(1, ChronoUnit.DAYS.between(LocalDate.parse(t.from), LocalDate.parse(t.to)) + 1); sub.append(" - ").append(Fmt.money(tot[0] / days)).append(" a day"); } }
    card.addView(Ui.text(a, sub.toString(), 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 4, 0, 0));
    card.addView(Ui.text(a, "Tap the tag trip:" + t.name.toLowerCase(Locale.US) + " when adding an entry", 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 4, 0, 0));
    c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10));
    Ui.tap(card, () -> Sheets.confirm(a, "Remove " + t.name + "?", "Entries stay; only the trip card goes.", "Remove", () -> { List<Trip> now = read(db); if (idx < now.size()) now.remove(idx); write(db, now); a.closeAll(); open(a); })); }
  });
 }
}
