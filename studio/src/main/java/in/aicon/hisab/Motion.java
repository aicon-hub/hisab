package in.aicon.hisab;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

/** Screen and tile motion. Only moves, scales and fades views (cheap on weak phones) and does nothing when the system animations are off. */
final class Motion {
 private Motion() {}
 /** Where the last tap landed, in window pixels, so the next screen can grow out of that tile. */
 static float tx, ty, tw, th; static long tapAt;

 static void noteTap(View v) {
  int[] p = new int[2]; v.getLocationInWindow(p); tx = p[0]; ty = p[1]; tw = v.getWidth(); th = v.getHeight(); tapAt = android.os.SystemClock.uptimeMillis();
 }
 /** True when a tile was tapped just before this screen opened, so it can morph from that tile. */
 static boolean fresh() { return tw > 0 && th > 0 && android.os.SystemClock.uptimeMillis() - tapAt < 700 && tw < Ui.dp(2000); }

 static final class Round extends ViewOutlineProvider { float r; @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); } }

 /** Grows the full-screen panel out of the tapped tile: position, size, corner radius and fade move together. Returns the tile rect so closing can shrink back into it. */
 static float[] morphIn(View panel, int W, int H, Runnable done) {
  float[] r = {tx, ty, tw, th}; tapAt = 0;
  float sx = Math.max(.1f, r[2] / W), sy = Math.max(.06f, r[3] / H);
  final Round o = new Round(); o.r = Ui.dp(24); panel.setOutlineProvider(o); panel.setClipToOutline(true);
  panel.setPivotX(W / 2f); panel.setPivotY(H / 2f);
  panel.setScaleX(sx); panel.setScaleY(sy); panel.setTranslationX(r[0] + r[2] / 2 - W / 2f); panel.setTranslationY(r[1] + r[3] / 2 - H / 2f); panel.setAlpha(0f);
  panel.animate().scaleX(1f).scaleY(1f).translationX(0).translationY(0).setDuration(320).setInterpolator(new DecelerateInterpolator(2.2f)).setUpdateListener(a -> {
   float f = a.getAnimatedFraction(); o.r = Ui.dp(24) * (1f - f); panel.setAlpha(Math.min(1f, f * 3.2f)); panel.invalidateOutline();
  }).withEndAction(() -> { panel.setClipToOutline(false); panel.setOutlineProvider(ViewOutlineProvider.BACKGROUND); if (done != null) done.run(); }).start();
  return r;
 }

 static void morphOut(View panel, int W, int H, float[] r, Runnable end) {
  final Round o = new Round(); o.r = 0; panel.setOutlineProvider(o); panel.setClipToOutline(true);
  panel.animate().scaleX(Math.max(.1f, r[2] / W)).scaleY(Math.max(.06f, r[3] / H)).translationX(r[0] + r[2] / 2 - W / 2f).translationY(r[1] + r[3] / 2 - H / 2f).setDuration(240).setInterpolator(new AccelerateInterpolator(1.6f)).setUpdateListener(a -> {
   float f = a.getAnimatedFraction(); o.r = Ui.dp(24) * f; panel.setAlpha(1f - Math.max(0f, f - .55f) / .45f); panel.invalidateOutline();
  }).withEndAction(end).start();
 }

 /** Tiles and rows of a screen rise and fade in one after another. Capped so long lists do not stall a weak phone. */
 static void stagger(View screen, long delay) {
  if (!Ui.motion(screen)) return;
  ViewGroup col = column(screen, 0); if (col == null) return;
  int n = Math.min(col.getChildCount(), 9);
  for (int i = 0; i < n; i++) {
   View c = col.getChildAt(i); c.setAlpha(0f); c.setTranslationY(Ui.dp(16));
   c.animate().alpha(1f).translationY(0).setStartDelay(delay + i * 32L).setDuration(260).setInterpolator(new DecelerateInterpolator(2f)).start();
  }
 }

 private static ViewGroup column(View v, int depth) {
  if (!(v instanceof ViewGroup) || depth > 4) return null; ViewGroup g = (ViewGroup) v;
  if (g instanceof android.widget.LinearLayout && ((android.widget.LinearLayout) g).getOrientation() == android.widget.LinearLayout.VERTICAL && g.getChildCount() >= 3) return g;
  for (int i = 0; i < g.getChildCount() && i < 3; i++) { ViewGroup r = column(g.getChildAt(i), depth + 1); if (r != null) return r; }
  return null;
 }
}
