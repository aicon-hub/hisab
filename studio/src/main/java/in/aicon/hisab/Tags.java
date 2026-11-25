package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.util.List;

/** Tag manager: see every tag with its count, rename or merge, remove, or jump to its entries. */
final class Tags {
 private Tags() {}

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
