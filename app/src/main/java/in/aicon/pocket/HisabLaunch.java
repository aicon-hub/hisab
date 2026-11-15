package in.aicon.pocket;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.Gravity;

/** Cold-launch logo reveal. Home is already ready underneath, never gated by I/O. */
final class HisabLaunch extends FrameLayout {
 static final long HOLD_MS=640, EXIT_MS=160;
 final LinearLayout mark;
 private final Runnable exit=this::dismiss;
 HisabLaunch(Context context,int background,int foreground,int accent){
  super(context);setBackgroundColor(background);setClickable(true);setFocusable(true);setContentDescription("Hisab");
  float density=getResources().getDisplayMetrics().density;
  mark=new LinearLayout(context);mark.setOrientation(LinearLayout.VERTICAL);mark.setGravity(Gravity.CENTER);
  View logo=new View(context);logo.setBackground(context.getDrawable(in.aicon.pocket.test.R.drawable.foreground));
  mark.addView(logo,new LinearLayout.LayoutParams((int)(112*density),(int)(112*density)));
  HisabWordmark wordmark=new HisabWordmark(context,foreground);LinearLayout.LayoutParams letters=new LinearLayout.LayoutParams((int)(116*density),(int)(49*density));letters.topMargin=(int)(14*density);mark.addView(wordmark,letters);
  LayoutParams centre=new LayoutParams(-2,-2,Gravity.CENTER);centre.bottomMargin=(int)(36*density);addView(mark,centre);
 }
 void start(){
  if(!HisabMotion.enabled(this)){dismiss();return;}
  mark.setAlpha(0);mark.setScaleX(.88f);mark.setScaleY(.88f);mark.setTranslationY(10*getResources().getDisplayMetrics().density);
  mark.animate().alpha(1).scaleX(1).scaleY(1).translationY(0).setDuration(320).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
  postDelayed(exit,HOLD_MS);
 }
 void dismiss(){
  removeCallbacks(exit);
  if(HisabMotion.enabled(this))animate().alpha(0).setDuration(EXIT_MS).withEndAction(this::remove).start();else remove();
 }
 private void remove(){if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(this);}
 @Override protected void onDetachedFromWindow(){removeCallbacks(exit);animate().cancel();mark.animate().cancel();super.onDetachedFromWindow();}
}
