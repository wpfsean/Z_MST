package com.tehike.client.mst.app.project.ui.landactivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.adapters.SipGroupAdapter;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.cmscallbacks.SipGroupResourcesCallback;
import com.tehike.client.mst.app.project.entity.SipGroupBean;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.SipManager;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.services.BatteryAndWifiCallback;
import com.tehike.client.mst.app.project.services.BatteryAndWifiService;
import com.tehike.client.mst.app.project.utils.BatteryUtils;
import com.tehike.client.mst.app.project.utils.GsonUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;

import org.linphone.core.LinphoneChatRoom;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：横屏Sip分组页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/23 9:45
 */

public class LandSipGroupActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

    /**
     * 展示列表的recyclerview
     */
    @BindView(R.id.sip_group_recyclearview)
    public RecyclerView promptSipGroupItemView;

    /**
     * 下拉 刷新控件
     */
    @BindView(R.id.sipgroup_refresh_layout_land)
    SwipeRefreshLayout sipGroupRefreshView;

    /**
     * 信号强度
     */
    @BindView(R.id.icon_network)
    ImageView rssiIcon;

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
     * 消息图标
     */
    @BindView(R.id.icon_message_show)
    ImageView messageIcon;

    /**
     * 当前电量显示
     */
    @BindView(R.id.prompt_electrity_values_land_layout)
    TextView displayCurrentBattery;

    /**
     * 连接状态
     */
    @BindView(R.id.icon_connection_show)
    ImageView connetIcon;

    /**
     * 存放SipGroup信息的集合
     */
    List<SipGroupBean> sipGroupResources = null;

    /**
     * 线程是否正在运行
     */
    boolean threadIsRun = true;


    @Override
    protected int intiLayout() {
        return R.layout.activity_land_sip_group;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        //初始化下拉刷新控件
        initializeRefreshView();

        //显示时间
        initializeTime();

        //获取sip分组数据
        getSipGroupDataFromCms();
    }

    /**
     * 初始化显示时间及日期
     */
    private void initializeTime() {
        //刷新时间的线程
        TimingThread timeThread = new TimingThread();
        new Thread(timeThread).start();

        //显示当前的年月日
        SimpleDateFormat dateD = new SimpleDateFormat("yyyy年MM月dd日");
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        currentYearLayout.setText(dateD.format(date).toString());

        //显示当前的电量
        int electricityValues = BatteryUtils.getSystemBattery(App.getApplication());
        displayCurrentBattery.setText(electricityValues + "");
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
                handler.sendEmptyMessage(8);
            } while (threadIsRun);
        }
    }

    /**
     * 显示当前的时间
     */
    private void displayCurrentTime() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat timeD = new SimpleDateFormat("HH:mm:ss");
        String currentTime = timeD.format(date).toString();
        if (!TextUtils.isEmpty(currentTime)) {
            currentTimeLayout.setText(currentTime);
        }

    }

    /**
     * 初始化下拉刷新控件
     */
    private void initializeRefreshView() {
        //设置下拉 颜色
        sipGroupRefreshView.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        //设置下拉 刷新
        sipGroupRefreshView.setOnRefreshListener(this);
    }

    /**
     * CMS获取Sip分组信息
     */
    private void getSipGroupDataFromCms() {
        //盛放sip数据的集合
        sipGroupResources = new ArrayList<>();
        //清除集合数据
        if (sipGroupResources != null && sipGroupResources.size() > 0) {
            sipGroupResources.clear();
        }
        //判断 网络
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(17);
            return;
        }
        //请求数据
        SipGroupResourcesCallback requestThread = new SipGroupResourcesCallback(new SipGroupResourcesCallback.SipGroupDataCallback() {
            @Override
            public void callbackSuccessData(List<SipGroupBean> mList) {
                //判断请求的数据是否为空
                if (mList == null) {
                    handler.sendEmptyMessage(2);
                    return;
                }
                //每次获取到的Sip组数据保存到本地

                String sipGroupDataStr =   GsonUtils.GsonString(mList);
                if (!TextUtils.isEmpty(sipGroupDataStr))
                    SharedPreferencesUtils.putObject(LandSipGroupActivity.this, "sipGroupDataStr", sipGroupDataStr);

                sipGroupResources = mList;
                handler.sendEmptyMessage(5);
            }
        });
        requestThread.start();
    }

    /**
     * 适配数据器
     */
    private void initSipGroupAdapter() {
        if (!isVisible) {
            return;
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(LandSipGroupActivity.this, 3);
        gridLayoutManager.setReverseLayout(false);
        gridLayoutManager.setOrientation(GridLayout.VERTICAL);
        promptSipGroupItemView.setLayoutManager(gridLayoutManager);
        SipGroupAdapter adapter = new SipGroupAdapter(LandSipGroupActivity.this, sipGroupResources);
        promptSipGroupItemView.setAdapter(adapter);

        adapter.setItemClickListener(new SipGroupAdapter.MyItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                SipGroupBean sipGroupBean = sipGroupResources.get(position);
                if (sipGroupBean != null) {
                    int group_id = sipGroupBean.getGroup_id();
                    Intent intent = new Intent();
                    intent.putExtra("group_id", group_id);
                    intent.setClass(LandSipGroupActivity.this, LandSipInforActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * 下拉刷新事件
     */
    @Override
    public void onRefresh() {
        //利用子线程延迟两秒后重新刷新数据
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sipGroupRefreshView != null)
                    sipGroupRefreshView.setRefreshing(false);
                getResources();

            }
        }, 2 * 1000);
    }

    /**
     * 下拉刷新数据
     */
    private void refreshData() {
        //显示正在刷新
        sipGroupRefreshView.setRefreshing(true);
        //延迟两秒后停止刷新 并获取新的数据
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sipGroupRefreshView != null)
                    //提示刷新结果
                    handler.sendEmptyMessage(19);
                sipGroupRefreshView.setRefreshing(false);
                getResources();

            }
        }, 2 * 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();

        disPlayAppStatusIcon();
    }

    /**
     * 显示当前app的状态图标
     */
    private void disPlayAppStatusIcon() {
        //电量回调
        BatteryAndWifiService.addBatterCallback(new BatteryAndWifiCallback() {
            @Override
            public void getBatteryData(int level) {

                Message message = new Message();
                message.arg1 = level;
                message.what = 23;
                handler.sendMessage(message);
            }
        });
        //信息状态
        int rssi = AppConfig.DEVICE_WIFI;
        if (rssi > -50 && rssi < 0) {
            updateUi(rssiIcon, R.mipmap.icon_network);
        } else if (rssi > -70 && rssi <= -50) {
            updateUi(rssiIcon, R.mipmap.icon_network_a);
        } else if (rssi < -70) {
            updateUi(rssiIcon, R.mipmap.icon_network_b);
        } else if (rssi == -200) {
            updateUi(rssiIcon, R.mipmap.icon_network_disable);
        }

        if (SipService.isReady() || SipManager.isInstanceiated()) {
            LinphoneChatRoom[] rooms = SipManager.getLc().getChatRooms();
            String nativeSipNumber = AppConfig.SIP_NUMBER;
            if (TextUtils.isEmpty(nativeSipNumber)) {
                return;
            }
            for (int j = 0; j < rooms.length; j++) {
                int unRead = rooms[j].getUnreadMessagesCount();
                Logutils.i("unRead:" + unRead);
                if (unRead > 0)
                    handler.sendEmptyMessage(16);
                else
                    handler.sendEmptyMessage(15);
            }
        }

        if (AppConfig.SIP_STATUS) {
            handler.sendEmptyMessage(3);
        } else {
            handler.sendEmptyMessage(4);
        }
    }


    /**
     * 加载本地保存的sip分组数据
     *
     */
    private void loadLocalData() {
        //提示未加载到新数据
        showProgressFail("未请求到最新数据！");
        //取出本地保存的数据
        String dataSources = (String) SharedPreferencesUtils.getObject(LandSipGroupActivity.this, "sipGroupDataStr", "");
        if (TextUtils.isEmpty(dataSources)) {
            return;
        }
        //字符串转成集合
        List<SipGroupBean> alterSamples = GsonUtils.GsonToList(dataSources,SipGroupBean.class);
        //判断集合数据是否为空
        if (alterSamples != null) {
            if (sipGroupResources != null && sipGroupResources.size() > 0)
                sipGroupResources.clear();
            sipGroupResources = alterSamples;
            initSipGroupAdapter();
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
     * 网络状态回调
     *
     * @param state
     * @param name
     */
    @Override
    public void onNetChange(int state, String name) {
        if (state == -1 || state == 5) {
            //提示无网络
            handler.sendEmptyMessage(17);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止刷新时间线程
        threadIsRun = false;
        //移除所有的handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    /**
     * 右侧功能按键的点击事件
     *
     * @param view
     */
    @OnClick({R.id.remote_warning_layou, R.id.remote_gunshoot_layou, R.id.remote_speaking_layou,
            R.id.instant_message_layout, R.id.voice_intercom_icon_layout, R.id.video_intercom_layout, R.id.sip_group_refresh_layout, R.id.sip_group_finish_icon})
    public void onclickEvent(View view) {
        switch (view.getId()) {
            case R.id.voice_intercom_icon_layout:
                if (isVisible)
                    showProgressFail("暂不支持!");
                break;
            case R.id.video_intercom_layout:
                if (isVisible)
                    showProgressFail("暂不支持!");
                break;
            case R.id.instant_message_layout:
                if (isVisible)
                    showProgressFail("暂不支持!");
                break;
            case R.id.remote_warning_layou:
                if (isVisible)
                    showProgressFail("暂不支持!");
                break;
            case R.id.remote_gunshoot_layou:
                if (isVisible)
                    showProgressFail("暂不支持!");
                break;
            case R.id.remote_speaking_layou:
                if (isVisible)
                    showProgressFail("暂不支持!");
                break;
            case R.id.sip_group_refresh_layout:
                refreshData();
                break;
            case R.id.sip_group_finish_icon:
                LandSipGroupActivity.this.finish();
                break;
        }
    }


    /**
     * Handler处理子线程发送的消息
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2://未获取到数据时加载本地数据
                    if (isVisible)
                        loadLocalData();
                    break;
                case 3://提示sip连接状态正常
                    if (isVisible)
                        connetIcon.setBackgroundResource(R.mipmap.icon_connection_normal);
                    break;
                case 4://提示sip连接状态断开
                    if (isVisible)
                        connetIcon.setBackgroundResource(R.mipmap.icon_connection_disable);
                    break;
                case 5://初始化sip组适配器
                    initSipGroupAdapter();
                    break;
                case 6://提示未获取到值班室信息
                    if (isVisible)
                        showProgressFail("未获取到值班室信息");
                    break;
                case 7://提示值班室不在线
                    if (isVisible)
                        showProgressFail("对方不在线！");
                    break;
                case 8://刷新显示当前的时间
                    if (isVisible)
                        displayCurrentTime();
                    break;
                case 15://新消息提示消除
                    updateUi(messageIcon, R.mipmap.message);
                    break;
                case 16://提示新消息
                    updateUi(messageIcon, R.mipmap.newmessage);
                    break;
                case 17://提示网络异常
                    if (isVisible)
                        showProgressFail("请检查网络状态！");
                    break;
                case 19://提示数据刷新成功
                    if (isVisible)
                        showProgressSuccess("刷新成功!");
                    break;
                case 23://显示当前回调的电量数据
                    int level = msg.arg1;
                    if (isVisible)
                        displayCurrentBattery.setText("" + level);
                    break;

            }
        }
    };
}
