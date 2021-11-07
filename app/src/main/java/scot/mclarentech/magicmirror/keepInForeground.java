package scot.mclarentech.magicmirror;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class keepInForeground extends Service {

    private static String TAG = "kifService";
    private Handler handler;
    private Runnable runnable;
    private final int runTime = 5000;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        // handler = new Handler();
        // runnable = new Runnable() {
            // @Override
            // public void run() {
                // handler.postDelayed(runnable, runTime);
            // }
        // };
        // handler.post(runnable);
    }


    // public keepInForeground() {
        // Log.i(TAG,"keepInForeground");
    // }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.i(TAG,"onBind");
        // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        Intent i = new Intent(this, FullscreenActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Log.i(TAG, "Start Activity");
        // this.startActivity(i);
        return START_STICKY;
    }

    // @SuppressWarnings("deprecation")
    // @Override
    // public void onStart(Intent intent, int startId) {
        //super.onStart(intent, startId);
        //Log.i(TAG, "onStart");
    //}

}
