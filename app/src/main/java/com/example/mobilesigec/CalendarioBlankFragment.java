package com.example.mobilesigec;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CalendarioBlankFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalendarioBlankFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CalendarioBlankFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CalendarioBlankFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CalendarioBlankFragment newInstance(String param1, String param2) {
        CalendarioBlankFragment fragment = new CalendarioBlankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_calendario_blank, container, false);

        GridLayout calendarGrid = view.findViewById(R.id.calendar_grid);
        populateCalendar(calendarGrid, inflater);

        // Set current date in details section
        TextView textCurrentDate = view.findViewById(R.id.text_current_date);
        SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));
        textCurrentDate.setText(sdf.format(new Date()));

        return view;
    }

    private void populateCalendar(GridLayout grid, LayoutInflater inflater) {
        // September 2026 starts on a Tuesday (Index 2 if Sunday is 0)
        int startOffset = 2;
        int daysInMonth = 30;
        float density = getResources().getDisplayMetrics().density;

        // Add empty spacers for the offset
        for (int i = 0; i < startOffset; i++) {
            View spacer = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (60 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            spacer.setLayoutParams(params);
            spacer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
            grid.addView(spacer);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            View dayView = inflater.inflate(R.layout.item_calendar_day, grid, false);
            TextView dayText = dayView.findViewById(R.id.day_text);
            View dayContainer = dayView.findViewById(R.id.day_container);
            ImageView dot1 = dayView.findViewById(R.id.dot1);
            ImageView dot2 = dayView.findViewById(R.id.dot2);
            ImageView dotWeekend = dayView.findViewById(R.id.dot_weekend);
            dayText.setText(String.valueOf(day));

            int dayOfWeek = (day + startOffset - 1) % 7;
            boolean isWeekend = (dayOfWeek == 0 || dayOfWeek == 6);

            if (isWeekend) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_weekend);
                dayText.setTextColor(Color.WHITE);
                dotWeekend.setVisibility(View.VISIBLE);
            } else {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
                dayText.setTextColor(Color.parseColor("#002F6C"));
            }

            // Highlights based on the image
            if (day == 1) {
                dot1.setVisibility(View.VISIBLE);
                dot1.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00")));
            } else if (day == 2) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                dot1.setVisibility(View.VISIBLE);
                dot1.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00")));
            } else if (day == 8) {
                dot1.setVisibility(View.VISIBLE);
                dot2.setVisibility(View.VISIBLE);
                dot1.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00")));
                dot2.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00")));
            } else if (day == 10) {
                dot1.setVisibility(View.VISIBLE);
                dot1.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00")));
            } else if (day == 14) {
                dot1.setVisibility(View.VISIBLE);
                dot1.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F57C00")));
            }

            grid.addView(dayView);
        }


        int totalCells = startOffset + daysInMonth;
        int remaining = (7 - (totalCells % 7)) % 7;
        for (int i = 0; i < remaining; i++) {
            View spacer = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (60 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            spacer.setLayoutParams(params);
            spacer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
            grid.addView(spacer);
        }
    }
}