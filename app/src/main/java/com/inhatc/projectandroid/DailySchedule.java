package com.inhatc.projectandroid;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "DailySchedule")
class DailySchedule {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String date;
    private String content;

    public DailySchedule(String date, String content) {
        this.date = date;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
