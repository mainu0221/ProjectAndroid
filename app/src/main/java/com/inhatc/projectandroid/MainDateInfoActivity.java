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

public class MainDateInfoActivity extends BottomSheetDialogFragment {

    private final String selectedDate;
    private TextView editText;
    private TextView textView;
    private Button deleteButton;
    private LocalDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

    private void loadData() {
        textView.setText(selectedDate);

        executor.execute(() -> {
            String schedule = db.getDailyScheduleDao().getDailyScheduleInfoSync(selectedDate);

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

    private void deleteData() {
        executor.execute(() -> {
            db.getDailyScheduleDao().deleteDailyScheduleInfo(selectedDate);

            requireActivity().runOnUiThread(() -> {
                Bundle result = new Bundle();
                result.putString("deletedDate", selectedDate);
                requireActivity().getSupportFragmentManager().setFragmentResult("memoDeleted", result);

                editText.setText("");
                Toast.makeText(requireContext(), "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        });
    }
}
