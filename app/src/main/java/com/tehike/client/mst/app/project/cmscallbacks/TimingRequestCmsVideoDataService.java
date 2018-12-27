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
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
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
 * 描述：启动服务每隔30秒去CMS获取一次数据
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/10/18 10:32
 */

public class TimingRequestCmsVideoDataService extends Service {

    //定时线程池任务
    ScheduledExecutorService mScheduledExecutorService = null;

    //记录返回的视频资源个数
    int videoResourcesNum = 0;

    //用于存放数据的集合
    List<Device> dataSources;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //复用线程池定时任务去向cms请求数据
        if (mScheduledExecutorService == null) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            mScheduledExecutorService.scheduleWithFixedDelay(new RequestCmsVideoSourcesThread(), 0L, AppConfig.REFRESH_DATA_TIME, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //本服务结束时使线程池关掉
        mScheduledExecutorService.shutdown();


        if (handler != null)
            handler.removeCallbacksAndMessages(null);

    }

    /**
     * 向cms请求video资源数据
     */
    class RequestCmsVideoSourcesThread extends Thread {
        @Override
        public void run() {

            //tcp向cms请求数据并解析
            requestCmsDataAndResolveRtsp();
        }

        /**
         * 向cms索要video数据并解析
         */
        private void requestCmsDataAndResolveRtsp() {

            //存放视频数据的集合
            List<VideoBen> videoSourceInfoList = new ArrayList<>();

            //存放解析onvif后数据的集合
            dataSources = new ArrayList<>();

            //tcp客户端对象
            Socket socket = null;

            //读取输入流
            InputStream is = null;

            ByteArrayOutputStream bos = null;

            try {
                //每次请求前先清除集合内的数据
                if (videoSourceInfoList != null && videoSourceInfoList.size() > 0)
                    videoSourceInfoList.clear();
                //每次请求前先清除集合内的数据
                if (dataSources != null && dataSources.size() > 0)
                    dataSources.clear();

                //socket要发送的总数据（见文档）
                byte[] bys = new byte[140];
                //数据头转byte
                byte[] flag = AppConfig.VIDEO_HEDER_ID.getBytes();
                //把头写入要发送的byte中
                for (int i = 0; i < flag.length; i++) {
                    bys[i] = flag[i];
                }
                //action 1（获取资源列表）
                bys[4] = (byte) AppConfig.GET_VIDEO_ACTION;
                bys[5] = 0;
                bys[6] = 0;
                bys[7] = 0;
                //判断网络是否通顺
                if (!NetworkUtils.isConnected()) {
                    Logutils.e("请求video资源时无网络!");
                    WriteLogToFile.info("请求video资源时无网络!");
                    return;
                }
                //获取ip
                String ip = NetworkUtils.getIPAddress(true);
                if (TextUtils.isEmpty(ip)) {
                    Logutils.e("获取ip异常");
                    return;
                }
                //用户名
                String rUserName = DbConfig.getInstance().getData(1);
                if (TextUtils.isEmpty(rUserName)) {
                    rUserName = AppConfig.USERNAME;
                }
                //口令
                String rUserPwd =  DbConfig.getInstance().getData(2);
                if (TextUtils.isEmpty(AppConfig.PWD)) {
                    rUserPwd =AppConfig.PWD;
                }
                //要发送的数据
                String data = rUserName + "/" + rUserPwd + "/" + ip;
                //字符串转成byte再写入要发送的数据中
                byte[] dataByte = data.getBytes(AppConfig.CMS_FORMAT);
                for (int i = 0; i < dataByte.length; i++) {
                    bys[i + 8] = dataByte[i];
                }
                //sock请求的地址
                String requestAddress = DbConfig.getInstance().getData(4);
                if (TextUtils.isEmpty(requestAddress)) {
                    requestAddress = AppConfig.SERVERIP;
                }
                //登录端口
                String requestPort = DbConfig.getInstance().getData(3);
                if (TextUtils.isEmpty(requestPort))
                    requestPort = AppConfig.LOGIN_CMS_PORT+"";

                //登录端口
                int rPort = Integer.parseInt(requestPort);
                //socket请求
                socket = new Socket(requestAddress, rPort);
                OutputStream os = socket.getOutputStream();
                os.write(bys);
                os.flush();

                is = socket.getInputStream();
                bos = new ByteArrayOutputStream();
                //获取前8个byte
                byte[] headers = new byte[188];
                //获取前4个，获取头文件
                int read = is.read(headers);
                //获取前4个，获取头文件
                byte[] rflag = new byte[4];
                for (int i = 0; i < 4; i++) {
                    rflag[i] = headers[i];
                }
                //获取后4个字节
                byte[] videos = new byte[4];
                for (int i = 0; i < 4; i++) {
                    videos[i] = headers[i + 4];
                }
                //返回的资源数量
                int videoCount = videos[0];

                //本设备的guid
                byte[] guid = new byte[48];
                for (int i = 0; i < 48; i++) {
                    guid[i] = headers[i + 8];
                }
                int guidPosition = ByteUtils.getPosiotion(guid);
                String str_guid = new String(guid, 0, guidPosition, AppConfig.CMS_FORMAT);
                if (!TextUtils.isEmpty(str_guid)) {
                    //设置的guid
                    AppConfig.DEVICE_GUID = str_guid;
                    //发送心中的guid
                    AppConfig.SENDHB_ID = str_guid;
                }

                //设备名称
                byte[] deviceName = new byte[128];
                for (int i = 0; i < 128; i++) {
                    deviceName[i] = headers[i + 8 + 48];
                }
                int deviceName_position = ByteUtils.getPosiotion(deviceName);
                String str_deviceName = new String(deviceName, 0, deviceName_position, AppConfig.CMS_FORMAT);
                if (!TextUtils.isEmpty(str_deviceName))
                    AppConfig.DEVICE_NAME = str_deviceName;

                // 总的数据长度（查文档，查看每个视频源的长度）
                int alldata = 424 * videoCount;
                byte[] buffer = new byte[1024];
                int nIdx = 0;
                int nReadLen = 0;
                while (nIdx < alldata) {
                    nReadLen = is.read(buffer);
                    bos.write(buffer, 0, nReadLen);
                    if (nReadLen > 0) {
                        nIdx += nReadLen;
                    } else {
                        break;
                    }
                }
                // 总数据写入bos
                byte[] result = bos.toByteArray();
                int currentIndex = 0;
                List<byte[]> videoList = new ArrayList<>();
                //生成集合数据
                for (int i = 0; i < videoCount; i++) {
                    byte[] oneVideo = new byte[424];
                    System.arraycopy(result, currentIndex, oneVideo, 0, 424);
                    currentIndex += 424;
                    videoList.add(oneVideo);
                }

                // 解析每个视频数据
                for (byte[] vInfo : videoList) {
                    //数据头
                    byte[] videoFlagByte = new byte[4];
                    System.arraycopy(vInfo, 0, videoFlagByte, 0, 4);
                    String videoFlag = new String(videoFlagByte, AppConfig.CMS_FORMAT).trim();
                    //视频唯一 id
                    byte[] videoIdByte = new byte[48];
                    System.arraycopy(vInfo, 4, videoIdByte, 0, 48);
                    String videoId = new String(videoIdByte, AppConfig.CMS_FORMAT).trim();
                    //视频名称
                    byte[] videoNameByte = new byte[128];
                    System.arraycopy(vInfo, 52, videoNameByte, 0, 128);
                    String videoName = new String(videoNameByte, AppConfig.CMS_FORMAT).trim();
                    //设备型号
                    byte[] videoDeviceTypeByte = new byte[16];
                    System.arraycopy(vInfo, 180, videoDeviceTypeByte, 0, 16);
                    String videoDeviceType = new String(videoDeviceTypeByte, AppConfig.CMS_FORMAT).trim();
                    //视频中的ip
                    byte[] videoIpByte = new byte[32];
                    System.arraycopy(vInfo, 196, videoIpByte, 0, 32);
                    String videoIp = new String(videoIpByte, AppConfig.CMS_FORMAT).trim();
                    //端口
                    byte[] videoPortByte = new byte[4];
                    System.arraycopy(vInfo, 228, videoPortByte, 0, 4);
                    String videoPort = videoPortByte[0] + "";
                    //通道
                    byte[] videoChannelByte = new byte[128];
                    System.arraycopy(vInfo, 232, videoChannelByte, 0, 128);
                    String videoChannel = new String(videoChannelByte, AppConfig.CMS_FORMAT).trim();
                    //用户名
                    byte[] videoUserNameByte = new byte[32];
                    System.arraycopy(vInfo, 360, videoUserNameByte, 0, 32);
                    String videoUserName = new String(videoUserNameByte, AppConfig.CMS_FORMAT).trim();
                    //密码
                    byte[] videoPwdByte = new byte[32];
                    System.arraycopy(vInfo, 392, videoPwdByte, 0, 32);
                    String videoPwd = new String(videoPwdByte, AppConfig.CMS_FORMAT).trim();

                    //封装成对象放入集合
                    VideoBen videoBen = new VideoBen();
                    videoBen.setFlage(videoFlag);
                    videoBen.setId(videoId);
                    videoBen.setName(videoName);
                    videoBen.setDevicetype(videoDeviceType);
                    videoBen.setIp(videoIp);
                    videoBen.setPort(videoPort);
                    videoBen.setChannel(videoChannel);
                    videoBen.setUsername(videoUserName);
                    videoBen.setPassword(videoPwd);
                    videoBen.setRtsp("");
                    videoBen.setPtz_url("");
                    videoBen.setToken("");
                    videoSourceInfoList.add(videoBen);
                }
                //解析video中的rtsp地址（播放用）
                resolveVideoRTSP(videoSourceInfoList);
            } catch (Exception e) {
                Logutils.e("获取Video资源失败:" + e.getMessage());
                WriteLogToFile.info("获取Video资源失败:" + e.getMessage());
            } finally {
                try {
                    if (bos != null)
                        bos.close();
                    if (is != null)
                        is.close();
                    if (socket != null)
                        socket.close();
                } catch (Exception e) {
                    Logutils.e("RequestCmsVideoSourcesThread关流异常:" + e.getMessage());
                }
            }
        }

