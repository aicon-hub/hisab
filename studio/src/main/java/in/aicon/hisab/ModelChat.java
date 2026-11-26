package in.aicon.hisab;

import android.widget.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The AI model half of Ask Hisab: a model downloaded to this phone answers; Hisab code supplies exact facts when Read is on. */
final class ModelChat {
 private ModelChat() {}
 static final String SYSTEM = "You are a helpful assistant inside the Hisab money app. Answer like a normal AI chat, clearly and briefly. You cannot see the user's Hisab entries now, so never invent their numbers. If asked about their own data, tell them to switch on Read in the chat settings.";
 static final String SYSTEM_DATA = "You are the assistant inside the Hisab money app. The user allowed you to read their Hisab data. Use ONLY the facts below for anything about their money. The numbers in the facts are exact: copy them, never change, add up or invent numbers. If the facts do not answer the question, say what is missing. Answer briefly.\nFACTS FROM HISAB:\n";
 static TextView liveView, liveStat;
 private static final android.os.Handler H = new android.os.Handler(android.os.Looper.getMainLooper());

 static boolean ready(MainActivity a) { return AiEngine.installed(a) && current(a) != null; }

 /** Engine and model set-up cards. Returns true when a model is ready to answer. */
 static boolean setup(MainActivity a, LinearLayout box, Runnable rebuild) {
  if (!AiEngine.installed(a)) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, "Step 1: the AI engine (about " + AiModels.mb(AiEngine.size()) + ")", 15, Ui.ink, true));
   card.addView(Ui.text(a, "A small program that runs the model on this phone (llama.cpp, MIT licence). It is downloaded once from Hisab's GitHub release, checked against its SHA-256, and kept in Hisab's private storage. Nothing about you is sent.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   if (AiEngine.dl) {
    DlBar bar = new DlBar(a); bar.set(AiEngine.done / (float) AiEngine.size(), true); card.addView(bar, Ui.lpm(-1, -2, 0, 10, 0, 0));
    TextView st = Ui.text(a, (int) Math.min(100, AiEngine.done * 100 / AiEngine.size()) + "%", 12.5f, Ui.ink, true); card.addView(st, Ui.lpm(-1, -2, 0, 6, 0, 0));
    poll(a, bar, st);
   } else {
    TextView go = Ui.button(a, "Download engine", true); Ui.tap(go, () -> Sheets.confirm(a, "Download the AI engine?", AiModels.mb(AiEngine.size()) + " from github.com. It runs models on this phone only.", "Download", () -> {
     AiEngine.download(a, () -> { }, err -> a.runOnUiThread(() -> { if (err != null) Toast.makeText(a, err, Toast.LENGTH_LONG).show(); rebuild.run(); })); rebuild.run(); }));
    card.addView(go, Ui.lpm(-1, -2, 0, 10, 0, 0));
    if (AiEngine.dlError != null) card.addView(Ui.text(a, AiEngine.dlError, 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   }
   box.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10)); return false;
  }
  if (installed(a).isEmpty()) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, "Step 2: download a model", 15, Ui.ink, true));
   card.addView(Ui.text(a, "Pick one for your phone's memory. It stays on this phone.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 4, 0, 0));
   TextView go = Ui.button(a, "Choose and download a model", true); Ui.tap(go, () -> ModelsPage.open(a)); card.addView(go, Ui.lpm(-1, -2, 0, 10, 0, 0));
   box.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10)); return false;
  }
  return true;
 }

 /** Model picker and load status. */
 static void modelBar(MainActivity a, LinearLayout box, Runnable rebuild) {
  AiModels.M cur = current(a); if (cur == null) return;
  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout pr = Ui.row(a);
  for (AiModels.M x : installed(a)) { TextView p = Ui.pill(a, x.name.replace(" Instruct", ""), x == cur); Ui.tap(p, () -> { if (AiEngine.running || AiEngine.loading) return; a.db.setPref("aiModel", x.id); rebuild.run(); }); pr.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0)); }
  hs.addView(pr); box.addView(hs, Ui.lpm(-1, -2, 0, 0, 0, 4));
  TextView st = Ui.text(a, "", 12, Ui.muted, false); box.addView(st, Ui.lpm(-1, -2, 0, 0, 0, 6));
  AiEngine.warm(a, cur, () -> a.runOnUiThread(() -> status(a, st)));
  status(a, st); if (AiEngine.loading) pollStatus(a, st);
 }
 static void status(MainActivity a, TextView st) {
  AiModels.M cur = current(a); if (cur == null) return;
  if (AiEngine.warmError != null && !AiEngine.loading && !cur.id.equals(AiEngine.loadedId)) st.setText(AiEngine.warmError);
  else if (AiEngine.loading) st.setText("Loading " + cur.name + "... " + (System.currentTimeMillis() - AiEngine.tWarm) / 1000 + " s");
  else if (cur.id.equals(AiEngine.loadedId)) st.setText("Model loaded and ready" + (AiEngine.loadMs > 0 ? " (took " + String.format(java.util.Locale.US, "%.1f", AiEngine.loadMs / 1000.0) + " s)" : "") + ". It frees its memory 5 minutes after the last answer.");
  else st.setText("Model not loaded. The first question loads it.");
 }
 static void pollStatus(MainActivity a, TextView st) { H.postDelayed(() -> { if (!st.isAttachedToWindow()) return; status(a, st); if (AiEngine.loading) pollStatus(a, st); }, 500); }

 static List<AiModels.M> installed(MainActivity a) { List<AiModels.M> l = new ArrayList<>(); for (AiModels.M m : AiModels.ALL) if (AiModels.installed(a, m)) l.add(m); return l; }
 static AiModels.M current(MainActivity a) {
  List<AiModels.M> h = installed(a); if (h.isEmpty()) return null; String id = a.db.pref("aiModel", "");
  for (AiModels.M m : h) if (m.id.equals(id)) return m; return h.get(0);
 }
 static void poll(MainActivity a, DlBar bar, TextView st) {
  H.postDelayed(() -> { if (!AiEngine.dl || !bar.isAttachedToWindow()) return; bar.set(AiEngine.done / (float) AiEngine.size(), true); st.setText((int) Math.min(100, AiEngine.done * 100 / AiEngine.size()) + "%  " + AiModels.mb(AiEngine.done) + " of " + AiModels.mb(AiEngine.size())); poll(a, bar, st); }, 250);
 }

 static void paint() {
  if (liveView == null || liveStat == null) return;
  String t = AiEngine.live.toString(); liveView.setText(t.isEmpty() ? "..." : t);
  String s = AiEngine.running ? AiEngine.stage : "";
  if (AiEngine.tokens > 0) s = AiEngine.tokens + " pieces, " + String.format(java.util.Locale.US, "%.1f", AiEngine.tps()) + " per second";
  liveStat.setText(s + (AiEngine.running ? (s.isEmpty() ? "" : "  ") + (System.currentTimeMillis() - AiEngine.tStart) / 1000 + " s" : ""));
 }

 /** Exact facts for the model, computed by Hisab code from the user's own entries. */
 static String facts(MainActivity a, String q, ChatBrain.Reply r) {
  StringBuilder b = new StringBuilder(); LocalDate today = LocalDate.now();
  b.append("Today is ").append(today).append(". Currency amounts are shown the way Hisab shows them.\n");
  if (r != null && !r.intent.isEmpty() && r.act.isEmpty()) {
   b.append("About the question (").append(r.intent).append("): ").append(r.text).append("\n");
   String src = ChatPage.sources(a, r); if (!src.isEmpty()) b.append("Entries used:\n").append(src).append("\n");
  }
  String[] qs = {"how much did I spend this month", "income this month", "top categories this month", "account balances", "who owes whom"};
  for (String x : qs) {
   try { ChatBrain.Reply s = ChatBrain.ask(a.db, x, today); if (!s.intent.isEmpty() && s.act.isEmpty() && b.indexOf(s.text) < 0) b.append(s.text).append("\n"); } catch (Exception ignored) { }
   if (b.length() > 2200) break;
  }
  return b.length() > 2400 ? b.substring(0, 2400) : b.toString();
 }

 /** Sends the typed text to the model. facts is null when Read is off. */
 static void send(MainActivity a, String text, String facts, List<String[]> log, Runnable rebuild) {
  AiModels.M m = current(a);
  if (m == null || !AiEngine.installed(a)) { Toast.makeText(a, "Download the engine and a model first.", Toast.LENGTH_SHORT).show(); return; }
  List<String> roles = new ArrayList<>(), texts = new ArrayList<>(); roles.add("system"); texts.add(facts == null ? SYSTEM : SYSTEM_DATA + facts);
  int budget = 3500; List<String[]> hist = new ArrayList<>();
  for (int i = log.size() - 2; i >= 0 && hist.size() < 6; i--) {
   String[] e = log.get(i); boolean me = "me".equals(e[0]); boolean mod = e.length > 2 && e[2].startsWith("AI model");
   if (!me && !mod) continue; if (budget - e[1].length() < 0) break; budget -= e[1].length(); hist.add(0, e);
  }
  for (String[] e : hist) { roles.add("me".equals(e[0]) ? "user" : "assistant"); texts.add(e[1]); }
  roles.add("user"); texts.add(text.length() > 4000 ? text.substring(0, 4000) : text);
  final String used = facts == null ? "" : " Hisab data used (exact): " + (facts.length() > 500 ? facts.substring(0, 500) + "..." : facts).replace("\n", " ");
  AiEngine.ask(a, m, roles.toArray(new String[0]), texts.toArray(new String[0]), facts == null ? 0.7f : 0.2f,
   () -> a.runOnUiThread(ModelChat::paint),
   err -> a.runOnUiThread(() -> {
    String ans = AiEngine.live.toString().trim();
    if (!ans.isEmpty()) log.add(new String[]{"hisab", ans, "AI model " + (facts == null ? "(general knowledge, not checked against your data). " : "with your Hisab data. ") + AiEngine.lastNote + used});
    else if (err != null) log.add(new String[]{"hisab", err, ""});
    else log.add(new String[]{"hisab", "The model gave no answer.", ""});
    if (err != null && !ans.isEmpty()) log.add(new String[]{"hisab", "Stopped: " + err, ""});
    liveView = null; liveStat = null; rebuild.run();
   }));
  rebuild.run();
 }
}
