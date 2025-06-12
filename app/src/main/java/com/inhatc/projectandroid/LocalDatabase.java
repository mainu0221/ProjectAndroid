package com.inhatc.projectandroid;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {WeatherList.class, DailySchedule.class}, version = 1)
public abstract class LocalDatabase extends RoomDatabase {

    private static volatile LocalDatabase INSTANCE;

    public abstract WeatherTextDao getWeatherTextDao();
    public abstract DailyScheduleDao getDailyScheduleDao();

    public static LocalDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (LocalDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    LocalDatabase.class, "local_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
