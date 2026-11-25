package in.aicon.hisab;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.PathParser;
import java.util.*;

/** Original line icons on a 24 unit grid, drawn from path data. */
final class Icons {
 private Icons() {}
 static final Map<String, String> P = new HashMap<>();
 static final Map<String, Path> CACHE = new HashMap<>();
 private static String circle(double x, double y, double r) { return "M" + (x - r) + " " + y + "a" + r + " " + r + " 0 1 0 " + (2 * r) + " 0a" + r + " " + r + " 0 1 0 " + (-2 * r) + " 0Z "; }
 static {

  P.put("fitness", "M3 9V15 M6 6V18 M18 6V18 M21 9V15 M6 12H18");
  P.put("pet", "M7 17C7 12 10 10 12 12C14 10 17 12 17 17C17 21 14 18 12 19C10 18 7 21 7 17Z M5 7V9 M10 4V6 M15 4V6 M20 7V9");
  P.put("rent", "M4 11L12 4L20 11 M6 10V20H18V10 M10 20V14H14V20 M15 7H18V10");
  P.put("wifi", "M3 8Q12 1 21 8 M6 12Q12 7 18 12 M9 16Q12 13 15 16 M12 20H12.1");
  P.put("subscription", "M5 5H19V19H5Z M10 9L15 12L10 15Z");
  P.put("charity", "M3 14H7L11 17H16L21 12 M7 14V20H16L21 16 M12 11C7 8 9 3 12 6C15 3 17 8 12 11Z");
  P.put("repair", "M15 4L14 8L17 10L21 8C21 12 18 14 15 13L7 21L3 17L11 9C10 5 12 3 15 4Z");
  P.put("beauty", "M5 5L19 19 M5 19L19 5 M8 7C8 11 2 11 2 7C2 3 8 3 8 7 M8 17C8 21 2 21 2 17C2 13 8 13 8 17");
  P.put("work", "M3 8H21V20H3Z M8 8V4H16V8 M3 12L12 15L21 12 M10 15V17H14V15");
  P.put("baby", "M4 9H20L17 17H8Z M4 5H7L9 17 M10 20H10.1 M16 20H16.1 M12 9V3C17 3 20 5 20 9");
  P.put("insurance", "M12 3L20 7V12C20 16 17 19 12 22C7 19 4 16 4 12V7Z M8 12L11 15L16 9");
  P.put("tax", "M5 3H19V21L16 19L12 21L8 19L5 21Z M9 8H9.1 M15 14H15.1 M15 7L9 15");
  P.put("water", "M12 3C10 7 5 12 5 16A7 7 0 0 0 19 16C19 12 14 7 12 3Z M9 16C9 18 10 19 12 19");
  P.put("laundry", "M4 3H20V21H4Z M4 8H20 M7 5H7.1 M10 5H10.1 M17 5H17.1 M12 10A4.5 4.5 0 1 0 12 19A4.5 4.5 0 1 0 12 10 M8 14Q12 17 16 14");
  P.put("electronics", "M4 4H20V16H4Z M9 20H15 M12 16V20 M2 16H22");
  P.put("eye", "M2 12Q12 0 22 12Q12 24 2 12Z M12 9A3 3 0 1 0 12 15A3 3 0 1 0 12 9");
  P.put("mic", "M12 3A3 3 0 0 1 15 6V11A3 3 0 0 1 9 11V6A3 3 0 0 1 12 3Z M6 11A6 6 0 0 0 18 11H16A4 4 0 0 1 8 11Z M11 17H13V21H11Z");
  P.put("sms", "M4 4H20V17H9L4 21Z M8 8H16 M8 12H14");
  P.put("telegram", "M3 11L21 4L17 21L11 16L8 19L9 14L18 7L7 13Z");
  P.put("globe", circle(12,12,9)+"M3 12H21 M12 3C6 8 6 16 12 21C18 16 18 8 12 3Z");
  P.put("lock", "M5 10H19V21H5Z M8 10V7A4 4 0 0 1 16 7V10 M12 14V17");
  P.put("home", "M4 11L12 4L20 11 M6 10V20H18V10 M10 20V14H14V20");
  P.put("plus", "M12 5V19 M5 12H19");
  P.put("ledger", "M9 6H20 M9 12H20 M9 18H20 M4.5 6H4.6 M4.5 12H4.6 M4.5 18H4.6");
  P.put("chart", "M5 20V13 M12 20V5 M19 20V9");
  P.put("people", circle(9, 8, 3) + "M3 20C3 16.7 5.7 14 9 14S15 16.7 15 20 M16 5.5A3 3 0 0 1 16 11 M18 14.3C19.8 15.1 21 16.8 21 19");
  P.put("user", circle(12, 8, 4) + "M4 21C4 17 7.6 14.5 12 14.5S20 17 20 21");
  P.put("settings", "M4 7H12 M17 7H20 M4 17H7 M12 17H20 " + circle(14.5, 7, 2.5) + circle(9.5, 17, 2.5));
  P.put("search", circle(11, 11, 6) + "M16 16L20 20");
  P.put("tag", "M4 4H10L20 14L14 20L4 10Z " + circle(8, 8, 1.2));
  P.put("food", "M5 8H17V14A5 5 0 0 1 12 19H10A5 5 0 0 1 5 14Z M17 10H18.5A2 2 0 0 1 18.5 14H17 M8 3.5V5 M12 3.5V5");
  P.put("groceries", "M3 4H5.2L7.5 15H18L20 8H6 M9.5 19H9.6 M16.5 19H16.6");
  P.put("transport", "M5 5H19V16H5Z M5 11H19 M8 19V16 M16 19V16 M8.5 13.5H8.6 M15.5 13.5H15.6");
  P.put("fuel", "M5 20V5H13V20 M3 20H15 M13 9H16A2 2 0 0 1 18 11V16A1.5 1.5 0 0 0 21 16V9L18 6 M7.5 9H10.5");
  P.put("shopping", "M5 8H19L18 20H6Z M9 8V6A3 3 0 0 1 15 6V8");
  P.put("bills", "M6 3H18V21L15 19L12 21L9 19L6 21Z M9 8H15 M9 12H15");
  P.put("health", "M12 20C12 20 4 15 4 9.5A4 4 0 0 1 12 7.5A4 4 0 0 1 20 9.5C20 15 12 20 12 20Z");
  P.put("fun", "M12 4L14.4 9.2L20 9.9L15.9 13.8L17 19.4L12 16.6L7 19.4L8.1 13.8L4 9.9L9.6 9.2Z");
  P.put("education", "M4 5H10A2 2 0 0 1 12 7V20A2 2 0 0 0 10 18H4Z M20 5H14A2 2 0 0 0 12 7V20A2 2 0 0 1 14 18H20Z");
  P.put("travel", "M21 4L3 11L10 13L12 20Z M10 13L21 4");
  P.put("phone", "M7 3H17V21H7Z M11 18H13");
  P.put("coin", circle(12, 12, 8) + "M9 9H15 M9 12H15 M10 9C14 9 14 14 10 14L14.5 17");
  P.put("gift", "M4 10H20V20H4Z M3 7H21V10H3Z M12 7V20 M12 7C9 7 9 4.5 11 4.5C12.5 4.5 12 7 12 7 M12 7C15 7 15 4.5 13 4.5C11.5 4.5 12 7 12 7");
  P.put("other", "M6 12H6.1 M12 12H12.1 M18 12H18.1");
  P.put("wallet", "M4 7H18A2 2 0 0 1 20 9V18A2 2 0 0 1 18 20H5A1 1 0 0 1 4 19Z M4 7L16 4V7 M16 13.5H16.1");
  P.put("bank", "M3 9L12 4L21 9Z M5 10V17 M10 10V17 M14 10V17 M19 10V17 M3 20H21");
  P.put("card", "M3 6H21V18H3Z M3 10H21 M7 15H10");
  P.put("cash", "M3 7H21V17H3Z " + circle(12, 12, 2.5));
  P.put("target", circle(12, 12, 8) + circle(12, 12, 4) + "M12 12H12.1");
  P.put("repeat", "M17 3L20 6L17 9 M4 11V10A4 4 0 0 1 8 6H20 M7 21L4 18L7 15 M20 13V14A4 4 0 0 1 16 18H4");
  P.put("calendar", "M4 6H20V20H4Z M4 10H20 M8 3V7 M16 3V7");
  P.put("filter", "M4 6H20 M7 12H17 M10 18H14");
  P.put("share", "M12 4V15 M8 8L12 4L16 8 M5 13V20H19V13");
  P.put("check", "M5 12.5L10 17.5L19 7");
  P.put("right", "M9 5L16 12L9 19");
  P.put("left", "M15 5L8 12L15 19");
  P.put("down", "M5 9L12 16L19 9");
  P.put("back", "M19 12H5 M11 6L5 12L11 18");
  P.put("close", "M6 6L18 18 M18 6L6 18");
  P.put("trash", "M5 7H19 M9 7V4H15V7 M7 7L8 20H16L17 7");
  P.put("edit", "M4 20L5 15L16 4L20 8L9 19Z M14 6L18 10");
  P.put("message", "M4 5H20V16H10L5 20V16H4Z");
  P.put("swap", "M4 8H18 M14 4L18 8L14 12 M20 16H6 M10 12L6 16L10 20");
  P.put("up", "M12 19V5 M6 11L12 5L18 11");
  P.put("lent", "M7 17L17 7 M8 7H17V16");
  P.put("borrowed", "M17 7L7 17 M16 17H7V8");
  P.put("bell", "M6 16V11A6 6 0 0 1 18 11V16L20 18H4Z M10 21H14");
  P.put("moon", "M20 14A8 8 0 1 1 10 4A6.5 6.5 0 0 0 20 14Z");
  P.put("info", circle(12, 12, 8) + "M12 11V16 M12 8H12.1");
  P.put("download", "M12 4V15 M8 11L12 15L16 11 M5 19H19");
  P.put("upload", "M12 15V4 M8 8L12 4L16 8 M5 19H19");
  P.put("split", "M12 4V10 M12 10L6 16V20 M12 10L18 16V20");
  P.put("flame", "M12 21C8 21 6 18 6 15C6 11 10 9.5 10 5C14 7 18 10.5 18 15C18 18 16 21 12 21Z");
  P.put("spark", "M12 3V8 M12 16V21 M3 12H8 M16 12H21 M6 6L9 9 M15 15L18 18 M18 6L15 9 M9 15L6 18");
  P.put("printer", "M6 9V3H18V9 M6 18H4A2 2 0 0 1 2 16V11A2 2 0 0 1 4 9H20A2 2 0 0 1 22 11V16A2 2 0 0 1 20 18H18 M6 14H18V21H6Z");
  P.put("laptop", "M5 5H19V15H5Z M2 19H22 M5 15L2 19 M19 15L22 19");
  P.put("pen", "M17 3A2.83 2.83 0 1 1 21 7L7.5 20.5L2 22L3.5 16.5Z");
  P.put("cap", "M22 10L12 5L2 10L12 15L22 10Z M6 12V17C9 19 15 19 18 17V12 M22 10V16");
  P.put("cycle", circle(5.5, 17.5, 3.5) + circle(18.5, 17.5, 3.5) + "M5.5 17.5L9 8H14 M12 6H15L18.5 17.5");
 }
 static final String[] CATEGORY_ICONS = {"food", "groceries", "transport", "fuel", "shopping", "bills", "health", "fun", "education", "travel", "phone", "home", "gift", "coin", "fitness", "pet", "rent", "wifi", "subscription", "charity", "repair", "beauty", "work", "baby", "insurance", "tax", "water", "laundry", "electronics", "printer", "laptop", "pen", "cap", "cycle", "other"};

