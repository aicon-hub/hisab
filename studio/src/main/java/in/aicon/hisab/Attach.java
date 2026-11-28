package in.aicon.hisab;

import android.content.Intent;
import android.graphics.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Photos and bill images attached to any entry. Files live in the app's own folder on this phone. */
final class Attach {
 private Attach() {}

 static File dir(MainActivity a) { File d = a.getExternalFilesDir("attachments"); if (d != null) d.mkdirs(); return d; }

 static void add(MainActivity a, long txId, Runnable changed) {
  List<String> labels = new ArrayList<>(), icons = new ArrayList<>();
  labels.add("Take a photo"); icons.add("eye");
  labels.add("Pick from gallery"); icons.add("upload");
  Sheets.pick(a, "Attach to this entry", labels, icons, 0, i -> {
   if (i == 0) {
    File f = new File(dir(a), "att-" + txId + "-" + System.currentTimeMillis() + ".jpg");
    android.net.Uri out = androidx.core.content.FileProvider.getUriForFile(a, "in.aicon.hisab.studio2.fileprovider", f);
    Intent cap = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT, out).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    a.request(cap, d -> {
     if (f.exists() && f.length() > 0 && seal(a, f)) { a.db.addAttach(txId, f.getAbsolutePath()); a.toast("Photo attached", null); } else f.delete();
     a.refresh(); if (changed != null) changed.run();
    });
   } else {
    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
    a.request(pick, d -> {
     if (d == null || d.getData() == null) return;
     File f = copyIn(a, txId, d.getData());
     if (f != null) { a.db.addAttach(txId, f.getAbsolutePath()); a.toast("Photo attached", null); } else a.toast("That photo could not be read", null);
     a.refresh(); if (changed != null) changed.run();
    });
   }
  });
 }

 /** Encrypts a photo the camera just wrote. False when it could not be saved safely. */
 static boolean seal(MainActivity a, File f) { try { PhotoVault.sealInPlace(a, f); return true; } catch (Exception e) { return false; } }

 static File copyIn(MainActivity a, long txId, android.net.Uri u) {
  try {
   byte[] bytes = Backup.read(a.getContentResolver().openInputStream(u));
   BitmapFactory.Options opt = new BitmapFactory.Options(); opt.inJustDecodeBounds = true;
   BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
   if (opt.outWidth <= 0) return null;
   opt.inSampleSize = 1;
   while (Math.max(opt.outWidth, opt.outHeight) / opt.inSampleSize > 2000) opt.inSampleSize *= 2;
   opt.inJustDecodeBounds = false;
   Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
   if (bmp == null) return null;
   File f = new File(dir(a), "att-" + txId + "-" + System.currentTimeMillis() + ".jpg");
   ByteArrayOutputStream jpg = new ByteArrayOutputStream();
   bmp.compress(Bitmap.CompressFormat.JPEG, 88, jpg); bmp.recycle();
   PhotoVault.write(a, f, jpg.toByteArray());
   return f;
  } catch (Exception e) { return null; }
 }

 /** Decodes a stored photo, sealed or not. Null when it is missing or damaged. */
 static Bitmap load(MainActivity a, String path) {
  try { byte[] b = PhotoVault.read(a, new File(path)); return BitmapFactory.decodeByteArray(b, 0, b.length); } catch (Exception e) { return null; }
 }

 static void removeAll(MainActivity a, long txId) {
  for (Store.Att att : a.db.attachments(txId)) new File(att.path).delete();
  a.db.removeAttachFor(txId);
 }

 static void view(MainActivity a, Store.Tx t) {
  LinearLayout c = Sheets.sheetBody(a, "Bills and photos");
  List<Store.Att> atts = a.db.attachments(t.id);
  if (atts.isEmpty()) c.addView(Ui.text(a, "Nothing attached yet. Add a photo of the bill or the item.", 14, Ui.muted, false));
  for (Store.Att att : atts) {
   LinearLayout col = Ui.col(a); col.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
   ImageView iv = new ImageView(a); iv.setAdjustViewBounds(true); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
   Bitmap bmp = load(a, att.path);
   if (bmp != null) iv.setImageBitmap(bmp);
   col.addView(iv, new LinearLayout.LayoutParams(-1, Ui.dp(220)));
   TextView rm = Ui.text(a, "Remove", 13, Ui.neg, true); rm.setPadding(0, Ui.dp(6), 0, Ui.dp(4)); col.addView(rm);
   Ui.tap(rm, () -> { new File(att.path).delete(); a.db.removeAttach(att.id); a.closeAll(); view(a, t); });
   c.addView(col);
  }
  TextView add = Ui.button(a, "Add bill or photo", true); c.addView(add, Ui.lpm(-1, -2, 0, 12, 0, 0));
  Ui.tap(add, () -> add(a, t.id, () -> { a.closeAll(); view(a, t); }));
  Sheets.show(a, c);
 }
}
