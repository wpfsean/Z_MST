package com.tehike.client.mst.app.project.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.tehike.client.mst.app.project.utils.Logutils;

/**
 * Created by Root on 2018/8/5.
 * <p>
 * DbUser user = new DbUser();
 * user.setLoginTime(new Date().toString());
 * user.setName("admin");
 * user.setPass("pass");
 * user.setNativeIp("19.0.0.79");
 * DatabaseHelper databaseHelper = new DatabaseHelper(LoginActivity.this);
 * databaseHelper.insertOneUser(user);
 * databaseHelper.getFirstUser();
 *
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private SQLiteDatabase db;// 数据库

    public DatabaseHelper(Context context) {
        super(context, "zkth.db", null, 1);
        db = this.getWritableDatabase();
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        /**
         * 保存用户及所有重要报警信息的表字段
         *
         */
        String sql1 = "CREATE TABLE " + "users" + " (" + "_id"
                + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "loginTime" + " TEXT,"
                + "userName" + " TEXT,"
                + "userPwd" + " TEXT,"
                + "loginPort" + " TEXT,"
                + "cmsServer" + " TEXT,"
                + "heartbeatPort" + " TEXT,"
                + "heartbeatServer" + " TEXT,"
                + "alertPort" + " TEXT,"
                + "alertServer" + " TEXT,"
                + "sipUsername" + " TEXT,"
                + "sipNumber" + " TEXT,"
                + "sipPassword" + " TEXT,"
                + "sipServer" + " TEXT,"
                + "deviceGuid" + " TEXT,"
                + "deviceName" + " TEXT,"
                + "neighborWatchPort" + " TEXT)";
        sqLiteDatabase.execSQL(sql1);


        String sql2 = "CREATE TABLE " + "chat" + " (" + "_id"
                + " INTEGER PRIMARY KEY AUTOINCREMENT," + "time" + " TEXT," +
                "fromuser" + " TEXT,"+ "message" + " TEXT,"
                + "touser" + " TEXT)";

        sqLiteDatabase.execSQL(sql2);


        String sql3 = "CREATE TABLE " + "receivermess" + " (" + "_id"
                + " INTEGER PRIMARY KEY AUTOINCREMENT," + "time" + " TEXT," +
                "flage" + " TEXT,"
                + "data" + " TEXT)";

        sqLiteDatabase.execSQL(sql3);

        String sql4 = "CREATE TABLE " + "receiveralarm" + " (" + "_id"
                + " INTEGER PRIMARY KEY AUTOINCREMENT," + "time" + " TEXT," +
                "flage" + " TEXT,"
                + "data" + " TEXT)";
        sqLiteDatabase.execSQL(sql4);

    }


    public void insertMessData(String time, String flage, String data,String table) {
        ContentValues values = new ContentValues();
        values.put("time", time);
        values.put("flage", flage);
        values.put("data", data);
        long row = db.insert(table, null, values);
        Logutils.i("insertData row=" + row);
    }


//       + "loginTime" + " TEXT,"
//               + "userName" + " TEXT,"
//               + "userPwd" + " TEXT,"
//               + "loginPort" + " TEXT,"
//               + "cmsServer" + " TEXT,"
//               + "heartbeatPort" + " TEXT,"
//               + "heartbeatServer" + " TEXT,"
//               + "alertPort" + " TEXT,"
//               + "alertServer" + " TEXT,"
//               + "sipUsername" + " TEXT,"
//               + "sipNumber" + " TEXT,"
//               + "sipPassword" + " TEXT,"
//               + "sipServer" + " TEXT,"
//               + "deviceGuid" + " TEXT,"
//               + "deviceName" + " TEXT)";

    public void insertOneUser(DbUser u) {
        Cursor cursor =db.query("users", null, "userName =? and userPwd =? and loginPort =? and cmsServer = ?", new String[]{u.getUserName(), u.getUserPwd(),u.getLoginPort(), u.getCmsServer()}, null, null, null);
        if (cursor != null){
            if (cursor.getCount()>0){
                Logutils.i("已存在");
            }else {
                ContentValues contentValues = new ContentValues();
                contentValues.put("loginTime",u.getLoginTime());
                contentValues.put("userName",u.getUserName());
                contentValues.put("userPwd",u.getUserPwd());
                contentValues.put("loginPort",u.getLoginPort());
                contentValues.put("cmsServer",u.getCmsServer());
                contentValues.put("heartbeatPort",u.getHeartbeatPort());
                contentValues.put("heartbeatServer",u.getHeartbeatServer());
                contentValues.put("alertPort",u.getAlertPort());
                contentValues.put("alertServer",u.getAlertServer());
                contentValues.put("sipUsername",u.getSipUsername());
                contentValues.put("sipNumber",u.getSipNumber());
                contentValues.put("sipPassword",u.getSipPassword());
                contentValues.put("sipServer",u.getSipServer());
                contentValues.put("deviceGuid",u.getDeviceGuid());
                contentValues.put("deviceName",u.getDeviceName());
                contentValues.put("neighborWatchPort",u.getNeighborWatchPort());
                db.insert("users",null,contentValues);
                Logutils.i("插入成功");
            }
        }
    }



    public Cursor  getUserCursor(){
        Cursor cursor = db.query("users",null,null,null,null,null,null);
        if (cursor != null){
            if (cursor.getCount()>0){
                return cursor;
            }
        }
        return null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }


}
