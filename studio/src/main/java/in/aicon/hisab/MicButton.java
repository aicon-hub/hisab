package in.aicon.hisab;

import android.animation.ValueAnimator;
import android.view.View;
import android.widget.FrameLayout;

/** The voice-entry button: an accent circle with a microphone. A soft ring pulses while the phone is listening. Drawn in code, no images. */
final class MicButton extends FrameLayout {
 private final View ring; private ValueAnimator pulse; boolean listening;

 MicButton(MainActivity a) {
  super(a); int s = Ui.dp(42);
  ring = new View(a); ring.setBackground(Ui.outline(0, Ui.accent, 30, 2.5f)); ring.setAlpha(0f); addView(ring, new FrameLayout.LayoutParams(s, s, android.view.Gravity.CENTER));
  FrameLayout core = Ui.badge(a, "mic2", 42, Ui.accent, Ui.onAccent); core.setElevation(Ui.dp(2)); addView(core, new FrameLayout.LayoutParams(s, s, android.view.Gravity.CENTER));
  setClipChildren(false); setClipToPadding(false); setContentDescription("Say it"); setTag("micButton");
  setOnClickListener(null);
 }

 void setListening(boolean on) {
  listening = on;
  if (pulse != null) { pulse.cancel(); pulse = null; }
  if (!on || !Ui.motion(this)) { ring.setAlpha(0f); ring.setScaleX(1f); ring.setScaleY(1f); return; }
  pulse = ValueAnimator.ofFloat(0f, 1f); pulse.setDuration(1100); pulse.setRepeatCount(ValueAnimator.INFINITE);
  pulse.addUpdateListener(x -> { float f = (float) x.getAnimatedValue(); ring.setScaleX(1f + 0.55f * f); ring.setScaleY(1f + 0.55f * f); ring.setAlpha(0.7f * (1f - f)); });
  pulse.start();
 }

 @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); if (pulse != null) { pulse.cancel(); pulse = null; } }
}
