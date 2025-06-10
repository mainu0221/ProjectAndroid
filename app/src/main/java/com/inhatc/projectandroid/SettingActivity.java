package com.inhatc.projectandroid;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        nav.setSelectedItemId(R.id.nav_settings);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_weather) {
                startActivity(new Intent(this, WeatherActivity.class));
                return true;
            } else if (id == R.id.nav_add) {
                new MainAddActivity().show(getSupportFragmentManager(), "MainAddActivity");
                return true;
            } else if (id == R.id.nav_check) {
                startActivity(new Intent(this, CheckActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }
}
