package com.inhatc.projectandroid;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WeatherTextDao {
    @Insert
    long insertWeatherList(WeatherList weatherList);

    @Delete
    void deleteWeatherList(WeatherList weatherList);

    @Query("SELECT * FROM WeatherList")
    List<WeatherList> getAllWeatherList();

    @Query("SELECT * FROM WeatherList WHERE isNotified = 0 AND weather = :description")
    List<WeatherList> getNotNotifiedItems(String description);

    @Update
    void updateWeatherList(WeatherList item);

}
