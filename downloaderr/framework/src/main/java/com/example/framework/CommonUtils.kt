package com.example.framework

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

fun Context.showToast(msg:String){
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

fun Context.debugToast(msg:String){
    if (BuildConfig.DEBUG){
        showToast(msg)
    }
}