package com.inhatc.projectandroid;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//API 클라이언트를 초기화하고, Retrofit 인스턴스를 통해 Weather API 서비스 인터페이스를 반환하는 클래스
public class ApiClient {

    // OpenWeatherMap API의 기본 URL
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    // Retrofit 인스턴스를 저장하는 정적 변수
    private static Retrofit retrofit = null;

    /*
    WeatherApiService 인스턴스를 반환하는 정적 메서드
    Retrofit이 초기화되지 않았을 경우 새로 생성하여 사용
     */
    public static WeatherApiService getWeatherApiService() {
        if (retrofit == null) {
            // Retrofit 인스턴스 생성
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        // Weather API 서비스 인터페이스 생성 및 반환
        return retrofit.create(WeatherApiService.class);
    }
}
