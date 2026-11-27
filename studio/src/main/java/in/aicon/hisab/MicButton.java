package in.aicon.hisab;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/** The voice-entry button on Home. A lit accent disc with a large microphone. While the phone listens, two rings pulse out and the microphone turns into live sound bars. Drawn in code, no images. */
final class MicButton extends FrameLayout {
 private final View ring, ring2, core; private final ImageView glyph; private final LinearLayout bars; private final View[] bar = new View[4];
 private ValueAnimator pulse, eq; boolean listening;

 MicButton(MainActivity a) {
  super(a); int s = Ui.dp(44);
  ring = new View(a); ring.setBackground(Ui.outline(0, Ui.accent, 30, 2f)); ring.setAlpha(0f); addView(ring, new LayoutParams(s, s, Gravity.CENTER));
  ring2 = new View(a); ring2.setBackground(Ui.outline(0, Ui.accent, 30, 1.5f)); ring2.setAlpha(0f); addView(ring2, new LayoutParams(s, s, Gravity.CENTER));
  core = new View(a); core.setTag("micCore");
  GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{lift(Ui.accent, 0.28f), Ui.accent, shade(Ui.accent, 0.82f)});
  g.setShape(GradientDrawable.OVAL); g.setStroke(Ui.dp(1), (0x59 << 24) | 0xFFFFFF); core.setBackground(g); core.setElevation(Ui.dp(4));
  addView(core, new LayoutParams(s, s, Gravity.CENTER));
  glyph = Ui.icon(a, "mic2", 28, Ui.onAccent); glyph.setElevation(Ui.dp(5)); addView(glyph, new LayoutParams(Ui.dp(28), Ui.dp(28), Gravity.CENTER));
  bars = new LinearLayout(a); bars.setOrientation(LinearLayout.HORIZONTAL); bars.setGravity(Gravity.CENTER); bars.setElevation(Ui.dp(5)); bars.setAlpha(0f);
  for (int i = 0; i < bar.length; i++) { bar[i] = new View(a); bar[i].setBackground(Ui.shape(Ui.onAccent, 2)); bars.addView(bar[i], Ui.lpm(Ui.dp(3.5f), Ui.dp(22), i == 0 ? 0 : 3, 0, 0, 0)); }
  addView(bars, new LayoutParams(Ui.dp(26), Ui.dp(26), Gravity.CENTER));
  setClipChildren(false); setClipToPadding(false); setContentDescription("Say it"); setTag("micButton");
 }

 /** One frame of the listening animation; also used to draw a still picture for checks. */
 void frame(float f) {
  float f2 = (f + 0.5f) % 1f; glyph.setAlpha(0f); bars.setAlpha(1f);
  ring.setScaleX(1f + 0.42f * f); ring.setScaleY(1f + 0.42f * f); ring.setAlpha(0.75f * (1f - f));
  ring2.setScaleX(1f + 0.42f * f2); ring2.setScaleY(1f + 0.42f * f2); ring2.setAlpha(0.6f * (1f - f2));
  float[] k = {0.35f, 0.9f, 0.55f, 0.8f}; for (int i = 0; i < bar.length; i++) bar[i].setScaleY(0.25f + 0.75f * (i % 2 == 0 ? 0.6f : 0.4f) * k[i] + 0.15f);
 }

 static int lift(int c, float f) { int r = (c >> 16) & 255, gr = (c >> 8) & 255, b = c & 255; return 0xFF000000 | ((r + (int) ((255 - r) * f)) << 16) | ((gr + (int) ((255 - gr) * f)) << 8) | (b + (int) ((255 - b) * f)); }
 static int shade(int c, float f) { int r = (c >> 16) & 255, gr = (c >> 8) & 255, b = c & 255; return 0xFF000000 | ((int) (r * f) << 16) | ((int) (gr * f) << 8) | (int) (b * f); }

 void setListening(boolean on) {
  listening = on;
  if (pulse != null) { pulse.cancel(); pulse = null; } if (eq != null) { eq.cancel(); eq = null; }
  boolean motion = Ui.motion(this);
  glyph.setAlpha(on && motion ? 0f : 1f); bars.setAlpha(on && motion ? 1f : 0f);
  if (!on || !motion) { ring.setAlpha(0f); ring2.setAlpha(0f); for (View b : bar) b.setScaleY(1f); return; }
  pulse = ValueAnimator.ofFloat(0f, 1f); pulse.setDuration(1400); pulse.setRepeatCount(ValueAnimator.INFINITE);
  pulse.addUpdateListener(x -> frame((float) x.getAnimatedValue()));
  pulse.start();
  eq = ValueAnimator.ofFloat(0f, 1f); eq.setDuration(700); eq.setRepeatCount(ValueAnimator.INFINITE); eq.setRepeatMode(ValueAnimator.REVERSE);
  eq.addUpdateListener(x -> { float f = (float) x.getAnimatedValue(); float[] k = {0.35f, 0.9f, 0.55f, 0.8f}; for (int i = 0; i < bar.length; i++) bar[i].setScaleY(0.25f + 0.75f * (i % 2 == 0 ? f : 1f - f) * k[i] + 0.15f); });
  eq.start();
 }

 /** The rings grow past the button, so no parent may clip them to a square. */
 @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); android.view.ViewParent p = getParent(); for (int i = 0; i < 8 && p instanceof android.view.ViewGroup; i++) { android.view.ViewGroup g = (android.view.ViewGroup) p; g.setClipChildren(false); g.setClipToPadding(false); p = g.getParent(); } }

 @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); if (pulse != null) { pulse.cancel(); pulse = null; } if (eq != null) { eq.cancel(); eq = null; } }
}
