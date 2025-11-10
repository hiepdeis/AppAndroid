package com.fptu.prm392.mad.Domains.Tasks.Interfaces;
import com.fptu.prm392.mad.Domains.Projects.Models.Task;
import java.util.List;
public interface TasksCallback {
    // Sẽ được gọi khi Firebase trả về dữ liệu thành công
    void onSuccess(List<Task> tasks);

    // Sẽ được gọi khi có lỗi xảy ra
    void onFailure(Exception e);
}
