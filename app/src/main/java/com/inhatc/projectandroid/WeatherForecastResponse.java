package com.inhatc.projectandroid;

import java.util.List;

public class WeatherForecastResponse {
    private List<Forecast> list;

    public List<Forecast> getList() {
        return list;
    }

    public void setList(List<Forecast> list) {
        this.list = list;
    }
}
