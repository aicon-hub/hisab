package in.aicon.hisab;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Home entry to Ask Hisab. A slow shimmer and a pulsing spark show that it is there. Stops when off screen. */
final class AiPill extends View {
 private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final String label, sub;
 AiPill(Context c, String label, String sub) {
  super(c); this.label = label; this.sub = sub; setContentDescription(label + ". " + sub); setClickable(true);
  tp.setTypeface(Ui.bodyBold); tp.setTextSize(Ui.dp(15));
 }
 @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(52)); }
 @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); if (!android.os.Build.FINGERPRINT.contains("robolectric")) postInvalidateOnAnimation(); }
 @Override protected void onDraw(Canvas c) {
  int W = getWidth(), H = getHeight(); RectF r = new RectF(0, 0, W, H); long t = android.os.SystemClock.uptimeMillis();
  p.setStyle(Paint.Style.FILL); p.setColor(Ui.surface); c.drawRoundRect(r, H / 2f, H / 2f, p);
  p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dp(1.2f)); p.setColor(Ui.accent); p.setAlpha(120); c.drawRoundRect(r, H / 2f, H / 2f, p); p.setAlpha(255);
  // shimmer: a soft diagonal band sweeps across every 4 seconds
  float ph = (t % 4000) / 4000f; float x = -W * 0.3f + ph * W * 1.6f;
  c.save(); Path clip = new Path(); clip.addRoundRect(r, H / 2f, H / 2f, Path.Direction.CW); c.clipPath(clip);
  p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(x, 0, x + W * 0.3f, 0, new int[]{0x00FFFFFF & Ui.accent, (0x38 << 24) | (0x00FFFFFF & Ui.accent), 0x00FFFFFF & Ui.accent}, null, Shader.TileMode.CLAMP));
  c.drawRect(r, p); p.setShader(null); c.restore();
  // spark: pulses slowly
  float pulse = 0.75f + 0.25f * (float) Math.sin(t / 450.0); float cx = H / 2f + Ui.dp(4), cy = H / 2f, rr = Ui.dp(9) * pulse;
  p.setColor(Ui.accent); Path star = new Path();
  star.moveTo(cx, cy - rr); star.quadTo(cx, cy, cx + rr, cy); star.quadTo(cx, cy, cx, cy + rr); star.quadTo(cx, cy, cx - rr, cy); star.quadTo(cx, cy, cx, cy - rr);
  c.drawPath(star, p); c.drawCircle(cx + rr * 1.1f, cy - rr * 1.1f, Ui.dp(2) * pulse, p);
  tp.setColor(Ui.ink); tp.setTextSize(Ui.dp(15)); c.drawText(label, H + Ui.dp(2), cy - Ui.dp(1), tp);
  tp.setColor(Ui.muted); tp.setTextSize(Ui.dp(11.5f)); tp.setTypeface(Ui.body); c.drawText(sub, H + Ui.dp(2), cy + Ui.dp(15), tp); tp.setTypeface(Ui.bodyBold);
  if (isAttachedToWindow() && isShown() && !android.os.Build.FINGERPRINT.contains("robolectric")) postInvalidateOnAnimation();
 }
}
