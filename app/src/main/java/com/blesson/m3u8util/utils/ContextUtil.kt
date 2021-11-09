package com.blesson.m3u8util.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class ContextUtil : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

}