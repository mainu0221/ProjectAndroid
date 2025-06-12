package com.inhatc.projectandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface DailyScheduleDao {

    @Insert
    long insertDailySchedule(DailySchedule dailySchedule);

    @Delete
    void deleteDailySchedule(DailySchedule dailySchedule);

    @Query("SELECT * FROM DailySchedule WHERE date = :date")
    LiveData<DailySchedule> getDailyScheduleInfo(String date);

    @Query("SELECT content FROM DailySchedule WHERE date = :date LIMIT 1")
    String getDailyScheduleInfoSync(String date);

    @Query("DELETE FROM DailySchedule WHERE date = :date")
    void deleteDailyScheduleInfo(String date);
}
