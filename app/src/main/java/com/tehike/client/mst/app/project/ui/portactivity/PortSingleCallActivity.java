package com.tehike.client.mst.app.project.ui.portactivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.adapters.ButtomSlidingAdapter;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.Linphone;
import com.tehike.client.mst.app.project.linphone.PhoneCallback;
import com.tehike.client.mst.app.project.onvif.Device;
import com.tehike.client.mst.app.project.utils.ActivityUtils;
import com.tehike.client.mst.app.project.utils.GsonUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.TimeUtils;

import org.linphone.core.LinphoneCall;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

/**
 * Created by Root on 2018/7/16.
 */

public class PortSingleCallActivity extends BaseActivity implements View.OnClickListener, NodePlayerDelegate {
    /**
     * 来电时间显示
     */
    @BindView(R.id.show_call_time)
    public TextView show_call_time;

    /**
     * 挂电话Icon
     */
    @BindView(R.id.btn_handup_icon)
    public ImageButton hangupButton;

    /**
     * 静音
     */
    @BindView(R.id.btn_mute)
    public ImageButton btn_mute;

    /**
     * 声音加
     */
    @BindView(R.id.btn_volumeadd)
    public ImageButton btn_volumeadd;

    /**
     * 摄像头切换
     */
    @BindView(R.id.btn_camera)
    public ImageButton btn_camera;

    /**
     * 声音减
     */
    @BindView(R.id.btn_volumelow)
    public ImageButton btn_volumelow;

    /**
     * 正在与谁通话
     */
    @BindView(R.id.text_who_is_calling_information)
    public TextView text_who_is_calling_information;

    /**
     * 远端播放器
     */
    @BindView(R.id.main_view)
    NodePlayerView remoteView;

    /**
     * 远端播放器进度条
     */
    @BindView(R.id.main_progressbar_port)
    ProgressBar remotePr;

    /**
     * 显示远端播放器加载信息
     */
    @BindView(R.id.remote_loading_infor_port)
    TextView remoteLoading;

    /**
     * 远端视频显示的主区域
     */
    @BindView(R.id.framelayout_bg_layout_port)
    FrameLayout remoteFrameLyout;

    /**
     * 本地端的视频区域
     */
    @BindView(R.id.relativelayout_bg_layout_port)
    RelativeLayout nativeRelativeLayout;

    /**
     * 获取所有的sip数据(用于查找对方的视频源)
     */
    List<Device> rtspSources = new ArrayList<>();

    /**
     * 本地采集视频预览区域
     */
    @BindView(R.id.secodary_surfacevie)
    StreamLiveCameraView mLiveCameraView;


    @BindView(R.id.image_bg_layout)
    ImageView image_bg_layout;


    /**
     * 电量信息图标
     */
    @BindView(R.id.icon_electritity_show)
    ImageView batteryIcon;

    /**
     * 信号强度图标
     */
    @BindView(R.id.icon_network)
    ImageView rssiIcon;

    /**
     * 连接状态图标
     */
    @BindView(R.id.icon_connection_show)
    ImageView connetIConb;


    /**
     * 显示当前时间分秒
     */
    @BindView(R.id.sipinfor_title_time_layout)
    TextView currentTimeLayout;

    /**
     * 显示当前的年月日
     */
    @BindView(R.id.sipinfor_title_date_layout)
    TextView currentYearLayout;

    /**
     * 声音控制对象
     */
    AudioManager mAudioManager = null;

    /**
     * 远端播放器
     */
    NodePlayer remotePlayer;

    /**
     * 来源是打电话还是接电话，true为打电话，false为接电话
     */
    boolean isCall = true;

    /**
     * 来电者
     */
    String userName = "";

    /**
     * 是否是视频电话
     */
    boolean isVideo = false;

    /**
     * 是否静音
     */
    boolean isSilent = false;

    /**
     * 来电者的哨位名称
     */
    String remoteDeviceName = "";


