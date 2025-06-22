package com.inhatc.projectandroid;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 선택된 날짜의 메모를 확인하고 삭제할 수 있는 BottomSheet 다이얼로그
public class MainDateInfoActivity extends BottomSheetDialogFragment {

    private final String selectedDate;
    private TextView editText;
    private TextView textView;
    private Button deleteButton;
    private LocalDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 생성자에서 선택 날짜를 받아 저장
    public MainDateInfoActivity(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_main_checkanddelete, container, false);

        editText = view.findViewById(R.id.textview_mainCheck_input);
        textView = view.findViewById(R.id.textview_mainCheck_selectedDate);
        deleteButton = view.findViewById(R.id.button_mainAdd_deleteData);

        db = LocalDatabase.getDatabase(requireContext());

        loadData();

        deleteButton.setOnClickListener(v -> deleteData());

        return view;
    }

    // 선택한 날짜에 해당하는 일정 데이터를 DB에서 불러와 표시
    private void loadData() {
        textView.setText(selectedDate);

        executor.execute(() -> {
            // DB에서 동기적으로 데이터 가져오기
            String schedule = db.getDailyScheduleDao().getDailyScheduleInfoSync(selectedDate);

            // UI 쓰레드에서 결과 반영
            requireActivity().runOnUiThread(() -> {
                if (!TextUtils.isEmpty(schedule)) {
                    editText.setText(schedule);
                } else {
                    editText.setText("");
                    Toast.makeText(requireContext(), "해당 날짜에 저장된 일정이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // DB에서 해당 날짜의 일정을 삭제하고 UI 및 메인 액티비티에 반영
    private void deleteData() {
        executor.execute(() -> {
            // DB에서 삭제 실행
            db.getDailyScheduleDao().deleteDailyScheduleInfo(selectedDate);

            // UI 반영
            requireActivity().runOnUiThread(() -> {
                // 결과 전달 MainActivity에서 캘린더 갱신
                Bundle result = new Bundle();
                result.putString("deletedDate", selectedDate);
                requireActivity().getSupportFragmentManager().setFragmentResult("memoDeleted", result);

                // UI 초기화 및 알림
                editText.setText("");
                Toast.makeText(requireContext(), "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        });
    }
}
