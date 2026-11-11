package in.aicon.pocket;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Original five-letter geometric wordmark, drawn as paths rather than a font asset. */
final class HisabWordmark extends View {
 private final int color;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 HisabWordmark(Context c,int color){super(c);this.color=color;setContentDescription("Hisab");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=Math.min(getWidth()/104f,getHeight()/40f);c.translate(0,(getHeight()-40*scale)/2);c.scale(scale,scale);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.4f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path a=new Path();a.moveTo(3,9);a.lineTo(3,30);a.moveTo(3,22);a.cubicTo(3,12,17,12,17,22);a.lineTo(17,30);a.moveTo(27,17);a.lineTo(27,30);a.moveTo(48,17);a.cubicTo(36,12,31,22,42,24);a.cubicTo(54,25,46,35,35,29);a.moveTo(69,30);a.lineTo(69,17);a.moveTo(69,22);a.cubicTo(68,11,53,14,53,24);a.cubicTo(53,34,67,34,69,24);a.moveTo(79,8);a.lineTo(79,30);a.moveTo(79,22);a.cubicTo(80,12,95,12,95,23);a.cubicTo(95,34,80,34,79,24);c.drawPath(a,p);p.setStyle(Paint.Style.FILL);c.drawCircle(27,9,2.1f,p);c.restore();}
}
