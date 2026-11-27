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
 static float scale = 1f;
 static int customHex = 0xFFC7F23A;
 static boolean dark;
 static int bg, surface, surface2, ink, muted, line, accent, onAccent, onAccentSub, onAccentFaint, onAccentTrack, accentOnDark, onAccentOnDark, pos, neg, scrim;
 static Typeface body, bodyBold, display, displayBold;

 static final String[] ACCENT_NAMES = {"Lime", "Sky", "Lilac", "Mint", "Rose", "Sand", "Peach", "Teal", "Periwinkle", "Coral", "Aqua", "Lavender", "Butter", "Sage", "Apricot", "Ice", "Black and White", "Wallpaper"};
 static final int[] ACCENTS = {0xFFC7F23A, 0xFF8ACBFF, 0xFFBDA9FF, 0xFF62E3B8, 0xFFFF93B5, 0xFFF0D17C, 0xFFFFC49B, 0xFF6CD4C5, 0xFFA7B6FF, 0xFFFFACAB, 0xFF83E5EE, 0xFFD6B8FF, 0xFFF8E78F, 0xFFB6D3A0, 0xFFF2BC8D, 0xFFC1E4F9, 0, 0};

 static String initial(String name) {
  String n = name == null ? "" : name.trim();
  return n.isEmpty() ? "?" : n.substring(0, n.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
 }
 static int dp(float v) { return Math.round(v * density); }

 static int accentFor(Context c, String name) {
  for (int i = 0; i < ACCENT_NAMES.length; i++) if (ACCENT_NAMES[i].equals(name)) {
   if (name.equals("Black and White")) return dark ? 0xFFF2F0EA : 0xFF15161A;
   if (ACCENTS[i] != 0) return ACCENTS[i];
   if (Build.VERSION.SDK_INT >= 31) try { return c.getColor(android.R.color.system_accent1_200); } catch (Exception ignored) {}
   return ACCENTS[0];
  }
  return ACCENTS[0];
 }

 static void theme(Context c, boolean isDark, boolean pureBlack, String accentName, String font) { theme(c, isDark, pureBlack, accentName, font, 1f, customHex); }

 static void theme(Context c, boolean isDark, boolean pureBlack, String accentName, String font, float textScale, int customAccent) {
  density = c.getResources().getDisplayMetrics().density;
  scale = textScale <= 0 ? 1f : textScale;
  customHex = customAccent;
  dark = isDark;
  accent = accentName.equals("Custom") ? customAccent : accentFor(c, accentName);
  onAccent = onAccentFor(accent);
  onAccentSub = withAlpha(onAccent, 0xCC); onAccentFaint = withAlpha(onAccent, 0x99); onAccentTrack = withAlpha(onAccent, 0x2E);
  accentOnDark = accentOnDarkFor(accent);
  onAccentOnDark = onAccentFor(accentOnDark);
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
  if(L.isMl()){ body=Typeface.create("sans-serif",Typeface.NORMAL);bodyBold=Typeface.create("sans-serif",Typeface.BOLD);display=body;displayBold=bodyBold; }
 }

 /** Rough relative luminance, 0..1. */
 static float lum(int color) { return (0.299f * ((color >> 16) & 0xFF) + 0.587f * ((color >> 8) & 0xFF) + 0.114f * (color & 0xFF)) / 255f; }
 /** Readable text on top of the accent: dark ink on light accents, off-white on dark ones (Black and White in light mode, dark custom colours). */
 static int onAccentFor(int accentColor) { return lum(accentColor) > 0.55f ? 0xFF14151A : 0xFFF2F0EA; }
 /** Accent elements sitting on always-dark surfaces (the dock, toasts): a dark accent would vanish there, so swap it for off-white. */
 static int accentOnDarkFor(int accentColor) { return lum(accentColor) <= 0.35f ? 0xFFF2F0EA : accentColor; }
 static int withAlpha(int color, int alpha) { return (color & 0xFFFFFF) | (alpha << 24); }

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
  TextView t = new TextView(c); t.setText(L.t(s)); t.setTextSize(sp * scale); t.setTextColor(color);
  t.setTypeface(bold ? bodyBold : body); t.setIncludeFontPadding(L.isMl()); return t;
 }
 static TextView big(Context c, CharSequence s, float sp, int color) {
  TextView t = new TextView(c); t.setText(L.t(s)); t.setTextSize(sp * scale); t.setTextColor(color);
  t.setTypeface(displayBold); t.setIncludeFontPadding(L.isMl()); return t;
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

 /** A switch tinted to the theme accent instead of the platform default teal. */
 static Switch toggle(Context c) {
  Switch s = new Switch(c);
  s.setOnTouchListener((x, e) -> { if (e.getActionMasked() == MotionEvent.ACTION_UP) buzz(x, android.view.HapticFeedbackConstants.CLOCK_TICK); return false; });
  int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{}};
  s.setThumbTintList(new android.content.res.ColorStateList(states, new int[]{accent, dark ? muted : 0xFFFFFFFF}));
  s.setTrackTintList(new android.content.res.ColorStateList(states, new int[]{withAlpha(accent, 0x59), withAlpha(muted, 0x33)}));
  return s;
 }

 static LinearLayout card(Context c) {
  LinearLayout l = col(c); l.setBackground(shape(surface, 24)); l.setPadding(dp(18), dp(16), dp(18), dp(16)); return l;
 }

 static ImageView icon(Context c, String name, float sizeDp, int color) {
  ImageView v = new ImageView(c); v.setImageDrawable(name.equals("google") ? Icons.google() : name.equals("whatsapp") ? Icons.brand(c,name) : new Icons.Draw(name, color)); v.setLayoutParams(lp(dp(sizeDp), dp(sizeDp))); return v;
 }

 /** Circular icon badge. */
 static FrameLayout badge(Context c, String icon, float sizeDp, int fill, int fg) {
  FrameLayout f = new FrameLayout(c); f.setBackground(shape(fill, sizeDp / 2));
  ImageView i = icon(c, icon, sizeDp * 0.5f, fg); FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(sizeDp * 0.5f), dp(sizeDp * 0.5f), Gravity.CENTER); f.addView(i, p);
  f.setLayoutParams(lp(dp(sizeDp), dp(sizeDp))); return f;
 }

 /** Click handling with a small press scale. */
 /** Whether Hisab adds its own touch feedback. Still follows the phone's touch vibration setting. */
 static boolean haptics = true;
 static android.os.Vibrator vib;
 static void initVib(Context c) { try { vib = (android.os.Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE); } catch (Throwable t) { vib = null; } }
 private static void v(View v, long[] pattern, int[] amp, int fallback) {
  if (!haptics) return;
  boolean done = false;
  try {
   if (vib != null && vib.hasVibrator()) {
    android.os.VibrationEffect e = pattern.length == 2 ? android.os.VibrationEffect.createOneShot(pattern[1], android.os.VibrationEffect.DEFAULT_AMPLITUDE) : android.os.VibrationEffect.createWaveform(pattern, -1);
    // Alarm usage so the phone's "touch vibration" switch does not mute it
    vib.vibrate(e, new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
    done = true;
   }
  } catch (Throwable t) { }
  if (!done) { try { if (v != null) v.performHapticFeedback(fallback, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING); } catch (Throwable t) { } }
 }
 static void buzz(View v, int kind) { v(v, new long[]{0, 30}, null, kind); }
 static void buzzTick(View v) { v(v, new long[]{0, 28}, null, android.view.HapticFeedbackConstants.KEYBOARD_TAP); }
 static void buzzOk(View v) { v(v, new long[]{0, 35, 50, 45}, null, android.view.HapticFeedbackConstants.LONG_PRESS); }
 static void buzzNo(View v) { v(v, new long[]{0, 80, 70, 80}, null, android.view.HapticFeedbackConstants.LONG_PRESS); }
 static void tap(View v, Runnable r) {
  v.setClickable(true);
  v.setOnClickListener(x -> r.run());
  v.setOnTouchListener((x, e) -> {
   int a = e.getActionMasked();
   if (a == MotionEvent.ACTION_DOWN) { buzzTick(x); x.animate().scaleX(.965f).scaleY(.965f).setDuration(90).start(); }
   else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) { if (a == MotionEvent.ACTION_UP) Motion.noteTap(x); x.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(new OvershootInterpolator(2f)).start(); }
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
