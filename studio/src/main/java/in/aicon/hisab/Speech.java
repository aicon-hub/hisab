package in.aicon.hisab;

import android.speech.tts.TextToSpeech;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Spoken feedback through the phone's own text-to-speech engine. Nothing is sent anywhere by Hisab. */
final class Speech {
 private Speech() {}
 private static TextToSpeech tts; private static boolean ready; private static String pending; private static boolean warned;

 static boolean on(Store db, String key, boolean def) { return db.flag(key, def); }

 /** Amount as it should be spoken: no symbol, no grouping commas, with the currency word. */
 static String money(long minor) {
  String n = Fmt.num(minor).replace(",", "").replace(" ", "");
  String s = Fmt.symbol.trim(); boolean ml = L.isMl();
  String w = s.equals("\u20B9") ? (ml ? "രൂപ" : "rupees") : s.equals("$") ? (ml ? "ഡോളർ" : "dollars") : s.equals("\u20AC") ? "euros" : s.equals("\u00A3") ? "pounds" : s;
  return (minor < 0 ? (ml ? "മൈനസ് " : "minus ") : "") + n.replace("-", "") + " " + w;
 }

 /** Speaks the text now. Safe to call from any screen; failures are silent. */
 /** Spoken feedback is switched off in the app (Salman, 27 Nov). Code kept so it can return. */
 static final boolean DISABLED = true;
 static void say(MainActivity a, String text) {
  if (DISABLED || text == null || text.trim().isEmpty()) return;
  sayWithPhone(a, text);
 }
 private static void sayWithPhone(MainActivity a, String text) {
  try {
   if (tts == null) {
    pending = text;
    tts = new TextToSpeech(a.getApplicationContext(), st -> { ready = st == TextToSpeech.SUCCESS; if (ready && pending != null) { String p = pending; pending = null; speak(a, p); } });
    return;
   }
   if (!ready) { pending = text; return; }
   speak(a, text);
  } catch (Exception e) { /* no engine on this phone */ }
 }
 private static void speak(MainActivity a, String text) {
  try {
   int r = tts.setLanguage(L.locale());
   if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
    tts.setLanguage(java.util.Locale.ENGLISH);
    if (L.isMl() && !warned) { warned = true; a.runOnUiThread(() -> a.toast("No Malayalam voice on this phone, speaking English. Install one in the phone's text-to-speech settings.", null)); }
   }
   tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hisab");
  } catch (Exception e) { }
 }

 /** After saving an entry: speaks only when the user switched "Speak after saving" on. */
 static void saved(MainActivity a, String what, long amount, String cat) {
  if (DISABLED || !on(a.db, "speakSave", false)) return;
  say(a, L.isMl() ? money(amount) + " ചേർത്തു" + (cat == null ? "" : ", " + cat) : what + " " + money(amount) + (cat == null ? "" : ", " + cat));
 }
 /** Voice-entry confirmation: on by default, so a spoken entry is confirmed by voice. */
 static void heard(MainActivity a, long amount, String cat) {
  if (!on(a.db, "speakVoice", true)) return;
  say(a, L.isMl() ? money(amount) + " ചേർത്തു" + (cat == null ? "" : ", " + cat) : "Added " + money(amount) + (cat == null ? "" : ", " + cat));
 }

 /** "Read out" summary of the month so far. */
 static String monthLine(Store db, YearMonth ym) {
  long in = 0, out = 0;
  for (Store.Tx t : db.txs(Fmt.from(ym), Fmt.to(ym))) { if (t.kind.equals("income")) in += t.amount; if (t.kind.equals("expense")) out += t.amount; }
  return L.isMl() ? "ഈ മാസം വരവ് " + money(in) + ", ചെലവ് " + money(out) + ", ബാക്കി " + money(in - out) + "."
    : "This month you received " + money(in) + ", spent " + money(out) + ", and kept " + money(in - out) + ".";
 }
 static String todayLine(Store db, LocalDate today) {
  long out = 0; for (Store.Tx t : db.txs(today.toString(), today.toString())) if (t.kind.equals("expense")) out += t.amount;
  return L.isMl() ? "ഇന്ന് ചെലവ് " + money(out) + "." : "Today you spent " + money(out) + ".";
 }
 static String dueLine(List<Store.Rec> due, LocalDate today) {
  if (due.isEmpty()) return L.isMl() ? "ഈ ആഴ്ച ബിൽ ഒന്നും വരാനില്ല." : "Nothing is due this week.";
  StringBuilder b = new StringBuilder(L.isMl() ? "വരാനിരിക്കുന്നത്: " : "Coming up: ");
  for (int i = 0; i < due.size() && i < 5; i++) {
   Store.Rec r = due.get(i); long days = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(r.next));
   String when = L.isMl() ? (days < 0 ? "വൈകി" : days == 0 ? "ഇന്ന്" : days == 1 ? "നാളെ" : days + " ദിവസത്തിൽ") : (days < 0 ? "overdue" : days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days");
   b.append(r.title).append(' ').append(money(r.amount)).append(' ').append(when).append(". ");
  }
  return b.toString().trim();
 }
 static String tripLine(String name, long total, long count) {
  return L.isMl() ? name + " യാത്രയിൽ " + count + " ചെലവുകൾ, ആകെ " + money(total) + "." : "Trip " + name + ": " + count + (count == 1 ? " expense" : " expenses") + ", " + money(total) + " in total.";
 }

 /** Small "Read out" button in the app's usual style. */
 static android.widget.TextView button(MainActivity a, String label, Runnable what) {
  android.widget.TextView t = Ui.button(a, label, false); t.setContentDescription(label); Ui.tap(t, what); if (DISABLED) t.setVisibility(android.view.View.GONE); return t;
 }


 /** Settings page. */
 static void settings(MainActivity a) {
  Kit.page(a, "Spoken feedback", null, null, c -> {
   c.addView(Ui.text(a, "Hisab can speak amounts and summaries using your phone's own text-to-speech voice. Nothing is sent anywhere. For Malayalam, install a Malayalam voice in the phone's text-to-speech settings; otherwise English is used.", 14, Ui.muted, false), Ui.lpm(-1, -2, 0, 0, 0, 12));
   Privacy.toggle(a, c, "Confirm voice entries by voice", "speakVoice", true, () -> {});
   Privacy.toggle(a, c, "Speak after saving an entry", "speakSave", false, () -> {});
   c.addView(button(a, "Test the voice", () -> say(a, L.isMl() ? "ഹിസാബ് സംസാരിക്കുന്നു. " + money(125000) : "Hisab is speaking. " + money(125000))), Ui.lpm(-1, -2, 0, 16, 0, 0));
   c.addView(Ui.text(a, "Read-out buttons: Home (Read out today), Insights (Read out this month), Coming up (Read out), and each trip card.", 12.5f, Ui.muted, false), Ui.lpm(-1, -2, 0, 12, 0, 0));
  });
 }
}
