package com.berry_med.ktble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berry_med.adapter.BleAdapter
import com.berry_med.adapter.ListViewAdapter
import com.berry_med.service.BleReceiver
import com.berry_med.service.BleService
import com.berry_med.utils.AppHelper
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity(), View.OnClickListener, BleReceiver.ConnectionListener {
    private lateinit var startScan: Button

    private lateinit var bleReceiver: BleReceiver
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var listAdapter: ListViewAdapter
    private var mac: String? = null
    private lateinit var context: Activity
    private var onTap: Boolean = false
    private var skip: Boolean = false

    val handler = Handler(Looper.getMainLooper())

    private val activityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data?.getIntExtra("back", -1)
            if (data == 200) {
                skip = false
                bleReceiver.disconnect()
                bleReceiver.stopScan()
            }
        }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        context = this
        initView()
        deviceListView()
        bottomSheetView()

        isPermission()
    }

    @SuppressLint("InflateParams")
    private fun initView() {
        AppHelper.launch = true
        mac = intent.getStringExtra("launchMac")
        startScan = findViewById(R.id.start_scan)
        startScan.setOnClickListener(this)
        bleReceiver = BleReceiver(this)
        bleReceiver.connectionListener = this

        val intent = Intent(this, BleService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startForegroundService(intent)

        if (mac != null) bleReceiver.startScan(macAddress = mac)
    }

    private fun deviceListView() {
        val listView: ListView = findViewById(R.id.listview)
        listAdapter = ListViewAdapter(bleReceiver.getDevices())
        listView.adapter = listAdapter

        listAdapter.onItemClickListener = { mac ->
            onTap = true
            bleReceiver.startScan(macAddress = mac)
        }
    }

    @SuppressLint("InflateParams")
    private fun bottomSheetView() {
        bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.devicesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = BleAdapter(emptyList(), onConnectClicked = { device ->
            bleReceiver.connect(device)
            bottomSheetDialog.dismiss()
        })
        bleReceiver.setBleAdapter(adapter, listAdapter)
        recyclerView.adapter = adapter
        bottomSheetDialog.setContentView(view)

        handler.postDelayed(runnable, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        bleReceiver.clear()
        activityLauncher.unregister()
        handler.removeCallbacks(runnable)
        AppHelper.clear()
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.start_scan -> {
                onTap = true
                if (!isPermission()) return
                bottomSheetDialog.show()
                bleReceiver.startScan(autoConnect = false)
            }
        }
    }

    override fun onConnected() {
        if (!skip) {
            skip = true
            val intent = Intent(this, DataActivity::class.java)
            activityLauncher.launch(intent)
        }
    }

    val runnable = object : Runnable {
        override fun run() {
            if (!bleReceiver.isConnected && !bottomSheetDialog.isShowing && !bleReceiver.getDevices()
                    .isEmpty() && !onTap && isPermission()
            ) {
                bleReceiver.startScan()
            }
            onTap = false
            handler.postDelayed(this, 5000)
        }
    }

    @SuppressLint("NewApi")
    fun isPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ), 200
            )
            return false
        }
        return true
    }
}