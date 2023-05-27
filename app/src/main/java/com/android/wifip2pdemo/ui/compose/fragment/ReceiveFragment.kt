package com.android.wifip2pdemo.ui.compose.fragment

import android.annotation.SuppressLint
import android.net.wifi.WifiInfo
import android.net.wifi.WpsInfo
import android.net.wifi.WpsInfo.DISPLAY
import android.net.wifi.WpsInfo.PBC
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pWfdInfo
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.ui.compose.Screen
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils

@OptIn(ExperimentalMaterial3Api::class)
object ReceiveFragment {

    private const val pin = "12345678"
    private val netWorkName = "DIRECT-xy-zxy"

//    @Composable
//    fun receiverFragment(
//        navController: NavHostController,
//        viewModel: WifiP2pViewModel,
//    ) {
//        //表示不想成为组长
//        Scaffold(
//            topBar = {
//                Screen.setTopBar(
//                    title = "接收端",
//                    back = { navController.navigateUp() }
//                )
//            }) {
//
//            Screen.lazyItem(it, viewModel)
//        }
//    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun receiverFragment(
        navController: NavHostController,
        viewModel: WifiP2pViewModel,
    ) {
        //表示想成为组长
        viewModel.owner = 15
        Scaffold(topBar = {
            Screen.setTopBar(
                title = "接收端",
                back = { navController.navigateUp() }
            )
        }) { it ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //显示内容页面
                val isCreate = remember {
                    mutableStateOf(true)
                }
                if (isCreate.value) {
                    //创建选项
                    val netWorkName = remember {
                        mutableStateOf(netWorkName)
                    }
                    val pin = remember {
                        mutableStateOf(pin)
                    }
                    var switchState by remember { mutableStateOf(false) }
                    //Android10
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "是否自定义PIN")
                                Spacer(modifier = Modifier.padding(10.dp))
                                Switch(
                                    checked = switchState,
                                    onCheckedChange = { isChecked ->
                                        switchState = isChecked
                                    })
                            }
                        }
                        if (switchState) {
                            item {
                                TextField(
                                    value = netWorkName.value,
                                    label = {
                                        Text(text = "请输入NetWork")
                                    },
                                    onValueChange = { text ->
                                        netWorkName.value = text
                                    })
                            }
                            item {
                                TextField(
                                    value = pin.value,
                                    label = {
                                        Text(text = "请输入NetWork")
                                    },
                                    onValueChange = { text ->
                                        pin.value = text
                                    })
                            }
                        }
                        item {
                            TextButton(onClick = {
                                if (switchState) {
                                    if (pin.value.isEmpty()) {
                                        pin.value = "12345678"
                                    }
                                    val p2pInfo = P2pInfo(
                                        netWorkName.value, pin.value
                                    )
                                    viewModel.createNewGroup(p2pInfo)
                                    isCreate.value = false

                                    netWorkName.value = ""
                                    pin.value = ""
                                } else {
                                    viewModel.createNewGroup()
                                    isCreate.value = false

                                }


                            }) {
                                Text(text = "创建组")
                            }
                        }
                    }
                } else {
                    Screen.lazyItem(it = it, viewModel = viewModel)
                }
            } else {
                LaunchedEffect(Unit) {
                    ToastUtils.showShort("Android10以下默认创建组")
                    LogUtils.i("Android10以下默认创建组")
                    viewModel.createNewGroup()
                }

                Screen.lazyItem(it, viewModel)
            }

        }
    }


    data class P2pInfo(
        val netWorkName: String,
        val pin: String,
    ) {

    }

}