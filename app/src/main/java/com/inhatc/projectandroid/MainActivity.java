package com.inhatc.projectandroid;

import android.Manifest;
import android.content.Context;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView tvCurrentMonth, selectedDateTextView;
    private GridLayout gridCalendar;
    private final Calendar calendar = Calendar.getInstance();
    private Integer lastSelectedDay = null;
    private TextView lastSelectedTextView = null;
    private SharedPreferences sharedPreferences;
    private WeatherNotificationManager weatherNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();
        setupBottomNavigation();
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

    private void initializeDefaultSharedPreferences() {
        if (!sharedPreferences.contains("selectedRegion")) {
            sharedPreferences.edit().putString("selectedRegion", "Seoul").apply();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "알림 권한이 허용됨");
            } else {
                Log.d("MainActivity", "알림 권한이 거부됨, 알림 기능 제한");
            }
        }
    }

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

    private long calculateInitialDelay() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() - System.currentTimeMillis();
    }

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
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int prevMonthDays = firstDayOfWeek - 1;
        Calendar today = Calendar.getInstance();

        for (int i = 1; i <= prevMonthDays + maxDaysInMonth; i++) {
            TextView dayTextView = createDayTextView(i, prevMonthDays);

            if (!dayTextView.getText().toString().isEmpty()) {
                int day = Integer.parseInt(dayTextView.getText().toString());
                int correctedDayOfWeek = (prevMonthDays + day) % 7;
                correctedDayOfWeek = (correctedDayOfWeek == 0) ? 7 : correctedDayOfWeek;

                int color;
                switch (correctedDayOfWeek) {
                    case 1:
                        color = ContextCompat.getColor(this, R.color.Sunday);
                        break;
                    case 7:
                        color = ContextCompat.getColor(this, R.color.Saturday);
                        break;
                    default:
                        color = ContextCompat.getColor(this, R.color.black);
                        break;
                }
                dayTextView.setTextColor(color);

                if (isToday(calendar, today, String.valueOf(day))) {
                    dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Today));
                }

                Calendar currentCalendar = Calendar.getInstance();
                currentCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), day);
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.getTime());

                LocalDatabase.getDatabase(this).getDailyScheduleDao().getDailyScheduleInfo(currentDate)
                        .observe(this, schedule -> {
                            if (schedule != null) {
                                dayTextView.setBackgroundResource(R.drawable.calender_underline);
                            }
                        });
            }

            dayTextView.setOnClickListener(view -> onDaySelected(dayTextView));
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
        tv.setText(dayIndex > prevMonthDays ? String.valueOf(dayIndex - prevMonthDays) : "");
        return tv;
    }

    private boolean isToday(Calendar calendar, Calendar today, String dayText) {
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && dayText.equals(String.valueOf(today.get(Calendar.DAY_OF_MONTH)));
    }

    private void onDaySelected(TextView dayTextView) {
        int selectedDay = Integer.parseInt(dayTextView.getText().toString());
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDay);
        Calendar today = Calendar.getInstance();

        if (lastSelectedTextView != null) {
            int previousDay = Integer.parseInt(lastSelectedTextView.getText().toString());
            Calendar previousDate = Calendar.getInstance();
            previousDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), previousDay);

            boolean isToday = previousDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && previousDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && previousDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

            int resetColor;
            if (isToday) {
                resetColor = ContextCompat.getColor(this, R.color.Today);
            } else if (previousDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                resetColor = ContextCompat.getColor(this, R.color.Sunday);
            } else if (previousDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                resetColor = ContextCompat.getColor(this, R.color.Saturday);
            } else {
                resetColor = ContextCompat.getColor(this, R.color.black);
            }
            lastSelectedTextView.setTextColor(resetColor);
        }

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
        MainDateInfoActivity bottomSheet = new MainDateInfoActivity(formattedDate);
        bottomSheet.show(getSupportFragmentManager(), "DateInfoBottomSheet");
    }

    private void updateSelectedDateText(Calendar selectedDate) {
        String regionInKorean = getRegionInKorean(sharedPreferences);
        String regionText = regionInKorean + " 날씨 정보 입니다.";
        String formatted = new SimpleDateFormat("  MM월 dd일 " + regionText, Locale.getDefault()).format(selectedDate.getTime());
        selectedDateTextView.setText(formatted);
        updateWeatherScrollView(sharedPreferences, selectedDate);
    }

    private String getRegionInKorean(SharedPreferences sharedPreferences) {
        Map<String, String> regionMap = new HashMap<>();
        regionMap.put("서울", "Seoul"); regionMap.put("부산", "Busan"); regionMap.put("대구", "Daegu");
        regionMap.put("인천", "Incheon"); regionMap.put("광주", "Gwangju"); regionMap.put("대전", "Daejeon");
        regionMap.put("울산", "Ulsan"); regionMap.put("세종", "Sejong"); regionMap.put("경기도", "Gyeonggi-do");
        regionMap.put("강원도", "Gangwon-do"); regionMap.put("충청북도", "Chungcheongbuk-do");
        regionMap.put("충청남도", "Chungcheongnam-do"); regionMap.put("전라북도", "Jeollabuk-do");
        regionMap.put("전라남도", "Jeollanam-do"); regionMap.put("경상북도", "Gyeongsangbuk-do");
        regionMap.put("경상남도", "Gyeongsangnam-do"); regionMap.put("제주도", "Jeju-do");

        String savedRegion = sharedPreferences.getString("selectedRegion", "Seoul");
        for (Map.Entry<String, String> entry : regionMap.entrySet()) {
            if (entry.getValue().equals(savedRegion)) {
                return entry.getKey();
            }
        }
        return "서울";
    }

    private void updateMonthDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy 년 MM월", Locale.getDefault());
        tvCurrentMonth.setText(dateFormat.format(calendar.getTime()));
    }

    private void setupCalendarControls() {
        findViewById(R.id.imageButton_calender_monthLeft).setOnClickListener(v -> navigateMonth(-1));
        findViewById(R.id.imageButton_calender_monthRight).setOnClickListener(v -> navigateMonth(1));
    }

    private void navigateMonth(int offset) {
        calendar.add(Calendar.MONTH, offset);
        updateCalendar();
    }

    private void setupResultListener() {
        getSupportFragmentManager().setFragmentResultListener("memoAdded", this, (key, bundle) -> {
            String addedDate = bundle.getString("addedDate");
            if (addedDate != null && !addedDate.isEmpty()) {
                Log.d("MainActivity", "메모 추가됨: " + addedDate);
                updateCalendar();
            }
        });
    }

    private void setupDeleteListener() {
        getSupportFragmentManager().setFragmentResultListener("memoDeleted", this, (key, bundle) -> {
            String deletedDate = bundle.getString("deletedDate");
            if (deletedDate != null && !deletedDate.isEmpty()) {
                Log.d("MainActivity", "메모 삭제됨: " + deletedDate);
                updateCalendar();
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_calendar) return true;
            else if (itemId == R.id.nav_weather) {
                startActivity(new Intent(this, WeatherActivity.class));
                return true;
            } else if (itemId == R.id.nav_add) {
                MainAddActivity bottomSheet = new MainAddActivity();
                bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
                return true;
            }
            return false;
        });
    }

    private void updateWeatherScrollView(SharedPreferences sharedPreferences, Calendar selectedDate) {
        LinearLayout weatherScrollLayout = findViewById(R.id.linearLayout_main_in_scrollview);
        HorizontalScrollView parentScrollView = findViewById(R.id.scrollview_main_in_cardview);
        String city = sharedPreferences.getString("selectedRegion", "defaultCity");
        Log.e("API_LOG_Checking_Region", "selected Region is : " + city);

        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());

        Calendar currentDate = Calendar.getInstance();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        currentDate.set(Calendar.MILLISECOND, 0);

        Calendar fiveDaysLater = Calendar.getInstance();
        fiveDaysLater.add(Calendar.DAY_OF_YEAR, 5);
        fiveDaysLater.set(Calendar.HOUR_OF_DAY, 0);
        fiveDaysLater.set(Calendar.MINUTE, 0);
        fiveDaysLater.set(Calendar.SECOND, 0);
        fiveDaysLater.set(Calendar.MILLISECOND, 0);

        if (selectedDate.before(currentDate) || selectedDate.after(fiveDaysLater)) {
            parentScrollView.setVisibility(View.GONE);
        } else {
            parentScrollView.setVisibility(View.VISIBLE);
            MainScrollView mainScrollView = new MainScrollView(this, city, formattedDate, weatherScrollLayout);
            mainScrollView.getWeatherForecast();
        }
    }
}
