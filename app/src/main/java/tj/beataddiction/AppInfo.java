package tj.beataddiction;

import android.graphics.drawable.Drawable;

public class AppInfo implements Comparable<AppInfo>{
    private String appName;
    private Drawable icon;
    private String packageName;
    private boolean isTracked;
    private boolean isUsageExceeded;

    AppInfo(String appName, Drawable icon, String packageName, boolean isTracked, boolean isUsageExceeded) {
        this.appName = appName;
        this.icon = icon;
        this.packageName = packageName;
        this.isTracked = isTracked;
        this.isUsageExceeded = isUsageExceeded;
    }
    String getAppName() {
        return appName;
    }
    Drawable getIcon() {
        return icon;
    }
    String getPackageName() {
        return packageName;
    }
    boolean getIsTracked() {
        return isTracked;
    }
    boolean getIsUsageExceeded() {
        return isUsageExceeded;
    }

    @Override
    public int compareTo(AppInfo appInfo) {
        return appName.compareTo(appInfo.getAppName());
    }
}
