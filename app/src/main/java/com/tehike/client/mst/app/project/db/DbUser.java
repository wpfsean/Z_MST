package com.tehike.client.mst.app.project.db;

import java.io.Serializable;

/**
 * Created by Root on 2018/8/5.
 *
 * 用于整个项目配置的数据
 *
 */

public class DbUser implements Serializable {


    String loginTime;
    String userName;
    String userPwd;
    String loginPort;
    String cmsServer;
    String heartbeatPort;
    String heartbeatServer;
    String alertPort;
    String alertServer;
    String sipUsername;
    String sipNumber;
    String sipPassword;
    String sipServer;
    String deviceGuid;
    String deviceName;
    String neighborWatchPort;


    public DbUser(String loginTime, String userName, String userPwd, String loginPort, String cmsServer, String heartbeatPort, String heartbeatServer, String alertPort, String alertServer, String sipUsername, String sipNumber, String sipPassword, String sipServer, String deviceGuid, String deviceName, String neighborWatchPort) {
        this.loginTime = loginTime;
        this.userName = userName;
        this.userPwd = userPwd;
        this.loginPort = loginPort;
        this.cmsServer = cmsServer;
        this.heartbeatPort = heartbeatPort;
        this.heartbeatServer = heartbeatServer;
        this.alertPort = alertPort;
        this.alertServer = alertServer;
        this.sipUsername = sipUsername;
        this.sipNumber = sipNumber;
        this.sipPassword = sipPassword;
        this.sipServer = sipServer;
        this.deviceGuid = deviceGuid;
        this.deviceName = deviceName;
        this.neighborWatchPort = neighborWatchPort;
    }

    public String getLoginTime() {

        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

    public String getLoginPort() {
        return loginPort;
    }

    public void setLoginPort(String loginPort) {
        this.loginPort = loginPort;
    }

    public String getCmsServer() {
        return cmsServer;
    }

    public void setCmsServer(String cmsServer) {
        this.cmsServer = cmsServer;
    }

    public String getHeartbeatPort() {
        return heartbeatPort;
    }

    public void setHeartbeatPort(String heartbeatPort) {
        this.heartbeatPort = heartbeatPort;
    }

    public String getHeartbeatServer() {
        return heartbeatServer;
    }

    public void setHeartbeatServer(String heartbeatServer) {
        this.heartbeatServer = heartbeatServer;
    }

    public String getAlertPort() {
        return alertPort;
    }

    public void setAlertPort(String alertPort) {
        this.alertPort = alertPort;
    }

    public String getAlertServer() {
        return alertServer;
    }

    public void setAlertServer(String alertServer) {
        this.alertServer = alertServer;
    }

    public String getSipUsername() {
        return sipUsername;
    }

    public void setSipUsername(String sipUsername) {
        this.sipUsername = sipUsername;
    }

    public String getSipNumber() {
        return sipNumber;
    }

    public void setSipNumber(String sipNumber) {
        this.sipNumber = sipNumber;
    }

    public String getSipPassword() {
        return sipPassword;
    }

    public void setSipPassword(String sipPassword) {
        this.sipPassword = sipPassword;
    }

    public String getSipServer() {
        return sipServer;
    }

    public void setSipServer(String sipServer) {
        this.sipServer = sipServer;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getNeighborWatchPort() {
        return neighborWatchPort;
    }

    public void setNeighborWatchPort(String neighborWatchPort) {
        this.neighborWatchPort = neighborWatchPort;
    }
}
