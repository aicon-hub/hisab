package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.util.List;

/** Tag manager: see every tag with its count, rename or merge, remove, or jump to its entries. */
final class Tags {
 private Tags() {}


 /** Tap-to-pick tag chips under the tags field: trips first (as trip:name), then your most used tags. Tap adds or removes it. */
 static View chooser(MainActivity a, EditText f) {
  List<String> opts = new java.util.ArrayList<>();
  try { for (Trips.Trip t : Trips.read(a.db)) opts.add("trip:" + t.name.toLowerCase(java.util.Locale.US)); } catch (Exception e) {}
  try { for (String[] t : a.db.tagCounts()) { if (!opts.contains(t[0]) && opts.size() < 16) opts.add(t[0]); } } catch (Exception e) {}
  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false);
  LinearLayout row = Ui.row(a); hs.addView(row);
  if (opts.isEmpty()) { hs.setVisibility(View.GONE); return hs; }
  final List<TextView> chips = new java.util.ArrayList<>();
  final Runnable paint = () -> { List<String> cur = parse(f.getText().toString()); for (int i = 0; i < chips.size(); i++) { boolean on = cur.contains(opts.get(i).toLowerCase(java.util.Locale.US)); chips.get(i).setBackground(Ui.shape(on ? Ui.ink : Ui.surface2, 18)); chips.get(i).setTextColor(on ? Ui.bg : Ui.ink); } };
  for (String o : opts) {
   TextView chip = Ui.text(a, o, 13, Ui.ink, true); chip.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(8)); chip.setContentDescription("Tag " + o); chips.add(chip);
   row.addView(chip, Ui.lpm(-2, -2, 0, 0, 8, 0));
   Ui.tap(chip, () -> { List<String> cur = parse(f.getText().toString()); String k = o.toLowerCase(java.util.Locale.US); if (cur.contains(k)) cur.remove(k); else cur.add(k); f.setText(String.join(", ", cur)); f.setSelection(f.getText().length()); });
  }
  f.addTextChangedListener(new android.text.TextWatcher() { public void beforeTextChanged(CharSequence x, int i, int j, int k) {} public void onTextChanged(CharSequence x, int i, int j, int k) { paint.run(); } public void afterTextChanged(android.text.Editable x) {} });
  paint.run(); return hs;
 }
 static List<String> parse(String s) { List<String> out = new java.util.ArrayList<>(); for (String p : s.split(",")) { String t = p.trim().toLowerCase(java.util.Locale.US); if (!t.isEmpty() && !out.contains(t)) out.add(t); } return out; }

 static void open(MainActivity a) {
  Kit.page(a, "Tags", null, null, c -> {
   c.addView(Ui.text(a, "Every tag you have used. Tap one to rename it, or give it the name of another tag to merge them. Removing a tag never deletes entries.", 14, Ui.muted, false), Ui.lpm(-1, -2, 0, 12, 0, 12));
   List<String[]> tags = a.db.tagCounts();
   if (tags.isEmpty()) { c.addView(Kit.empty(a, "tag", "No tags yet", "Add tags when you save an entry and they will show up here.")); return; }
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4));
   for (int i = 0; i < tags.size(); i++) {
    final String name = tags.get(i)[0], n = tags.get(i)[1];
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
    row.addView(Ui.badge(a, "tag", 40, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(40), Ui.dp(40), 0, 0, 12, 0));
    LinearLayout mid = Ui.col(a); mid.addView(Ui.text(a, name, 14.5f, Ui.ink, true));
    mid.addView(Ui.text(a, n + (n.equals("1") ? " entry" : " entries"), 12.5f, Ui.muted, false)); row.addView(mid, Ui.weight(1));
    TextView show = Ui.button(a, "Show", false); show.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8)); row.addView(show, Ui.lpm(-2, -2, 4, 0, 0, 0));
    TextView edit = Ui.button(a, "Rename", false); edit.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8)); row.addView(edit, Ui.lpm(-2, -2, 4, 0, 4, 0));
    Ui.tap(show, () -> { a.ledgerQuery = name; a.closeAll(); a.showTab(1, true); });
    Ui.tap(edit, () -> Sheets.form(a, "Rename tag", new String[]{"Tag name"}, new String[]{name}, new int[]{Sheets.TEXT}, "Save",
      v -> { if (!v[0].isEmpty()) { a.db.renameTag(name, v[0]); a.closeAll(); a.refresh(); open(a); a.toast("Tag updated", null); } },
      () -> { a.db.dropTag(name); a.closeAll(); a.refresh(); open(a); a.toast("Tag removed", null); }));
    card.addView(row); if (i < tags.size() - 1) card.addView(Ui.divider(a));
   }
   c.addView(card);
  });
 }
}
