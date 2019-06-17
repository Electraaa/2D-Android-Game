package nr23730.lagekarte_trackerlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;

public class StartTracker extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, BackgroundTracker.class);
        if (VERSION.SDK_INT >= 26) {
            context.startForegroundService(myIntent);
        } else {
            context.startService(myIntent);
        }
    }
}
