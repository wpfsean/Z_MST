package com.tehike.client.mst.app.project.ui.landactivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.base.BaseActivity;
import com.tehike.client.mst.app.project.cmscallbacks.LoginCMSThread;
import com.tehike.client.mst.app.project.db.DatabaseHelper;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.db.DbUser;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.ui.portactivity.PortLoginActivity;
import com.tehike.client.mst.app.project.utils.ActivityUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 描述：横屏登录页面（通过获取VideoResources协议的长度判断是否登录成功）
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/18 10:38
 */

public class LandLoginActivity extends BaseActivity {

    /**
     * 无网络时提示
     */
    @BindView(R.id.no_network_layout)
    RelativeLayout noNetWorkShow;

    //进度动画
    @BindView(R.id.image_loading)
    ImageView loadingImageView;

    //登录错误信息提示
    @BindView(R.id.loin_error_infor_layout)
    TextView promptErrorInforTextView;

    //用户名
    @BindView(R.id.edit_username_layout)
    EditText editUserName;

    //密码
    @BindView(R.id.edit_userpass_layout)
    EditText editUserPwd;

    //记住密码Checkbox
    @BindView(R.id.remember_pass_layout)
    Checkable checkRememberPwd;

    //自动登录CheckBox
    @BindView(R.id.auto_login_layout)
    Checkable checkAutoLogin;

    //服务器
    @BindView(R.id.edit_serviceip_layout)
    EditText editServerIp;

    //修改服务器的checkbox
    @BindView(R.id.remembe_serverip_layout)
    CheckBox checkUpdateServerIp;

//    /**
//     * 设置竖向
//     */
//    @BindView(R.id.custom_vertical)
//    Switch setDirectionForLand;

    /**
     * 加载时的动画
     */
    Animation mLoadingAnim;

    /**
     * 本机iP
     */
    String nativeIP = "";

    /**
     * 数据库对象
     */
    DatabaseHelper databaseHelper = null;


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

    List<String> mPermissionList = new ArrayList<>();


    @Override
    protected int intiLayout() {
        return R.layout.activity_land_login;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        //横屏标识
        AppConfig.APP_DIRECTION = 2;

        if (NetworkUtils.isConnected()) {
            Logutils.i("当前设备ip:" + NetworkUtils.getIPAddress(true));
        }

        //初始化数据
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            initData();
        }

