package com.tehike.client.mst.app.project.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.tehike.client.mst.app.project.R;
import com.tehike.client.mst.app.project.base.App;
import com.tehike.client.mst.app.project.global.AppConfig;
import com.tehike.client.mst.app.project.utils.ByteUtils;
import com.tehike.client.mst.app.project.utils.G711Utils;
import com.tehike.client.mst.app.project.utils.Logutils;
import com.tehike.client.mst.app.project.utils.WriteLogToFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;

import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

/**
 * Created by Root on 2018/9/12.
 * <p>
 * 接收远程喊话的服务
 */

public class RemoteVoiceService extends Service {

    /**
     * 数据传输对象
     */
    private StreamLiveCameraView mLiveCameraView = null;

    /**
     * rtmp推流参数
     */
    private StreamAVOption streamAVOption;

    /**
     * rtmp推流地址
     */
    private String nativeRTMP = "rtmp://" + AppConfig.SERVERIP + "/oflaDemo/" + AppConfig.SIP_NUMBER + "@" + AppConfig.SIP_SERVER;

    /**
     * 客户端的ip
     */
    String remoteClientIp = "";

    /**
     * 播放器(播放警告，播放鸣枪)
     */
    MediaPlayer mediaPlayer;

    /**
     * 发送声音数据 的UDP
     */
    DatagramSocket sendUdpServer = null;

    /**
     * 接收tcp客户端的tcp服务
     */
    ServerSocket tcpServerSocket = null;

    /**
     * 接收声音数据的udp服务
     */
    DatagramSocket receiveUdpServer = null;

    /**
     * 声音播放缓存区大小
     */
    int pBufferSize;

    /**
     * 声音播放器
     */
    AudioTrack player;

    /**
     * 发送声音的UDP端口
     */
    private int port = -1;

    /**
     * 声音采样率
     */
    public int frequency = 16000;

    /**
     * 声音缓存 Buffer大小
     */
    private int recordBufferSize;

    /**
     * 录音线程
     */
    private RecordingVoiceThread mRecordingVoiceThread;

    /**
     * 停止录音 参数
     */
    private boolean stopRecordingFlag = false;

    /**
     * 录音 器对象
     */
    private AudioRecord recorder;

    /**
     * 是否有对象连接
     */
    boolean tcpClientHasConnected = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //启动tcp服务监听
        RemoteListern listern = new RemoteListern();
        new Thread(listern).start();
        Logutils.i("启动Tcp服务");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //本服务结束时的操作
        if (mediaPlayer != null)
            mediaPlayer = null;

