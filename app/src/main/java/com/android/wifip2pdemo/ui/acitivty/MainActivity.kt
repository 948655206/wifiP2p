package com.android.wifip2pdemo.ui.acitivty

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.ScrollState.Companion.Saver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.wifip2pdemo.broadCast.WiFiDirectBroadcastReceiver
import com.android.wifip2pdemo.ui.acitivty.base.BaseVMActivity
import com.android.wifip2pdemo.ui.compose.Screen
import com.android.wifip2pdemo.ui.compose.Screen.HOME_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.MESSAGE_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.RECEIVER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.REQUEST_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.SENDER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.SERVER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.fragment.HomeFragment.homeFragment
import com.android.wifip2pdemo.ui.compose.fragment.MessageFragment.messageFragment
import com.android.wifip2pdemo.ui.compose.fragment.ReceiveFragment.receiverFragment
import com.android.wifip2pdemo.ui.compose.fragment.SenderFragment.senderFragment
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.CONNECT_PREPARE
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

        val sdkVersion = Build.VERSION.SDK_INT
        val isWifiP2pSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)

        if (sdkVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && isWifiP2pSupported) {
            // 设备支持 WifiP2p 功能
            // 进行相应的处理
            LogUtils.i("有权限")
        } else {
            // 设备不支持 WifiP2p 功能
            // 提示用户该设备不支持该功能
            LogUtils.i("无权限")
        }


        //请求权限
        val permissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
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
            //设备状态变化
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            //附近设备变化
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            //搜索状态
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            //连接状态
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            //本设备操作变更
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)
        }
    }


    @Composable
    fun showTitle() {

        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = HOME_FRAGMENT) {
            composable(HOME_FRAGMENT) {
                homeFragment(
                    navController,
                )
                LaunchedEffect(Unit) {
                    viewModel.disconnect()
                    manager.stopPeerDiscovery(mChannel, null)
                    viewModel.connectState.postValue(CONNECT_PREPARE)
                }

            }

            composable(SENDER_FRAGMENT) {
                senderFragment(
                    navController,
                    viewModel
                )
                manager.discoverPeers(mChannel, null)
                LaunchedEffect(Unit) {
                    viewModel.disconnect()
                    viewModel.connectState.postValue(CONNECT_PREPARE)
                }
            }

            composable(RECEIVER_FRAGMENT) {
                receiverFragment(
                    navController,
                    viewModel
                )
            }

            composable(MESSAGE_FRAGMENT) {
                messageFragment(
                    navController,
                    viewModel,
                ) {

                    LogUtils.i("打开相册...")
                    val intentPick =
                        Intent(Intent.ACTION_GET_CONTENT)
                    intentPick.type = "application/zip"
                    startActivityForResult(intentPick, 666)
//                    val intentPick =
//                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//                    startActivityForResult(intentPick, 666)
                }
                viewModel.chooseState.postValue(ChooseState.MESSAGE_FRAGMENT)
            }

            composable(SERVER_FRAGMENT) {
                LaunchedEffect(Unit) {
                    viewModel.createService()
                }
            }
            composable(REQUEST_FRAGMENT) {
                LaunchedEffect(Unit) {
                    viewModel.requestService()
                }
            }
        }

        val connectState by viewModel.connectState.observeAsState()

        when (connectState) {
            WifiP2pViewModel.ConnectState.FIRST_TIME -> {
                ToastUtils.showShort("欢迎使用!")
            }
            CONNECT_PREPARE -> {
                LogUtils.i("准备选择中...")
            }
            WifiP2pViewModel.ConnectState.CONNECT_LOADING -> {
                ToastUtils.showShort("加载中...")
                LogUtils.i("加载中...")
                navController.navigate(MESSAGE_FRAGMENT)
            }
            WifiP2pViewModel.ConnectState.CONNECT_SUCCESS -> {
                LogUtils.i("连接成功...")
            }
            WifiP2pViewModel.ConnectState.CONNECT_DISCONNECT -> {
                LogUtils.i("断开连接...")
            }
            WifiP2pViewModel.ConnectState.CONNECT_FAILURE -> {
                LogUtils.e("连接失败...")
                navController.navigate(HOME_FRAGMENT)
            }
            WifiP2pViewModel.ConnectState.CONNECT_CREATER -> {
                LogUtils.i("CONNECT_CREATER...")
            }
            WifiP2pViewModel.ConnectState.CONNECT_STOP -> {
                LogUtils.i("连接结束")
                navController.navigate(HOME_FRAGMENT)
            }
            else -> {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 666 && resultCode == RESULT_OK) {
            val uri = data?.data!!

            //测试速率
            viewModel.sendFileByUri(uri)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    @Composable
    override fun setView() {
        showTitle()

    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.i("看不见了...")
        viewModel.disconnect()
        unregisterReceiver(receiver)
    }
}