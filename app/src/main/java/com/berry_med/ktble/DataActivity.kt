package com.berry_med.ktble

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.berry_med.service.Parse
import com.berry_med.utils.AppHelper


/*
 * @Description: Data
 * @Date: 2025/6/23 14:48
 * @Author: zl
 */
class DataActivity : AppCompatActivity(), AppHelper.DataListener, Parse.ParseListener {
    private lateinit var nameTv: TextView
    private lateinit var macTv: TextView
    private lateinit var spo2Value: TextView
    private lateinit var prValue: TextView
    private lateinit var sysValue: TextView
    private lateinit var diaValue: TextView
    private lateinit var pressureValue: TextView
    private lateinit var parse: Parse


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_data)
        initView()
        back()
    }

    private fun initView() {
        nameTv = findViewById(R.id.name)
        macTv = findViewById(R.id.mac)
        spo2Value = findViewById(R.id.spo2_value)
        prValue = findViewById(R.id.pr_value)
        sysValue = findViewById(R.id.sys_value)
        diaValue = findViewById(R.id.dia_value)
        pressureValue = findViewById(R.id.pressure_value)
        AppHelper.dataListener = this

        parse = Parse(this)
        parse.clear()
    }

    private fun back() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent()
                intent.putExtra("back", 200)
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    override fun changed(value: ByteArray, name: String, mac: String, model: String) {
        runOnUiThread {
            nameTv.text = name
            macTv.text = mac

            parse.readData(value, model)
        }
    }

    override fun onBci(spo2: Int, pr: Int) {
        runOnUiThread {
            spo2Value.text = "$spo2"
            prValue.text = "$pr"
        }
    }

    override fun onLd(sys: Int, dia: Int, pressure: Int) {
        runOnUiThread {
            sysValue.text = "$sys"
            diaValue.text = "$dia"
            pressureValue.text = "$pressure"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppHelper.clear()
    }
}