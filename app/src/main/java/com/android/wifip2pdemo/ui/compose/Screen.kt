package com.android.wifip2pdemo.ui.compose

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.wifip2pdemo.ui.compose.fragment.Constants.DISPLAY
import com.android.wifip2pdemo.ui.compose.fragment.Constants.INVALID
import com.android.wifip2pdemo.ui.compose.fragment.Constants.KEYPAD
import com.android.wifip2pdemo.ui.compose.fragment.Constants.LABEL
import com.android.wifip2pdemo.ui.compose.fragment.Constants.PBC
import com.android.wifip2pdemo.ui.compose.fragment.Constants.PIN
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel.ConnectState.*
import com.blankj.utilcode.util.LogUtils

@Suppress("TODO")
@OptIn(ExperimentalMaterial3Api::class)
object Screen {
    const val HOME_FRAGMENT = "HOME"
    const val SENDER_FRAGMENT = "SENDER"
    const val RECEIVER_FRAGMENT = "RECEIVER"
    const val MESSAGE_FRAGMENT = "MESSAGE"
    const val SERVER_FRAGMENT="SERVER_CREATED"
    const val REQUEST_FRAGMENT="REQUEST_CREATED"


    @Composable
    fun setTopBar(
        title: String,
//        back: NavHostController?=null,
        back: (() -> Any?)? = null,
    ) {
        TopAppBar(
            title = { Text(text = title) },
            navigationIcon = {
                back?.let {
                    IconButton(onClick = {
                        back.invoke()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Blue)
        )
    }

    @Composable
    fun lazyItem(
        it: PaddingValues,
        viewModel: WifiP2pViewModel,
    ) {

        val listState by viewModel.peerList.observeAsState(emptyList())
        //是否显示Dialog
        var showDialog by remember {
            mutableStateOf(false)
        }
        //选中的设备
        var chooseDevice by remember {
            mutableStateOf<WifiP2pDevice?>(null)
        }
        //选择创建连接方式...
        var connectState by remember {
            mutableStateOf(PBC)
        }
        var password by remember {
            mutableStateOf("")
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(text = "请选择你需要的连接方式")
                },
                text = {
                    val connectList = listOf(
                        PBC,
                        DISPLAY,
                        KEYPAD,
                        PIN,
                        INVALID,
                        LABEL,
                    )

                    var expand by remember {
                        mutableStateOf(false)
                    }
                    Column(modifier = Modifier.height(100.dp)) {
                        Box {
                            TextButton(onClick = { expand = !expand }) {
                                Text(text = connectState)
                            }
                            DropdownMenu(expanded = expand, onDismissRequest = {
                                expand = false
                            }) {
                                connectList.forEach { item ->
                                    DropdownMenuItem(text = {
                                        Text(text = item)
                                    }, onClick = {
                                        connectState = item
                                        expand = false
                                    })
                                }
                            }
                        }
                        if (connectState== PIN) {
                            TextField(value = password, onValueChange = {
                                password=it
                            }, label = {
                                Text(text = "请输入组长密码")
                            })
                        }

                    }

                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.connect(chooseDevice!!,connectState,password)
                        viewModel.chooseState.postValue(ChooseState.MESSAGE_FRAGMENT)
                        showDialog=false
                    }) {
                        Text(text = "连接")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                    }) {
                        Text(text = "取消")
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = {
                item {
                    if (listState.isEmpty()) {
                        CircularProgressIndicator()
                    }
                }
                items(listState) { device ->
                    TextButton(
                        onClick = {
                            if (viewModel.connectState.value == CONNECT_PREPARE ||viewModel.connectState.value==CONNECT_DISCONNECT) {
                                chooseDevice = device
                                showDialog = true
                            } else if (viewModel.connectState.value==CONNECT_CREATER) {
                                viewModel.removeClient(device)
                            }else{
                                LogUtils.i("该条目被点击了...")

                            }

                        }, modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth()
                    ) {
                        Text(text = device.deviceName)
                    }
                }
            }
        )
    }


}