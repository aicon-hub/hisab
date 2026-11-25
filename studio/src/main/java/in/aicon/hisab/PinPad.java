package in.aicon.hisab;

import android.animation.ObjectAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Predicate;

/** Hisab's own number pad for PINs. No system keyboard, so no third-party keyboard app ever sees the digits. */
final class PinPad {
 static final int MAX = 8, MIN = 4;
 final StringBuilder buf = new StringBuilder();
 final LinearLayout view;
 private final LinearLayout dots;
 private final TextView msg;
 private final MainActivity a;
 private final Predicate<String> submit;
 private final boolean auto;

 /** submit returns true when the PIN was accepted. auto: try it after every digit from 4 on. */
 PinPad(MainActivity a, Predicate<String> submit, boolean auto) {
  this.a = a; this.submit = submit; this.auto = auto;
  view = Ui.col(a); view.setGravity(Gravity.CENTER_HORIZONTAL); view.setTag("pinPad");
  dots = Ui.row(a); dots.setGravity(Gravity.CENTER); dots.setTag("pinDots");
  view.addView(dots, Ui.lpm(-2, Ui.dp(30), 0, 6, 0, 0));
  msg = Ui.text(a, " ", 13, Ui.neg, true); msg.setGravity(Gravity.CENTER); msg.setPadding(0, Ui.dp(8), 0, Ui.dp(10)); msg.setTag("pinMsg"); view.addView(msg);
  String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"del", "0", "ok"}};
  for (String[] rk : keys) {
   LinearLayout r = Ui.row(a);
   for (String k : rk) {
    View key;
    if (k.equals("del")) { key = Ui.badge(a, "back", 64, Ui.surface2, Ui.ink); key.setContentDescription("Delete digit"); key.setTag("pinKeyDel"); }
    else if (k.equals("ok")) { key = Ui.badge(a, "check", 64, Ui.dark ? Ui.accent : Ui.ink, Ui.dark ? Ui.onAccent : 0xFFFFFFFF); key.setContentDescription("Confirm PIN"); key.setTag("pinKeyOk"); }
    else { TextView t = Ui.big(a, k, 26, Ui.ink); t.setGravity(Gravity.CENTER); t.setBackground(Ui.shape(Ui.surface2, 32)); t.setContentDescription(k); t.setTag("pinKey" + k); key = t; }
    if (k.equals("del") || k.equals("ok")) key.setBackground(Ui.shape(k.equals("ok") ? (Ui.dark ? Ui.accent : Ui.ink) : Ui.surface2, 32));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(72), Ui.dp(64)); lp.setMargins(Ui.dp(8), Ui.dp(6), Ui.dp(8), Ui.dp(6)); r.addView(key, lp);
    Ui.tap(key, () -> press(k));
   }
   view.addView(r);
  }
  render(false);
 }

 void press(String k) {
  msg.setText(" ");
  if (k.equals("del")) { if (buf.length() > 0) buf.setLength(buf.length() - 1); render(false); return; }
  if (k.equals("ok")) { confirm(); return; }
  if (buf.length() >= MAX) return;
  buf.append(k); render(true);
  if (auto && buf.length() >= MIN) {
   if (submit.test(buf.toString())) return;
   if (buf.length() == MAX) wrong("Wrong PIN");
  }
 }

 private void confirm() {
  if (buf.length() < MIN) { wrong("Use 4 to 8 digits"); return; }
  if (!submit.test(buf.toString())) wrong("Wrong PIN");
 }

 void wrong(String m) {
  msg.setText(L.t(m)); buf.setLength(0); render(false);
  Ui.buzzNo(view);
  if (Ui.motion(view)) {
   float d = Ui.dp(10);
   ObjectAnimator.ofFloat(dots, "translationX", 0, -d * 2, d * 2, -d * 1.4f, d * 1.4f, -d * .6f, d * .6f, 0).setDuration(380).start();
  }
 }

 private void render(boolean pop) {
  dots.removeAllViews();
  int n = Math.max(MIN, buf.length());
  for (int i = 0; i < n; i++) {
   View d = new View(a); boolean on = i < buf.length();
   d.setBackground(on ? Ui.shape(Ui.dark ? Ui.accent : Ui.ink, 8) : Ui.outline(0, Ui.muted, 8, 1.5f));
   d.setTag(on ? "pinDotOn" : "pinDotOff");
   LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(16), Ui.dp(16)); lp.setMargins(Ui.dp(7), 0, Ui.dp(7), 0); dots.addView(d, lp);
   if (pop && on && i == buf.length() - 1 && Ui.motion(view)) { d.setScaleX(.3f); d.setScaleY(.3f); d.animate().scaleX(1).scaleY(1).setDuration(220).setInterpolator(new OvershootInterpolator(3f)).start(); }
  }
 }

 /** Full-screen page that asks for one PIN with Hisab's pad, then hands it back. */
 static void ask(MainActivity a, String title, String sub, java.util.function.Consumer<String> got) {
  MainActivity.Layer[] ref = new MainActivity.Layer[1];
  ref[0] = Kit.page(a, title, null, null, c -> {
   c.setGravity(Gravity.CENTER_HORIZONTAL);
   TextView s = Ui.text(a, sub, 14, Ui.muted, false); s.setGravity(Gravity.CENTER); c.addView(s, Ui.lpm(-1, -2, 0, 24, 0, 8));
   PinPad p = new PinPad(a, pin -> { a.close(ref[0]); Ui.buzzOk(a.root); a.root.post(() -> got.accept(pin)); return true; }, false);
   c.addView(p.view, Ui.lpm(-2, -2, 0, 0, 0, 0));
  });
 }
}
