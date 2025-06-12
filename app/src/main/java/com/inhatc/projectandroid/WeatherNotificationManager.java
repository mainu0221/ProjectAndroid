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

    public void checkWeatherConditions(WeatherApiService apiService, String apiKey, String region) {
        String units = "metric";
        Call<WeatherForecastResponse> call = apiService.getWeatherForecast(region, apiKey, units);

        try {
            Response<WeatherForecastResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Forecast> forecastList = response.body().getList();
                if (forecastList == null || forecastList.isEmpty()) return;

                Calendar now = Calendar.getInstance();
                int hour = now.get(Calendar.HOUR_OF_DAY);

                if (hour >= 5 && hour < 8) {
                    handleDailyWeather(forecastList, "06:00:00");
                } else if (hour >= 21 || hour < 1) {
                    handleDailyWeather(forecastList, "21:00:00");
                } else {
                    handleCurrentWeather(forecastList);
                }
            } else {
                Log.e("WeatherNotify", "API response 실패: " + response.code());
            }
        } catch (Exception e) {
            Log.e("WeatherNotify", "API 요청 에러: " + e.getMessage());
        }
    }

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

    private String getTodayDate() {
        Calendar today = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH));
    }

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

    private void sendNotification(String title, String content) {
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
