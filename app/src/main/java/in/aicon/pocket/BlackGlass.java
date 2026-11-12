package in.aicon.pocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Original luminous glass-like surface. Static layered light, not optical refraction.
 * Bounds cache shaders so scrolling does not allocate new paint effects per frame. */
final class BlackGlass extends Drawable {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final float radius,density;private final int tint,base;private final boolean dark;private final RectF r=new RectF(),inner=new RectF();private Shader fill,edge,shine;
 BlackGlass(float radius,float density,int tint){this(radius,density,tint,0xff17251f,true);}
 BlackGlass(float radius,float density,int tint,int base,boolean dark){this.radius=radius;this.density=density;this.tint=tint;this.base=base;this.dark=dark;}
 static int blend(int a,int b,float n){return Color.rgb(Math.round(Color.red(a)*(1-n)+Color.red(b)*n),Math.round(Color.green(a)*(1-n)+Color.green(b)*n),Math.round(Color.blue(a)*(1-n)+Color.blue(b)*n));}
 @Override protected void onBoundsChange(Rect bounds){r.set(bounds);r.inset(density,density);inner.set(r);inner.inset(density*2,density*2);if(r.width()<=0||r.height()<=0)return;fill=new LinearGradient(0,r.top,0,r.bottom,new int[]{blend(base,Color.WHITE,dark?.025f:.5f),blend(base,tint,.035f),blend(base,tint,dark?.025f:.055f)},new float[]{0,.45f,1},Shader.TileMode.CLAMP);edge=new LinearGradient(0,r.top,0,r.bottom,new int[]{dark?0x668fbba4:0xeaffffff,dark?0x183d5d48:0x40aabdad,dark?0x405f8a72:0xcaffffff},new float[]{0,.58f,1},Shader.TileMode.CLAMP);shine=new LinearGradient(0,r.top,0,r.top+r.height()*.45f,new int[]{dark?0x0bffffff:0x88ffffff,0x00ffffff},null,Shader.TileMode.CLAMP);}
 @Override public void draw(Canvas c){if(fill==null)return;p.setStyle(Paint.Style.FILL);p.setShader(fill);c.drawRoundRect(r,radius,radius,p);p.setShader(shine);c.drawRoundRect(inner,Math.max(0,radius-density*2),Math.max(0,radius-density*2),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density*1.1f);p.setShader(edge);c.drawRoundRect(r,radius,radius,p);p.setShader(null);p.setStrokeWidth(density*.55f);p.setColor(dark?0x0cffffff:0x73ffffff);c.drawRoundRect(inner,Math.max(0,radius-density*2),Math.max(0,radius-density*2),p);p.setStyle(Paint.Style.FILL);}
 @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}@Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
