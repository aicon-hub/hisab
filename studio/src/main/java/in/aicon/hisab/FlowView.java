package in.aicon.hisab;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/**
 * Live graph of one Ask Hisab run. One block fans out to three, to three more, to three more, then all
 * lines gather into one line and end in the Answer block. Each block is one real engine step.
 */
final class FlowView extends View {
 static final int N = 11;
 static final String[] NAMES = {"Question", "Language", "Intent", "Period", "Match", "Read entries", "Read lists", "Filter", "Check", "Sources", "Answer"};
 private static final float[][] POS = {
  {0.07f, 0.5f},
  {0.27f, 0.14f}, {0.27f, 0.5f}, {0.27f, 0.86f},
  {0.48f, 0.14f}, {0.48f, 0.5f}, {0.48f, 0.86f},
  {0.69f, 0.14f}, {0.69f, 0.5f}, {0.69f, 0.86f},
  {0.93f, 0.5f}};
 private static final int[][] EDGES = {{0, 1}, {0, 2}, {0, 3}, {1, 4}, {1, 5}, {2, 5}, {2, 6}, {3, 6}, {3, 4}, {4, 7}, {4, 8}, {5, 8}, {5, 9}, {6, 9}, {6, 7}};
 private static final int[] LAST = {7, 8, 9};
 /** Time (ms on the display clock) at which each block starts and finishes. -1 = not reached yet. */
 final long[] start = new long[N], end = new long[N];
 /** One colour per block, so each step is recognisable. */
 static final int[] COLORS = {0xFF7AA2FF, 0xFFB58CFF, 0xFFFF8FB1, 0xFFFFB86B, 0xFF5EEAD4, 0xFF4ADE80, 0xFF67E8F9, 0xFFFDE047, 0xFFF472B6, 0xFFFB923C, 0xFFC7F23A};
 private long mergeAt = -1, gap = 250;
 private long now;
 private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG);

 FlowView(Context c) {
  super(c); reset();
  tp.setTypeface(Ui.bodyBold); tp.setTextAlign(Paint.Align.CENTER); tp.setTextSize(Ui.dp(8f));
 }
 void reset() { for (int i = 0; i < N; i++) { start[i] = -1; end[i] = -1; } mergeAt = -1; now = 0; invalidate(); }
 void at(long ms) { now = ms; invalidate(); }
 private float fill(int i) { if (start[i] < 0 || now < start[i]) return 0; return Math.max(0, Math.min(1, (now - start[i]) / (float) Math.max(1, end[i] - start[i]))); }
 private boolean active(int i) { return start[i] >= 0 && now >= start[i] && now < end[i]; }
 @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(210)); }

 private static float prog(long now, long a, long b) { if (a < 0 || b < 0 || now < a) return 0; return Math.max(0, Math.min(1, (now - a) / (float) Math.max(1, b - a))); }
 private void line(Canvas c, float x1, float y1, float x2, float y2, float f, int color) {
  Path path = new Path(); path.moveTo(x1, y1); float mx = (x1 + x2) / 2; path.cubicTo(mx, y1, mx, y2, x2, y2);
  p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dp(1.6f)); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(Ui.line); c.drawPath(path, p);
  if (f > 0) { PathMeasure pm = new PathMeasure(path, false); Path seg = new Path(); pm.getSegment(0, pm.getLength() * f, seg, true); p.setColor(color); p.setStrokeWidth(Ui.dp(2.4f)); c.drawPath(seg, p);
   if (f < 1) { float[] pos = new float[2]; pm.getPosTan(pm.getLength() * f, pos, null); p.setStyle(Paint.Style.FILL); c.drawCircle(pos[0], pos[1], Ui.dp(3f), p); }
  }
 }

 @Override protected void onDraw(Canvas c) {
  int W = getWidth(), H = getHeight(); float nw = W * 0.155f, nh = Ui.dp(34);
  for (int[] e : EDGES) {
   float x1 = POS[e[0]][0] * W + nw / 2, y1 = POS[e[0]][1] * H, x2 = POS[e[1]][0] * W - nw / 2, y2 = POS[e[1]][1] * H;
   line(c, x1, y1, x2, y2, prog(now, end[e[0]], start[e[1]]), COLORS[e[0]]);
  }
  // gather: the last blocks join one point, then one line to the answer
  float mx = W * 0.81f, my = H * 0.5f;
  for (int j : LAST) line(c, POS[j][0] * W + nw / 2, POS[j][1] * H, mx, my, mergeAt < 0 ? 0 : prog(now, end[j], mergeAt), COLORS[j]);
  line(c, mx, my, POS[10][0] * W - nw / 2, POS[10][1] * H, mergeAt < 0 ? 0 : prog(now, mergeAt, start[10]), COLORS[10]);
  p.setStyle(Paint.Style.FILL); p.setColor(mergeAt >= 0 && now >= mergeAt ? COLORS[10] : Ui.line); c.drawCircle(mx, my, Ui.dp(3.5f), p);
  for (int i = 0; i < N; i++) {
   float cx = POS[i][0] * W, cy = POS[i][1] * H; RectF r = new RectF(cx - nw / 2, cy - nh / 2, cx + nw / 2, cy + nh / 2);
   float f = fill(i);
   // a short "pop" right after the step finishes
   if (end[i] >= 0 && now >= end[i] && now < end[i] + 220) { float k = (float) Math.sin(Math.PI * (now - end[i]) / 220f) * Ui.dp(2.5f); r.inset(-k, -k); }
   p.setStyle(Paint.Style.FILL); p.setColor(Ui.surface2); c.drawRoundRect(r, Ui.dp(8), Ui.dp(8), p);
   p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dp(1)); p.setColor(COLORS[i]); p.setAlpha(70); c.drawRoundRect(r, Ui.dp(8), Ui.dp(8), p); p.setAlpha(255);
   if (f > 0) { p.setStyle(Paint.Style.FILL); p.setColor(COLORS[i]); p.setAlpha((int) (255 * f)); c.drawRoundRect(r, Ui.dp(8), Ui.dp(8), p); p.setAlpha(255); }
   if (active(i)) {
    // working: a ring that grows out of the block and fades, repeating while the step runs
    float ph = ((now - start[i]) % 600) / 600f; p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dp(2)); p.setColor(COLORS[i]); p.setAlpha((int) (255 * (1 - ph)));
    RectF g = new RectF(r); g.inset(-ph * Ui.dp(7), -ph * Ui.dp(7)); c.drawRoundRect(g, Ui.dp(8 + 7 * ph), Ui.dp(8 + 7 * ph), p); p.setAlpha(255);
   }
   tp.setColor(f > 0.5f ? 0xFF111317 : Ui.ink);
   String[] lines = NAMES[i].split(" ", 2); float ty = cy + (lines.length == 2 ? -Ui.dp(1) : Ui.dp(3));
   c.drawText(lines[0], cx, ty, tp); if (lines.length == 2) c.drawText(lines[1], cx, ty + Ui.dp(10), tp);
  }
 }

 /** Display schedule: level by level in the order the engine works. Returns total ms. */
 long schedule(long stepMs) {
  reset(); gap = Math.max(100, stepMs * 6 / 10); long lvl = 0, stag = 70;
  start[0] = 0; end[0] = stepMs; lvl = stepMs + gap;
  int[][] levels = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
  long last = 0;
  for (int[] L : levels) { for (int k = 0; k < L.length; k++) { start[L[k]] = lvl + k * stag; end[L[k]] = start[L[k]] + stepMs; last = Math.max(last, end[L[k]]); } lvl += stepMs + gap + 2 * stag; }
  mergeAt = last + gap / 2; start[10] = mergeAt + gap; end[10] = start[10] + stepMs;
  return end[10];
 }
}
