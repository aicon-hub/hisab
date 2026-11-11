package in.aicon.pocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Original static glass treatment: black base, a restrained top reflection and
 * fine edge. No full-screen blur, animation or claimed power saving benchmark. */
final class BlackGlass extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final float radius,density;private final int tint;
 BlackGlass(float radius,float density,int tint){this.radius=radius;this.density=density;this.tint=tint;}
 @Override public void draw(Canvas canvas){RectF rect=new RectF(getBounds());float inset=density*.5f;rect.inset(inset,inset);int top=androidx.core.graphics.ColorUtils.blendARGB(0xff181b20,tint,.025f);paint.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{top,0xff08090b,0xff020304},new float[]{0,.48f,1},Shader.TileMode.CLAMP));paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(new LinearGradient(0,rect.top,0,rect.bottom,new int[]{0x55ffffff,0x162b3037,0x242e3540},null,Shader.TileMode.CLAMP));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);}
 @Override public void setAlpha(int alpha){}@Override public void setColorFilter(ColorFilter filter){}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
