package com.berry_med.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application.ACTIVITY_SERVICE
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import com.berry_med.ktble.MainActivity

/*
 * @Description: App Helper
 * @Date: 2025/6/23 14:49
 * @Author: zl
 */
object AppHelper {
    var bluetoothGatt: BluetoothGatt? = null
    var dataListener: DataListener? = null
    var launch: Boolean = false

    // Wake Up
    fun wakeUpAppAndConnect(context: Context, mac: String?) {
        if (!isAppRunning(context) && !launch) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("launchMac", mac)
            }
            context.startActivity(launchIntent)
        }
    }

    //APP Running
    fun isAppRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return activityManager.runningAppProcesses.any {
            it.processName == context.packageName && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    @SuppressLint("MissingPermission")
    fun clear() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        launch = false
    }

    interface DataListener {
        fun changed(value: ByteArray, name: String, mac: String, model: String)
    }
}