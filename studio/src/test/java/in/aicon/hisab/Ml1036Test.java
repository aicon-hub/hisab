package in.aicon.hisab;

import static org.junit.Assert.*;
import org.junit.Test;

public class Ml1036Test {
 @Test public void newScreenTextHasMalayalam() {
  String[] some = {"Year report", "Backup saved to your Google Drive", "Wishlist", "Take a photo", "Net worth", "Trash bin", "Forgot PIN? Use your phone lock"};
  for (String k : some) { assertNotNull(k, L.ML.get(k)); assertTrue(k, L.ML.get(k).matches(".*[\\u0D00-\\u0D7F].*") || k.equals("English")); }
  assertTrue("table grew past 560 entries, now " + L.ML.size(), L.ML.size() > 560);
 }
 @Test public void tableHasNoEmDash() { for (String v : L.ML.values()) assertFalse(v, v.contains("\u2014")); }
}
