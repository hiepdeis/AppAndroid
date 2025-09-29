package com.fptu.prm392.mad;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.logging.Logger;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        Logger.getGlobal().info("RegisterActivity instance - onCreate() -> Created ");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Logger.getGlobal().info("RegisterActivity instance - onStart() -> Started ");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Logger.getGlobal().info("RegisterActivity instance - onResume() -> Resumed/Running ");

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Logger.getGlobal().info("RegisterActivity instance - onPause() -> Paused ");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Logger.getGlobal().info("RegisterActivity instance - onStop() -> Stopped ");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Logger.getGlobal().info("RegisterActivity instance - onDestroy() -> Destroyed ");
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Logger.getGlobal().info("RegisterActivity instance - onRestart() -> Restarted ");
    }

    public void onBackPressed(View view)
    {
        finish();
    }
}