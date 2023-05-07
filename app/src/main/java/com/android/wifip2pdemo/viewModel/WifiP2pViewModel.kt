package com.android.wifip2pdemo.viewModel

import android.net.wifi.p2p.*
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WifiP2pViewModel : ViewModel() {
    var mDevice: WifiP2pDevice? = null
    private val _wifiState = MutableLiveData<WifiState>()
    val wifiState = _wifiState

    private val _chooseState = MutableLiveData<ChooseState>()
    val chooseState = _chooseState

    private val _peerList = MutableLiveData<List<WifiP2pDevice>>()
    val peerList = _peerList

    var manager: WifiP2pManager? = null
    var mChannel: WifiP2pManager.Channel? = null

    var owner: Int = 0
    val port: Int = 6666

    fun setState(state: WifiState) {
        _wifiState.postValue(state)
    }

    fun addPeer(deviceList: List<WifiP2pDevice>) {
        println("加入addPeer")
        val devices = mutableListOf<WifiP2pDevice>()
        devices.addAll(deviceList)
        _peerList.postValue(devices)
    }

    //连接成员
    fun connectPeers(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        //是否想成为组长
        config.groupOwnerIntent = owner
        config

        LogUtils.i("连接地址==>${config.deviceAddress}")
        manager?.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                LogUtils.i("连接成功....")
//                sendMessage(device.deviceAddress)
                manager?.requestConnectionInfo(mChannel) { info ->
                    val address = info.groupOwnerAddress
                    val groupOwner = info.isGroupOwner
                    val groupFormed = info.groupFormed
                    println("是否形成==>${groupFormed}")
                    println("连接信息==>${groupOwner}")
                    println("连接信息==>${address}")

                    if (address != null) {
                        LogUtils.i("连接信息==>${address.hostAddress}")
                        sendMessage(address.hostAddress)
                    }
                }

            }

            override fun onFailure(p0: Int) {
                LogUtils.i("连接失败...$p0")
                if (p0 == 2) {
                    ToastUtils.showShort("已建立连接...")
                }
            }

        })
    }

    fun createNewGroup() {
        //为了防止已经有组创建，删除之前的组，重新创建
        manager?.requestGroupInfo(
            mChannel
        ) { group ->
            if (group != null) {
                LogUtils.i("当前已经存在组")
                manager?.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        LogUtils.i("删除组成功...")
                        createGroup()
                    }

                    override fun onFailure(p0: Int) {
                        LogUtils.i("删除组失败...$p0")
                    }

                })
            } else {
                createGroup()
            }
        }

    }

    private fun createGroup() {
        val config = WifiP2pConfig()
        manager?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createGroup(mChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        LogUtils.i("创建组成功...${mDevice?.deviceName}")
                        requestGroupInfo(mChannel
                        ) {group->
                            val address = group.owner.deviceAddress
                            LogUtils.i("本机IP==>$address")
                        }
                    }

                    override fun onFailure(p0: Int) {
                        LogUtils.i("创建组失败...$p0")
                    }

                })
            }
        }
    }

    //发送信息
    fun sendMessage(socketAddress: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            LogUtils.i("开始发送信息...$socketAddress")
            try {
                val message = "zxy 666"
                val socket = Socket()
                socket.connect(InetSocketAddress(socketAddress, port))
                val outputStream = socket.getOutputStream()
                outputStream.write(message.toByteArray())
                outputStream.flush()

                //记得关闭连接
            } catch (e: Exception) {
                LogUtils.e("发送信息出错了...$e")
            }
        }

    }

    fun receiveMessage() {
        LogUtils.i("开始接收信息...")
        try {
            val serverSocket = ServerSocket(port)
            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    LogUtils.i("阻塞中...")
                    val socket = serverSocket.accept()

                    viewModelScope.launch(Dispatchers.IO) {

                        val inputStream = socket.getInputStream()
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                        val message = stringBuilder.toString()

                        LogUtils.i("收到的信息...$message")
                    }

                }

            }
        } catch (e: Exception) {
            LogUtils.e("接收信息出错。。。${e.toString()}")
        }

    }
}

enum class ChooseState {
    HOME_FRAGMENT,
    SENDER_FRAGMENT,
    RECEIVER_FRAGMENT
}

enum class WifiState {
    WIFI_P2P_DISCOVERY_STARTED,     //开始搜索
    WIFI_P2P_DISCOVERY_STOPPED,     //停止搜索
    WIFI_P2P_PEERS_CHANGED_ACTION,  //搜索设备更新了...
    WIFI_P2P_CONNECTION_CHANGED_ACTION //连接状态改变
}