package scot.mclarentech.magicmirror;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class RunOnStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autorun = sharedPref.getBoolean("autorun_switch", false);
            if (autorun) {
                Intent i = new Intent(context, FullscreenActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(i);
            }
            else {
                System.exit(0);
            }
        }
    }

}