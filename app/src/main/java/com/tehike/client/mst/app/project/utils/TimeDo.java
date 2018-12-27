package com.tehike.client.mst.app.project.utils;

import android.content.Context;
import android.text.TextUtils;

import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.global.AppConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Root on 2018/7/23.
 */

public class TimeDo implements Runnable {


    Callback callback;

    private volatile static TimeDo instance = null;
    private ScheduledExecutorService mScheduledExecutorService;
    private long time;

    private TimeDo() {

        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }


    public void setListern(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {

        String serverIp = DbConfig.getInstance().getData(4);
        if (TextUtils.isEmpty(serverIp))
            serverIp = AppConfig.SERVERIP;

        HttpUtils sipHttpUtils = new HttpUtils("http://"+serverIp+":8080/webapi/"+ AppConfig.WEB_API_SIPSATTUS_PARAMATER, new HttpUtils.GetHttpData() {
            @Override
            public void httpData(String result) {
                if (TextUtils.isEmpty(result)) {
                    callback.resultCallback("");
                    return;
                }
                callback.resultCallback(result);
            }
        });
        sipHttpUtils.start();
    }

    public static TimeDo getInstance() {
        if (instance == null) {
            synchronized (TimeDo.class) {
                if (instance == null) {
                    instance = new TimeDo();
                }
            }
        }
        return instance;
    }

    public void init(Context context, long time) {
        this.time = time;
    }

    public void start() {

        if (!mScheduledExecutorService.isShutdown()) {
            mScheduledExecutorService.scheduleWithFixedDelay(this, 0L, time, TimeUnit.MILLISECONDS);
        }
    }


    public interface Callback {
        public void resultCallback(String result);
    }
}