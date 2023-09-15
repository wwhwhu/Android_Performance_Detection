package com.wwh.updatemonitor;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class AppInfo implements Serializable {
    public int uid;
    public String label;//应用名称
    public String package_name;//应用包名
    public int pid;
    public float cpuUsage;
    public float memUsage;
    public boolean System; //是否系统应用
    public double gain;

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public AppInfo() {
        uid = 0;
        label = "";
        package_name = "";
        System = false;
        cpuUsage = 0;
        memUsage =0;
        pid = 0;
        gain = 0;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public float getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(float cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public float getMemUsage() {
        return memUsage;
    }

    public void setMemUsage(float memUsage) {
        this.memUsage = memUsage;
    }



    public boolean isSystem() {
        return System;
    }

    public void setSystem(boolean system) {
        System = system;
    }





    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPackage_name() {
        return package_name;
    }

    public void setPackage_name(String package_name) {
        this.package_name = package_name;
    }

}