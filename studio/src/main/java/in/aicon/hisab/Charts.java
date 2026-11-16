package in.aicon.hisab;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.*;

/** Hand-drawn charts. Each animates in once, then stays still. */
final class Charts {
 private Charts() {}

 abstract static class Anim extends View {
  float t = 1f;
  Anim(Context c) { super(c); }
  void play() {
   if (!Ui.motion(this)) { t = 1f; return; }
   t = 0f; ValueAnimator a = ValueAnimator.ofFloat(0f, 1f); a.setDuration(700); a.setInterpolator(new DecelerateInterpolator(1.8f));
   a.addUpdateListener(x -> { t = (float) x.getAnimatedValue(); invalidate(); }); a.start();
  }
  @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); if (t >= 1f && !played) { played = true; play(); } }
  boolean played;
 }

 static final class Ring extends Anim {
  final float value; final int color; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final String icon; final int track;
  Ring(Context c, float value, int color, String icon) { super(c); this.value = value; this.color = color; this.icon = icon; track = Ui.surface2; p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); }
  @Override protected void onDraw(Canvas c) {
   float w = Math.min(getWidth(), getHeight()), sw = w * 0.11f, r = (w - sw) / 2f, cx = getWidth() / 2f, cy = getHeight() / 2f;
   p.setStrokeWidth(sw); p.setColor(track); c.drawCircle(cx, cy, r, p);
   p.setColor(color); float sweep = 360f * Math.min(1f, Math.max(value, 0.015f)) * t; c.drawArc(cx - r, cy - r, cx + r, cy + r, -90, sweep, false, p);
   Icons.Draw d = new Icons.Draw(icon, Ui.ink); int h = (int) (w * 0.3f); d.setBounds((int) (cx - h), (int) (cy - h), (int) (cx + h), (int) (cy + h)); d.draw(c);
  }
 }

 static final class Bars extends Anim {
  final long[] v; final String[] labels; final int hi; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG); final boolean values;
  Bars(Context c, long[] v, String[] labels, int hi, boolean values) { super(c); this.v = v; this.labels = labels; this.hi = hi; this.values = values; tp.setTypeface(Ui.bodyBold); tp.setTextAlign(Paint.Align.CENTER); tp.setTextSize(Ui.dp(11) ); }
  @Override protected void onDraw(Canvas c) {
   int n = v.length; if (n == 0) return; long max = 1; for (long x : v) max = Math.max(max, x);
   float lh = Ui.dp(22), top = values ? Ui.dp(18) : 0, h = getHeight() - lh - top, slot = getWidth() / (float) n, bw = Math.min(slot * 0.56f, Ui.dp(30));
   p.setColor(Ui.surface2);
   for (int i = 0; i < n; i++) {
    float cx = slot * i + slot / 2f, bh = Math.max(Ui.dp(6), (v[i] / (float) max) * h) * t;
    boolean on = i == hi; p.setColor(on ? (Ui.dark ? Ui.accent : Ui.ink) : (Ui.dark ? 0xFF2C2F38 : 0xFFE0D9CA));
    RectF r = new RectF(cx - bw / 2, top + h - bh, cx + bw / 2, top + h); c.drawRoundRect(r, bw / 2.6f, bw / 2.6f, p);
    tp.setColor(on ? Ui.ink : Ui.muted); c.drawText(labels[i], cx, getHeight() - Ui.dp(5), tp);
    if (values && v[i] > 0) { tp.setColor(on ? Ui.ink : Ui.muted); tp.setTextSize(Ui.dp(10)); c.drawText(Fmt.compact(v[i]).replace(Fmt.symbol.trim(), ""), cx, top + h - bh - Ui.dp(5), tp); tp.setTextSize(Ui.dp(11)); }
   }
  }
 }

 /** Income and spending side by side per month. */
 static final class Pairs extends Anim {
  final long[] in, out; final String[] labels; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG);
  Pairs(Context c, long[] in, long[] out, String[] labels) { super(c); this.in = in; this.out = out; this.labels = labels; tp.setTypeface(Ui.bodyBold); tp.setTextAlign(Paint.Align.CENTER); tp.setTextSize(Ui.dp(11)); }
  @Override protected void onDraw(Canvas c) {
   int n = in.length; long max = 1; for (int i = 0; i < n; i++) max = Math.max(max, Math.max(in[i], out[i]));
   float lh = Ui.dp(22), h = getHeight() - lh, slot = getWidth() / (float) n, bw = Math.min(slot * 0.28f, Ui.dp(18));
   for (int i = 0; i < n; i++) {
    float cx = slot * i + slot / 2f;
    float h1 = Math.max(Ui.dp(4), in[i] / (float) max * h) * t, h2 = Math.max(Ui.dp(4), out[i] / (float) max * h) * t;
    p.setColor(Ui.dark ? 0xFF3A3E49 : 0xFFD9D2C2); c.drawRoundRect(new RectF(cx - bw - Ui.dp(2), h - h1, cx - Ui.dp(2), h), bw / 3, bw / 3, p);
    p.setColor(Ui.dark ? Ui.accent : Ui.ink); c.drawRoundRect(new RectF(cx + Ui.dp(2), h - h2, cx + bw + Ui.dp(2), h), bw / 3, bw / 3, p);
    tp.setColor(i == n - 1 ? Ui.ink : Ui.muted); c.drawText(labels[i], cx, getHeight() - Ui.dp(5), tp);
   }
  }
 }

 static final class Donut extends Anim {
  final long[] v; final int[] colors; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); String centerTop = "", centerBottom = ""; final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
  Donut(Context c, long[] v, int[] colors, String top, String bottom) { super(c); this.v = v; this.colors = colors; centerTop = top; centerBottom = bottom; p.setStyle(Paint.Style.STROKE); tp.setTextAlign(Paint.Align.CENTER); }
  @Override protected void onDraw(Canvas c) {
   float w = Math.min(getWidth(), getHeight()), sw = w * 0.17f, r = (w - sw) / 2f, cx = getWidth() / 2f, cy = getHeight() / 2f; long total = 0; for (long x : v) total += x;
   p.setStrokeWidth(sw); p.setStrokeCap(Paint.Cap.BUTT); p.setColor(Ui.surface2); c.drawCircle(cx, cy, r, p);
   if (total > 0) { float start = -90, gap = v.length > 1 ? 2.4f : 0, avail = 360f * t;
    for (int i = 0; i < v.length; i++) { float sweep = 360f * v[i] / total; float draw = Math.max(0, sweep - gap); if (start + 90 + 0 > avail) break; p.setColor(colors[i % colors.length]); c.drawArc(cx - r, cy - r, cx + r, cy + r, start + gap / 2, Math.min(draw, avail - (start + 90)), false, p); start += sweep; } }
   tp.setTypeface(Ui.displayBold); tp.setColor(Ui.ink); tp.setTextSize(Ui.dp(22)); c.drawText(centerTop, cx, cy + Ui.dp(2), tp);
   tp.setTypeface(Ui.body); tp.setColor(Ui.muted); tp.setTextSize(Ui.dp(11)); c.drawText(centerBottom, cx, cy + Ui.dp(20), tp);
  }
 }

 /** Month calendar coloured by spending. */
 static final class Heat extends View {
  final java.time.YearMonth month; final Map<Integer, Long> spend; final int selected; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG); final java.util.function.IntConsumer pick;
  Heat(Context c, java.time.YearMonth month, Map<Integer, Long> spend, int selected, java.util.function.IntConsumer pick) { super(c); this.month = month; this.spend = spend; this.selected = selected; this.pick = pick; tp.setTextAlign(Paint.Align.CENTER); tp.setTypeface(Ui.bodyBold); }
  int rows() { int off = month.atDay(1).getDayOfWeek().getValue() - 1; return (off + month.lengthOfMonth() + 6) / 7; }
  @Override protected void onMeasure(int w, int h) { int width = MeasureSpec.getSize(w); setMeasuredDimension(width, (int) (width / 7f * rows())); }
  @Override protected void onDraw(Canvas c) {
   long max = 1; for (long x : spend.values()) max = Math.max(max, x);
   float cell = getWidth() / 7f, pad = Ui.dp(3); int off = month.atDay(1).getDayOfWeek().getValue() - 1; int today = month.equals(java.time.YearMonth.now()) ? java.time.LocalDate.now().getDayOfMonth() : -1;
   tp.setTextSize(Ui.dp(12));
   for (int d = 1; d <= month.lengthOfMonth(); d++) {
    int idx = off + d - 1, col = idx % 7, row = idx / 7; RectF r = new RectF(col * cell + pad, row * cell + pad, (col + 1) * cell - pad, (row + 1) * cell - pad);
    Long s = spend.get(d); float k = s == null ? 0 : Math.min(1f, 0.25f + 0.75f * s / (float) max);
    int base = Ui.surface2; int fill = s == null ? base : blend(base, Ui.accent, k);
    p.setStyle(Paint.Style.FILL); p.setColor(fill); c.drawRoundRect(r, cell * 0.3f, cell * 0.3f, p);
    if (d == selected || d == today) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dp(1.8f)); p.setColor(Ui.ink); if (d == selected) c.drawRoundRect(r, cell * 0.3f, cell * 0.3f, p); else { p.setStrokeWidth(Ui.dp(1.2f)); p.setColor(Ui.muted); c.drawRoundRect(r, cell * 0.3f, cell * 0.3f, p); } }
    tp.setColor(k > 0.55f ? Ui.onAccent : Ui.ink); c.drawText("" + d, r.centerX(), r.centerY() + Ui.dp(4), tp);
   }
  }
  static int blend(int a, int b, float t) { return Color.rgb((int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t), (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t), (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t)); }
  @Override public boolean onTouchEvent(android.view.MotionEvent e) {
   if (e.getAction() == android.view.MotionEvent.ACTION_UP && pick != null) { float cell = getWidth() / 7f; int col = (int) (e.getX() / cell), row = (int) (e.getY() / cell); int off = month.atDay(1).getDayOfWeek().getValue() - 1; int d = row * 7 + col - off + 1; if (d >= 1 && d <= month.lengthOfMonth()) pick.accept(d); }
   return true;
  }
 }

 /** Thin pace bar: filled share, with a tick where the month should be by now. */
 static final class Pace extends Anim {
  final float used, tick; final int fill, trackColor; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  Pace(Context c, float used, float tick, int fill, int trackColor) { super(c); this.used = used; this.tick = tick; this.fill = fill; this.trackColor = trackColor; }
  @Override protected void onDraw(Canvas c) {
   float h = Ui.dp(8), y = getHeight() / 2f, w = getWidth(); p.setColor(trackColor); c.drawRoundRect(new RectF(0, y - h / 2, w, y + h / 2), h, h, p);
   p.setColor(fill); c.drawRoundRect(new RectF(0, y - h / 2, Math.max(h, Math.min(1f, used) * w * t), y + h / 2), h, h, p);
   if (tick >= 0) { p.setColor(fill); p.setAlpha(255); float x = Math.min(w - Ui.dp(1.5f), tick * w); c.drawRect(x - Ui.dp(1), y - h, x + Ui.dp(1), y + h, p); }
  }
 }

 /** Launch mark: the ledger H draws itself, then fills. */
 static final class Logo extends Anim {
  final Path path = new Path(); final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), mark = new Paint(Paint.ANTI_ALIAS_FLAG); final PathMeasure pm; final float len; final Path part = new Path(); final Path ticks = new Path();
  Logo(Context c) {
   super(c);
   path.moveTo(282, 278); path.lineTo(464, 320); path.lineTo(464, 478); path.lineTo(560, 456); path.lineTo(560, 298); path.lineTo(742, 256); path.lineTo(742, 710); path.lineTo(560, 752); path.lineTo(560, 578); path.lineTo(464, 600); path.lineTo(464, 774); path.lineTo(282, 732); path.close();
   pm = new PathMeasure(path, true); len = pm.getLength();
   fill.setColor(Ui.onAccent); stroke.setColor(Ui.onAccent); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(10); stroke.setStrokeJoin(Paint.Join.ROUND);
   mark.setColor(Ui.accent); mark.setStyle(Paint.Style.STROKE); mark.setStrokeWidth(17); mark.setStrokeCap(Paint.Cap.ROUND);
   ticks.moveTo(342, 407); ticks.lineTo(404, 421); ticks.moveTo(622, 397); ticks.lineTo(684, 383); ticks.moveTo(342, 656); ticks.lineTo(404, 670); ticks.moveTo(622, 661); ticks.lineTo(684, 647);
  }
  @Override void play() {
   if (!Ui.motion(this)) { t = 1f; return; }
   t = 0f; ValueAnimator a = ValueAnimator.ofFloat(0f, 1f); a.setDuration(720); a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
   a.addUpdateListener(x -> { t = (float) x.getAnimatedValue(); invalidate(); }); a.start();
  }
  @Override protected void onDraw(Canvas c) {
   float side = Math.min(getWidth(), getHeight()), left = (getWidth() - side) / 2f, top = (getHeight() - side) / 2f;
   float tile = Math.min(1f, t / 0.28f); float sc = 0.82f + 0.18f * tile; Paint bgp = new Paint(Paint.ANTI_ALIAS_FLAG); bgp.setColor(Ui.accent); bgp.setAlpha((int) (255 * tile));
   c.save(); c.scale(sc, sc, getWidth() / 2f, getHeight() / 2f); c.drawRoundRect(new RectF(left, top, left + side, top + side), side * 0.24f, side * 0.24f, bgp);
   float s = side * 0.62f / 500f; c.translate(getWidth() / 2f - 512 * s, getHeight() / 2f - 526 * s); c.scale(s, s);
   float draw = Math.max(0f, Math.min(1f, (t - 0.18f) / 0.5f)), flood = Math.max(0f, (t - 0.6f) / 0.4f);
   stroke.setStrokeWidth(12); part.reset(); pm.getSegment(0, len * draw, part, true); c.drawPath(part, stroke);
   if (flood > 0) { fill.setAlpha((int) (255 * flood)); c.drawPath(path, fill); mark.setAlpha((int) (255 * flood)); c.drawPath(ticks, mark); }
   c.restore();
  }
 }
}
