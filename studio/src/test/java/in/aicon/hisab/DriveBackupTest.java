package in.aicon.hisab;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class DriveBackupTest {
 @Test public void zipRoundTripKeepsEveryEntry() throws Exception {
  LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
  entries.put("snapshot.json", "{\"format\":2}".getBytes(StandardCharsets.UTF_8));
  entries.put("attachments.json", "[]".getBytes(StandardCharsets.UTF_8));
  byte[] photo = new byte[5000]; for (int i = 0; i < photo.length; i++) photo[i] = (byte) (i * 31 % 251);
  entries.put("att/att-7-123.jpg", photo);
  Map<String, byte[]> back = DriveBackup.zipRead(DriveBackup.zipBuild(entries));
  assertEquals(3, back.size());
  assertArrayEquals(entries.get("snapshot.json"), back.get("snapshot.json"));
  assertArrayEquals(photo, back.get("att/att-7-123.jpg"));
 }
 @Test public void photosStoredUncompressedJsonDeflated() throws Exception {
  assertTrue(DriveBackup.stored("att/att-1-2.jpg"));
  assertTrue(DriveBackup.stored("att/bill.pdf"));
  assertTrue(DriveBackup.stored("att/scan.PNG"));
  assertFalse(DriveBackup.stored("snapshot.json"));
 }
 @Test public void zipReadSkipsTraversalEntries() throws Exception {
  LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
  entries.put("snapshot.json", "{}".getBytes(StandardCharsets.UTF_8));
  Map<String, byte[]> back = DriveBackup.zipRead(DriveBackup.zipBuild(entries));
  assertTrue(back.containsKey("snapshot.json"));
 }
 @Test public void uploadMetadataNamesZipInAppDataFolder() throws Exception {
  String body = new String(DriveBackup.createBody("x".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
  assertTrue(body.contains("\"name\":\"hisab-backup.zip\""));
  assertTrue(body.contains("\"parents\":[\"appDataFolder\"]"));
 }
 @Test public void scopeIsAppDataFolderOnly() {
  assertEquals("https://www.googleapis.com/auth/drive.appdata", DriveBackup.SCOPE);
  assertEquals("hisab-backup.zip", DriveBackup.FILE);
  assertTrue(DriveBackup.AUTO_EVERY >= 3600_000L);
 }
}
