package com.inhatc.projectandroid;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "WeatherList")
public class WeatherList {

    @PrimaryKey(autoGenerate = true)
    private long wNo;
    private String weather;
    private String wTime;
    private String wText;
    private boolean isNotified;

    @Ignore
    public WeatherList(String weather, String wTime, String wText, boolean isNotified) {
        this.weather = weather;
        this.wTime = wTime;
        this.wText = wText;
        this.isNotified = isNotified;
    }

    public WeatherList(long wNo, String weather, String wTime, String wText, boolean isNotified) {
        this.wNo = wNo;
        this.weather = weather;
        this.wTime = wTime;
        this.wText = wText;
        this.isNotified = isNotified;
    }

    public long getWNo() {
        return wNo;
    }

    public void setWNo(long wNo) {
        this.wNo = wNo;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getWTime() {
        return wTime;
    }

    public void setWTime(String wTime) {
        this.wTime = wTime;
    }

    public String getWText() {
        return wText;
    }

    public void setWText(String wText) {
        this.wText = wText;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean notified) {
        isNotified = notified;
    }
}
