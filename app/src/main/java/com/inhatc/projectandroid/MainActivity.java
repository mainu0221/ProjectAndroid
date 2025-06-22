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

        // 오늘 날짜 강조 및 초기 표시
        Calendar today = Calendar.getInstance();
        updateSelectedDateText(today);

        // 날씨 알림 매니저 초기화
        weatherNotificationManager = new WeatherNotificationManager(this, LocalDatabase.getDatabase(this));

        // 최초 실행 시 알림 예약
        if (!sharedPreferences.getBoolean("isNotificationScheduled", false)) {
            scheduleWeatherNotifications();
            sharedPreferences.edit().putBoolean("isNotificationScheduled", true).apply();
        }
    }

    //지역 설정이 없으면 기본값으로 "Seoul" 저장
    private void initializeDefaultSharedPreferences() {
        if (!sharedPreferences.contains("selectedRegion")) {
            sharedPreferences.edit().putString("selectedRegion", "Seoul").apply();
        }
    }

    //알림 권한 요청 (Android 13 이상)
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
                Log.d("MainActivity", "알림 권한이 거부됨, 알림 기능 제한");
            }
        }
    }

    // 매 정각에 날씨 알림 예약
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

    // 다음 정각까지 남은 시간 계산
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

    // 달력에 날짜 표시 및 각 날짜별 처리
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
                // 요일별 색상 적용
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
                // 오늘 날짜 강조
                if (isToday(calendar, today, String.valueOf(day))) {
                    dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Today));
                }
                // 일정 존재 시 밑줄 표시
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
            // 날짜 클릭 이벤트
            dayTextView.setOnClickListener(view -> onDaySelected(dayTextView));
            gridCalendar.addView(dayTextView);
        }
    }

    // 달력에 표시될 개별 날짜를 생성하는 메서드
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

    // 특정 날짜가 오늘인지 판단하는 메서드
    private boolean isToday(Calendar calendar, Calendar today, String dayText) {
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && dayText.equals(String.valueOf(today.get(Calendar.DAY_OF_MONTH)));
    }

    // 날짜 셀이 클릭되었을 때 처리하는 메서드
    private void onDaySelected(TextView dayTextView) {
        int selectedDay = Integer.parseInt(dayTextView.getText().toString());
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDay);
        Calendar today = Calendar.getInstance();
        // 이전에 선택된 셀이 있다면 색상 복구
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
        // 현재 선택된 날짜 셀 강조
        dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Selected));
        lastSelectedTextView = dayTextView;
        // 같은 날짜를 두 번 클릭하면 하단 시트 표시
        if (lastSelectedDay != null && lastSelectedDay == selectedDay) {
            Log.d("Double", "(" + selectedDay + ")");
            showBottomSheet(selectedDate);
        } else {
            // 새로운 날짜 선택 처리
            lastSelectedDay = selectedDay;
            updateSelectedDateText(selectedDate);
        }
    }

    // 날짜 상세정보를 보여주는 BottomSheet 다이얼로그 호출
    private void showBottomSheet(Calendar selectedDate) {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
        MainDateInfoActivity bottomSheet = new MainDateInfoActivity(formattedDate);
        bottomSheet.show(getSupportFragmentManager(), "DateInfoBottomSheet");
    }

    // 상단 날짜 텍스트 및 날씨 스크롤뷰 업데이트
    private void updateSelectedDateText(Calendar selectedDate) {
        String regionText = "서울 날씨 정보 입니다.";
        String formatted = new SimpleDateFormat("  MM월 dd일 " + regionText, Locale.getDefault()).format(selectedDate.getTime());
        selectedDateTextView.setText(formatted);
        updateWeatherScrollView(sharedPreferences, selectedDate);
    }

    // 텍스트뷰에 현재 년/월 정보 표시
    private void updateMonthDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy 년 MM월", Locale.getDefault());
        tvCurrentMonth.setText(dateFormat.format(calendar.getTime()));
    }

    // 이전/다음 달 이동 버튼 설정
    private void setupCalendarControls() {
        findViewById(R.id.imageButton_calender_monthLeft).setOnClickListener(v -> navigateMonth(-1));
        findViewById(R.id.imageButton_calender_monthRight).setOnClickListener(v -> navigateMonth(1));
    }

    // 월 이동 후 달력 갱신
    private void navigateMonth(int offset) {
        calendar.add(Calendar.MONTH, offset);
        updateCalendar();
    }

    // 메모가 추가되었을 때 달력 새로고침 리스너 설정
    private void setupResultListener() {
        getSupportFragmentManager().setFragmentResultListener("memoAdded", this, (key, bundle) -> {
            String addedDate = bundle.getString("addedDate");
            if (addedDate != null && !addedDate.isEmpty()) {
                Log.d("MainActivity", "메모 추가됨: " + addedDate);
                updateCalendar();
            }
        });
    }

    // 메모가 삭제되었을 때 달력 새로고침 리스너 설정
    private void setupDeleteListener() {
        getSupportFragmentManager().setFragmentResultListener("memoDeleted", this, (key, bundle) -> {
            String deletedDate = bundle.getString("deletedDate");
            if (deletedDate != null && !deletedDate.isEmpty()) {
                Log.d("MainActivity", "메모 삭제됨: " + deletedDate);
                updateCalendar();
            }
        });
    }

    // 하단 네비게이션바 추가 및 화면 이동 처리
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

    // 선택된 날짜에 따라 날씨 예보 스크롤 뷰 업데이트
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
