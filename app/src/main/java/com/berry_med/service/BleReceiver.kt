package com.berry_med.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.berry_med.adapter.BleAdapter
import com.berry_med.adapter.ListViewAdapter
import com.berry_med.utils.AppHelper
import com.berry_med.utils.Const
import com.berry_med.utils.MyToast
import org.json.JSONArray
import org.json.JSONObject

/*
 * @Description: Ble Receiver
 * @Date: 2025/6/23 14:48
 * @Author: zl
 */
class BleReceiver(private val context: Context) {
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    var isConnected = false
    private val scanPeriod: Long = 3000 // 3s

    private val scannedDevices = mutableListOf<BluetoothDevice>()

    private lateinit var bleAdapter: BleAdapter
    private lateinit var listAdapter: ListViewAdapter

    private val sharedPreferences = context.getSharedPreferences("BleKt", Context.MODE_PRIVATE)

    private val toast: MyToast = MyToast()
    private var macAddress: String? = null
    private var autoConnect: Boolean = true

    var connectionListener: ConnectionListener? = null

    init {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothLeScanner = manager.adapter?.bluetoothLeScanner

        val intent = Intent(context, BleService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startForegroundService(intent)
    }

    fun setBleAdapter(bleAdapter: BleAdapter, listAdapter: ListViewAdapter) {
        this.bleAdapter = bleAdapter
        this.listAdapter = listAdapter
    }

    // Scan Callback
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val deviceName = result?.device?.name
            val deviceMac = result?.device?.address
            if (deviceName.isNullOrEmpty()) return

            if (scannedDevices.none { it.address == deviceMac }) {
                scannedDevices.add(result.device)
            }
            scannedDevices.sortByDescending { result.rssi }
            if (::bleAdapter.isInitialized) bleAdapter.updateDevices(scannedDevices)

            if (autoConnect) {
                when {
                    macAddress == deviceMac -> {
                        stopScan()
                        connect(result.device)
                        return
                    }

                    getDevices().any { it.containsKey(deviceMac) } -> {
                        stopScan()
                        connect(result.device)
                        return
                    }
                }
            }

            Log.d(
                "BleReceiver", "results = ${result.device?.name} | ${result.device?.address}"
            )

        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("BleReceiver", "onScanFailed: errorCode = $errorCode")

            toast.show("onScanFailed: errorCode = $errorCode")
        }
    }

    // GATT Callback
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    AppHelper.bluetoothGatt = gatt
                    isConnected = true
                    gatt?.discoverServices()
                    saveDevices(gatt?.device)

                    handler.postDelayed({
                        if (::bleAdapter.isInitialized) {
                            listAdapter.updateData(getDevices())
                            listAdapter.updateConnectionStatus(gatt?.device?.address)
                        }
                        connectionListener?.onConnected()
                    }, 1000)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false
                    handler.postDelayed({
                        listAdapter.updateConnectionStatus("")

                        Log.d("BleReceiver", "onDisconnected ")
                    }, 1000)

                    closeGatt()
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d("BleReceiver", "onConnecting")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(
                    "BleReceiver",
                    "onServicesDiscovered: services = ${gatt?.services?.map { it.uuid }}"
                )
                characteristicChanged(gatt) //Notification
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            var model = ""
            when (characteristic.service.uuid) {
                Const.SERVICE_UUID -> model = "BCI"
                Const.YJ_BLE_SERVICE -> model = "LD"
            }
            AppHelper.dataListener?.changed(
                value,
                gatt.device.name ?: "",
                gatt.device.address ?: "",
                model
            )
        }
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun characteristicChanged(gatt: BluetoothGatt?) {
        val service =
            gatt?.getService(Const.YJ_BLE_SERVICE) ?: gatt?.getService(Const.SERVICE_UUID)
        val characteristic =
            service?.getCharacteristic(Const.YJ_BLE_NOTIFY)
                ?: service?.getCharacteristic(Const.CHARACTERISTIC_UUID_SEND)
        gatt?.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic?.getDescriptor(Const.CLIENT_CHARACTER_CONFIG)
        descriptor?.let {
            gatt?.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        }
    }

    // Start Scan
    @SuppressLint("MissingPermission")
    fun startScan(macAddress: String? = null, autoConnect: Boolean = true) {
        this.autoConnect = autoConnect
        this.macAddress = macAddress
        if (isScanning || bluetoothLeScanner == null) return

        disconnect()
        scannedDevices.clear()
        isScanning = true

        val settings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        bluetoothLeScanner?.startScan(null, settings, scanCallback)

        // Scan Timeout
        handler.postDelayed(::stopScan, scanPeriod)
    }


    // Stop Scan
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning || bluetoothLeScanner == null) return

        bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false

        Log.d(
            "BleReceiver",
            "onScanStopped: ${scannedDevices.size} | ${scannedDevices.map { "${it.name} | ${it.address}" }}"
        )
    }

    // Connect
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (device.address.isEmpty()) return
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // Disconnect
    @SuppressLint("MissingPermission")
    fun disconnect() = bluetoothGatt?.disconnect()

    // Close GATT
    @SuppressLint("MissingPermission")
    fun closeGatt() {
        disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        isConnected = false

        toast.show("Bluetooth is disconnected")
    }

    fun clear() {
        stopScan()
        disconnect()
        closeGatt()
    }

    @SuppressLint("MissingPermission", "UseKtx")
    private fun saveDevices(device: BluetoothDevice?) {
        val mac = device?.address
        if (mac.isNullOrEmpty()) return

        var found = false
        var deviceSet = sharedPreferences.getString("deviceArray", "[]") ?: "[]"
        val deviceArray = JSONArray(deviceSet)
        for (i in 0 until deviceArray.length()) {
            val device = deviceArray.getJSONObject(i)
            if (device.has(mac)) {
                found = true
                break
            }
        }
        if (!found) {
            val editor = sharedPreferences.edit()
            deviceArray.put(JSONObject().put(mac, device.name))
            editor.putString("deviceArray", deviceArray.toString())
            editor.apply()
        }
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

    interface ConnectionListener {
        fun onConnected()
    }
}