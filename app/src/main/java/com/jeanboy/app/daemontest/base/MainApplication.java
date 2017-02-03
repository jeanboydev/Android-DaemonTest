package com.jeanboy.app.daemontest.base;

import android.app.Application;
import android.content.Intent;

import com.jeanboy.app.daemontest.service.CoreService;

/**
 * Created by jeanboy on 2017/2/3.
 */

public class MainApplication extends Application {

    public static MainApplication app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;


        startService(new Intent(getApplicationContext(), CoreService.class));
    }
}
