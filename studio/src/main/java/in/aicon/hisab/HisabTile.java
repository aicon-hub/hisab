package in.aicon.hisab;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

/** Quick-settings tile: tap opens Hisab. */
public class HisabTile extends TileService {
 @Override public void onClick() {
  super.onClick();
  Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try {
   if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
   else startActivityAndCollapse(i);
  } catch (Exception ignored) {}
 }
}
