package com.berry_med.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berry_med.ktble.R

/*
 * @Description: Ble Adapter
 * @Date: 2025/6/23 14:47
 * @Author: zl
 */
class BleAdapter(
    private var devices: List<BluetoothDevice>,
    private val onConnectClicked: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BleAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceAddress: TextView = itemView.findViewById(R.id.deviceAddress)
        val connectButton: Button = itemView.findViewById(R.id.connectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name ?: "Unknown"
        holder.deviceAddress.text = device.address

        holder.connectButton.setOnClickListener {
            onConnectClicked(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateDevices(newDevices: List<BluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}
