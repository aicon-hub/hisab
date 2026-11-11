package in.aicon.pocket;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Original 28-mark line set drawn for Hisab. One 24-unit grid, rounded 1.5-unit
 * stroke, inset composition, and small signature corner dots. No stock assets.
 */
public final class CategoryMark extends Drawable {
 private final int kind;private int color;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 public CategoryMark(int kind,int color){this.kind=kind;this.color=color;}
 void line(Canvas c,float x,float y,float a,float b){c.drawLine(x,y,a,b,p);}
 void box(Canvas c,float l,float t,float r,float b){c.drawRoundRect(l,t,r,b,2,2,p);}
 void path(Canvas c,float... xy){Path q=new Path();q.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)q.lineTo(xy[i],xy[i+1]);c.drawPath(q,p);}
 @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(color);p.setStrokeWidth(1.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
  switch(kind){
   case 0:c.drawArc(4,6,20,20,0,180,false,p);line(c,4,13,20,13);path(c,9,4,8,7,10,9);line(c,7,21,17,21);break;
   case 1:path(c,4,9,6,20,18,20,20,9,4,9);path(c,8,10,9,4,12,7,15,4,17,10);line(c,10,13,10,17);line(c,15,13,15,17);break;
   case 2:box(c,5,4,19,18);line(c,5,12,19,12);line(c,8,7,16,7);c.drawCircle(8,19,1.5f,p);c.drawCircle(16,19,1.5f,p);break;
   case 3:box(c,5,7,19,20);path(c,9,7,9,4,15,4,15,7);line(c,9,11,9,16);line(c,15,11,15,16);break;
   case 4:path(c,4,11,12,4,20,11);path(c,6,10,6,20,18,20,18,10);box(c,10,14,14,20);break;
   case 5:box(c,4,6,20,20);path(c,8,6,8,3,16,3,16,6);line(c,8,11,16,11);c.drawCircle(12,16,1.5f,p);break;
   case 6:path(c,14,3,7,13,12,13,10,21,18,10,13,10,14,3);break;
   case 7:box(c,7,3,17,21);line(c,10,6,14,6);line(c,11,18,13,18);c.drawArc(3,6,21,22,205,40,false,p);break;
   case 8:path(c,4,12,8,12,10,7,13,17,16,12,20,12);c.drawArc(3,4,21,22,195,150,false,p);break;
   case 9:c.save();c.rotate(-35,12,12);box(c,7,3,17,21);line(c,7,12,17,12);line(c,10,6,14,6);c.restore();break;
   case 10:path(c,4,5,11,7,11,21,4,19,4,5);path(c,20,5,13,7,13,21,20,19,20,5);line(c,7,10,9,11);line(c,15,11,17,10);break;
   case 11:box(c,5,7,19,21);c.drawArc(8,2,16,12,180,180,false,p);line(c,9,13,15,13);break;
   case 12:path(c,8,4,4,7,6,12,8,10,8,21,16,21,16,10,18,12,20,7,16,4);c.drawArc(8,1,16,9,0,180,false,p);break;
   case 13:box(c,9,8,17,21);path(c,11,8,11,3,15,3,15,8);line(c,6,9,6,19);line(c,3,14,7,14);break;
   case 14:box(c,3,6,21,19);path(c,10,9,15,12.5f,10,16,10,9);line(c,7,3,10,6);line(c,17,3,14,6);break;
   case 15:box(c,5,5,19,19);c.drawArc(2,2,22,22,220,80,false,p);path(c,18,3,20,7,16,7);line(c,9,10,15,10);line(c,9,14,13,14);break;
   case 16:c.drawCircle(8,8,3,p);c.drawCircle(17,9,2.5f,p);c.drawArc(2,11,14,24,180,180,false,p);c.drawArc(12,13,22,24,180,180,false,p);break;
   case 17:box(c,4,10,20,21);box(c,3,7,21,11);line(c,12,7,12,21);c.drawOval(7,3,12,7,p);c.drawOval(12,3,17,7,p);break;
   case 18:path(c,4,17,8,13,12,15,16,11,20,15,13,21,4,17);c.drawCircle(9,6,2,p);c.drawCircle(15,6,2,p);line(c,11,8,13,10);break;
   case 19:c.drawOval(7,12,17,21,p);c.drawOval(3,7,7,12,p);c.drawOval(9,3,13,9,p);c.drawOval(16,5,20,11,p);break;
   case 20:line(c,7,12,17,12);box(c,4,6,7,18);box(c,17,6,20,18);line(c,2,9,2,15);line(c,22,9,22,15);break;
   case 21:path(c,5,4,5,10,10,12,15,19,19,15,12,10,10,5,5,4);c.drawCircle(17,17,1,p);break;
   case 22:box(c,3,8,21,20);path(c,8,8,8,4,16,4,16,8);line(c,3,13,21,13);box(c,10,11,14,15);break;
   case 23:box(c,5,3,19,21);line(c,9,7,15,7);path(c,8,12,11,15,17,10);line(c,9,18,15,18);break;
   case 24:path(c,4,18,6,11,16,3,21,8,11,18,4,21,4,18);line(c,14,5,19,10);line(c,4,21,20,21);break;
   case 25:box(c,4,8,20,21);path(c,3,8,6,3,18,3,21,8);line(c,4,11,20,11);box(c,8,15,12,21);line(c,16,15,17,15);break;
   case 26:box(c,5,4,19,20);path(c,16,9,9,9,9,15,6,12);line(c,9,15,12,12);break;
   case 28:c.drawCircle(12,12,8,p);c.drawArc(4,4,20,20,90,180,false,p);path(c,12,4,12,20);p.setStyle(Paint.Style.FILL);c.drawArc(4,4,20,20,270,180,false,p);p.setStyle(Paint.Style.STROKE);break;
   case 29:line(c,4,6,20,6);line(c,4,12,20,12);line(c,4,18,20,18);c.drawCircle(9,6,2,p);c.drawCircle(16,12,2,p);c.drawCircle(8,18,2,p);break;
   case 30:c.drawCircle(12,12,8,p);c.drawOval(8,4,16,20,p);line(c,4,12,20,12);c.drawArc(4,6,20,14,0,180,false,p);break;
   case 31:path(c,12,3,20,6,19,15,16,19,12,21,8,19,5,15,4,6,12,3);path(c,8,12,11,15,16,9);break;
   case 32:box(c,4,14,20,21);path(c,12,16,12,3,7,8);path(c,12,3,17,8);line(c,7,18,8,18);break;
   case 33:c.drawArc(4,4,20,20,215,310,false,p);path(c,4,4,4,10,10,10);path(c,12,7,12,12,16,14);break;
   case 34:c.drawCircle(12,12,8,p);line(c,12,11,12,17);c.drawCircle(12,7.5f,.5f,p);break;
   case 35:path(c,3,9,12,3,21,9,3,9);line(c,4,21,20,21);line(c,5,18,19,18);line(c,6,11,6,17);line(c,12,11,12,17);line(c,18,11,18,17);break;
   case 36:box(c,4,3,20,21);line(c,9,6,9,18);line(c,15,6,15,18);line(c,9,12,15,12);break;
   case 37:line(c,4,4,4,21);line(c,4,21,21,21);box(c,7,13,10,18);box(c,12,9,15,18);box(c,17,5,20,18);break;
   case 38:path(c,6,7,7,21,17,21,18,7);line(c,4,7,20,7);path(c,8,7,8,3,16,3,16,7);line(c,10,11,10,17);line(c,14,11,14,17);break;
   case 39:path(c,4,3,15,3,20,8,20,21,4,21,4,3);path(c,15,3,15,8,20,8);line(c,7,12,17,12);line(c,7,16,17,16);line(c,11,10,11,18);break;
   default:c.drawCircle(7,7,2.5f,p);c.drawCircle(17,7,2.5f,p);c.drawCircle(7,17,2.5f,p);box(c,14,14,20,20);
  }
  p.setStyle(Paint.Style.FILL);c.drawCircle(21,3,0.9f,p);c.restore();
 }
 @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
