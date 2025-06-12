package com.inhatc.projectandroid;

public class WeatherListItem {
    private long wNo;
    private String contents;
    private String weather;
    private String time;
    private boolean isNotified;

    public WeatherListItem(long wNo, String contents, String weather, String time, boolean isNotified) {
        this.wNo = wNo;
        this.contents = contents;
        this.weather = weather;
        this.time = time;
        this.isNotified = isNotified;
    }

    public long getWNo() {
        return wNo;
    }

    public void setWNo(long wNo) {
        this.wNo = wNo;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean notified) {
        isNotified = notified;
    }
}
