package com.tehike.client.mst.app.project.services;
/*
 * -----------------------------------------------------------------
 *
 * 描述: 电池电量和wifi信息的的回调
 *
 * -----------------------------------------------------------------
 *
 * File: BatteryAndWifiCallback.java
 *
 * Author: wangpf
 *
 * Version: V1.0
 *
 * Create: 2018/7/4.
 *
 * Changes (from 2018/10/8)
 *
 * -----------------------------------------------------------------
 *
 */

public abstract class BatteryAndWifiCallback {
    public void getBatteryData(int level) {
    }

    ;

    public void getWifiData(int rssi) {
    }

    ;

}
