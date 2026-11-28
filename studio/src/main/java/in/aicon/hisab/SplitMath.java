package in.aicon.hisab;

/** Splits a bill in whole paise. The parts always add up to the total. */
final class SplitMath {
 private SplitMath() {}

 /** Shares for n people. The first (total mod n) people pay one paisa more. Index 0 is the user, the rest follow in the order picked. */
 static long[] shares(long total, int n) {
  if (n < 1) throw new IllegalArgumentException("people");
  long base = total / n, extra = total % n;
  long[] s = new long[n];
  for (int i = 0; i < n; i++) s[i] = base + (i < extra ? 1 : 0);
  return s;
 }
}
