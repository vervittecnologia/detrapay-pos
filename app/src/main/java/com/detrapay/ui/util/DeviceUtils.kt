package com.detrapay.ui.util

import android.annotation.SuppressLint
import android.os.Build

object DeviceUtils {
    @SuppressLint("HardwareIds")
    fun getSerialNumber(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                Build.SERIAL
            }
        } catch (e: SecurityException) {
            ""
        }
    }
}