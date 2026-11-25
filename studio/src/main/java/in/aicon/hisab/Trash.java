package in.aicon.hisab;

import android.view.*;
import android.widget.*;
import java.io.File;

/** Entries deleted in the last 30 days. Restorable until they age out; older ones are removed for good. */
final class Trash {
 private Trash() {}

 /** Removes entries trashed over 30 days ago, with their attachment files. */
 static void purge(MainActivity a) {
  try {
   for (long id : a.db.purgeTrash()) {
    File d = Attach.dir(a); if (d == null) continue;
    File[] fs = d.listFiles((dir, n) -> n.startsWith("att-" + id + "-"));
    if (fs != null) for (File f : fs) f.delete();
   }
  } catch (Exception ignored) {}
 }

 static void open(MainActivity a) {
  Kit.page(a, "Trash bin", null, null, c -> {
   c.addView(Ui.text(a, "Deleted entries stay here for 30 days, then they are removed for good. Restoring puts an entry back exactly as it was, photos included.", 14, Ui.muted, false), Ui.lpm(-1, -2, 0, 12, 0, 12));
   java.util.List<Store.Tx> items = a.db.trash();
   if (items.isEmpty()) { c.addView(Kit.empty(a, "search", "Nothing in the bin", "Entries you delete will wait here for 30 days.")); return; }
   java.util.Map<Long, Store.Cat> cats = a.db.categoryMap(); java.util.Map<Long, Store.Acc> accs = Kit.accMap(a.db); java.util.Map<Long, Store.Person> people = Kit.peopleMap(a.db);
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4));
   for (Store.Tx t : items) {
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
    row.addView(Ui.badge(a, Kit.iconOf(t, cats), 40, Ui.surface2, Ui.ink), Ui.lpm(Ui.dp(40), Ui.dp(40), 0, 0, 12, 0));
    LinearLayout mid = Ui.col(a); mid.addView(Ui.text(a, Kit.titleOf(t, cats, people), 14.5f, Ui.ink, true));
    mid.addView(Ui.text(a, Fmt.dayLabel(t.day) + " - " + Fmt.money(t.amount), 12.5f, Ui.muted, false)); row.addView(mid, Ui.weight(1));
    TextView re = Ui.button(a, "Restore", false); re.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8)); row.addView(re, Ui.lpm(-2, -2, 4, 0, 0, 0));
    TextView del = Ui.button(a, "Delete", false); del.setTextColor(Ui.neg); del.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8)); row.addView(del, Ui.lpm(-2, -2, 4, 0, 4, 0));
    Ui.tap(re, () -> { a.db.restoreTx(t.id); a.closeAll(); a.refresh(); a.toast("Entry restored", null); });
    Ui.tap(del, () -> { Attach.removeAll(a, t.id); a.db.deleteTx(t.id); a.closeAll(); a.refresh(); a.toast("Deleted for good", null); });
    card.addView(row); if (t != items.get(items.size() - 1)) card.addView(Ui.divider(a));
   }
   c.addView(card);
  });
 }
}
