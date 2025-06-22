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

// RecyclerView 어댑터 클래스 - 저장된 날씨 알림 항목들을 표시
public class WeatherListAdapter extends RecyclerView.Adapter<WeatherListAdapter.WeatherListViewHolder> {

    // 삭제 이벤트를 처리하기 위한 콜백 인터페이스
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

    // 데이터 바인딩
    @Override
    public void onBindViewHolder(@NonNull WeatherListViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    // 전체 아이템 개수 반환
    @Override
    public int getItemCount() {
        return items.size();
    }

    // 각 항목의 ViewHolder 클래스
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

        // ViewHolder에 데이터 바인딩 수행
        public void bind(WeatherListItem item) {
            contents.setText(item.getContents());
            time.setText(item.getTime());
            // 날씨 설명에 따라 아이콘 변경
            weather.setImageResource(getWeatherIconId(item.getWeather()));
            // 이미 알림이 전송된 항목은 반투명 처리
            itemView.setAlpha(item.isNotified() ? 0.5f : 1.0f);

            // 삭제 버튼 클릭 시 콜백 실행
            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(item.getWNo());
                }
            });
        }

        // 날씨 설명 문자열에 따라 아이콘 리소스 ID 반환
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
