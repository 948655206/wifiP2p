package com.example.checkdatabase.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.example.checkdatabase.activity.ReceiverActivity;

import java.util.ArrayList;
import java.util.List;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG="WifiP2pReceiver";
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private ReceiverActivity mActivity;
    private List<WifiP2pDevice> mList=new ArrayList<>();


    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       ReceiverActivity activity) {
        this.mManager= manager;
        this.mChannel= channel;
        this.mActivity =activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // 检查Wi-fi是否已经启用。并通知适当的activity
            int state=intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // WiFi P2P 能够使用
                Log.d(TAG, "wifip2p能够使用: "+state);
            } else {
                // WiFi P2P 不能够使用
                Log.d(TAG, "wifiPp2p不能使用: "+state);
            }
        }

        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "wifi搜索已经监听到: ");
            // 调用WifiP2pManager.requestPeers()来获取当前的对等网络（设备）列表
            if (mManager!=null){
                mManager.requestPeers(mChannel, mActivity.mPeerListListener);
                Log.d(TAG, "成功搜索到: ");
            }else {
                Log.d(TAG, "mManager为空: ");
                return;
            }
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // 响应新的连接或者断开连接
            Log.d(TAG, "连接状态改变: ");
            if (mManager==null){
                return;
            }
            NetworkInfo networkInfo=intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()){
                Log.d(TAG, "连接中: ");
                mManager.requestConnectionInfo(mChannel, mActivity.mConnectionInfoListener);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // 响应本台设备wifi状态的改变
        }
    }



}
