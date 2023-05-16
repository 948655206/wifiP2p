package com.android.wifip2pdemo.ui.compose.fragment

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.ui.compose.Screen
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils

@OptIn(ExperimentalMaterial3Api::class)
object ReceiveFragment {

    private const val pin = "12345678"
    private val netWorkName = "DIRECT-xy-zxy"

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
        }) {
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

                    //Android10
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                        item {
                            TextButton(onClick = {
                                if (pin.value.isEmpty()) {
                                    pin.value = "12345678"
                                }
                                val p2pInfo = P2pInfo(
                                    netWorkName.value, pin.value
                                )
                                viewModel.createNewGroup(p2pInfo)
                                isCreate.value=false

                                netWorkName.value = ""
                                pin.value = ""

                            }) {
                                Text(text = "创建组")
                            }
                        }
                    }
                }else{
                    Screen.lazyItem(it = it, viewModel = viewModel)
                }
            } else {
                ToastUtils.showShort("Android10以下默认创建组")
                LogUtils.i("Android10以下默认创建组")
                Screen.lazyItem(it, viewModel)
                viewModel.createNewGroup()
            }

        }
    }


    data class P2pInfo(
        val netWorkName: String,
        val pin: String,
    ) {

    }
}