package com.inhatc.projectandroid;

import static android.text.format.DateUtils.isToday;

import android.content.Context;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.ExistingWorkPolicy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ImageButton buttonLeft1;
    private ImageButton buttonLeft2;
    private ImageButton buttonRight1;
    private ImageButton buttonRight2;
    private ImageButton buttonCenter;

    private TextView tvCurrentMonth;
    private TextView selectedDateTextView;
    private GridLayout gridCalendar;
    private Calendar calendar = Calendar.getInstance();

    private Integer lastSelectedDay = null;
    private TextView lastSelectedTextView = null;

    private SharedPreferences sharedPreferences;
    private WeatherNotificationManager weatherNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();
        setupButtonListeners();
        setupResultListener();
        setupDeleteListener();

        gridCalendar = findViewById(R.id.gridLayout_calender_date);
        selectedDateTextView = findViewById(R.id.textview_main_dateWeather);
        tvCurrentMonth = findViewById(R.id.textview_calender_yearMonth);

        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        initializeDefaultSharedPreferences();
        setupCalendarControls();
        updateCalendar();

        Calendar today = Calendar.getInstance();
        updateSelectedDateText(today);

        weatherNotificationManager = new WeatherNotificationManager(this, LocalDatabase.getDatabase(this));

        if (!sharedPreferences.getBoolean("isNotificationScheduled", false)) {
            scheduleWeatherNotifications();
            sharedPreferences.edit().putBoolean("isNotificationScheduled", true).apply();
        }
    }

    // SharedPreferences에 기본 지역 설정 저장
    private void initializeDefaultSharedPreferences() {
        if (!sharedPreferences.contains("selectedRegion")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selectedRegion", "Seoul");
            editor.apply(); // 비동기 저장
        }
    }

    // Android 13 이상에서 알림 권한 요청
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "알림 권한이 허용됨");
            } else {
                Log.d("MainActivity", "알림 권한이 거부됨, 알림 기능 제한됨");
            }
        }
    }

    // 매 정각에 알림 예약
    private void scheduleWeatherNotifications() {
        long initialDelay = calculateInitialDelay();

        OneTimeWorkRequest notificationRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "HourlyWeatherNotification",
                ExistingWorkPolicy.REPLACE,
                notificationRequest
        );
    }

    // 다음 1시간 정각까지 남은 시간 계산
    private long calculateInitialDelay() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis() - System.currentTimeMillis();
    }

    // 월 이동 버튼 설정
    private void setupCalendarControls() {
        findViewById(R.id.imageButton_calender_monthLeft).setOnClickListener(view -> navigateMonth(-1));
        findViewById(R.id.imageButton_calender_monthRight).setOnClickListener(view -> navigateMonth(1));
    }

    // 현재 달로 이동 (onResume 시 호출)
    @Override
    protected void onResume() {
        super.onResume();
        moveToCurrentMonth();
    }

    private void moveToCurrentMonth() {
        calendar.setTime(Calendar.getInstance().getTime());
        updateCalendar();
    }

    private void updateCalendar() {
        updateMonthDisplay();
        displayDaysInGrid();
    }

    private void displayDaysInGrid() {
        gridCalendar.removeAllViews();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK);
        int maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int prevMonthDays = firstDayOfMonth - 1;

        Calendar today = Calendar.getInstance();

        for (int i = 1; i <= prevMonthDays + maxDaysInMonth; i++) {
            TextView dayTextView = createDayTextView(i, prevMonthDays);

            if (!dayTextView.getText().toString().isEmpty()) {
                int day = Integer.parseInt(dayTextView.getText().toString());
                int dayOfWeek = (prevMonthDays + day) % 7;
                int correctedDayOfWeek = (dayOfWeek == 0) ? 7 : dayOfWeek;

                int colorRes;
                switch (correctedDayOfWeek) {
                    case 1:
                        colorRes = R.color.Sunday;
                        break;
                    case 7:
                        colorRes = R.color.Saturday;
                        break;
                    default:
                        colorRes = R.color.black;
                }
                dayTextView.setTextColor(ContextCompat.getColor(this, colorRes));
            }

            if (isToday(calendar, today, dayTextView.getText().toString())) {
                dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Today));
            }

            // 날짜 일정 데이터 있는지 확인
            if (!dayTextView.getText().toString().isEmpty()) {
                int day = Integer.parseInt(dayTextView.getText().toString());

                Calendar currentCalendar = Calendar.getInstance();
                currentCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), day);

                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(currentCalendar.getTime());

                // Java에서는 코루틴 대신 백그라운드 쓰레드 사용
                new Thread(() -> {
                    DailyScheduleDao dao = LocalDatabase.getDatabase(MainActivity.this).getDailyScheduleDao();
                    DailySchedule dailySchedule = dao.getDailyScheduleInfo(currentDate);

                    if (dailySchedule != null) {
                        runOnUiThread(() -> dayTextView.setBackgroundResource(R.drawable.calender_underline));
                    }
                }).start();
            }

            // 클릭 이벤트 설정
            dayTextView.setOnClickListener(v -> onDaySelected(dayTextView));
            gridCalendar.addView(dayTextView);
        }
    }

    private TextView createDayTextView(int dayIndex, int prevMonthDays) {
        TextView tv = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tv.setLayoutParams(params);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(23f);
        tv.setPadding(0, 8, 0, 8);

        if (dayIndex > prevMonthDays) {
            tv.setText(String.valueOf(dayIndex - prevMonthDays));
        } else {
            tv.setText("");
        }

        return tv;
    }

    private void onDaySelected(TextView dayTextView) {
        String dayStr = dayTextView.getText().toString();
        if (dayStr.isEmpty()) return;

        int selectedDay = Integer.parseInt(dayStr);

        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDay);

        Calendar today = Calendar.getInstance();

        if (lastSelectedTextView != null) {
            String prevDayStr = lastSelectedTextView.getText().toString();
            if (!prevDayStr.isEmpty()) {
                int previousDay = Integer.parseInt(prevDayStr);
                Calendar previousDate = Calendar.getInstance();
                previousDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), previousDay);

                boolean isToday = previousDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        previousDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        previousDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

                int resetColor;
                if (isToday) {
                    resetColor = ContextCompat.getColor(this, R.color.Today);
                } else {
                    int dayOfWeek = previousDate.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == Calendar.SUNDAY) {
                        resetColor = ContextCompat.getColor(this, R.color.Sunday);
                    } else if (dayOfWeek == Calendar.SATURDAY) {
                        resetColor = ContextCompat.getColor(this, R.color.Saturday);
                    } else {
                        resetColor = ContextCompat.getColor(this, R.color.black);
                    }
                }

                lastSelectedTextView.setTextColor(resetColor);
            }
        }

        // 새로 선택된 날짜 강조
        dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Selected));
        lastSelectedTextView = dayTextView;

        if (lastSelectedDay != null && lastSelectedDay == selectedDay) {
            Log.d("Double", "(" + selectedDay + ")");
            showBottomSheet(selectedDate);
        } else {
            lastSelectedDay = selectedDay;
            updateSelectedDateText(selectedDate);
        }
    }

    private void showBottomSheet(Calendar selectedDate) {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());

        // BottomSheetDialogFragment를 Java에서 직접 사용하려면 클래스가 DialogFragment를 상속해야 함
        MainDateInfoActivity bottomSheet = new MainDateInfoActivity(formattedDate);
        bottomSheet.show(getSupportFragmentManager(), "DateInfoBottomSheet");
    }

}