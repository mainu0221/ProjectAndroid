package com.inhatc.projectandroid;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//선택된 날짜 및 지역(city)에 대한 날씨 정보를 API에서 받아와 화면 내 LinearLayout에 최대 8개의 예보 정보를 표시하는 클래스
public class MainScrollView {
    private final Context context;
    private final String city;
    private final String selectedDate;
    private final LinearLayout linearLayoutMain;
    private final String apiKey = "74c26aef7529a784cee3247a261edd92";
    private final int maxUIItems = 8;

    public MainScrollView(Context context, String city, String selectedDate, LinearLayout linearLayoutMain) {
        this.context = context;
        this.city = city;
        this.selectedDate = selectedDate;
        this.linearLayoutMain = linearLayoutMain;
    }

    // 날씨 예보 API 호출 및 UI 업데이트 시작
    public void getWeatherForecast() {
        Call<WeatherForecastResponse> call = ApiClient.getWeatherApiService().getWeatherForecast(city, apiKey, "metric");

        call.enqueue(new Callback<WeatherForecastResponse>() {
            @Override
            public void onResponse(Call<WeatherForecastResponse> call, Response<WeatherForecastResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Forecast> forecastList = response.body().getList();

                    String nextDate = getNextDateString(selectedDate);
                    long currentTimeMillis = System.currentTimeMillis();

                    int count = 0;
                    for (Forecast forecast : forecastList) {
                        String dtTxt = forecast.getDt_txt();
                        // 현재 날짜 또는 다음날 데이터 중 시간 순으로 필터링
                        if (dtTxt.startsWith(selectedDate) || dtTxt.startsWith(nextDate)) {
                            Date forecastTime = parseUtcToKstTime(dtTxt);
                            if (forecastTime != null && forecastTime.getTime() >= currentTimeMillis) {
                                updateWeatherUI(forecast, count);
                                count++;
                                if (count >= maxUIItems) break;
                            }
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Error code: " + response.code());
                    Log.e("API_ERROR", "city check, " + city);
                }
            }

            @Override
            public void onFailure(Call<WeatherForecastResponse> call, Throwable t) {
                Log.e("API_FAILURE", "Error: " + t.getMessage());
            }
        });
    }

    // UTC 시간을 KST 시간으로 변환
    private Date parseUtcToKstTime(String utcDateTime) {
        try {
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date utcDate = utcFormat.parse(utcDateTime);

            SimpleDateFormat kstFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            kstFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            return utcDate != null ? kstFormat.parse(kstFormat.format(utcDate)) : null;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 날씨 정보를 화면의 각 위치(index)에 해당하는 View에 반영
    private void updateWeatherUI(Forecast forecast, int index) {
        // UI 컴포넌트 ID 배열
        int[] timeIds = {
                R.id.textview_mainScrollItem_time1, R.id.textview_mainScrollItem_time2,
                R.id.textview_mainScrollItem_time3, R.id.textview_mainScrollItem_time4,
                R.id.textview_mainScrollItem_time5, R.id.textview_mainScrollItem_time6,
                R.id.textview_mainScrollItem_time7, R.id.textview_mainScrollItem_time8
        };

        int[] tempIds = {
                R.id.textview_mainScrollItem_temperature1, R.id.textview_mainScrollItem_temperature2,
                R.id.textview_mainScrollItem_temperature3, R.id.textview_mainScrollItem_temperature4,
                R.id.textview_mainScrollItem_temperature5, R.id.textview_mainScrollItem_temperature6,
                R.id.textview_mainScrollItem_temperature7, R.id.textview_mainScrollItem_temperature8
        };

        int[] iconIds = {
                R.id.imageview_mainScrollItem_weatherIcon1, R.id.imageview_mainScrollItem_weatherIcon2,
                R.id.imageview_mainScrollItem_weatherIcon3, R.id.imageview_mainScrollItem_weatherIcon4,
                R.id.imageview_mainScrollItem_weatherIcon5, R.id.imageview_mainScrollItem_weatherIcon6,
                R.id.imageview_mainScrollItem_weatherIcon7, R.id.imageview_mainScrollItem_weatherIcon8
        };

        int[] descIds = {
                R.id.textview_mainScrollItem_description1, R.id.textview_mainScrollItem_description2,
                R.id.textview_mainScrollItem_description3, R.id.textview_mainScrollItem_description4,
                R.id.textview_mainScrollItem_description5, R.id.textview_mainScrollItem_description6,
                R.id.textview_mainScrollItem_description7, R.id.textview_mainScrollItem_description8
        };

        int[] rainIds = {
                R.id.textview_mainScrollItem_rainText1, R.id.textview_mainScrollItem_rainText2,
                R.id.textview_mainScrollItem_rainText3, R.id.textview_mainScrollItem_rainText4,
                R.id.textview_mainScrollItem_rainText5, R.id.textview_mainScrollItem_rainText6,
                R.id.textview_mainScrollItem_rainText7, R.id.textview_mainScrollItem_rainText8
        };

        // View 객체 가져오기
        TextView timeView = linearLayoutMain.findViewById(timeIds[index]);
        TextView tempView = linearLayoutMain.findViewById(tempIds[index]);
        ImageView iconView = linearLayoutMain.findViewById(iconIds[index]);
        TextView descView = linearLayoutMain.findViewById(descIds[index]);
        TextView rainView = linearLayoutMain.findViewById(rainIds[index]);

        // 시간, 아이콘 코드, 설명, 강수 확률 추출
        Date kstDate = parseUtcToKstTime(forecast.getDt_txt());
        String formattedTime = (kstDate != null) ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(kstDate) : "알 수 없음";

        String iconCode = forecast.getWeather().get(0).getIcon();
        String category = getWeatherCategory(iconCode);
        String description = getWeatherDescriptionInKorean(iconCode);
        String iconUrl = "https://openweathermap.org/img/wn/" + category + "@2x.png";

        // UI 반영
        timeView.setText(formattedTime);
        tempView.setText(String.format(Locale.getDefault(), "%.1f°C", forecast.getMain().getTemp()));
        rainView.setText(String.format(Locale.getDefault(), "%.0f%%", forecast.getPop() * 100));
        descView.setText(description);

        Glide.with(context).load(iconUrl).into(iconView);
    }

    // 다음 날짜(yyyy-MM-dd)를 문자열로 반환
    private String getNextDateString(String selectedDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(selectedDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return sdf.format(cal.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return selectedDate;
        }
    }

    // 날씨 아이콘 코드에 따라 OpenWeather용 정규화된 카테고리 반환
    private String getWeatherCategory(String iconCode) {
        switch (iconCode) {
            case "01d": case "01n": return "01d";
            case "02d": case "02n": case "03d": case "03n": case "04d": case "04n": return "03d";
            case "10d": case "10n": return "10d";
            case "09d": case "09n": return "09d";
            case "11d": case "11n": return "11d";
            case "13d": case "13n": return "13d";
            case "50d": case "50n": return "50d";
            default: return "01d";
        }
    }

    // 날씨 아이콘 코드에 따른 한국어 설명 반환
    private String getWeatherDescriptionInKorean(String iconCode) {
        switch (iconCode) {
            case "01d": case "01n": return "맑은 날씨 ";
            case "02d": case "02n": return "적은 구름 ";
            case "03d": case "03n": return "흐림 ";
            case "04d": case "04n": return "많은 구름 ";
            case "09d": case "09n": return "소나기";
            case "10d": case "10n": return "비";
            case "11d": case "11n": return "천둥 번개";
            case "13d": case "13n": return "눈";
            case "50d": case "50n": return "안개";
            default: return "알 수 없는 날씨";
        }
    }
}
