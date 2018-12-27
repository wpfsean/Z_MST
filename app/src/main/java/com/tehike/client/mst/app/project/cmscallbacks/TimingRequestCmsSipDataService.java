package com.tehike.client.mst.app.project.cmscallbacks;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.db.DatabaseHelper;
import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.SipBean;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.linphone.Linphone;
import com.tehike.client.mst.app.project.linphone.RegistrationCallback;
import com.tehike.client.mst.app.project.linphone.SipManager;
import com.tehike.client.mst.app.project.linphone.SipService;
import com.tehike.client.mst.app.project.onvif.Device;
import com.tehike.client.mst.app.project.onvif.Onvif;
import com.tehike.client.mst.app.project.onvif.ResolveOnvif;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.GsonUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;
import com.tehike.client.mst.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 描述：每隔15分钟向cms要一次SipResources数据
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/16 14:29
 */
public class TimingRequestCmsSipDataService extends Service {

    //定时任务线程池
    ScheduledExecutorService mScheduledExecutorService = null;
    //存放cms返回的sip数据的集合
    List<SipBean> sipSources = null;
    //存放onvif解析后的数据集合
    List<Device> sipData = null;
    //记录数据量标识
    int sipResourcesNum = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //启动线程池服务让子线程去处理
        if (mScheduledExecutorService == null) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            mScheduledExecutorService.scheduleWithFixedDelay(new RequestCmsSipSourcesThread(), 0L, AppConfig.REFRESH_DATA_TIME, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //关闭线程池
        mScheduledExecutorService.shutdown();

        //移除handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }

    /**
     * 向cms索要sip数据
     */
    class RequestCmsSipSourcesThread extends Thread {
        @Override
        public void run() {
            //tcp向cms请求数据
            requestCmsSipData();
        }

        /**
         * 通过tcp向cms请求数据
         */
        private void requestCmsSipData() {

            //实例存放数据的集合
            sipData = new ArrayList<>();
            sipSources = new ArrayList<>();

            //每次请求前先清空集合
            if (sipSources != null && sipSources.size() > 0)
                sipSources.clear();
            if (sipData != null && sipData.size() > 0)
                sipData.clear();

            //socket对象
            Socket socket = null;
            //socket返回的输出流
            InputStream is = null;
            ByteArrayOutputStream bos = null;
            try {
                //要发送的数据
                byte[] requestByte = new byte[140];
                //头标识转byte并写入要发送的数据中
                byte[] rFlag = AppConfig.VIDEO_HEDER_ID.getBytes();
                for (int i = 0; i < rFlag.length; i++) {
                    requestByte[i] = rFlag[i];
                }
                // 请求动作（见文档）
                requestByte[4] = 3;
                requestByte[5] = 0;
                requestByte[6] = 0;
                requestByte[7] = 0;

                //判断网络是否通顺
                if (!NetworkUtils.isConnected()) {
                    Logutils.e("请求sip资源时无网络!");
                    WriteLogToFile.info("请求sip资源时无网络!");
                    return;
                }
                //获取ip
                String ip = NetworkUtils.getIPAddress(true);
                if (TextUtils.isEmpty(ip)) {
                    ip = DbConfig.getInstance().getData(3);
                }
                //用户名
                String rUserName = AppConfig.USERNAME;
                if (TextUtils.isEmpty(rUserName)) {
                    rUserName = DbConfig.getInstance().getData(0);
                }
                //口令
                String rUserPwd = AppConfig.PWD;
                if (TextUtils.isEmpty(AppConfig.PWD)) {
                    rUserPwd = DbConfig.getInstance().getData(1);
                }
                //要发送的数据
                String data = rUserName + "/" + rUserPwd + "/" + ip;
                //字符串转成byte再写入要发送的数据中
                byte[] dataByte = data.getBytes(AppConfig.CMS_FORMAT);
                for (int i = 0; i < dataByte.length; i++) {
                    requestByte[i + 8] = dataByte[i];
                }
                //sock请求的地址
                String requestAddress = DbConfig.getInstance().getData(4);
                if (TextUtils.isEmpty(requestAddress)) {
                    requestAddress = AppConfig.SERVERIP;
                }

                String requestPort = DbConfig.getInstance().getData(3);
                if (TextUtils.isEmpty(requestPort))
                    requestPort = AppConfig.LOGIN_CMS_PORT + "";

                //登录端口
                int rPort = Integer.parseInt(requestPort);
                //socket请求
                socket = new Socket(requestAddress, rPort);
                OutputStream os = socket.getOutputStream();
                os.write(requestByte);
                os.flush();
                //获取 返回的输出流
                is = socket.getInputStream();
                bos = new ByteArrayOutputStream();
                byte[] headers = new byte[60];
                int read = is.read(headers);
                // 解析数据头
                byte[] flag = new byte[4];
                for (int i = 0; i < 4; i++) {
                    flag[i] = headers[i];
                }
                //sip地址
                byte[] sipServer = new byte[16];
                for (int i = 0; i < 16; i++) {
                    sipServer[i] = headers[i + 4];
                }
                //截取到后面为0
                int serverPosition = ByteUtils.getPosiotion(sipServer);
                String sipStrServer = new String(sipServer, 0, serverPosition, "ASCII");

                //返回本设备sip号码
                byte[] sipName = new byte[16];
                for (int i = 0; i < 16; i++) {
                    sipName[i] = headers[i + 20];
                }
                int namePosition = ByteUtils.getPosiotion(sipName);
                String sipStrName = new String(sipName, 0, namePosition, "ASCII");

                //返回本设备的sip密码
                byte[] sipPass = new byte[16];
                for (int i = 0; i < 16; i++) {
                    sipPass[i] = headers[i + 36];
                }
                int passPosition = ByteUtils.getPosiotion(sipPass);
                String sipStrPass = new String(sipPass, 0, passPosition, "ASCII");

                //列表内的视频源数量
                byte[] sips = new byte[4];
                for (int i = 0; i < 4; i++) {
                    sips[i] = headers[i + 52];
                }
                int sipCounts = sips[0];

                byte[] groupid = new byte[4];
                for (int i = 0; i < 4; i++) {
                    groupid[i] = headers[i + 56];
                }


                // 数据总长度(通过文档计算每个数据的总长度264)
                int alldata = 660 * sipCounts;
                byte[] buffer = new byte[1024];
                int nIdx = 0;
                int nReadLen = 0;

                // 把数据写入bos
                while (nIdx < alldata) {
                    nReadLen = is.read(buffer);
                    bos.write(buffer, 0, nReadLen);
                    if (nReadLen > 0) {
                        nIdx += nReadLen;
                    } else {
                        break;
                    }
                }

                // 把总数据写入bos
                byte[] result = bos.toByteArray();
                int currentIndex = 0;
                List<byte[]> sipList = new ArrayList<>();
                for (int i = 0; i < sipCounts; i++) {
                    byte[] oneSip = new byte[660];
                    System.arraycopy(result, currentIndex, oneSip, 0, 660);
                    currentIndex += 660;
                    sipList.add(oneSip);
                }

                // 遍历数据
                for (byte[] vSip : sipList) {
                    // 标识头
                    byte[] sipFlageByte = new byte[4];
                    System.arraycopy(vSip, 0, sipFlageByte, 0, 4);
                    String sipFlageString = new String(sipFlageByte, AppConfig.CMS_FORMAT);

                    // 唯一识别编号
                    byte[] sipIdByte = new byte[48];
                    System.arraycopy(vSip, 4, sipIdByte, 0, 48);
                    String sipIdString = new String(sipIdByte, AppConfig.CMS_FORMAT).trim();
                    // SIP终端设备IP地址
                    byte[] sipIpByte = new byte[32];
                    System.arraycopy(vSip, 52, sipIpByte, 0, 32);
                    String sipIpString = new String(sipIpByte, AppConfig.CMS_FORMAT).trim();
                    //  名称servername
                    byte[] sipNameByte = new byte[112];
                    System.arraycopy(vSip, 84, sipNameByte, 0, 112);
                    String sipNameString = new String(sipNameByte, AppConfig.CMS_FORMAT).trim();

                    // 设备类型
                    byte[] sipDeviceType = new byte[16];
                    System.arraycopy(vSip, 196, sipDeviceType, 0, 16);
                    String sipDeviceTypeString = new String(sipDeviceType, AppConfig.CMS_FORMAT).trim();


                    // 哨位编号
                    int sipSentry = ByteUtils.bytesToInt(vSip, 212);
                    // // SIP 号码 Number
                    byte[] sipNumberByte = new byte[16];
                    System.arraycopy(vSip, 216, sipNumberByte, 0, 16);
                    String sipNumberString = new String(sipNumberByte, AppConfig.CMS_FORMAT).trim();
                    // videoSources

                    //获取sip内的video面部视频信息
                    // video flage
                    byte[] videoFlagByte = new byte[4];
                    System.arraycopy(vSip, 232, videoFlagByte, 0, 4);
                    String videoFlageString = new String(videoFlagByte, AppConfig.CMS_FORMAT).trim();
                    // video id
                    byte[] videoIdByte = new byte[48];
                    System.arraycopy(vSip, 236, videoIdByte, 0, 48);
                    String videoIdString = new String(videoIdByte, AppConfig.CMS_FORMAT).trim();

                    // video name
                    byte[] videoNameByte = new byte[128];
                    System.arraycopy(vSip, 284, videoNameByte, 0, 128);
                    String videoNameString = new String(videoNameByte, AppConfig.CMS_FORMAT).trim();

                    // video devicetype
                    byte[] videoDeviceTypeByte = new byte[16];
                    System.arraycopy(vSip, 412, videoDeviceTypeByte, 0, 16);
                    String videoDeviceTypeString = new String(videoDeviceTypeByte, AppConfig.CMS_FORMAT).trim();
                    // video ip
                    byte[] videoIpByte = new byte[32];
                    System.arraycopy(vSip, 428, videoIpByte, 0, 32);
                    String videoIpString = new String(videoIpByte, AppConfig.CMS_FORMAT).trim();
                    // video port_icon
                    byte[] videoPortByte = new byte[4];
                    System.arraycopy(vSip, 460, videoPortByte, 0, 4);
                    int videoPortString = ByteUtils.bytesToInt(videoPortByte, 0);

                    // video channel
                    byte[] videoChannelByte = new byte[128];
                    System.arraycopy(vSip, 464, videoChannelByte, 0, 128);
                    String videoChannelString = new String(videoChannelByte, AppConfig.CMS_FORMAT).trim();
                    // video username
                    byte[] videoUserNameByte = new byte[32];
                    System.arraycopy(vSip, 592, videoUserNameByte, 0, 32);
                    String videoUserNameString = new String(videoUserNameByte, AppConfig.CMS_FORMAT).trim();
                    // video password
                    byte[] videoPassWordByte = new byte[32];
                    System.arraycopy(vSip, 624, videoPassWordByte, 0, 32);
                    String videoPassWordString = new String(videoPassWordByte, AppConfig.CMS_FORMAT).trim();

                    //封装成实体类并添加到集合中
                    VideoBen videoBen = new VideoBen();
                    videoBen.setFlage(videoFlageString);
                    videoBen.setId(videoIdString);
                    videoBen.setName(videoNameString);
                    videoBen.setDevicetype(videoDeviceTypeString);
                    videoBen.setIp(videoIpString);
                    videoBen.setPort(videoPortString + "");
                    videoBen.setChannel(videoChannelString);
                    videoBen.setUsername(videoUserNameString);
                    videoBen.setPassword(videoPassWordString);

                    //得到本机的sip相关信息并赋值到其它的变量
                    if (sipNumberString.equals(sipStrName)) {
                        AppConfig.SIP_NAME = sipStrName;
                        AppConfig.SIP_NUMBER = sipNumberString;
                        AppConfig.SIP_PWD = sipStrPass;
                        AppConfig.SIP_SERVER = sipStrServer;
                        //获取到相关的信息后去注册到sip服务器上
                        handler.sendEmptyMessage(222);
                        SipBean sipBen = new SipBean(sipFlageString, sipIdString, sipIpString, sipNameString, sipDeviceTypeString, sipSentry,
                                sipNumberString, sipStrServer, sipStrName, sipStrPass, videoBen, "", false, "", "");
                        sipSources.add(sipBen);

                    } else {
                        SipBean sipBen = new SipBean(sipFlageString, sipIdString, sipIpString, sipNameString, sipDeviceTypeString, sipSentry,
                                sipNumberString, "", "", "", videoBen, "", false, "", "");
                        sipSources.add(sipBen);
                    }
                }
                if (sipSources != null) {
                    //解析sip对象面部视频中的onvif地址
                    resolveOnvifRtsp(sipSources);
                }
            } catch (Exception e) {
                Logutils.e("Sip数据请求异常:" + e.getMessage());
                WriteLogToFile.info("Sip数据请求异常:" + e.getMessage());
            } finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    Logutils.e("Sip请求关流失败:" + e.getMessage());
                }
            }
        }

