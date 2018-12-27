package com.tehike.client.mst.app.project.utils;


import android.text.TextUtils;

import com.tehike.client.mst.app.project.db.DbConfig;
import com.tehike.client.mst.app.project.global.AppConfig;

import org.json.JSONArray;
import org.json.JSONObject;

public class SipIsOnline {


    //是否在线标识
    static boolean isOnline = false;

    //构造方法
    public SipIsOnline() {
        throw new UnsupportedOperationException("不能被实例化");
    }


    //判断是否在线
    public static boolean isOnline(final String sipNum) {
        String serverIp = DbConfig.getInstance().getData(4);
        if (TextUtils.isEmpty(serverIp))
            serverIp = AppConfig.SERVERIP;
        HttpUtils httpUtils = new HttpUtils( "http://"+serverIp+":8080/webapi/" + AppConfig.WEB_API_SIPSATTUS_PARAMATER, new HttpUtils.GetHttpData() {
            @Override
            public void httpData(String result) {
                if (TextUtils.isEmpty(result)) {
                    isOnline = false;
                    return;
                }
                if (result.contains("Execption") || result.contains("code != 200")) {
                    isOnline = false;
                    return;
                }
                try {
                    //解析请求到数据并放入集合内
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String username = jsonObject.getString("usrname");
                        String state = jsonObject.getString("state");
                        if (username.equals(sipNum)) {
                            if (state.equals("1")) {
                                isOnline = true;
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    Logutils.e("解析json异常:" + e.getMessage());
                    isOnline = false;
                }
            }
        });
        httpUtils.start();

        try {
            Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return isOnline;
    }
}
