package in.aicon.hisab;

import android.view.View;
import android.widget.*;

/** Hisab AI models: pick, download, delete. Models are stored only for now; Hisab answers with its own rules. */
final class ModelsPage {
 private ModelsPage() {}
 static AiModels.Phone phone;
 static LinearLayout col;
 private static final android.os.Handler H = new android.os.Handler(android.os.Looper.getMainLooper());

 static void open(MainActivity a) { Kit.page(a, "Hisab AI models", null, null, c -> { col = c; build(a, c); }); }

 static void build(MainActivity a, LinearLayout c) {
  c.removeAllViews();
  LinearLayout intro = Ui.card(a);
  intro.addView(Ui.text(a, "Optional. Nothing is downloaded unless you tap Download.", 15, Ui.ink, true));
  intro.addView(Ui.text(a, "Models come straight from huggingface.co to this phone, are checked against their published SHA-256, and stay on this phone. Hisab never sends your entries anywhere. Hisab itself stays about 5 MB.", 13, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
  intro.addView(Ui.text(a, "A downloaded model runs in Ask Hisab under \"All-model answers\" (needs the small AI engine, offered there). \"Hisab only answers\" never uses a model and answers only from your own data.", 13, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
  c.addView(intro, Ui.lpm(-1, -2, 0, 0, 0, 10));

  LinearLayout me = Ui.card(a);
  if (phone == null) {
   me.addView(Ui.text(a, "Check my phone", 15, Ui.ink, true));
   me.addView(Ui.text(a, "Tap Allow and Hisab reads your phone's memory, processor, graphics chip and free storage to mark each model. It reads only what Android shows any app, on this phone only, and sends nothing.", 13, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   TextView allow = Ui.button(a, "Allow and check", true); Ui.tap(allow, () -> {
    allow.setText("Checking..."); new Thread(() -> { AiModels.Phone p = AiModels.check(a); a.runOnUiThread(() -> { phone = p; build(a, c); }); }).start();
   });
   me.addView(allow, Ui.lpm(-1, -2, 0, 10, 0, 0));
  } else {
   AiModels.Phone p = phone;
   me.addView(Ui.text(a, "Your phone: " + p.ramGb + " GB memory", 15, Ui.ink, true));
   me.addView(Ui.text(a, "Memory: about " + p.ramGb + " GB (Android reports " + String.format(java.util.Locale.US, "%.1f", p.totalRam / 1073741824.0) + " GiB)\nProcessor: " + p.cores + " cores, " + (p.abi.isEmpty() ? "unknown" : p.abi) + (p.soc.isEmpty() ? "" : ", " + p.soc) + "\nGraphics: " + p.gpu + "\nNPU: " + p.npu + "\nFree storage: " + AiModels.mb(p.freeBytes), 13, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   me.addView(Ui.text(a, "Marks are a rule of thumb from memory, cores and storage. They are not a speed test. The NPU cannot be measured by an app.", 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
   TextView again = Ui.button(a, "Check again", false); Ui.tap(again, () -> { phone = null; build(a, c); }); me.addView(again, Ui.lpm(-1, -2, 0, 8, 0, 0));
  }
  c.addView(me, Ui.lpm(-1, -2, 0, 0, 0, 14));

  for (int tier : AiModels.TIERS) {
   boolean any = false;
   for (AiModels.M m : AiModels.ALL) if (m.minRam == tier) { if (!any) { c.addView(Ui.label(a, "For phones with " + tier + " GB memory or more"), Ui.lpm(-1, -2, 0, 4, 0, 6)); any = true; } c.addView(card(a, c, m), Ui.lpm(-1, -2, 0, 0, 0, 8)); }
  }
  LinearLayout t = Ui.card(a);
  t.addView(Ui.text(a, "Which models for how much memory", 15, Ui.ink, true));
  StringBuilder sb = new StringBuilder();
  for (int tier : AiModels.TIERS) { sb.append(tier).append(" GB: "); boolean f = true; for (AiModels.M m : AiModels.ALL) if (m.minRam == tier) { sb.append(f ? "" : ", ").append(m.name.replace(" Instruct", "")); f = false; } sb.append("\n"); }
  sb.append("Engine: llama.cpp (MIT licence, ggml authors). ");
  sb.append("A phone can also download the models of the smaller tiers. A 4 GB phone: the first tier only. Below 4 GB, nothing is advised.");
  t.addView(Ui.text(a, sb.toString(), 13, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
  c.addView(t, Ui.lpm(-1, -2, 0, 4, 0, 8));
  tick(a, c);
 }

 static LinearLayout card(MainActivity a, LinearLayout c, AiModels.M m) {
  LinearLayout card = Ui.card(a);
  card.addView(Ui.text(a, m.name, 15, Ui.ink, true));
  card.addView(Ui.text(a, AiModels.mb(m.size) + " download, needs " + m.minRam + " GB memory. Q4 quantised.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 2, 0, 0));
  card.addView(Ui.text(a, m.note, 13, Ui.ink, false), Ui.lpm(-1, -2, 0, 4, 0, 0));
  if (phone != null) {
   int r = AiModels.rate(phone, m);
   TextView chip = Ui.text(a, (r == AiModels.OK ? "Suitable" : r == AiModels.BORDER ? "Borderline" : "Not suitable") + ": " + AiModels.why(phone, m), 12.5f, r == AiModels.OK ? Ui.ink : Ui.muted, true);
   chip.setBackground(Ui.outline(r == AiModels.OK ? Ui.accent : Ui.surface2, Ui.line, 14, 1)); if (r == AiModels.OK) chip.setTextColor(Ui.onAccent);
   chip.setPadding(Ui.dp(10), Ui.dp(5), Ui.dp(10), Ui.dp(5)); card.addView(chip, Ui.lpm(-2, -2, 0, 8, 0, 0));
  }
  card.addView(Ui.text(a, "Model page and licence: " + m.page(), 11.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
  card.setTag(m.id);
  LinearLayout body = Ui.col(a); body.setTag("body"); card.addView(body, Ui.lpm(-1, -2, 0, 8, 0, 0));
  fill(a, c, m, body);
  return card;
 }

 static void fill(MainActivity a, LinearLayout c, AiModels.M m, LinearLayout body) {
  body.removeAllViews(); boolean act = AiModels.active == m;
  if (AiModels.installed(a, m)) {
   body.addView(Ui.text(a, "Downloaded and checked. Stored on this phone, not used yet.", 13, Ui.ink, true));
   TextView del = Ui.button(a, "Delete from this phone", false); Ui.tap(del, () -> Sheets.confirm(a, "Delete " + m.name + "?", "The file is removed from this phone and " + AiModels.mb(m.size) + " is freed. You can download it again later.", "Delete", () -> { AiModels.delete(a, m); build(a, c); }));
   body.addView(del, Ui.lpm(-1, -2, 0, 8, 0, 0)); return;
  }
  long have = act ? AiModels.done : AiModels.partial(a, m);
  if (act || have > 0) {
   DlBar bar = new DlBar(a); bar.setTag("bar"); body.addView(bar, new LinearLayout.LayoutParams(-1, -2));
   TextView st = Ui.text(a, "", 12.5f, Ui.ink, true); st.setTag("st"); body.addView(st, Ui.lpm(-1, -2, 0, 6, 0, 0));
   bar.set(have / (float) m.size, act); st.setText(status(m, have, act));
  }
  if (act) {
   TextView p = Ui.button(a, AiModels.phase.equals("Checking") ? "Checking file..." : "Pause", false);
   if (!AiModels.phase.equals("Checking")) Ui.tap(p, () -> { AiModels.pause(); p.setText("Pausing..."); });
   body.addView(p, Ui.lpm(-1, -2, 0, 8, 0, 0));
  } else {
   TextView go = Ui.button(a, have > 0 ? "Resume download" : "Download " + AiModels.mb(m.size), true);
   Ui.tap(go, () -> {
    if (AiModels.busy()) { Toast.makeText(a, "One download at a time.", Toast.LENGTH_SHORT).show(); return; }
    Sheets.confirm(a, "Download " + m.name + "?", AiModels.mb(m.size - have) + " will be downloaded from huggingface.co. This uses your data plan if you are not on Wi-Fi. Models are not used yet.", "Download", () -> {
     AiModels.start(a, m, () -> { }, err -> a.runOnUiThread(() -> { if (err != null && !"paused".equals(err)) Toast.makeText(a, err, Toast.LENGTH_LONG).show(); if (col != null) build(a, col); }));
     build(a, c);
    });
   });
   body.addView(go, Ui.lpm(-1, -2, 0, 8, 0, 0));
   if (AiModels.error != null && have > 0) body.addView(Ui.text(a, AiModels.error, 12, Ui.muted, false), Ui.lpm(-1, -2, 0, 6, 0, 0));
  }
 }

 static String status(AiModels.M m, long d, boolean act) {
  int pct = (int) Math.min(100, d * 100 / m.size);
  String s = pct + "%  " + AiModels.mb(d) + " of " + AiModels.mb(m.size);
  if (act && AiModels.phase.equals("Checking")) return "100%  checking the file against its checksum";
  if (act) { long sp = AiModels.speed(); if (sp > 0) s += "  " + AiModels.mb(sp) + "/s"; return s; }
  return s + "  paused";
 }

 /** Refreshes the live bars while a download runs. Stops when the page is gone or nothing runs. */
 static void tick(MainActivity a, LinearLayout c) {
  if (AiModels.active == null) return;
  H.postDelayed(() -> {
   if (col != c || !c.isAttachedToWindow()) return;
   AiModels.M m = AiModels.active; if (m == null) return;
   View card = c.findViewWithTag(m.id);
   if (card != null) {
    DlBar bar = card.findViewWithTag("bar"); TextView st = card.findViewWithTag("st");
    if (bar != null) bar.set(AiModels.done / (float) m.size, !AiModels.phase.equals("Checking")); if (st != null) st.setText(status(m, AiModels.done, true));
   }
   tick(a, c);
  }, 250);
 }
}
