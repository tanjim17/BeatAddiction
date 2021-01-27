package tj.beataddiction;

import androidx.appcompat.app.AppCompatActivity;

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class ChartActivity extends AppCompatActivity {
    private String packageName;
    private String appName;
    private BarChart barChart;
    private TextView appNameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        packageName = getIntent().getStringExtra("packageName");
        appName = getIntent().getStringExtra("appName");

        barChart = findViewById(R.id.barchart);
        appNameView = findViewById(R.id.chart_app_name);
        appNameView.setText(appName);

        ArrayList<BarEntry> entries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long beginTime = calendar.getTimeInMillis();
        long endTime = beginTime + Utils.DAY_IN_MILLIS;
        for (int daysAgo = 0; daysAgo < 7; daysAgo++) {
            HashMap<String, Integer> appUsageMap = Utils.getTimeSpent(this, packageName, beginTime, endTime);
            Integer usageTime = appUsageMap.get(packageName);
            if (usageTime == null) usageTime = 0;
            entries.add(new BarEntry(usageTime, 7 - daysAgo - 1));
            beginTime -= Utils.DAY_IN_MILLIS;
            endTime -= Utils.DAY_IN_MILLIS;
        }
        BarDataSet bardataset = new BarDataSet(entries, "daily usage");

        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM");
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -6);
        for (int i = 0; i < 7; i++) {
            labels.add(dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DATE, 1);
        }

        BarData data = new BarData(labels, bardataset);
        barChart.setData(data);
        barChart.setDescription(null);
        barChart.getData().setValueFormatter(new TimeFormatter());
        barChart.getXAxis().setLabelsToSkip(0);
        barChart.getAxisLeft().setDrawLabels(false);
        barChart.getAxisRight().setDrawLabels(false);
        barChart.getAxisLeft().setAxisMinValue(0);
        barChart.animateY(500);
        bardataset.setColors(ColorTemplate.COLORFUL_COLORS);
    }

    private class TimeFormatter implements ValueFormatter {
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            if(value == 0) {
                return "0s";
            }
            int[] usageTimes = Utils.reverseProcessTime((int)value);
            String hour = String.valueOf(usageTimes[0]);
            String minute = String.valueOf(usageTimes[1]);
            String second = String.valueOf(usageTimes[2]);
            String time = "";
            if(usageTimes[0] > 0) time += hour + "h ";
            if(usageTimes[1] > 0) time += minute + "m ";
            if(usageTimes[2] > 0) time += second + "s";
            return time;
        }
    }
}
