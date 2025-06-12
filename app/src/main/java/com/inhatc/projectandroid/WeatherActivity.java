package com.inhatc.projectandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherActivity extends AppCompatActivity {

    private RecyclerView weatherlistRecyclerView;
    private WeatherListAdapter adapter;
    private final List<WeatherListItem> items = new ArrayList<>();
    private LocalDatabase database;
    private SharedPreferences sharedPreferences;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        setupBottomNavigation();

        database = LocalDatabase.getDatabase(this);
        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);

        weatherlistRecyclerView = findViewById(R.id.recyclerview_weather_list);
        adapter = new WeatherListAdapter(items, wNo -> {
            int index = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getWNo() == wNo) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                WeatherListItem itemToRemove = items.get(index);
                items.remove(index);
                adapter.notifyItemRemoved(index);

                executor.execute(() -> {
                    database.getWeatherTextDao().deleteWeatherList(new WeatherList(
                            itemToRemove.getWNo(),
                            itemToRemove.getWeather(),
                            itemToRemove.getTime(),
                            itemToRemove.getContents(),
                            itemToRemove.isNotified()
                    ));
                });
            }
        });

        weatherlistRecyclerView.setAdapter(adapter);
        weatherlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadWeatherList();
    }

    private void loadWeatherList() {
        executor.execute(() -> {
            List<WeatherList> savedItems = database.getWeatherTextDao().getAllWeatherList();

            runOnUiThread(() -> {
                for (WeatherList weatherList : savedItems) {
                    items.add(new WeatherListItem(
                            weatherList.getWNo(),
                            weatherList.getWText(),
                            weatherList.getWeather(),
                            weatherList.getWTime(),
                            weatherList.isNotified()
                    ));
                }
                adapter.notifyItemRangeInserted(0, savedItems.size());
            });
        });
    }

    private void addItemToWeatherList(WeatherListItem newItem) {
        executor.execute(() -> {
            WeatherList weatherList = new WeatherList(
                    newItem.getWeather(),
                    newItem.getTime(),
                    newItem.getContents(),
                    newItem.isNotified()
            );
            long insertedId = database.getWeatherTextDao().insertWeatherList(weatherList);

            WeatherListItem updatedItem = new WeatherListItem(
                    insertedId,
                    newItem.getContents(),
                    newItem.getWeather(),
                    newItem.getTime(),
                    newItem.isNotified()
            );

            runOnUiThread(() -> {
                items.add(updatedItem);
                adapter.notifyItemInserted(items.size() - 1);
            });
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_weather) {
                return true;
            } else if (itemId == R.id.nav_add) {
                WeatherAddActivity bottomSheet = new WeatherAddActivity(newItem -> addItemToWeatherList(newItem));
                bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
                return true;
            }
            return false;
        });
    }
}
