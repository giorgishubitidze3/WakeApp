package com.spearson.wakeapp.di

import android.content.Context
import org.koin.android.ext.koin.androidContext

fun initKoinAndroid(context: Context) {
    initKoin {
        androidContext(context.applicationContext)
    }
}
