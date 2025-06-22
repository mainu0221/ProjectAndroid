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

// 날씨 알림 항목을 추가하는 클래스
public class WeatherAddActivity extends BottomSheetDialogFragment {

    // 아이템이 저장되었을 때 WeatherActivity로 전달하기 위한 콜백 인터페이스
    public interface OnWeatherItemAddedListener {
        void onItemAdded(WeatherListItem newItem);
    }

    private OnWeatherItemAddedListener listener;
    private String selectedWeatherDescription = null;
    private boolean isWeatherIconSelected = false;
    private Button selectedTimeButton = null;
    private EditText editText;

    // 날씨 아이콘 ID ↔ 설명 매핑
    private final Map<Integer, String> weatherIconMap = new HashMap<Integer, String>() {{
        put(R.id.imageButton_weatherAdd_Icon_Sun, "clear sky");
        put(R.id.imageButton_weatherAdd_Icon_Cloud, "partly cloudy");
        put(R.id.imageButton_weatherAdd_Icon_Rain, "rain");
        put(R.id.imageButton_weatherAdd_Icon_Thunder, "thunderstorm");
        put(R.id.imageButton_weatherAdd_Icon_Show, "snow");
        put(R.id.imageButton_weatherAdd_Icon_SunCloud, "clouds");
    }};

    // 에러 메시지 매핑
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

        // 날씨 아이콘 버튼 클릭 처리
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

        // 시간 선택 버튼 처리
        List<Button> timeButtons = List.of(
                view.findViewById(R.id.button_weatherAdd_dayBefore),
                view.findViewById(R.id.button_weatherAdd_timeNow),
                view.findViewById(R.id.button_weatherAdd_allDay)
        );

        for (Button timeBtn : timeButtons) {
            timeBtn.setOnClickListener(v -> selectTimeButton(timeBtn, timeButtons));
        }

        // 저장 버튼 클릭 처리
        Button saveButton = view.findViewById(R.id.button_weatherAdd_saveData);
        saveButton.setOnClickListener(v -> saveItem());

        return view;
    }

    // 시간 버튼을 선택한 경우 스타일 처리 및 선택 상태 반영
    private void selectTimeButton(Button button, List<Button> allButtons) {
        for (Button b : allButtons) {
            resetButtonStyle(b);
        }
        applyHighlightStyle(button);
        selectedTimeButton = button;
    }

    // 날씨 아이콘 버튼 선택 시 스타일 처리 (하이라이트)
    private void highlightSelectedWeatherButton(ImageButton selected, GridLayout grid) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child instanceof ImageButton) {
                resetButtonStyle(child);
            }
        }
        applyHighlightStyle(selected);
    }

    // 버튼 스타일 초기화 (선택 해제 상태)
    private void resetButtonStyle(View button) {
        button.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background));
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
    }

    // 선택된 버튼 강조 스타일 적용
    private void applyHighlightStyle(View button) {
        button.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background));
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC));
    }

    // 저장 버튼 클릭 시 입력값 유효성 검사를 거쳐 WeatherListItem을 생성 후 콜백 전달
    private void saveItem() {
        String contents = editText.getText().toString();
        String selectedTime = selectedTimeButton != null ? selectedTimeButton.getText().toString() : "";

        // 필수 항목 유효성 검사
        if (!isWeatherIconSelected) {
            Toast.makeText(requireContext(), errorMessageMap.get("weather"), Toast.LENGTH_SHORT).show();
        } else if (selectedTimeButton == null) {
            Toast.makeText(requireContext(), errorMessageMap.get("time"), Toast.LENGTH_SHORT).show();
        } else if (contents.isEmpty()) {
            Toast.makeText(requireContext(), errorMessageMap.get("content"), Toast.LENGTH_SHORT).show();
        } else {
            // WeatherListItem 객체 생성 및 콜백 호출
            WeatherListItem item = new WeatherListItem(0, contents, selectedWeatherDescription, selectedTime, false);
            if (listener != null) listener.onItemAdded(item);
            dismiss();
        }
    }

    // 뷰가 파괴될 때 리스너 참조 해제
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}
