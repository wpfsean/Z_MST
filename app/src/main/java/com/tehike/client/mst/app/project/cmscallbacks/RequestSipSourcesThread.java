package com.tehike.client.mst.app.project.cmscallbacks;

import android.content.Context;
import android.text.TextUtils;

import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.SipBean;
import com.tehike.client.mst.app.project.entity.VideoBen;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.NetworkUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
/**
 * 描述：获取sip资源的回调
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2018/10/18 10:31
 * @version V1.0
 */
public class RequestSipSourcesThread implements Runnable {

    SipListern listern;
    List<SipBean> sipSources = new ArrayList<>();
    Context mContext;
    String type;

    // 构造对象
    public RequestSipSourcesThread(Context mContext, String type, SipListern listern) {
        this.mContext = mContext;
        this.type = type;
        this.listern = listern;
    }

    @Override
    public void run() {
        Socket socket = null;
        InputStream is = null;
        ByteArrayOutputStream bos = null;
        try {
            byte[] bys = new byte[140];
            byte[] zk = AppConfig.VIDEO_HEDER_ID.getBytes();
            for (int i = 0; i < zk.length; i++) {
                bys[i] = zk[i];
            }
            // action 3
            bys[4] = 3;
            bys[5] = 0;
            bys[6] = 0;
            bys[7] = 0;

            String data = "";
            String dbUser = DbConfig.getInstance().getData(1);
            if (TextUtils.isEmpty(dbUser))
                dbUser = AppConfig.USERNAME;
            String dbPwd = DbConfig.getInstance().getData(2);
            if (TextUtils.isEmpty(dbPwd))
                dbPwd = AppConfig.PWD;
            String dbNativeIp = NetworkUtils.getIPAddress(true);
            String dbServerIp = DbConfig.getInstance().getData(4);
            if (TextUtils.isEmpty(dbServerIp))
                dbServerIp = AppConfig.SERVERIP;
            String dbPort = DbConfig.getInstance().getData(3);
            if (TextUtils.isEmpty(dbPort))
                dbPort = AppConfig.LOGIN_CMS_PORT+"";

            if (!TextUtils.isEmpty(dbUser) && !TextUtils.isEmpty(dbPwd) && !TextUtils.isEmpty(dbNativeIp) && !TextUtils.isEmpty(dbServerIp) && !TextUtils.isEmpty(dbPort)) {
                data = dbUser + "/" + dbPwd + "/" + dbNativeIp + "/" + type;
            }

            byte[] na = data.getBytes(AppConfig.CMS_FORMAT);
            for (int i = 0; i < na.length; i++) {
                bys[i + 8] = na[i];
            }
            int sendPort = 0;
            if (!TextUtils.isEmpty(dbPort)) {
                sendPort = Integer.parseInt(dbPort);
            }
            socket = new Socket(dbServerIp, sendPort);
            OutputStream os = socket.getOutputStream();
            os.write(bys);
            os.flush();
            is = socket.getInputStream();
            bos = new ByteArrayOutputStream();
            byte[] headers = new byte[60];
            int read = is.read(headers);
            // 解析数据头
            byte[] flag = new byte[4];
            for (int i = 0; i < 4; i++) {
                flag[i] = headers[i];
            }

            byte[] sipServer = new byte[16];
            for (int i = 0; i < 16; i++) {
                sipServer[i] = headers[i + 4];
            }
            int serverPosition = ByteUtils.getPosiotion(sipServer);
            String sipStrServer = new String(sipServer, 0, serverPosition, "ASCII");


            byte[] sipName = new byte[16];
            for (int i = 0; i < 16; i++) {
                sipName[i] = headers[i + 20];
            }
            int namePosition = ByteUtils.getPosiotion(sipName);
            String sipStrName = new String(sipName, 0, namePosition, "ASCII");


            byte[] sipPass = new byte[16];
            for (int i = 0; i < 16; i++) {
                sipPass[i] = headers[i + 36];
            }
            int passPosition = ByteUtils.getPosiotion(sipPass);
            String sipStrPass = new String(sipPass, 0, passPosition, "ASCII");


            //
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
                // // 名称servername
                byte[] sipNameByte = new byte[112];
                System.arraycopy(vSip, 84, sipNameByte, 0, 112);
                String sipNameString = new String(sipNameByte, AppConfig.CMS_FORMAT).trim();

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

                // video flage
                byte[] videoFlagByte = new byte[4];
                System.arraycopy(vSip, 232, videoFlagByte, 0, 4);
                String videoFlageString = new String(videoFlagByte, AppConfig.CMS_FORMAT);
                // video id
                byte[] videoIdByte = new byte[48];
                System.arraycopy(vSip, 236, videoIdByte, 0, 48);
                String videoIdString = new String(videoIdByte, AppConfig.CMS_FORMAT).trim().trim();

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

                if (sipNumberString.equals(sipStrName)) {
                    SipBean sipBen = new SipBean(sipFlageString, sipIdString, sipIpString, sipNameString, sipDeviceTypeString,sipSentry,
                            sipNumberString, sipStrServer, sipStrName, sipStrPass, videoBen, "", false, "", "");
                    sipSources.add(sipBen);

                } else {
                    SipBean sipBen = new SipBean(sipFlageString, sipIdString, sipIpString, sipNameString, sipDeviceTypeString,sipSentry,
                            sipNumberString, "", "", "", videoBen, "", false, "", "");
                    sipSources.add(sipBen);
                }
            }
            if (sipSources.size() > 0) {
                if (listern != null) {
                    listern.getDataListern(sipSources);
                }
            } else {
                if (listern != null) {
                    listern.getDataListern(null);
                }
            }
        } catch (Exception e) {
            if (listern != null) {
                listern.getDataListern(sipSources);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        new Thread(this).start();
    }

    // 定义回调接口
    public interface SipListern {
        void getDataListern(List<SipBean> mList);
    }
}