        if (tcpServerSocket != null) {
            try {
                tcpServerSocket.close();
                tcpServerSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        //初始化录音 参数
        initSendParamater();
        try {
            if (sendUdpServer == null)
                sendUdpServer = new DatagramSocket();
        } catch (Exception e) {
            Logutils.e("udp client error:" + e.getMessage());
        }
        //把停止录音 标识 置为false
        stopRecordingFlag = false;
        //启动录音 线程
        if (mRecordingVoiceThread == null)
            mRecordingVoiceThread = new RecordingVoiceThread();
        new Thread(mRecordingVoiceThread).start();
        Logutils.i("开始录音");
    }

    /**
     * 结束录音
     */
    public void stopRecord() {
        stopRecordingFlag = true;
    }

    /**
     * 初始化录音参数
     */
    private void initSendParamater() {
        //获取录音 缓存 区的长度大小
        recordBufferSize = AudioRecord.getMinBufferSize(frequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //获取录音机对象
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                frequency, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, recordBufferSize);
    }

    /**
     * 录音线程
     */
    class RecordingVoiceThread extends Thread {
        @Override
        public void run() {

            try {
                byte[] tempBuffer, readBuffer = new byte[recordBufferSize];
                int bufResult = 0;
                recorder.startRecording();
                //循环的录音并把数据写入tempBuffer中
                while (!stopRecordingFlag) {
                    bufResult = recorder.read(readBuffer, 0, recordBufferSize);
                    if (bufResult > 0 && bufResult % 2 == 0) {
                        tempBuffer = new byte[bufResult];
                        System.arraycopy(readBuffer, 0, tempBuffer, 0, recordBufferSize);
                        //采用g711压缩
                        g711EncodeData(tempBuffer);
                    }
                }
                recorder.stop();
                Looper.prepare();
                Looper.loop();
            } catch (Exception e) {
                String msg = "ERROR AudioRecord: " + e.getStackTrace();
                Looper.prepare();
                Looper.loop();
            }
        }
    }

    /**
     * G711声音压缩（压缩1/2）
     *
     * @param tempBuffer
     */
    private void g711EncodeData(byte[] tempBuffer) {
        DatagramPacket dp = null;
        try {
            dp = new DatagramPacket(G711Utils.encode(tempBuffer), G711Utils.encode(tempBuffer).length, InetAddress.getByName(remoteClientIp), port);
            try {
                sendUdpServer.send(dp);
                Logutils.i("正在发送....+" + remoteClientIp + "\t" + port + "\n" + Arrays.toString(G711Utils.encode(tempBuffer)) + "\n长度" + G711Utils.encode(tempBuffer).length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * 被动操作线程（被动警告、被动鸣枪、被动侦听、被动喊话）
     */
    class RemoteListern extends Thread {
        @Override
        public void run() {
            try {
                //启动tcp服务
                if (tcpServerSocket == null)
                    tcpServerSocket = new ServerSocket(AppConfig.REMOTE_PORT);
                while (true) {
                    Socket tcpClient = tcpServerSocket.accept();
                    if (!tcpClientHasConnected) {
                        //启动新的子线程处理新过来 的socket
                        tcpClientHasConnected = true;
                        new Thread(new HandlerTcpClientThread(tcpClient)).start();
                    } else {
                        //拒绝第二个tcp连接
                        reject(tcpClient);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Logutils.e("异常:" + e.getMessage());
            }
        }
    }

    /**
     * 拒绝连接
     *
     * @param socket
     */
    private void reject(Socket socket) {

        try {
            //输出流
            InputStream inputStream = socket.getInputStream();
            //接收到数据
            byte receiveData[] = new byte[16];
            //读到byte中
            int num = inputStream.read(receiveData);

            //即将返回的数据
            byte[] returnData = new byte[20];
            //头标识
            byte[] title = new byte[4];
            for (int i = 0; i < 4; i++) {
                title[i] = receiveData[i];
            }
            //动作
            byte[] action = new byte[4];
            for (int j = 0; j < action.length; j++) {
                action[j] = receiveData[j + 4];
            }
            //状态（拒绝）
            byte[] status = new byte[4];
            status[0] = 1;
            status[1] = 0;
            status[2] = 0;
            status[3] = 0;
            //拼加返回的数据
            System.arraycopy(title, 0, returnData, 0, title.length);
            System.arraycopy(action, 0, returnData, 4, 4);
            System.arraycopy(status, 0, returnData, 8, 4);
            //写入数据
            OutputStream os = socket.getOutputStream();
            os.write(returnData);
            os.flush();
            socket.close();
            Logutils.e("拒绝第二个连接了:" + Arrays.toString(returnData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理连接的tcp客户端
     */
    class HandlerTcpClientThread extends Thread {
        //tcp客户端对象
        Socket tcpClient;

        //构造方法
        public HandlerTcpClientThread(Socket tcpClient) {
            this.tcpClient = tcpClient;
        }

        @Override
        public void run() {
            super.run();
            try {
                InputStream inputStream = tcpClient.getInputStream();
                remoteClientIp = tcpClient.getInetAddress().getHostAddress();
                Logutils.i("remoteClientIp:" + remoteClientIp);
                //循环的读取tcp对象发送的数据
                while (true) {
                    //接收到的请求数据是16字节，返回的20字节
                    byte receiveData[] = new byte[16];
                    int num = inputStream.read(receiveData);
                    //判断 TCp是否正在连接
                    if (num == -1) {
                        //停止录音
                        stopRecord();
                        //断开tcp连接
                        if (tcpClient != null)
                            tcpClient.close();
                        //关闭udp发送声音对象
                        if (sendUdpServer != null) {
                            sendUdpServer.close();
                            sendUdpServer = null;
                        }
                        if (receiveUdpServer != null) {
                            receiveUdpServer.close();
                            receiveUdpServer = null;
                            Logutils.e("Udp接收服务关了啊");
                        }
                        stopReceiveUdp = true;
                        if (player != null) {
                            player.stop();
                            player.release();
                            player = null;
                        }
                        //关闭rtmp推流服务
                        if (mLiveCameraView != null) {
                            if (mLiveCameraView.isStreaming()) {
                                mLiveCameraView.destroy();
                            }
                        }

                        //使连接标识变为已断开
                        tcpClientHasConnected = false;
                        Logutils.e("TCP断开了///////////////////////////////////");
                        return;
                    }
                    //打开相机推流操作
                    if (receiveData[4] == 6) {
                        acceptCameraMonitoring(receiveData, tcpClient);
                    }

                    //如果拒绝被动远程喊话
                    if (receiveData[4] == 1) {
                        rejectSpeaking(receiveData, tcpClient);
                    }
                    //警告
                    if (receiveData[4] == 2) {
                        accpetReturnData(receiveData, tcpClient);
                        playVoice(R.raw.warning);
                        Logutils.i("播放警告完成");
                        break;
                    }
                    //鸣枪
                    if (receiveData[4] == 3) {
                        accpetReturnData(receiveData, tcpClient);
                        playVoice(R.raw.gunshoot);
                        Logutils.i("播放鸣枪完成");
                        break;
                    }
                    //声音监听
                    if (receiveData[4] == 4) {
                        acceptVoiceMonitoring(receiveData, tcpClient);
                    }
                    //单向广播（接收声音播报）
                    if (receiveData[4] == 5) {
                        //同意单向的的广播请求（被动的播放声音）
                        Logutils.i("Action5---->>>>：" + Arrays.toString(receiveData));
                        acceptVoiceBroadcast(receiveData, tcpClient);
                    }
                }
            } catch (Exception e) {
                //Tcp客户端连接异常时的处理
                //停止录音
                stopRecord();
                //断开tcp
                if (tcpClient != null)
                    try {
                        tcpClient.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                //停止udpsocket发送声音
                if (sendUdpServer != null) {
                    sendUdpServer.close();
                    sendUdpServer = null;
                }

                if (receiveUdpServer != null) {
                    receiveUdpServer.close();
                    receiveUdpServer = null;
                    Logutils.e("Udp接收服务关了啊--->>>异常");
                }
                if (player != null) {
                    player.stop();
                    player.release();
                    player = null;
                }
                stopReceiveUdp = true;

                //停止rtmp的推流服务
                if (mLiveCameraView != null) {
                    if (mLiveCameraView.isStreaming()) {
                        mLiveCameraView.destroy();
                    }
                }
                //使连接标识为断开
                tcpClientHasConnected = false;
                Logutils.e("Tcp客户端连接异常:" + e.getMessage());
                WriteLogToFile.info("TCp客户端处理异常:" + e.getMessage());
            }
        }
    }

    /**
     * 同意单向的广播服务（被动的播放服务端发送的声音）
     *
     * @param receiveData
     * @param tcpClient
     */
    private void acceptVoiceBroadcast(byte[] receiveData, Socket tcpClient) {

        //同意广播时的发送的数据
        byte[] broadcastReturnData = new byte[20];
        //头标识
        byte[] title = new byte[4];
        for (int i = 0; i < 4; i++) {
            title[i] = receiveData[i];
        }
        //动作
        byte[] action = new byte[4];
        for (int j = 0; j < action.length; j++) {
            action[j] = receiveData[j + 4];
        }
        //参数
        byte[] returnParamater = new byte[4];
        for (int j = 0; j < returnParamater.length; j++) {
            returnParamater[j] = receiveData[j + 8];
        }
        //拼加返回的数据头
        System.arraycopy(title, 0, broadcastReturnData, 0, title.length);
        //拼加返回的请求动作
        System.arraycopy(action, 0, broadcastReturnData, 4, action.length);
        //要返回的应答状态（同意）
        byte[] staus = new byte[4];
        staus[0] = 0;
        staus[1] = 0;
        staus[2] = 0;
        staus[3] = 0;
        //拼加要返回的应答状态（同意）
        System.arraycopy(staus, 0, broadcastReturnData, 8, staus.length);
        //返回听监听端口
        int sendPort = 8899;
        byte[] paramater = ByteUtils.toByteArray(sendPort);

        //拼加要返回的发送声音的端口
        System.arraycopy(paramater, 0, broadcastReturnData, 12, paramater.length);
        //保留字符
        byte[] Reserved = new byte[4];
        //拼加保留字符
        System.arraycopy(Reserved, 0, broadcastReturnData, 16, Reserved.length);
        try {
            //写入数据返回
            OutputStream os = tcpClient.getOutputStream();
            os.write(broadcastReturnData);
            os.flush();
            Logutils.i("同意Action5并返回数据：---->>>" + Arrays.toString(broadcastReturnData));
            //播放
            startPlayVoice();
            //子线程去接收数据
            new Thread(new ReceiveBroadCastUdpThread(sendPort)).start();
        } catch (Exception e) {
            Logutils.e("broadcastReturnData写入数据异常:" + e.getMessage());
        }
    }

    /**
     * 开始 播放声音
     */
    private void startPlayVoice() {
        //设置播放器缓冲区大小
        pBufferSize = AudioTrack.getMinBufferSize(frequency, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        //获取播放器对象
        player = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                pBufferSize, AudioTrack.MODE_STREAM);
        //设置声音并播放
        player.setStereoVolume(0.7f, 0.7f);
        player.play();
        stopReceiveUdp = false;
    }

    /**
     * 停止udp服务循环接收的标识
     */
    boolean stopReceiveUdp = false;

    /**
     * udp线程服务接收发送过来的声音
     */
    class ReceiveBroadCastUdpThread extends Thread {

        //udp服务接收的端口
        int port;

        //构造函数
        public ReceiveBroadCastUdpThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            Logutils.i("启动UDP服务接收声音数据");
            try {
                DatagramPacket udpPackage = null;
                if (receiveUdpServer == null)
                    receiveUdpServer = new DatagramSocket(port);
                byte[] buf = new byte[2048];
                while (!stopReceiveUdp) {
                    udpPackage = new DatagramPacket(buf, buf.length);
                    if (receiveUdpServer != null)
                        receiveUdpServer.receive(udpPackage);
                    //得到声音数据
                    byte[] voiceData = udpPackage.getData();
                    //找到数据中不为零的下标
                    int length = ByteUtils.getPosiotion(voiceData);
                    //新byte数组接收数据
                    byte[] subData = new byte[length];
                    //把声音数据放到新的byte中
                    System.arraycopy(voiceData, 0, subData, 0, length);
                    //g711解压
                    byte[] g711Data = G711Utils.decode(subData);
                    //数据写入播放器并播放
                    if (player != null)
                        player.write(g711Data, 0, g711Data.length);
                    Logutils.i("正在解压声音:" + Arrays.toString(g711Data));
                }
            } catch (Exception e) {
                Logutils.e("Udp server error:" + e.getMessage());
                //异常时停止声音播放
                if (player != null) {
                    player.release();
                }
                //异常时断开udp服务
                if (receiveUdpServer != null) {
                    receiveUdpServer.close();
                    receiveUdpServer = null;
                }
                stopReceiveUdp = true;
            }
        }
    }

    /**
     * 同意后返回数据
     */
    private void accpetReturnData(byte[] receiveData, Socket tcpClient) throws IOException {

        //返回的头标识
        byte[] title = new byte[4];
        for (int i = 0; i < 4; i++) {
            title[i] = receiveData[i];
        }
        //动作
        byte[] action = new byte[4];
        for (int j = 0; j < action.length; j++) {
            action[j] = receiveData[j + 4];
        }
        //参数
        byte[] returnParamater = new byte[4];
        for (int j = 0; j < returnParamater.length; j++) {
            returnParamater[j] = receiveData[j + 8];
        }

        byte[] returnCameraData = new byte[20];
        System.arraycopy(title, 0, returnCameraData, 0, title.length);
        System.arraycopy(action, 0, returnCameraData, 4, action.length);
        byte[] staus = new byte[4];
        staus[0] = 0;
        staus[1] = 0;
        staus[2] = 0;
        staus[3] = 0;
        System.arraycopy(staus, 0, returnCameraData, 8, staus.length);
        byte[] paramater = new byte[4];
        System.arraycopy(paramater, 0, returnCameraData, 12, paramater.length);

        byte[] reserved = new byte[4];
        System.arraycopy(reserved, 0, returnCameraData, 16, reserved.length);
        Logutils.i("同意操作:" + Arrays.toString(returnCameraData));
        OutputStream os = tcpClient.getOutputStream();
        os.write(returnCameraData);
        os.flush();
        tcpClient.close();
        tcpClientHasConnected = false;
    }

    /**
     * 同意打开相机传递数据
     */
    private void acceptCameraMonitoring(byte[] receiveData, Socket tcpClient) throws IOException {

        byte[] title = new byte[4];
        for (int i = 0; i < 4; i++) {
            title[i] = receiveData[i];
        }
        //动作
        byte[] action = new byte[4];
        for (int j = 0; j < action.length; j++) {
            action[j] = receiveData[j + 4];
        }

        byte[] returnParamater = new byte[4];
        for (int j = 0; j < returnParamater.length; j++) {
            returnParamater[j] = receiveData[j + 8];
        }

        byte[] returnCameraData = new byte[20];
        System.arraycopy(title, 0, returnCameraData, 0, title.length);
        System.arraycopy(action, 0, returnCameraData, 4, action.length);
        byte[] staus = new byte[4];
        staus[0] = 0;
        staus[1] = 0;
        staus[2] = 0;
        staus[3] = 0;
        System.arraycopy(staus, 0, returnCameraData, 8, staus.length);
        byte[] paramater = new byte[4];
        System.arraycopy(paramater, 0, returnCameraData, 12, paramater.length);

        byte[] reserved = new byte[4];
        System.arraycopy(reserved, 0, returnCameraData, 16, reserved.length);
        Logutils.i("打开相机:" + Arrays.toString(returnCameraData));
        OutputStream os = tcpClient.getOutputStream();
        os.write(returnCameraData);
        os.flush();

        Logutils.i("打开相机时收到的数据:" + Arrays.toString(receiveData));
        int cameraID = receiveData[9];
        openCamera(cameraID);
    }

    /**
     * 同意声音监听
     */
    private void acceptVoiceMonitoring(byte[] receiveData, Socket tcpClient) throws IOException {

        //头标识
        byte[] title = new byte[4];
        for (int i = 0; i < 4; i++) {
            title[i] = receiveData[i];
        }
        //动作
        byte[] action = new byte[4];
        for (int j = 0; j < action.length; j++) {
            action[j] = receiveData[j + 4];
        }

        byte[] returnParamater = new byte[4];
        for (int j = 0; j < returnParamater.length; j++) {
            returnParamater[j] = receiveData[j + 8];
        }
        Logutils.i("参数:" + Arrays.toString(returnParamater) + "");
        port = ByteUtils.bytesToInt(returnParamater, 0);

        byte[] returnData = new byte[20];
        System.arraycopy(title, 0, returnData, 0, title.length);
        System.arraycopy(action, 0, returnData, 4, action.length);
        byte[] staus = new byte[4];
        staus[0] = 0;
        staus[1] = 0;
        staus[2] = 0;
        staus[3] = 0;
        System.arraycopy(staus, 0, returnData, 8, staus.length);
        byte[] paramater = new byte[4];
        System.arraycopy(paramater, 0, returnData, 12, paramater.length);

        byte[] reserved = new byte[4];
        System.arraycopy(reserved, 0, returnData, 16, reserved.length);
        Logutils.i("侦听返回的数据。。。。" + Arrays.toString(returnData));
        OutputStream os = tcpClient.getOutputStream();
        os.write(returnData);
        os.flush();
        startRecord();
    }

    /**
     * 拒绝喊话
     */
    private void rejectSpeaking(byte[] receiveData, Socket tcpClient) throws IOException {

        byte[] returnData = new byte[20];
        //头标识
        byte[] title = new byte[4];
        for (int i = 0; i < 4; i++) {
            title[i] = receiveData[i];
        }
        //动作
        byte[] action = new byte[4];
        for (int j = 0; j < action.length; j++) {
            action[j] = receiveData[j + 4];
        }
        byte[] status = new byte[4];
        status[0] = 1;
        status[1] = 0;
        status[2] = 0;
        status[3] = 0;

        System.arraycopy(title, 0, returnData, 0, title.length);
        System.arraycopy(action, 0, returnData, 4, 4);
        System.arraycopy(status, 0, returnData, 8, 4);

        OutputStream os = tcpClient.getOutputStream();
        os.write(returnData);
        os.flush();
        tcpClient.close();
        tcpClientHasConnected = false;
        Logutils.i("被动远程喊话:" + Arrays.toString(returnData) + "关闭");
        Logutils.e("哈哈，我拒绝了你喊话");
    }


    /**
     * 传递视频 数据
     */
    private void openCamera(int cameraID) {

        if (mLiveCameraView == null)
            mLiveCameraView = new StreamLiveCameraView(App.getApplication());

        //初始化传递参数
        initParamater();

        if (!mLiveCameraView.isStreaming()) {
            mLiveCameraView.startStreaming(nativeRTMP);
        } else {
            mLiveCameraView.swapCamera();
        }
    }

    /**
     * 设置传递信息
     */
    private void initParamater() {
        streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl = nativeRTMP;
        //参数配置 end
        mLiveCameraView.init(this, streamAVOption);
        mLiveCameraView.addStreamStateListener(resConnectionListener);
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        mLiveCameraView.setHardVideoFilter(new HardVideoGroupFilter(files));
    }

    /**
     * 推流监听回调
     */
    RESConnectionListener resConnectionListener = new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int result) {


            Logutils.i("打开推流连接 状态：" + result + " 推流地址：" + nativeRTMP);
            //result 0成功  1 失败
        }

        @Override
        public void onWriteError(int errno) {
            Logutils.i("推流出错,请尝试重连");
        }

        @Override
        public void onCloseConnectionResult(int result) {

            Logutils.i("关闭推流连接 状态：" + result);
            //result 0成功  1 失败
        }
    };

    /**
     * 播放鸣枪或警告
     */
    private void playVoice(final int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayer = MediaPlayer.create(App.getApplication(), type);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Logutils.i("播放完成");

                    }
                });
            }
        }).start();
    }
}
