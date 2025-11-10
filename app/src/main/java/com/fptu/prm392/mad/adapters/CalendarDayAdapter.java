package com.fptu.prm392.mad.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    private List<CalendarDay> days;
    private Map<Integer, Integer> taskCountMap; // day -> count
    private int selectedPosition = -1;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CalendarDayAdapter(OnDayClickListener listener) {
        this.days = new ArrayList<>();
        this.taskCountMap = new HashMap<>();
        this.listener = listener;
    }

    public void setDays(List<CalendarDay> days) {
        this.days = days;
        notifyDataSetChanged();
    }

    public void setTaskCounts(Map<Integer, Integer> taskCounts) {
        this.taskCountMap = taskCounts;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDayNumber;
        private View vTaskDot;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            vTaskDot = itemView.findViewById(R.id.vTaskDot);
        }

        public void bind(CalendarDay day, boolean isSelected) {
            if (day.isEmpty) {
                tvDayNumber.setText("");
                tvDayNumber.setBackgroundColor(Color.TRANSPARENT);
                vTaskDot.setVisibility(View.GONE);
                itemView.setClickable(false);
            } else {
                tvDayNumber.setText(String.valueOf(day.dayNumber));

                // Styling
                if (isSelected) {
                    tvDayNumber.setBackgroundResource(R.drawable.bg_fab_main);
                    tvDayNumber.setTextColor(Color.WHITE);
                    tvDayNumber.setTypeface(null, Typeface.BOLD);
                } else if (day.isToday) {
                    tvDayNumber.setBackgroundResource(R.drawable.bg_simple_fab_ripple);
                    tvDayNumber.setTextColor(Color.WHITE);
                    tvDayNumber.setTypeface(null, Typeface.BOLD);
                } else if (day.isCurrentMonth) {
                    tvDayNumber.setBackgroundColor(Color.TRANSPARENT);
                    tvDayNumber.setTextColor(Color.WHITE);
                    tvDayNumber.setTypeface(null, Typeface.BOLD);
                } else {
                    tvDayNumber.setBackgroundColor(Color.TRANSPARENT);
                    tvDayNumber.setTextColor(Color.parseColor("#80FFFFFF"));
                    tvDayNumber.setTypeface(null, Typeface.NORMAL);
                }

                // Show task dot if there are tasks
                Integer count = taskCountMap.get(day.dayNumber);
                if (count != null && count > 0) {
                    vTaskDot.setVisibility(View.VISIBLE);
                } else {
                    vTaskDot.setVisibility(View.GONE);
                }

                itemView.setClickable(true);
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDayClick(day);
                    }
                });
            }
        }
    }

    public static class CalendarDay {
        public int dayNumber;
        public boolean isCurrentMonth;
        public boolean isToday;
        public boolean isEmpty;
        public Calendar calendar;

        public CalendarDay(int dayNumber, boolean isCurrentMonth, boolean isToday, Calendar calendar) {
            this.dayNumber = dayNumber;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.isEmpty = false;
            this.calendar = calendar;
        }

        public CalendarDay() {
            this.isEmpty = true;
        }
    }
}

