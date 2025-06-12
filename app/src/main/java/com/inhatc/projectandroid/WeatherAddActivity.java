package com.inhatc.projectandroid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherAddActivity extends BottomSheetDialogFragment {

    public interface OnWeatherItemAddedListener {
        void onItemAdded(WeatherListItem newItem);
    }

    private OnWeatherItemAddedListener listener;
    private String selectedWeatherDescription = null;
    private boolean isWeatherIconSelected = false;
    private Button selectedTimeButton = null;
    private EditText editText;

    private final Map<Integer, String> weatherIconMap = new HashMap<Integer, String>() {{
        put(R.id.imageButton_weatherAdd_Icon_Sun, "clear sky");
        put(R.id.imageButton_weatherAdd_Icon_Cloud, "partly cloudy");
        put(R.id.imageButton_weatherAdd_Icon_Rain, "rain");
        put(R.id.imageButton_weatherAdd_Icon_Thunder, "thunderstorm");
        put(R.id.imageButton_weatherAdd_Icon_Show, "snow");
        put(R.id.imageButton_weatherAdd_Icon_SunCloud, "clouds");
    }};

    private final Map<String, String> errorMessageMap = new HashMap<String, String>() {{
        put("weather", "날씨 버튼을 선택해주세요.");
        put("time", "시간 버튼을 선택해주세요.");
        put("content", "내용을 입력해주세요.");
    }};

    public WeatherAddActivity(OnWeatherItemAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_weather_add, container, false);

        GridLayout gridWeather = view.findViewById(R.id.gridLayout_weatherAdd_selectWeather);
        editText = view.findViewById(R.id.edittext_weatherAdd_inputText);

        for (int i = 0; i < gridWeather.getChildCount(); i++) {
            View child = gridWeather.getChildAt(i);
            if (child instanceof ImageButton) {
                child.setOnClickListener(v -> {
                    selectedWeatherDescription = weatherIconMap.getOrDefault(v.getId(), "");
                    isWeatherIconSelected = true;
                    highlightSelectedWeatherButton((ImageButton) v, gridWeather);
                });
            }
        }

        List<Button> timeButtons = List.of(
                view.findViewById(R.id.button_weatherAdd_dayBefore),
                view.findViewById(R.id.button_weatherAdd_timeNow),
                view.findViewById(R.id.button_weatherAdd_allDay)
        );

        for (Button timeBtn : timeButtons) {
            timeBtn.setOnClickListener(v -> selectTimeButton(timeBtn, timeButtons));
        }

        Button saveButton = view.findViewById(R.id.button_weatherAdd_saveData);
        saveButton.setOnClickListener(v -> saveItem());

        return view;
    }

    private void selectTimeButton(Button button, List<Button> allButtons) {
        for (Button b : allButtons) {
            resetButtonStyle(b);
        }
        applyHighlightStyle(button);
        selectedTimeButton = button;
    }

    private void highlightSelectedWeatherButton(ImageButton selected, GridLayout grid) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child instanceof ImageButton) {
                resetButtonStyle(child);
            }
        }
        applyHighlightStyle(selected);
    }

    private void resetButtonStyle(View button) {
        button.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background));
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
    }

    private void applyHighlightStyle(View button) {
        button.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background));
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC));
    }

    private void saveItem() {
        String contents = editText.getText().toString();
        String selectedTime = selectedTimeButton != null ? selectedTimeButton.getText().toString() : "";

        if (!isWeatherIconSelected) {
            Toast.makeText(requireContext(), errorMessageMap.get("weather"), Toast.LENGTH_SHORT).show();
        } else if (selectedTimeButton == null) {
            Toast.makeText(requireContext(), errorMessageMap.get("time"), Toast.LENGTH_SHORT).show();
        } else if (contents.isEmpty()) {
            Toast.makeText(requireContext(), errorMessageMap.get("content"), Toast.LENGTH_SHORT).show();
        } else {
            WeatherListItem item = new WeatherListItem(0, contents, selectedWeatherDescription, selectedTime, false);
            if (listener != null) listener.onItemAdded(item);
            dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}
