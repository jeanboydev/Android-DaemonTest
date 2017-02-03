package com.jeanboy.app.daemontest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jeanboy.app.daemontest.service.CoreService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View view) {
        startService(new Intent(getApplicationContext(), CoreService.class));
    }

    public void stopService(View view) {
        CoreService.toStop();
    }
}
