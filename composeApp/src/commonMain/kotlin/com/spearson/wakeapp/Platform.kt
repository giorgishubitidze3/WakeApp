package com.spearson.wakeapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform