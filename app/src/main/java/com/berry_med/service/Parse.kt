package com.berry_med.service

import android.util.Log

/*
 * @Description: Parse Data
 * @Date: 2025/6/23 14:49
 * @Author: zl
 */
class Parse(private val parseListener: ParseListener) {

    private var buffArray: MutableList<Int> = mutableListOf()

    fun clear() {
        buffArray.clear()
    }

    fun readData(array: ByteArray, model: String) {
        when (model) {
            "BCI" -> readBci(array)

            "LD" -> readLd(array)
        }
    }

    fun xFF(value: Int, n: Int = 0, x: Int = 255): Int {
        var v = value
        if (v >= x) v = x
        if (v <= n) v = n
        return maxOf(v, v and 0xFF)
    }

    interface ParseListener {
        fun onBci(spo2: Int, pr: Int)
        fun onLd(sys: Int, dia: Int, pressure: Int)
    }

    fun readBci(array: ByteArray) {
        buffArray.addAll(array.map { it.toInt() and 0xFF })
        var i = 0
        var validIndex = 0
        val maxCount = buffArray.size - 5

        while (i <= maxCount) {
            if (buffArray[i] >= 128 &&
                buffArray[i + 1] < 128 &&
                buffArray[i + 2] < 128 &&
                buffArray[i + 3] < 128 &&
                buffArray[i + 4] < 128
            ) {
                val pr = if (buffArray[i + 2] >= 64) buffArray[i + 3] + 128 else buffArray[i + 3]
                val spo2 = buffArray[i + 4]

                parseListener.onBci(spo2, pr)

                i += 5
                validIndex = i
                continue
            }
            i += 1
            validIndex = i
        }

        buffArray = ArrayList(buffArray.subList(validIndex, buffArray.size))
    }

    fun readLd(array: ByteArray) {
        if (array.size > 4 && xFF(array[1].toInt() and 0xFF) == 0xFF && xFF(array[2].toInt() and 0xFF) == 0xFF) {
            when {
                array[3] == 0x0A.toByte() && array[4] == 0x02.toByte() && array.size >= 10 -> {
                    Log.d("BleReceiver", "Measuring...")

                    monitoringData(array)
                }

                array[3] == 0x49.toByte() && array[4] == 0x03.toByte() -> {
                    Log.d("BleReceiver", "Result...")
                    monitoringData(array)
                }
            }
        }
    }

    private fun monitoringData(array: ByteArray) {
        try {
            val pressure = xFF(xFF(array[6].toInt()) or (array[7].toInt() shl 8 and 0xFF00), 0, 300)
            if (array.isNotEmpty() && array.size >= 9) {
                val sys = xFF(array[6].toInt()) + 30
                val dia = xFF(array[7].toInt()) + 30
                parseListener.onLd(sys, dia, pressure)
            }
        } catch (e: Exception) {
            Log.d("BleReceiver", "err = ${e.toString()}")
        }
    }
}

