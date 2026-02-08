package com.spearson.wakeapp

import androidx.compose.runtime.Composable
import com.spearson.wakeapp.core.presentation.NavigationRoot
import com.spearson.wakeapp.core.theme.WakeAppTheme
import com.spearson.wakeapp.di.appModule
import org.koin.compose.KoinApplication
import org.koin.dsl.koinApplication

@Composable
fun App() {
    KoinApplication(
        application = {
            koinApplication {
                modules(appModule)
            }
        }
    ) {
        WakeAppTheme {
            NavigationRoot()
        }
    }
}
