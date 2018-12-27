package com.tehike.client.mst.app.project.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.entity.AlarmBean;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 回调接收服务器的报警报文(须去掉空的byte)
 * <p>
 * Created by Root on 2018/4/20.
 */

public class ReceiverServerAlarmService extends Service {


    ReceiverServerAlarm thread = null;
    ServerSocket serverSocket = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (thread == null)
            thread = new ReceiverServerAlarm();
        new Thread(thread).start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serverSocket != null)
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    class ReceiverServerAlarm extends Thread {

        @Override
        public void run() {
            super.run();

            try {
                if (serverSocket == null)
                    serverSocket = new ServerSocket(2000, 3);
                InputStream in = null;
                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        in = socket.getInputStream();
                        //falge 4
                        byte[] header = new byte[524];
                        int read = in.read(header);
                        byte[] flage = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            flage[i] = header[i];
                        }
                        String falge1 = new String(flage, AppConfig.CMS_FORMAT);
                        //sender 32
                        byte[] sender = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            sender[i] = header[i + 4];
                        }
                        int sender_position = ByteUtils.getPosiotion(sender);
                        String sender1 = new String(sender, 0, sender_position, AppConfig.CMS_FORMAT);

                        //videoource

                        //videoFlage 4
                        byte[] videoFlage = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            videoFlage[i] = header[i + 36];
                        }
                        String videoFlage1 = new String(videoFlage, AppConfig.CMS_FORMAT);

                        //videoId 48
                        byte[] videoId = new byte[48];
                        for (int i = 0; i < 48; i++) {
                            videoId[i] = header[i + 40];
                        }
                        int videoId_position = ByteUtils.getPosiotion(videoId);
                        String videoId1 = new String(videoId, 0, videoId_position, AppConfig.CMS_FORMAT);

                        //name 128
                        byte[] videoName = new byte[128];
                        for (int i = 0; i < 128; i++) {
                            videoName[i] = header[i + 88];
                        }
                        int videoName_position = ByteUtils.getPosiotion(videoName);
                        String videoName1 = new String(videoName, 0, videoName_position, AppConfig.CMS_FORMAT);

                        //DeviceType  16
                        byte[] videoDeviceType = new byte[16];
                        for (int i = 0; i < 16; i++) {
                            videoDeviceType[i] = header[i + 216];
                        }
                        int videoDeviceType_position = ByteUtils.getPosiotion(videoDeviceType);
                        String deviceType1 = new String(videoDeviceType, 0, videoDeviceType_position, AppConfig.CMS_FORMAT);

                        //videoIPAddress  32
                        byte[] videoIPAddress = new byte[32];
                        for (int i = 0; i < 16; i++) {
                            videoIPAddress[i] = header[i + 232];
                        }
                        int videoIPAddress_position = ByteUtils.getPosiotion(videoIPAddress);
                        String videoIp1 = new String(videoIPAddress, 0, videoIPAddress_position, AppConfig.CMS_FORMAT);

                        //port_icon 4
                        int sentryId = ByteUtils.bytesToInt(header, 264);
                        System.out.println(sentryId);

                        //Channel  128
                        byte[] videoChannel = new byte[128];
                        for (int i = 0; i < 16; i++) {
                            videoChannel[i] = header[i + 268];
                        }
                        int videoChannel_position = ByteUtils.getPosiotion(videoChannel);
                        String channel1 = new String(videoChannel, 0, videoChannel_position, AppConfig.CMS_FORMAT);

                        //Username  32
                        byte[] videoUsername = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            videoUsername[i] = header[i + 396];
                        }
                        int videoUsername_position = ByteUtils.getPosiotion(videoUsername);
                        String username1 = new String(videoUsername, 0, videoUsername_position, AppConfig.CMS_FORMAT);

                        //Password  32
                        byte[] videoPassword = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            videoPassword[i] = header[i + 428];
                        }
                        int videoPassword_position = ByteUtils.getPosiotion(videoPassword);
                        String videoPass1 = new String(videoPassword, 0, videoPassword_position, AppConfig.CMS_FORMAT);

                        //alarmType 32
                        byte[] videoAlarmType = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            videoAlarmType[i] = header[i + 460];
                        }
                        int videoAlarmType_position = ByteUtils.getPosiotion(videoAlarmType);
                        String alarmType1 = new String(videoAlarmType, 0, videoAlarmType_position, AppConfig.CMS_FORMAT);

                        VideoBen videoBen = new VideoBen(videoFlage1, videoId1, videoName1, deviceType1, videoIp1, sentryId + "", channel1, username1, videoPass1, "", false, "", "");
                        AlarmBean alarmBen = new AlarmBean(sender1, videoBen, alarmType1, "");

                        if (alarmBen != null) {
                            Message message = new Message();
                            message.obj = alarmBen;
                            message.what = 1;
                            handler.sendMessage(message);
                        }
                        Logutils.i("接收到的报警:" + alarmBen.toString());


                    } catch (IOException e) {
                        WriteLogToFile.info("接收报警socket异常:" + e.getMessage());
                        if (socket != null)
                            socket.close();
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logutils.e("接收报警socket异常:" + e.getMessage());
                WriteLogToFile.info("接收报警socket异常:" + e.getMessage());
            }
        }
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == 1) {
                AlarmBean alarmBen = (AlarmBean) msg.obj;
                AlertDialog.Builder builder = new AlertDialog.Builder(App.getApplication());
                builder.setMessage("公告");
                View view = View.inflate(App.getApplication(), R.layout.view_receiveralarm_layout, null);
                TextView showInformation = view.findViewById(R.id.prompt_alarm_information_layout);
                if (alarmBen != null)
                    showInformation.setText("\u3000\u3000" + alarmBen.toString());
                builder.setView(view);
                builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                final Dialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();
            }
        }
    };
}
