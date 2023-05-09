package com.android.wifip2pdemo.viewModel

import android.app.Application
import android.net.Uri
import android.net.wifi.p2p.*
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class WifiP2pViewModel(
    private val context: Application
) : AndroidViewModel(context) {

    //连接状态
    private val _wifiState = MutableLiveData<WifiState>()
    val wifiState = _wifiState

    //页面状态
    private val _chooseState = MutableLiveData<ChooseState>()
    val chooseState = _chooseState

    //设备列表
    private val _peerList = MutableLiveData<List<WifiP2pDevice>>()
    val peerList = _peerList


//    private val _isConnected = MutableLiveData(false)
//    val isConnected = _isConnected

    private val _connectState = MutableLiveData(FIRST_TIME)
    val connectState = _connectState

    enum class ConnectState {
        FIRST_TIME,//首次进入
        CONNECT_LOADING, //正在连接中...
        CONNECT_SUCCESS, //连接成功...
        CONNECT_DISCONNECT,//未连接..
        CONNECT_CREATER,//组长
    }


    private lateinit var server: ServerSocket

    var manager: WifiP2pManager? = null
    var mChannel: WifiP2pManager.Channel? = null

    var owner: Int = 0
    val port: Int = 6666


    fun setState(state: WifiState) {
        _wifiState.postValue(state)
    }

    fun addPeer(deviceList: List<WifiP2pDevice>) {
        println("加入addPeer==>${deviceList.size}")
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

        LogUtils.i("连接地址==>${config.deviceAddress}")
        manager?.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                LogUtils.i("连接成功....${device.deviceName}")
                _chooseState.postValue(ChooseState.MESSAGE_FRAGMENT)
            }

            override fun onFailure(p0: Int) {
                LogUtils.i("连接失败...$p0")
                if (p0 == 2) {
                    ToastUtils.showShort("已建立连接...")
                    connectState.postValue(CONNECT_SUCCESS)
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
                        LogUtils.e("删除组失败...$p0")
                    }

                })
            } else {
                createGroup()
            }
        }

    }

    private fun createGroup() {
        manager?.apply {
            createGroup(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    LogUtils.i("创建组成功...")
                    receiveImage()
                    _connectState.postValue(CONNECT_CREATER)
                }

                override fun onFailure(p0: Int) {
                    LogUtils.e("创建组失败...$p0")
                }

            })
        }
    }

    fun socketSendMessage(str: String) {
        manager?.requestConnectionInfo(mChannel) { info ->
            val address = info.groupOwnerAddress
            if (address != null) {
                ToastUtils.showShort("发送信息成功...")
                viewModelScope.launch(Dispatchers.IO) {
                    val socket = Socket(address.hostAddress, port)
                    val outputStream = socket.getOutputStream()
                    val byteArray = str.toByteArray()

                    outputStream.write(byteArray)
                    outputStream.flush()
                    LogUtils.i("发送完毕...")
                    socket.close()
                }

            } else {
                ToastUtils.showShort("发送信息失败...")

            }
        }


    }

    fun receiveImage() {
        LogUtils.i("开始接收图片...")
        val byteArray = ByteArray(1024)
        try {
            server = ServerSocket(port)
            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    LogUtils.i("阻塞中...")
                    try {
                        val socket = server.accept()

                        viewModelScope.launch(Dispatchers.IO) {

                            var startTime = System.currentTimeMillis()
                            var totalRead = 0

                            val inputStream = socket.getInputStream()
                            var len: Int
                            val packageName = context.packageName
                            var totalByte = 0
                            //创建文件夹
                            val file =
                                File(context.getExternalFilesDir(null), "$packageName/zxy.zip")
                            file.parentFile?.mkdir()

                            val fileOutputStream = FileOutputStream(file)

                            while (inputStream.read(byteArray).also {
                                    len = it
                                } != -1) {
                                fileOutputStream.write(byteArray, 0, len)
                                totalByte += len

                                totalRead += len
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - startTime >= 2000) {
                                    // 每两秒打印一次接收速度
                                    val speedMbps = totalRead * 8.0 / 2000000
                                    LogUtils.i("接收速度：${speedMbps} Mbps")
                                    totalRead = 0
                                    startTime = currentTime
                                }
                            }

                            fileOutputStream.flush()
                            fileOutputStream.close()
                            LogUtils.i("路径为==>${file.absoluteFile}")
                            LogUtils.i("接收完毕...大小为..${totalByte / (1024.0 * 1024.0)}")

                            socket.close()
                            val simpleDateFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                            val format = simpleDateFormat.format(Date(System.currentTimeMillis()))
                            LogUtils.i("接收图片结束==>${format}")
                        }
                    } catch (e: Exception) {
                        LogUtils.e("socket。。。${e.toString()}")
                        break
                    }
                }

            }
        } catch (e: Exception) {
            LogUtils.e("接收信息出错。。。${e.toString()}")
        }
    }

    fun receiveMessage() {
        LogUtils.i("开始接收信息...")
        val byteArray = ByteArray(1024)
        try {
            server = ServerSocket(port)
            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    LogUtils.i("阻塞中...")
                    try {
                        val socket = server.accept()

                        viewModelScope.launch(Dispatchers.IO) {
                            val inputStream = socket.getInputStream()
                            var len: Int
                            val stringBuffer = StringBuffer()
                            while (inputStream.read(byteArray).also {
                                    len = it
                                } != -1) {
                                stringBuffer.append(String(byteArray, 0, len))
                            }
                            val string = stringBuffer.toString()
                            LogUtils.i("收到的信息...$string")
                            socket.close()
                        }
                    } catch (e: Exception) {
                        LogUtils.e("socket。。。${e.toString()}")
                        break
                    }
                }

            }
        } catch (e: Exception) {
            LogUtils.e("接收信息出错。。。${e.toString()}")
        }

    }

    fun removeGroup() {
        manager?.apply {
            cancelConnect(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    LogUtils.i("断开连接成功...")
                }

                override fun onFailure(p0: Int) {
                    LogUtils.e("断开连接失败...$p0")
                    requestGroupInfo(mChannel) { group ->
                        if (group != null) {
                            removeGroup(mChannel, object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    LogUtils.i("移除组成功....")
                                    removeGroup()
                                }

                                override fun onFailure(p0: Int) {
                                    LogUtils.i("移除组失败....$p0")
                                }

                            })
                        }
                    }
                }

            })

        }
    }

    fun disconnect() {
        try {
            //服务器断开连接...

            if (::server.isInitialized) {
                server.close()
            }
            when (connectState.value) {
                CONNECT_LOADING -> {
                    manager?.cancelConnect(mChannel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            LogUtils.i("取消连接成功...")
                            _connectState.postValue(CONNECT_DISCONNECT)
                        }

                        override fun onFailure(p0: Int) {
                            LogUtils.e("取消连接失败...$p0")
                        }

                    })

                }
                CONNECT_DISCONNECT -> {
                    LogUtils.i("未连接组....")
                }
                FIRST_TIME -> {

                }
                else -> {
                    manager?.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            LogUtils.i("移除组成功....")
                            _connectState.postValue(CONNECT_DISCONNECT)
                        }

                        override fun onFailure(p0: Int) {
                            LogUtils.e("移除组失败....$p0")
                        }
                    })
                }
            }
        } catch (e: Exception) {
            LogUtils.e("退出失败...$e")
        }


    }

    fun sendFile(uri: Uri){
        if (connectState.value == CONNECT_SUCCESS) {
            manager?.requestConnectionInfo(mChannel) { info ->
                viewModelScope.launch(Dispatchers.IO) {
//                    while (true) {

                    val simpleDateFormat =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    val format = simpleDateFormat.format(Date(System.currentTimeMillis()))
                    LogUtils.i("发送图片开始==>${format}")

                    val inputStream = context.contentResolver.openInputStream(uri)!!

                    var startTime = System.currentTimeMillis()
                    try {
                        val address = info.groupOwnerAddress
                        if (address != null) {
                            val socket = Socket(address, port)
                            val outputStream = socket.getOutputStream()
                            val buffer = ByteArray(1024 * 10)
                            var byteRead = 0
                            var totalBytes = 0
                            var totalReadSpeed = 0

                            while (inputStream.read(buffer)
                                    .also {
                                        byteRead = it
                                    } != -1
                            ) {
                                outputStream.write(buffer, 0, byteRead)
                                totalBytes += byteRead
                                totalReadSpeed += byteRead

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - startTime >= 2000) {
                                    // 每两秒打印一次接收速度
                                    val speedMbps = totalReadSpeed * 8.0 / 2000000
                                    LogUtils.i("发送速度：${speedMbps} Mbps")
                                    totalReadSpeed = 0
                                    startTime = currentTime
                                }
                            }
                            LogUtils.i("发送完毕...大小为${totalBytes / (1024.0 * 1024.0)}")
                            socket.close()
                        } else {
                            ToastUtils.showShort("发送信息失败...")
                        }
                        inputStream.close()
                    } catch (e: Exception) {
                        LogUtils.e("意外结束..$e")
                        inputStream.close()
                    }
//                    }

                }
            }
        } else {
            ToastUtils.showShort("wifiP2p未连接...")
        }
    }
    fun sendImage(uri: Uri) {
        if (connectState.value == CONNECT_SUCCESS) {
            manager?.requestConnectionInfo(mChannel) { info ->
                viewModelScope.launch(Dispatchers.IO) {
//                    while (true) {

                        val inputStream = context.contentResolver.openInputStream(uri)!!

                        var startTime = System.currentTimeMillis()
                        try {
                            val address = info.groupOwnerAddress
                            if (address != null) {
                                val socket = Socket(address, port)
                                val outputStream = socket.getOutputStream()
                                val buffer = ByteArray(1024 * 10)
                                var byteRead = 0
                                var totalBytes = 0
                                var totalReadSpeed = 0

                                while (inputStream.read(buffer)
                                        .also {
                                            byteRead = it
                                        } != -1
                                ) {
                                    outputStream.write(buffer, 0, byteRead)
                                    totalBytes += byteRead
                                    totalReadSpeed += byteRead

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - startTime >= 2000) {
                                        // 每两秒打印一次接收速度
                                        val speedMbps = totalReadSpeed * 8.0 / 2000000
                                        LogUtils.i("发送速度：${speedMbps} Mbps")
                                        totalReadSpeed = 0
                                        startTime = currentTime
                                    }
                                }
                                LogUtils.i("发送完毕路...大小为${totalBytes / (1024.0 * 1024.0)}")
                                socket.close()
                            } else {
                                ToastUtils.showShort("发送信息失败...")
                            }
                            inputStream.close()
                        } catch (e: Exception) {
                            LogUtils.e("意外结束..$e")
                            inputStream.close()
                        }
//                    }

                }
            }
        } else {
            ToastUtils.showShort("wifiP2p未连接...")
        }


    }

//    fun setConnectState(state: Boolean) {
//        isConnected.postValue(state)
//    }
}

enum class ChooseState {
    HOME_FRAGMENT,
    SENDER_FRAGMENT,
    RECEIVER_FRAGMENT,
    MESSAGE_FRAGMENT //传输界面...
}

enum class WifiState {
    WIFI_P2P_DISCOVERY_STARTED,     //开始搜索
    WIFI_P2P_DISCOVERY_STOPPED,     //停止搜索
    WIFI_P2P_PEERS_CHANGED_ACTION,  //搜索设备更新了...
    WIFI_P2P_CONNECTION_CHANGED_ACTION //连接状态改变
}