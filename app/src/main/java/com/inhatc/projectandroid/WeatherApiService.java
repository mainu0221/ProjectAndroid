package com.inhatc.projectandroid;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("forecast")
    Call<WeatherForecastResponse> getWeatherForecast(
            @Query("q") String city,
            @Query("appid") String apiKey,
            @Query("units") String units  // 기본값은 설정 못 하니 호출 시 "metric" 전달 필요
    );
}
