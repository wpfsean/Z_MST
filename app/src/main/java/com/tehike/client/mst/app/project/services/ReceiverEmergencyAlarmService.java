package com.tehike.client.mst.app.project.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.AlarmBean;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.onvif.Device;
import com.tehike.client.mst.app.project.onvif.ResolveOnvif;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.ScreenUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerView;

/**
 * 接收友邻哨报警功能服务
 */

public class ReceiverEmergencyAlarmService extends Service {

    ReceiverEmergencyAlarm thread = null;

    ServerSocket serverSocket = null;


    String port = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //启动子线程执行socket服务
        if (thread == null)
            thread = new ReceiverEmergencyAlarm();
        new Thread(thread).start();
        Logutils.i("启动接收友邻哨报警服务");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        Logutils.i("stop");
    }


    /**
     * 接收友邻哨报警
     */
    class ReceiverEmergencyAlarm extends Thread {

        @Override
        public void run() {
            //从数据库取出报警接收的端口
            port = DbConfig.getInstance().getData(15);
            //判断端口是否为空
            if (TextUtils.isEmpty(port)) {
                //如果为为，就每五秒会取一次
                while (true) {
                    try {
                        Logutils.e("未获取到接收友邻哨的端口");
                        WriteLogToFile.info("未获取到接收友邻哨的端口！");
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    port = DbConfig.getInstance().getData(15);
                    //如果不为空就终止循环
                    if (!TextUtils.isEmpty(port)) {
                        Logutils.i("获取到了端口了");
                        break;
                    }
                }
            }
            try {
                //Tcpserver接收报警报文
                if (serverSocket == null)
                    serverSocket = new ServerSocket(Integer.parseInt(port), 3);
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
                        //报警报文的头
                        String falge1 = new String(flage, "gb2312");

                        //sender 32
                        byte[] sender = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            sender[i] = header[i + 4];
                        }
                        int sender_position = ByteUtils.getPosiotion(sender);
                        //获取报警报文的senderip
                        String sender1 = new String(sender, 0, sender_position, "gb2312");

                        /**
                         * 解析video视频对象
                         */


                        //视频数据头
                        byte[] videoFlage = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            videoFlage[i] = header[i + 36];
                        }
                        String videoFlage1 = new String(videoFlage, "gb2312");
                        Logutils.i(videoFlage1);

                        //唯一识别编号
                        byte[] videoId = new byte[48];
                        for (int i = 0; i < 48; i++) {
                            videoId[i] = header[i + 40];
                        }
                        int videoId_position = ByteUtils.getPosiotion(videoId);
                        String videoId1 = new String(videoId, 0, videoId_position, "gb2312");

                        //视频源名称
                        byte[] videoName = new byte[128];
                        for (int i = 0; i < 128; i++) {
                            videoName[i] = header[i + 88];
                        }
                        int videoName_position = ByteUtils.getPosiotion(videoName);
                        String videoName1 = new String(videoName, 0, videoName_position, "gb2312");

                        //设备类型，默认为 ONVIF，终端视频源为RTSP
                        byte[] videoDeviceType = new byte[16];
                        for (int i = 0; i < 16; i++) {
                            videoDeviceType[i] = header[i + 216];
                        }
                        int videoDeviceType_position = ByteUtils.getPosiotion(videoDeviceType);
                        String deviceType1 = new String(videoDeviceType, 0, videoDeviceType_position, "gb2312");

                        //视频源设备IP地址
                        byte[] videoIPAddress = new byte[32];
                        for (int i = 0; i < 16; i++) {
                            videoIPAddress[i] = header[i + 232];
                        }
                        int videoIPAddress_position = ByteUtils.getPosiotion(videoIPAddress);
                        String videoIp1 = new String(videoIPAddress, 0, videoIPAddress_position, "gb2312");

                        //视频源设备端口
                        int sentryId = ByteUtils.bytesToInt(header, 264);
                        System.out.println(sentryId);

                        //视频源通道编号，默认为 1	设备类型=RTSP时，Channel保存rtsp uri 的PathAndQuery 部分

                        byte[] videoChannel = new byte[128];
                        for (int i = 0; i < 128; i++) {
                            videoChannel[i] = header[i + 268];
                        }
                        int videoChannel_position = ByteUtils.getPosiotion(videoChannel);
                        String channel1 = new String(videoChannel, 0, videoChannel_position, "gb2312");

                        //用户名
                        byte[] videoUsername = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            videoUsername[i] = header[i + 396];
                        }
                        int videoUsername_position = ByteUtils.getPosiotion(videoUsername);
                        String username1 = new String(videoUsername, 0, videoUsername_position, "gb2312");

                        //口令
                        byte[] videoPassword = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            videoPassword[i] = header[i + 428];
                        }
                        int videoPassword_position = ByteUtils.getPosiotion(videoPassword);
                        String videoPass1 = new String(videoPassword, 0, videoPassword_position, "gb2312");

                        //报警类型
                        byte[] videoAlarmType = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            videoAlarmType[i] = header[i + 460];
                        }
                        int videoAlarmType_position = ByteUtils.getPosiotion(videoAlarmType);
                        String alarmType1 = new String(videoAlarmType, 0, videoAlarmType_position, "gb2312");

                        VideoBen videoBen = new VideoBen(videoFlage1, videoId1, videoName1, deviceType1, videoIp1, sentryId + "", channel1, username1, videoPass1, "", false, "", "");
                        AlarmBean alarmBen = new AlarmBean(sender1, videoBen, alarmType1, "");

                        if (alarmBen != null) {
                            Message message = new Message();
                            message.obj = alarmBen;
                            message.what = 1;
                            handler.sendMessage(message);
                        } else {
                            Logutils.e("接收到的报警信息为空");
                        }

                    } catch (IOException e) {
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
                Logutils.e("接收友邻哨报警socket异常:" + e.getMessage());
            }
        }
    }

    /**
     * Handler处理子线程发送过来 的数据
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //获取报警源信息
                    AlarmBean alarmBen = (AlarmBean) msg.obj;
                    Logutils.d(alarmBen.toString());
                    receiveAlarmInfo(alarmBen);
                    break;
                case 2:
                    Bundle dbundle = msg.getData();
                    Device device = (Device) dbundle.getSerializable("device");
                    Logutils.d(device.toString());
                    promptAlarmInfo(device);
                    break;
            }
        }
    };

    NodePlayer nodePlayer = null;
    NodePlayerView mNodePlayerView = null;

    /**
     * 显示报警信息
     *
     * @param device
     */
    private void promptAlarmInfo(Device device) {

        //获取最终要播放的视频地址
        String videoUrl = "";
        if (device != null) {
            if (!TextUtils.isEmpty(device.getRtspUrl())) {
                videoUrl = device.getRtspUrl();
            }
        }
        //用dialog显示
        final AlertDialog.Builder builder = new AlertDialog.Builder(App.getApplication());
        builder.setCancelable(false);

        final View view = View.inflate(App.getApplication(), R.layout.prompt_receive_alarm_info_dialog_layout, null);
        builder.setView(view);
        //关闭按键
        ImageButton closeBtn = view.findViewById(R.id.prompt_alarm_close_btn_layout);
        //视频加载提示
        final TextView loadingLayout = view.findViewById(R.id.prompt_alarm_loading_layout);
        //播放器视图
        mNodePlayerView = view.findViewById(R.id.prompt_alarm_video_view_layout);

        //上方滚动打着
        TextView alarmLayout = view.findViewById(R.id.auto_scroll_text_layout);

        String alarmResult = alarmName+"发生"+alarmType+"报警,发生地点"+alarmFrom;
        alarmLayout.setText(alarmResult);

        //如果未获取 到报警源的播放地址
        if (TextUtils.isEmpty(videoUrl)) {
            mNodePlayerView.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.VISIBLE);
        }

        Logutils.i("播放地址:"+videoUrl);

        final NodePlayer nodePlayer = new NodePlayer(App.getApplication());
        nodePlayer.setPlayerView(mNodePlayerView);
        nodePlayer.setAudioEnable(false);
        nodePlayer.setVideoEnable(true);
        nodePlayer.setInputUrl(videoUrl);
        nodePlayer.start();

        //显示dialog
        final Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        //通过当前的dialog获取window对象
        Window window = dialog.getWindow();
        //设置背景，防止变形
        window.setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = ScreenUtils.getInstance(App.getApplication()).getWidth() - 44;//两边设置的间隙相当于margin
        lp.alpha = 0.9f;
        window.setDimAmount(0.5f);//使用时设置窗口后面的暗淡量
        window.setAttributes(lp);


        //底部关闭按键
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (nodePlayer != null) {
                    nodePlayer.stop();
                    nodePlayer.release();
                }
            }
        });
    }

    String alarmFrom = "";
    String alarmName = "";
    String alarmType = "";

    /**
     * 处理报警信息
     *
     * @param alarmBen
     */
    private void receiveAlarmInfo(AlarmBean alarmBen) {

        //判断报警源信息是否为空
        if (alarmBen == null) {
            Logutils.e("接收报警的对象为空！");
            WriteLogToFile.info("接收到的报警信息为空！");
            return;
        }

        //获取报警来源
        alarmFrom = alarmBen.getSender();
        //获取报警点视频名称
        alarmName = alarmBen.getVideoBen().getName();
        //获取报警类型
        alarmType = alarmBen.getAlertType();

        VideoBen videoBen = alarmBen.getVideoBen();


        //判断类型
        String alarmDeviceType = alarmBen.getVideoBen().getDevicetype();
        //用户名
        String alarmUserName = alarmBen.getVideoBen().getUsername();
        //口令
        String alarmUserPwd = alarmBen.getVideoBen().getPassword();
        //ip
        String alarmIp = alarmBen.getVideoBen().getIp();

        /**
         * 设备类型
         *
         * http://19.0.0.23:8080/webapi/videotypes
         *
         *
         */

        //判断视频名称和视频源类型
        if (!TextUtils.isEmpty(alarmName) && !TextUtils.isEmpty(alarmDeviceType) && !TextUtils.isEmpty(alarmUserName) && !TextUtils.isEmpty(alarmUserPwd) && !TextUtils.isEmpty(alarmIp)) {

            String videoUrl = "";
            Device device = new Device();
            if (alarmDeviceType.toUpperCase().equals("RTSP")) {
                //判断是否是rtsp类型
                videoUrl = "rtsp://" + videoBen.getUsername() + ":" + videoBen.getPassword() + "@" + videoBen.getIp() + "/" + videoBen.getChannel();
                Message message = new Message();
                Bundle bundle = new Bundle();
                device.setRtspUrl(videoUrl);
                bundle.putSerializable("device", device);
                message.setData(bundle);
                message.what = 2;
                handler.sendMessage(message);
            } else if (alarmDeviceType.equals("TH-C6000") || alarmDeviceType.toUpperCase().equals("RTMP")) {
                //判断是否rtmp类型
                videoUrl = videoBen.getChannel();
                Message message = new Message();
                Bundle bundle = new Bundle();
                device.setRtspUrl(videoUrl);
                bundle.putSerializable("device", device);
                message.setData(bundle);
                message.what = 2;
                handler.sendMessage(message);
            } else {
                device.setVideoBen(videoBen);
                device.setServiceUrl("http://" + alarmIp + "/onvif/device_service");
                ResolveOnvif onvif = new ResolveOnvif(device, new ResolveOnvif.GetRtspCallback() {
                    @Override
                    public void getDeviceInfoResult(String rtsp, boolean isSuccess, Device mDevice) {
                        //handler处理解析返回的设备对象
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("device", mDevice);
                        message.setData(bundle);
                        message.what = 2;
                        handler.sendMessage(message);
                    }
                });
                //执行线程
                new Thread(onvif).start();
            }
        }
    }
}
