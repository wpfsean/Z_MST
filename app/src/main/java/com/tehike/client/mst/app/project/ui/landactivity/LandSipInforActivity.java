package com.tehike.client.mst.app.project.ui.landactivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.cmscallbacks.RequestSipSourcesThread;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.SipBean;
import com.tehike.client.mst.app.project.entity.SipClient;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.Linphone;
import com.tehike.client.mst.app.project.linphone.MessageCallback;
import com.tehike.client.mst.app.project.linphone.PhoneCallback;
import com.tehike.client.mst.app.project.linphone.RegistrationCallback;
import com.tehike.client.mst.app.project.linphone.SipManager;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.onvif.Device;
import com.tehike.client.mst.app.project.services.BatteryAndWifiCallback;
import com.tehike.client.mst.app.project.services.BatteryAndWifiService;
import com.tehike.client.mst.app.project.utils.ActivityUtils;
import com.tehike.client.mst.app.project.utils.BatteryUtils;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.G711Utils;
import com.tehike.client.mst.app.project.utils.ContextUtils;
import com.tehike.client.mst.app.project.utils.GsonUtils;
import com.tehike.client.mst.app.project.utils.HttpUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.RemoteVoiceRequestUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.TimeDo;
import com.tehike.client.mst.app.project.utils.TimeUtils;
import com.tehike.client.mst.app.project.utils.ToastUtils;
import com.tehike.client.mst.app.project.utils.UIUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：Sip组显示页面
 * Cms获取Sip分组数据
 * <p>
 * 本功能思路：
 * 根据传递的组Id,从cms上拿回某个组内的sip数据
 * 再通过webapi接口拿回sip状态信息，组合数据并刷新
 * 其次通过定时线程任务池每隔三秒刷新下数据
 * <p>
 * 本页面有未读消息处理
 * <p>
 * 远程喊话
 * 远程警告
 * 远程鸣枪
 * 等回调状态提示
 * <p>
 * 最后是语音电话、视频电话处理
 * <p>
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/23 9:50
 */

