package com.tehike.client.mst.app.project.cmscallbacks;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.onvif.Device;
import com.tehike.client.mst.app.project.onvif.Onvif;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.DeviceUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.TimeUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 描述：定时发送心跳服务
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/18 10:32
 */
public class TimingSendHbService extends Service {

    ScheduledExecutorService mScheduledExecutorService = null;

    int sendPort = -1;
    String sendIp = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mScheduledExecutorService == null) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            mScheduledExecutorService.scheduleWithFixedDelay(new RequestVideoSourcesThread(), 0L, AppConfig.SEND_HB_SPACING, TimeUnit.MILLISECONDS);
        }

        Logutils.i("启动心跳服务");
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


    class RequestVideoSourcesThread extends Thread {
        @Override
        public void run() {
            sendHbData();
        }

        private void sendHbData() {

            //获取心中的服务器地址
            if (TextUtils.isEmpty(sendIp)) {
                sendIp = DbConfig.getInstance().getData(6);
            }
            //获取心中服务器的端口
            if (sendPort == -1) {
                sendPort = Integer.parseInt(DbConfig.getInstance().getData(5));
            }

            //udp发送的总数据
            byte[] requestBytes = new byte[96];

            //数据头
            byte[] flag = AppConfig.HEADER_HEADER_ID.getBytes();
            System.arraycopy(flag, 0, requestBytes, 0, 4);

            //手机唯一标识
            byte[] id = new byte[52];

            String senderGuid = DbConfig.getInstance().getData(13);

            if (TextUtils.isEmpty(senderGuid)) {
                senderGuid = AppConfig.SENDHB_ID;
            }

            byte[] idKey = senderGuid.getBytes();//Guid
            System.arraycopy(idKey, 0, id, 0, idKey.length);
            System.arraycopy(id, 0, requestBytes, 4, 52);

            //时间戳
            long timeStemp = TimeUtils.dateStamp();
            byte[] timeByte = ByteUtils.longToBytes(timeStemp);
            byte[] stamp = new byte[8];
            byte[] timeT = timeByte;
            System.arraycopy(timeT, 0, stamp, 0, timeT.length);
            System.arraycopy(stamp, 0, requestBytes, 56, stamp.length);

            // 纬度
            double lat = AppConfig.LOCATION_LAT;
            byte[] latB = ByteUtils.getBytes(lat);
            System.arraycopy(latB, 0, requestBytes, 64, 8);

            // 经度
            double lon = AppConfig.LOCATION_LOG;
            byte[] lonB = ByteUtils.getBytes(lon);
            System.arraycopy(lonB, 0, requestBytes, 72, 8);

            //剩余电量
            byte[] power = new byte[1];
            power[0] = (byte) AppConfig.DEVICE_BATTERY;
            System.arraycopy(power, 0, requestBytes, 80, 1);

            //内存使用
            byte[] mem = new byte[1];
            mem[0] = (byte) Double.parseDouble(AppConfig.DEVICE_RAM + "");
            System.arraycopy(mem, 0, requestBytes, 81, 1);

            //cpu使用
            byte[] cpu = new byte[1];
            cpu[0] = (byte) Double.parseDouble(AppConfig.DEVICE_CPU + "");
            System.arraycopy(cpu, 0, requestBytes, 82, 1);

            //信号强度
            byte[] signal = new byte[1];

            //注意（信号强度为负值）
            signal[0] = (byte) Math.abs(AppConfig.DEVICE_WIFI);
            System.arraycopy(signal, 0, requestBytes, 83, 1);

            //蓝牙连接状态， 0-未连接，1-已连接
            byte[] bluetooth = new byte[1];
            bluetooth[0] = (byte) AppConfig.BLUETOOTH_STATE;
            System.arraycopy(bluetooth, 0, requestBytes, 84, 1);

            //弹箱锁闭状态， 0-未锁闭，1-已锁闭
            byte[] ammobox = new byte[1];
            ammobox[0] = (byte) AppConfig.AMMOBOX_STATE;
            System.arraycopy(ammobox, 0, requestBytes, 85, 1);

            //保留
            byte[] save = new byte[11];
            System.arraycopy(save, 0, requestBytes, 86, 10);

            //把第56到64位（时间戳）的四个字节反转
            byte[] temp = new byte[8];
            System.arraycopy(requestBytes, 56, temp, 0, 8);
            byte[] reversalByte = new byte[8];
            reversalByte[0] = temp[7];
            reversalByte[1] = temp[6];
            reversalByte[2] = temp[5];
            reversalByte[3] = temp[4];
            reversalByte[4] = temp[3];
            reversalByte[5] = temp[2];
            reversalByte[6] = temp[1];
            reversalByte[7] = temp[0];
            System.arraycopy(reversalByte, 0, requestBytes, 56, 8);

            //建立UDP请求
            DatagramSocket socketUdp = null;
            DatagramPacket datagramPacket = null;

            try {
                socketUdp = new DatagramSocket(sendPort);
                datagramPacket = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(sendIp), sendPort);
                socketUdp.send(datagramPacket);
                socketUdp.close();
                Logutils.i("心跳发送成功");
            } catch (Exception e) {
                Logutils.e("发送心跳异常:" + e.getMessage());
                WriteLogToFile.info("发送心跳异常:" + e.getMessage());
            }
        }
    }
}
