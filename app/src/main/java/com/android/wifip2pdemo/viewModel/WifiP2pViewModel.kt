package com.android.wifip2pdemo.viewModel

import android.app.Application
import android.net.Uri
import android.net.wifi.p2p.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.wifip2pdemo.utils.MediaUtils
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.*
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ToastUtils
import fileConfig.FileConfig
import fileConfig.FileConfig.Config.Type.FILE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket

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
                    receiveMessage()
                    _connectState.postValue(CONNECT_CREATER)
                }

                override fun onFailure(p0: Int) {
                    LogUtils.e("创建组失败...$p0")
                }

            })
        }
    }

    fun receiveMessage() {
        try {
            server = ServerSocket(port)
            viewModelScope.launch(Dispatchers.IO) {
                while (!server.isClosed) {
                    LogUtils.i("阻塞中....")
                    try {
                        val socket = server.accept()

                        viewModelScope.launch(Dispatchers.IO) {
                            val inputStream = socket.getInputStream()
                            //通过protobuf 获取定义的文件信息
                            //原理是protobuf设置长度，所以不会和后面信息混淆
                            val fileConfig = FileConfig.Config.parseDelimitedFrom(inputStream)


                            when (fileConfig.type) {
                                FileConfig.Config.Type.FILE -> {
                                    //以后可以优化 目前是单个socket接收一段信息 没有粘包问题
                                    val fileSize = fileConfig.fileSize
                                    //类型是文件
                                    val savePath =
                                        PathUtils.getExternalDownloadsPath() + "/" + fileConfig.fileName
                                    LogUtils.i("存储路径==>$savePath")

                                    val bufferedInputStream = BufferedInputStream(inputStream)
                                    val file = File(savePath)
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                    file.createNewFile()
                                    val fileOutputStream = FileOutputStream(file)

                                    //开始接收数据的时间
                                    val startTime = System.currentTimeMillis()
                                    var lastPrintTime = startTime

                                    //每次接收的数据大小
                                    var len = 0
                                    //总接收的数据大小
                                    var totalSize = 0

                                    while (bufferedInputStream.read(buffer).also {
                                            len = it
                                            totalSize += len
                                        } != -1) {
                                        fileOutputStream.write(buffer, 0, len)
                                        val currentTime = System.currentTimeMillis()
                                        //总共耗时
                                        val totalTime = currentTime - startTime
                                        if ((currentTime - lastPrintTime) >= 2000) {
                                            //每两秒打印一次接收速率
                                            LogUtils.i("平均接收速率==>${totalSize * 8 / 1000000.0 / totalTime}Mbps")
                                            lastPrintTime = currentTime
                                        }
                                    }

                                    //关闭流...
                                    fileOutputStream.flush()
                                    fileOutputStream.close()
                                    socket.close()
                                }

                                FileConfig.Config.Type.TEXT -> {

                                }
                                FileConfig.Config.Type.UNRECOGNIZED -> {
                                    LogUtils.i("收到未知文件流...")
                                }
                            }

                        }
                    } catch (e: Exception) {
                        LogUtils.e("socket出错==>${e.toString()}")
                    }

                }

            }
        } catch (e: Exception) {
            LogUtils.e("接收出错==>${e.toString()}")
            server.close()
        }

    }
    
    //移除组
    fun removeGroup() {
        manager?.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                LogUtils.i("移除组成功...")
                connectState.postValue(CONNECT_DISCONNECT)
            }

            override fun onFailure(p0: Int) {
                //一般不会失败 如果失败 需要重启才能解决
                LogUtils.e("移除组失败...$p0")
            }
        })
    }

    //取消连接
    //改方法一般用于 正在连接中 还未连接成功的情况
    fun cancelConnect(){
        manager?.cancelConnect(mChannel,object :WifiP2pManager.ActionListener{
            override fun onSuccess() {
                LogUtils.i("取消连接成功...")
                connectState.postValue(CONNECT_DISCONNECT)
            }

            override fun onFailure(p0: Int) {
                LogUtils.e("取消连接失败...$p0")
                //如果失败代表,已经建立连接,需要移除组
                removeGroup()
            }

        })
    }

    //连接成员
    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        //是否想成为组长
        config.groupOwnerIntent = owner

        LogUtils.i("连接地址==>${config.deviceAddress}")
        manager?.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                LogUtils.i("连接中....${device.deviceName}")
                _chooseState.postValue(ChooseState.MESSAGE_FRAGMENT)
                connectState.postValue(CONNECT_LOADING)
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
    fun disconnect() {
        try {
            //服务器断开连接...

            if (::server.isInitialized) {
                server.close()
            }
            when (connectState.value) {
                CONNECT_LOADING -> {
                    cancelConnect()
                }
                CONNECT_DISCONNECT -> {
                    LogUtils.i("未连接组....")
                }
                FIRST_TIME -> {

                }
                else -> {
                    removeGroup()
                }
            }
        } catch (e: Exception) {
            LogUtils.e("退出失败...$e")
        }
    }


    private val buffer by lazy {
        ByteArray(1024 * 1024)
    }

    fun sendFileByUri(uri: Uri) {
        manager?.requestConnectionInfo(mChannel) { info ->
            info.groupOwnerAddress?.let { address ->
                viewModelScope.launch(Dispatchers.IO) {
                    LogUtils.i("阻塞中...")
                    val socket = Socket(address, port)
                    try {
                        //输出流
                        val outputStream = socket.getOutputStream()

                        val filePath = MediaUtils.getRealPathFromUri(context, uri)
                        val fileName = FileUtils.getFileName(filePath)
                        val fileType = FileUtils.getFileExtension(filePath)
                        val fileLength = FileUtils.getFileLength(filePath)

                        val openInputStream = context.contentResolver.openInputStream(uri)
                        LogUtils.i("filePath==>$filePath")
                        LogUtils.i("fileType==>$fileType")
                        LogUtils.i("fileName==>$fileName")
                        LogUtils.i("fileLength==>$fileLength")

                        //配置发送文件类型
                        val fileConfig = FileConfig.Config.newBuilder()

                        fileConfig.apply {
                            this.fileName = fileName
                            this.type = FILE
                        }.build().writeDelimitedTo(outputStream)

                        //发送文件实体
//                        val inputStream = BufferedInputStream(FileInputStream(filePath))
                        val inputStream = BufferedInputStream(openInputStream)

                        //总共发送大小
                        var totalSize = 0
                        //记录本次发送的字节大小
                        var len = 0
                        //当前时间
                        var startTime = System.currentTimeMillis()
                        //最后输出时间
                        var lastPrintTime: Long = 0

                        while (inputStream.read(buffer).also {
                                len = it
                                totalSize += len
                            } != -1) {
                            outputStream.write(buffer, 0, len)

                            //当前时间
                            val currentTime = System.currentTimeMillis()

                            //总共花费时间
                            var totalTime = currentTime - startTime
                            if ((currentTime - lastPrintTime) >= 2000) {
                                //每两秒打印一次传输速度
                                LogUtils.i("平均发送速率==>${totalSize * 8 / 1000000.0 / totalTime}Mbps")
                                lastPrintTime = currentTime
                            }
                        }

                        outputStream.flush()
                        outputStream.close()
                        socket.close()
                    } catch (e: Exception) {
                        socket.close()
                        LogUtils.e("接收端socket出错==>${e.toString()}")
                    }

                }
            }
        }
    }


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