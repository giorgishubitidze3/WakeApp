package com.spearson.wakeapp

import androidx.compose.ui.window.ComposeUIViewController
import com.spearson.wakeapp.di.initKoin

fun MainViewController() = run {
    initKoin()
    ComposeUIViewController {
        App()
    }
}
