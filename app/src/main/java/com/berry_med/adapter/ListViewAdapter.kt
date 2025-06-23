package com.berry_med.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.berry_med.ktble.R


/*
 * @Description: ListView Adapter
 * @Date: 2025/6/23 14:47
 * @Author: zl
 */
class ListViewAdapter(private var items: List<Map<String, String>>) : BaseAdapter() {
    private var macAddress: String? = null
    var onItemClickListener: ((macAddress: String) -> Unit)? = null

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(parent?.context)
                .inflate(R.layout.list_view_adapter, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position) as Map<*, *>
        holder.name.text = "${item.values.first()}"
        holder.mac.text = "${item.keys.first()}"

        if (macAddress == holder.mac.text.toString()) {
            holder.status.text = "✓"
            holder.status.setTextColor(Color.GREEN)
        } else {
            holder.status.text = "x"
            holder.status.setTextColor(Color.RED)
        }

        view.setOnClickListener {
            onItemClickListener?.invoke(holder.mac.text.toString())
        }

        return view
    }

    private class ViewHolder(view: View) {
        val name: TextView = view.findViewById(R.id.device_name)
        val mac: TextView = view.findViewById(R.id.device_mac)
        val status: TextView = view.findViewById(R.id.device_status)
    }

    // update
    fun updateData(newItems: List<Map<String, String>>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    fun updateConnectionStatus(macAddress: String?) {
        this.macAddress = macAddress
        notifyDataSetChanged()
    }
}