package com.berry_med.ktble

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class MyApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null
        fun getContext(): Context? {
            return context
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}