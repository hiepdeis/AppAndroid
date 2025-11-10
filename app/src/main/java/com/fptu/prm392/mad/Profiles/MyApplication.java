package com.fptu.prm392.mad.Profiles;
import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();

        // Cấu hình Cloudinary tại đây
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dnma6wea8"); // Tên cloud của bạn
        config.put("api_key", "112218869857264"); // API Key của bạn
        config.put("api_secret", "CgImoorLyB1AXhonm5moGm27TT0"); // API Secret của bạn
        MediaManager.init(this, config);
    }
}
