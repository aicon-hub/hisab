package in.aicon.hisab;

import android.text.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

final class LedgerPage {
 private LedgerPage() {}
 static final String[][] FILTERS = {{"all", "All"}, {"expense", "Spent"}, {"income", "Income"}, {"transfer", "Moves"}, {"people", "People"}};

 static boolean match(String filter, Store.Tx t) {
  switch (filter) { case "all": return true; case "people": return t.kind.equals("lent") || t.kind.equals("borrowed") || t.kind.startsWith("repay"); default: return t.kind.equals(filter); }
 }

 /** Swipe left on the month for the newer month, swipe right for the older one. The arrow buttons still work. */
 static void swipeMonth(MainActivity a, View v, Runnable newer, Runnable older) {
  final float[] d = {0, 0}; final boolean[] drag = {false}; final int slop = android.view.ViewConfiguration.get(a).getScaledTouchSlop();
  v.setOnTouchListener((vv, e) -> {
   int ac = e.getActionMasked();
   if (ac == android.view.MotionEvent.ACTION_DOWN) { d[0] = e.getX(); d[1] = e.getY(); drag[0] = false; return true; }
   float dx = e.getX() - d[0], dy = e.getY() - d[1];
   if (ac == android.view.MotionEvent.ACTION_MOVE) {
    if (!drag[0] && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.3f) { drag[0] = true; if (vv.getParent() != null) vv.getParent().requestDisallowInterceptTouchEvent(true); }
    return true;
   }
   if (ac == android.view.MotionEvent.ACTION_UP && drag[0]) {
    if (dx < -Ui.dp(48)) { Ui.buzzTick(vv); newer.run(); } else if (dx > Ui.dp(48)) { Ui.buzzTick(vv); older.run(); }
   }
   return true;
  });
 }

