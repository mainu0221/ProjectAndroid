package com.inhatc.projectandroid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeatherListAdapter extends RecyclerView.Adapter<WeatherListAdapter.WeatherListViewHolder> {

    public interface OnItemDeleteListener {
        void onDelete(long wNo);
    }

    private final List<WeatherListItem> items;
    private final OnItemDeleteListener deleteListener;

    public WeatherListAdapter(List<WeatherListItem> items, OnItemDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public WeatherListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weatherlist, parent, false);
        return new WeatherListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherListViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class WeatherListViewHolder extends RecyclerView.ViewHolder {
        private final TextView contents;
        private final ImageView weather;
        private final TextView time;
        private final Button deleteButton;

        public WeatherListViewHolder(@NonNull View itemView) {
            super(itemView);
            contents = itemView.findViewById(R.id.textview_weatherItem_text);
            weather = itemView.findViewById(R.id.imageview_weatherItem_selectedWeatherIcon);
            time = itemView.findViewById(R.id.textview_weatherItem_selectedTime);
            deleteButton = itemView.findViewById(R.id.button_weatherItem_delete);
        }

        public void bind(WeatherListItem item) {
            contents.setText(item.getContents());
            time.setText(item.getTime());
            weather.setImageResource(getWeatherIconId(item.getWeather()));
            itemView.setAlpha(item.isNotified() ? 0.5f : 1.0f);

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(item.getWNo());
                }
            });
        }

        private int getWeatherIconId(String weatherDescription) {
            switch (weatherDescription) {
                case "clear sky": return R.drawable.weather_sun_icon;
                case "partly cloudy": return R.drawable.weather_cloud_icon;
                case "rain": return R.drawable.weather_rain_icon;
                case "thunderstorm": return R.drawable.weather_thunder_icon;
                case "snow": return R.drawable.weather_snow_icon;
                case "clouds": return R.drawable.weather_suncloud_icon;
                default: return R.drawable.weather_sun_icon;
            }
        }
    }
}
