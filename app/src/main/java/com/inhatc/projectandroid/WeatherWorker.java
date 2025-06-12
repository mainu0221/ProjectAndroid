package com.inhatc.projectandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WeatherWorker extends Worker {

    private final LocalDatabase database;
    private final WeatherNotificationManager notificationManager;
    private final SharedPreferences sharedPreferences;
    private final WeatherApiService apiService;
    private final String apiKey = "74c26aef7529a784cee3247a261edd92";

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        database = LocalDatabase.getDatabase(context);
        sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        apiService = ApiClient.getWeatherApiService();
        notificationManager = new WeatherNotificationManager(context, database);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul");
            if (selectedRegion == null) selectedRegion = "Seoul";

            ExecutorService executor = Executors.newSingleThreadExecutor();
            String finalSelectedRegion = selectedRegion;
            executor.execute(() -> {
                try {
                    notificationManager.checkWeatherConditions(apiService, apiKey, finalSelectedRegion);
                } catch (Exception e) {
                    Log.e("WeatherWorker", "Notification check failed: " + e.getMessage());
                }
            });

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);

            Log.d("WeatherWorker", "작업 성공적으로 완료");
            scheduleNextRun();
            return Result.success();

        } catch (Exception e) {
            Log.e("WeatherWorker", "Error: " + e.getMessage());
            return Result.retry();
        }
    }

    private void scheduleNextRun() {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("WeatherWorker", ExistingWorkPolicy.REPLACE, workRequest);

        Log.d("WeatherWorker", "다음 작업이 15분 후에 예약되었습니다.");
    }
}
