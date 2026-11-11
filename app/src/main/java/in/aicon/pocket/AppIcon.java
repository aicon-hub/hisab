package in.aicon.pocket;
import android.content.*;import android.content.pm.PackageManager;import java.util.*;
/** Original colour variants. Launcher refresh is controlled by the device launcher. */
public final class AppIcon {
 public static final String[] COLORS={"Orange","Teal","Blue","Violet","Rose","Graphite"};
 private AppIcon(){}
 public static void choose(Context context,String color){
  if(!Arrays.asList(COLORS).contains(color))throw new IllegalArgumentException("Unknown icon colour");
  PackageManager pm=context.getPackageManager();String prefix="in.aicon.pocket.Icon";ComponentName selected=new ComponentName(context,prefix+color);
  // Enable the chosen launcher BEFORE disabling any previous one.
  pm.setComponentEnabledSetting(selected,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
  for(String other:COLORS)if(!other.equals(color))pm.setComponentEnabledSetting(new ComponentName(context,prefix+other),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
  context.getSharedPreferences("prefs",0).edit().putString("icon",color).apply();
 }
}
