package com.android.wifip2pdemo.ui.acitivty

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.wifip2pdemo.broadCast.WiFiDirectBroadcastReceiver
import com.android.wifip2pdemo.ui.acitivty.base.BaseVMActivity
import com.android.wifip2pdemo.ui.compose.Screen
import com.android.wifip2pdemo.ui.compose.Screen.HOME_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.RECEIVER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.SENDER_FRAGMENT
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.XXPermissions

class MainActivity : BaseVMActivity<WifiP2pViewModel>(WifiP2pViewModel::class.java) {


    private val manager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }


    private var mChannel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    private var intentFilter: IntentFilter? = null


    override fun setEvent() {
        //请求权限
        val permissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (!XXPermissions.isGranted(this, permissions)) {
            XXPermissions.with(this)
                .permission(XXPermissions.getDenied(this, permissions))
                .request { _, allGranted ->
                    if (allGranted) {
                        ToastUtils.showShort("获取权限成功...")
                    } else {
                        ToastUtils.showShort("部分权限未获取...")
                    }
                }
        } else {
            LogUtils.i("全部授予权限了...")
        }

        mChannel = manager.initialize(this, mainLooper, null)
        viewModel.manager = this.manager
        viewModel.mChannel = this.mChannel
        mChannel?.also { channel ->
            receiver = WiFiDirectBroadcastReceiver(manager, channel, viewModel)
        }

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            //搜索状态
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                addAction(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        LogUtils.v("显示页面...")
        intentFilter?.let {
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        LogUtils.i("看不见了...")
        receiver.also {
            unregisterReceiver(receiver)
        }
    }

    @SuppressLint("NewApi")
    @Composable
    fun showTitle() {
        val navController = rememberNavController()

        val chooseState by viewModel.chooseState.observeAsState()
        when (chooseState) {
            ChooseState.HOME_FRAGMENT -> {
                LogUtils.i("HOME_FRAGMENT")
            }
            ChooseState.SENDER_FRAGMENT -> {
                LogUtils.i("SENDER_FRAGMENT")

            }
            ChooseState.RECEIVER_FRAGMENT -> {
                LogUtils.i("RECEIVER_FRAGMENT")
                viewModel.createNewGroup()
                viewModel.receiveMessage()
            }
            null -> {}
        }
        NavHost(navController = navController, startDestination = HOME_FRAGMENT) {
            composable(HOME_FRAGMENT) {
//                manager.apply {
//                    stopPeerDiscovery(mChannel,null)
//
//                    cancelConnect(mChannel,object :WifiP2pManager.ActionListener{
//                        override fun onSuccess() {
//                            println("断开连接成功....")
//                        }
//
//                        override fun onFailure(p0: Int) {
//                            println("断开连接失败....$p0")
//                        }
//                    })
//                }
                Screen.homeFragment(
                    navController
                )
                viewModel.chooseState.postValue(ChooseState.SENDER_FRAGMENT)

            }

            composable(SENDER_FRAGMENT) {
                manager?.discoverPeers(mChannel, null)
                Screen.senderFragment(
                    navController,
                    viewModel
                )
                viewModel.chooseState.postValue(ChooseState.SENDER_FRAGMENT)
            }

            composable(RECEIVER_FRAGMENT) {
                Screen.receiverFragment(
                    navController,
                    viewModel
                )
                //解决compose 重复两次的BUG
                viewModel.chooseState.postValue(ChooseState.RECEIVER_FRAGMENT)
            }
        }
    }

    fun isWifiDirectSupported(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
    }


    @Composable
    override fun setView() {
        showTitle()
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}