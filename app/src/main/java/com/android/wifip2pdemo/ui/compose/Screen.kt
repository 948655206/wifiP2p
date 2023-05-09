package com.android.wifip2pdemo.ui.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel

@Suppress("TODO")
@OptIn(ExperimentalMaterial3Api::class)
object Screen {
    const val HOME_FRAGMENT = "HOME"
    const val SENDER_FRAGMENT = "SENDER"
    const val RECEIVER_FRAGMENT = "RECEIVER"
    const val MESSAGE_FRAGMENT = "MESSAGE"

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
                                viewModel.chooseState.postValue(ChooseState.MESSAGE_FRAGMENT)
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

    @Composable
    fun messageFragment(
        navController: NavHostController,
        viewModel: WifiP2pViewModel,
        send: () -> Unit
    ) {
        val connectState by viewModel.connectState.observeAsState()

        Scaffold(
            topBar = {
                setTopBar(title = "文件传输页", back = {
                    navController.navigateUp()
                    viewModel.disconnect()
                })
            }
        ) {
            when (connectState) {
                WifiP2pViewModel.ConnectState.CONNECT_LOADING -> {
                    Box(
                        modifier = Modifier.padding(it).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.padding(it),
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
                    }
                }

            }


        }
    }

}