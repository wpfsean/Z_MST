package com.tehike.client.mst.app.project.cmscallbacks;


import android.Manifest;
import android.widget.Toast;

import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.PermissionUtils;
import com.tehike.client.mst.app.project.utils.ToastUtils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
/**
 * 描述：判断是否登录成功
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2018/10/18 10:28
 * @version V1.0
 */

public class LoginCMSThread implements Runnable {

    String name;
    String pass;
    String nativeIp;
    String serverIp;
    LoginCallback callback;

    /**
     * Construct
     *
     * @param name  登录用户名
     * @param pass  登录密码
     * @param nativeIp  登录者ip
     * @param serverIp  登录服务器地址
     * @param callback  登录返回状态回调
     */
    public LoginCMSThread(String name, String pass, String nativeIp, String serverIp, LoginCallback callback) {
        this.name = name;
        this.pass = pass;
        this.nativeIp = nativeIp;
        this.serverIp = serverIp;
        this.callback = callback;
    }

    @Override
    public void run() {
        Socket socket = null;
        InputStream in = null;
        try {
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

            //需要传递的参数
            String parameters = name + File.separatorChar + pass + File.separatorChar + nativeIp;
            byte[] data = parameters.getBytes(AppConfig.CMS_FORMAT);
            for (int i = 0; i < data.length; i++) {
                sendData[i + 8] = data[i];
            }
            //socket请求
            socket = new Socket(serverIp, AppConfig.LOGIN_CMS_PORT);
            socket.setSoTimeout(6 * 1000);
            OutputStream os = socket.getOutputStream();
            os.write(sendData);
            os.flush();
            in = socket.getInputStream();
            //获取前8个byte
            byte[] headers = new byte[188];
            int read = in.read(headers);


            Logutils.i("isLoginSuccess--->>"+headers[4]);
            if (callback != null) {
                callback.getLoginStatus(headers[4]);
            }

        } catch (Exception e) {
            Logutils.e(new Date().toString() + "\t" + "LoginCMS_error:" + e.getMessage());
            if (callback != null) {
                callback.getLoginStatus(0);
            }
            WriteLogToFile.info("登录异常:" + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 运行线程
     */
    public void start() {
        new Thread(this).start();
    }

    /**
     * 回调结果
     */
    public interface LoginCallback {
        void getLoginStatus(int count);
    }
}
