package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
          setContentView(R.layout.activity_main);
        Logger.getGlobal().info("MainActivity instance - onCreate() -> Created ");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Logger.getGlobal().info("MainActivity instance - onStart() -> Started ");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Logger.getGlobal().info("MainActivity instance - onResume() -> Resumed/Running ");

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Logger.getGlobal().info("MainActivity instance - onPause() -> Paused ");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Logger.getGlobal().info("MainActivity instance - onStop() -> Stopped ");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Logger.getGlobal().info("MainActivity instance - onDestroy() -> Destroyed ");
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Logger.getGlobal().info("MainActivity instance - onRestart() -> Restarted ");
    }

    public void onRegisterClick(View view)
    {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void onClose(View view)
    {
        finish();
    }
}