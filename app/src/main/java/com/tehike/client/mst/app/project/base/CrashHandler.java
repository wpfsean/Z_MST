package com.tehike.client.mst.app.project.base;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.ui.landactivity.LandMainActivity;
import com.tehike.client.mst.app.project.ui.portactivity.PortMainActivity;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 手机端出现崩溃时捕捉异常
 * <p>
 * Created by Root on 2018/9/27.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    //
    private static CrashHandler sInstance = null;

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    //上下文
    private Context mContext;

    // 保存信息的集合
    private Map<String, String> mMessage = new HashMap<>();

    //单例
    public static CrashHandler getInstance() {
        if (sInstance == null) {
            synchronized (CrashHandler.class) {
                if (sInstance == null) {
                    synchronized (CrashHandler.class) {
                        sInstance = new CrashHandler();
                    }
                }
            }
        }
        return sInstance;
    }

    //私有化构造方法
    private CrashHandler() {
    }

    /**
     * 初始化默认异常捕获
     *
     * @param context context
     */
    public void init(Context context) {
        mContext = context;
        // 获取默认异常处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 将此类设为默认异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }


    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!handleException(e)) {
            // 未经过人为处理,则调用系统默认处理异常,弹出系统强制关闭的对话框
            if (mDefaultHandler != null) {
                mDefaultHandler.uncaughtException(t, e);
            }
        } else {
            // 已经人为处理,系统自己退出
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e1) {
//                e1.printStackTrace();
//            }
            App.exit();
//            mContext.startActivity(mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName()));
            restart();
            Process.killProcess(Process.myPid());
            //  System.exit(1);
        }
    }

    private void restart() {
        Intent intent = null;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            android.util.Log.e("TAG", "error : " + e);
        }
        if (AppConfig.APP_DIRECTION == 1){

            intent = new Intent(
                    App.getApplication(),
                    PortMainActivity.class);

        }else if (AppConfig.APP_DIRECTION == 2)
        {
            intent = new Intent(
                    App.getApplication(),
                    LandMainActivity.class);
        }
        @SuppressLint("WrongConstant") PendingIntent restartIntent = PendingIntent.getActivity(
                App.getApplication(), 0, intent,
                Intent.FLAG_ACTIVITY_NEW_TASK);

        //退出程序
        AlarmManager mgr = (AlarmManager) App.getApplication().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
                restartIntent); // 1秒钟后重启应用
    }


    //是否人为的处理
    private boolean handleException(Throwable e) {
        if (e == null) {
            return false;
        }
        new Thread() {// 在主线程中弹出提示
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "程序开小差了呢..", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();
        collectErrorMessages();
        saveErrorMessages(e);
        return true;
    }

    /**
     * 收集信息
     */
    private void collectErrorMessages() {

        // 通过反射拿到错误信息
        Field[] fields = Build.class.getFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    mMessage.put(field.getName(), field.get(null).toString());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 保存信息
     */
    private void saveErrorMessages(Throwable e) {

        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        e.printStackTrace(pw);
        Throwable cause = e.getCause();
        // 循环取出Cause
        while (cause != null) {
            cause.printStackTrace(pw);
            cause = e.getCause();
        }
        pw.close();
        String result = writer.toString();
        Log.e("TAG", "sb:" + result);
        WriteLogToFile.info("App崩溃异常:" + result);

        //上传错误信息
//        new HttpUtils("http://19.0.0.28/RecordTheNumForData/a.php?paramater=" + result+"&ip=19.0.0.88", new HttpUtils.GetHttpData() {
//            @Override
//            public void httpData(String result) {
//
//                Log.i("TAG","result:"+result);
//            }
//        }).start();
    }
}

