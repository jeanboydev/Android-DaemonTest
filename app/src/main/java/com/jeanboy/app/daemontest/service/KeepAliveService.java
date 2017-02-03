package com.jeanboy.app.daemontest.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jeanboy.app.daemontest.base.MainApplication;

/**
 * Created by jeanboy on 2017/2/3.
 */

public class KeepAliveService extends Service {

    private final static String TAG = KeepAliveService.class.getSimpleName();

    private final static int ALARM_INTERVAL = 5 * 60 * 1000;//定时唤醒的时间间隔，5分钟
    private final static int WAKE_REQUEST_CODE = 6666;

    private final static int SERVICE_ID = -1002;

    static PendingIntent sPendingIntent;

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
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onEnd(rootIntent);
    }

    /**
     * 守护服务，运行在:daemon子进程中
     */
    int onStart(Intent intent, int flags, int startId) {

        //启动前台服务而不显示通知的漏洞已在 API Level 25 修复
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            //利用漏洞在 API Level 17 及以下的 Android 系统中，启动前台服务而不显示通知
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                startForeground(KeepAliveService.SERVICE_ID, new Notification());//API < 18 ，此方法能有效隐藏Notification上的图标
            } else {
                //利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
                Intent innerIntent = new Intent(this, KeepLiveNotificationService.class);
                startService(innerIntent);
                startForeground(KeepAliveService.SERVICE_ID, new Notification());
            }
        }

        //定时检查 WorkService 是否在运行，如果不在运行就把它拉起来
        //Android 5.0+ 使用 JobScheduler，效果比 AlarmManager 好
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobInfo.Builder builder = new JobInfo.Builder(WAKE_REQUEST_CODE, new ComponentName(MainApplication.app, JobSchedulerService.class));
            builder.setPeriodic(ALARM_INTERVAL);
            //Android 7.0+ 增加了一项针对 JobScheduler 的新限制，最小间隔只能是下面设定的数字
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
            builder.setPersisted(true);
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        } else {
            //Android 4.4- 使用 AlarmManager
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(MainApplication.app, CoreService.class);
            sPendingIntent = PendingIntent.getService(MainApplication.app, WAKE_REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ALARM_INTERVAL, ALARM_INTERVAL, sPendingIntent);
        }

        //守护 Service 组件的启用状态, 使其不被 MAT 等工具禁用
        getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(), CoreService.class.getName()),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        return START_STICKY;
    }

    void onEnd(Intent rootIntent) {
        startService(new Intent(getApplicationContext(), CoreService.class));
        startService(new Intent(getApplicationContext(), KeepAliveService.class));
    }


    /**
     * 用于在不需要服务运行的时候取消 Job / Alarm / Subscription.
     * <p>
     * 因 WatchDogService 运行在 :daemon 子进程, 请勿在主进程中直接调用此方法.
     * 而是向 WakeUpReceiver 发送一个 Action 为 WakeUpReceiver.ACTION_CANCEL_KEEP_LIVE 的广播.
     */
    public static void cancelService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) MainApplication.app.getSystemService(JOB_SCHEDULER_SERVICE);
            scheduler.cancel(WAKE_REQUEST_CODE);
        } else {
            AlarmManager am = (AlarmManager) MainApplication.app.getSystemService(ALARM_SERVICE);
            if (sPendingIntent != null) am.cancel(sPendingIntent);
        }
    }

    public static class KeepLiveNotificationService extends Service {
        /**
         * 利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
         */
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.e(TAG, "onStartCommand");
            startForeground(KeepAliveService.SERVICE_ID, new Notification());
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