        /**
         * 解析Onvif集合
         *
         * @param mList
         */
        private void resolveVideoRTSP(List<VideoBen> mList) {
            //遍历集合
            for (int i = 0; i < mList.size(); i++) {
                final Device device = new Device();
                String deviceType = mList.get(i).getDevicetype();
                String ip = mList.get(i).getIp();
                //先判断设备类型和ip是否为空
                if (!TextUtils.isEmpty(deviceType) && !TextUtils.isEmpty(ip)) {
                    videoResourcesNum = mList.size();
                    if (deviceType.toUpperCase().equals("ONVIF")) {
                        device.setVideoBen(mList.get(i));
                        device.setServiceUrl("http://" + ip + "/onvif/device_service");
                        ResolveOnvif onvif = new ResolveOnvif(device, new ResolveOnvif.GetRtspCallback() {
                            @Override
                            public void getDeviceInfoResult(String rtsp, boolean isSuccess, Device mDevice) {
                                //handler处理解析返回的设备对象
                                Message message = new Message();
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("device", mDevice);
                                message.setData(bundle);
                                message.what = 1000;
                                handler.sendMessage(message);
                            }
                        });
                        //执行线程
                        App.getExecutorService().execute(onvif);
                    } else if (deviceType.toUpperCase().equals("RTSP")) {
                        //若设备类型是RTSP类型，拼加成rtsp
                        String mRtsp = "rtsp://" + mList.get(i).getUsername() + ":" + mList.get(i).getPassword() + "@" + mList.get(i).getIp() + "/" + mList.get(i).getChannel();
                        VideoBen v = mList.get(i);
                        v.setRtsp(mRtsp);
                        device.setRtspUrl(mRtsp);
                        device.setVideoBen(v);
                        //同样用handler处理这个设备对象
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("device", device);
                        message.setData(bundle);
                        message.what = 1000;
                        handler.sendMessage(message);
                    }
                } else {
                    //如果为空说明没面部视频
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    device.setDeviceType(mList.get(i).getDevicetype());
                    device.setRtspUrl("");
                    bundle.putSerializable("device", device);
                    message.setData(bundle);
                    message.what = 111;
                    handler.sendMessage(message);
                }
            }
        }
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    Bundle dbundle = msg.getData();
                    Device device = (Device) dbundle.getSerializable("device");
                    dataSources.add(device);
                    //通过gson把集合转成字符串
                    String result = GsonUtils.GsonToString(dataSources);
                    if (TextUtils.isEmpty(result)) {
                        Logutils.e("Gson转字符串失败");
                        return;
                    }
                    SharedPreferencesUtils.putObject(App.getApplication(), "video_resources", result);
                    WriteLogToFile.info(new Date().toString() + "\n" + "Video资源解析完成");
                    break;
            }
        }
    };
}
