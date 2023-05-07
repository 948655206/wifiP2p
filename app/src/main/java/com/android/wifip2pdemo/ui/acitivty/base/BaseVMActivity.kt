package com.android.wifip2pdemo.ui.acitivty.base

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get

abstract class BaseVMActivity<VM : ViewModel>(private val viewModelClass: Class<VM>)
    : AppCompatActivity() {

    protected lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel= ViewModelProvider(this)[viewModelClass]

        setEvent()
        supportActionBar?.let {
            it.hide()
        }
        setContent {
            setView()
        }
    }

    open fun setEvent() {

    }

    @Composable
    abstract fun setView()


}