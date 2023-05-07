package com.android.wifip2pdemo.ui.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.blankj.utilcode.util.LogUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Suppress("TODO")
@OptIn(ExperimentalMaterial3Api::class)
object Screen {
    const val HOME_FRAGMENT = "HOME"
    const val P2P_FRAGMENT = "p2p"

    const val SENDER_FRAGMENT = "SENDER"
    const val RECEIVER_FRAGMENT = "RECEIVER"

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun homeFragment(
        navController: NavHostController,
    ) {
        Scaffold(
            topBar = {
                setTopBar(title = "主页")
            },
            content = {
                Column(
                    modifier = Modifier.padding(it),
                ) {
                    TextButton(onClick = {
                        navController.navigate(RECEIVER_FRAGMENT)
                    }) {
                        Text(text = "接收端")
                    }
                    TextButton(onClick = {
                        navController.navigate(SENDER_FRAGMENT)
                    }) {
                        Text(text = "发送端")
                    }
                }
            },
        )
    }

    @Composable
    fun senderFragment(
        navController: NavHostController,
        viewModel: WifiP2pViewModel,
    ) {
        //表示不想成为组长
        viewModel.owner == 0
        Scaffold(
            topBar = {
                setTopBar(
                    title = "发送端",
                    back = { navController.navigateUp() }
                )
            }) {
            lazyItem(it, viewModel)
        }
    }


    @Composable
    fun receiverFragment(
        navController: NavHostController,
        viewModel: WifiP2pViewModel,
    ) {
        //表示想成为组长
        viewModel.owner = 15
        Scaffold(topBar = {
            setTopBar(
                title = "接收端",
                back = { navController.navigateUp() }
            )
        }) {
            lazyItem(it, viewModel)
        }
    }

    @Composable
    private fun setTopBar(
        title: String,
//        back: NavHostController?=null,
        back: (() -> Boolean?)? = null,
    ) {
        TopAppBar(
            title = { Text(text = title) },
            navigationIcon = {
                back?.let {
                    IconButton(onClick = {
                        back.invoke()
                        println("点击了返回按钮...")
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
    private fun lazyItem(
        it: PaddingValues,
        viewModel: WifiP2pViewModel,
    ) {
        val listState by viewModel.peerList.observeAsState(emptyList())
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                listState?.let {
                    items(listState) { device ->
                        TextButton(
                            onClick = {
                                viewModel.connectPeers(device)
                            }, modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth()
                        ) {
                            Text(text = device.deviceName)
                        }

                    }
                }

            }
        )
    }

    private fun getDeviceIpAddress(deviceAddress: String): String {
        // 通过设备的MAC地址获取IP地址
        val cmd = "ping -c 1 -w 1 $deviceAddress"
        val runtime = Runtime.getRuntime()
        try {
            val process = runtime.exec(cmd)
            val input = BufferedReader(InputStreamReader(process.inputStream))
            var line = input.readLine()
            while (line != null) {
                if (line.contains("PING")) {
                    val start = line.indexOf('(')
                    val end = line.indexOf(')')
                    return line.substring(start + 1, end)
                }
                line = input.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
}