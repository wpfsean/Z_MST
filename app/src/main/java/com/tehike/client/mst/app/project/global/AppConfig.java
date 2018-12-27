package com.tehike.client.mst.app.project.global;

import com.tehike.client.mst.app.project.entity.SysInfoBean;

/**
 * Created by Root on 2018/8/5.
 */

public class AppConfig {


    public AppConfig() {
        throw new UnsupportedOperationException("不能被实例化");
    }


    ////**************************************************************/////
    ////                                                              ////
    ////                         常量                                  ////
    ///                                                               ///
    ////**************************************************************////


    /**
     * 更新apk包的路径
     */
    public static  String UPDATE_APK_PATH = "http://19.0.0.20/zkth/auto_update/auto_update_apk.php";



    public  static SysInfoBean sysInfoBean = null;




    public static String SD_DIR = "zk";


    /**
     * 当前 的用户名
     */
    public static String USERNAME = "";

    /**
     * 当前的密码
     */
    public static String PWD = "";

    /**
     * 当前的服务器地址
     */
    public static String SERVERIP = "";

    /**
     * 当前的方向(androidManifest设置)
     * 1 竖屏
     * 2 横屏
     */
    public static int APP_DIRECTION = 2;

    /**
     * Header ID when logging in
     */
    public static String VIDEO_HEDER_ID = "ZKTH";

    /**
     * 开箱申请的数据头
     */
    public static String AMMO_HEADER_ID = "ReqB";

    /**
     * Send header to server id
     */
    public static String HEADER_HEADER_ID = "ZDHB";

    /**
     * 报警协议头
     */
    public static String ALARM_HEADER_ID = "ATIF";

    /**
     * 请求数据的编码格式
     */
    public static String CMS_FORMAT = "GB2312";

    /**
     * 报警类型
     */
    public static String ALERT_TYPE = "暴狱";


    /**
     * sd卡目录名称
     */
    public static String SD_DIRECTORY = "zkth";


    /**
     * Login Action logging in
     */
    public static int LOGIN_CMS_ACTION = 1;

    /**
     * 获取Video资源 的action
     */
    public static int GET_VIDEO_ACTION = 1;


    /**
     * Native address ip
     */
    public static String NATIVE_IP = "";


    /**
     * Login CMS Port
     */
    public static int LOGIN_CMS_PORT = 2010;


    /**
     * 经纬度
     */
    public static double LOCATION_LAT = 0;
    public static double LOCATION_LOG = 0;

    /**
     * DEVICE_CPU
     * DEVICE_RAM
     * DEVICE_BATTERY
     * DEVICE_WIFI
     */
    public static double DEVICE_CPU = 0;
    public static double DEVICE_RAM = 0;
    public static int DEVICE_BATTERY = 0;
    public static int DEVICE_WIFI = 0;

    /**
     * SIp配置
     */

    public static String SIP_NAME = "";
    public static String SIP_NUMBER = "";
    public static String SIP_PWD = "";
    public static String SIP_SERVER = "";

    /**
     * 远程 喊话
     */

    public static int REMOTE_PORT = 18720;

    /**
     * 接收声音的udp端口
     */
    public static int REMOTE_SPEAKINF_PORT = 9999;

    /**
     * SIp是否注册成功
     */
    public static boolean SIP_STATUS = false;


    /**
     * MiniSip服务器上拿数据
     */
    public static String SIP_SERVER_RESOURCES = "http://19.0.0.60:8080/openapi/localuser/list?{%22syskey%22:%22123456%22}";



    /**
     * 是否解析主码流或播放声音
     */
    public static boolean IS_MAIN_STREAM = false;

    public static boolean ISVIDEOSOUNDS = false;


    /**
     * 控件
     * 发送心跳间隔(秒)
     */
    public static int SEND_HB_SPACING = 15 * 1000;


    /**
     * 每隔15分钟去加载刷新一下数据
     */
    public static int REFRESH_DATA_TIME = 15 * 60 * 1000;

    /**
     * 获取视频信息的当前的Guid
     */
    public static String DEVICE_GUID = "";

    /**
     * 获取视频信息的当前的DeviceName
     */
    public static String DEVICE_NAME = "";


    /**
     * 值班室号码及视频地址
     */
    public static String DURY_ROOM = "";

    public static String DURY_ROOM_RTSP = "";


    //弹箱锁闭状态， 0-未锁闭，1-已锁闭
    public static int AMMOBOX_STATE = 1;


    //蓝牙连接状态， 0-未连接，1-已连接
    public static int BLUETOOTH_STATE = 0;


    /**
     * 发送心跳时的Guid
     * <p>
     * //{89f2a5ba-d46d-48c6-9a27-3e2c60ed0a1b}
     */
    public static String SENDHB_ID = "";

    /**
     * web接口url
     */

    /**
     * 视频资源分组参数
     */
    public static String WEB_API_VIDEOGROUPS_PARAMATER = "videogroups";

    /**
     * 输出视频资源
     */
    public static String WEB_API_VIDEOGROUPS_SOURCES_PARAMATER = "videos?groupid=";

    /**
     * 输出 SIP 注册，振铃，通话的状态 (不在此列表中的SIP客户端视为离线)
     */
    public static String WEB_API_SIPSATTUS_PARAMATER = "sipstatus";

}
