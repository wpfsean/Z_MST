package com.tehike.client.mst.app.project.entity;

import java.io.Serializable;
/**
 * 描述：数据分组时对应的实体类
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2018/11/12 16:00
 * @version V1.0
 */
public class SipGroupBean implements Serializable {

    private String flage;
    private int group_id;
    private String group_name;
    private int sip_count;

    @Override
    public String toString() {
        return "SipGroupBean{" +
                "flage='" + flage + '\'' +
                ", group_id=" + group_id +
                ", group_name='" + group_name + '\'' +
                ", sip_count=" + sip_count +
                '}';
    }

    public SipGroupBean() {
    }

    public SipGroupBean(String flage, int group_id, String group_name, int sip_count) {

        this.flage = flage;
        this.group_id = group_id;
        this.group_name = group_name;
        this.sip_count = sip_count;
    }

    public String getFlage() {

        return flage;
    }

    public void setFlage(String flage) {
        this.flage = flage;
    }

    public int getGroup_id() {
        return group_id;
    }

    public void setGroup_id(int group_id) {
        this.group_id = group_id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public int getSip_count() {
        return sip_count;
    }

    public void setSip_count(int sip_count) {
        this.sip_count = sip_count;
    }
}
