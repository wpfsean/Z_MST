package com.tehike.client.mst.app.project.base;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.cmscallbacks.TimingCheckSipStatus;
import com.tehike.client.mst.app.project.cmscallbacks.TimingRequestCmsVideoDataService;
import com.tehike.client.mst.app.project.cmscallbacks.TimingRequestCmsSipDataService;
import com.tehike.client.mst.app.project.cmscallbacks.TimingSendHbService;
import com.tehike.client.mst.app.project.execption.Cockroach;
import com.tehike.client.mst.app.project.execption.CrashLog;
import com.tehike.client.mst.app.project.execption.ExceptionHandler;
import com.tehike.client.mst.app.project.receiver.CpuAndRamUtils;
import com.tehike.client.mst.app.project.services.BatteryAndWifiService;
import com.tehike.client.mst.app.project.services.ReceiverEmergencyAlarmService;
import com.tehike.client.mst.app.project.services.ReceiverServerAlarmService;
import com.tehike.client.mst.app.project.services.RemoteVoiceService;
import com.tehike.client.mst.app.project.services.ServiceUtils;
import com.tehike.client.mst.app.project.utils.ActivityUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.ToastUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * 描述：Application全局配置
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2018/10/8 10:34
 * @version V1.0
 */

public class App extends Application {
    /**
     * 上下文
     */
    private static App mContext;

    /**
     * 线程池
     */
    public static ExecutorService mExecutorService = null;

    /**
     * Volly请求队列
     */
    public static RequestQueue mRequestQueue = null;


    int threadCount = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        //获取最大的可用线程数
        threadCount = Runtime.getRuntime().availableProcessors();
        Logutils.i("线程最大数量："+threadCount);

        //初始化采集崩溃的信息
        CrashHandler.getInstance().init(this);

        install();


        if (mExecutorService == null) {
            //线程池内运行程序可执行的最大线程数
            mExecutorService = Executors.newFixedThreadPool(threadCount);
        }

        if (mRequestQueue == null) {
            //实例请求队列
            mRequestQueue = Volley.newRequestQueue(this);
        }

        //启动cpu和ram监听
        CpuAndRamUtils.getInstance().init(getApplicationContext(), 10 * 1000L);
        CpuAndRamUtils.getInstance().start();

        //启动电量和信号监听
        if (!ServiceUtils.isServiceRunning(BatteryAndWifiService.class))
            ServiceUtils.startService(BatteryAndWifiService.class);

    }

    private void install() {
        final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();
       // final Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        Cockroach.install(new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, final Throwable throwable) {
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", throwable);
                CrashLog.saveCrashLog(mContext, throwable);
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        toast.setText("捕获到导致崩溃的异常" + "\n" + throwable.getMessage());
//                        toast.show();
//                    }
//                });
                Logutils.e(throwable.getMessage());
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                throwable.printStackTrace();//打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
//                toast.setText("阻止！");
//                toast.show();
                ToastUtils.showShort("Arrest!");
            }

            @Override
            protected void onEnterSafeMode() {

            }

            @Override
            protected void onMayBeBlackScreen(Throwable e) {
                Thread thread = Looper.getMainLooper().getThread();
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", e);
                //黑屏时建议直接杀死app
                sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
            }

        });
    }


    //全局上下文
    public static App getApplication() {
        return mContext;
    }

    //线程池
    public static ExecutorService getExecutorService() {
        return mExecutorService;
    }

    //Volly队列
    public static RequestQueue getQuest() {
        return mRequestQueue;
    }



    //退出
    public static void exit() {
        //移除栈中所有的activiry
        ActivityUtils.removeAllActivity();

        //关闭电量监听服务
        if (ServiceUtils.isServiceRunning(BatteryAndWifiService.class))
            ServiceUtils.stopService(BatteryAndWifiService.class);

        //关闭被动远程喊话的服务
        if (ServiceUtils.isServiceRunning(RemoteVoiceService.class)){
            ServiceUtils.stopService(RemoteVoiceService.class);
            Logutils.i("关闭远程喊话服务");
        }


        //关闭服务器接收报警的服务
        if (ServiceUtils.isServiceRunning(ReceiverServerAlarmService.class))
            ServiceUtils.stopService(ReceiverServerAlarmService.class);

        //关闭接收友邻哨报警的服务
        if (ServiceUtils.isServiceRunning(ReceiverEmergencyAlarmService.class))
            ServiceUtils.stopService(ReceiverEmergencyAlarmService.class);

        //关闭发送心跳的服务
        if (ServiceUtils.isServiceRunning(TimingSendHbService.class))
            ServiceUtils.stopService(TimingSendHbService.class);

        //关闭定时请求sip数据的服务
        if (ServiceUtils.isServiceRunning(TimingRequestCmsSipDataService.class))
            ServiceUtils.stopService(TimingRequestCmsSipDataService.class);

        //关闭定时请求video数据的服务
        if (ServiceUtils.isServiceRunning(TimingRequestCmsVideoDataService.class))
            ServiceUtils.stopService(TimingRequestCmsVideoDataService.class);

        if (ServiceUtils.isServiceRunning(TimingCheckSipStatus.class))
            ServiceUtils.stopService(TimingCheckSipStatus.class);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Logutils.i("程序终止");
    }
}
