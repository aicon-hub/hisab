package in.aicon.hisab;

import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** "Ask Hisab": a chat where a model on this phone answers. Reading and writing the user's data are separate switches, both off until he turns them on. */
final class ChatPage {
 private ChatPage() { }
 /** [who, text, note]: who is "me" or "hisab". */
 static final List<String[]> LOG = new ArrayList<>();
 static ChatBrain.Reply pending;
 static String draft = "";
 private static LinearLayout top, msgs; private static ScrollView scroll; private static MainActivity act;
 private static final android.os.Handler H = new android.os.Handler(android.os.Looper.getMainLooper());

 static boolean read(MainActivity a) { return a.db.flag("chatRead", false); }
 static boolean write(MainActivity a) { return a.db.flag("chatWrite", false); }

 static void open(MainActivity a) {
  pending = null; act = a; try { AiEngine.threadsPref = Integer.parseInt(a.db.pref("aiThreads", "0")); } catch (Exception ignored) { }
  if (LOG.isEmpty() && a.db.flag("chatSave", false)) load(a);
  final MainActivity.Layer[] ref = new MainActivity.Layer[1];
  ref[0] = a.open(() -> {
   LinearLayout outer = Ui.col(a);
   outer.addView(Kit.bar(a, "Ask Hisab", ref, null, null));
   top = Ui.col(a); top.setPadding(Ui.dp(16), 0, Ui.dp(16), 0); outer.addView(top, new LinearLayout.LayoutParams(-1, -2));
   scroll = new ScrollView(a); scroll.setVerticalScrollBarEnabled(false); msgs = Ui.col(a); msgs.setPadding(Ui.dp(14), Ui.dp(6), Ui.dp(14), Ui.dp(10)); scroll.addView(msgs);
   outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
   outer.addView(footer(a), new LinearLayout.LayoutParams(-1, -2));
   refresh(a); return outer;
  }, false);
 }

