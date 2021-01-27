package tj.beataddiction;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class AppInfoActivity extends AppCompatActivity {
    private static final String TAG = "AppInfoActivity";
    private static final int LAUNCH_SETTINGS_ACTIVITY = 1;
    private DatabaseHelper dbHelper;
    private String packageName;
    private String appName;
    private TextView appNameView;
    private ImageView appIcon;
    private Spinner hour1;
    private Spinner minute1;
    private TextView hour2;
    private TextView minute2;
    private TextView second2;
    private Button saveButton;
    private Button stopButton;
    private Button chartButton;
    private ImageView warning;
    private TrackedAppInfo trackedAppInfo;
    private int timeAllowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        if(!Utils.isUsageAccessAllowed(this)) {
            openUsageDialog();
        }

        packageName = getIntent().getStringExtra("packageName");
        appName = getIntent().getStringExtra("appName");

        dbHelper = new DatabaseHelper(this);

        appNameView = findViewById(R.id.chart_app_name);
        appIcon = findViewById(R.id.list_app_icon);
        hour1 = findViewById(R.id.hour1);
        minute1 = findViewById(R.id.minute1);
        hour2 = findViewById(R.id.hour2);
        minute2 = findViewById(R.id.minute2);
        second2 = findViewById(R.id.second2);
        saveButton = findViewById(R.id.save);
        stopButton = findViewById(R.id.stop);
        chartButton = findViewById(R.id.show_chart);
        warning = findViewById(R.id.isUsageExceeded);

        setSpinner();
        setAppNameAndImage();

        trackedAppInfo = dbHelper.getRow(packageName);
        if(trackedAppInfo != null) {
            timeAllowed = trackedAppInfo.getTimeAllowed();
            showTimeAllowed(timeAllowed);

            stopButton.setVisibility(View.VISIBLE);
            saveButton.setText(R.string.save);
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openTrackingDialog();
                }
            });
        }
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTrackingInfo();
            }
        });

        chartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AppInfoActivity.this, ChartActivity.class);
                intent.putExtra("packageName", packageName);
                intent.putExtra("appName", appName);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int timeSpent = getTimeSpent();
        showTimeSpent(timeSpent);
        if(trackedAppInfo != null) {
            if(timeSpent > timeAllowed) {
                warning.setVisibility(ImageView.VISIBLE);
                hour2.setTextColor(getResources().getColor(R.color.warning, null));
                minute2.setTextColor(getResources().getColor(R.color.warning, null));
                second2.setTextColor(getResources().getColor(R.color.warning, null));
            }
        }
    }

    private void setSpinner() {
        Integer[] hourValues = new Integer[24];
        final Integer[] minuteValues = new Integer[60];
        for(int i=0; i<24; i++) {
            hourValues[i] = i;
        }
        for(int i=0; i<60; i++) {
            minuteValues[i] = i;
        }
        ArrayAdapter<Integer> hourAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, hourValues);
        ArrayAdapter<Integer> minuteAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, minuteValues);
        hour1.setAdapter(hourAdapter);
        minute1.setAdapter(minuteAdapter);
    }

    private void setAppNameAndImage() {
        appNameView.setText(appName);
        PackageManager packageManager = getPackageManager();
        try {
            appIcon.setImageDrawable(packageManager.getApplicationIcon(packageName));
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "package name not found");
        }
    }

    private void showTimeAllowed(int timeAllowed) {
        int[] timesAllowed = Utils.reverseProcessTime(timeAllowed);
        hour1.setSelection(timesAllowed[0]);
        minute1.setSelection(timesAllowed[1]);
    }

    private void showTimeSpent(int timeSpent) {
        int[] timesAllowed = Utils.reverseProcessTime(timeSpent);
        hour2.setText(String.valueOf(timesAllowed[0]));
        minute2.setText(String.valueOf(timesAllowed[1]));
        second2.setText(String.valueOf(timesAllowed[2]));
    }

    private int getTimeSpent() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long beginTime = calendar.getTimeInMillis();
        long endTime = beginTime + Utils.DAY_IN_MILLIS;
        HashMap<String, Integer> appUsageMap = Utils.getTimeSpent(this, packageName, beginTime, endTime);
        Integer usageTime = appUsageMap.get(packageName);
        if (usageTime == null) usageTime = 0;
        return usageTime;
    }

    private void editTrackingInfo() {
        Integer hour = Integer.parseInt(hour1.getSelectedItem().toString());
        Integer minute = Integer.parseInt(minute1.getSelectedItem().toString());
        int editedTimeAllowed = Utils.processTime(hour, minute, 0);
        if(dbHelper.getRow(packageName) == null) {
            dbHelper.insert(packageName, editedTimeAllowed);
        }
        else {
            dbHelper.setTimeAllowed(packageName, editedTimeAllowed);
        }
        if(editedTimeAllowed > getTimeSpent()) {
            dbHelper.resetIsUsageExceeded(packageName);
        }
        Toast.makeText(getApplicationContext(),"Changes Saved!",Toast.LENGTH_LONG).show();
        finish();
    }

    private void openUsageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Usage Access Needed :(")
                .setMessage("You need to give usage access to this app to see usage data of your apps. " +
                        "Click \"Go To Settings\" and then give the access :)")
                .setPositiveButton("Go To Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent usageAccessIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivityForResult(usageAccessIntent, LAUNCH_SETTINGS_ACTIVITY);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false);
        builder.show();
    }

    private void openTrackingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you want to stop tracking this app?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.delete(packageName);
                        dialog.dismiss();
                        finish();
                    }
                })
                .setCancelable(false);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_SETTINGS_ACTIVITY) {
            if(Utils.isUsageAccessAllowed(this)) {
                Alarms.scheduleNotification(this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}