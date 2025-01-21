package com.adm.core.services.logger

import android.util.Log

class LoggerImpl : Logger {
    override fun logMessage(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun logError(tag: String, msg: String) {
        Log.e(tag, msg)
    }
}