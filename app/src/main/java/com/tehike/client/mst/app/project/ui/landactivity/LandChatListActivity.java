package com.tehike.client.mst.app.project.ui.landactivity;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.adapters.ChatListAdapter;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.cmscallbacks.RequestSipSourcesThread;
import com.tehike.client.mst.app.project.entity.SipBean;
import com.tehike.client.mst.app.project.entity.SipClient;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.MessageCallback;
import com.tehike.client.mst.app.project.linphone.SipManager;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.ui.views.SpaceItemDecoration;
import com.tehike.client.mst.app.project.ui.views.WrapContentLinearLayoutManager;
import com.tehike.client.mst.app.project.utils.NetworkUtils;

import org.linphone.core.LinphoneChatMessage;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 *Created by wpf
 *
 * 横屏sip列表数据展示
 */


public class LandChatListActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

    /**
     * 联系人列表
     */
    @BindView(R.id.land_chat_list_recyclerview_layout)
    RecyclerView chatListView;

    /**
     * 下拉刷新
     */
    @BindView(R.id.swipeRefreshLayout_land_layout)
    SwipeRefreshLayout chatSwipeView;

    /**
     * 展示数据的集合
     */
    List<SipClient> dataResources = new ArrayList<>();

    /**
     * 数据适配器
     */
    ChatListAdapter chatListAdapter = null;

    /**
     * 当前页面是否可见
     */
    boolean isFront = false;


    @Override
    public void onNetChange(int state, String name) {

    }

    @Override
    protected int intiLayout() {
        return R.layout.activity_land_chat_list;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        //设置recycleview方向
        chatListView.setLayoutManager(new WrapContentLinearLayoutManager(this, WrapContentLinearLayoutManager.VERTICAL, false));
        chatListView.addItemDecoration(new SpaceItemDecoration(0, 10));
        chatListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        //初始化下拉刷新组件
        initRefreshView();

        //加载cms数据
        initData();
    }

    /**
     * CMS上数据数据
     */
    private void initData() {
        //group分组的id
        String groupID = "0";

        if (NetworkUtils.isConnected()) {
            RequestSipSourcesThread requestSipSourcesThread = new RequestSipSourcesThread(LandChatListActivity.this, groupID + "", new RequestSipSourcesThread.SipListern() {
                @Override
                public void getDataListern(List<SipBean> sipList) {
                    if (sipList != null && sipList.size() > 0) {
                        if (dataResources != null || dataResources.size() > 0) {
                            dataResources.clear();
                        }
                        for (SipBean sipBean : sipList) {
                            SipClient sipClient = new SipClient();
                            sipClient.setDeviceName(sipBean.getName());
                            sipClient.setUsrname(sipBean.getNumber());
                            if (sipBean.getNumber().equals(AppConfig.SIP_NUMBER))
                            {
                                continue;
                            }
                            dataResources.add(sipClient);
                        }
                        handler.sendEmptyMessage(2);
                    } else {
                        handler.sendEmptyMessage(1);
                    }
                }
            });
            requestSipSourcesThread.start();
        } else {
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 初始化下拉刷新组件
     */
    private void initRefreshView() {
        //设置颜色
        chatSwipeView.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        //设置下拉刷新监听
        chatSwipeView.setOnRefreshListener(this);

    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        //定时两秒后刷新数据并消失
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initData();
                if (chatSwipeView != null)
                    chatSwipeView.setRefreshing(false);
            }
        }, 2 * 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isFront = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isFront = true;

        if (SipService.isReady() || SipManager.isInstanceiated()) {
            //回调,消息时时的刷新
            SipService.addMessageCallback(new MessageCallback() {
                @Override
                public void receiverMessage(LinphoneChatMessage linphoneChatMessage) {
                    String from = linphoneChatMessage.getFrom().getUserName();
                    int p = -1;
                    for (int i = 0; i < dataResources.size(); i++) {
                        if (dataResources.get(i).getUsrname().equals(from)) {
                            p = i;
                            break;
                        }
                    }
                    if (chatListAdapter != null)
                        chatListAdapter.notifyItemChanged(p);
                }
            });
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        //重新刷新本页面
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initData();

            }
        }, 1 * 1000);

    }

    /**
     * 展示列表数据
     */
    private void disPlayeListData() {
        dismissProgressDialog();
        if (isFront) {
            chatListAdapter = new ChatListAdapter(LandChatListActivity.this, dataResources);
            if (chatListView != null) {
                chatListView.setAdapter(chatListAdapter);
                chatListAdapter.setOnItemClickListener(new ChatListAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(SipClient sipClient) {
                        if (sipClient != null) {
                            Intent intent = new Intent();
                            intent.setClass(LandChatListActivity.this, LandChatActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("sipclient", sipClient);
                            intent.putExtras(bundle);
                            LandChatListActivity.this.startActivity(intent);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (isFront)
                        showProgressFail("No data!!!");
                    break;
                case 2:
                    if (isFront)
                        disPlayeListData();
                    break;
            }
        }
    };
}
