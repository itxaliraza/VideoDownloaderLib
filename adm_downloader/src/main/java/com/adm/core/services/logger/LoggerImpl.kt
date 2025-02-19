package com.adm.core.services.logger

import android.util.Log
var logsss=""

class LoggerImpl : Logger {
    override fun logMessage(tag: String, msg: String) {
        Log.d(tag, msg)
        logsss+="\n\n$tag: $msg\n\n"
    }

    override fun logError(tag: String, msg: String) {
        Log.e(tag, msg)
    }
}