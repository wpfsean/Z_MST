package com.tehike.client.mst.app.project.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.adapters.SipGroupAdapter;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseFragment;
import com.tehike.client.mst.app.project.cmscallbacks.SipGroupResourcesCallback;
import com.tehike.client.mst.app.project.entity.SipGroupBean;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.Linphone;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.ui.portactivity.PortSingleCallActivity;
import com.tehike.client.mst.app.project.ui.portactivity.PortSipInforActivity;
import com.tehike.client.mst.app.project.utils.GsonUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.SipIsOnline;
import com.tehike.client.mst.app.project.utils.ToastUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：竖屏sip分组页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/11/2 14:58
 */

public class IntercomFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    /**
     * 显示数据 的recyclerview
     */
    @BindView(R.id.sip_group_recyclearview)
    public RecyclerView recyclearview;

    /**
     * 下拉刷新布局
     */
    @BindView(R.id.sipgrou_intercom_refreshlayout)
    SwipeRefreshLayout refreshLayout;


    /**
     * 盛放数据的集合
     */
    List<SipGroupBean> sipGroupDataList = new ArrayList<>();

    boolean currentFragmentVisiable = false;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_port_fragment_sipgroup_intercom_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //初始化下拉刷新
        initRefresh();

        //获取sip分组资源
        getSipGroupResources();

    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        Logutils.i("Sip组页面销毁");
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    /**
     * 初始化下拉刷新
     */
    private void initRefresh() {
        //下拉刷新的颜色
        refreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        //下拉监听
        refreshLayout.setOnRefreshListener(this);
    }

    /**
     * 初始化适配器
     */
    private void initAdapter() {

        dismissProgressDialog();
        SipGroupAdapter adapter = new SipGroupAdapter(getActivity(), sipGroupDataList);
        if (recyclearview != null) {
            recyclearview.setAdapter(adapter);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
            gridLayoutManager.setReverseLayout(false);
            gridLayoutManager.setOrientation(GridLayout.VERTICAL);
            recyclearview.setLayoutManager(gridLayoutManager);
            adapter.setItemClickListener(new SipGroupAdapter.MyItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    int group_id = sipGroupDataList.get(position).getGroup_id();
                    Intent intent = new Intent();
                    intent.putExtra("group_id", group_id);
                    intent.setClass(getActivity(), PortSipInforActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
            });
        }
    }

    /**
     * 向cms获取数据
     */
    private void getSipGroupResources() {

        //先清空集合内的数据
        if (sipGroupDataList != null && sipGroupDataList.size() > 0) {
            sipGroupDataList.clear();
        }
        //判断网络状态
        if (NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(1);
            //数据请求
            SipGroupResourcesCallback sipGroupResourcesCallback = new SipGroupResourcesCallback(new SipGroupResourcesCallback.SipGroupDataCallback() {
                @Override
                public void callbackSuccessData(final List<SipGroupBean> dataList) {
                    if (dataList != null && dataList.size() > 0) {
                        processingData(dataList);
                        handler.sendEmptyMessage(5);
                    } else {
                        handler.sendEmptyMessage(3);
                    }
                }
            });
            sipGroupResourcesCallback.start();
        } else {
            handler.sendEmptyMessage(2);
        }
    }

    /**
     * 处理请求到的数据
     */
    private void processingData(List<SipGroupBean> dataList) {
        sipGroupDataList = dataList;
        //获取到数据保存到本地
        String sipGroupDataStr = GsonUtils.GsonToString(sipGroupDataList);
        if (!TextUtils.isEmpty(sipGroupDataStr))
            SharedPreferencesUtils.putObject(App.getApplication(), "sipGroupDataStr", sipGroupDataStr);
    }

    @Override
    public void onNetworkViewRefresh() {
        getSipGroupResources();
    }

    /**
     * 点击返回按键
     */
    @OnClick(R.id.finish_intercom_fragment_layout)
    public void finishPage(View view) {
        getActivity().finish();
    }

    /**
     * 点击刷新按钮
     */
    @OnClick(R.id.refresh_sipgroup_btn_layout)
    public void refreshPageData(View view) {
        refreshData();
    }


    /**
     * 刷新数据
     */
    private void refreshData() {
        if (refreshLayout != null)
            refreshLayout.setRefreshing(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (refreshLayout != null)
                    refreshLayout.setRefreshing(false);
                getResources();
                handler.sendEmptyMessage(10);

            }
        }, 2 * 1000);
    }

    /**
     * 向外拨打电话（暂未使用）
     */
    public void call(int type) {
        if (TextUtils.isEmpty(AppConfig.DURY_ROOM)) {
            handler.sendEmptyMessage(6);
            return;
        }
        if (!SipService.isReady()) {
            handler.sendEmptyMessage(7);
            return;
        }


        if (!AppConfig.SIP_STATUS) {
            handler.sendEmptyMessage(7);
            return;
        }
        if (!SipIsOnline.isOnline(AppConfig.DURY_ROOM)) {
            handler.sendEmptyMessage(9);
            return;
        }

        Intent intent = new Intent();
        intent.setClass(getActivity(), PortSingleCallActivity.class);
        intent.putExtra("callerNumber", AppConfig.DURY_ROOM);
        intent.putExtra("isMakingCall", true);
        if (type == 0) {
            Linphone.callTo(AppConfig.DURY_ROOM, false);
            intent.putExtra("isMakingVideoCall", false);
        } else {
            Linphone.callTo(AppConfig.DURY_ROOM, true);
            intent.putExtra("isMakingVideoCall", true);
        }
        Linphone.toggleSpeaker(true);
        startActivity(intent);
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getSipGroupResources();
                if (refreshLayout != null)
                    refreshLayout.setRefreshing(false);
            }
        }, 2 * 1000);
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        if (handler  != null)
            handler.removeCallbacksAndMessages(null);
        dismissProgressDialog();
        super.onDetach();
    }

    /**
     * 六大按键的点击事件
     */
    @OnClick({R.id.port_sipgroup_voice_btn, R.id.port_sipgroup_intercom_btn, R.id.port_sipgroup_remote_waring_btn, R.id.port_sipgroup_video_intercom_btn, R.id.port_sipgroup_remote_shot_btn, R.id.port_sipgroup_remote_speaking_btn})
    public void btnClickEvent(View view) {
        switch (view.getId()) {
            case R.id.port_sipgroup_voice_btn:
                showProgressFail("暂不支持!");
                break;
            case R.id.port_sipgroup_intercom_btn:
                showProgressFail("暂不支持!");
                break;
            case R.id.port_sipgroup_remote_waring_btn:
                showProgressFail("暂不支持!");
                break;
            case R.id.port_sipgroup_video_intercom_btn:
                showProgressFail("暂不支持!");
                break;
            case R.id.port_sipgroup_remote_shot_btn:
                showProgressFail("暂不支持!");
                break;
            case R.id.port_sipgroup_remote_speaking_btn:
                showProgressFail("暂不支持!");
                break;
        }
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        currentFragmentVisiable = isVisibleToUser;
        Logutils.i("Sip分组页面:" + currentFragmentVisiable);
        super.setUserVisibleHint(isVisibleToUser);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //showProgressDialogWithText("正在加载数据...");
                    break;
                case 2:
                    showNoNetworkView();
                    break;
                case 3:
                    showProgressFail("未请求到最新数据！");
                    String dataSources = (String) SharedPreferencesUtils.getObject(App.getApplication(), "sipGroupDataStr", "");
                    if (TextUtils.isEmpty(dataSources)) {
                        return;
                    }
                    List<SipGroupBean> alterSamples = GsonUtils.GsonToList(dataSources, SipGroupBean.class);
                    if (alterSamples != null) {

                        if (sipGroupDataList != null && sipGroupDataList.size() > 0)
                            sipGroupDataList.clear();
                        sipGroupDataList = alterSamples;
                        initAdapter();
                    }
                    break;
                case 5:
                    initAdapter();
                    break;

                case 6:
                    ToastUtils.showShort("未获取到值班室信息!!!");
                    break;
                case 7:
                    ToastUtils.showShort("Sip未注册！！！");
                    break;
                case 8:
                    ToastUtils.showShort("无更多设备!");
                    break;
                case 9:
                    showProgressFail("对方不在线或忙！");
                    break;
                case 10:
                    if (isVisible())
                        showProgressSuccess("已更新!");
                    break;

            }
        }
    };
}
