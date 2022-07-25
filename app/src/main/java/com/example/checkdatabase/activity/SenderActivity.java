package com.example.checkdatabase.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.checkdatabase.Contants.Contants;
import com.example.checkdatabase.LocalMacAddress;
import com.example.checkdatabase.MainActivity;
import com.example.checkdatabase.R;
import com.example.checkdatabase.SreamTool;
import com.example.checkdatabase.adapter.SenderAdapter;
import com.example.checkdatabase.broadcast.WiFiDirectBroadcastSender;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SenderActivity extends AppCompatActivity {
    public static String TAG="SenderActivity";
    private WifiP2pManager mManager ;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter  ;
    private WifiP2pManager.Channel mChannel;
    private ListView mReceiverLv;
    private SenderAdapter mSenderAdapter;
    private List<WifiP2pDevice> peers =new ArrayList<>();
    String[] deviceNameArry;
    WifiP2pDevice[] deviceArry;
    private WifiP2pInfo mWifiP2pInfo;
    int group;//标记组是否创建
    AlertDialog alertDialog;
    private int isFinished= Contants.IS_NOT_START;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        group=0;//没有组 group=1 为有组

        //判定是否可用
        String Mac= LocalMacAddress.getLocalMacAddress();
        Log.d(TAG, "ReceiverOnClick: "+Mac);

        new Thread(){
            @Override
            public void run() {
                try {
                    Log.d(TAG, "onCreate:1 "+InetAddress.getLocalHost());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }.start();


        if (true) {

            mManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel=mManager.initialize(this,getMainLooper(), null);

            initView();


            mReceiver=new WiFiDirectBroadcastSender(mManager,mChannel, SenderActivity.this);
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



            //注册广播
            registerReceiver(mReceiver,mIntentFilter);

        }

        if (mManager != null) {
            //发现
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "开始搜索成功: ");

                }
                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "onFailure: "+i);
                }
            });
        }

    }

    //监听 搜索完了 获取搜索到的内容
    public WifiP2pManager.PeerListListener mPeerListListener =new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArry=new String[peerList.getDeviceList().size()];
                deviceArry=new WifiP2pDevice[peerList.getDeviceList().size()];
                int index=0;

                for (WifiP2pDevice device:peerList.getDeviceList()){
                    deviceNameArry[index]=device.deviceName;
                    deviceArry[index]=device;
                    index++;
                }
                mSenderAdapter=new SenderAdapter(deviceNameArry,SenderActivity.this);
                mSenderAdapter.notifyDataSetInvalidated();

                mReceiverLv.setAdapter(mSenderAdapter);
                mReceiverLv.setOnItemClickListener(mOnItemClickListener);
            }
        }
    };

    public WifiP2pDevice mDevice=null;
    //选择连接那个设备进行连接
    AdapterView.OnItemClickListener mOnItemClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mDevice = deviceArry[i];
            WifiP2pConfig config=new WifiP2pConfig();
            config.deviceAddress= mDevice.deviceAddress;


            mManager.connect(mChannel, config,new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "连接成功123: ");
                    Toast.makeText(SenderActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "失败: "+i);
                }
            });
        }
    };

    private Handler mHandler=new Handler(Looper.myLooper()){
        @SuppressLint("NewApi")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what!=1){
                return;
            }
            int message= (int) msg.obj;
            switch (message){
                case Contants.IS_NOT_START:
                    Toast.makeText(SenderActivity.this,"传输启动失败！",Toast.LENGTH_SHORT).show();
                    break;
                case Contants.IS_TRANSPORT_SUCCESS:
                    Log.d(TAG, "收到子线程发来的信息了: ");
                    if (alertDialog.isShowing()){
                        alertDialog.dismiss();
                    }
                    Intent intent=new Intent();
                    intent.setClass(SenderActivity.this, MainActivity.class);
                    startActivity(intent);
                    Toast.makeText(SenderActivity.this,"传输完成",Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case Contants.IS_TRANSPOT_FAIL:
                    if (alertDialog.isShowing()){
                        alertDialog.dismiss();
                    }
                    finish();
                    Toast.makeText(SenderActivity.this,"传输失败,请重试！",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private long mExitTime=0;
    private int t=0;
    public WifiP2pManager.ConnectionInfoListener mConnectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress=wifiP2pInfo.groupOwnerAddress;
            Log.d(TAG, "输出组长ID: "+groupOwnerAddress);
            mWifiP2pInfo=wifiP2pInfo;
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                //如果是组长.....
                Log.d(TAG, "我是组长: ");
            }
            else if (wifiP2pInfo.groupFormed ){
                //如果是组员.....
                Log.d(TAG, "我是组员: ");
                group=1;
                new Thread(new Runnable() {

                    private Socket mSocket;

                    @Override
                    public void run() {
                        Log.d(TAG, "run: ");
                        try {
                            //socket启动
                            mSocket = new Socket(groupOwnerAddress,8887);
                            //发送
                            Log.d(TAG, "发送文件启动: ");
                            //dialog
                            runOnUiThread(new Runnable() {
                                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder=new AlertDialog.Builder(SenderActivity.this);
                                    alertDialog=builder.setView(R.layout.dialoy_layout).show();
                                    alertDialog.setCancelable(false);
                                    alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                                            if (keyCode==keyEvent.KEYCODE_BACK && keyEvent.getRepeatCount()==0 && keyEvent.getAction()==KeyEvent.ACTION_DOWN){
                                                if ((System.currentTimeMillis()-mExitTime)>2000){
                                                    Log.d(TAG, "点击了一次: "+System.currentTimeMillis());
                                                    Toast.makeText(SenderActivity.this,"再次点击退出！",Toast.LENGTH_SHORT).show();
                                                    mExitTime =System.currentTimeMillis();
                                                }
                                                else {
                                                    Log.d(TAG, "连续点击两次: "+mExitTime);
                                                    Toast.makeText(SenderActivity.this,"传输中断！",Toast.LENGTH_SHORT).show();
                                                    alertDialog.dismiss();
                                                    finish();
                                                }
                                            }
                                            return false;
                                        }
                                    });
                                }
                            });

                            SreamTool sreamTool=new SreamTool();
                            sreamTool.send(mSocket);
                            isFinished=sreamTool.isFinished;
                            Log.d(TAG, "发送文件完成: ");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "socket启动失败: ");
                        }
                        //socket关闭
                        try {
                            if (mSocket!=null){
                                mSocket.close();
                                Log.d(TAG, "socket关闭！: ");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "socket关闭失败: ");
                        }



                        Message message=new Message();
                        //消息代码,确定主线程能收到
                        message.what=1;
                        //信息内容
                        message.obj=isFinished;
                        //向主线程发送信息
                        mHandler.sendMessage(message);
                    }
                }).start();
            }
        }
    };
    private void initView() {
        //初始控件
        mReceiverLv = this.findViewById(R.id.receiver_lv);
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止
        stop();
    }

    private void stop() {
        WifiP2pManager.ActionListener mActionListener=new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 123");
            }

            @Override
            public void onFailure(int i) {

                Log.d(TAG, "onFailure123: "+i);
            }
        };
        //断开连接
//        Log.d(TAG, "开始执行断开连接: ");
        //搜索停止
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mManager.stopPeerDiscovery(mChannel,mActionListener);
        }
        //TODO: 先打开传输端口
        //删除组 没有组group=0
        if (group==1){
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "删除组成功: ");
                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "删除组失败: "+i);
                }
            });
        }
        //取消注册
        unregisterReceiver(mReceiver);
    }

}