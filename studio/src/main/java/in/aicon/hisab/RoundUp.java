package in.aicon.hisab;

final class RoundUp {
 private RoundUp() {}

 /** Round-up step in minor units from the setting: 0 (off), 1000 (to nearest 10) or 10000 (to nearest 100). */
 static long step(Store db) { String v = db.pref("roundUp", ""); if (v.equals("10")) return 1000; if (v.equals("100")) return 10000; return 0; }

 /** Spare change gathered so far: every expense rounded up to the next step, summed. Virtual - no entries are created. */
 static long pot(Store db) {
  long step = step(db); if (step == 0) return 0;
  long pot = 0;
  for (Store.Tx t : db.txs("0000-01-01", "9999-12-31")) if (t.kind.equals("expense")) { long rem = t.amount % step; if (rem > 0) pot += step - rem; }
  return pot;
 }
}