 static View footer(MainActivity a) {
  LinearLayout f = Ui.col(a); f.setPadding(Ui.dp(14), Ui.dp(6), Ui.dp(14), Ui.dp(16));
  EditText in = Sheets.field(a, "Ask anything, or type an entry (tea 20)", draft, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
  in.setTag("chatInput"); in.setMaxLines(4);
  android.widget.FrameLayout mic = Ui.badge(a, "mic", 42, Ui.surface2, Ui.ink); mic.setContentDescription("Say it");
  android.widget.FrameLayout send = Ui.badge(a, "right", 42, Ui.accent, Ui.onAccent); send.setContentDescription("Send");
  Runnable go = () -> { String s = in.getText().toString().trim(); if (s.isEmpty() || AiEngine.running) return; in.setText(""); draft = ""; submit(a, s); };
  Ui.tap(send, go);
  Ui.tap(mic, () -> {
   if (a.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) { a.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 6); return; }
   Voice.listen(a, v -> { in.setText(v); in.setSelection(v.length()); go.run(); });
  });
  in.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
  in.setOnEditorActionListener((v, id, ev) -> { go.run(); return true; });
  LinearLayout row = Ui.row(a); row.setGravity(Gravity.CENTER_VERTICAL);
  row.addView(in, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(mic, Ui.lpm(Ui.dp(42), Ui.dp(42), 8, 0, 0, 0)); row.addView(send, Ui.lpm(Ui.dp(42), Ui.dp(42), 8, 0, 0, 0));
  f.addView(row); return f;
 }

 /** Redraws the set-up area and the messages; the input bar stays untouched so typing is not lost. */
 static void refresh(MainActivity a) {
  if (top == null || msgs == null || top.getParent() == null) return;
  top.removeAllViews(); msgs.removeAllViews();
  boolean ok = ModelChat.setup(a, top, () -> refresh(a));
  if (ok) ModelChat.modelBar(a, top, () -> refresh(a));
  LinearLayout tools = Ui.row(a);
  TextView st = Ui.pill(a, "Chat settings", false), nw = Ui.pill(a, "New chat", false), md = Ui.pill(a, "AI models", false);
  Ui.tap(st, () -> settings(a)); Ui.tap(nw, () -> { if (AiEngine.running) return; LOG.clear(); pending = null; save(a); refresh(a); }); Ui.tap(md, () -> ModelsPage.open(a));
  tools.addView(st, Ui.lpm(-2, -2, 0, 0, 8, 0)); tools.addView(nw, Ui.lpm(-2, -2, 0, 0, 8, 0)); tools.addView(md);
  top.addView(tools, Ui.lpm(-1, -2, 0, 0, 0, 4));
  top.addView(Ui.text(a, (read(a) ? "Hisab data: Read on" : "Hisab data: Read off") + (write(a) ? ", Write on (always with Confirm)" : ", Write off"), 11.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 4));
  if (LOG.isEmpty() && !AiEngine.running) {
   msgs.addView(Ui.text(a, "Ask me anything. I run on this phone, no internet.", 14.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 12, 0, 8));
   String[] chips = read(a) ? new String[]{"How much did I spend this month?", "Who owes me?", "Top categories"} : new String[]{"Explain a budget in simple words", "Ideas to save money"};
   for (String ch : chips) { TextView p = Ui.pill(a, ch, false); Ui.tap(p, () -> submit(a, ch)); msgs.addView(p, Ui.lpm(-2, -2, 0, 0, 0, 8)); }
  }
  for (String[] m : LOG) bubble(a, m);
  if (!AiEngine.running && pending != null && !pending.act.isEmpty()) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, pending.text, 15, Ui.ink, true));
   TextView yes = Ui.button(a, "Confirm", true), no = Ui.button(a, "Cancel", false);
   Ui.tap(yes, () -> confirm(a)); Ui.tap(no, () -> { LOG.add(new String[]{"hisab", "Cancelled. Nothing was changed.", ""}); pending = null; save(a); refresh(a); });
   card.addView(yes, Ui.lpm(-1, -2, 0, 10, 0, 0)); card.addView(no, Ui.lpm(-1, -2, 0, 8, 0, 0));
   msgs.addView(card, Ui.lpm(-1, -2, 0, 4, 0, 8));
  }
  if (AiEngine.running) live(a);
  scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
 }

 static void bubble(MainActivity a, String[] m) {
  boolean me = "me".equals(m[0]);
  LinearLayout b = Ui.col(a); b.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
  GradientDrawable g = new GradientDrawable(); g.setColor(me ? Ui.accent : Ui.surface); g.setCornerRadius(Ui.dp(18)); b.setBackground(g);
  b.addView(Ui.text(a, m[1], 14.5f, me ? Ui.onAccent : Ui.ink, false));
  if (!me) {
   if (m.length > 2 && !m[2].isEmpty()) b.addView(Ui.text(a, m[2], 11, Ui.muted, false), Ui.lpm(-1, -2, 0, 8, 0, 0));
   TextView cp = Ui.text(a, "Copy", 11.5f, Ui.muted, true); cp.setPadding(0, Ui.dp(8), Ui.dp(12), 0);
   Ui.tap(cp, () -> { android.content.ClipboardManager cm = (android.content.ClipboardManager) a.getSystemService(android.content.Context.CLIPBOARD_SERVICE); if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("Hisab", m[1])); a.toast("Copied", null); });
   b.addView(cp);
  }
  LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.gravity = me ? Gravity.END : Gravity.START; lp.bottomMargin = Ui.dp(8);
  if (me) lp.leftMargin = Ui.dp(48); else lp.rightMargin = Ui.dp(32);
  msgs.addView(b, lp);
 }

 static void live(MainActivity a) {
  LinearLayout b = Ui.col(a); b.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
  GradientDrawable g = new GradientDrawable(); g.setColor(Ui.surface); g.setCornerRadius(Ui.dp(18)); b.setBackground(g);
  ModelChat.liveView = Ui.text(a, "...", 14.5f, Ui.ink, false); b.addView(ModelChat.liveView);
  ModelChat.liveStat = Ui.text(a, "", 11.5f, Ui.muted, false); b.addView(ModelChat.liveStat, Ui.lpm(-1, -2, 0, 6, 0, 0));
  TextView stop = Ui.text(a, "Stop", 12, Ui.ink, true); stop.setPadding(0, Ui.dp(8), Ui.dp(12), 0); Ui.tap(stop, AiEngine::stop); b.addView(stop);
  ModelChat.paint();
  LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.rightMargin = Ui.dp(32); lp.bottomMargin = Ui.dp(8); msgs.addView(b, lp);
  scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
 }

 /** Entry point for typed or spoken text. The first message asks whether to keep chat history. */
 static void submit(MainActivity a, String s) {
  if (!a.db.flag("chatAsked", false)) { askHistory(a, () -> run(a, s)); return; }
  run(a, s);
 }

 static void askHistory(MainActivity a, Runnable then) {
  LinearLayout c = Sheets.sheetBody(a, "Keep this chat?");
  c.addView(Ui.text(a, "Should Hisab store your chat history on this phone? If you say no, nothing is saved and the chat is gone when you close it. You can change this later in Chat settings.", 14, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 14));
  final MainActivity.Layer[] l = new MainActivity.Layer[1];
  TextView yes = Ui.button(a, "Yes, store it", true), no = Ui.button(a, "No, do not store", false);
  Ui.tap(yes, () -> { a.db.setPref("chatAsked", "1"); a.db.setPref("chatSave", "1"); a.close(l[0]); then.run(); });
  Ui.tap(no, () -> { a.db.setPref("chatAsked", "1"); a.db.setPref("chatSave", "0"); a.close(l[0]); then.run(); });
  c.addView(yes, Ui.lpm(-1, -2, 0, 0, 0, 8)); c.addView(no);
  l[0] = Sheets.show(a, c);
 }

 static void settings(MainActivity a) {
  LinearLayout c = Sheets.sheetBody(a, "Chat settings");
  Privacy.toggle(a, c, "Read my Hisab data", "chatRead", false, () -> refresh(a));
  c.addView(Ui.text(a, "The model sees your numbers. Hisab code works them out exactly and gives them to the model, which only words the answer. Off: the model never sees your data.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 12));
  Privacy.toggle(a, c, "Write to my Hisab data", "chatWrite", false, () -> refresh(a));
  c.addView(Ui.text(a, "Typing an entry like \"tea 20\" in the chat can add, edit or delete. Every change shows a Confirm card first. Nothing changes without Confirm.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 12));
  Privacy.toggle(a, c, "Store chat history on this phone", "chatSave", false, () -> { a.db.setPref("chatAsked", "1"); if (!a.db.flag("chatSave", false)) a.db.setPref("chatLog", ""); else save(a); });
  TextView clr = Ui.button(a, "Clear saved history", false); Ui.tap(clr, () -> { LOG.clear(); a.db.setPref("chatLog", ""); a.toast("History cleared", null); refresh(a); });
  c.addView(clr, Ui.lpm(-1, -2, 0, 8, 0, 0));
  c.addView(Ui.text(a, "Speed", 14, Ui.ink, true), Ui.lpm(-1, -2, 0, 18, 0, 4));
  c.addView(Ui.text(a, AiEngine.diag() + (AiEngine.loadMs > 0 ? " Model load " + String.format(java.util.Locale.US, "%.1f", AiEngine.loadMs / 1000.0) + " s." : "") + (AiEngine.lastNote.isEmpty() ? "" : " Last answer: " + AiEngine.lastNote), 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 8));
  c.addView(Ui.text(a, "Threads (changes reload the model)", 12, Ui.muted, true), Ui.lpm(-1, -2, 0, 0, 0, 4));
  LinearLayout tr = Ui.row(a); int cur = a.db.pref("aiThreads", "0").equals("0") ? 0 : Integer.parseInt(a.db.pref("aiThreads", "0"));
  for (int t : new int[]{0, 2, 3, 4, 6}) { TextView p = Ui.pill(a, t == 0 ? "Auto" : "" + t, t == cur); Ui.tap(p, () -> { a.db.setPref("aiThreads", "" + t); AiEngine.threadsPref = t; refresh(a); }); tr.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0)); }
  c.addView(tr, Ui.lpm(-1, -2, 0, 0, 0, 8));
  TextView sp = Ui.button(a, "Speed test (one short answer)", false); Ui.tap(sp, () -> { if (!AiEngine.running) submit(a, "Say hello in one short sentence."); });
  c.addView(sp, Ui.lpm(-1, -2, 0, 4, 0, 0));
  Sheets.show(a, c);
 }

 static void run(MainActivity a, String s) {
  LOG.add(new String[]{"me", s, ""}); while (LOG.size() > 60) LOG.remove(0);
  ChatBrain.Reply r = ChatBrain.ask(a.db, s, LocalDate.now());
  if (!r.act.isEmpty()) {
   if (write(a) && r.tx != null) { pending = r; save(a); refresh(a); return; }
   LOG.add(new String[]{"hisab", "To add, change or delete entries from chat, turn on Write in Chat settings. Nothing was changed.", ""}); save(a); refresh(a); return;
  }
  String facts = read(a) ? ModelChat.facts(a, s, r) : null;
  String ground = read(a) && !r.intent.isEmpty() ? r.text : null;
  ModelChat.send(a, s, facts, ground, LOG, () -> { save(a); refresh(a); });
 }

 static void save(MainActivity a) {
  if (!a.db.flag("chatSave", false)) return;
  try {
   org.json.JSONArray arr = new org.json.JSONArray();
   for (String[] m : LOG) { org.json.JSONArray e = new org.json.JSONArray(); e.put(m[0]); e.put(m[1]); e.put(m.length > 2 ? m[2] : ""); arr.put(e); }
   a.db.setPref("chatLog", arr.toString());
  } catch (Exception ignored) { }
 }

 static void load(MainActivity a) {
  try {
   org.json.JSONArray arr = new org.json.JSONArray(a.db.pref("chatLog", "[]"));
   for (int i = 0; i < arr.length(); i++) { org.json.JSONArray e = arr.getJSONArray(i); LOG.add(new String[]{e.getString(0), e.getString(1), e.getString(2)}); }
  } catch (Exception ignored) { }
 }

 static String sources(MainActivity a, ChatBrain.Reply r) {
  if (r.sources.isEmpty()) return "";
  Map<Long, Store.Cat> cm = a.db.categoryMap(); StringBuilder b = new StringBuilder(); int n = 0;
  for (Store.Tx x : r.sources) { if (n++ >= 8) break; b.append(ChatBrain.line(a.db, x, cm)).append("\n"); }
  if (r.sources.size() > 8) b.append("+ ").append(r.sources.size() - 8).append(" more");
  return b.toString().trim();
 }


 static void confirm(MainActivity a) {
  ChatBrain.Reply r = pending; pending = null; if (r == null || r.tx == null) { refresh(a); return; }
  String done;
  switch (r.act) {
   case "add": a.db.save(r.tx); done = "Added: " + Fmt.money(r.tx.amount) + "  " + r.tx.note; break;
   case "delete": a.db.trashTx(r.tx.id); done = "Deleted. You can bring it back from the Trash."; break;
   case "edit": a.db.save(r.tx); done = "Changed to " + Fmt.money(r.tx.amount) + "."; break;
   default: done = "";
  }
  LOG.add(new String[]{"hisab", L.isMl() ? "ശരി, ചെയ്തു. " + done : done, ""});
  try { a.refresh(); } catch (Exception ignored) { }
  save(a); refresh(a);
 }
}
