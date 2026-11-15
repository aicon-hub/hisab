package in.aicon.hisab;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.widget.*;
import android.view.*;
import android.util.Base64;
import java.io.*;
final class Profile {
 static View avatar(MainActivity a,int size){
  String encoded=a.db.pref("profilePhoto","");if(!encoded.isEmpty())try{byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap!=null){ImageView view=new ImageView(a);view.setImageBitmap(bitmap);view.setScaleType(ImageView.ScaleType.CENTER_CROP);view.setBackground(Ui.shape(Ui.surface2,size/2));view.setClipToOutline(true);view.setLayoutParams(Ui.lp(Ui.dp(size),Ui.dp(size)));return view;}}catch(Exception ignored){}
  FrameLayout f=new FrameLayout(a);f.setBackground(Ui.shape(Ui.accent,size/2));f.addView(Ui.big(a,Ui.initial(a.db.pref("name","You")),size*.42f,Ui.onAccent),new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));return f;
 }
 static void choose(MainActivity a){Sheets.pick(a,"Profile photo",java.util.Arrays.asList("Choose a photo","Remove photo"),java.util.Arrays.asList("user","trash"),-1,i->{if(i==1){a.db.setPref("profilePhoto","");a.closeAll();a.refresh();Settings.open(a);return;}Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);a.request(intent,d->{if(d.getData()==null)return;new Thread(()->{try{byte[] bytes=Backup.read(a.getContentResolver().openInputStream(d.getData()));BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);if(opt.outWidth<=0||opt.outHeight<=0)throw new IOException();opt.inSampleSize=1;while(Math.max(opt.outWidth,opt.outHeight)/opt.inSampleSize>1024)opt.inSampleSize*=2;opt.inJustDecodeBounds=false;Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);if(image==null)throw new IOException();int side=Math.min(image.getWidth(),image.getHeight());Bitmap cropped=Bitmap.createBitmap(image,(image.getWidth()-side)/2,(image.getHeight()-side)/2,side,side);Bitmap small=Bitmap.createScaledBitmap(cropped,320,320,true);ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,85,out);a.db.setPref("profilePhoto",Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP));a.runOnUiThread(()->{a.closeAll();a.refresh();Settings.open(a);a.toast("Profile photo saved on this phone",null);});}catch(Exception|OutOfMemoryError e){a.runOnUiThread(()->a.toast("Could not open photo. Try a smaller image",null));}},"hisab-photo").start();});});}
}
