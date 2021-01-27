package tj.beataddiction;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BackgroundService extends JobIntentService {
    private DatabaseHelper dbHelper;
    private static final String TAG = "BackgroundService";
    private static final int JOB_ID = 1;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, BackgroundService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        dbHelper = new DatabaseHelper(this);
        List<TrackedAppInfo> trackedAppInfos = dbHelper.getAllRows();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long beginTime = calendar.getTimeInMillis();
        long endTime = beginTime + Utils.DAY_IN_MILLIS;
        HashMap<String, Integer> appUsageMap = Utils.getTimeSpent(this, null, beginTime, endTime);

        String currentRunningPackageName = null;
        List<String> list = appUsageMap.keySet().stream().filter(s -> s.startsWith("current")).collect(Collectors.toList());
        if(list.size() > 0) {currentRunningPackageName = list.get(0).replaceFirst("current", "");}

        for(int i = 0; i < trackedAppInfos.size(); i++) {
            TrackedAppInfo trackedAppInfo = trackedAppInfos.get(i);
            String packageName = trackedAppInfo.getPackageName();

            if(appUsageMap.containsKey(packageName)) {
                Integer usageTime = appUsageMap.get(packageName);
                if(usageTime == null) usageTime = 0;
                int allowedTime = trackedAppInfo.getTimeAllowed();
                int isUsageExceeded = trackedAppInfo.getIsUsageExceeded();

                if((usageTime > allowedTime && isUsageExceeded == 0) ||
                        (isUsageExceeded == 1 && packageName.equals(currentRunningPackageName))) {
                    try {
                        dbHelper.setIsUsageExceeded(packageName);
                        String appName = (String) getPackageManager()
                                .getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0));
                        showNotification(appName, i);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "package name not found");
                    }
                }
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void showNotification(String appName, int id) {
        Notification.Builder builder
                = new Notification.Builder(getApplicationContext())
                .setContentTitle(appName + " usage exceeded!")
                .setContentText("Close your app now!")
                .setSmallIcon(R.drawable.warning)
                .setPriority(Notification.PRIORITY_MAX);
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }
}


