package com.tehike.client.mst.app.project.cmscallbacks;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.Linphone;
import com.tehike.client.mst.app.project.linphone.SipManager;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.TimeUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 描述：定时的检查sip状态,如果 未注册就重新注册
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/17 14:23
 */

public class TimingCheckSipStatus extends Service {

    //定时任务的线程池
    ScheduledExecutorService mScheduledExecutorService = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //延迟10秒后每30秒执行一次
        if (mScheduledExecutorService == null) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            mScheduledExecutorService.scheduleWithFixedDelay(new CheckSipStatusThread(), 10, 15 * 1000, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScheduledExecutorService.shutdown();
    }


    class CheckSipStatusThread extends Thread {
        @Override
        public void run() {

            //获取sip注册时相关的信息
            String sipNum = DbConfig.getInstance().getData(10);
            String sipServer = DbConfig.getInstance().getData(12);
            String sipPwd = DbConfig.getInstance().getData(11);
            if (TextUtils.isEmpty(sipNum))
                sipNum = AppConfig.SIP_NUMBER;
            if (TextUtils.isEmpty(sipServer))
                sipServer = AppConfig.SIP_SERVER;
            if (TextUtils.isEmpty(sipPwd))
                sipPwd = AppConfig.SIP_PWD;
            //判断是否为空


            if (TextUtils.isEmpty(sipNum) || TextUtils.isEmpty(sipServer) || TextUtils.isEmpty(sipPwd)) {
                Logutils.e("AAAA未获取到sip信息");
                return;
            }
            //当前sip是否在线
            if (AppConfig.SIP_STATUS) {
                // Logutils.e("AAAA当前Sip在线");
                return;
            }
            //当前sip服务是否已启动
            if (!SipService.isReady() || !SipManager.isInstanceiated()) {
                Linphone.startService(App.getApplication());
            }
            Linphone.setAccount(sipNum, sipPwd, sipServer);
            Linphone.login();
            Logutils.i("AAAASip重新登录");
        }
    }
}