        //设置方向
        setDirection();
    }

    //检查权限并申请
    private void checkPermission() {
        mPermissionList.clear();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(LandLoginActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);
            }
        } /** * 判断存储委授予权限的集合是否为空 */
        if (!mPermissionList.isEmpty()) {
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(LandLoginActivity.this, permissions, 1);
        } else {
            initData();
        }
    }

    /**
     * 设置方向
     */
    private void setDirection() {
        findViewById(R.id.land_set_direction_btn_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppConfig.APP_DIRECTION = 1;
                ActivityUtils.removeAllActivity();
                finish();
                openActivity(PortLoginActivity.class);
            }
        });
    }

    /**
     * 初始化数据
     */
    private void initData() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(LandLoginActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 10);
            }
        }

        //初始化数据库对象
        if (databaseHelper == null)
            databaseHelper = new DatabaseHelper(LandLoginActivity.this);

        //动画
        if (mLoadingAnim == null)
            mLoadingAnim = AnimationUtils.loadAnimation(this, R.anim.loading);

        //获取本机ip
        if (NetworkUtils.isConnected()) {
            nativeIP = NetworkUtils.getIPAddress(true);
            AppConfig.NATIVE_IP = nativeIP;
        } else {
            //网络不可用
            handler.sendEmptyMessage(100);
        }


        //是否自动登录
        boolean isAutoLogin = (boolean) SharedPreferencesUtils.getObject(LandLoginActivity.this, "autologin", false);
        if (isAutoLogin) {
            loginSuccess();
        }
        //是否记住密码
        boolean isrePwd = (boolean) SharedPreferencesUtils.getObject(LandLoginActivity.this, "isremember", false);
        //如果是记住密码就从数据库中读取信息并显示
        if (isrePwd) {

            checkRememberPwd.setChecked(true);
            String db_name = DbConfig.getInstance().getData(1);
            if (!TextUtils.isEmpty(db_name)) {
                editUserName.setText(db_name);
            }
            String db_pwd = DbConfig.getInstance().getData(2);
            if (!TextUtils.isEmpty(db_pwd)) {
                editUserPwd.setText(db_pwd);
            }

            String db_server = DbConfig.getInstance().getData(4);
            if (!TextUtils.isEmpty(db_server)) {
                editServerIp.setText(db_server);
                editServerIp.setEnabled(false);
            }
        }

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


    //网络状态更改时的提示
    @Override
    public void onNetChange(int state, String name) {
        if (state == -1 || state == 5) {
            handler.sendEmptyMessage(108);
        } else {
            handler.sendEmptyMessage(109);
        }
    }

    String name = "";
    String pass = "";
    String server_IP = "";
    //是否记住密码
    boolean isRemember;
    //是否自动 登录
    boolean isAuto;


    /**
     * 登录按键
     *
     * @param view
     */
    @OnClick(R.id.userlogin_button_layout)
    public void loginCMS(View view) {
        //提示信息隐藏
        promptErrorInforTextView.setText("");
        //防止点击过快
        if (!isFastClick()) {
            Logutils.e("点击过快");
            return;
        }
        //获取用户名，密码、服务器地址
        name = editUserName.getText().toString().trim();
        pass = editUserPwd.getText().toString().trim();
        server_IP = editServerIp.getText().toString().trim();

        //判断是否为空
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pass) && !TextUtils.isEmpty(server_IP)) {
            //判断网络是否连接成功
            if (NetworkUtils.isConnected()) {
                //加载动画显示
                loadingImageView.setVisibility(View.VISIBLE);
                loadingImageView.startAnimation(mLoadingAnim);
                //正则判断服务器ip是否合法
                if (!TextUtils.isEmpty(server_IP)) {
                    if (!NetworkUtils.isboolIp(server_IP)) {
                        handler.sendEmptyMessage(106);
                        return;
                    }
                }
                //判断是否获取了本机的ip信息
                if (TextUtils.isEmpty(nativeIP)) {
                    nativeIP = NetworkUtils.getIPAddress(true);
                    if (TextUtils.isEmpty(name)) {
                        handler.sendEmptyMessage(107);
                        return;
                    }
                }
                //开启子线程去执行
                TcpLoginCmsThread thread = new TcpLoginCmsThread(name, pass, server_IP);
                new Thread(thread).start();

            } else {
                //显示没网界面
                handler.sendEmptyMessage(104);
            }
        } else {
            //提示信息缺失
            handler.sendEmptyMessage(103);
        }
    }

    /**
     * 取消登录
     *
     * @param view
     */
    @OnClick(R.id.userlogin_button_cancel_layout)
    public void cancelLoginCms(View view) {

        if (loginCmsSocket != null) {
            try {
                loginCmsSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            loginCmsSocket = null;
        }
        handler.sendEmptyMessage(110);

    }

    Socket loginCmsSocket = null;

    /**
     * tcp去登录cms
     */
    class TcpLoginCmsThread extends Thread {
        String userName;
        String userPwd;
        String userServerIp;

        /**
         * 构造方法
         *
         * @param userName
         * @param userPwd
         * @param userServerIp
         */
        public TcpLoginCmsThread(String userName, String userPwd, String userServerIp) {
            this.userName = userName;
            this.userPwd = userPwd;
            this.userServerIp = userServerIp;
        }

        @Override
        public void run() {
            try {
                //要发送的总数据
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

                //需要传递的参数
                String parameters = name + File.separatorChar + pass + File.separatorChar + nativeIP;

                //写入数据
                byte[] data = parameters.getBytes(AppConfig.CMS_FORMAT);
                for (int i = 0; i < data.length; i++) {
                    sendData[i + 8] = data[i];
                }
                if (loginCmsSocket == null)
                    loginCmsSocket = new Socket(userServerIp, AppConfig.LOGIN_CMS_PORT);
                loginCmsSocket.setSoTimeout(2 * 1000);
                OutputStream os = loginCmsSocket.getOutputStream();
                os.write(sendData);
                os.flush();
                InputStream in = loginCmsSocket.getInputStream();
                //获取前8个byte
                byte[] headers = new byte[188];
                int read = in.read(headers);
                in.close();

                int num = headers[4];
                if (num == 0) {
                    //提示登录失败
                    handler.sendEmptyMessage(105);
                } else if (num > 0) {
                    //提示登录成功
                    handler.sendEmptyMessage(110);
                    //保存记住密码的
                    isRemember = checkRememberPwd.isChecked();
                    isAuto = checkAutoLogin.isChecked();
                    //判断当前是否记住密码，如果记住密码就把配置信息提前插入数据库
                    if (isRemember) {
                        SharedPreferencesUtils.putObject(LandLoginActivity.this, "isremember", isRemember);
                    }
                    if (isAuto) {
                        SharedPreferencesUtils.putObject(LandLoginActivity.this, "autologin", isAuto);
                    }
                    //登录后先清除user表数据
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    db.execSQL("delete from users");
                    Logutils.d("删除user表数据");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("loginTime",new Date().toString());
                    contentValues.put("userName",name);
                    contentValues.put("userPwd",pass);
                    contentValues.put("loginPort",AppConfig.LOGIN_CMS_PORT);
                    contentValues.put("cmsServer",server_IP);
                    db.insert("users",null,contentValues);
                    Logutils.d("向user表插入数据");

                    //赋值全局变量
                    AppConfig.USERNAME = name;
                    AppConfig.PWD = pass;
                    AppConfig.SERVERIP = server_IP;
                    //登录成功跳转
                    loginSuccess();
                }
                Logutils.i(Arrays.toString(headers));
            } catch (IOException e) {
                Logutils.e("登录异常:" + e.getMessage());
                handler.sendEmptyMessage(105);
                WriteLogToFile.info("登录失败:" + e.getMessage());
            }
        }
    }

    /**
     * 登录成功跳转
     */
    public void loginSuccess() {
        handler.sendEmptyMessage(110);
        Intent intent = new Intent(LandLoginActivity.this, LandMainActivity.class);
        LandLoginActivity.this.startActivity(intent);
        ActivityUtils.removeActivity(LandLoginActivity.this);
        LandLoginActivity.this.finish();
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
                case 100://当前网络不可用
                    if (isVisible) {
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("网络不可用");
                    }
                    break;
                case 101://提示有未申请的权限
                    if (isVisible)
                        showProgressFail("请授权所有权限！！！");
                    initData();
                    break;

                case 103://登录信息缺失
                    if (isVisible) {
                        loadingImageView.setVisibility(View.GONE);
                        loadingImageView.clearAnimation();
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("信息缺失！");
                    }
                    break;
                case 104://网络不可用
                    if (isVisible) {
                        noNetWorkShow.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("网络异常！");
                    }
                    break;
                case 105://登录失败
                    if (isVisible) {
                        loadingImageView.setVisibility(View.GONE);
                        loadingImageView.clearAnimation();
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("登录失败！");
                    }
                    break;
                case 106://ip不合法
                    if (isVisible) {
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("ip不合法！");
                        loadingImageView.setVisibility(View.GONE);
                        loadingImageView.clearAnimation();
                    }
                    break;

                case 107://未获取到本机ip
                    if (isVisible) {
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("未获取到本机IP！");
                        loadingImageView.setVisibility(View.GONE);
                        loadingImageView.clearAnimation();
                    }
                    break;

                case 108://顶部没网提示
                    if (isVisible)
                        noNetWorkShow.setVisibility(View.VISIBLE);
                    break;
                case 109:
                    if (isVisible) {
                        noNetWorkShow.setVisibility(View.GONE);
                        promptErrorInforTextView.setVisibility(View.GONE);
                    }
                    break;
                case 110://登录成功
                    if (isVisible) {
                        loadingImageView.setVisibility(View.GONE);
                        loadingImageView.clearAnimation();
                        promptErrorInforTextView.setVisibility(View.VISIBLE);
                        promptErrorInforTextView.setText("");
                    }
                    break;
            }
        }
    };


    /**
     * 按home键时保存当前的输入状态
     *
     * @param outState
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
     *
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        editUserName.setText(savedInstanceState.getString("name"));
        editUserPwd.setText(savedInstanceState.getString("pass"));
        editServerIp.setText(savedInstanceState.getString("serverip"));
    }

    boolean mShowRequestPermission = true;//用户是否禁止权限

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        //判断是否勾选禁止后不再询问
                        boolean showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(LandLoginActivity.this, permissions[i]);
                        if (showRequestPermission) {//
                            checkPermission();//重新申请权限
                            return;
                        } else {
                            mShowRequestPermission = false;//已经禁止
                            String permisson = permissions[i];
                            Logutils.w("未授予的权限:" + permisson);
                        }
                    }
                }
                initData();
                break;
            default:
                break;
        }
    }

    /**
     * 点击间隔3秒
     */
    private static final int MIN_CLICK_DELAY_TIME = 3000;

    /**
     * 最后一次点击时间
     */
    private static long lastClickTime;

    public static boolean isFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }
}

