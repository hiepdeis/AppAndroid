package com.fptu.prm392.mad.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.CalendarDayAdapter;
import com.fptu.prm392.mad.adapters.TaskListAdapter;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private TextView tvCalendarMonth;
    private ImageView btnPrevMonth;
    private ImageView btnNextMonth;
    private RecyclerView rvCalendarDays;

    private TaskRepository taskRepository;
    private FirebaseAuth mAuth;
    private CalendarDayAdapter calendarAdapter;

    private Calendar currentCalendar;
    private Calendar selectedCalendar;
    private Calendar todayCalendar;
    private List<Task> allTasks;

    private OnTaskClickListener taskClickListener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public static CalendarFragment newInstance() {
        return new CalendarFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskRepository = new TaskRepository();
        mAuth = FirebaseAuth.getInstance();
        currentCalendar = Calendar.getInstance();
        selectedCalendar = Calendar.getInstance();
        todayCalendar = Calendar.getInstance();
        allTasks = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialize views
        tvCalendarMonth = view.findViewById(R.id.tvCalendarMonth);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvCalendarDays = view.findViewById(R.id.rvCalendarDays);

        // Setup calendar grid (7 columns for days of week)
        rvCalendarDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarDayAdapter(day -> {
            if (!day.isEmpty) {
                selectedCalendar = (Calendar) day.calendar.clone();
                showTasksForSelectedDay();
            }
        });
        rvCalendarDays.setAdapter(calendarAdapter);

        // Setup navigation buttons
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        // Load initial data
        loadTasks();

        return view;
    }

    private void updateCalendar() {
        // Update month/year display
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCalendarMonth.setText(monthFormat.format(currentCalendar.getTime()));

        // Generate calendar days
        generateCalendarDays();
    }

    private void generateCalendarDays() {
        List<CalendarDayAdapter.CalendarDay> days = new ArrayList<>();

        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        // Get first day of week (0 = Sunday, 1 = Monday, etc.)
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        // Add empty days for previous month
        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(new CalendarDayAdapter.CalendarDay());
        }

        // Add days of current month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);

            boolean isToday = calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                            calendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                            calendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH);

            Calendar dayCal = (Calendar) calendar.clone();
            days.add(new CalendarDayAdapter.CalendarDay(day, true, isToday, dayCal));
        }

        calendarAdapter.setDays(days);
        updateTaskCounts();
    }

    private void loadTasks() {
        if (mAuth.getCurrentUser() == null) return;

        taskRepository.getMyTasks(
            tasks -> {
                allTasks = tasks;
                updateCalendar();
            },
            e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading tasks: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void updateTaskCounts() {
        if (allTasks == null || allTasks.isEmpty()) {
            return;
        }

        Map<Integer, Integer> taskCounts = new HashMap<>();
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        for (Task task : allTasks) {
            // Skip tasks that are already done
            if ("done".equalsIgnoreCase(task.getStatus())) {
                continue;
            }

            if (task.getDueDate() != null) {
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTime(task.getDueDate().toDate());

                if (taskCal.get(Calendar.MONTH) == currentMonth &&
                    taskCal.get(Calendar.YEAR) == currentYear) {
                    int day = taskCal.get(Calendar.DAY_OF_MONTH);
                    taskCounts.put(day, taskCounts.getOrDefault(day, 0) + 1);
                }
            }
        }

        calendarAdapter.setTaskCounts(taskCounts);
    }

    private void showTasksForSelectedDay() {
        // Get tasks for selected day
        List<Task> dayTasks = getTasksForDay(selectedCalendar);

        if (dayTasks.isEmpty()) {
            if (getContext() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                Toast.makeText(getContext(),
                    "No tasks on " + dateFormat.format(selectedCalendar.getTime()),
                    Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show dialog with task list
            showDayTasksDialog(dayTasks);
        }
    }

    private void showDayTasksDialog(List<Task> tasks) {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_day_tasks);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Find views
        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        ImageView btnCloseDialog = dialog.findViewById(R.id.btnCloseDialog);
        RecyclerView rvDayTasks = dialog.findViewById(R.id.rvDayTasks);
        LinearLayout emptyStateDayTasks = dialog.findViewById(R.id.emptyStateDayTasks);

        // Set title with date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        tvDialogTitle.setText("Tasks for " + dateFormat.format(selectedCalendar.getTime()));

        // Setup RecyclerView
        rvDayTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        TaskListAdapter taskAdapter = new TaskListAdapter(task -> {
            // Close dialog and open task detail
            dialog.dismiss();
            if (taskClickListener != null) {
                taskClickListener.onTaskClick(task);
            }
        });
        rvDayTasks.setAdapter(taskAdapter);

        // Display tasks
        if (tasks.isEmpty()) {
            rvDayTasks.setVisibility(View.GONE);
            emptyStateDayTasks.setVisibility(View.VISIBLE);
        } else {
            rvDayTasks.setVisibility(View.VISIBLE);
            emptyStateDayTasks.setVisibility(View.GONE);
            taskAdapter.setTasks(tasks);
        }

        // Close button
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private List<Task> getTasksForDay(Calendar day) {
        List<Task> dayTasks = new ArrayList<>();

        Calendar startOfDay = (Calendar) day.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) day.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        for (Task task : allTasks) {
            // Skip tasks that are already done
            if ("done".equalsIgnoreCase(task.getStatus())) {
                continue;
            }

            if (task.getDueDate() != null) {
                Date dueDate = task.getDueDate().toDate();
                if (dueDate.getTime() >= startOfDay.getTimeInMillis() &&
                    dueDate.getTime() <= endOfDay.getTimeInMillis()) {
                    dayTasks.add(task);
                }
            }
        }

        return dayTasks;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.taskClickListener = listener;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }
}

