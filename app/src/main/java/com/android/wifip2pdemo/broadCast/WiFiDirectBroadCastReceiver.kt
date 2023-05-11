package com.android.wifip2pdemo.broadCast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.CONNECT_CREATER
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.CONNECT_SUCCESS
import com.android.wifip2pdemo.viewModel.WifiState
import com.blankj.utilcode.util.LogUtils
import java.io.File


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
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                viewModel.setState(WifiState.WIFI_P2P_PEERS_CHANGED_ACTION)
                manager.apply {
                    requestPeers(channel) { peers ->
                        if (viewModel.chooseState.value==ChooseState.SENDER_FRAGMENT){
                            viewModel.addPeer(peers.deviceList.toList())
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
                        manager.requestGroupInfo(channel){group->
                            group?.let {
                                LogUtils.i("组员==>${it.clientList.size}")
                                viewModel.addPeer(it.clientList.toList())

                            }
                        }
                    }
                    if (formed && !groupOwner) {
                        //如果是组长
                        viewModel.connectState.postValue(CONNECT_SUCCESS)
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
}