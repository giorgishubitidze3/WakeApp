package com.spearson.wakeapp

import androidx.compose.runtime.Composable
import com.spearson.wakeapp.core.presentation.NavigationRoot
import com.spearson.wakeapp.core.theme.WakeAppTheme

@Composable
fun App() {
    WakeAppTheme {
        NavigationRoot()
    }
}
