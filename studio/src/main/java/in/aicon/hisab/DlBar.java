package in.aicon.hisab;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Download progress bar: fills to 100% with a light stripe that moves while the download runs. */
final class DlBar extends View {
 private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
 float frac; boolean live;
 DlBar(Context c) { super(c); }
 void set(float f, boolean running) { frac = Math.max(0, Math.min(1, f)); live = running; invalidate(); }
 @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(14)); }
 @Override protected void onDraw(Canvas c) {
  int W = getWidth(), H = getHeight(); RectF all = new RectF(0, 0, W, H);
  p.setStyle(Paint.Style.FILL); p.setColor(Ui.surface2); c.drawRoundRect(all, H / 2f, H / 2f, p);
  float w = Math.max(H, W * frac); if (frac <= 0) return;
  RectF fill = new RectF(0, 0, w, H); p.setColor(Ui.accent); c.drawRoundRect(fill, H / 2f, H / 2f, p);
  if (live) {
   c.save(); Path clip = new Path(); clip.addRoundRect(fill, H / 2f, H / 2f, Path.Direction.CW); c.clipPath(clip);
   float ph = (android.os.SystemClock.uptimeMillis() % 1100) / 1100f; float sw = Ui.dp(28); p.setColor(0x55FFFFFF);
   for (float x = -sw * 2 + ph * sw * 2; x < w; x += sw * 2) { Path s = new Path(); s.moveTo(x, H); s.lineTo(x + sw, H); s.lineTo(x + sw + H, 0); s.lineTo(x + H, 0); s.close(); c.drawPath(s, p); }
   c.restore();
   if (isShown() && !android.os.Build.FINGERPRINT.contains("robolectric")) postInvalidateOnAnimation();
  }
 }
}
