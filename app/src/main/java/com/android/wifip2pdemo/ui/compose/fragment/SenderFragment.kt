package com.android.wifip2pdemo.ui.compose.fragment

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.android.wifip2pdemo.ui.compose.Screen
import com.android.wifip2pdemo.viewModel.WifiP2pViewModel

@OptIn(ExperimentalMaterial3Api::class)
object SenderFragment {

    @Composable
    fun senderFragment(
        navController: NavHostController,
        viewModel: WifiP2pViewModel,
    ) {
        //表示不想成为组长
        Scaffold(
            topBar = {
                Screen.setTopBar(
                    title = "发送端",
                    back = { navController.navigateUp() }
                )
            }) {
            Screen.lazyItem(it, viewModel)
        }
    }

}