package com.android.wifip2pdemo.broadCast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Address
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.*
import com.android.wifip2pdemo.viewModel.WifiState
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.impl.DNSRecord.IPv4Address


/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val viewModel: WifiP2pViewModel
) : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                LogUtils.i("搜索状态改变了...")
                val state = intent.getIntExtra(
                    WifiP2pManager.EXTRA_DISCOVERY_STATE,
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED
                )
                when (state) {
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> {
                        LogUtils.i("开始搜索...")
                        viewModel.setState(WifiState.WIFI_P2P_DISCOVERY_STARTED)
                    }
                    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> {
                        LogUtils.i("搜索停止...")
                        viewModel.setState(WifiState.WIFI_P2P_DISCOVERY_STOPPED)
                    }
                    else -> {}
                }
            }
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                LogUtils.i("123状态改变...")
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wi-Fi Direct (P2P)已启用
                    LogUtils.i("Wi-Fi Direct (P2P)已启用...")

                } else {
                    // Wi-Fi Direct (P2P)已禁用
                    LogUtils.e("Wi-Fi Direct (P2P)已禁用")
                }

            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                viewModel.setState(WifiState.WIFI_P2P_PEERS_CHANGED_ACTION)

                manager.apply {

                    requestPeers(channel) { peers ->
                        viewModel.addPeer(peers.deviceList.toList())

                        if (peers.deviceList.isNotEmpty()) {
                            peers.deviceList.forEach {device->
                                LogUtils.i("当前设备状态==>${device.deviceName}....${device.status}")
                            }
                        }
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                viewModel.setState(WifiState.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                LogUtils.i("连接状态改变...")

                manager.requestConnectionInfo(channel) { info ->
                    val address = info.groupOwnerAddress
                    val groupOwner = info.isGroupOwner
                    val formed = info.groupFormed

                    println("是否形成组==>$formed")
                    println("是否为组长==>${groupOwner}")
                    println("连接信息==>${address}")

                    if (groupOwner) {
                        //如果是组长
                        viewModel.connectState.postValue(CONNECT_CREATER)
                        manager.requestGroupInfo(channel) { group ->
                            group?.let {
                                LogUtils.i("组员==>${it.clientList.size}")

                                viewModel.discoverPeers()

//                                viewModel.addPeer(it.clientList.toList())

//                                it.clientList.forEach {
//                                    LogUtils.i("mac地址==》${it.deviceAddress}")
//                                }
                                LogUtils.i("信道==>${it.frequency}")
                            }
                        }


                    }
                    if (formed && !groupOwner) {
                        //如果是组组员
                        viewModel.connectState.postValue(CONNECT_SUCCESS)
                    }
                    if (!formed){
                        if (viewModel.connectState.value==CONNECT_SUCCESS) {
                            //如果之前已经形成则是断开连接
                            viewModel.connectState.postValue(WifiP2pViewModel.ConnectState.CONNECT_STOP)
                        }
                    }
                }


            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing


            }
            else -> {

            }

        }
    }


    fun getIpAddressFromMacAddress(macAddress: String): String? {
        val macBytes = macAddress.split(":", "-")
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByAddress(macBytes))

        val addresses = networkInterface.inetAddresses
        for (address in addresses) {
            if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                return address.hostAddress
            }
        }
        return null
    }
}