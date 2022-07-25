package com.example.checkdatabase;

import android.net.wifi.p2p.WifiP2pDevice;

public class UserBean {
    private String deviceName;
    private WifiP2pDevice mDevice;
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public WifiP2pDevice getDevice() {
        return mDevice;
    }

    public void setDevice(WifiP2pDevice device) {
        mDevice = device;
    }

    @Override
    public String toString() {
        return "UserBean{" +
                "deviceName='" + deviceName + '\'' +
                ", mDevice=" + mDevice +
                '}';
    }
}
