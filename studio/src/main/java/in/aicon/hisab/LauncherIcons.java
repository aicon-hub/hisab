package in.aicon.hisab;
import android.content.*;
import android.content.pm.*;
final class LauncherIcons {
 static String variant(Store db){String accent=db.pref("accent","Lime");if(!java.util.Arrays.asList(Ui.ACCENT_NAMES).contains(accent))accent="Lime";return db.flag("matchLauncher",true)?accent:"Original";}
 static void apply(MainActivity a){String chosen=variant(a.db);PackageManager pm=a.getPackageManager();
  try{String[] names=java.util.Arrays.copyOf(Ui.ACCENT_NAMES,Ui.ACCENT_NAMES.length+1);names[names.length-1]="Original";
   // Enable the new launcher target before disabling the previous one.
   ComponentName target=new ComponentName(a,"in.aicon.hisab.Icon"+chosen.replace(" ",""));if(pm.getComponentEnabledSetting(target)!=PackageManager.COMPONENT_ENABLED_STATE_ENABLED)pm.setComponentEnabledSetting(target,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
   for(String name:names)if(!name.equals(chosen)){ComponentName c=new ComponentName(a,"in.aicon.hisab.Icon"+name.replace(" ",""));if(pm.getComponentEnabledSetting(c)!=PackageManager.COMPONENT_ENABLED_STATE_DISABLED)pm.setComponentEnabledSetting(c,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);}
  }catch(IllegalArgumentException|SecurityException e){a.toast("Launcher icon could not change on this launcher",null);}
 }
}
