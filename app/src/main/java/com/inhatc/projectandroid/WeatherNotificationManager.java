package com.inhatc.projectandroid;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;

// 사용자 DB에 등록된 날씨 조건과 실제 예보를 비교하여 조건이 충족되면 알림을 전송하는 관리자 클래스
public class WeatherNotificationManager {

    private static final String CHANNEL_ID = "weather_alert_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final Context context;
    private final LocalDatabase database;

    public WeatherNotificationManager(Context context, LocalDatabase database) {
        this.context = context;
        this.database = database;
        createNotificationChannel();
    }

    // 날씨 예보를 가져오고, 시간대에 따라 알림 검사 수행
    public void checkWeatherConditions(WeatherApiService apiService, String apiKey, String region) {
        String units = "metric"; // 섭씨 단위
        Call<WeatherForecastResponse> call = apiService.getWeatherForecast(region, apiKey, units);

        try {
            Response<WeatherForecastResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Forecast> forecastList = response.body().getList();
                if (forecastList == null || forecastList.isEmpty()) return;

                Calendar now = Calendar.getInstance();
                int hour = now.get(Calendar.HOUR_OF_DAY);

                // 시간대별 알림 로직 분기
                if (hour >= 5 && hour < 8) {
                    handleDailyWeather(forecastList, "06:00:00"); // 오전 6시 예보 기반 알림
                } else if (hour >= 21 || hour < 1) {
                    handleDailyWeather(forecastList, "21:00:00"); // 오후 9시 예보 기반 알림
                } else {
                    handleCurrentWeather(forecastList); // 그 외는 현재 날씨 기준 알림
                }
            } else {
                Log.e("WeatherNotify", "API response 실패: " + response.code());
            }
        } catch (Exception e) {
            Log.e("WeatherNotify", "API 요청 에러: " + e.getMessage());
        }
    }

    // 현재 시간 기준으로 첫 번째 예보를 비교하여 알림 전송
    private void handleCurrentWeather(List<Forecast> forecastList) {
        if (forecastList.isEmpty()) return;

        Forecast nowForecast = forecastList.get(0);
        String description = getCommonDescription(nowForecast.getWeather().get(0).getDescription());

        List<WeatherList> matchedList = database.getWeatherTextDao().getNotNotifiedItems(description);
        for (WeatherList item : matchedList) {
            sendNotification(item.getWeather(), item.getWText());
            item.setNotified(true);
            database.getWeatherTextDao().updateWeatherList(item);
        }
    }

    // 특정 시간대 예보와 DB 조건 비교하여 알림 전송
    private void handleDailyWeather(List<Forecast> forecastList, String targetTime) {
        String today = getTodayDate();
        for (Forecast forecast : forecastList) {
            if (forecast.getDt_txt().contains(today + " " + targetTime)) {
                String description = getCommonDescription(forecast.getWeather().get(0).getDescription());
                List<WeatherList> matched = database.getWeatherTextDao().getNotNotifiedItems(description);
                for (WeatherList item : matched) {
                    sendNotification(item.getWeather(), item.getWText());
                    item.setNotified(true);
                    database.getWeatherTextDao().updateWeatherList(item);
                }
                break;
            }
        }
    }

    // 오늘 날짜를 yyyy-MM-dd 형식으로 반환
    private String getTodayDate() {
        Calendar today = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH));
    }

    // OpenWeatherMap에서 제공하는 다양한 날씨 설명을 공통된 분류로 정리
    private String getCommonDescription(String original) {
        original = original.toLowerCase(Locale.KOREA).trim();
        switch (original) {
            case "clear sky":
                return "clear sky";
            case "few clouds":
            case "scattered clouds":
                return "partly cloudy";
            case "broken clouds":
            case "overcast clouds":
            case "mist":
                return "clouds";
            case "drizzle":
            case "light rain":
            case "moderate rain":
            case "heavy intensity rain":
                return "rain";
            case "light thunderstorm":
            case "thunderstorm":
            case "heavy thunderstorm":
                return "thunderstorm";
            case "light snow":
            case "snow":
            case "heavy snow":
            case "sleet":
                return "snow";
            default:
                return "clear sky";
        }
    }

    // 실제 푸시 알림을 사용자에게 전송
    private void sendNotification(String title, String content) {
        // Android 13 이상에서는 알림 권한 체크 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("WeatherNotify", "알림 권한 없음. 알림 전송 취소됨.");
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(getWeatherIcon(title))
                .setContentTitle("날씨 알림: " + title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    // 날씨 설명에 따른 알림 아이콘 리소스 ID 반환
    private int getWeatherIcon(String weatherDescription) {
        switch (weatherDescription) {
            case "clear sky":
                return R.drawable.weather_sun_icon;
            case "partly cloudy":
                return R.drawable.weather_cloud_icon;
            case "rain":
                return R.drawable.weather_rain_icon;
            case "thunderstorm":
                return R.drawable.weather_thunder_icon;
            case "snow":
                return R.drawable.weather_snow_icon;
            case "clouds":
                return R.drawable.weather_suncloud_icon;
            default:
                return R.drawable.weather_sun_icon;
        }
    }

    // 알림 채널 생성
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "날씨 알림 채널";
            String description = "중요한 날씨 변화에 대한 알림을 제공합니다.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
