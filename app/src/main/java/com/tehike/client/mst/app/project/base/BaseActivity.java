package com.tehike.client.mst.app.project.base;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.receiver.NetChangedReceiver;
import com.tehike.client.mst.app.project.ui.widget.NetworkStateView;
import com.tehike.client.mst.app.project.utils.ActivityUtils;
import com.tehike.client.mst.app.project.utils.ProgressDialogUtils;

import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.Unbinder;
/*
 * -----------------------------------------------------------------
 *
 * 描述:Activity基类
 *
 * -----------------------------------------------------------------
 *
 * File: BaseActivity.java
 *
 * Author: wangpf
 *
 * Version: V1.0
 *
 * Create:
 *
 * Changes (from 2018/10/8)
 *
 * -----------------------------------------------------------------
 *
 */

public abstract class BaseActivity extends AppCompatActivity implements NetworkStateView.OnRefreshListener, NetChangedReceiver.NetStatusChangeEvent {

    //An unbinder contract that will unbind views when called.
    private Unbinder unbinder;

    //Dialog工具类
    private ProgressDialogUtils progressDialog;

    //网络监听
    private NetChangedReceiver mNetReceiver;

    //网编状态的view
    private NetworkStateView networkStateView;

    //回调
    public static NetChangedReceiver.NetStatusChangeEvent event;

    //接收报警的广播
    private BroadcastReceiver mReceiver;

    //action
    private IntentFilter mIntentFilter;

    OnActionResponse listern;

    PowerManager powerManager;

    PowerManager.WakeLock mWakeLock;



    //当前页面是否可见
    public boolean isVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //清除状态栏和actionbar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        if (checkDeviceHasNavigationBar(this)){
            hideBottomUIMenu();
        }

        //设置布局
        setContentView(intiLayout());
        unbinder = ButterKnife.bind(this);

        //广播监听
        initReceiver();

        event = this;

        //activity加入栈中
        ActivityUtils.addActivity(this);

        //初始化dialog
        initDialog();

        //
        afterCreate(savedInstanceState);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK , "WakeLock");
        }

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("received_alarm");
        mReceiver = new Receiver();
        this.registerReceiver(mReceiver, mIntentFilter);

    }

    /**
     * 隐藏虚拟按键
     */
    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 判断是否有虚拟按键
     * @param context
     * @return
     */
    public static boolean checkDeviceHasNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {
        }
        return hasNavigationBar;
    }



    class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (listern != null) {
                listern.onResponse(intent);
            }
        }
    }

    public void addCallback(OnActionResponse callback,Intent intent) {
        this.listern = callback;
        callback.onResponse(intent);
    }


    @SuppressLint("InflateParams")
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View view = getLayoutInflater().inflate(R.layout.base_activity, null);
        //设置填充activity_base布局
        super.setContentView(view);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            view.setFitsSystemWindows(true);
        }
        //加载子类Activity的布局
        initDefaultView(layoutResID);
    }

    /**
     * 初始化默认布局的View
     *
     * @param layoutResId 子View的布局id
     */
    private void initDefaultView(int layoutResId) {
        networkStateView = (NetworkStateView) findViewById(R.id.nsv_state_view);
        FrameLayout container = (FrameLayout) findViewById(R.id.fl_activity_child_container);
        View childView = LayoutInflater.from(this).inflate(layoutResId, null);
        container.addView(childView, 0);
    }

    protected abstract int intiLayout();


    //页面跳转
    public void openActivity(Class cls) {
        startActivity(new Intent(this, cls));
    }

    //页面跳转并结束本页面
    public void openActivityAndCloseThis(Class cls) {
        startActivity(new Intent(this, cls));
        finish();
    }


    protected abstract void afterCreate(Bundle savedInstanceState);

    private void initDialog() {
        progressDialog = new ProgressDialogUtils(this, R.style.dialog_transparent_style);
    }

    /**
     * 显示加载中的布局
     */
    public void showLoadingView() {
        networkStateView.showLoading();
    }

    /**
     * 显示加载完成后的布局(即子类Activity的布局)
     */
    public void showContentView() {
        networkStateView.showSuccess();
    }

    /**
     * 显示没有网络的布局
     */
    public void showNoNetworkView() {
        networkStateView.showNoNetwork();
        networkStateView.setOnRefreshListener(this);
    }

    /**
     * 显示没有数据的布局
     */
    public void showEmptyView() {
        networkStateView.showEmpty();
        networkStateView.setOnRefreshListener(this);
    }

    /**
     * 显示数据错误，网络错误等布局
     */
    public void showErrorView() {
        networkStateView.showError();
        networkStateView.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        onNetworkViewRefresh();
    }

    /**
     * 重新请求网络
     */
    public void onNetworkViewRefresh() {
    }

    /**
     * 显示加载的ProgressDialog
     */
    public void showProgressDialog() {
        progressDialog.showProgressDialog();
    }

    /**
     * 显示有加载文字ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param text 需要显示的文字
     */
    public void showProgressDialogWithText(String text) {
        progressDialog.showProgressDialogWithText(text);
    }

    /**
     * 显示加载成功的ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param message 加载成功需要显示的文字
     * @param time    需要显示的时间长度(以毫秒为单位)
     */
    public void showProgressSuccess(String message, long time) {
        progressDialog.showProgressSuccess(message, time);
    }

    /**
     * 显示加载成功的ProgressDialog，文字显示在ProgressDialog的下面
     * ProgressDialog默认消失时间为1秒(1000毫秒)
     *
     * @param message 加载成功需要显示的文字
     */
    public void showProgressSuccess(String message) {
        progressDialog.showProgressSuccess(message);
    }

    /**
     * 显示加载失败的ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param message 加载失败需要显示的文字
     * @param time    需要显示的时间长度(以毫秒为单位)
     */
    public void showProgressFail(String message, long time) {
        progressDialog.showProgressFail(message, time);
    }

    /**
     * 显示加载失败的ProgressDialog，文字显示在ProgressDialog的下面
     * ProgressDialog默认消失时间为1秒(1000毫秒)
     *
     * @param message 加载成功需要显示的文字
     */
    public void showProgressFail(String message) {
        progressDialog.showProgressFail(message);
    }

    /**
     * 隐藏加载的ProgressDialog
     */
    public void dismissProgressDialog() {
        progressDialog.dismissProgressDialog();
    }


    private void initReceiver() {
        mNetReceiver = new NetChangedReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetReceiver, intentFilter);
    }

    //防止点击过快
    public boolean fastClick() {
        long lastClick = 0;
        if (System.currentTimeMillis() - lastClick <= 1000) {
            return false;
        }
        lastClick = System.currentTimeMillis();
        return true;
    }


    protected interface OnActionResponse {
        void onResponse(Intent intent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNetReceiver != null) {
            unregisterReceiver(mNetReceiver);
            mNetReceiver = null;
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        unbinder.unbind();
        ActivityUtils.removeActivity(this);
    }
}
