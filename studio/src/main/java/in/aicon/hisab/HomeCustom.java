package in.aicon.hisab;

import android.view.*;
import android.widget.*;

/** Per-section Home screen switches. Full customisation is Hisab's hallmark. */
final class HomeCustom {
 private HomeCustom() {}

 static final String[][] SECTIONS = {
  {"hero", "Top card"},
  {"quickAdd", "Quick add box"},
  {"kinds", "Shortcut buttons"},
  {"comingUp", "Coming up"},
  {"accounts", "Accounts"},
  {"budgets", "Budgets"},
  {"week", "Last 7 days chart"},
  {"people", "People"},
  {"recent", "Recent entries"},
 };

 static boolean on(Store db, String key) { return db.flag("home:" + key, true); }

 static void open(MainActivity a) {
  Kit.page(a, "Customise Home", null, null, c -> {
   c.addView(Ui.text(a, "Choose which sections appear on Home. Your entries are not affected.", 13.5f, Ui.muted, false), Ui.lpm(-2, -2, 2, 0, 0, 10));
   LinearLayout card = Ui.card(a); card.setPadding(Ui.dp(18), Ui.dp(4), Ui.dp(18), Ui.dp(4));
   for (int i = 0; i < SECTIONS.length; i++) {
    final String key = SECTIONS[i][0]; String label = SECTIONS[i][1];
    if (i > 0) card.addView(Ui.divider(a));
    LinearLayout row = Ui.row(a); row.setPadding(0, Ui.dp(13), 0, Ui.dp(13));
    row.addView(Ui.text(a, label, 15.5f, Ui.ink, true), Ui.weight(1));
    Switch sw = Ui.toggle(a); sw.setChecked(on(a.db, key)); sw.setContentDescription(L.t(label));
    row.addView(sw);
    sw.setOnCheckedChangeListener((v, on) -> a.db.setPref("home:" + key, on ? "1" : "0"));
    card.addView(row);
   }
   c.addView(card);
   TextView reset = Ui.button(a, "Show everything", false);
   c.addView(reset, Ui.lpm(-1, -2, 0, 14, 0, 0));
   Ui.tap(reset, () -> { for (String[] s : SECTIONS) a.db.setPref("home:" + s[0], "1"); a.closeAll(); open(a); });
  });
 }
}
