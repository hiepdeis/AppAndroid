//package com.fptu.prm392.mad.Domains.Tasks.Activities; // (Kiểm tra lại package của bạn)
//
//import android.graphics.Color;
//import android.os.Bundle;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.fptu.prm392.mad.Domains.Projects.Models.Task;
//import com.fptu.prm392.mad.Domains.Projects.Adapters.TaskAdapter; // Import Adapter
//import com.fptu.prm392.mad.Domains.Tasks.Repo.TaskRepository;
//import com.fptu.prm392.mad.Domains.Tasks.Interfaces.TasksCallback;
//import com.fptu.prm392.mad.R;
//import com.google.firebase.Timestamp;
//import com.prolificinteractive.materialcalendarview.CalendarDay;
//import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
//import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
//import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//
//public class CalendarActivity extends AppCompatActivity {
//
//    private MaterialCalendarView calendarView;
//    private RecyclerView rvTasks;
//    private TextView tvSelectedDateTasks;
//    private TaskAdapter taskAdapter;
//    private TaskRepository taskRepository;
//
//    private String currentUserId;
//
//    // Dùng để lưu trữ dữ liệu của tháng đang xem
//    // Key: Ngày (CalendarDay), Value: Danh sách Task của ngày đó
//    private Map<CalendarDay, List<Task>> taskMap = new HashMap<>();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_calendar); // <-- Dùng layout XML của bạn
//
//        // 1. Lấy userId từ HomeActivity
//        currentUserId = getIntent().getStringExtra("USER_ID");
//        if (currentUserId == null || currentUserId.isEmpty()) {
//            Toast.makeText(this, "Lỗi: Không có thông tin người dùng", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
//
//        // 2. Khởi tạo
//        taskRepository = new TaskRepository();
//
//        // Ánh xạ các view từ file XML của bạn
//        calendarView = findViewById(R.id.calendarView);
//        rvTasks = findViewById(R.id.rvTasks);
//        tvSelectedDateTasks = findViewById(R.id.tvSelectedDateTasks);
//        // KHÔNG có tvMonth
//
//        // 3. Cấu hình RecyclerView
//        taskAdapter = new TaskAdapter(); // (Bạn cần tạo file TaskAdapter.java)
//        rvTasks.setLayoutManager(new LinearLayoutManager(this));
//        rvTasks.setAdapter(taskAdapter);
//
//        // 4. Tải dữ liệu lần đầu cho tháng hiện tại
//        CalendarDay today = CalendarDay.today();
//        fetchTasksForMonth(today.getYear(), today.getMonth());
//
//        // 5. Lắng nghe sự kiện VUỐT THÁNG (Luồng Bước 3)
//        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
//            @Override
//            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
//                // Chú ý: Thư viện này month là 1-12
//                fetchTasksForMonth(date.getYear(), date.getMonth());
//            }
//        });
//
//        // 6. Lắng nghe sự kiện CLICK NGÀY (Luồng Bước 4/6)
//        calendarView.setOnDateSelectedListener(new OnDateSelectedListener() {
//            @Override
//            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
//                updateRecyclerViewForDate(date);
//            }
//        });
//    }
//
//    /**
//     * Bước 3: Hàm fetch dữ liệu theo tháng
//     */
//    private void fetchTasksForMonth(int year, int month) {
//        // (Hiển thị ProgressBar ở đây)
//        tvSelectedDateTasks.setText("Đang tải dữ liệu tháng " + month + "...");
//
//        // 1. Tính toán ngày bắt đầu và kết thúc tháng
//        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
//        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
//
//        // Chuyển sang Timestamp của Firebase
//        Timestamp startOfMonth = new Timestamp(Date.from(firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant()));
//        Timestamp endOfMonth = new Timestamp(Date.from(lastDayOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()));
//
//        // 2. Gọi hàm Repository đã tối ưu
//        taskRepository.getTasksByUserIdAndMonth(currentUserId, startOfMonth, endOfMonth, new TasksCallback() {
//            @Override
//            public void onSuccess(List<Task> tasks) {
//                // 3. Xử lý dữ liệu khi thành công
//                runOnUiThread(() -> processTasks(tasks));
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//                // 4. Xử lý khi thất bại
//                runOnUiThread(() -> {
//                    Toast.makeText(CalendarActivity.this, "Lỗi tải task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    // (Ẩn ProgressBar ở đây)
//                });
//            }
//        });
//    }
//
//    /**
//     * Bước 4: Xử lý dữ liệu trả về (Group task và Vẽ dấu chấm)
//     */
//    private void processTasks(List<Task> tasks) {
//        // Xóa dữ liệu cũ
//        taskMap.clear();
//        HashSet<CalendarDay> taskDates = new HashSet<>();
//
//        // Group task theo ngày (Tạo Map)
//        for (Task task : tasks) {
//            if (task.getDueDate() != null) {
//                // Chuyển Timestamp (Firebase) -> CalendarDay (MaterialCalendar)
//                CalendarDay day = CalendarDay.from(
//                        task.getDueDate().toDate().toInstant()
//                                .atZone(ZoneId.systemDefault())
//                                .toLocalDate()
//                );
//
//                taskDates.add(day); // Thêm ngày này vào danh sách cần đánh dấu
//
//                // Thêm task vào Map
//                List<Task> tasksForDay = taskMap.get(day);
//                if (tasksForDay == null) {
//                    tasksForDay = new ArrayList<>();
//                    taskMap.put(day, tasksForDay);
//                }
//                tasksForDay.add(task);
//            }
//        }
//
//        // Bước 5: Vẽ dấu chấm lên Calendar
//        calendarView.clearDecorators(); // Xóa dấu chấm cũ
//        // (Bạn cần tạo file EventDecorator.java)
//        calendarView.addDecorator(new EventDecorator(Color.RED, taskDates));
//
//        // Cập nhật UI
//        tvSelectedDateTasks.setText("Tasks cho ngày đã chọn");
//        // (Ẩn ProgressBar ở đây)
//
//        // Tự động cập nhật list cho ngày đang chọn (nếu có)
//        CalendarDay selectedDay = calendarView.getSelectedDate();
//        if (selectedDay != null) {
//            updateRecyclerViewForDate(selectedDay);
//        } else {
//            taskAdapter.setTasks(null); // Xóa list nếu không chọn ngày nào
//        }
//    }
//
//    /**
//     * Bước 6: Cập nhật RecyclerView khi click vào 1 ngày
//     */
//    private void updateRecyclerViewForDate(CalendarDay date) {
//        // Lấy danh sách task từ Map đã lưu
//        List<Task> tasksForSelectedDay = taskMap.get(date);
//
//        if (tasksForSelectedDay != null && !tasksForSelectedDay.isEmpty()) {
//            tvSelectedDateTasks.setText("Tasks cho ngày " + date.getDay() + "/" + date.getMonth() + "/" + date.getYear());
//            taskAdapter.setTasks(tasksForSelectedDay);
//        } else {
//            tvSelectedDateTasks.setText("Không có task cho ngày " + date.getDay() + "/" + date.getMonth() + "/" + date.getYear());
//            taskAdapter.setTasks(null); // Gửi list rỗng
//        }
//    }
//}