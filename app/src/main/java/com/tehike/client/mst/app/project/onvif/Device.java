package com.tehike.client.mst.app.project.onvif;



import com.tehike.client.mst.app.project.entity.VideoBen;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *  Device设备信息类
 *
 */

public class Device implements Serializable {

    /**
     * serviceUrl,uuid 通过广播包搜索设备获取
     */
    private String serviceUrl;



    /**
     * getCapabilities
     */
    private String mediaUrl;
    private String ptzUrl;
    private String imageUrl;
    private String eventUrl;
    private String analyticsUrl;
    private String shotPicUrl;

    public String getShotPicUrl() {
        return shotPicUrl;
    }

    public void setShotPicUrl(String shotPicUrl) {
        this.shotPicUrl = shotPicUrl;
    }

    private String sipNum ;
    private  String deviceType;
    private  String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getSipNum() {
        return sipNum;
    }

    public void setSipNum(String sipNum) {
        this.sipNum = sipNum;
    }

    //RTSP 地址
    private String rtspUrl;
    public String getRtspUrl() {
        return rtspUrl;
    }

    public void setRtspUrl(String rtspUrl) {
        this.rtspUrl = rtspUrl;
    }

    VideoBen videoBen ;

    public VideoBen getVideoBen() {
        return videoBen;
    }

    public void setVideoBen(VideoBen videoBen) {
        this.videoBen = videoBen;
    }

    /**
     * onvif MediaProfile
     */
    private ArrayList<MediaProfile> profiles;


    public Device() {
        profiles = new ArrayList<>();
    }





    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }



    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getPtzUrl() {
        return ptzUrl;
    }

    public void setPtzUrl(String ptzUrl) {
        this.ptzUrl = ptzUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(String eventUrl) {
        this.eventUrl = eventUrl;
    }

    public String getAnalyticsUrl() {
        return analyticsUrl;
    }

    public void setAnalyticsUrl(String analyticsUrl) {
        this.analyticsUrl = analyticsUrl;
    }

    public ArrayList<MediaProfile> getProfiles() {
        return profiles;
    }

    public void addProfile(MediaProfile profile) {
        this.profiles.add(profile);
    }

    public void addProfiles(ArrayList<MediaProfile> profiles) {
        this.profiles.addAll(profiles);
    }

    @Override
    public String toString() {
        return "Device{" +
                "serviceUrl='" + serviceUrl + '\'' +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", ptzUrl='" + ptzUrl + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", eventUrl='" + eventUrl + '\'' +
                ", analyticsUrl='" + analyticsUrl + '\'' +
                ", shotPicUrl='" + shotPicUrl + '\'' +
                ", sipNum='" + sipNum + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", name='" + name + '\'' +
                ", rtspUrl='" + rtspUrl + '\'' +
                ", videoBen=" + videoBen +
                ", profiles=" + profiles +
                '}';
    }
}

