package com.android.wifip2pdemo.ui.compose.fragment

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.ui.compose.Screen
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.blankj.utilcode.util.LogUtils

@OptIn(ExperimentalMaterial3Api::class)
object MessageFragment {
    @Composable
    fun messageFragment(
        navController: NavHostController,
        viewModel: WifiP2pViewModel,
        send: () -> Unit,
    ) {
        val connectState by viewModel.connectState.observeAsState()

        Scaffold(
            topBar = {
                Screen.setTopBar(title = "文件传输页", back = {
                    navController.navigateUp()
                    viewModel.disconnect()
                })
            }
        ) {
            when (connectState) {
                WifiP2pViewModel.ConnectState.CONNECT_LOADING -> {
                    Box(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    val density = LocalDensity.current
                    var isVisible by remember {
                        mutableStateOf(true)
                    }
                    AnimatedVisibility(
                        visible = isVisible, enter = slideInVertically {
                            with(density) { -40.dp.roundToPx() }
                        } + expandVertically(
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            initialAlpha = 0.3f
                        ), exit = slideOutVertically() + shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = {
                                    send.invoke()
                                },
                            ) {
                                Icon(imageVector = Icons.Default.Info, null)
                            }
                            var text by remember { mutableStateOf("") }
                            TextField(value = text,
                                onValueChange = { it ->
                                    text = it
                                }, label = {
                                    "请在此输入内容"
                                }, trailingIcon = {
                                    IconButton(onClick = {
                                        LogUtils.i("发送图标...")
                                        viewModel.sendText(text)
                                        text = ""
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null
                                        )
                                    }
                                })
                        }
                    }

                }
            }


        }
    }
}