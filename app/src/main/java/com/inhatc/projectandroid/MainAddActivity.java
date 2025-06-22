package com.inhatc.projectandroid;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//메모(일정) 추가를 위한 BottomSheet 다이얼로그 클래스
public class MainAddActivity extends BottomSheetDialogFragment {

    private EditText editText;
    private TextView selectedDateTextView;
    private LocalDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_main_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // DB 객체 초기화
        db = LocalDatabase.getDatabase(requireContext());
        setupViewAndButton(view);
    }

    // 레이아웃 내 버튼 및 입력 필드 초기화 + 이벤트 설정
    private void setupViewAndButton(View view) {
        editText = view.findViewById(R.id.edittext_mainAdd_input);
        selectedDateTextView = view.findViewById(R.id.textview_mainAdd_printDate);

        // 날짜 선택 버튼 클릭 시 DatePicker 표시
        view.findViewById(R.id.button_mainAdd_selectDate).setOnClickListener(v -> showDatePickerDialog());

        // 저장 버튼 클릭 시 데이터 저장 시도
        view.findViewById(R.id.button_mainAdd_saveData).setOnClickListener(v -> saveData());
    }

    // 날짜 선택 다이얼로그 생성 및 처리
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    selectedDateTextView.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    // 입력 값 유효성 검사 후 저장 요청
    private void saveData() {
        String info = editText.getText().toString();
        String selectedDate = selectedDateTextView.getText().toString();

        // 둘 다 입력되어 있으면 DB에 저장
        if (!info.isEmpty() && !selectedDate.equals("선택된 날짜 없음")) {
            saveToDatabase(selectedDate, info);
            Bundle result = new Bundle();
            result.putString("addedDate", selectedDate);
            requireActivity().getSupportFragmentManager().setFragmentResult("memoAdded", result);
            dismiss();
        } else {
            // 필드가 비어있으면 경고 메시지 표시
            Toast.makeText(requireContext(), "내용과 날짜를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    // Room 데이터베이스에 일정 정보 저장
    private void saveToDatabase(String selectedDate, String info) {
        DailySchedule scheduleInfo = new DailySchedule(selectedDate, info);
        // 단일 스레드를 통해 Room 작업 실행
        executor.execute(() -> {
            try {
                db.getDailyScheduleDao().insertDailySchedule(scheduleInfo);
                Log.d("MainAddActivity", "insert ok: " + selectedDate + ", : " + info);
            } catch (Exception e) {
                Log.e("MainAddActivity", "Failed to save DailySchedule: " + e.getMessage());
            }
        });
    }
}
