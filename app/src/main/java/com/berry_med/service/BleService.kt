package com.berry_med.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.berry_med.ktble.MainActivity
import com.berry_med.ktble.R
import com.berry_med.utils.AppHelper
import org.json.JSONArray

/*
 * @Description: Ble Service
 * @Date: 2025/6/23 14:48
 * @Author: zl
 */
class BleService : Service() {
    private lateinit var context: Context
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var settings: ScanSettings? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var scanning: Boolean = false

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        context = this
        scanning = false
        sharedPreferences = context.getSharedPreferences("BleKt", MODE_PRIVATE)

        settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val manager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothLeScanner = manager.adapter?.bluetoothLeScanner
        stopScan()

        handler.postDelayed(runnable, 3000)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            startId,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "ke_ble"
        val channelName = "KtBle"

        val serviceChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

        var contentIntent = Intent()

        if (!AppHelper.isAppRunning(this)) {
            contentIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, channelId)
            .setContentTitle("KtBle")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val deviceMac = result?.device?.address
            if (getDevices().any { it.containsKey(deviceMac) }) {
                scanning = true
                AppHelper.wakeUpAppAndConnect(context, deviceMac)
            }
        }
    }

    private val runnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            Log.d("BleReceiver", "Service is running in the background")

            if (!scanning) {
                bluetoothLeScanner?.startScan(null, settings, scanCallback)
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        handler.removeCallbacks(runnable)
        AppHelper.clear()
        scanning = false
    }

    fun getDevices(): List<Map<String, String>> {
        val dataSet = sharedPreferences.getString("deviceArray", "[]") ?: "[]"
        val jsonArray = JSONArray(dataSet)
        var list = mutableListOf<Map<String, String>>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val map = mutableMapOf<String, String>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getString(key)
            }
            list.add(map)
        }
        return list
    }

    @SuppressLint("MissingPermission")
    fun stopScan() = bluetoothLeScanner?.stopScan(scanCallback)
}