public class LandSipInforActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

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
     * 下拉 刷新控件
     */
    @BindView(R.id.sipinfor_refresh_layout_land)
    SwipeRefreshLayout topRefreshView;

    /**
     * 网络图标
     */
    @BindView(R.id.icon_network)
    ImageView networkIcon;

    /**
     * sip连接状态
     */
    @BindView(R.id.icon_connection_show)
    ImageView connetIcon;

    /**
     * 消息图标
     */
    @BindView(R.id.icon_message_show)
    ImageView messageIcon;

    /**
     * 显示当前的电量值
     */
    @BindView(R.id.prompt_electrity_values_land_layout)
    TextView displayCurrentBattery;

    /**
     * 定时器
     */
    Timer timer = null;

    /**
     * gridview列表
     */
    @BindView(R.id.gridview)
    public GridView gridview;

    /**
     * 上下文
     */
    Context mContext;

    /**
     * 存放 minisip上数据的集合
     */
    List<SipClient> mList = new ArrayList<>();

    /**
     * 存放 cms获取数据的集合
     */
    List<SipBean> sipListResources = new ArrayList<>();

    /**
     * 展示的数据
     */
    List<SipClient> adapterList = new ArrayList<>();

    /**
     * 适配器
     */
    SipInforAdapter sipAdapter = null;

    /**
     * 当前选项的id
     */
    int selected = -1;

    /**
     * 发送录音数据 的udp Socket
     */
    DatagramSocket udpSendSocket = null;

    /**
     * 发送请求喊话的tcp Socket
     */
    Socket tcpSocket = null;

    /**
     * 解析返回要发送数据 的Port
     */
    int port = -1;

    /**
     * 线程是否正在运行
     */
    boolean threadIsRun = true;


    /**
     * 声音采集的采样率
     */
    public int frequency = 16000;

    /**
     * 声音采集的缓存量
     */
    private int recordBuffterSize;

    /**
     * 录音 对象
     */
    private AudioRecord recorder;

    /**
     * 录音 线程
     */
    private RecordingVoiceThread mRecordingVoiceThread;

    /**
     * 停止录音 的标识
     */
    private boolean stopRecordingFlag = false;

    /**
     * 要操作对象的ip
     */
    String remoteDeviceIp = "";


    /**
     * webapi请求sip状态的参数
     */
    String webAPi_SipStatus = "";

    @Override
    protected int intiLayout() {
        return R.layout.activity_land_sip_infor;
    }


    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        //初始化请求参数
        initializeParamter();

        //初始化下拉刷新组件
        initializeRefreshView();

        //初始化录音参数
        initializeRecordParamater();

        //显示时间
        initializeTime();

        //初始加载数据
        initializeData();
    }

    /**
     * 初始化数据参数
     */
    private void initializeParamter() {
        //上下文
        mContext = this;
        //获取服务器的Ip地址
        String serverIp = DbConfig.getInstance().getData(4);
        if (TextUtils.isEmpty(serverIp))
            serverIp = AppConfig.SERVERIP;
        //拉回 webapi接口地址
        webAPi_SipStatus = "http://" + serverIp + ":8080/webapi/sipstatus";
    }

    /**
     * 加载数据
     */
    private void initializeData() {
        //获取组id
        final int groupID = getIntent().getIntExtra("group_id", 0);

        //根据组id加载组内资源
        if (groupID != 0) {
            //先判断网络是否通
            if (NetworkUtils.isConnected()) {
                //子线程去请求
                RequestSipSourcesThread requestSipSourcesThread = new RequestSipSourcesThread(mContext, groupID + "", new RequestSipSourcesThread.SipListern() {
                    @Override
                    public void getDataListern(List<SipBean> sipList) {
                        //判断是否请求到数据
                        if (sipList != null && sipList.size() > 0) {
                            //在cms数据内剔除自己
                            for (SipBean sipBean : sipList) {
                                if (sipBean.getNumber().equals(AppConfig.SIP_NUMBER)) {
                                    sipList.remove(sipBean);
                                }
                            }
                            //剔除自己后的数据
                            sipListResources = sipList;

                            //请求sip状态
                            initWebApiSipStatusData();
                        } else {
                            //未请求到数据提示并写入Log日志
                            handler.sendEmptyMessage(7);
                            WriteLogToFile.info("未加载到sip组数据，组id" + groupID);
                        }
                    }
                });
                //执行子线程
                requestSipSourcesThread.start();
            } else {
                //没网提示
                handler.sendEmptyMessage(8);
            }
        } else {
            //求获取到传过来的组ID
            handler.sendEmptyMessage(9);
        }


        //通过子线程延迟两秒后再定时刷新 minisip上的数据（防止 数据重复出现 ）
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //启动定时刷新任务
                timingRefreshData();
            }
        }).start();
    }

    /**
     * 显示当前的时间
     */
    private void initializeTime() {

        //运行时间线程每隔1秒刷新一下时间显示
        TimingThread timeThread = new TimingThread();
        new Thread(timeThread).start();

        //显示当前的年月日
        SimpleDateFormat dateD = new SimpleDateFormat("yyyy年MM月dd日");
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        currentYearLayout.setText(dateD.format(date).toString());

        //显示当前的电量值
        int electricityValues = BatteryUtils.getSystemBattery(App.getApplication());
        displayCurrentBattery.setText(electricityValues + "");
    }

    /**
     * 通过webapi接口查看当前所有sip信息的状态
     */
    private void initWebApiSipStatusData() {

        //启动子线程去请求数据
        HttpUtils sipHttpUtils = new HttpUtils(webAPi_SipStatus, new HttpUtils.GetHttpData() {
            @Override
            public void httpData(String result) {
                //判断未请求到数据
                if (TextUtils.isEmpty(result)) {
                    handler.sendEmptyMessage(4);
                    return;
                }
                //请求数据是否异常
                if (result.contains("Execption") || result.contains("code != 200")) {
                    handler.sendEmptyMessage(5);
                    return;
                }
                try {
                    //json解析请求到的数据
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        //解析sip号码和sip状态
                        String username = jsonObject.getString("usrname");
                        String state = jsonObject.getString("state");
                        SipClient sipClient = new SipClient(username, "", "", "", state, "", "");
                        mList.add(sipClient);
                    }
                    //如果集合不为空，下一步处理
                    if (mList != null && mList.size() > 0)
                        handler.sendEmptyMessage(6);
                } catch (Exception e) {
                    Logutils.e("json解析error:" + e.getMessage());
                }
            }
        });
        sipHttpUtils.start();
    }

    /**
     * 初始化配置下拉刷新组件
     */
    private void initializeRefreshView() {
        //设置下拉 颜色
        topRefreshView.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        //设置下拉 刷新
        topRefreshView.setOnRefreshListener(this);
    }

    /**
     * 跳转聊天
     */
    private void goChatPage() {
        if (selected != -1) {
            SipClient mSipClient = adapterList.get(selected);
            //判断当前选中的对象是否空
            if (mSipClient != null) {
                String currentName = mSipClient.getUsrname();
                //使未读消息状态为为已读
                if (SipService.isReady() || SipManager.isInstanceiated()) {
                    LinphoneChatRoom[] rooms = SipManager.getLc().getChatRooms();
                    if (rooms.length > 0) {
                        for (LinphoneChatRoom room : rooms) {
                            //使未读消息为空已读
                            if (room.getPeerAddress().getUserName().equals(currentName)) {
                                room.markAsRead();
                                handler.sendEmptyMessage(15);
                            }
                        }
                    }
                }
            }
            //跳转页面并传递数据
            if (mSipClient != null) {
                Intent intent = new Intent();
                intent.setClass(LandSipInforActivity.this, LandChatActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("sipclient", mSipClient);
                intent.putExtras(bundle);
                LandSipInforActivity.this.startActivity(intent);
            }
        }
    }

    /**
     * 远程 喊话
     */
    private void remoteSpeaking() {

        //判断当前的对象是否是移动终端
        if (adapterList != null && adapterList.size() > 0) {
            //获取当前对象的设备类型
            String deviceType = adapterList.get(selected).getDispname();
            //判断是移动端就不支持
            if (!TextUtils.isEmpty(deviceType)) {
                if (deviceType.equals("TH-C6000")) {
                    handler.sendEmptyMessage(20);
                    return;
                }
            }
        }
        //判断udp对象是否为空
        try {
            if (udpSendSocket == null)
                udpSendSocket = new DatagramSocket();
        } catch (SocketException e) {
            Logutils.i("Socket error:" + e.getMessage());
        }

        //判断当前对象是否选中
        if (selected != -1) {
            remoteDeviceIp = adapterList.get(selected).getAddr();
            if (!TextUtils.isEmpty(remoteDeviceIp)) {
                //先用tcp去请求喊话操作
                RequestRemoteSpeakTcp sendSoundData = new RequestRemoteSpeakTcp();
                new Thread(sendSoundData).start();
            }
        }
    }

    /**
     * 远程鸣枪或警告操作
     */
    private void remoteDevice(int i) {
        if (selected != -1) {

            if (adapterList == null || adapterList.size() == 0) {
                return;
            }
            //获取选中对象的ip地址
            remoteDeviceIp = adapterList.get(selected).getAddr();

            //获取当前对象的设备类型
            String deviceType = adapterList.get(selected).getDispname();
            //判断是移动端就不支持
            if (!TextUtils.isEmpty(deviceType)) {
                if (deviceType.equals("TH-C6000")) {
                    handler.sendEmptyMessage(20);
                    return;
                }
            }

            //判断ip是否为空
            if (!TextUtils.isEmpty(remoteDeviceIp)) {
                //启动线程去做远程操作（鸣枪，警告）
                RemoteVoiceRequestUtils remoteVoiceRequestUtils = new RemoteVoiceRequestUtils(i, remoteDeviceIp, new RemoteVoiceRequestUtils.RemoteCallbck() {
                    @Override
                    public void remoteStatus(String status) {
                        //返回状态处理
                        if (!TextUtils.isEmpty(status)) {
                            Message message = new Message();
                            message.what = 12;
                            message.obj = status;
                            handler.sendMessage(message);
                        }
                    }
                });
                new Thread(remoteVoiceRequestUtils).start();
            }
        }
    }

    /**
     * 拨打电话操作
     */
    private void makingCall(int i) {
        if (selected != -1) {
            //获取选中的对象
            SipClient mSipClient = adapterList.get(selected);
            //判断对象是为空
            if (mSipClient != null) {
                //判断自身sip状态信息
                if (!AppConfig.SIP_STATUS) {
                    ToastUtils.showShort("sip还未注册");
                    return;
                }
                Intent intent = new Intent();
                //判断是语音电话还是视频电话
                if (i == 0) {
                    intent.putExtra("isMakingVideoCall", false);
                    Linphone.callTo(mSipClient.getUsrname(), false);

                } else if (i == 1) {
                    intent.putExtra("isMakingVideoCall", true);
                    Linphone.callTo(mSipClient.getUsrname(), true);
                }
                //开启（声音）外放
                Linphone.toggleSpeaker(true);
                //传递数据并跳转
                intent.putExtra("callerNumber", mSipClient.getUsrname());
                intent.putExtra("isMakingCall", true);
                intent.setClass(LandSipInforActivity.this, LandSingleCallActivity.class);
                startActivity(intent);
            }
        }
    }

    /**
     * 下拉刷新数据
     */
    private void initRefreshData() {
        //显示下拉刷新
        topRefreshView.setRefreshing(true);
        //利用handler延时两秒后停止刷新并重新加载数据
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (topRefreshView != null)
                    topRefreshView.setRefreshing(false);
                getResources();
                handler.sendEmptyMessage(11);

            }
        }, 2 * 1000);
    }

    /**
     * 关闭本页面
     */
    private void finishNativePage() {
        //取消定时任务
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        //停止计时
        threadIsRun = false;

        //移除handler监听回调
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        //finishi本页面
        ActivityUtils.removeActivity(this);
        LandSipInforActivity.this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //更改图标
        upDateAppStatusIcon();

    }

    /**
     * 显示App状态
     */
    private void upDateAppStatusIcon() {
        //电量信息

        BatteryAndWifiService.addBatterCallback(new BatteryAndWifiCallback() {
            @Override
            public void getBatteryData(int level) {

                Message message = new Message();
                message.arg1 = level;
                message.what = 23;
                handler.sendMessage(message);
            }
        });

        //wifi信息
        int rssi = AppConfig.DEVICE_WIFI;
        if (rssi > -50 && rssi < 0) {
            updateUi(networkIcon, R.mipmap.icon_network);
        } else if (rssi > -70 && rssi <= -50) {
            updateUi(networkIcon, R.mipmap.icon_network_a);
        } else if (rssi < -70) {
            updateUi(networkIcon, R.mipmap.icon_network_b);
        } else if (rssi == -200) {
            updateUi(networkIcon, R.mipmap.icon_network_disable);
        }

        //Sip注册状态
        if (AppConfig.SIP_STATUS) {
            handler.sendEmptyMessage(3);
        } else {
            handler.sendEmptyMessage(4);
        }

        //状态和电话监听回调
        if (SipService.isReady()) {
            Linphone.addCallback(new RegistrationCallback() {
                @Override
                public void registrationOk() {
                    handler.sendEmptyMessage(3);
                }

                @Override
                public void registrationFailed() {
                    handler.sendEmptyMessage(4);
                }
            }, new PhoneCallback() {
                @Override
                public void incomingCall(LinphoneCall linphoneCall) {
                    super.incomingCall(linphoneCall);
                }

                @Override
                public void outgoingInit() {
                    super.outgoingInit();
                }

                @Override
                public void callConnected() {
                    super.callConnected();
                }

                @Override
                public void callEnd() {
                    super.callEnd();
                }

                @Override
                public void callReleased() {
                    super.callReleased();
                }

                @Override
                public void error() {
                    super.error();
                }
            });
        }
        //如果当前页面有新消息时， 更改消息提示图标
        if (SipService.isReady() || SipManager.isInstanceiated()) {
            //消息回调，收到新消息时就更改消息提示图标
            SipService.addMessageCallback(new MessageCallback() {
                @Override
                public void receiverMessage(LinphoneChatMessage linphoneChatMessage) {
                    handler.sendEmptyMessage(16);
                }
            });
        }
    }

    /**
     * 屏蔽返回键盘
     */
    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    /**
     * 显示当前的时间
     */
    private void displayCurrentTime() {
        //显示当前 的时分秒
        Date date = new Date();
        SimpleDateFormat timeD = new SimpleDateFormat("HH:mm:ss");
        String currentTime = timeD.format(date).toString();
        if (!TextUtils.isEmpty(currentTime)) {
            currentTimeLayout.setText(currentTime);
        }
    }

    /**
     * 更新UI
     */
    public void updateUi(final ImageView imageView, final int n) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setBackgroundResource(n);
            }
        });
    }


    /**
     * 显示喊话时长
     */
    TextView speaking_time = null;

    /**
     * 喊话时长
     */
    int speakingTime = 0;

    /**
     * 喊话时计线程
     */
    SpeakingTimeThread thread = null;

    /**
     * 弹出正在喊话的alert
     */
    private void disPlaySpeakingDialog() {

        //加载布局
        View view = View.inflate(LandSipInforActivity.this, R.layout.activity_port_prompt_dialog, null);
        speaking_time = view.findViewById(R.id.speaking_time_layout);
        TextView speaking_name = view.findViewById(R.id.speaking_name_layout);
        TextView close_dialog = view.findViewById(R.id.seaking_close_dialog_layout);

        //alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(LandSipInforActivity.this);
        builder.setView(view);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        //此处设置位置窗体大小
        dialog.getWindow().setLayout(ContextUtils.dip2px(this, 274), ContextUtils.dip2px(this, 276));

        //判断是否正在显示
        if (dialog.isShowing()) {

            //运行喊话计时线程
            if (thread == null)
                thread = new SpeakingTimeThread();
            new Thread(thread).start();
            //取本本地保存的sip数据
            String dataSources = (String) SharedPreferencesUtils.getObject(LandSipInforActivity.this, "sip_resources", "");
            if (TextUtils.isEmpty(dataSources)) {
                return;
            }
            //把String转成集合
            List<Device> alterSamples = GsonUtils.GsonToList(dataSources, Device.class);
            SipClient mSipClient = adapterList.get(selected);

            String callNumber = "";
            //判断对象是为空
            if (mSipClient != null) {
                callNumber = mSipClient.getUsrname();
            }

            //根据选中的sip号码，显示当前的设备名称
            if (alterSamples != null && alterSamples.size() > 0) {
                for (Device device : alterSamples) {
                    if (device.getSipNum().equals(callNumber)) {
                        String deviceName = device.getName();
                        //显示名称
                        speaking_name.setText("正在向:" + deviceName + "远程喊话");
                        break;
                    }
                }
            }
        }
        //关闭监听
        close_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                speakingTime = 0;
                try {
                    stopRecord();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * 合并数据并刷新 适配器
     */
    private void mergeData() {
        if (adapterList != null)
            adapterList.clear();

        for (int i = 0; i < mList.size(); i++) {
            for (int j = 0; j < sipListResources.size(); j++) {
                if (mList.get(i).getUsrname().equals(sipListResources.get(j).getNumber())) {
                    SipClient sipClient = new SipClient();
                    sipClient.setState(mList.get(i).getState());
                    sipClient.setUsrname(mList.get(i).getUsrname());
                    sipClient.setDeviceName(sipListResources.get(j).getName());
                    sipClient.setAddr(sipListResources.get(j).getIp());
                    sipClient.setDispname(sipListResources.get(j).getDeviceType());
                    adapterList.add(sipClient);
                }
            }
        }

        if (adapterList != null && adapterList.size() > 0) {
            if (isVisible) {
                if (sipAdapter != null)
                    sipAdapter = null;
                sipAdapter = new SipInforAdapter(mContext);
                gridview.setAdapter(sipAdapter);
                gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (sipAdapter != null) {
                            Logutils.i("position" + position);
                            if (adapterList != null && adapterList.size() > 0) {
                                //判断选中的是否是在线状态对象
                                if (adapterList.get(position).getState().equals("1")) {
                                    if (!adapterList.get(position).getUsrname().equals(AppConfig.SIP_NUMBER)) {
                                        remoteDeviceIp = adapterList.get(position).getAddr();
                                        //callNumber = adapterList.get(position).getUsrname();
                                        Logutils.i("remoteIp" + remoteDeviceIp);
                                        selected = position;
                                    }
                                } else {
                                    selected = -1;
                                }
                                Logutils.i("selected---->>" + selected);
                                sipAdapter.setSeclection(position);
                                sipAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * 定时刷新minisip数据（3秒刷新一次）
     */
    private void timingRefreshData() {
        //启动定时任务线程池
        TimeDo.getInstance().init(mContext, 3 * 1000);
        TimeDo.getInstance().start();
        //回调返回结果
        TimeDo.getInstance().setListern(new TimeDo.Callback() {
            @Override
            public void resultCallback(String result) {
                //清除盛放前一次请求数据的集合
                if (mList != null)
                    mList.clear();
                //判断是否请求到了数据
                if (TextUtils.isEmpty(result)) {
                    handler.sendEmptyMessage(4);
                    return;
                }
                //判断请求到的数据是否是异常的
                if (result.contains("Execption") || result.contains("code != 200")) {
                    handler.sendEmptyMessage(5);
                    return;
                }
                //json去解析数据
                try {
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        //获取返回的sip号码和状态并添加到集合内
                        String username = jsonObject.getString("usrname");
                        String state = jsonObject.getString("state");
                        SipClient sipClient = new SipClient(username, "", "", "", state, "", "");
                        mList.add(sipClient);
                    }
                    //清除要适配时的集合数据
                    if (adapterList != null)
                        adapterList.clear();

                    //根据web接口返回的sip状态和cms返回的sip数据相结合，生成要适配显示的数据
                    for (int i = 0; i < mList.size(); i++) {
                        for (int j = 0; j < sipListResources.size(); j++) {
                            if (mList.get(i).getUsrname().equals(sipListResources.get(j).getNumber())) {
                                SipClient sipClient = new SipClient();
                                sipClient.setState(mList.get(i).getState());
                                sipClient.setUsrname(mList.get(i).getUsrname());
                                sipClient.setDeviceName(sipListResources.get(j).getName());
                                sipClient.setAddr(sipListResources.get(j).getIp());
                                sipClient.setDispname(sipListResources.get(j).getDeviceType());
                                adapterList.add(sipClient);
                            }
                        }
                    }
                    //刷新适配器
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (sipAdapter != null)
                                sipAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    Logutils.i("异常:" + e.getMessage());
                }
            }
        });
    }

    /**
     * 展示数据的适配器
     */
    class SipInforAdapter extends BaseAdapter {

        //点击时的标识
        private int clickTemp = -1;
        //布局管理器
        private LayoutInflater layoutInflater;

        //构造函数
        public SipInforAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return adapterList.size();
        }

        @Override
        public Object getItem(int position) {
            return adapterList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setSeclection(int position) {
            clickTemp = position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.activity_land_sipstatus_item, null);
                viewHolder.item_name = (TextView) convertView.findViewById(R.id.item_name);
                viewHolder.linearLayout = convertView.findViewById(R.id.item_layout);
                viewHolder.main_layout = convertView.findViewById(R.id.sipstatus_main_layout);
                viewHolder.deviceType = convertView.findViewById(R.id.device_type_layout);
                viewHolder.new_message = convertView.findViewById(R.id.sip_item_newmessage_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            if (adapterList != null) {
                //根据不同的sip状态显示不同的提示
                if (adapterList.get(position).getState().equals("0")) {
                    viewHolder.item_name.setText(adapterList.get(position).getDeviceName());
                    viewHolder.linearLayout.setBackgroundResource(R.mipmap.btn_lixian);
                } else if (adapterList.get(position).getState().equals("1")) {
                    viewHolder.item_name.setText(adapterList.get(position).getDeviceName());
                    viewHolder.linearLayout.setBackgroundResource(R.drawable.sip_call_select_bg);
                } else if (adapterList.get(position).getState().equals("2")) {
                    viewHolder.item_name.setText(adapterList.get(position).getDeviceName());
                    viewHolder.linearLayout.setBackgroundResource(R.mipmap.btn_zhenling);
                } else if (adapterList.get(position).getState().equals("3")) {
                    viewHolder.item_name.setText(adapterList.get(position).getDeviceName());
                    viewHolder.linearLayout.setBackgroundResource(R.mipmap.btn_tonghua);
                }
            }
            /**
             * 防止数据越界
             */
            if (adapterList != null && adapterList.size() > 0) {
                if (position < adapterList.size()) {
                    //根据设备型号显示设备类型
                    SipClient mSipBean = adapterList.get(position);
                    if (mSipBean != null) {
                        //获取当前显示对象的型号
                        String type = mSipBean.getDispname();
                        if (!TextUtils.isEmpty(type)) {
                            if (type.equals("TH-C6000")) {
                                viewHolder.deviceType.setText("移动终端");
                            }
                            if (type.equals("TH-S6100")) {
                                viewHolder.deviceType.setText("哨位终端");
                            }
                            if (type.equals("TH-S6200")) {
                                viewHolder.deviceType.setText("值班终端");
                            }
                        }
                    }
                }
            }

            /**
             * 显示未读图标
             */
            if (adapterList != null) {
                //获取当前的单个对象
                SipClient mSipClient = adapterList.get(position);
                if (mSipClient != null) {
                    String currentName = mSipClient.getUsrname();
                    //判断sip服役是否开启
                    if (SipManager.isInstanceiated() || SipService.isReady()) {
                        //获取所有的聊天室
                        LinphoneChatRoom[] rooms = SipManager.getLc().getChatRooms();
                        if (rooms.length > 0) {
                            //遍历聊天室
                            for (LinphoneChatRoom room : rooms) {
                                if (room != null) {
                                    //获取每个聊天室对号码（对方 ）
                                    String fromUserName = room.getPeerAddress().getUserName();
                                    //判断当前对象号码和聊天室对方是否是同一个人
                                    if (fromUserName.equals(currentName)) {
                                        //判断本机与对方是否存在 未读消息
                                        int unReadCount = room.getUnreadMessagesCount();
                                        //如果有未读消息，更改图标
                                        if (unReadCount > 0) {
                                            viewHolder.new_message.setVisibility(View.VISIBLE);
                                            viewHolder.new_message.setBackgroundResource(R.mipmap.newmessage3);
                                        } else {
                                            viewHolder.new_message.setVisibility(View.GONE);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            /**
             * 如果 是在线并且不是自己的，点击时有背景选中效果
             */
            if (clickTemp == position) {
                //只有在线的设备才能被选中
                if (adapterList.get(position).getState().equals("1"))
                    viewHolder.main_layout.setBackgroundResource(R.drawable.sip_selected_bg);
            } else {
                viewHolder.main_layout.setBackgroundColor(Color.TRANSPARENT);
            }
            return convertView;
        }

        /**
         * 内部 类
         */
        class ViewHolder {
            //当前设备名称
            TextView item_name;
            //item布局
            LinearLayout main_layout;
            RelativeLayout linearLayout;
            //设备类型（哨位终端，移动终端，值班终端,详见文档）
            TextView deviceType;
            //新消息提示图标
            ImageView new_message;
        }
    }

    /**
     * 先用tcp去请求喊话操作
     * <p>
     * 对方返回同意后的再操作
     */
    class RequestRemoteSpeakTcp extends Thread {
        @Override
        public void run() {
            try {
                if (tcpSocket == null) {
                    tcpSocket = new Socket(remoteDeviceIp, AppConfig.REMOTE_PORT);
                    tcpSocket.setSoTimeout(3 * 1000);
                    byte[] requestData = new byte[4 + 4 + 4 + 4];

                    // flag
                    byte[] flag = new byte[4];
                    flag = "RVRD".getBytes();
                    System.arraycopy(flag, 0, requestData, 0, flag.length);

                    // action
                    byte[] action = new byte[4];
                    action[0] = 1;// 0無操作，1遠程喊話，2播放語音警告，3播放鳴槍警告，4遠程監聽，5單向廣播
                    action[1] = 0;
                    action[2] = 0;
                    action[3] = 0;
                    System.arraycopy(action, 0, requestData, 4, action.length);

                    // 接受喊话时=接收语音数据包的 UDP端口(测试)
                    byte[] parameter = new byte[4];
                    System.arraycopy(parameter, 0, requestData, 8, parameter.length);
                    // // 向服务器发消息
                    OutputStream os = tcpSocket.getOutputStream();// 字节输出流
                    os.write(requestData);
                    // 读取服务器返回的消息
                    InputStream in = tcpSocket.getInputStream();
                    byte[] data = new byte[20];
                    int read = in.read(data);
                    //   System.out.println("返回的數據" + Arrays.toString(data));
                    // 解析数据头
                    byte[] r_flag = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_flag[i] = data[i];
                    }
                    String r_DataFlag = new String(r_flag, "gb2312");
                    //     System.out.println("數據頭:" + new String(r_flag, "gb2312"));
                    // 解析返回的請求
                    byte[] r_quest = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_quest[i] = data[i + 4];
                    }
                    // 0無操作，1遠程喊話，2播放語音警告，3播放鳴槍警告，4遠程監聽，5單向廣播
                    int r_questCode = r_quest[0];
                    String r_questMess = RemoteVoiceRequestUtils.getMessage(r_questCode);
                    Logutils.i("发出的请求:" + r_questCode + "\t" + r_questMess);

                    // 返回的状态
                    byte[] r_status = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_status[i] = data[i + 8];
                    }
                    int r_statusCode = r_status[0];
                    String r_statusMess = RemoteVoiceRequestUtils.getStatusMessage(r_statusCode);
                    Logutils.i("应答状态:" + r_statusCode + "\t" + r_statusMess);

                    // 返回参数
                    byte[] r_paramater = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_paramater[i] = data[i + 12];
                    }
                    Logutils.i(Arrays.toString(r_paramater));
                    port = ByteUtils.bytesToInt(r_paramater, 0);

                    Logutils.i("返回状态:" + port + "\t" + r_statusMess);
                    if (r_statusMess.equals("Accept"))
                        handler.sendEmptyMessage(13);
                    else
                        handler.sendEmptyMessage(14);

                }
            } catch (Exception e) {
                //异常处理
                Logutils.e("error:" + e.getMessage());
                handler.sendEmptyMessage(18);
            }
        }
    }

    /**
     * 初始化录音参数
     */
    public void initializeRecordParamater() {

        //创建udp发送数据
        try {
            udpSendSocket = new DatagramSocket();
        } catch (SocketException e) {
            Logutils.i("Socket error:" + e.getMessage());
        }
        try {

            //获取录音缓冲区大小
            recordBuffterSize = AudioRecord.getMinBufferSize(frequency,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

            //获取录音机对象
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    frequency, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, recordBuffterSize);

        } catch (Exception e) {
            String msg = "ERROR init: " + e.getStackTrace();
            Logutils.e("error:" + msg);
        }
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        //重置停止标识
        stopRecordingFlag = false;
        //启动录音线程
        mRecordingVoiceThread = new RecordingVoiceThread();
        mRecordingVoiceThread.start();
    }

    /**
     * 结束录音
     */
    public void stopRecord() throws IOException {
        //断开tcp连接
        if (tcpSocket != null) {
            tcpSocket.close();
            tcpSocket = null;
        }
        //断开udp连接
        if (udpSendSocket != null) {
            udpSendSocket.close();
            udpSendSocket = null;
        }
        //重置停止录音 标识
        stopRecordingFlag = true;
    }

    /**
     * 录音 线程
     */
    public class RecordingVoiceThread extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                byte[] tempBuffer, readBuffer = new byte[recordBuffterSize];
                int bufResult = 0;
                recorder.startRecording();
                //循环采集声音
                while (!stopRecordingFlag) {
                    bufResult = recorder.read(readBuffer, 0, recordBuffterSize);
                    if (bufResult > 0 && bufResult % 2 == 0) {
                        tempBuffer = new byte[bufResult];
                        System.arraycopy(readBuffer, 0, tempBuffer, 0, recordBuffterSize);
                        //g711压缩
                        G711EncondeVoice(tempBuffer);
                    }
                }
                recorder.stop();
                Looper.prepare();
                Looper.loop();
            } catch (Exception e) {
                String msg = "ERROR AudioRecord: " + e.getStackTrace();
                Looper.prepare();
                Looper.loop();
            }
        }
    }

    /**
     * G711a压缩，压缩到原来的1/2
     */
    private void G711EncondeVoice(byte[] tempBuffer) {
        DatagramPacket dp = null;
        try {
            dp = new DatagramPacket(G711Utils.encode(tempBuffer), G711Utils.encode(tempBuffer).length, InetAddress.getByName(remoteDeviceIp), port);
            try {
                udpSendSocket.send(dp);
                Logutils.i("正在发送...." + Arrays.toString(G711Utils.encode(tempBuffer)) + "\n长度" + G711Utils.encode(tempBuffer).length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下拉刷新事件
     */
    @Override
    public void onRefresh() {
        //两秒后停止刷新
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (topRefreshView != null)
                    topRefreshView.setRefreshing(false);
            }
        }, 2 * 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //时间线程停止标识
        threadIsRun = false;

        //移除所有的监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }

    /**
     * 网络状态变化的回调
     *
     * @param state
     * @param name
     */
    @Override
    public void onNetChange(int state, String name) {
        if (state == -1 || state == 5) {
            //提示网络不可用
            handler.sendEmptyMessage(17);
        }
    }

    /**
     * 显示时间的线程
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
     * 喊话计时线程
     */
    class SpeakingTimeThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logutils.i("Thread error:" + e.getMessage());
                }
                handler.sendEmptyMessage(19);
            } while (!stopRecordingFlag);
        }
    }


    /**
     * 点击事件
     *
     * @param view
     */
    @OnClick({R.id.instant_message_layout, R.id.remote_speaking_layou, R.id.remote_warning_layou, R.id.remote_gunshoot_layou, R.id.voice_intercom_icon_layout, R.id.video_intercom_layout, R.id.sip_group_refresh_layout, R.id.sip_group_back_layout})
    public void onclickEvent(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.remote_speaking_layou://远程喊话
                //判断对象否选 中
                if (selected == -1) {
                    if (isVisible)
                        showProgressFail("请选择远程操作对象！");
                    return;
                }
                //执行喊话操作
                remoteSpeaking();
                break;
            case R.id.remote_warning_layou://远程警告
                //判断对象否选 中
                if (selected == -1) {
                    if (isVisible)
                        showProgressFail("请选择远程操作对象！");
                    return;
                }
                //执行远程警告操作
                remoteDevice(2);
                break;
            case R.id.remote_gunshoot_layou://远程鸣枪
                //判断对象否选 中
                if (selected == -1) {
                    if (isVisible)
                        showProgressFail("请选择远程操作对象！");
                    return;
                }
                //执行远程鸣枪操作
                remoteDevice(3);
                break;
            case R.id.instant_message_layout://即时通信
                //判断对象否选 中
                if (selected == -1) {
                    if (isVisible)
                        showProgressFail("请选择远程操作对象！");
                    return;
                }
                //跳转到聊天界面
                goChatPage();
                break;
            case R.id.voice_intercom_icon_layout://语音电话
                //判断对象否选 中
                if (selected == -1) {
                    if (isVisible)
                        showProgressFail("请指定拨打对象！");
                    return;
                }
                //拨打语音电话
                makingCall(0);
                break;
            case R.id.video_intercom_layout://视频电话
                //判断对象否选 中
                if (selected == -1) {
                    if (isVisible)
                        showProgressFail("请指定拨打对象！");
                    return;
                }
                //拨打视频电话
                makingCall(1);
                break;
            case R.id.sip_group_refresh_layout://刷新 数据
                initRefreshData();
                break;
            case R.id.sip_group_back_layout://返回按钮
                finishNativePage();
                break;
        }
    }

    /**
     * 提示远程 操作结果
     *
     * @param msg
     */
    private void promptRemoteOpera(Message msg) {
        String result = (String) msg.obj;
        //如果有异常
        if (result.contains("error")) {
            showProgressFail(UIUtils.getString(R.string.unable_connect));
        } else {
            showProgressSuccess(UIUtils.getString(R.string.play_finish));
        }
    }


    /***
     * handler处理子线程发送的消息
     *
     *
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1://显示当前的时间
                    if (isVisible)
                        displayCurrentTime();
                    break;
                case 3://显示sip连接状态正常
                    if (isVisible)
                        connetIcon.setBackgroundResource(R.mipmap.icon_connection_normal);
                    break;
                case 4://显示sip连接状态断开
                    if (isVisible)
                        connetIcon.setBackgroundResource(R.mipmap.icon_connection_disable);
                    break;
                case 6://合并数据
                    mergeData();
                    break;
                case 7://未获取到数据
                    if (isVisible)
                        ToastUtils.showShort("未获取到数据!");
                    break;
                case 8://网编状态提示
                    if (isVisible)
                        showProgressFail("请检查网络状态");
                    break;
                case 9://未获取到组id
                    if (isVisible)
                        ToastUtils.showShort("未获取到组id!");
                    WriteLogToFile.info("No Get GroupID");
                    break;
                case 10://未获取到本机ip
                    if (isVisible)
                        ToastUtils.showShort("未获取到ip");
                    break;
                case 11://提示刷新成功
                    if (isVisible)
                        ToastUtils.showShort(UIUtils.getString(R.string.refresh_success));
                    break;
                case 12://提示远程操作的结果
                    if (isVisible)
                        //判断远程警告和鸣枪返回的信息
                        promptRemoteOpera(msg);

                    break;
                case 13://显示喊话对话框和开始录音
                    if (isVisible) {
                        disPlaySpeakingDialog();
                        startRecord();
                    }
                    break;
                case 14://提示对方拒绝
                    if (isVisible)
                        showProgressFail("对方拒绝或设备忙！");
                    //对方拒绝后关闭socket客户端
                    if (tcpSocket != null) {
                        try {
                            tcpSocket.close();
                            tcpSocket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case 15://去除新消息提示
                    if (isVisible)
                        updateUi(messageIcon, R.mipmap.message);
                    break;
                case 16://新消息提示
                    if (isVisible)
                        updateUi(messageIcon, R.mipmap.newmessage);
                    break;
                case 17://网络状态异常
                    if (isVisible)
                        showProgressFail("请检查网络状态！");
                    break;
                case 18://远程操作无法连接
                    if (isVisible)
                        showProgressFail(UIUtils.getString(R.string.unable_connect));
                    if (tcpSocket != null) {
                        try {
                            tcpSocket.close();
                            tcpSocket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 19://显示喊话计时
                    speakingTime += 1;
                    String time = TimeUtils.getTime(speakingTime);
                    if (speaking_time != null)
                        if (isVisible)
                            speaking_time.setText(time);

                    break;
                case 20://提示移动端不支持
                    if (isVisible) {
                        showProgressFail("手持终端不支持");
                    }
                    break;
                case 23://显示当前 的电量 信息
                    int level = msg.arg1;
                    if (isVisible) {
                        displayCurrentBattery.setText("" + level);
                    }
                    break;
            }
        }
    };


}
