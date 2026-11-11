package in.aicon.pocket;
import android.content.Context;import android.graphics.Typeface;import android.view.*;import android.widget.*;import java.util.*;
final class AppFonts {
 static final String[] CHOICES={"System default","Comfortaa Medium","Inter","Lora","Poppins"};static final Map<String,Typeface> cache=new HashMap<>();
 static String selected(Context c){return c instanceof QuickActivity&&((QuickActivity)c).settings!=null?((QuickActivity)c).settings.get("font"):CHOICES[0];}
 static Typeface face(Context c,boolean bold){String selected=selected(c);if(selected.equals(CHOICES[0]))return Typeface.create(bold?"sans-serif-medium":"sans-serif",0);String name=selected.equals(CHOICES[1])?"comfortaa":selected.toLowerCase(Locale.ROOT);int weight=selected.equals(CHOICES[1])?500:bold?600:400;String key=name+weight;Typeface face=cache.get(key);if(face==null){Typeface.Builder builder=new Typeface.Builder(c.getAssets(),"fonts/"+name+".ttf");builder.setWeight(weight);if(!name.equals("poppins"))builder.setFontVariationSettings("'wght' "+weight);face=builder.build();cache.put(key,face);}return face;}
 static void apply(Context c,View v){if(v instanceof TextView){TextView t=(TextView)v;boolean bold=t.getTypeface()!=null&&t.getTypeface().isBold();t.setTypeface(face(c,bold));}if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)apply(c,((ViewGroup)v).getChildAt(n));}
}
