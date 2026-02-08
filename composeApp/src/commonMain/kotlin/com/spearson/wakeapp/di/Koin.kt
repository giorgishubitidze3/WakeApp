package com.spearson.wakeapp.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

fun initKoin(
    config: (KoinApplication.() -> Unit)? = null,
) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return

    startKoin {
        config?.invoke(this)
        modules(appModule)
    }
}
