package in.aicon.hisab;
final class AiModelsTestUtil { static String sha(byte[] d) throws Exception { StringBuilder b = new StringBuilder(); for (byte x : java.security.MessageDigest.getInstance("SHA-256").digest(d)) b.append(String.format("%02x", x)); return b.toString(); } }
