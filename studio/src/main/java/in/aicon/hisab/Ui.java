package in.aicon.hisab;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.view.*;
import android.view.animation.*;
import android.widget.*;

/** Theme colours, fonts and small view helpers. Everything is drawn in code. */
final class Ui {
 private Ui() {}
 static float density = 3f;
 static boolean dark;
 static int bg, surface, surface2, ink, muted, line, accent, onAccent, pos, neg, scrim;
 static Typeface body, bodyBold, display, displayBold;

 static final String[] ACCENT_NAMES = {"Lime", "Sky", "Lilac", "Mint", "Rose", "Sand", "Peach", "Teal", "Periwinkle", "Coral", "Aqua", "Lavender", "Butter", "Sage", "Apricot", "Ice", "Wallpaper"};
 static final int[] ACCENTS = {0xFFC7F23A, 0xFF8ACBFF, 0xFFBDA9FF, 0xFF62E3B8, 0xFFFF93B5, 0xFFF0D17C, 0xFFFFC49B, 0xFF6CD4C5, 0xFFA7B6FF, 0xFFFFACAB, 0xFF83E5EE, 0xFFD6B8FF, 0xFFF8E78F, 0xFFB6D3A0, 0xFFF2BC8D, 0xFFC1E4F9, 0};

 static String initial(String name) {
  String n = name == null ? "" : name.trim();
  return n.isEmpty() ? "?" : n.substring(0, n.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
 }
 static int dp(float v) { return Math.round(v * density); }

 static int accentFor(Context c, String name) {
  for (int i = 0; i < ACCENT_NAMES.length; i++) if (ACCENT_NAMES[i].equals(name)) {
   if (ACCENTS[i] != 0) return ACCENTS[i];
   if (Build.VERSION.SDK_INT >= 31) try { return c.getColor(android.R.color.system_accent1_200); } catch (Exception ignored) {}
   return ACCENTS[0];
  }
  return ACCENTS[0];
 }

 static void theme(Context c, boolean isDark, boolean pureBlack, String accentName, String font) {
  density = c.getResources().getDisplayMetrics().density;
  dark = isDark;
  accent = accentFor(c, accentName);
  onAccent = 0xFF14151A;
  if (isDark) {
   bg = pureBlack ? 0xFF000000 : 0xFF0D0E11; surface = pureBlack ? 0xFF111214 : 0xFF17181D; surface2 = pureBlack ? 0xFF1B1C20 : 0xFF202229;
   ink = 0xFFF2F0EA; muted = 0xFF8F919A; line = 0xFF282B33; pos = 0xFF4FDDA0; neg = 0xFFFF7B83; scrim = 0xB0000000;
  } else {
   bg = 0xFFF4F0E8; surface = 0xFFFFFFFF; surface2 = 0xFFECE7DC;
   ink = 0xFF15161A; muted = 0xFF77736A; line = 0xFFE3DDCF; pos = 0xFF168A5C; neg = 0xFFD93C45; scrim = 0x8C101114;
  }
  Typeface inter = load(c, "inter", 400), interB = load(c, "inter", 650), lora = load(c, "lora", 500), loraB = load(c, "lora", 650), pop = load(c, "poppins", 400), popB = load(c, "poppins", 600);
  Typeface sys = Typeface.create("sans-serif", Typeface.NORMAL), sysB = Typeface.create("sans-serif-medium", Typeface.NORMAL);
  switch (font) {
   case "System": body = sys; bodyBold = sysB; display = sysB; displayBold = sysB; break;
   case "Lora": body = lora; bodyBold = loraB; display = loraB; displayBold = loraB; break;
   case "Poppins": body = pop; bodyBold = popB; display = popB; displayBold = popB; break;
   default: body = inter; bodyBold = interB; display = lora; displayBold = loraB;
  }
 }

 private static Typeface load(Context c, String name, int weight) {
  try {
   Typeface.Builder b = new Typeface.Builder(c.getAssets(), "fonts/" + name + ".ttf");
   b.setWeight(weight);
   if (!name.equals("poppins")) b.setFontVariationSettings("'wght' " + weight);
   Typeface t = b.build();
   if (t != null) return t;
  } catch (Exception ignored) {}
  return Typeface.create("sans-serif", Typeface.NORMAL);
 }

 static GradientDrawable shape(int color, float radiusDp) {
  GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radiusDp)); return g;
 }
 static GradientDrawable outline(int fill, int stroke, float radiusDp, float strokeDp) {
  GradientDrawable g = shape(fill, radiusDp); g.setStroke(Math.max(1, dp(strokeDp)), stroke); return g;
 }

 static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
  TextView t = new TextView(c); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
  t.setTypeface(bold ? bodyBold : body); t.setIncludeFontPadding(false); return t;
 }
 static TextView big(Context c, CharSequence s, float sp, int color) {
  TextView t = new TextView(c); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
  t.setTypeface(displayBold); t.setIncludeFontPadding(false); return t;
 }
 static TextView label(Context c, CharSequence s) {
  TextView t = text(c, s.toString().toUpperCase(java.util.Locale.ROOT), 11, muted, true); t.setLetterSpacing(0.09f); return t;
 }

 static LinearLayout col(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
 static LinearLayout row(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }

 static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
 static LinearLayout.LayoutParams lpm(int w, int h, float l, float t, float r, float b) {
  LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
 }
 static LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, -2, w); }

 static View space(Context c, float h) { View v = new View(c); v.setLayoutParams(lp(1, dp(h))); return v; }
 static View hspace(Context c, float w) { View v = new View(c); v.setLayoutParams(lp(dp(w), 1)); return v; }
 static View divider(Context c) { View v = new View(c); v.setBackgroundColor(line); v.setLayoutParams(lp(-1, Math.max(1, dp(0.6f)))); return v; }

 static LinearLayout card(Context c) {
  LinearLayout l = col(c); l.setBackground(shape(surface, 24)); l.setPadding(dp(18), dp(16), dp(18), dp(16)); return l;
 }

 static ImageView icon(Context c, String name, float sizeDp, int color) {
  ImageView v = new ImageView(c); v.setImageDrawable(name.equals("whatsapp")?Icons.brand(c,name):new Icons.Draw(name, color)); v.setLayoutParams(lp(dp(sizeDp), dp(sizeDp))); return v;
 }

 /** Circular icon badge. */
 static FrameLayout badge(Context c, String icon, float sizeDp, int fill, int fg) {
  FrameLayout f = new FrameLayout(c); f.setBackground(shape(fill, sizeDp / 2));
  ImageView i = icon(c, icon, sizeDp * 0.5f, fg); FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(sizeDp * 0.5f), dp(sizeDp * 0.5f), Gravity.CENTER); f.addView(i, p);
  f.setLayoutParams(lp(dp(sizeDp), dp(sizeDp))); return f;
 }

 /** Click handling with a small press scale. */
 static void tap(View v, Runnable r) {
  v.setClickable(true);
  v.setOnClickListener(x -> r.run());
  v.setOnTouchListener((x, e) -> {
   int a = e.getActionMasked();
   if (a == MotionEvent.ACTION_DOWN) x.animate().scaleX(.965f).scaleY(.965f).setDuration(90).start();
   else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) x.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(new OvershootInterpolator(2f)).start();
   return false;
  });
 }

 static TextView pill(Context c, String s, boolean selected) {
  TextView t = text(c, s, 13, selected ? onAccent : ink, true);
  t.setPadding(dp(14), dp(9), dp(14), dp(9)); t.setGravity(Gravity.CENTER);
  t.setBackground(selected ? shape(accent, 20) : outline(0, line, 20, 1)); return t;
 }

 static TextView button(Context c, String s, boolean primary) {
  TextView t = text(c, s, 15, primary ? (dark ? onAccent : 0xFFFFFFFF) : ink, true);
  t.setGravity(Gravity.CENTER); t.setPadding(dp(20), dp(15), dp(20), dp(15));
  t.setBackground(primary ? shape(dark ? accent : ink, 26) : outline(0, line, 26, 1.2f)); return t;
 }

 static void enter(ViewGroup g) {
  if (!motion(g)) return;
  int n = Math.min(g.getChildCount(), 10);
  for (int i = 0; i < n; i++) {
   View v = g.getChildAt(i); v.setAlpha(0); v.setTranslationY(dp(14));
   v.animate().alpha(1).translationY(0).setStartDelay(i * 28L).setDuration(300).setInterpolator(new DecelerateInterpolator(1.6f)).start();
  }
 }
 static boolean motion(View v) {
  try { return ValueAnimator.areAnimatorsEnabled(); } catch (Throwable t) { return true; }
 }
}
