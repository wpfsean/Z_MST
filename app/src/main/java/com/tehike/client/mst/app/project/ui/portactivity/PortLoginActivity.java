package com.tehike.client.mst.app.project.ui.portactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.cmscallbacks.LoginCMSThread;
import com.tehike.client.mst.app.project.db.DatabaseHelper;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.services.BatteryAndWifiCallback;
import com.tehike.client.mst.app.project.services.BatteryAndWifiService;
import com.tehike.client.mst.app.project.ui.landactivity.LandLoginActivity;
import com.tehike.client.mst.app.project.utils.ActivityUtils;
import com.tehike.client.mst.app.project.utils.BatteryUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：竖屏登录界面
 * <p>
 * 思路 ：
 * 通过向cms索要 videoResources的长度
 * 如果长度大于0  登录成功
 * 如果长度为0  登录 失败
 * <p>
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/11/1 14:27
 */

public class PortLoginActivity extends BaseActivity {

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
    ImageView connetIcon;

    /**
     * 消息图标
     */
    @BindView(R.id.icon_message_show)
    ImageView messIcon;

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
     * 无网络时提示
     */
    @BindView(R.id.no_network_layout)
    RelativeLayout noNetWorkShow;

    /**
     * 数据库对象
     */
    DatabaseHelper databaseHelper = null;

    /**
     * 进度动画
     */
    @BindView(R.id.image_loading)
    ImageView loadingImageView;

    /**
     * 登录错误信息提示
     */
    @BindView(R.id.loin_error_infor_layout)
    TextView promptErrorInforTextView;

    /**
     * 用户名
     */
    @BindView(R.id.edit_username_layout)
    EditText editUserName;

    /**
     * 密码
     */
    @BindView(R.id.edit_userpass_layout)
    EditText editUserPwd;

    /**
     * 记住密码Checkbox
     */
    @BindView(R.id.remember_pass_layout)
    Checkable checkRememberPwd;

    /**
     * 自动登录CheckBox
     */
    @BindView(R.id.auto_login_layout)
    Checkable checkAutoLogin;

    /**
     * 服务器
     */
    @BindView(R.id.edit_serviceip_layout)
    EditText editServerIp;

    /**
     * 修改服务器的checkbox
     */
    @BindView(R.id.remembe_serverip_layout)
    CheckBox checkUpdateServerIp;

    /**
     * 加载时的动画
     */
    Animation mLoadingAnim;

    /**
     * 本机iP
     */
    String nativeIP = "";

    /**
     * 时分秒显示的格式
     */
    SimpleDateFormat hoursFormat = null;

    /**
     * 显示时间的线程是否正在运行
     */
    boolean threadIsRun = true;

    /**
     * 位置
     */
    LocationManager locationManager;

    /**
     * 用户是否禁止权限
     */
    boolean mShowRequestPermission = true;

    /**
     * 需要申请的权限
     */
    String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.USE_SIP,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * 存放未同同意的权限
     */
    List<String> mPermissionList = new ArrayList<>();

    @Override
    protected int intiLayout() {
        return R.layout.activity_port_login_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        //显示时间
        initializeTime();

        //设置屏幕方向
        setScreenOrientation();

        //申请所需要的权限 
        checkAllPermissions();
    }

    /**
     * 显示当前的时间
     */
    private void initializeTime() {

        hoursFormat = new SimpleDateFormat("HH:mm:ss");

        TimingThread timeThread = new TimingThread();
        new Thread(timeThread).start();

        //显示当前的年月日
        Date date = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy年MM月dd日");
        String currntYearDate = yearFormat.format(date);
        currentYearLayout.setText(currntYearDate);

        //节能当前的电量
        int level = BatteryUtils.getSystemBattery(App.getApplication());
        Message currentBatteryMess = new Message();
        currentBatteryMess.arg1 = level;
        currentBatteryMess.what = 2;
        handler.sendMessage(currentBatteryMess);
    }

