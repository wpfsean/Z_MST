package com.tehike.client.mst.app.project.ui.landactivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.cmscallbacks.AmmoRequestCallBack;
import com.tehike.client.mst.app.project.cmscallbacks.SendEmergencyAlarmToServerThrad;
import com.tehike.client.mst.app.project.cmscallbacks.TimingCheckSipStatus;
import com.tehike.client.mst.app.project.cmscallbacks.TimingRequestCmsSipDataService;
import com.tehike.client.mst.app.project.cmscallbacks.TimingRequestCmsVideoDataService;
import com.tehike.client.mst.app.project.cmscallbacks.TimingSendHbService;
import com.tehike.client.mst.app.project.db.DatabaseHelper;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.SysInfoBean;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.Linphone;
import com.tehike.client.mst.app.project.linphone.PhoneCallback;
import com.tehike.client.mst.app.project.linphone.RegistrationCallback;
import com.tehike.client.mst.app.project.linphone.SipManager;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.onvif.Device;
import com.tehike.client.mst.app.project.services.ReceiverEmergencyAlarmService;
import com.tehike.client.mst.app.project.services.ReceiverServerAlarmService;
import com.tehike.client.mst.app.project.services.RemoteVoiceService;
import com.tehike.client.mst.app.project.services.ServiceUtils;
import com.tehike.client.mst.app.project.utils.GsonUtils;
import com.tehike.client.mst.app.project.utils.HttpRequestSysinfo;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.SipIsOnline;
import com.tehike.client.mst.app.project.utils.ToastUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import org.linphone.core.LinphoneCall;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：横屏的首页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/18 15:54
 */


public class LandMainActivity extends BaseActivity {

    /**
     * 显示当前时间分秒
     */
    @BindView(R.id.home_show_current_time_layout)
    TextView currentTimeLayout;

    /**
     * 显示当前的年月日
     */
    @BindView(R.id.show_display_year_layout)
    TextView currentYearLayout;

    /**
     * Sip是否在线状态
     */
    @BindView(R.id.home_sip_online_layout)
    ImageView sipStatusIcon;

    /**
     * 电池电量状态图标
     */
    @BindView(R.id.home_battery_infor_layout)
    ImageView batteryStatusIcon;

    /**
     * 信息状态图标
     */
    @BindView(R.id.home_signal_infor_layout)
    ImageView signalStatusIcon;

    @BindView(R.id.open_box_loading_icon_layout)
    ImageView loadingIcon;

    /**
     * 线程是否正在运行
     */
    boolean threadIsRun = true;

    /**
     * 本机的ip
     */
    String ip = "";


    @Override
    protected int intiLayout() {
        return R.layout.activity_land_main;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        //初始化所有服务
        initializeAllService();

        //显示当前日期
        initializeCurrentDate();
    }

    /**
     * 显示时间及日期
     */
    private void initializeCurrentDate() {
        //启动显示时间的线程
        TimingThread timeThread = new TimingThread();
        new Thread(timeThread).start();

        //显示当前的年月日
        SimpleDateFormat dateD = new SimpleDateFormat("yyyy年MM月dd日");
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        currentYearLayout.setText(dateD.format(date).toString());

    }

