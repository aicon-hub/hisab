package in.aicon.hisab;

import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** "Ask Hisab": a rule-based chat over the user's own entries. Nothing leaves the phone. */
final class ChatPage {
 private ChatPage() { }
 /** [who, text, sources]: who is "me" or "hisab". Kept while the app is open. */
 static final List<String[]> LOG = new ArrayList<>();
 static ChatBrain.Reply pending;
 /** The run being shown in the flow card (real steps from the engine). */
 static ChatBrain.Reply run;
 static boolean running;
 static LinearLayout lastC;
 static String draft = "";
 static long clock;
 static final long STEP_MS = 420;
 private static final android.os.Handler H = new android.os.Handler(android.os.Looper.getMainLooper());

 static void open(MainActivity a) {
  pending = null; run = null; running = false;
  Kit.page(a, "Ask Hisab", null, null, c -> { lastC = c; build(a, c); });
 }

 static void build(MainActivity a, LinearLayout c) {
  c.removeAllViews();
  EditText in = Sheets.field(a, "Ask or type an entry (tea 20)", draft, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
  in.setTag("chatInput");
  FrameLayout mic = Ui.badge(a, "mic", 42, Ui.surface2, Ui.ink); mic.setContentDescription("Say it");
  TextView send = Ui.button(a, "Send", true);
  Runnable go = () -> send(a, c, in);
  Ui.tap(send, go);
  Ui.tap(mic, () -> {
   if (a.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) { a.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 6); return; }
   Voice.listen(a, v -> { in.setText(v); in.setSelection(v.length()); go.run(); });
  });
  in.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
  in.setOnEditorActionListener((v, id, ev) -> { go.run(); return true; });
  LinearLayout row = new LinearLayout(a); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
  row.addView(in, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(mic, Ui.lpm(Ui.dp(42), Ui.dp(42), 8, 0, 0, 0));
  c.addView(row, Ui.lpm(-1, -2, 0, 0, 0, 8)); c.addView(send, Ui.lpm(-1, -2, 0, 0, 0, 12));
  c.addView(Ui.text(a, "Hisab mode: answers only from your own Hisab data, on this phone.", 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 10));
  { TextView mp = Ui.pill(a, "AI models", false); Ui.tap(mp, () -> ModelsPage.open(a)); c.addView(mp, Ui.lpm(-2, -2, 0, 0, 0, 10)); }
  if (run != null) c.addView(runCard(a, c), Ui.lpm(-1, -2, 0, 0, 0, 10));
  if (!running && pending != null && !pending.act.isEmpty()) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, pending.text, 15, Ui.ink, true));
   TextView yes = Ui.button(a, "Confirm", true), no = Ui.button(a, "Cancel", false);
   Ui.tap(yes, () -> confirm(a, c)); Ui.tap(no, () -> { LOG.add(new String[]{"hisab", "Cancelled. Nothing was changed.", ""}); pending = null; build(a, c); });
   card.addView(yes, Ui.lpm(-1, -2, 0, 10, 0, 0)); card.addView(no, Ui.lpm(-1, -2, 0, 8, 0, 0));
   c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10));
  }
  if (LOG.isEmpty() && run == null) c.addView(Ui.text(a, ChatBrain.help(L.isMl()), 14, Ui.muted, false));
  for (int i = LOG.size() - 1; i >= 0; i--) {
   String[] m = LOG.get(i); boolean me = "me".equals(m[0]);
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, me ? "You" : "Hisab", 11, Ui.muted, true));
   card.addView(Ui.text(a, m[1], 14.5f, Ui.ink, me));
   if (!me && m.length > 2 && !m[2].isEmpty()) {
    card.addView(Ui.text(a, "Sources", 11, Ui.muted, true), Ui.lpm(-1, -2, 0, 10, 0, 2));
    card.addView(Ui.text(a, m[2], 12, Ui.muted, false));
   }
   c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 8));
  }
  Privacy.toggle(a, c, "Show the steps while answering", "chatAnim", true, () -> { });
 }

 /** The live tracker: a diagram plus the real steps the engine took, each with the time it really took. */
 static LinearLayout runCard(MainActivity a, LinearLayout c) {
  LinearLayout card = Ui.card(a);
  FlowView f = new FlowView(a); long total = f.schedule(STEP_MS);
  card.addView(f, new LinearLayout.LayoutParams(-1, -2));
  TextView status = Ui.text(a, "", 13, Ui.ink, true); card.addView(status, Ui.lpm(-1, -2, 0, 6, 0, 0));
  TextView steps = Ui.text(a, "", 11.5f, Ui.muted, false); card.addView(steps, Ui.lpm(-1, -2, 0, 6, 0, 0));
  card.addView(Ui.text(a, "Local, on this phone. No network.", 11, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
  paint(f, status, steps, running ? clock : total + 1000);
  if (running) tick(a, c, f, status, steps, total);
  return card;
 }

 static void paint(FlowView f, TextView status, TextView steps, long ms) {
  f.at(ms); StringBuilder b = new StringBuilder(); String cur = "";
  List<Object[]> sorted = new ArrayList<>(run.steps); sorted.sort((x, y) -> Integer.compare((Integer) x[0], (Integer) y[0]));
  for (Object[] s : sorted) {
   int n = (Integer) s[0];
   if (f.start[n] >= 0 && f.start[n] <= ms) { cur = (String) s[1]; b.append(f.end[n] <= ms ? "\u2713 " : "\u2022 ").append((String) s[1]).append("  (").append(((Long) s[2]) < 1000 ? "<1" : "" + ((Long) s[2]) / 1000).append(" ms)\n"); }
  }
  status.setText(ms > f.end[10] ? "Done" : cur.isEmpty() ? "Starting..." : cur);
  steps.setText(b.toString().trim());
 }

 static void tick(MainActivity a, LinearLayout c, FlowView f, TextView status, TextView steps, long total) {
  H.postDelayed(new Runnable() {
   @Override public void run() {
    if (!running || c.getParent() == null) return;
    clock += 33; paint(f, status, steps, clock);
    if (clock > total + 150) finish(a, c); else H.postDelayed(this, 33);
   }
  }, 33);
 }

 static void finish(MainActivity a, LinearLayout c) {
  running = false; ChatBrain.Reply r = run; if (r == null) return;
  if (r.act.isEmpty()) LOG.add(new String[]{"hisab", r.text, sources(a, r)});
  build(a, c);
 }

 static String sources(MainActivity a, ChatBrain.Reply r) {
  if (r.sources.isEmpty()) return "";
  Map<Long, Store.Cat> cm = a.db.categoryMap(); StringBuilder b = new StringBuilder(); int n = 0;
  for (Store.Tx x : r.sources) { if (n++ >= 8) break; b.append(ChatBrain.line(a.db, x, cm)).append("\n"); }
  if (r.sources.size() > 8) b.append("+ ").append(r.sources.size() - 8).append(" more");
  return b.toString().trim();
 }

 static void send(MainActivity a, LinearLayout c, EditText in) {
  String s = in.getText().toString().trim(); if (s.isEmpty() || running) return;
  LOG.add(new String[]{"me", s, ""});
  ChatBrain.Reply r = ChatBrain.ask(a.db, s, LocalDate.now());
  pending = r.act.isEmpty() ? null : r; run = r;
  while (LOG.size() > 60) LOG.remove(0);
  if (a.db.flag("chatAnim", true)) { running = true; clock = 0; build(a, c); }
  else { running = false; if (r.act.isEmpty()) LOG.add(new String[]{"hisab", r.text, sources(a, r)}); build(a, c); }
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
  LOG.add(new String[]{"hisab", L.isMl() ? "ശരി, ചെയ്തു. " + done : done, ""});
  try { a.refresh(); } catch (Exception ignored) { }
  build(a, c);
 }
}
