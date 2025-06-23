package com.berry_med.utils

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.berry_med.ktble.MyApplication

/*
 * @Description: Toast
 * @Date: 2025/6/23 14:50
 * @Author: zl
 */
class MyToast {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toast: Toast? = null

    fun show(content: String) {
        mainHandler.post {
            if (toast == null) {
                toast = Toast.makeText(MyApplication.getContext(), content, Toast.LENGTH_LONG)
            } else {
                toast?.setText(content)
            }
            toast?.show()
        }
    }
}