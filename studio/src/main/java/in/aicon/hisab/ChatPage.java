package in.aicon.hisab;

import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** "Ask Hisab": a rule-based chat over the user's own entries. Nothing leaves the phone. */
final class ChatPage {
 private ChatPage() { }
 /** [who, text]: who is "me" or "hisab". Kept while the app is open. */
 static final List<String[]> LOG = new ArrayList<>();
 static ChatBrain.Reply pending;

 static void open(MainActivity a) {
  pending = null;
  Kit.page(a, "Ask Hisab", null, null, c -> build(a, c));
 }

 static void build(MainActivity a, LinearLayout c) {
  c.removeAllViews();
  EditText in = Sheets.field(a, "Ask or type an entry (tea 20)", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
  in.setTag("chatInput");
  TextView send = Ui.button(a, "Send", true);
  Runnable go = () -> send(a, c, in);
  Ui.tap(send, go);
  in.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
  in.setOnEditorActionListener((v, id, ev) -> { go.run(); return true; });
  c.addView(in, Ui.lpm(-1, -2, 0, 0, 0, 8)); c.addView(send, Ui.lpm(-1, -2, 0, 0, 0, 12));
  if (pending != null && !pending.act.isEmpty()) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, pending.text, 15, Ui.ink, true));
   TextView yes = Ui.button(a, "Confirm", true), no = Ui.button(a, "Cancel", false);
   Ui.tap(yes, () -> confirm(a, c)); Ui.tap(no, () -> { LOG.add(new String[]{"hisab", "Cancelled. Nothing was changed."}); pending = null; build(a, c); });
   card.addView(yes, Ui.lpm(-1, -2, 0, 10, 0, 0)); card.addView(no, Ui.lpm(-1, -2, 0, 8, 0, 0));
   c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10));
  }
  if (LOG.isEmpty()) c.addView(Ui.text(a, ChatBrain.help(L.isMl()), 14, Ui.muted, false));
  for (int i = LOG.size() - 1; i >= 0; i--) {
   String[] m = LOG.get(i); boolean me = "me".equals(m[0]);
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, me ? "You" : "Hisab", 11, Ui.muted, true));
   card.addView(Ui.text(a, m[1], 14.5f, Ui.ink, me));
   c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 8));
  }
 }

 static void send(MainActivity a, LinearLayout c, EditText in) {
  String s = in.getText().toString().trim(); if (s.isEmpty()) return;
  LOG.add(new String[]{"me", s});
  ChatBrain.Reply r = ChatBrain.ask(a.db, s, LocalDate.now());
  pending = r.act.isEmpty() ? null : r;
  if (r.act.isEmpty()) LOG.add(new String[]{"hisab", r.text});
  while (LOG.size() > 60) LOG.remove(0);
  build(a, c);
 }

 static void confirm(MainActivity a, LinearLayout c) {
  ChatBrain.Reply r = pending; pending = null; if (r == null || r.tx == null) { build(a, c); return; }
  String done;
  switch (r.act) {
   case "add": a.db.save(r.tx); done = "Added: " + Fmt.money(r.tx.amount) + "  " + r.tx.note; break;
   case "delete": a.db.trashTx(r.tx.id); done = "Deleted. You can bring it back from the Trash."; break;
   case "edit": a.db.save(r.tx); done = "Changed to " + Fmt.money(r.tx.amount) + "."; break;
   default: done = "";
  }
  LOG.add(new String[]{"hisab", L.isMl() ? "ശരി, ചെയ്തു. " + done : done});
  try { a.refresh(); } catch (Exception ignored) { }
  build(a, c);
 }
}
