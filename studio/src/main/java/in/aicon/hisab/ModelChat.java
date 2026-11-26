package in.aicon.hisab;

import android.widget.*;
import java.util.ArrayList;
import java.util.List;

/** The "All-model answers" half of Ask Hisab: a normal AI chat run by a downloaded model on this phone. */
final class ModelChat {
 private ModelChat() {}
 static final String SYSTEM = "You are a helpful assistant inside the Hisab money app. Answer like a normal AI chat, clearly and briefly. You cannot see the user's Hisab entries in this mode, so never invent their numbers. If asked about their own data, tell them to switch to Hisab only answers.";
 static TextView liveView; static TextView liveStat; static boolean pendingShow;
 private static final android.os.Handler H = new android.os.Handler(android.os.Looper.getMainLooper());

 static boolean on(MainActivity a) { return "model".equals(a.db.pref("chatMode", "hisab")); }

 /** Mode switch at the very top, plus the engine and model status in model mode. */
 static void top(MainActivity a, LinearLayout c, Runnable rebuild) {
  LinearLayout row = Ui.row(a); boolean m = on(a);
  TextView h = Ui.pill(a, "Hisab only answers", !m), g = Ui.pill(a, "All-model answers", m);
  Ui.tap(h, () -> { if (AiEngine.running) return; a.db.setPref("chatMode", "hisab"); rebuild.run(); });
  Ui.tap(g, () -> { if (AiEngine.running) return; a.db.setPref("chatMode", "model"); rebuild.run(); });
  row.addView(h, Ui.lpm(-2, -2, 0, 0, 8, 0)); row.addView(g);
  c.addView(row, Ui.lpm(-1, -2, 0, 0, 0, 6));
  c.addView(Ui.text(a, m ? "All-model answers: a downloaded model answers like a normal AI chat from general knowledge. It does not see your Hisab data and can be wrong, so do not use it for your own numbers."
    : "Hisab only answers: only your own Hisab data, with sources. No model.", 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 10));
  if (!m) return;
  if (!AiEngine.installed(a)) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, "AI engine needed (about 7 MB)", 15, Ui.ink, true));
   card.addView(Ui.text(a, "A small program that runs the model on this phone (llama.cpp, MIT licence). It is downloaded once from Hisab's GitHub release, checked against its SHA-256, and kept in Hisab's private storage. Nothing about you is sent.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   if (AiEngine.dl) {
    DlBar bar = new DlBar(a); bar.set(AiEngine.done / (float) AiEngine.SIZE, true); card.addView(bar, Ui.lpm(-1, -2, 0, 10, 0, 0));
    TextView st = Ui.text(a, (int) Math.min(100, AiEngine.done * 100 / AiEngine.SIZE) + "%", 12.5f, Ui.ink, true); card.addView(st, Ui.lpm(-1, -2, 0, 6, 0, 0));
    poll(a, bar, st, rebuild);
   } else {
    TextView go = Ui.button(a, "Download engine", true); Ui.tap(go, () -> Sheets.confirm(a, "Download the AI engine?", "About 7 MB from github.com. It runs models on this phone only.", "Download", () -> {
     AiEngine.download(a, () -> { }, err -> a.runOnUiThread(() -> { if (err != null) Toast.makeText(a, err, Toast.LENGTH_LONG).show(); rebuild.run(); })); rebuild.run(); }));
    card.addView(go, Ui.lpm(-1, -2, 0, 10, 0, 0));
    if (AiEngine.dlError != null) card.addView(Ui.text(a, AiEngine.dlError, 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   }
   c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10)); return;
  }
  List<AiModels.M> have = installed(a);
  if (have.isEmpty()) {
   LinearLayout card = Ui.card(a);
   card.addView(Ui.text(a, "No model downloaded yet", 15, Ui.ink, true));
   TextView go = Ui.button(a, "Choose and download a model", true); Ui.tap(go, () -> ModelsPage.open(a)); card.addView(go, Ui.lpm(-1, -2, 0, 10, 0, 0));
   c.addView(card, Ui.lpm(-1, -2, 0, 0, 0, 10)); return;
  }
  AiModels.M cur = current(a);
  c.addView(Ui.text(a, "Model", 11, Ui.muted, true), Ui.lpm(-1, -2, 0, 0, 0, 4));
  HorizontalScrollView hs = new HorizontalScrollView(a); hs.setHorizontalScrollBarEnabled(false); LinearLayout pr = Ui.row(a);
  for (AiModels.M x : have) { TextView p = Ui.pill(a, x.name.replace(" Instruct", ""), x == cur); Ui.tap(p, () -> { if (AiEngine.running) return; a.db.setPref("aiModel", x.id); rebuild.run(); }); pr.addView(p, Ui.lpm(-2, -2, 0, 0, 8, 0)); }
  hs.addView(pr); c.addView(hs, Ui.lpm(-1, -2, 0, 0, 0, 6));
  c.addView(Ui.text(a, "Can do: " + features(cur), 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 10));
 }

 /** What each model is good for, so the screen only offers what suits it. */
 static String features(AiModels.M m) {
  boolean small = m.size < 900_000_000L;
  return "chat" + (small ? " (short answers)" : "") + (small ? "" : ", summarise pasted text") + ". Images: not with this model. Files: " + (small ? "short text only (coming next)" : "text read in the app first (coming next)") + ".";
 }

 static List<AiModels.M> installed(MainActivity a) { List<AiModels.M> l = new ArrayList<>(); for (AiModels.M m : AiModels.ALL) if (AiModels.installed(a, m)) l.add(m); return l; }
 static AiModels.M current(MainActivity a) {
  List<AiModels.M> h = installed(a); if (h.isEmpty()) return null; String id = a.db.pref("aiModel", "");
  for (AiModels.M m : h) if (m.id.equals(id)) return m; return h.get(0);
 }

 static void poll(MainActivity a, DlBar bar, TextView st, Runnable rebuild) {
  H.postDelayed(() -> { if (!AiEngine.dl || !bar.isAttachedToWindow()) return; bar.set(AiEngine.done / (float) AiEngine.SIZE, true); st.setText((int) Math.min(100, AiEngine.done * 100 / AiEngine.SIZE) + "%  " + AiModels.mb(AiEngine.done) + " of " + AiModels.mb(AiEngine.SIZE)); poll(a, bar, st, rebuild); }, 250);
 }

 /** The answer being written right now, with timing. */
 static LinearLayout liveCard(MainActivity a) {
  LinearLayout card = Ui.card(a);
  card.addView(Ui.text(a, "Model answer (on this phone)", 11, Ui.muted, true));
  liveView = Ui.text(a, AiEngine.live.toString(), 14.5f, Ui.ink, false); card.addView(liveView, Ui.lpm(-1, -2, 0, 4, 0, 0));
  liveStat = Ui.text(a, "", 12, Ui.muted, false); card.addView(liveStat, Ui.lpm(-1, -2, 0, 8, 0, 0));
  paint();
  if (AiEngine.running) { TextView stop = Ui.button(a, "Stop", false); Ui.tap(stop, AiEngine::stop); card.addView(stop, Ui.lpm(-1, -2, 0, 8, 0, 0)); }
  return card;
 }
 static void paint() {
  if (liveView == null || liveStat == null) return;
  String t = AiEngine.live.toString(); liveView.setText(t.isEmpty() ? "..." : t);
  String s = AiEngine.running ? AiEngine.stage : "";
  if (AiEngine.tokens > 0) s = (AiEngine.running ? "Answering" : "Done") + "  " + AiEngine.tokens + " pieces  " + String.format(java.util.Locale.US, "%.1f", AiEngine.tps()) + " per second";
  liveStat.setText(s + (AiEngine.running ? "  " + (System.currentTimeMillis() - AiEngine.tStart) / 1000 + " s" : ""));
 }

 /** Sends the typed text to the model. Returns false if it cannot run so the caller can say why. */
 static void send(MainActivity a, LinearLayout c, String text, List<String[]> log, Runnable rebuild) {
  AiModels.M m = current(a);
  if (m == null || !AiEngine.installed(a)) { Toast.makeText(a, "Download the engine and a model first.", Toast.LENGTH_SHORT).show(); return; }
  List<String> roles = new ArrayList<>(), texts = new ArrayList<>(); roles.add("system"); texts.add(SYSTEM);
  int budget = 5000; List<String[]> hist = new ArrayList<>();
  for (int i = log.size() - 2; i >= 0 && hist.size() < 6; i--) {
   String[] e = log.get(i); boolean me = "me".equals(e[0]); boolean mod = e.length > 2 && e[2].startsWith("AI model");
   if (!me && !mod) continue; if (budget - e[1].length() < 0) break; budget -= e[1].length(); hist.add(0, e);
  }
  for (String[] e : hist) { roles.add("me".equals(e[0]) ? "user" : "assistant"); texts.add(e[1]); }
  roles.add("user"); texts.add(text.length() > 6000 ? text.substring(0, 6000) : text);
  AiEngine.ask(a, m, roles.toArray(new String[0]), texts.toArray(new String[0]), 0.7f,
   () -> a.runOnUiThread(ModelChat::paint),
   err -> a.runOnUiThread(() -> {
    String ans = AiEngine.live.toString().trim();
    if (!ans.isEmpty()) log.add(new String[]{"hisab", ans, "AI model, general knowledge, not checked against your data. " + AiEngine.lastNote});
    else if (err != null) log.add(new String[]{"hisab", err, ""});
    else log.add(new String[]{"hisab", "The model gave no answer.", ""});
    if (err != null && !ans.isEmpty()) log.add(new String[]{"hisab", "Stopped: " + err, ""});
    liveView = null; liveStat = null; rebuild.run();
   }));
  rebuild.run();
 }
}