 static View build(MainActivity a) {
  Store db = a.db; ScrollView[] sv = new ScrollView[1]; LinearLayout c = Kit.column(a, sv, true);
  c.addView(Kit.title(a, "Everything you have noted", "Ledger", false));
  YearMonth ym = a.ledgerMonth;
  LinearLayout nav = Ui.row(a); nav.setBackground(Ui.shape(Ui.surface, 26)); nav.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
  FrameLayout l = Ui.badge(a, "left", 38, Ui.surface2, Ui.ink), r = Ui.badge(a, "right", 38, Ui.surface2, Ui.ink); l.setContentDescription("Previous month"); r.setContentDescription("Next month");
  nav.addView(l, Ui.lp(Ui.dp(38), Ui.dp(38))); TextView mt = Ui.big(a, Fmt.month(ym), 18, Ui.ink); mt.setGravity(Gravity.CENTER); nav.addView(mt, Ui.weight(1)); nav.addView(r, Ui.lp(Ui.dp(38), Ui.dp(38)));
  Ui.tap(l, () -> { a.ledgerMonth = ym.minusMonths(1); a.ledgerDay = 0; a.refresh(); }); Ui.tap(r, () -> { a.ledgerMonth = ym.plusMonths(1); a.ledgerDay = 0; a.refresh(); }); c.addView(nav);
  swipeMonth(a, mt, () -> { a.ledgerMonth = ym.plusMonths(1); a.ledgerDay = 0; a.refresh(); }, () -> { a.ledgerMonth = ym.minusMonths(1); a.ledgerDay = 0; a.refresh(); });

  boolean ranged = !a.ledgerFrom.isEmpty() || !a.ledgerTo.isEmpty();
  List<Store.Tx> month = db.txs(ranged ? (a.ledgerFrom.isEmpty() ? "0000-01-01" : a.ledgerFrom) : Fmt.from(ym), ranged ? (a.ledgerTo.isEmpty() ? "9999-12-31" : a.ledgerTo) : Fmt.to(ym)); long in = 0, out = 0; Map<Integer, Long> spend = new HashMap<>();
  for (Store.Tx t : month) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) { out += t.amount; spend.merge(LocalDate.parse(t.day).getDayOfMonth(), t.amount, Long::sum); } }
  LinearLayout sum = Ui.row(a); sum.setPadding(Ui.dp(4), Ui.dp(16), Ui.dp(4), Ui.dp(4));
  sum.addView(kv(a, "Income", Fmt.money(in), Ui.pos), Ui.weight(1)); sum.addView(kv(a, "Spent", Fmt.money(out), Ui.ink), Ui.weight(1)); sum.addView(kv(a, "Net", Fmt.money(in - out), in - out < 0 ? Ui.neg : Ui.ink), Ui.weight(1)); c.addView(sum);

  // search + calendar toggle
  LinearLayout sr = Ui.row(a); sr.setBackground(Ui.outline(Ui.surface, Ui.line, 26, 1)); sr.setPadding(Ui.dp(16), Ui.dp(2), Ui.dp(8), Ui.dp(2));
  sr.addView(Ui.icon(a, "search", 20, Ui.muted)); EditText e = new EditText(a); e.setHint("Search notes, tags, amounts"); e.setHintTextColor(Ui.muted); e.setTextColor(Ui.ink); e.setTextSize(15); e.setTypeface(Ui.body); e.setBackground(null); e.setSingleLine(true); e.setText(a.ledgerQuery); e.setPadding(Ui.dp(10), Ui.dp(12), Ui.dp(10), Ui.dp(12)); sr.addView(e, Ui.weight(1));
  if (!a.ledgerQuery.isEmpty()) { FrameLayout clr = Ui.badge(a, "close", 32, Ui.surface2, Ui.ink); clr.setContentDescription("Clear search"); clr.setTag("clearSearch"); Ui.tap(clr, () -> { a.ledgerQuery = ""; a.refresh(); }); sr.addView(clr, Ui.lpm(Ui.dp(32), Ui.dp(32), 0, 0, 6, 0)); }
  FrameLayout cal = Ui.badge(a, "calendar", 38, a.ledgerCalendar ? Ui.accent : Ui.surface2, a.ledgerCalendar ? Ui.onAccent : Ui.ink); sr.addView(cal, Ui.lp(Ui.dp(38), Ui.dp(38))); cal.setContentDescription("Calendar view"); Ui.tap(cal, () -> { a.ledgerCalendar = !a.ledgerCalendar; a.refresh(); });
  FrameLayout fb = Ui.badge(a, "filter", 38, filtersOn(a) ? Ui.accent : Ui.surface2, filtersOn(a) ? Ui.onAccent : Ui.ink); sr.addView(fb, Ui.lp(Ui.dp(38), Ui.dp(38))); fb.setContentDescription("More filters"); fb.setTag("filterBtn"); Ui.tap(fb, () -> filterSheet(a));
  c.addView(sr, Ui.lpm(-1, -2, 0, 14, 0, 0));
  final boolean[] ignore = {true};
  e.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence x, int i, int j, int k) {} public void afterTextChanged(Editable ed) {} public void onTextChanged(CharSequence x, int i, int j, int k) { if (ignore[0]) { ignore[0] = false; if (x.toString().equals(a.ledgerQuery)) return; } a.ledgerQuery = x.toString(); a.tabHost.removeAllViews(); View v = LedgerPage.build(a); a.tabHost.addView(v); EditText ne = v.findViewWithTag("q"); if (ne != null) { ne.requestFocus(); ne.setSelection(ne.getText().length()); } } });
  e.setTag("q");

  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout pr = Ui.row(a); pr.setPadding(0, Ui.dp(12), 0, 0);
  for (String[] f : FILTERS) { TextView p = Ui.pill(a, f[1], a.ledgerFilter.equals(f[0])); pr.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0)); Ui.tap(p, () -> { a.ledgerFilter = f[0]; a.refresh(); }); }
  hs.addView(pr); c.addView(hs);

  if (a.ledgerCalendar) { LinearLayout cc = Ui.card(a); Charts.Heat h = new Charts.Heat(a, ym, spend, a.ledgerDay, d -> { a.ledgerDay = a.ledgerDay == d ? 0 : d; a.refresh(); }); cc.addView(h, new LinearLayout.LayoutParams(-1, -2)); cc.addView(Ui.text(a, a.ledgerDay > 0 ? "Showing day " + a.ledgerDay + ". Tap it again to clear." : "Darker days are where you spent more. Tap a day.", 12, Ui.muted, false), Ui.lpm(-2, -2, 0, 10, 0, 0)); c.addView(cc, Ui.lpm(-1, -2, 0, 14, 0, 0)); }

  Map<Long, Store.Cat> cats = db.categoryMap(); Map<Long, Store.Acc> accs = Kit.accMap(db); Map<Long, Store.Person> people = Kit.peopleMap(db);
  String q = a.ledgerQuery.trim().toLowerCase(Locale.ROOT); long amt = amountQuery(q); List<Store.Tx> shown = new ArrayList<>();
  for (Store.Tx t : month) { if (!match(a.ledgerFilter, t)) continue; if (a.ledgerCat != 0 && t.cat != a.ledgerCat) continue; if (a.ledgerMin > 0 && t.amount < a.ledgerMin) continue; if (a.ledgerMax > 0 && t.amount > a.ledgerMax) continue; if (a.ledgerDay > 0 && LocalDate.parse(t.day).getDayOfMonth() != a.ledgerDay) continue; if (!q.isEmpty()) { Store.Cat ct = cats.get(t.cat); Store.Person p = people.get(t.person); String hay = (t.note + " " + (t.tags == null ? "" : t.tags) + " " + (ct == null ? "" : ct.name) + " " + (p == null ? "" : p.name) + " " + Kit.kindName(t.kind)).toLowerCase(Locale.ROOT); if (!hay.contains(q) && (amt < 0 || t.amount != amt)) continue; } shown.add(t); }
  if (shown.isEmpty()) { LinearLayout em = Ui.card(a); em.addView(Kit.empty(a, "search", "No entries here", q.isEmpty() ? "Nothing in this month for this filter." : "Nothing matches your search.")); c.addView(em, Ui.lpm(-1, -2, 0, 16, 0, 0)); }
  String day = ""; LinearLayout card = null;
  for (Store.Tx t : shown) {
   if (!t.day.equals(day)) { day = t.day; long dayOut = 0; for (Store.Tx x : shown) if (x.day.equals(t.day) && x.kind.equals("expense")) dayOut += x.amount;
    LinearLayout hd = Ui.row(a); hd.setPadding(Ui.dp(4), Ui.dp(22), Ui.dp(4), Ui.dp(10)); hd.addView(Ui.text(a, Fmt.dayLabel(t.day), 14, Ui.ink, true), Ui.weight(1)); if (dayOut > 0) hd.addView(Ui.text(a, "-" + Fmt.money(dayOut), 13, Ui.muted, true)); c.addView(hd);
    card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4)); c.addView(card); } else card.addView(Ui.divider(a));
   card.addView(Kit.txRow(a, t, cats, accs, people, false));
  }
  return sv[0];
 }
 /** A plain amount typed in search, in minor units; -1 when the query is not a plain amount. */
 static long amountQuery(String q) {
  String x = q.replace("\u20B9", "").replace(",", "").replace(" ", "").trim();
  if (!x.matches("\\d+(\\.\\d{1,2})?") || x.length() > 15) return -1;
  try { return Math.round(Double.parseDouble(x) * 100); } catch (Exception e) { return -1; }
 }

 static boolean filtersOn(MainActivity a) { return a.ledgerCat != 0 || a.ledgerMin > 0 || a.ledgerMax > 0 || !a.ledgerFrom.isEmpty() || !a.ledgerTo.isEmpty(); }

 static void filterSheet(MainActivity a) {
  LinearLayout c = Sheets.sheetBody(a, "Filter entries");
  Map<Long, Store.Cat> cats = a.db.categoryMap();
  TextView cat = Ui.button(a, a.ledgerCat == 0 ? "Any category" : (cats.get(a.ledgerCat) == null ? "Any category" : cats.get(a.ledgerCat).name), false); c.addView(cat, Ui.lpm(-1, -2, 0, 8, 0, 0));
  Ui.tap(cat, () -> {
   List<Store.Cat> all = a.db.categories(null); List<String> labels = new ArrayList<>(), icons = new ArrayList<>();
   labels.add("Any category"); icons.add("other");
   for (Store.Cat x : all) { labels.add(x.name); icons.add(x.icon); }
   int sel = 0; for (int i = 0; i < all.size(); i++) if (all.get(i).id == a.ledgerCat) sel = i + 1;
   Sheets.pick(a, "Category", labels, icons, sel, i -> { a.ledgerCat = i == 0 ? 0 : all.get(i - 1).id; a.closeAll(); filterSheet(a); });
  });
  TextView from = Ui.button(a, a.ledgerFrom.isEmpty() ? "From any date" : "From " + a.ledgerFrom, false); c.addView(from, Ui.lpm(-1, -2, 0, 8, 0, 0));
  Ui.tap(from, () -> DateSheet.show(a, a.ledgerFrom.isEmpty() ? LocalDate.now() : LocalDate.parse(a.ledgerFrom), d -> { a.ledgerFrom = d.toString(); a.closeAll(); filterSheet(a); }));
  TextView to = Ui.button(a, a.ledgerTo.isEmpty() ? "To any date" : "To " + a.ledgerTo, false); c.addView(to, Ui.lpm(-1, -2, 0, 8, 0, 0));
  Ui.tap(to, () -> DateSheet.show(a, a.ledgerTo.isEmpty() ? LocalDate.now() : LocalDate.parse(a.ledgerTo), d -> { a.ledgerTo = d.toString(); a.closeAll(); filterSheet(a); }));
  EditText min = Sheets.field(a, "At least (amount, optional)", a.ledgerMin > 0 ? "" + (a.ledgerMin / 100.0) : "", Sheets.NUMBER); c.addView(min, Ui.lpm(-1, -2, 0, 8, 0, 0));
  EditText max = Sheets.field(a, "At most (amount, optional)", a.ledgerMax > 0 ? "" + (a.ledgerMax / 100.0) : "", Sheets.NUMBER); c.addView(max, Ui.lpm(-1, -2, 0, 8, 0, 0));
  TextView apply = Ui.button(a, "Apply", true); c.addView(apply, Ui.lpm(-1, -2, 0, 12, 0, 0));
  Ui.tap(apply, () -> {
   a.ledgerMin = Ledger.parse(min.getText().toString().trim()); if (a.ledgerMin < 0) a.ledgerMin = 0;
   a.ledgerMax = Ledger.parse(max.getText().toString().trim()); if (a.ledgerMax < 0) a.ledgerMax = 0;
   a.closeAll(); a.refresh();
  });
  TextView clear = Ui.button(a, "Clear all filters", false); c.addView(clear, Ui.lpm(-1, -2, 0, 8, 0, 0));
  Ui.tap(clear, () -> { a.ledgerCat = 0; a.ledgerMin = 0; a.ledgerMax = 0; a.ledgerFrom = ""; a.ledgerTo = ""; a.closeAll(); a.refresh(); });
  Sheets.show(a, c);
 }

 static View kv(MainActivity a, String k, String v, int color) { LinearLayout l = Ui.col(a); l.addView(Ui.text(a, k, 12, Ui.muted, true)); TextView t = Ui.big(a, v, 18, color); t.setPadding(0, Ui.dp(5), 0, 0); l.addView(t); return l; }
}
