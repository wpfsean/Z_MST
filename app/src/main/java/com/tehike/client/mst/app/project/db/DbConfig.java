package com.tehike.client.mst.app.project.db;

import android.database.Cursor;

import com.tehike.client.mst.app.project.base.App;


//  Logutils.i(DbConfig.getInstance().getData(-1)+"id");
//          Logutils.i(DbConfig.getInstance().getData(0)+"登录时间");
//          Logutils.i(DbConfig.getInstance().getData(1)+"当前帐号");
//          Logutils.i(DbConfig.getInstance().getData(2)+"当前密码");
//          Logutils.i(DbConfig.getInstance().getData(3)+"前登录 cms端口");
//          Logutils.i(DbConfig.getInstance().getData(4)+"cms地址");
//          Logutils.i(DbConfig.getInstance().getData(5)+"心跳端口");
//          Logutils.i(DbConfig.getInstance().getData(6)+"心跳地址");
//          Logutils.i(DbConfig.getInstance().getData(7)+"报警端口");
//          Logutils.i(DbConfig.getInstance().getData(8)+"报警地址");
//          Logutils.i(DbConfig.getInstance().getData(9)+"sip用户名");
//          Logutils.i(DbConfig.getInstance().getData(10)+"sip号码");
//          Logutils.i(DbConfig.getInstance().getData(11)+"sip密码");
//          Logutils.i(DbConfig.getInstance().getData(12)+"sip服务器地址");
//          Logutils.i(DbConfig.getInstance().getData(13)+"设备的guid");
//          Logutils.i(DbConfig.getInstance().getData(14)+"设备名称");


public class DbConfig {
    static DatabaseHelper databaseHelper;
    private volatile static DbConfig instance = null;

    private DbConfig() {
    }

    public static DbConfig getInstance() {
        if (instance == null) {
            synchronized (DbConfig.class) {
                if (instance == null) {
                    instance = new DbConfig();
                    databaseHelper = new DatabaseHelper(App.getApplication());
                }
            }
        }
        return instance;
    }

    public String getData(int type) {
        try {
            Cursor cursor = databaseHelper.getUserCursor();
            String data = "";
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    switch (type) {
                        case -1://表中用户id
                            data = cursor.getString(cursor.getColumnIndex("_id"));
                            break;
                        case 0: //登录时间
                            data = cursor.getString(cursor.getColumnIndex("loginTime"));
                            break;
                        case 1://当前帐号
                            data = cursor.getString(cursor.getColumnIndex("userName"));
                            break;
                        case 2://当前密码
                            data = cursor.getString(cursor.getColumnIndex("userPwd"));
                            break;
                        case 3://当前登录 cms端口
                            data = cursor.getString(cursor.getColumnIndex("loginPort"));
                            break;
                        case 4://cms地址
                            data = cursor.getString(cursor.getColumnIndex("cmsServer"));
                            break;
                        case 5://心跳端口
                            data = cursor.getString(cursor.getColumnIndex("heartbeatPort"));
                            break;
                        case 6://心跳地址
                            data = cursor.getString(cursor.getColumnIndex("heartbeatServer"));
                            break;
                        case 7://报警端口
                            data = cursor.getString(cursor.getColumnIndex("alertPort"));
                            break;
                        case 8://报警地址
                            data = cursor.getString(cursor.getColumnIndex("alertServer"));
                            break;
                        case 9://sip用户名
                            data = cursor.getString(cursor.getColumnIndex("sipUsername"));
                            break;
                        case 10://sip号码
                            data = cursor.getString(cursor.getColumnIndex("sipNumber"));
                            break;
                        case 11://sip密码
                            data = cursor.getString(cursor.getColumnIndex("sipPassword"));
                            break;
                        case 12://sip服务器地址
                            data = cursor.getString(cursor.getColumnIndex("sipServer"));
                            break;
                        case 13://设备的guid
                            data = cursor.getString(cursor.getColumnIndex("deviceGuid"));
                            break;
                        case 14://设备名称
                            data = cursor.getString(cursor.getColumnIndex("deviceName"));
                            break;
                        case 15://接收友邻哨端口
                            data = cursor.getString(cursor.getColumnIndex("neighborWatchPort"));
                            break;
                    }
                } else {
                    return "";
                }
            }
            return data;
        } catch (Exception e) {

        }
        return "";
    }
}