    /**
     * 启动各种服务
     * <p>
     * 从cms上服务数据
     * <p>
     * 检查sip是否在线
     * <p>
     * 检查sip是否启动
     * <p>
     * 接收友邻哨服务
     */
    private void initializeAllService() {

        //启动服务获取CMS上的video数据并解析
        if (!ServiceUtils.isServiceRunning(TimingRequestCmsVideoDataService.class))
            ServiceUtils.startService(TimingRequestCmsVideoDataService.class);

        //启动服务获取CMS上的SIp数据并解析
        if (!ServiceUtils.isServiceRunning(TimingRequestCmsSipDataService.class))
            ServiceUtils.startService(TimingRequestCmsSipDataService.class);

        //启动发送心跳的服务
        if (!ServiceUtils.isServiceRunning(TimingSendHbService.class))
            ServiceUtils.startService(TimingSendHbService.class);

        //启动Linphone
        if (!SipService.isReady() || !SipManager.isInstanceiated()) {
            Linphone.startService(App.getApplication());
        }
        //启动检查sip状态的服务
        if (!ServiceUtils.isServiceRunning(TimingCheckSipStatus.class))
            ServiceUtils.startService(TimingCheckSipStatus.class);

        //启动被动喊话监听
        if (!ServiceUtils.isServiceRunning(RemoteVoiceService.class))
            ServiceUtils.startService(RemoteVoiceService.class);

        //启动接收报警接收
        if (!ServiceUtils.isServiceRunning(ReceiverServerAlarmService.class))
            ServiceUtils.startService(ReceiverServerAlarmService.class);

        //启动接收友邻哨报警接收
        if (!ServiceUtils.isServiceRunning(ReceiverEmergencyAlarmService.class))
            ServiceUtils.startService(ReceiverEmergencyAlarmService.class);

        //当前登录的用户名
        String userName = DbConfig.getInstance().getData(1);
        if (TextUtils.isEmpty(userName))
            userName = AppConfig.USERNAME;

        //当前登录的密码
        String userPwd = DbConfig.getInstance().getData(2);
        if (TextUtils.isEmpty(userPwd))
            userPwd = AppConfig.PWD;

        HttpRequestSysinfo httpRequestSysinfo = new HttpRequestSysinfo(userName, userPwd, new HttpRequestSysinfo.GetSysinfo() {
            @Override
            public void callbackData(SysInfoBean mSysInfoBean) {
                if (mSysInfoBean != null) {
                    Message message = new Message();
                    message.obj = mSysInfoBean;
                    message.what = 23;
                    handler.sendMessage(message);
                } else {
                    WriteLogToFile.info("竖屏登录后未获取到配置文件信息");
                }
            }
        });
        new Thread(httpRequestSysinfo).start();

        //获取本机ip
        if (NetworkUtils.isConnected())
            ip = NetworkUtils.getIPAddress(true);
    }

    /**
     * 网络状态变化回调
     *
     * @param state 5和-1是无网络
     * @param name
     */
    @Override
    public void onNetChange(int state, String name) {
        if (state == -1 || state == 5) {
            handler.sendEmptyMessage(5);
        }
    }


