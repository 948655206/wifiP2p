package com.android.wifip2pdemo.ui.compose

import android.annotation.SuppressLint
import android.net.MacAddress
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.viewModel.ChooseState
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel
import com.blankj.utilcode.util.LogUtils

@Suppress("TODO")
@OptIn(ExperimentalMaterial3Api::class)
object Screen {
    const val HOME_FRAGMENT = "HOME"
    const val SENDER_FRAGMENT = "SENDER"
    const val RECEIVER_FRAGMENT = "RECEIVER"
    const val MESSAGE_FRAGMENT = "MESSAGE"


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
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = {
                item{
                    if (listState.isEmpty()) {
                        CircularProgressIndicator()
                    }
                }
                items(listState) { device ->

                    TextButton(
                        onClick = {
                            if (viewModel.chooseState.value == ChooseState.SENDER_FRAGMENT) {
                                viewModel.connect(device)
                                viewModel.chooseState.postValue(ChooseState.MESSAGE_FRAGMENT)
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