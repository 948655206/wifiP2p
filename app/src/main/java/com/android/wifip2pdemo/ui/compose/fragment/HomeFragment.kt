package com.android.wifip2pdemo.ui.compose.fragment

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.ui.compose.Screen

@OptIn(ExperimentalMaterial3Api::class)
object HomeFragment {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun homeFragment(
        navController: NavHostController,
    ) {
        Scaffold(
            topBar = {
                Screen.setTopBar(title = "主页")
            },
            content = {
                Column(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = {
                        navController.navigate(Screen.RECEIVER_FRAGMENT)
                    }) {
                        Text(text = "接收端")
                    }

                    TextButton(onClick = {
                        navController.navigate(Screen.SENDER_FRAGMENT)
                    }) {
                        Text(text = "发送端")
                    }

                    TextButton(onClick = {
                        navController.navigate(Screen.SERVER_FRAGMENT)
                    }) {
                        Text(text = "创建服务")
                    }

                    TextButton(onClick = {
                        navController.navigate(Screen.FIND_MIRACAST)
                    }) {
                        Text(text = "在Miracast中发现")
                    }
                }
            },
        )
    }
}