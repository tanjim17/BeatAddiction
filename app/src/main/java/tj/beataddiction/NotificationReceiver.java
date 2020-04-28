package tj.beataddiction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        BackgroundService.enqueueWork(context, new Intent(context, BackgroundService.class));
    }
}