    /**
     * 时间显示线程是否正在远行
     */
    boolean mWorking = false;

    /**
     * 来电是否是视频电话
     */
    boolean isComingVideo = false;

    /**
     * 计时的子线程
     */
    Thread mThread = null;

    /**
     * 时间戳
     */
    int num = 0;

    /**
     * rtmp推流设置对象
     */
    private StreamAVOption streamAVOption;

    /**
     * 显示时间的线程是否正在运行
     */
    boolean threadIsRun = true;

    /**
     * 对方视频源
     */
    String remoteRtsp = "";

    boolean isConnected = false;


    /**
     * 推流地址
     */
    private String nativeRTMP = "rtmp://" + "19.0.0.20" + "/oflaDemo/" + AppConfig.SIP_NUMBER + "@" + AppConfig.SIP_SERVER;

    @Override
    protected int intiLayout() {
        return R.layout.activity_port_calling;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {


        PackageManager pm = getPackageManager();
        boolean permission = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.CAMERA", "packageName"));
        if (permission) {
            // showToast("有这个权限");
            Logutils.i("有这个权限");
        } else {
            //showToast("木有这个权限");
            Logutils.i("木有这个权限");
        }


        //显示时间
        displayCurrentTime();

        //初始化view
        initView();

        //初始化参数
        initParamater();

        //电话监听回调
        phoneCallback();

        //获取传递到本页面参数
        getIntentParamaters();

        //获取数据
        initData();

        //根据手机不同的状态进行相应的处理
        phoneCallingStatus();

    }

    /**
     * 判断当前的来电状态
     */
    private void phoneCallingStatus() {
        //拨打视频电话
        if (isCall && isVideo) {
            Logutils.i("拨打视频电话");
            outGoingWithVideo();
        }

        //拨打语音电话
        if (isCall && !isVideo) {
            Logutils.i("拨打语音电话");
            outGoingWithOutVideo();
        }
        //接视频电话
        if (!isCall && isComingVideo) {
            Logutils.i("接视频电话");
            inComingWithVideo();
        }
    }

    /**
     * 获取本页面传递的参数
     */
    private void getIntentParamaters() {

        isCall = this.getIntent().getBooleanExtra("isMakingCall", false);//是打电话还是接电话
        userName = this.getIntent().getStringExtra("callerNumber");//对方号码
        isVideo = this.getIntent().getBooleanExtra("isMakingVideoCall", false);//是可视频电话，还是语音电话
        isComingVideo = this.getIntent().getBooleanExtra("isVideoCall", false);
        boolean isConnect = this.getIntent().getBooleanExtra("isCallConnected", false);
        if (isConnect) {
            isConnected = true;
            handler.sendEmptyMessage(4);
        }
    }

    /**
     * 视频电话
     */
    private void inComingWithVideo() {
        if (isConnected)
            try {
                if (!mLiveCameraView.isStreaming()) {
                    mLiveCameraView.startStreaming(nativeRTMP);
                    Logutils.i("nativeIp--->>" + nativeRTMP);
                }
            } catch (Exception e) {
            }
        image_bg_layout.setVisibility(View.GONE);
        hangupButton.setBackgroundResource(R.drawable.port_btn_hang_up_selected);
        remoteFrameLyout.setVisibility(View.VISIBLE);
        nativeRelativeLayout.setVisibility(View.VISIBLE);
        //  text_who_is_calling_information.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(remoteRtsp)) {
            remotePlayer.setInputUrl(remoteRtsp);
            remotePlayer.setAudioEnable(false);
            remotePlayer.start();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remotePr.setVisibility(View.GONE);
                    remoteLoading.setVisibility(View.VISIBLE);
                    remoteLoading.setText("未加载到对方的视频资源...");
                }
            });
        }
    }

    /**
     * 拨打语音电话
     */
    private void outGoingWithOutVideo() {
        hangupButton.setBackgroundResource(R.drawable.port_btn_hang_up_selected);
        text_who_is_calling_information.setVisibility(View.VISIBLE);
        nativeRelativeLayout.setVisibility(View.GONE);
    }

    /**
     * 显示当前的时间
     */
    private void displayCurrentTime() {
        //启动线程每秒刷新一下
        TimeThread timeThread = new TimeThread();
        new Thread(timeThread).start();

        //显示当前的年月日
        Date date = new Date();

        //格式
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy年MM月dd日");
        String currntYearDate = yearFormat.format(date);
        if (!TextUtils.isEmpty(currntYearDate))
            currentYearLayout.setText(currntYearDate);
    }

    //显示时间的线程
    class TimeThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(6);
            } while (threadIsRun);
        }
    }

    /**
     * 向外拨打视频电话
     */
    private void outGoingWithVideo() {

        //开始推流
        if (isConnected)
            try {
                if (!mLiveCameraView.isStreaming()) {
                    mLiveCameraView.startStreaming(nativeRTMP);
                }
            } catch (Exception e) {
            }

        image_bg_layout.setVisibility(View.GONE);
        remoteFrameLyout.setVisibility(View.VISIBLE);
        nativeRelativeLayout.setVisibility(View.VISIBLE);


        if (!TextUtils.isEmpty(remoteRtsp)) {
            remotePlayer.setInputUrl(remoteRtsp);
            remotePlayer.setAudioEnable(false);
            remotePlayer.start();
        } else {
            remotePr.setVisibility(View.GONE);
            remoteLoading.setText("未加载到对方的视频资源...");
        }
    }

    /**
     * 初始化抢注参数
     */
    private void initParamater() {
        streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl = nativeRTMP;
        //参数配置 end

        try {
            mLiveCameraView.init(App.getApplication(), streamAVOption);
        } catch (Exception e) {
        }
        mLiveCameraView.addStreamStateListener(resConnectionListener);
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        mLiveCameraView.setHardVideoFilter(new HardVideoGroupFilter(files));

    }

    /**
     * 初始化数据
     */
    private void initData() {
        //先清除已存在的数据
        if (rtspSources != null && rtspSources.size() > 0) {
            rtspSources.clear();
        }
        //获取本地保存的数据
        String dataSources = (String) SharedPreferencesUtils.getObject(PortSingleCallActivity.this, "sip_resources", "");
        if (TextUtils.isEmpty(dataSources)) {
            handler.sendEmptyMessage(1);
            return;
        }
        //把String转成集合
        List<Device> alterSamples = GsonUtils.GsonToList(dataSources, Device.class);

        //判断是否转换后的集合是否为空
        if (alterSamples != null) {
            rtspSources = alterSamples;
        }

        //获取当前来电的rtsp和设备名
        for (Device mDevice : rtspSources) {
            if (mDevice.getSipNum().equals(userName)) {
                String rtsp = mDevice.getRtspUrl();
                if (!TextUtils.isEmpty(rtsp)) {
                    remoteRtsp = rtsp;
                }
                String deviceType = mDevice.getName();
                if (!TextUtils.isEmpty(deviceType)) {
                    remoteDeviceName = deviceType;
                }
            }
        }
    }


    /**
     * 初始化View
     */
    private void initView() {
        //视频播放器的回调
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        remotePlayer = new NodePlayer(this);
        remotePlayer.setPlayerView(remoteView);

        //按键添加监听
        remotePlayer.setNodePlayerDelegate(this);
        hangupButton.setOnClickListener(this);
        btn_camera.setOnClickListener(this);
        btn_volumelow.setOnClickListener(this);
        btn_mute.setOnClickListener(this);
        btn_volumeadd.setOnClickListener(this);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_handup_icon:
                Linphone.hangUp();
                num = 0;
                threadStop();
                isConnected = false;
                ActivityUtils.removeActivity(this);
                PortSingleCallActivity.this.finish();
                break;
            case R.id.btn_mute:
                if (!isSilent) {
                    Linphone.toggleMicro(true);
                    btn_mute.setBackgroundResource(R.drawable.port_btn_mute_selected);
                    isSilent = true;
                } else {
                    Linphone.toggleMicro(false);
                    btn_mute.setBackgroundResource(R.mipmap.port_btn_mute_selected);
                    isSilent = false;
                }
                break;
            //前后摄像头的转换
            case R.id.btn_camera:
                if (mLiveCameraView != null)
                    if (mLiveCameraView.isStreaming())
                        mLiveCameraView.swapCamera();
                break;
            case R.id.btn_volumeadd:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            //音量减
            case R.id.btn_volumelow:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //显示状态
        displayTitleIcon();

        if (AppConfig.SIP_STATUS == true) {
            handler.sendEmptyMessage(7);
        } else {
            handler.sendEmptyMessage(8);
        }
    }

    /**
     * 显示当前的头部状态
     */
    private void displayTitleIcon() {

        if (AppConfig.SIP_STATUS) {
            connetIConb.setBackgroundResource(R.mipmap.icon_connection_normal);
        } else {
            connetIConb.setBackgroundResource(R.mipmap.icon_connection_disable);
        }
        //电池电量信息
        int level = AppConfig.DEVICE_BATTERY;
        if (level >= 75 && level <= 100) {
            batteryIcon.setBackgroundResource(R.mipmap.icon_electricity_a);
        }
        if (level >= 50 && level < 75) {
            batteryIcon.setBackgroundResource(R.mipmap.icon_electricity_b);
        }
        if (level >= 25 && level < 50) {
            batteryIcon.setBackgroundResource(R.mipmap.icon_electricity_c);
        }
        if (level >= 0 && level < 25) {
            batteryIcon.setBackgroundResource(R.mipmap.icon_electricity_disable);
        }
        //信号 状态
        int rssi = AppConfig.DEVICE_WIFI;
        if (rssi > -50 && rssi < 0) {
            rssiIcon.setBackgroundResource(R.mipmap.icon_network);
        } else if (rssi > -70 && rssi <= -50) {
            rssiIcon.setBackgroundResource(R.mipmap.icon_network_a);
        } else if (rssi < -70) {
            rssiIcon.setBackgroundResource(R.mipmap.icon_network_b);
        } else if (rssi == -200) {
            rssiIcon.setBackgroundResource(R.mipmap.icon_network_disable);
        }
    }

    /**
     * Linphone电话监听
     */
    private void phoneCallback() {
        Linphone.addCallback(null, new PhoneCallback() {
            @Override
            public void incomingCall(LinphoneCall linphoneCall) {
                Logutils.i("来电");
                // threadStart();
            }

            @Override
            public void outgoingInit() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        show_call_time.setText("00:00");
                        hangupButton.setBackgroundResource(R.drawable.port_btn_hang_up_selected);
                        text_who_is_calling_information.setText("正在响铃《  " + userName + "  》");
                    }
                });
            }

            @Override
            public void callConnected() {
                Logutils.i("callConnected");
                isConnected = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        show_call_time.setText("00:00");
                        hangupButton.setBackgroundResource(R.drawable.port_btn_hang_up_selected);

                        if (!TextUtils.isEmpty(remoteDeviceName)) {
                            text_who_is_calling_information.setText("正在与" + remoteDeviceName + "通话");
                        } else {
                            text_who_is_calling_information.setText("正在与" + userName + "通话");
                        }

                        threadStart();
                    }
                });
            }

            @Override
            public void callEnd() {
                isConnected = false;
                if (mLiveCameraView.isStreaming()) {
                    mLiveCameraView.stopStreaming();
                }
                Logutils.i("callEnd");
                closePlayer();

            }

            @Override
            public void callReleased() {
                isConnected = false;
                Logutils.i("callReleased");
                if (mLiveCameraView.isStreaming()) {
                    mLiveCameraView.stopStreaming();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        threadStop();
                        num = 0;
                        show_call_time.setText("00:00");
                        hangupButton.setBackgroundResource(R.drawable.port_btn_hang_up_selected);
                    }
                });
                closePlayer();
                PortSingleCallActivity.this.finish();
            }

            @Override
            public void error() {
                isConnected = false;
                if (mLiveCameraView.isStreaming()) {
                    mLiveCameraView.stopStreaming();
                }
                Logutils.i("error");
                closePlayer();
            }
        });
    }

    /**
     * 计时线程开启
     */
    public void threadStart() {
        Logutils.i("启动线程计时...");
        mWorking = true;
        if (mThread != null && mThread.isAlive()) {
            Logutils.i("start: thread is alive");
        } else {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mWorking) {

                        try {
                            Thread.sleep(1 * 1000);
                            handler.sendEmptyMessage(1);
                        } catch (InterruptedException e) {
                            Logutils.e("线程异常:" + e.getMessage());
                        }
                    }
                }
            });
            mThread.start();
        }
    }

    /**
     * 计时线程停止
     */
    public void threadStop() {
        if (mWorking) {
            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
                mThread = null;
            }
            show_call_time.setText("00:00");
            mWorking = false;
        }
    }

    @Override
    public void onNetChange(int state, String name) {

    }

    public void closePlayer() {
        if (remotePlayer != null) {
            remotePlayer.release();
            remotePlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //停止推流
        if (mLiveCameraView != null) {
            if (mLiveCameraView.isStreaming()) {

                mLiveCameraView.stopStreaming();
            }
            mLiveCameraView.stopRecord();
            mLiveCameraView.removeAllViews();
            mLiveCameraView.destroy();

        }

        threadIsRun = false;
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }


    @Override
    public void onEventCallback(NodePlayer player, int event, String msg) {
        if (player == remotePlayer) {
            if (event == 1001) {

                handler.sendEmptyMessage(2);
            }
            if (event == 1003) {
                handler.sendEmptyMessage(3);
            }
        }
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    num++;
                    if (isVisible)
                        show_call_time.setText(TimeUtils.getTime(num) + "");
                    break;
                case 2:
                    if (isVisible) {
                        remoteLoading.setVisibility(View.GONE);
                        remotePr.setVisibility(View.GONE);
                    }
                    break;
                case 3:
                    if (isVisible) {
                        remoteLoading.setVisibility(View.VISIBLE);
                        remotePr.setVisibility(View.GONE);
                    }
                    break;
                case 4:
                    threadStart();
                    if (!TextUtils.isEmpty(remoteDeviceName)) {
                        text_who_is_calling_information.setText("正在与" + remoteDeviceName + "通话");
                    } else {
                        text_who_is_calling_information.setText("正在与" + userName + "通话");
                    }

                    break;
                case 5:
                    // nativeLoading.setVisibility(View.VISIBLE);
                    break;
                case 6:
                    Date date = new Date();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                    String hoursStr = simpleDateFormat.format(date);
                    if (isVisible)
                        currentTimeLayout.setText(hoursStr);
                    break;
                case 7:
                    if (isVisible)
                        connetIConb.setBackgroundResource(R.mipmap.icon_connection_normal);
                    break;
                case 8:
                    if (isVisible)
                        connetIConb.setBackgroundResource(R.mipmap.icon_connection_disable);
                    break;
            }
        }
    };


    RESConnectionListener resConnectionListener = new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int result) {
            //result 0成功  1 失败
            //Toast.makeText(LandSingleCallActivity.this, "打开推流连接 状态：" + result + " 推流地址：" + rtmpUrl, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onWriteError(int errno) {
            //  Toast.makeText(LandSingleCallActivity.this, "推流出错,请尝试重连", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCloseConnectionResult(int result) {
            //result 0成功  1 失败
            // Toast.makeText(LandSingleCallActivity.this, "关闭推流连接 状态：" + result, Toast.LENGTH_LONG).show();
        }
    };
}
