package com.berry_med.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/*
 * @Description: Broadcast Receiver
 * @Date: 2025/6/23 14:48
 * @Author: zl
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, BleService::class.java)
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startForegroundService(serviceIntent)

            Log.d("BleReceiver", "onReceive")
        }
    }
}