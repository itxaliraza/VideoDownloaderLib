package com.example.main

import android.util.Log

object DownloadingListenerManager {
    private var listener: DownloadingListener? = null
    fun setDownloadListener(listener: DownloadingListener?) {
        Log.d("pause sdk","setDownloadListener ${this.listener} $listener")

        this.listener = listener
    }

    fun getDownloadListener() = listener

    fun onDownloadingPaused(id: String, isFromUser: Boolean) {
Log.d("pause sdk","onDownloadingPaused $listener")
        listener?.onDownloadingPaused(id,isFromUser)
    }
}