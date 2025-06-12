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

    private void updateWeatherUI(Forecast forecast, int index) {
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

        TextView timeView = linearLayoutMain.findViewById(timeIds[index]);
        TextView tempView = linearLayoutMain.findViewById(tempIds[index]);
        ImageView iconView = linearLayoutMain.findViewById(iconIds[index]);
        TextView descView = linearLayoutMain.findViewById(descIds[index]);
        TextView rainView = linearLayoutMain.findViewById(rainIds[index]);

        Date kstDate = parseUtcToKstTime(forecast.getDt_txt());
        String formattedTime = (kstDate != null) ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(kstDate) : "알 수 없음";

        String iconCode = forecast.getWeather().get(0).getIcon();
        String category = getWeatherCategory(iconCode);
        String description = getWeatherDescriptionInKorean(iconCode);
        String iconUrl = "https://openweathermap.org/img/wn/" + category + "@2x.png";

        timeView.setText(formattedTime);
        tempView.setText(String.format(Locale.getDefault(), "%.1f°C", forecast.getMain().getTemp()));
        rainView.setText(String.format(Locale.getDefault(), "%.0f%%", forecast.getPop() * 100));
        descView.setText(description);

        Glide.with(context).load(iconUrl).into(iconView);
    }

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