        /**
         * 解析Sip资源中面部视频的Rtsp地址
         */
        private void resolveOnvifRtsp(List<SipBean> mList) {
            for (int i = 0; i < mList.size(); i++) {
                //找到值班室号码并赋值给常量
                if (mList.get(i).getSentry() == 0) {
                    AppConfig.DURY_ROOM = mList.get(i).getNumber();
                }

                //获取每一个待处理的sip对象
                SipBean resolveSipBean = mList.get(i);
                //判断此对象是否为空
                if (resolveSipBean != null) {
                    sipResourcesNum += 1;
                    //实例化一个新的设备对象（待解析）
                    final Device device = new Device();
                    device.setName(resolveSipBean.getName());
                    final String sipNum = resolveSipBean.getNumber();

                    //获取sip对象的面部视频对象
                    VideoBen faceVideo = resolveSipBean.getVideoBen();
                    if (faceVideo != null) {
                        //获取面部视频中的重要参数
                        String faceVideoUserName = faceVideo.getUsername();
                        String faceVideoPwd = faceVideo.getPassword();
                        String faceVideoIp = faceVideo.getIp();
                        String faceVideoDevicetype = faceVideo.getDevicetype();
                        //判断此sip对象是否有面部视频
                        if (!TextUtils.isEmpty(faceVideoUserName) && !TextUtils.isEmpty(faceVideoPwd) && !TextUtils.isEmpty(faceVideoIp) && !TextUtils.isEmpty(faceVideoDevicetype)) {
                            //判断面部视频类型是否是onvif解析
                            if (faceVideoDevicetype.toUpperCase().equals("ONVIF")) {
                                //赋值给待解析对象必要的参数
                                device.setDeviceType(mList.get(i).getDeviceType());
                                device.setSipNum(mList.get(i).getNumber());
                                device.setVideoBen(mList.get(i).getVideoBen());
                                //构造onvif请求地址
                                device.setServiceUrl("http://" + faceVideoIp + "/onvif/device_service");
                                //启动子线程去处理
                                ResolveOnvif onvif = new ResolveOnvif(device, new ResolveOnvif.GetRtspCallback() {
                                    @Override
                                    public void getDeviceInfoResult(String rtsp, boolean isSuccess, Device mDevice) {
                                        Message message = new Message();
                                        Bundle bundle = new Bundle();
                                        device.setRtspUrl(rtsp);
                                        device.setSipNum(sipNum);
                                        bundle.putSerializable("sipVideo", device);
                                        message.setData(bundle);
                                        message.what = 111;
                                        handler.sendMessage(message);
                                    }
                                });
                                //利用线程池去请求
                                App.getExecutorService().execute(onvif);
                            }
                            //如果面部视频是rtsp就拼加一个rtsp播放地址
                            if (faceVideoDevicetype.toUpperCase().equals("RTSP")) {
                                String mRtsp = "rtsp://" + mList.get(i).getVideoBen().getUsername() + ":" + mList.get(i).getVideoBen().getPassword() + "@" + mList.get(i).getIp() + "/" + mList.get(i).getVideoBen().getChannel();
                                Message message = new Message();
                                Bundle bundle = new Bundle();
                                device.setDeviceType(mList.get(i).getDeviceType());
                                device.setSipNum(sipNum);
                                device.setRtspUrl(mRtsp);
                                device.setVideoBen(mList.get(i).getVideoBen());
                                bundle.putSerializable("sipVideo", device);
                                message.setData(bundle);
                                message.what = 111;
                                handler.sendMessage(message);
                            }
                            //面部视频如果是rtmp
                            if (faceVideoDevicetype.toUpperCase().equals("RTMP")) {
                                String rtmp = mList.get(i).getVideoBen().getChannel();
                                Message message = new Message();
                                Bundle bundle = new Bundle();
                                device.setDeviceType(mList.get(i).getDeviceType());
                                device.setSipNum(sipNum);
                                device.setVideoBen(mList.get(i).getVideoBen());
                                device.setRtspUrl("");
                                if (!TextUtils.isEmpty(rtmp))
                                    device.setRtspUrl(rtmp);
                                bundle.putSerializable("sipVideo", device);
                                message.setData(bundle);
                                message.what = 111;
                                handler.sendMessage(message);
                            }
                        } else {
                            //无面部视频处理
                            Message message = new Message();
                            Bundle bundle = new Bundle();
                            device.setDeviceType(mList.get(i).getDeviceType());
                            device.setSipNum(sipNum);
                            device.setRtspUrl("");
                            bundle.putSerializable("sipVideo", device);
                            message.setData(bundle);
                            message.what = 111;
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }

    }


    /**
     * Handler处理子线程发送的消息
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 222://注册sip到服务器
                    //判断sip服务是否开启
                    if (!SipService.isReady()) {
                        Linphone.startService(getApplicationContext());
                    }
                    //去注册
                    registerSipToServer();
                    break;
                case 111://接收对象
                    //获取传递的对象并添加到集合中
                    Bundle bundle = msg.getData();
                    Device mDevice = (Device) bundle.getSerializable("sipVideo");
                    sipData.add(mDevice);
                    //判断发送过来的总数据
                    if (sipData.size() == sipResourcesNum) {
                        String result = GsonUtils.GsonToString(sipData);
                        if (TextUtils.isEmpty(result)) {
                            Logutils.e("Gson转字符串失败");
                            return;
                        }
                        //保存到本地
                        SharedPreferencesUtils.putObject(App.getApplication(), "sip_resources", result);
                        Logutils.i("SipResources已保存更新");
                        WriteLogToFile.info(new Date().toString() + "\n" + "Sip资源解析完成");

                        //遍历集合找到值班室的面部信息
                        for (Device device : sipData) {
                            if (device.getSipNum().equals(AppConfig.DURY_ROOM)) {
                                AppConfig.DURY_ROOM_RTSP = device.getRtspUrl();
                                break;
                            }
                        }
                    }
                    break;
            }
        }
    };


    /**
     * 向服务器去注册sip
     */
    private void registerSipToServer() {
        //从数据库中拿出信息
        String sipNumber = DbConfig.getInstance().getData(10);
        if (TextUtils.isEmpty(sipNumber))
            sipNumber = AppConfig.SIP_NUMBER;
        String sipPwd = DbConfig.getInstance().getData(11);
        if (TextUtils.isEmpty(sipPwd))
            sipPwd = AppConfig.SIP_PWD;
        String sipServer = DbConfig.getInstance().getData(12);
        if (TextUtils.isEmpty(sipServer))
            sipServer = AppConfig.SIP_SERVER;

        //判断sip信息是否为空
        if (TextUtils.isEmpty(sipNumber) || TextUtils.isEmpty(sipPwd) || TextUtils.isEmpty(sipServer) || TextUtils.isEmpty(AppConfig.SIP_SERVER)) {
            Logutils.e("SIp信息为空");
            return;
        }

        //如果sip在线
        if (AppConfig.SIP_STATUS) {
            return;
        }
        Linphone.setAccount(sipNumber, sipPwd, sipServer);
        Linphone.login();
    }
}
