package tj.beataddiction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ResetDataReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.resetAllIsUsageExceeded();
        dbHelper.close();
    }
}