package in.aicon.hisab;

/** Phone number cleanup. Indian 10-digit mobiles get +91 so WhatsApp opens the chat instead of an invite page. */
final class Phone {
 private Phone() {}

 /** Canonical form: +<digits> when the country is known, bare digits otherwise. Unknown shapes are kept as typed. */
 static String normalise(String raw) {
  if (raw == null) return "";
  String trimmed = raw.trim();
  boolean plus = trimmed.startsWith("+");
  String d = trimmed.replaceAll("[^0-9]", "");
  if (d.isEmpty()) return "";
  if (plus) return "+" + d;
  if (d.length() == 10 && d.matches("[6-9][0-9]{9}")) return "+91" + d;
  if (d.length() == 11 && d.startsWith("0") && d.substring(1).matches("[6-9][0-9]{9}")) return "+91" + d.substring(1);
  if (d.length() == 12 && d.startsWith("91") && d.substring(2).matches("[6-9][0-9]{9}")) return "+" + d;
  if (d.length() >= 11 && d.length() <= 15) return "+" + d;
  return d;
 }

 /** Digits only, for wa.me links. */
 static String wa(String phone) { return normalise(phone).replace("+", ""); }
}
