package in.aicon.hisab;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class Drive1031Test {
 MainActivity a;
 @Before public void start()throws Exception{a=ShotTest.start(false);}
 @Test public void encryptRoundTripAndWrongPassword()throws Exception{
  byte[] zip="hello backup zip bytes".getBytes();byte[] enc=DriveBackup.encrypt(zip,"correct horse".toCharArray());
  assertTrue(DriveBackup.isEncrypted(enc));assertFalse(DriveBackup.isEncrypted(zip));
  assertArrayEquals(zip,DriveBackup.decrypt(enc,"correct horse".toCharArray()));
  try{DriveBackup.decrypt(enc,"wrong pass!".toCharArray());fail();}catch(Exception e){}
  enc[enc.length-1]^=1;try{DriveBackup.decrypt(enc,"correct horse".toCharArray());fail();}catch(Exception e){}
 }
 @Test public void payloadIsPlainZipByDefaultAndEncryptedWithPassword()throws Exception{
  DriveBackup.setPassword(a.db,"");byte[] p=DriveBackup.payload(a.db);assertFalse(DriveBackup.isEncrypted(p));assertTrue(DriveBackup.zipRead(p).containsKey("snapshot.json"));
  DriveBackup.setPassword(a.db,"password123");byte[] q=DriveBackup.payload(a.db);assertTrue(DriveBackup.isEncrypted(q));
  assertTrue(DriveBackup.zipRead(DriveBackup.decrypt(q,"password123".toCharArray())).containsKey("snapshot.json"));
  DriveBackup.setPassword(a.db,"");
 }
 @Test public void passwordNeverEntersTheSnapshot()throws Exception{
  DriveBackup.setPassword(a.db,"password123");String snap=new String(Backup.snapshot(a.db),"UTF-8");assertFalse(snap.contains("password123"));DriveBackup.setPassword(a.db,"");
 }
 @Test public void photosSwitchDefaultsOnAndSkipsImages()throws Exception{
  assertTrue(DriveBackup.images(a.db));
  java.io.File f=java.io.File.createTempFile("bill",".jpg");java.nio.file.Files.write(f.toPath(),new byte[]{1,2,3,4});
  long acc=a.db.accounts().get(0).id;Store.Tx t=new Store.Tx();t.kind="expense";t.amount=100;t.day="2026-11-20";t.account=acc;long id=a.db.save(t);a.db.addAttach(id,f.getAbsolutePath());
  assertTrue(DriveBackup.zipRead(DriveBackup.buildZip(a.db,true)).containsKey("att/"+f.getName()));
  assertFalse(DriveBackup.zipRead(DriveBackup.buildZip(a.db,false)).containsKey("att/"+f.getName()));
 }
 @Test public void settingsShowsTheTwoSwitchesWhenBackupIsOn()throws Exception{
  a.db.setPref("driveBackup","1");a.closeAll();a.settingsSection="data";Settings.open(a);
  String t=Release1030Test.texts(a.root);assertTrue(t.contains("Include photos and PDFs"));assertTrue(t.contains("Encrypt my backup with a password"));
 }
}
