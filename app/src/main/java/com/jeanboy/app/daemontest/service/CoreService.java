package com.jeanboy.app.daemontest.service;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jeanboy.app.daemontest.base.MainApplication;
import com.jeanboy.app.daemontest.receiver.WakeUpReceiver;

/**
 * Created by jeanboy on 2017/2/3.
 */

public class CoreService extends Service {

    private final static String TAG = CoreService.class.getSimpleName();

    private final static int SERVICE_ID = -1001;

    public static boolean isShouldStopService = false;

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        return onStart(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        onEnd(null);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        onStart(intent, 0, 0);
        return null;
    }

    /**
     * 最近任务列表中划掉卡片时回调
     *
     * @param rootIntent
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onEnd(rootIntent);
    }

    int onStart(Intent intent, int flags, int startId) {

        //启动前台服务而不显示通知的漏洞已在 API Level 25 修复
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            //利用漏洞在 API Level 17 及以下的 Android 系统中，启动前台服务而不显示通知
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                startForeground(CoreService.SERVICE_ID, new Notification());//API < 18 ，此方法能有效隐藏Notification上的图标
            } else {
                //利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
                Intent innerIntent = new Intent(this, CoreNotificationService.class);
                startService(innerIntent);
                startForeground(CoreService.SERVICE_ID, new Notification());
            }
        }

        //启动守护服务，运行在:daemon子进程中
        startService(new Intent(getApplicationContext(), KeepLiveService.class));

        //----------业务逻辑----------

        //...
        if (isShouldStopService) toStop();
        else toStart();

        //----------业务逻辑----------

        //守护 Service 组件的启用状态, 使其不被 MAT 等工具禁用
        getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(), KeepLiveService.class.getName()),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        /**
         * 利用系统Service机制拉活
         * 将 Service 设置为 START_STICKY
         * 适用范围:
         *      1.Service 第一次被异常杀死后会在5秒内重启，第二次被杀死会在10秒内重启，第三次会在20秒内重启，
         *        一旦在短时间内 Service 被杀死达到5次，则系统不再拉起。
         *      2.进程被取得 Root 权限的管理工具或系统工具通过 forestop 停止掉，无法重启。
         */
        return START_STICKY;
    }

    /**
     * 设置服务终止时启动
     *
     * @param rootIntent
     */
    void onEnd(Intent rootIntent) {
        System.out.println("保存数据到磁盘。");
        startService(new Intent(getApplicationContext(), CoreService.class));
        startService(new Intent(getApplicationContext(), KeepLiveService.class));
    }

    /**
     * 开启服务
     */
    static void toStart() {
        if (isShouldStopService) return;

    }

    /**
     * 停止服务
     */
    public static void toStop() {
        isShouldStopService = true;
        MainApplication.app.sendBroadcast(new Intent(WakeUpReceiver.ACTION_CANCEL_KEEP_LIVE));
    }

    public static class CoreNotificationService extends Service {
        /**
         * 利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
         */
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.e(TAG, "onStartCommand");
            startForeground(CoreService.SERVICE_ID, new Notification());
            stopSelf();
            return START_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
