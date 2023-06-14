package com.android.wifip2pdemo.ui.acitivty

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.net.wifi.p2p.WifiP2pWfdInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.ServiceManager
import android.os.SystemPropertiesProto.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.wifip2pdemo.broadCast.WiFiDirectBroadcastReceiver
import com.android.wifip2pdemo.ui.acitivty.base.BaseVMActivity
import com.android.wifip2pdemo.ui.compose.Screen.FIND_AIRPLAY
import com.android.wifip2pdemo.ui.compose.Screen.HOME_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.MESSAGE_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.RECEIVER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.FIND_MIRACAST
import com.android.wifip2pdemo.ui.compose.Screen.SENDER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.Screen.SERVER_FRAGMENT
import com.android.wifip2pdemo.ui.compose.fragment.HomeFragment.homeFragment
import com.android.wifip2pdemo.ui.compose.fragment.MessageFragment.messageFragment
import com.android.wifip2pdemo.ui.compose.fragment.ReceiveFragment.receiverFragment
import com.android.wifip2pdemo.ui.compose.fragment.SenderFragment.senderFragment
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.CONNECT_PREPARE
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.XXPermissions


class MainActivity : BaseVMActivity<WifiP2pViewModel>(WifiP2pViewModel::class.java) {


    private val manager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }


    private var mChannel: Channel? = null
    private var receiver: BroadcastReceiver? = null
    private var intentFilter: IntentFilter? = null


    override fun setEvent() {

        val sdkVersion = Build.VERSION.SDK_INT
        val isWifiP2pSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            LogUtils.i("WiFi已经开启")
        } else {
            LogUtils.e("WiF未开启")
            wifiManager.isWifiEnabled = true

        }

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
            ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            LogUtils.i("支持wifiP2p")
        } else {
            LogUtils.e("不支持wifiP2p")
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

                LaunchedEffect(Unit) {
                    viewModel.owner = 0
                    viewModel.disconnect()
                    manager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            LogUtils.i("开始搜索成功。。。。")
                        }

                        override fun onFailure(reason: Int) {
                            LogUtils.e("开始搜索失败。。。。$reason")
                        }

                    })
                    viewModel.connectState.postValue(CONNECT_PREPARE)
                }
            }

            composable(RECEIVER_FRAGMENT) {
                receiverFragment(
                    navController,
                    viewModel
                )
                LaunchedEffect(Unit) {
                    viewModel.owner = 15
                    viewModel.connectState.postValue(CONNECT_PREPARE)

                }
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

            composable(FIND_AIRPLAY) {


                //airplay发现p2p设备 还未实现
                val SERVICE_TYPE_AIRPLAY = "_airplay._tcp"
                val SERVICE_TYPE_RAOP = "_raop._tcp"
                val MAC_DEVICE = DeviceUtils.getMacAddress().replace(":", "")
                val inStanceName = "zxyTv"

//                val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
//                nsdManager.registerService(NsdServiceInfo().apply {
//                    serviceName = "zxyTv"
//                    serviceType = SERVICE_TYPE_AIRPLAY
//                    port = 5353
//                    mapOf(
//                        "rmodel" to "coolbuild1,0",
//                        "srcvers" to "220.68",
//                        "deviceid" to DeviceUtils.getMacAddress(),
//                        "features" to "0x5A7FEFF0",
//                        "model" to "AppleTV3, 2",
//                        "flags" to "0x4",
//                        "pk" to "3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29",
//                        "pi" to "12345678-9abc-def0-0000-2e0547f4a307",
//                        "psi" to "12345678-9abc-def0-0000-2e0547f4a307",
//                        "vv" to "2",
//                    ).forEach { it ->
//                        setAttribute(it.key, it.value)
//                    }
//                },
//                    NsdManager.PROTOCOL_DNS_SD,
//                    object : NsdManager.RegistrationListener {
//                        override fun onRegistrationFailed(
//                            serviceInfo: NsdServiceInfo?,
//                            errorCode: Int
//                        ) {
//                            LogUtils.i("11111111")
//                        }
//
//                        override fun onUnregistrationFailed(
//                            serviceInfo: NsdServiceInfo?,
//                            errorCode: Int
//                        ) {
//                            LogUtils.i("222222")
//                        }
//
//                        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
//                            LogUtils.i("33333333")
//                        }
//
//                        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
//                            LogUtils.i("44444444")
//                        }
//
//                    })
//                nsdManager.discoverServices(
//                    SERVICE_TYPE_AIRPLAY,
//                    NsdManager.PROTOCOL_DNS_SD,
//                    object : NsdManager.DiscoveryListener {
//                        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
//                            LogUtils.e("开启失败...")
//                        }
//
//                        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
//                            LogUtils.e("开启失败...")
//                        }
//
//                        override fun onDiscoveryStarted(serviceType: String?) {
//                            LogUtils.i("1231123")
//
//                        }
//
//                        override fun onDiscoveryStopped(serviceType: String?) {
//                            LogUtils.i("1231123")
//
//                        }
//
//                        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
//                            LogUtils.i("发现设备了....")
//                            nsdManager.resolveService(serviceInfo,
//                                object : NsdManager.ResolveListener {
//                                    override fun onResolveFailed(
//                                        serviceInfo: NsdServiceInfo?,
//                                        errorCode: Int
//                                    ) {
//                                        LogUtils.e("发送设备失败...$errorCode")
//                                    }
//
//                                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
//                                        LogUtils.i("设备发现成功==>${serviceInfo?.serviceName}")
//                                    }
//
//                                })
//                        }
//
//                        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
//                            LogUtils.i("1231123")
//                        }
//
//                    }
//                )

                val airplayInfo = WifiP2pDnsSdServiceInfo.newInstance(
                    inStanceName + "." + SERVICE_TYPE_AIRPLAY,
                    SERVICE_TYPE_AIRPLAY,
                    mapOf(
                        "rmodel" to "coolbuild1,0",
                        "srcvers" to "220.68",
                        "deviceid" to DeviceUtils.getMacAddress(),
                        "features" to "0x5A7FEFF0",
                        "model" to "AppleTV3, 2",
                        "flags" to "0x4",
                        "pk" to "3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29",
                        "pi" to "12345678-9abc-def0-0000-2e0547f4a307",
                        "psi" to "12345678-9abc-def0-0000-2e0547f4a307",
                        "vv" to "2",
                    )
                )

//                val raopInfo = WifiP2pDnsSdServiceInfo.newInstance(
//                    MAC_DEVICE + "@" + inStanceName + "." + SERVICE_TYPE_RAOP,
//                    SERVICE_TYPE_RAOP,
//                    mapOf(
//                        "txtvers" to "1",
//                        "ch" to "2",
//                        "cn" to "0,3",
//                        "et" to "0,3,5",
//                        "sv" to "false",
//                        "da" to "true",
//                        "sr" to "44100",
//                        "ss" to "16",
//                        "pw" to "false",
//                        "vn" to "3",
//                        "tp" to "UDP",
//                        "md" to "0,1,2",
//                        "vs" to "130.1",
//                        "sm" to "false",
//                        "ek" to "1",
//                        "sf" to "0x4",
//                        "am" to "KUV,1",
//                        "pk" to "3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29",
//                    )
//
//                )


                manager.apply {
                    setDnsSdResponseListeners(mChannel,  object :WifiP2pManager.DnsSdServiceResponseListener{
                        override fun onDnsSdServiceAvailable(
                            instanceName: String?,
                            registrationType: String?,
                            srcDevice: WifiP2pDevice?
                        ) {
                            LogUtils.i("DNS收到信息了...==>${srcDevice?.deviceName}")
                        }

                    }) { _, _, srcDevice ->
                        LogUtils.i("DNS收到信息了...==>${srcDevice.deviceName}")

                    }
                    addLocalService(mChannel, airplayInfo, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            LogUtils.i("添加成功${inStanceName + "." + SERVICE_TYPE_AIRPLAY}")

                        }

                        override fun onFailure(reason: Int) {
                            LogUtils.e("添加失败....$reason")
                        }

                    })
//                    addLocalService(mChannel, raopInfo, object : WifiP2pManager.ActionListener {
//                        override fun onSuccess() {
//                            LogUtils.i("添加成功${MAC_DEVICE + "@" + inStanceName + "." + SERVICE_TYPE_RAOP}")
//
//                        }
//
//                        override fun onFailure(reason: Int) {
//                            LogUtils.e("添加失败....$reason")
//                        }
//
//                    })

                    addServiceRequest(mChannel,
                        WifiP2pDnsSdServiceRequest.newInstance(), object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                LogUtils.i("添加成功...")

                            }

                            override fun onFailure(reason: Int) {
                                LogUtils.e("添加失败...$reason")
                            }

                        })
                    discoverServices(mChannel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            LogUtils.i("搜索服务成功..")

                        }

                        override fun onFailure(reason: Int) {
                            LogUtils.e("搜索服务失败..$reason")
                        }

                    })
                    setServiceResponseListener(mChannel,
                        object : WifiP2pManager.ServiceResponseListener {
                            override fun onServiceAvailable(
                                protocolType: Int,
                                responseData: ByteArray?,
                                srcDevice: WifiP2pDevice?
                            ) {
                                LogUtils.i("收到服务回调了==>${srcDevice?.deviceName}")
                            }

                        })


                }

                LogUtils.i("启动了...==>")
            }

            composable(SERVER_FRAGMENT) {
                LogUtils.i("SERVER_FRAGMENT")


                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        LogUtils.i("android11以上设备，MIRACAST_SOURCE")


                        //让miracast中发现 p2p设备
                        val wifiP2pWfdInfo = WifiP2pWfdInfo()

                        wifiP2pWfdInfo.apply {
                            isEnabled = true
                            deviceType = WifiP2pWfdInfo.DEVICE_TYPE_PRIMARY_SINK
                            isSessionAvailable = true
                        }

                        manager.setWfdInfo(mChannel!!, wifiP2pWfdInfo,
                            object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    LogUtils.i("添加成功...")
                                }

                                override fun onFailure(reason: Int) {
                                    LogUtils.e("添加服务失败...$reason")
                                }

                            }
                        )


                    } else {
                        LogUtils.i("android11以下设备修改成功...")
                        manager.setDeviceName(
                            mChannel!!,
                            "Mi 113",
                            object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    LogUtils.i("修改成功...")
                                }

                                override fun onFailure(p0: Int) {
                                    LogUtils.e("修改失败...$p0")
                                }

                            })

                    }

                }

            }
            composable(FIND_MIRACAST) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val wfdInfo = WifiP2pWfdInfo().apply {
                        isEnabled = true
                        deviceType = WifiP2pWfdInfo.DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK
                        isSessionAvailable = true
                    }
                    manager?.setWfdInfo(
                        mChannel!!,
                        wfdInfo,
                        object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                LogUtils.i("Miracast中发现成功..")
                            }

                            override fun onFailure(reason: Int) {
                                LogUtils.e("Miracast中发现失败..$reason")
                            }

                        })
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
                navController.navigate(MESSAGE_FRAGMENT)
                manager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        LogUtils.i("开始搜索成功。。。。")
                    }

                    override fun onFailure(reason: Int) {
                        LogUtils.e("开始搜索失败。。。。$reason")
                    }

                })
            }
            WifiP2pViewModel.ConnectState.CONNECT_DISCONNECT -> {
                LogUtils.i("断开连接...")
            }
            WifiP2pViewModel.ConnectState.CONNECT_FAILURE -> {
                LogUtils.e("连接失败...")
                navController.navigate(HOME_FRAGMENT)
            }
            WifiP2pViewModel.ConnectState.CONNECT_CREATER -> {

//                navController.navigate(MESSAGE_FRAGMENT)
                LogUtils.i("CONNECT_CREATER...")
                ToastUtils.showShort("此设备是组长..")
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