    /**
     * 控制按键的点击事件
     *
     * @param view
     */
    @OnClick({R.id.seting_icon_layout, R.id.the_standby_play_btn1, R.id.video_intercom_btn, R.id.video_btn, R.id.video_phone_btn, R.id.video_live_meeting_btn, R.id.cluster_intercom_btn, R.id.emergency_call_btn, R.id.alarm_btn, R.id.apply_for_play_btn, R.id.the_standby_play_btn2})
    public void clickEvent(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            //进行设置中心按键
            case R.id.seting_icon_layout:
                if (intent != null) {
                    VerifyPwdDialog(intent);
                }
                break;
            //视频对讲
            case R.id.video_intercom_btn:
                if (intent != null) {
                    intent.setClass(LandMainActivity.this, LandSipGroupActivity.class);
                    startActivity(intent);
                }
                break;
            //视频监控
            case R.id.video_btn:
                if (intent != null) {
                    intent.setClass(LandMainActivity.this, LandMutilScreenActivity.class);
                    startActivity(intent);
                }
                break;
            //t备用功能（即时通信）
            case R.id.video_phone_btn:
                showProgressSuccess("正在开发!");
                break;
            //会议直播
            case R.id.video_live_meeting_btn:
                showProgressSuccess("正在开发!");
                break;
            //集群对讲
            case R.id.cluster_intercom_btn:
                showProgressSuccess("正在开发!");
                break;
            //应急呼叫
            case R.id.emergency_call_btn:
                emergencyCallDutyRoom();
                break;
            //应急报警
            case R.id.alarm_btn:
                DisplayAlarmSelectionDialog();
                break;
            //申请供弹
            case R.id.apply_for_play_btn:
                OpenBullueBox();
                break;
            case R.id.the_standby_play_btn1:
                showProgressSuccess("正在开发!");
                break;
            case R.id.the_standby_play_btn2:
                showProgressSuccess("正在开发!");
                break;
        }
    }


    /**
     * 弹出验证密码框
     *
     * @param intent
     */
    private void VerifyPwdDialog(final Intent intent) {

        //显示的view
        View view = LayoutInflater.from(this).inflate(R.layout.prompt_verification_pwd_layout, null);
        //控件显示内容
        final EditText editTextPwd = view.findViewById(R.id.verification_pwd_layout);
        //确认按键
        TextView sureVerifyBtn = view.findViewById(R.id.verification_sure_layout);
        //popuwindow显示
        final PopupWindow popu = new PopupWindow(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        popu.setContentView(view);
        //显示所在的位置
        View rootview = LayoutInflater.from(LandMainActivity.this).inflate(R.layout.activity_land_main, null);
        popu.showAtLocation(rootview, Gravity.CENTER, 0, 0);
        popu.setBackgroundDrawable(new BitmapDrawable());
        popu.setFocusable(true);
        popu.setTouchable(true);
        popu.setOutsideTouchable(true);
        popu.update();
        //设置透明背景
        final WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.4f;
        getWindow().setAttributes(lp);
        //消失后背景透明度恢复
        popu.setOnDismissListener(new PopupWindow.OnDismissListener() {
            //在dismiss中恢复透明度
            public void onDismiss() {
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        //确认按键监听
        sureVerifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取输入的口令
                String edPwd = editTextPwd.getText().toString().trim();
                if (TextUtils.isEmpty(edPwd)) {
                    handler.sendEmptyMessage(21);
                    return;
                }
                //获取当前用户登录时的口令
                String pwd = DbConfig.getInstance().getData(2);
                if (TextUtils.isEmpty(pwd)) {
                    pwd = AppConfig.PWD;
                }
                //判断输入的口令是否正确
                if (edPwd.equals(pwd)) {
                    //正确就消失popu并跳转
                    if (popu.isShowing()) {
                        popu.dismiss();
                    }
                    intent.setClass(LandMainActivity.this, LandSettingActivity.class);
                    LandMainActivity.this.startActivity(intent);
                } else {
                    //不正确就提示
                    handler.sendEmptyMessage(22);
                }
            }
        });
    }

    /**
     * 申请开户子弹箱
     */
    private void OpenBullueBox() {
        //加载动画
        Animation mLoadingAnim = AnimationUtils.loadAnimation(this, R.anim.loading);
        loadingIcon.setVisibility(View.VISIBLE);
        loadingIcon.setAnimation(mLoadingAnim);
        //子线程去请求开启
        AmmoRequestCallBack ammoRequestCallBack = new AmmoRequestCallBack(new AmmoRequestCallBack.GetDataListern() {
            @Override
            public void getDataInformation(String result) {
                //判断返回的结果是否为空
                if (TextUtils.isEmpty(result)) {
                    Logutils.e("开箱结果为null");
                    WriteLogToFile.info("申请开户子弹箱失败,返回结果为NUll!");
                    return;
                }
                Message message = new Message();
                message.what = 18;
                message.obj = result;
                handler.sendMessage(message);
            }
        });
        ammoRequestCallBack.start();
    }

    /**
     * Popuwindow弹窗事件
     */
    PopupWindow popu = null;

    /**
     * 弹出报警选择窗口
     */
    private void DisplayAlarmSelectionDialog() {

        //要加载的布局view
        View view = LayoutInflater.from(this).inflate(R.layout.activity_land_home_alert_dialog_layout, null);
        //popu显示
        popu = new PopupWindow(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        popu.setContentView(view);
        //popu要显示的位置
        View rootview = LayoutInflater.from(LandMainActivity.this).inflate(R.layout.activity_land_main, null);
        popu.showAtLocation(rootview, Gravity.TOP | Gravity.CENTER, 0, 40);
        popu.setBackgroundDrawable(new BitmapDrawable());
        popu.setFocusable(true);
        popu.setTouchable(false);
        popu.setOutsideTouchable(false);
        //设置透明度
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.4f;
        getWindow().setAttributes(lp);
        //popu内的点击事件
        popuClickEvent(view, popu);
    }

    /**
     * popuwindow显示时，阻止activity事件分发机制
     *
     * @param event
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (popu != null && popu.isShowing()) {
            return false;
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * 弹窗内按键的点击事件
     *
     * @param view
     * @param popu
     */
    private void popuClickEvent(View view, final PopupWindow popu) {
        //脱逃
        view.findViewById(R.id.btn_takeoff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("脱逃");
                    popu.dismiss();
                }
            }
        });
        //暴狱
        view.findViewById(R.id.btn_prison).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("暴狱");
                    popu.dismiss();
                }
            }
        });
        //袭击
        view.findViewById(R.id.btn_attack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("袭击");
                    popu.dismiss();
                }
            }
        });
        //灾害
        view.findViewById(R.id.btn_disaster).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("灾害");
                    popu.dismiss();

                }
            }
        });
        //挟持
        view.findViewById(R.id.btn_hold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("挟持");
                    popu.dismiss();
                }
            }
        });
        //突发
        view.findViewById(R.id.btn_burst).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("突发");
                    popu.dismiss();
                }
            }
        });
        //应急
        view.findViewById(R.id.btn_emergency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    sendEmergencyAlarm("应急");
                    popu.dismiss();
                }
            }
        });
        //关闭
        view.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popu != null && popu.isShowing()) {
                    popu.dismiss();
                }
            }
        });
    }

    /**
     * 向后台服务器发送报警消息
     *
     * @param type
     */
    private void sendEmergencyAlarm(String type) {

        popu.setOnDismissListener(new PopupWindow.OnDismissListener() {
            //在dismiss中恢复透明度
            public void onDismiss() {
                 WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });


        //判断网络是否连接
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(5);
            return;
        }
        //获取本机ip
        String ip = NetworkUtils.getIPAddress(true);
        if (TextUtils.isEmpty(ip)) {
            handler.sendEmptyMessage(5);
            return;
        }
        //取机本地保存的所有的sip的面部视频信息
        String sipSources = (String) SharedPreferencesUtils.getObject(LandMainActivity.this, "sip_resources", "");
        if (TextUtils.isEmpty(sipSources)) {
            return;
        }
        //把本地缓存的字符串转成集合
        List<Device> receiveData = GsonUtils.GsonToList(sipSources, Device.class);
        if (receiveData == null) {
            return;
        }
        //获取 当前 的sip号码
        String currentSipNum = DbConfig.getInstance().getData(10);
        if (TextUtils.isEmpty(currentSipNum))
            currentSipNum = AppConfig.SIP_NUMBER;
        //判断sip号码 是否为空
        if (TextUtils.isEmpty(currentSipNum)) {
            return;
        }
        //遍历找到本机的视频源
        VideoBen videoBen = null;
        for (int i = 0; i < receiveData.size(); i++) {
            if (receiveData.get(i).getSipNum().equals(currentSipNum)) {
                videoBen = receiveData.get(i).getVideoBen();
                break;
            }
        }
        //若本机没有面部视频信息（模拟一个假的面部视频（不可用））
        if (videoBen == null) {
            videoBen = new VideoBen("VGRC", "{c246a818-292d-4df7-87c7-7bd3dd86b11e}", "哨位面部（单屏）监控点", "Onvif", "19.0.0.51", "80", "1", "admin", "pass", "", false, "", "");
        }
        //启动子线程去发送报警信息
        SendEmergencyAlarmToServerThrad sendEmergencyAlarmToServer = new SendEmergencyAlarmToServerThrad(videoBen, type, new SendEmergencyAlarmToServerThrad.Callback() {
            @Override
            public void getCallbackData(String result) {
                if (TextUtils.isEmpty(result)) {
                    Logutils.e("应急报警返回信息为Null:");
                    WriteLogToFile.info("发送应急报警时，返回的信息为null");
                    return;
                }
                Message callbackMessage = new Message();
                callbackMessage.what = 9;
                callbackMessage.obj = result;
                handler.sendMessage(callbackMessage);
            }
        });
        new Thread(sendEmergencyAlarmToServer).start();
    }


    /**
     * 应急呼叫值班室
     */
    private void emergencyCallDutyRoom() {

        //先从本地的文件中找到值班室的号码信息
        String duryRoomNumber = AppConfig.DURY_ROOM;

        //值班室号码是否已获取
        if (TextUtils.isEmpty(duryRoomNumber)) {
            handler.sendEmptyMessage(6);
            return;
        }
        //对方是否在线
        if (!SipIsOnline.isOnline(duryRoomNumber)) {
            handler.sendEmptyMessage(8);
            return;
        }
        //sip服务是否已启动
        if (!SipService.isReady() || !SipManager.isInstanceiated()) {
            handler.sendEmptyMessage(4);
            return;
        }
        //网络是否正常
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(5);
            return;
        }
        //当前sip是否在线
        if (!AppConfig.SIP_STATUS) {
            handler.sendEmptyMessage(7);
            return;
        }

        //打电话并跳转
        Linphone.callTo(duryRoomNumber, false);
        Linphone.toggleSpeaker(true);
        Intent intent = new Intent();
        intent.putExtra("isMakingVideoCall", false);
        intent.putExtra("callerNumber", duryRoomNumber);
        intent.putExtra("isMakingCall", true);
        intent.setClass(LandMainActivity.this, LandSingleCallActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //修改状态提示图标
        upDateStatusPromptIcon();

        //添加sip状态回调
        linphoneStatusCallback();
    }

    /**
     * Linphone状态监听回调
     */
    private void linphoneStatusCallback() {

        //linphone注册状态及电话状态的监听
        Linphone.addCallback(new RegistrationCallback() {
            @Override
            public void registrationProgress() {
                Logutils.i("注册中");
            }

            @Override
            public void registrationOk() {
                handler.sendEmptyMessage(2);
                Logutils.i("注册成功");
            }

            @Override
            public void registrationFailed() {
                handler.sendEmptyMessage(3);
                Logutils.i("注册失败");
            }
        }, new PhoneCallback() {
            @Override
            public void incomingCall(LinphoneCall linphoneCall) {
                Logutils.i("linphoneCall");
            }

            @Override
            public void outgoingInit() {
                Logutils.i("outgoingInit");
            }

            @Override
            public void callConnected() {
                Logutils.i("callConnected");
            }

            @Override
            public void callEnd() {
                Logutils.i("callEnd");
            }

            @Override
            public void callReleased() {
                Logutils.i("callReleased");
            }

            @Override
            public void error() {
                Logutils.i("error");
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //poPu消失
        if (popu != null) {
            popu.dismiss();
        }
        //刷新时间的线程停止
        threadIsRun = false;
        //handler移除监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }

    /**
     * 显示时间的线程(每秒刷新一下时间)
     */
    class TimingThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logutils.i("Thread error:" + e.getMessage());
                }
                handler.sendEmptyMessage(1);
            } while (threadIsRun);
        }
    }

    /**
     * 显示当前的时间
     */
    private void displayCurrentTime() {
        Date date = new Date();
        SimpleDateFormat timeD = new SimpleDateFormat("HH:mm:ss");
        String currentTime = timeD.format(date).toString();
        if (!TextUtils.isEmpty(currentTime)) {
            if (isVisible)
                currentTimeLayout.setText(currentTime);
        }
    }

    /**
     * 修改状态提示的图标
     */
    private void upDateStatusPromptIcon() {

        //Sip状态
        if (AppConfig.SIP_STATUS) {
            sipStatusIcon.setBackgroundResource(R.mipmap.icon_connection_normal);
        } else {
            sipStatusIcon.setBackgroundResource(R.mipmap.icon_connection_disable);
        }
        //电池电量信息
        int level = AppConfig.DEVICE_BATTERY;
        if (level >= 75 && level <= 100) {
            batteryStatusIcon.setBackgroundResource(R.mipmap.icon_electricity_a);
        }
        if (level >= 50 && level < 75) {
            batteryStatusIcon.setBackgroundResource(R.mipmap.icon_electricity_b);
        }
        if (level >= 25 && level < 50) {
            batteryStatusIcon.setBackgroundResource(R.mipmap.icon_electricity_c);
        }
        if (level >= 0 && level < 25) {
            batteryStatusIcon.setBackgroundResource(R.mipmap.icon_electricity_disable);
        }
        //信号 状态
        int rssi = AppConfig.DEVICE_WIFI;
        if (rssi > -50 && rssi < 0) {
            signalStatusIcon.setBackgroundResource(R.mipmap.icon_network);
        } else if (rssi > -70 && rssi <= -50) {
            signalStatusIcon.setBackgroundResource(R.mipmap.icon_network_a);
        } else if (rssi < -70) {
            signalStatusIcon.setBackgroundResource(R.mipmap.icon_network_b);
        } else if (rssi == -200) {
            signalStatusIcon.setBackgroundResource(R.mipmap.icon_network_disable);
        }
    }


    /**
     * Handler处理子线程发送的消息
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1://显示当前时间
                    if (isVisible)
                        displayCurrentTime();
                    break;
                case 2://sip连接正常状态提示
                    if (isVisible)
                        sipStatusIcon.setBackgroundResource(R.mipmap.icon_connection_normal);
                    break;
                case 3://sip断开状态提示
                    sipStatusIcon.setBackgroundResource(R.mipmap.icon_connection_disable);
                    break;
                case 4://linphone服务未开启
                    showProgressFail("电话功能未启动！");
                    //开始服务
                    Linphone.startService(App.getApplication());
                    break;
                case 5://网络异常提示
                    showProgressFail("请检查网络！");
                    break;
                case 6://未获取值班室信息
                    showProgressFail("未获取到值班室信息！");
                    break;
                case 7://本机sip未注册
                    showProgressFail("没有拨号权限！");
                    break;
                case 8://提示值班室不在线
                    showProgressFail("值班室忙或未在线！");
                    break;
                case 9://提示报警结果
                    if (isVisible) {
                        promptAlarmResult(msg);
                    }
                    break;
                case 18://提示开箱结果
                    if (isVisible) {
                        promptOpenBoxResult(msg);
                    }
                    break;
                case 20://提示密码错误
                    ToastUtils.showShort("密码错误！");
                    break;
                case 21://提示密码不能为空
                    ToastUtils.showShort("不能为空!");
                    break;
                case 22://提示密码不正确
                    ToastUtils.showShort("密码不正确!");
                    break;
                case 23://更改数据库信息
                    modifyDbInformation(msg);
                    break;
            }
        }
    };

    /**
     * 提示开箱结果
     *
     * @param msg
     */
    private void promptOpenBoxResult(Message msg) {
        //得到开箱结果
        String result = (String) msg.obj;
        //消失动画
        loadingIcon.clearAnimation();
        loadingIcon.setVisibility(View.GONE);
        //提示开箱结果并写入log日志
        if (result.contains("Execption")) {
            showProgressFail("开箱失败！");
        } else {
            showProgressSuccess("开箱成功!");
        }
        WriteLogToFile.info("开箱结果:" + result);
    }

    /**
     * 提示报警结果
     *
     * @param msg
     */
    private void promptAlarmResult(Message msg) {
        String result = (String) msg.obj;
        if (!TextUtils.isEmpty(result)) {
            //提示报警结果并写入日志
            if (result.contains("error")) {
                showProgressFail("报警失败！");
            } else {
                showProgressSuccess("报警成功！");
            }
            WriteLogToFile.info("报警结果：" + result);
        }
    }

    /**
     * 修改数据库
     *
     * @param msg
     */
    private void modifyDbInformation(Message msg) {

        SysInfoBean sysInfoBean = (SysInfoBean) msg.obj;
        if (sysInfoBean != null) {
            DatabaseHelper helper = new DatabaseHelper(App.getApplication());
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("heartbeatPort", sysInfoBean.getHeartbeatPort());
            contentValues.put("heartbeatServer", sysInfoBean.getHeartbeatServer());
            contentValues.put("alertPort", sysInfoBean.getAlertPort());
            contentValues.put("alertServer", sysInfoBean.getAlertServer());
            contentValues.put("sipUsername", sysInfoBean.getDeviceName());
            contentValues.put("sipNumber", sysInfoBean.getSipUsername());
            contentValues.put("sipPassword", sysInfoBean.getSipPassword());
            contentValues.put("sipServer", sysInfoBean.getSipServer());
            contentValues.put("deviceGuid", sysInfoBean.getDeviceGuid());
            contentValues.put("deviceName", sysInfoBean.getDeviceName());
            contentValues.put("neighborWatchPort", sysInfoBean.getNeighborWatchPort());
            db.update("users", contentValues, "userName = ? and userPwd = ?", new String[]{AppConfig.USERNAME, AppConfig.PWD});
            db.close();
            Logutils.i("数据更新成功");


            Logutils.i(DbConfig.getInstance().getData(-1) + "id");
            Logutils.i(DbConfig.getInstance().getData(0) + "登录时间");
            Logutils.i(DbConfig.getInstance().getData(1) + "当前帐号");
            Logutils.i(DbConfig.getInstance().getData(2) + "当前密码");
            Logutils.i(DbConfig.getInstance().getData(3) + "前登录 cms端口");
            Logutils.i(DbConfig.getInstance().getData(4) + "cms地址");
            Logutils.i(DbConfig.getInstance().getData(5) + "心跳端口");
            Logutils.i(DbConfig.getInstance().getData(6) + "心跳地址");
            Logutils.i(DbConfig.getInstance().getData(7) + "报警端口");
            Logutils.i(DbConfig.getInstance().getData(8) + "报警地址");
            Logutils.i(DbConfig.getInstance().getData(9) + "sip用户名");
            Logutils.i(DbConfig.getInstance().getData(10) + "sip号码");
            Logutils.i(DbConfig.getInstance().getData(11) + "sip密码");
            Logutils.i(DbConfig.getInstance().getData(12) + "sip服务器地址");
            Logutils.i(DbConfig.getInstance().getData(13) + "设备的guid");
            Logutils.i(DbConfig.getInstance().getData(14) + "设备名称");
            Logutils.i(DbConfig.getInstance().getData(15) + "友邻哨端口");
        }
    }

    /**
     * 返回键屏蔽
     */
    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}

