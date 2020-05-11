package tj.beataddiction;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.APP_OPS_SERVICE;

class Utils {
    static long DAY_IN_MILLIS = 86400 * 1000;

    static int processTime(int hour, int minute, int second) {
        return hour*3600 + minute*60 + second;
    }

    static int[] reverseProcessTime(int time) {
        int[] hourMinSec = new int[3];
        hourMinSec[0] = time / 3600;
        time = time % 3600;
        hourMinSec[1] = time / 60;
        hourMinSec[2] = time % 60;
        return hourMinSec;
    }

    static HashMap<String, Integer> getTimeSpent(Context context, String packageName, long beginTime, long endTime) {
        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEvents = new ArrayList<>();
        HashMap<String, Integer> appUsageMap = new HashMap<>();

        UsageStatsManager usageStatsManager = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);

        while (usageEvents.hasNextEvent()) {
            currentEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(currentEvent);
            if(currentEvent.getPackageName().equals(packageName) || packageName == null) {
                if (currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                        || currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED) {
                    allEvents.add(currentEvent);
                    String key = currentEvent.getPackageName();
                    if (appUsageMap.get(key) == null)
                        appUsageMap.put(key, 0);
                }
            }
        }

        for (int i = 0; i < allEvents.size() - 1; i++) {
            UsageEvents.Event E0 = allEvents.get(i);
            UsageEvents.Event E1 = allEvents.get(i + 1);

            if (E0.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                    && E1.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED
                    && E0.getClassName().equals(E1.getClassName())) {
                int diff = (int)(E1.getTimeStamp() - E0.getTimeStamp());
                diff /= 1000;
                Integer prev = appUsageMap.get(E0.getPackageName());
                if(prev == null) prev = 0;
                appUsageMap.put(E0.getPackageName(), prev + diff);
            }
        }

        UsageEvents.Event lastEvent = allEvents.get(allEvents.size() - 1);
        if(lastEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
            int diff = (int)System.currentTimeMillis() - (int)lastEvent.getTimeStamp();
            diff /= 1000;
            Integer prev = appUsageMap.get(lastEvent.getPackageName());
            if(prev == null) prev = 0;
            appUsageMap.put(lastEvent.getPackageName(), prev + diff);
        }

        return appUsageMap;
    }

    static boolean isUsageAccessAllowed(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager)context.getSystemService(APP_OPS_SERVICE);
            int mode = 0;
            if (appOpsManager != null) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid, applicationInfo.packageName);
            }
            return mode==AppOpsManager.MODE_ALLOWED;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}