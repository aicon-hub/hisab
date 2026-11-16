package in.aicon.hisab;

import android.view.WindowManager;
import android.widget.*;

final class Privacy {
 private Privacy() {}
 static boolean hidden(Store db, Store.Acc a) {return db.flag("hideAll",false)||db.flag("hide:"+a.kind,false);}
 static String amount(Store db,Store.Acc a,long balance){return hidden(db,a)?"••••":Fmt.money(balance);}
 static boolean anyHidden(Store db){for(Store.Acc a:db.accounts())if(hidden(db,a))return true;return false;}
 static void apply(MainActivity a){if(a.db.flag("secureScreen",false))a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);else a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}
 static boolean backupDue(Store db){if(!db.flag("backupNudge",true)||db.flag("sample",false)||!db.hasData())return false;try{return System.currentTimeMillis()-Long.parseLong(db.pref("lastBackupAt","0"))>7L*24*60*60*1000;}catch(Exception e){return true;}}
 static void toggle(MainActivity a,LinearLayout c,String label,String key,boolean def,Runnable after){
  LinearLayout row=Ui.row(a);row.setPadding(0,Ui.dp(14),0,Ui.dp(14));row.addView(Ui.text(a,label,15,Ui.ink,true),Ui.weight(1));Switch sw=new Switch(a);sw.setContentDescription(L.t(label));sw.setChecked(a.db.flag(key,def));row.addView(sw);sw.setTag(key);sw.setOnCheckedChangeListener((v,on)->{a.db.setPref(key,on?"1":"0");after.run();});c.addView(row);
 }
 static void open(MainActivity a){Kit.page(a,"Privacy",null,null,c->{
  c.addView(Ui.text(a,"Hide account balances",20,Ui.ink,true));
  c.addView(Ui.text(a,"Masks account and SMS balance displays, account pickers and totals. Entries, charts and exports are not hidden. This is a display option, not a password lock.",14,Ui.muted,false),Ui.lpm(-1,-2,0,12,0,12));
  toggle(a,c,"Hide all balances","hideAll",false,()->a.refresh());
  toggle(a,c,"Hide bank balances","hide:bank",false,()->a.refresh());
  toggle(a,c,"Hide cash balances","hide:cash",false,()->a.refresh());
  toggle(a,c,"Hide wallet balances","hide:wallet",false,()->a.refresh());
  toggle(a,c,"Hide card balances","hide:card",false,()->a.refresh());
  c.addView(Ui.divider(a));
  toggle(a,c,"Block screenshots and screen recording","secureScreen",false,()->apply(a));
  c.addView(Ui.text(a,"Uses Android screen protection, including the recent-app preview where supported. Some devices may behave differently. Turn it off before taking a screenshot for support.",13,Ui.muted,false));
  toggle(a,c,"Weekly backup nudge on Home","backupNudge",true,()->a.refresh());
  c.addView(Ui.text(a,"A local reminder only. No backup runs automatically. The nudge disappears for seven days after a successful encrypted export.",13,Ui.muted,false));
 });}
}
