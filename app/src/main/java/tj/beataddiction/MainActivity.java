package tj.beataddiction;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView appListView;
    private static int LAUNCH_SETTINGS_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        appListView = findViewById(R.id.app_list);

        Alarms.resetIsUsageExceededData(getApplicationContext());
        startBackgroundService();

        openDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAppListAndSetClickListener();
    }

    private List<AppInfo> getAppInfoList() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        List<AppInfo> appInfoList = new ArrayList<>();

        for (int i = 0; i < packageInfoList.size(); i++) {
            PackageInfo packageInfo = packageInfoList.get(i);
            if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                String appName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                Drawable appIcon = packageInfo.applicationInfo.loadIcon(packageManager);
                String packageName = packageInfo.packageName;

                TrackedAppInfo trackedAppInfo = dbHelper.getRow(packageName);
                if(trackedAppInfo != null) {
                    boolean isUsageExceeded = trackedAppInfo.getIsUsageExceeded() == 1;
                    appInfoList.add(new AppInfo(appName, appIcon, packageName, true, isUsageExceeded));
                }
                else {
                    appInfoList.add(new AppInfo(appName, appIcon, packageName, false, false));
                }
            }
        }
        Collections.sort(appInfoList);
        return appInfoList;
    }

    private void showAppListAndSetClickListener() {
        final List<AppInfo>appInfoList = getAppInfoList();
        AppInfoListAdapter appInfoListAdapter = new AppInfoListAdapter(MainActivity.this, appInfoList);
        appListView.setAdapter(appInfoListAdapter);

        appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                Intent intent = new Intent(MainActivity.this, AppInfoActivity.class);
                intent.putExtra("packageName", appInfoList.get(i).getPackageName());
                intent.putExtra("appName", appInfoList.get(i).getAppName());
                startActivity(intent);
            }
        });
    }

    private void startBackgroundService() {
        if(Utils.isUsageAccessAllowed(this)) {
            Alarms.scheduleNotification(getApplicationContext());
        }
    }

    private void openDialog() {
        final SharedPreferences sharedPreferences = getSharedPreferences("DialogInfo", Context.MODE_PRIVATE);

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("openDialog", false);
                editor.apply();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("IMPORTANT!")
                .setMessage("You might not get usage notifications after reboot. To solve this issue, you can either " +
                        "give auto start permission for this app manually from settings or launch " +
                        "this app at least once after reboot. That's all :)")
                .setView(checkBoxView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false);

        if(sharedPreferences.getBoolean("openDialog", true)) {
            builder.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();

    }
}