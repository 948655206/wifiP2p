package com.example.checkdatabase.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
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
import android.widget.Toast;

import com.example.checkdatabase.Contants.Contants;
import com.example.checkdatabase.LocalMacAddress;
import com.example.checkdatabase.MainActivity;
import com.example.checkdatabase.R;
import com.example.checkdatabase.SreamTool;
import com.example.checkdatabase.UserBean;
import com.example.checkdatabase.broadcast.WiFiDirectBroadcastReceiver;
import com.example.checkdatabase.adapter.ReCreateAdapter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ReceiverActivity extends AppCompatActivity {
    public static String TAG = "ReceiverActivity";
    private WifiP2pManager mManager;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private WifiP2pManager.Channel mChannel;
    private ListView mReceiverLv;
    private ReCreateAdapter mReCreateAdapter;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArry;
    WifiP2pDevice[] deviceArry;
    private Socket mSocket = null;
    private ServerSocket mServerSocket = null;
    AlertDialog alertDialog;
    private long mExitTime=0;
    private int isFinished= Contants.IS_NOT_START;
    private UserBean mUserBean;
    private List<UserBean> mList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

//
//        //判定是否可用
//        String Mac = LocalMacAddress.getLocalMacAddress();
//        Log.d(TAG, "ReceiverOnClick: " + Mac);

        if (true) {

            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);
            //初始化
            initView();
            //创建组
            createGroup();

            mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, ReceiverActivity.this);
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            //注册广播
            registerReceiver(mReceiver, mIntentFilter);

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
                    Log.d(TAG, "onFailure: " + i);
                }
            });
        }

    }

    private void createGroup() {
        if (mManager == null) {
            Log.d(TAG, "createGroup: mManger为Null");
            return;
        }
        //创建组,组长接受信息,组员传递信息
        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group == null) {
                    mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            //成功创建组长
                            Log.d(TAG, "创建组长成功: ");
                        }

                        @Override
                        public void onFailure(int i) {
                            //创建组长失败
                            Log.d(TAG, "创建组长失败: " + i);

                        }
                    });
                }
            }
        });
    }

    //监听 搜索完了 获取搜索到的内容
    public WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

//                deviceNameArry = new String[peerList.getDeviceList().size()];
//                deviceArry = new WifiP2pDevice[peerList.getDeviceList().size()];
                mList=new ArrayList<>();
                mUserBean=new UserBean();
                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArry[index] = device.deviceName;
                    deviceArry[index] = device;
//                    mUserBean.setDeviceName(device.deviceName);
//                    mUserBean.setDevice(device);
//                    mList.add(mUserBean);
                    index++;
                }

                mReCreateAdapter = new ReCreateAdapter(deviceNameArry, ReceiverActivity.this);
                mReceiverLv.setAdapter(mReCreateAdapter);
                mReceiverLv.setOnItemClickListener(mOnItemClickListener);
            }
        }
    };

    public WifiP2pDevice mDevice = null;
    //选择连接那个设备进行连接
    AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mDevice = deviceArry[i];
//            mDevice=mList.get(i).getDevice();
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = mDevice.deviceAddress;

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "连接成功123: ");
                    Toast.makeText(ReceiverActivity.this, "连接成功", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "连接失败: " + i);
                }
            });
        }
    };

    private Handler mHandler = new Handler(Looper.myLooper()) {


        @SuppressLint("NewApi")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what!=1){
                return;
            }
            int message= (int) msg.obj;
            switch (message){
                case Contants.IS_TRANSPORT_SUCCESS:
                    Log.d(TAG, "收到子线程发来的信息了: ");
                    if (alertDialog.isShowing()){
                        alertDialog.dismiss();
                    }
                    Intent intent=new Intent();
                    intent.setClass(ReceiverActivity.this, MainActivity.class);
                    startActivity(intent);
                    Toast.makeText(ReceiverActivity.this,"传输完成",Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case Contants.IS_TRANSPOT_FAIL:
                    Toast.makeText(ReceiverActivity.this,"传输失败,请重试！",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public WifiP2pManager.ConnectionInfoListener mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            Log.d(TAG, "输出组长ID: " + groupOwnerAddress);
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                //如果是组长.....
                Log.d(TAG, "我是组长123: ");
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            //socket监听启动
                            mServerSocket = new ServerSocket(8887);
                            Log.d(TAG, "socket监听启动: ");
                            mSocket = mServerSocket.accept();

                            //dialog
                            //dialog
                            runOnUiThread(new Runnable() {
                                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder=new AlertDialog.Builder(ReceiverActivity.this);
                                    alertDialog=builder.setView(R.layout.dialoy_layout).show();
                                    alertDialog.setCancelable(false);
                                    alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                                            if (keyCode==keyEvent.KEYCODE_BACK && keyEvent.getRepeatCount()==0 && keyEvent.getAction()==KeyEvent.ACTION_DOWN){
                                                if ((System.currentTimeMillis()-mExitTime)>2000){
                                                    Log.d(TAG, "点击了一次: "+System.currentTimeMillis());
                                                    Toast.makeText(ReceiverActivity.this,"再次点击退出！",Toast.LENGTH_SHORT).show();
                                                    mExitTime =System.currentTimeMillis();
                                                }
                                                else {
                                                    Log.d(TAG, "连续点击两次: "+mExitTime);
                                                    Toast.makeText(ReceiverActivity.this,"传输中断！",Toast.LENGTH_SHORT).show();
                                                    alertDialog.dismiss();
                                                    finish();
                                                }
                                            }
                                            return false;
                                        }
                                    });
                                }
                            });

                            SreamTool sreamTool = new SreamTool();
                            sreamTool.receive(mSocket);

                            isFinished=sreamTool.isFinished;
                            Log.d(TAG, "socket监听启动成功: "+isFinished);

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "socket监听已关闭！: ");
                        }


                        Message message = new Message();
                        //消息代码,确定主线程能收到
                        message.what = 1;
                        //信息内容
                        message.obj = isFinished;
                        //向主线程发送信息
                        mHandler.sendMessage(message);
                    }
                }).start();
            } else if (wifiP2pInfo.groupFormed) {
                //如果是组员.....
                Log.d(TAG, "我是组员: ");
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
        WifiP2pManager.ActionListener mActionListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 123");
            }

            @Override
            public void onFailure(int i) {

                Log.d(TAG, "onFailure123: " + i);
            }
        };
        //断开连接
        //搜索停止
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mManager.stopPeerDiscovery(mChannel, mActionListener);
        }
        //删除组
        mManager.removeGroup(mChannel, mActionListener);
        //取消注册
        unregisterReceiver(mReceiver);
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "异常关闭: ");
        }
    }
}