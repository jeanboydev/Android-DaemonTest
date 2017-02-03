package com.jeanboy.app.daemontest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jeanboy.app.daemontest.service.CoreService;
import com.jeanboy.app.daemontest.service.KeepAliveService;

/**
 * Created by jeanboy on 2017/2/3.
 */

public class WakeUpReceiver extends BroadcastReceiver {

    private final static String TAG = WakeUpReceiver.class.getSimpleName();

    public final static String ACTION_CANCEL_KEEP_LIVE = "com.jeanboy.app.daemon.ACTION_CANCEL_KEEP_LIVE";//取消保活广播的action

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_CANCEL_KEEP_LIVE.equals(action)) {
            KeepAliveService.cancelService();
            Log.e(TAG, "cancel service!!");
            return;
        }
        context.startService(new Intent(context, CoreService.class));
    }

    public static class WakeUpAutoStartReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, CoreService.class));
        }
    }
}
