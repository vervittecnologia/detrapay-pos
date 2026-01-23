package com.detrapay.ui.util

import android.util.Log
import com.detrapay.BuildConfig

class Logger {
    companion object {
        private const val IDENTIFIER: String = "DETRAPAY"

        fun d(message: String ){
            if (BuildConfig.DEBUG) {
                Log.d(IDENTIFIER, message)
            }
        }
    }
}