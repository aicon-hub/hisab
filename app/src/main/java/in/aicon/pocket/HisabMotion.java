package in.aicon.pocket;

import android.animation.ValueAnimator;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/** Bounded motion, with Android's reduced-motion setting respected. No perpetual work. */
final class HisabMotion {
 private HisabMotion() {}
 static boolean enabled(View view) {
  try { return ValueAnimator.areAnimatorsEnabled() && Settings.Global.getFloat(view.getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f)>0f; }
  catch(Exception ignored) { return true; }
 }
 static void enter(ViewGroup group) {
  if(!enabled(group))return;
  int count=Math.min(group.getChildCount(),9);
  float density=group.getResources().getDisplayMetrics().density;
  for(int i=0;i<count;i++) {
   View child=group.getChildAt(i);
   child.setAlpha(0);child.setTranslationY(12*density);
   child.animate().alpha(1).translationY(0).setStartDelay(i*22L).setDuration(240)
     .setInterpolator(new DecelerateInterpolator()).start();
  }
 }
}
