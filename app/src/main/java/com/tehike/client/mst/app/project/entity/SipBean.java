package com.tehike.client.mst.app.project.entity;

import java.io.Serializable;
/**
 * 描述：取cms上sip内容对应的实体类
 *
 * 属性：详见文档
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2018/11/12 15:56
 * @version V1.0
 */

public class SipBean implements Serializable {

	private String flage;
	private String id;
	private String ip;
	private String name;
	private String deviceType;
	private int sentry;
	private String number;
	private String sipserver;
	private String sipname;
	private String sippass;
	private VideoBen videoBen;
	private String rtsp;
	private boolean isSuporrtPtz;
	private  String ptz_url;
	private String token;



	public SipBean() {
	}


	public String getFlage() {
		return flage;
	}

	public void setFlage(String flage) {
		this.flage = flage;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSentry() {
		return sentry;
	}

	public void setSentry(int sentry) {
		this.sentry = sentry;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getSipserver() {
		return sipserver;
	}

	public void setSipserver(String sipserver) {
		this.sipserver = sipserver;
	}

	public String getSipname() {
		return sipname;
	}

	public void setSipname(String sipname) {
		this.sipname = sipname;
	}

	public String getSippass() {
		return sippass;
	}

	public void setSippass(String sippass) {
		this.sippass = sippass;
	}

	public VideoBen getVideoBen() {
		return videoBen;
	}

	public void setVideoBen(VideoBen videoBen) {
		this.videoBen = videoBen;
	}

	public String getRtsp() {
		return rtsp;
	}

	public void setRtsp(String rtsp) {
		this.rtsp = rtsp;
	}

	public boolean isSuporrtPtz() {
		return isSuporrtPtz;
	}

	public void setSuporrtPtz(boolean suporrtPtz) {
		isSuporrtPtz = suporrtPtz;
	}

	public String getPtz_url() {
		return ptz_url;
	}

	public void setPtz_url(String ptz_url) {
		this.ptz_url = ptz_url;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public SipBean(String flage, String id, String ip, String name, String deviceType, int sentry, String number, String sipserver, String sipname, String sippass, VideoBen videoBen, String rtsp, boolean isSuporrtPtz, String ptz_url, String token) {
		this.flage = flage;
		this.id = id;
		this.ip = ip;
		this.name = name;
		this.deviceType = deviceType;
		this.sentry = sentry;
		this.number = number;
		this.sipserver = sipserver;
		this.sipname = sipname;
		this.sippass = sippass;
		this.videoBen = videoBen;
		this.rtsp = rtsp;
		this.isSuporrtPtz = isSuporrtPtz;
		this.ptz_url = ptz_url;
		this.token = token;
	}

	@Override
	public String toString() {
		return "SipBean{" +
				"flage='" + flage + '\'' +
				", id='" + id + '\'' +
				", ip='" + ip + '\'' +
				", name='" + name + '\'' +
				", deviceType='" + deviceType + '\'' +
				", sentry=" + sentry +
				", number='" + number + '\'' +
				", sipserver='" + sipserver + '\'' +
				", sipname='" + sipname + '\'' +
				", sippass='" + sippass + '\'' +
				", videoBen=" + videoBen +
				", rtsp='" + rtsp + '\'' +
				", isSuporrtPtz=" + isSuporrtPtz +
				", ptz_url='" + ptz_url + '\'' +
				", token='" + token + '\'' +
				'}';
	}
}
