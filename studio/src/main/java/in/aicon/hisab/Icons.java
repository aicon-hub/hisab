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
  P.put("home", "M4 11L12 4L20 11 M6 10V20H18V10 M10 20V14H14V20");
  P.put("plus", "M12 5V19 M5 12H19");
  P.put("ledger", "M9 6H20 M9 12H20 M9 18H20 M4.5 6H4.6 M4.5 12H4.6 M4.5 18H4.6");
  P.put("chart", "M5 20V13 M12 20V5 M19 20V9");
  P.put("people", circle(9, 8, 3) + "M3 20C3 16.7 5.7 14 9 14S15 16.7 15 20 M16 5.5A3 3 0 0 1 16 11 M18 14.3C19.8 15.1 21 16.8 21 19");
  P.put("user", circle(12, 8, 4) + "M4 21C4 17 7.6 14.5 12 14.5S20 17 20 21");
  P.put("settings", "M4 7H12 M17 7H20 M4 17H7 M12 17H20 " + circle(14.5, 7, 2.5) + circle(9.5, 17, 2.5));
  P.put("search", circle(11, 11, 6) + "M16 16L20 20");
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
 }
 static final String[] CATEGORY_ICONS = {"food", "groceries", "transport", "fuel", "shopping", "bills", "health", "fun", "education", "travel", "phone", "home", "gift", "coin", "other"};

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
  @Override public void setAlpha(int a) { paint.setAlpha(a); }
  @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
  @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
 }
}
