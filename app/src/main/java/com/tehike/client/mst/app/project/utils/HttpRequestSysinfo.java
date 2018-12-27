package com.tehike.client.mst.app.project.utils;

import android.text.TextUtils;
import android.util.Base64;

import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.entity.SysInfoBean;
import com.tehike.client.mst.app.project.global.AppConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 描述：$desc$ 获取所有的资源信息
 * ===============================
 *
 * @author $user$ wpfsean@126.com
 * @version V1.0
 * @Create at:$date$ $time$
 */

public class HttpRequestSysinfo extends Thread {

    //basic用到的用户名和密码
    String userName;
    String userPwd;

    //回调接口
    GetSysinfo mGetSysinfo;

    /**
     * 构造函数
     *
     * @param userName
     * @param userPwd
     */
    public HttpRequestSysinfo(String userName, String userPwd, GetSysinfo mGetSysinfo) {
        this.userName = userName;
        this.userPwd = userPwd;
        this.mGetSysinfo = mGetSysinfo;
    }

    @Override
    public void run() {

        //同步锁
        synchronized (this) {
            try {
                //获取服务器的ip
                String serverIp = DbConfig.getInstance().getData(4);
                if (TextUtils.isEmpty(serverIp))
                    serverIp = AppConfig.SERVERIP;
                //请求地址
                String url = " http://" + serverIp + ":8080/webapi/sysinfo";
                //http请求
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(3000);
                //用户名密码
                String authString = userName + ":" + userPwd;
                //添加 basic参数
                con.setRequestProperty("Authorization", "Basic " + new String(Base64.encode(authString.getBytes(), 0)));
                con.connect();
                if (con.getResponseCode() == 200) {
                    InputStream in = con.getInputStream();
                    String result = readTxt(in);
                    if (TextUtils.isEmpty(result)) {
                        if (mGetSysinfo != null) {
                            mGetSysinfo.callbackData(null);
                        }
                    } else {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject != null) {
                            SysInfoBean sysInfoBean = new SysInfoBean(jsonObject.getInt("alertPort"),
                                    jsonObject.getString("alertServer"), jsonObject.getString("deviceGuid"),
                                    jsonObject.getString("deviceName"), jsonObject.getInt("fingerprintPort"),
                                    jsonObject.getString("fingerprintServer"), jsonObject.getInt("heartbeatPort"),
                                    jsonObject.getString("heartbeatServer"), jsonObject.getString("sipPassword"),
                                    jsonObject.getString("sipServer"), jsonObject.getString("sipUsername"),
                                    jsonObject.getInt("webresourcePort"), jsonObject.getString("webresourceServer"),jsonObject.getInt("neighborWatchPort"));
                            //回调json数据
                            if (mGetSysinfo != null) {
                                //赋值给一个全局的对象
                                AppConfig.sysInfoBean = sysInfoBean;
                                mGetSysinfo.callbackData(sysInfoBean);
                            }
                        } else {
                            if (mGetSysinfo != null) {
                                mGetSysinfo.callbackData(null);
                            }
                        }
                    }
                } else {
                    //请求结果非200时回调
                    if (mGetSysinfo != null) {
                        mGetSysinfo.callbackData(null);
                    }
                }
                con.disconnect();
            } catch (Exception e) {
                Logutils.e("sysinfo异常--->>>"+e.getMessage());
                WriteLogToFile.info("sysinfo异常--->>>"+e.getMessage());
                //请求异常时回调
                if (mGetSysinfo != null) {
                    mGetSysinfo.callbackData(null);
                }
            }
        }
    }

    /**
     * 回调
     */
    public interface GetSysinfo {
        void callbackData(SysInfoBean mSysInfoBean);
    }


    /**
     * 流转字符
     *
     * @param in
     * @return
     * @throws IOException
     */
    public String readTxt(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

}