 /** Google's multicolour G mark (48 unit grid, fill paths from Google's sign-in brand asset). */
 static android.graphics.drawable.Drawable google() {
  return new Drawable() {
   final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); { p.setStyle(Paint.Style.FILL); }
   final Path[] paths = {
    PathParser.createPathFromPathData("M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"),
    PathParser.createPathFromPathData("M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"),
    PathParser.createPathFromPathData("M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"),
    PathParser.createPathFromPathData("M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z")};
   final int[] cols = {0xFFEA4335, 0xFF4285F4, 0xFFFBBC05, 0xFF34A853};
   @Override public void draw(Canvas c) {
    Rect b = getBounds(); if (b.isEmpty()) return;
    float s = Math.min(b.width(), b.height()) / 48f;
    c.save(); c.translate(b.left + (b.width() - 48 * s) / 2f, b.top + (b.height() - 48 * s) / 2f); c.scale(s, s);
    for (int i = 0; i < 4; i++) { p.setColor(cols[i]); c.drawPath(paths[i], p); }
    c.restore();
   }
   @Override public void setAlpha(int a) { p.setAlpha(a); }
   @Override public void setColorFilter(ColorFilter f) {}
   @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
  };
 }
 static Bitmap whatsappBitmap;
 static android.graphics.drawable.Drawable brand(android.content.Context c,String name){
  if(!name.equals("whatsapp"))return new Draw(name,Ui.ink);
  try(java.io.InputStream in=c.getAssets().open("brands/whatsapp.png")){
   if(whatsappBitmap==null){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=32;whatsappBitmap=BitmapFactory.decodeStream(in,null,options);}Bitmap scaled=Bitmap.createScaledBitmap(whatsappBitmap,Ui.dp(22),Ui.dp(22),true);return new android.graphics.drawable.BitmapDrawable(c.getResources(),scaled);
  }catch(Exception e){return new Draw("phone",Ui.ink);}
 }
 static Path path(String name) {
  Path p = CACHE.get(name);
  if (p == null) { String d = P.get(name); if (d == null) d = P.get("other"); p = PathParser.createPathFromPathData(d); CACHE.put(name, p); }
  return p;
 }

 static final class Draw extends Drawable {
  private final String name; private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  Draw(String name, int color) {
   this.name = name; paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
  }
  void tint(int color) { paint.setColor(color); invalidateSelf(); }
  @Override public void draw(Canvas c) {
   Rect b = getBounds(); if (b.isEmpty()) return;
   float s = Math.min(b.width(), b.height()) / 24f;
   c.save(); c.translate(b.left + (b.width() - 24 * s) / 2f, b.top + (b.height() - 24 * s) / 2f); c.scale(s, s);
   paint.setStrokeWidth(1.9f); c.drawPath(path(name), paint); c.restore();
  }
  @Override public int getIntrinsicWidth(){return Ui.dp(20);}
  @Override public int getIntrinsicHeight(){return Ui.dp(20);}
  @Override public void setAlpha(int a) { paint.setAlpha(a); }
  @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
  @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
 }
}
