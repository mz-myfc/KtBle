package com.berry_med.utils

import java.util.UUID

/*
 * @Description: UUID
 * @Date: 2025/6/23 14:49
 * @Author: zl
 */
object Const {
    val CLIENT_CHARACTER_CONFIG: UUID? = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val SERVICE_UUID: UUID?             = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
    val CHARACTERISTIC_UUID_SEND: UUID? = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")

    val YJ_BLE_SERVICE: UUID? = UUID.fromString("01000000-0000-0000-0000-000000000080")
    val YJ_BLE_NOTIFY: UUID?  = UUID.fromString("02000000-0000-0000-0000-000000000080")
}