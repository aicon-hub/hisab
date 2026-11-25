package in.aicon.hisab;

import android.text.InputType;
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
  LinearLayout row=Ui.row(a);row.setPadding(0,Ui.dp(14),0,Ui.dp(14));row.addView(Ui.text(a,label,15,Ui.ink,true),Ui.weight(1));Switch sw=Ui.toggle(a);sw.setContentDescription(L.t(label));sw.setChecked(a.db.flag(key,def));row.addView(sw);sw.setTag(key);sw.setOnCheckedChangeListener((v,on)->{a.db.setPref(key,on?"1":"0");after.run();});c.addView(row);
 }
 static void pinForm(MainActivity a, String title, boolean needsCurrent, java.util.function.Consumer<Boolean> done){
  Runnable[] askNew = new Runnable[1];
  askNew[0] = () -> PinPad.ask(a, title, "Choose a PIN of 4 to 8 digits", p1 -> {
   if (!Lock.valid(p1)) { a.toast("Use 4 to 8 digits", null); done.accept(false); return; }
   PinPad.ask(a, title, "Enter the same PIN again", p2 -> {
    if (!p1.equals(p2)) { a.toast("The two PINs do not match", null); done.accept(false); return; }
    Lock.set(a.db, p1); done.accept(true);
   });
  });
  if (needsCurrent) PinPad.ask(a, title, "Enter your current PIN", cur -> {
   if (!Lock.verify(a.db, cur)) { a.toast("Current PIN is wrong", null); done.accept(false); return; }
   askNew[0].run();
  });
  else askNew[0].run();
 }
 static void open(MainActivity a){Kit.page(a,"Privacy",null,null,c->{
  c.addView(Ui.text(a,"App lock",20,Ui.ink,true));
  c.addView(Ui.text(a,"Ask for a PIN when Hisab opens in a fresh session. Only a scrambled check is stored on this phone, never the PIN itself. If you forget it, clearing the app's storage is the only way back in, so keep an encrypted backup.",14,Ui.muted,false),Ui.lpm(-1,-2,0,12,0,12));
  boolean locked = Lock.enabled(a.db);
  if (!locked) {
   TextView set = Ui.button(a, "Set a PIN", true); c.addView(set, Ui.lpm(-1,-2,0,0,0,16));
   Ui.tap(set, () -> pinForm(a, "Set a PIN", false, ok -> { if (ok) { a.closeAll(); a.toast("App lock is on", null); open(a); } }));
  } else {
   LinearLayout r = Ui.row(a); TextView ch = Ui.button(a, "Change PIN", false), off = Ui.button(a, "Turn off", false);
   r.addView(ch, Ui.lpm(0,-2,0,0,6,0)); ((LinearLayout.LayoutParams) ch.getLayoutParams()).weight = 1;
   r.addView(off, Ui.lpm(0,-2,6,0,0,0)); ((LinearLayout.LayoutParams) off.getLayoutParams()).weight = 1;
   c.addView(r, Ui.lpm(-1,-2,0,0,0,16));
   Ui.tap(ch, () -> pinForm(a, "Change PIN", true, ok -> { if (ok) { a.closeAll(); a.toast("PIN changed", null); open(a); } }));
   Ui.tap(off, () -> PinPad.ask(a, "Turn off app lock", "Enter your current PIN", cur -> {
    if (Lock.verify(a.db, cur)) { Lock.clear(a.db); a.toast("App lock is off", null); } else a.toast("Wrong PIN. App lock stays on", null);
    a.closeAll(); open(a);
   }));
   if (Bio.available(a)) {
    toggle(a,c,"Unlock with fingerprint or face","bioUnlock",false,()->{});
    c.addView(Ui.text(a,"Uses the phone's own fingerprint or face screen after the app is locked. Hisab never sees or stores anything from the sensor. The PIN still works.",13,Ui.muted,false));
   }
  }
  c.addView(Ui.divider(a));
  c.addView(Ui.text(a,"Hide account balances",20,Ui.ink,true),Ui.lpm(-1,-2,0,20,0,0));
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
