package in.aicon.pocket;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Original contour detail. No image assets, no costly blur, no animated background. */
final class BalanceSurface extends Drawable {
 private final Paint p=new Paint(3); private final int base,accent; private final boolean dark; private final float density;
 BalanceSurface(int base,int accent,boolean dark,float density){this.base=base;this.accent=accent;this.dark=dark;this.density=density;}
 @Override public void draw(Canvas canvas){
  RectF box=new RectF(getBounds());float r=28*density;Path clip=new Path();clip.addRoundRect(box,r,r,Path.Direction.CW);
  int save=canvas.save();canvas.clipPath(clip);
  p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,new int[]{QuickActivity.mix(base,accent,dark?.1f:.07f),base},null,Shader.TileMode.CLAMP));canvas.drawRect(box,p);p.setShader(null);
  p.setColor((accent&0xffffff)|(dark?0x10000000:0x0c000000));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density);
  for(int i=0;i<5;i++){float radius=(66+i*24)*density;canvas.drawCircle(box.right+22*density,box.top+12*density,radius,p);}
  canvas.restoreToCount(save);p.setColor(QuickActivity.mix(base,accent,dark?.24f:.2f));p.setStrokeWidth(density);box.inset(density/2,density/2);canvas.drawRoundRect(box,r,r,p);
 }
 @Override public void setAlpha(int alpha){} @Override public void setColorFilter(ColorFilter filter){} @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