    /**
     * 每隔1秒刷新一下时间的线程
     */
    class TimingThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(1);
            } while (threadIsRun);
        }
    }

    /**
     * 显示当前的时间
     */
    private void disPlayCurrentTime() {
        Date currentDate = new Date();
        if (hoursFormat != null) {
            String hoursStr = hoursFormat.format(currentDate);
            if (isVisible) {
                currentTimeLayout.setText(hoursStr);
            }
        }
    }

    /**
     * 显示当前的电量 信息
     *
     * @param level
     */
    private void diaPlayCurrentBattery(int level) {
        if (isVisible) {
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
        }
    }

    /**
     * 设置方向
     */
    private void setScreenOrientation() {
        findViewById(R.id.port_set_direction_btn_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppConfig.APP_DIRECTION = 2;
                ActivityUtils.removeAllActivity();
                PortLoginActivity.this.finish();
                openActivity(LandLoginActivity.class);
            }
        });
    }

    /**
     * 初始化数据
     */
    private void initParamaters() {

        //申请系统权限
        checkSysPermissions();

        //定位信息
        initializeLocation();

        //数据库对象
        if (databaseHelper == null)
            databaseHelper = new DatabaseHelper(PortLoginActivity.this);

        //登录时动画
        mLoadingAnim = AnimationUtils.loadAnimation(this, R.anim.loading);

        //获取本机的ip
        if (NetworkUtils.isConnected()) {
            nativeIP = NetworkUtils.getIPAddress(true);
            if (!TextUtils.isEmpty(nativeIP))
                AppConfig.NATIVE_IP = nativeIP;
            Logutils.i("Ip-->>>:"+nativeIP);
        } else
            handler.sendEmptyMessage(3);

        //判断是否自动登录
        boolean isAutoLogin = (boolean) SharedPreferencesUtils.getObject(PortLoginActivity.this, "autologin", false);
        if (isAutoLogin) {
            loginSuccessToCms();
        }

        //是否记住密码
        boolean isrePwd = (boolean) SharedPreferencesUtils.getObject(PortLoginActivity.this, "isremember", false);
        if (isrePwd) {
            checkRememberPwd.setChecked(true);
            //填充用户名框
            String db_name = DbConfig.getInstance().getData(1);
            if (!TextUtils.isEmpty(db_name)) {
                editUserName.setText(db_name);
            }
            //填充密码框
            String db_pwd = DbConfig.getInstance().getData(2);
            if (!TextUtils.isEmpty(db_pwd)) {
                editUserPwd.setText(db_pwd);
            }
            //填充服务器地址框
            String db_server = DbConfig.getInstance().getData(4);
            if (!TextUtils.isEmpty(db_server)) {
                editServerIp.setText(db_server);
                editServerIp.setEnabled(false);
            }
            //修改服务器地址
            checkUpdateServerIp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        editServerIp.setEnabled(true);
                    } else {
                        editServerIp.setEnabled(false);
                    }
                }
            });
        }
    }

    /**
     * 申请系统权限（允许弹窗，修改系统亮度）
     */
    private void checkSysPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(PortLoginActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, 2);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                //如果没有修改系统的权限这请求修改系统的权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, 3);
            }
        }
    }

    /**
     * 申请所有的权限
     */
    private void checkAllPermissions() {
        //清空存放未申请成功的权限的集合
        mPermissionList.clear();
        //循环的去申请
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(PortLoginActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);
            }
        }
        //继续申请
        if (!mPermissionList.isEmpty()) {
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(PortLoginActivity.this, permissions, 1);
        } else {
            //初始化参数
            initParamaters();
        }
    }

    /**
     * 成功登录cms（cms验证通过）
     */
    public void loginSuccessToCms() {
        handler.sendEmptyMessage(4);
        //跳转到主页面并finish本页面
        Intent intent = new Intent(PortLoginActivity.this, PortMainActivity.class);
        PortLoginActivity.this.startActivity(intent);
        ActivityUtils.removeActivity(PortLoginActivity.this);
        PortLoginActivity.this.finish();
    }

    /**
     * 网络变化异常
     *
     * @param state
     * @param name
     */
    @Override
    public void onNetChange(int state, String name) {

    }

    /**
     * 初始化位置监听
     */
    @SuppressLint("MissingPermission")
    private void initializeLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //查看定位是否打开
        boolean isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetwork) {
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                AppConfig.LOCATION_LAT = location.getLatitude();
                AppConfig.LOCATION_LOG = location.getLongitude();
            }
            //定位监听
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else if (isGps) {
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                AppConfig.LOCATION_LAT = location.getLatitude();
                AppConfig.LOCATION_LOG = location.getLongitude();
            }
            //定位监听
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    /**
     * 位置监听回调
     */
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Logutils.i("Location-->>" + location.getLatitude() + "\t" + location.getLongitude());
            AppConfig.LOCATION_LAT = location.getLatitude();
            AppConfig.LOCATION_LOG = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    //是否点击过了登录按键
    boolean isLoginingFlag = false;
    //获取输入框内的用户名
    String name = "";
    //获取输入框内的密码
    String pass = "";
    //获取输入框内的服务器地址
    String server_IP = "";
    //是否记住密码
    boolean isRememberPwd;
    //是否自动 登录
    boolean isAutoLogin;

    /**
     * 登录
     *
     * @param view
     */
    @OnClick(R.id.userlogin_button_layout)
    public void loginCMS(View view) {
        promptErrorInforTextView.setText("");
        //防止点击过快
        //获取当前输入框内的内容
        name = editUserName.getText().toString().trim();
        pass = editUserPwd.getText().toString().trim();
        server_IP = editServerIp.getText().toString().trim();
        //判断信息是否齐全
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pass) && !TextUtils.isEmpty(server_IP)) {
            //判断是否有网
            if (NetworkUtils.isConnected()) {
                //加载动画显示
                loadingImageView.setVisibility(View.VISIBLE);
                loadingImageView.startAnimation(mLoadingAnim);
                //正则表达
                if (!NetworkUtils.isboolIp(server_IP)) {
                    handler.sendEmptyMessage(6);
                    return;
                }
                //判断是否正在登录
                if (!isLoginingFlag) {
                    TcpLoginCmsThread thread = new TcpLoginCmsThread(name, pass, server_IP);
                    new Thread(thread).start();
                    isLoginingFlag = true;
                } else {
                    //取消登录，并关闭socket
                    isLoginingFlag = false;
                    if (loginCmsTcp != null) {
                        try {
                            loginCmsTcp.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        loginCmsTcp = null;
                    }
                    //提示取消登录
                    handler.sendEmptyMessage(9);
                }
            } else {
                //提示无网络
                handler.sendEmptyMessage(3);
            }
        } else {
            //提示信息缺失
            handler.sendEmptyMessage(5);
        }
    }

    //tcp请求cms数据
    Socket loginCmsTcp = null;

    /**
     * 登录cms子线程
     */
    class TcpLoginCmsThread extends Thread {

        String name;
        String pwd;
        String serverIp;

        //构造函数
        public TcpLoginCmsThread(String name, String pwd, String serverIp) {
            this.name = name;
            this.pwd = pwd;
            this.serverIp = serverIp;
        }

        @Override
        public void run() {
            //需要发送的数据
            byte[] sendData = new byte[140];
            //数据头
            byte[] flag = AppConfig.VIDEO_HEDER_ID.getBytes();
            for (int i = 0; i < flag.length; i++) {
                sendData[i] = flag[i];
            }
            //action 1（获取资源列表------需查看文档，根据实际的要求写入Action----------）
            sendData[4] = (byte) AppConfig.LOGIN_CMS_ACTION;
            sendData[5] = 0;
            sendData[6] = 0;
            sendData[7] = 0;
            //需要 的参数
            if (TextUtils.isEmpty(nativeIP)) {
                nativeIP = NetworkUtils.getIPAddress(true);
            }

            if (TextUtils.isEmpty(nativeIP)) {
                handler.sendEmptyMessage(7);
                return;
            }
            try {
                //拼加登录 参数
                String parameters = name + File.separatorChar + pass + File.separatorChar + nativeIP;
                byte[] data = parameters.getBytes(AppConfig.CMS_FORMAT);
                for (int i = 0; i < data.length; i++) {
                    sendData[i + 8] = data[i];
                }
                if (loginCmsTcp == null)
                    loginCmsTcp = new Socket(serverIp, AppConfig.LOGIN_CMS_PORT);
                loginCmsTcp.setSoTimeout(4 * 1000);
                OutputStream os = loginCmsTcp.getOutputStream();
                os.write(sendData);
                os.flush();
                InputStream in = loginCmsTcp.getInputStream();
                //获取前8个byte
                byte[] headers = new byte[188];
                int read = in.read(headers);
                in.close();
                Logutils.i("headers" + Arrays.toString(headers));

                //登录成功
                if (headers[4] > 0) {

                    handler.sendEmptyMessage(10);
                } else {
                    //登录失败
                    handler.sendEmptyMessage(8);
                }
            } catch (Exception e) {
                Logutils.e("TcpLoginCmsThread--->>>error:" + e.getMessage());
                if (loginCmsTcp != null)
                    try {
                        loginCmsTcp.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                handler.sendEmptyMessage(8);
            } finally {
                if (loginCmsTcp != null)
                    try {
                        loginCmsTcp.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
            }
        }
    }

    /**
     * 登录成功
     */
    private void LoginSuccessMethond() {
        //获取记住密码的状态
        isRememberPwd = checkRememberPwd.isChecked();
        //获取自动登录的状态
        isAutoLogin = checkAutoLogin.isChecked();
        //判断当前是否记住密码，如果记住密码就把配置信息提前插入数据库
        if (isRememberPwd) {
            //保存记住密码的状态
            SharedPreferencesUtils.putObject(PortLoginActivity.this, "isremember", isRememberPwd);
        }
        //保存自动登录的状态
        if (isAutoLogin) {
            SharedPreferencesUtils.putObject(PortLoginActivity.this, "autologin", isRememberPwd);
        }

        //登录后先清除user表数据
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.execSQL("delete from users");
        Logutils.d("删除user表数据");
        //再向表中插入新的数据
        ContentValues contentValues = new ContentValues();
        contentValues.put("loginTime", new Date().toString());
        contentValues.put("userName", name);
        contentValues.put("userPwd", pass);
        contentValues.put("loginPort", AppConfig.LOGIN_CMS_PORT);
        contentValues.put("cmsServer", server_IP);
        db.insert("users", null, contentValues);
        Logutils.d("向user表插入数据");
        //赋值给常量
        AppConfig.USERNAME = name;
        AppConfig.PWD = pass;
        AppConfig.SERVERIP = server_IP;
        //延迟半秒后登录成功（取消动画的加载状态）
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loginSuccessToCms();
            }
        }, 500);
    }


    /**
     * 权限申请回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        //判断是否勾选禁止后不再询问
                        boolean showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(PortLoginActivity.this, permissions[i]);
                        if (showRequestPermission) {
                            //重新申请权限
                            checkAllPermissions();
                            return;
                        } else {
                            //已经禁止
                            mShowRequestPermission = false;
                            String permisson = permissions[i];
                            Logutils.e("未授予的权限:" + permisson);
                            WriteLogToFile.info("用户禁止申请以下的权限:" + permisson);
                        }
                    }
                }
                //初始化参数
                initParamaters();
                break;
            default:
                break;
        }
    }

    /**
     * Activity回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2:
                Logutils.i("ACTION_MANAGE_OVERLAY_PERMISSION--->>>" + resultCode);
                break;
            case 3:
                Logutils.i("ACTION_MANAGE_WRITE_SETTINGS--->>>" + resultCode);
                break;
        }
    }

    /**
     * 按home键时保存当前的输入状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", editUserName.getText().toString().trim());
        outState.putString("pass", editUserPwd.getText().toString().trim());
        outState.putString("serverip", editServerIp.getText().toString().trim());
    }

    /**
     * 恢复刚才的输入状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        editUserName.setText(savedInstanceState.getString("name"));
        editUserPwd.setText(savedInstanceState.getString("pass"));
        editServerIp.setText(savedInstanceState.getString("serverip"));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {

        BatteryAndWifiService.addBatterCallback(new BatteryAndWifiCallback() {
            @Override
            public void getBatteryData(int level) {
                Message batteryMess = new Message();
                batteryMess.arg1 = level;
                batteryMess.what = 2;
                handler.sendMessage(batteryMess);
            }

            @Override
            public void getWifiData(int rssi) {
            }
        });

        super.onResume();
    }

    @Override
    protected void onDestroy() {

        threadIsRun = false;

        if (hoursFormat != null)
            hoursFormat = null;

        if (databaseHelper != null)
            databaseHelper = null;

        if (mLoadingAnim != null)
            mLoadingAnim = null;

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            locationManager = null;
        }

        if (handler != null)
            handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    /**
     * Handler处理子线程发送的消息
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //显示时间
                    disPlayCurrentTime();
                    break;
                case 2:
                    //显示电量
                    int level = msg.arg1;
                    diaPlayCurrentBattery(level);
                    break;
                case 3:
                    //提示网络不可用
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("网络不可用!");
                    break;
                case 4:
                    //登录成功
                    loadingImageView.setVisibility(View.GONE);
                    loadingImageView.clearAnimation();
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("");
                    break;
                case 5:
                    //登录信息缺失
                    loadingImageView.setVisibility(View.GONE);
                    loadingImageView.clearAnimation();
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("信息缺失!");
                    break;
                case 6:
                    //ip不合法
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("服务器ip不合法!");
                    loadingImageView.setVisibility(View.GONE);
                    loadingImageView.clearAnimation();
                    break;
                case 7:
                    //ip不合法
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("登录未获取本机Ip!");
                    loadingImageView.setVisibility(View.GONE);
                    loadingImageView.clearAnimation();
                    break;
                case 8:
                    //登录失败
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("登录失败!");
                    loadingImageView.setVisibility(View.GONE);
                    loadingImageView.clearAnimation();
                    break;
                case 9:
                    //取消登录
                    promptErrorInforTextView.setVisibility(View.VISIBLE);
                    promptErrorInforTextView.setText("取消登录!");
                    loadingImageView.setVisibility(View.GONE);
                    loadingImageView.clearAnimation();
                    break;
                case 10:
                    LoginSuccessMethond();
                    break;
            }
        }
    };
}
