package com.example.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adm.domain.managers.get_intent.GetIntentMain

class DownloadNotificationManager(private val context: Context,private val getIntentMain: GetIntentMain) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val TAG="DownloadNotificationManager"
//        const val NOTIFICATION_ID = 1
    }

    fun log(msg:String){
        Log.d(TAG,msg)
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notifications for file downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createDownloadingNotification(fileName: String): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setGroup("downloads")
            .setProgress(100, 0, false)
            .setContentIntent(pendingIntent)
    }

    private fun getPendingIntent(): PendingIntent? {
        val intent = getIntentMain.getMainIntent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0, // Request code (can be any unique integer)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Flags for PendingIntent
        )
        return pendingIntent
    }

    @SuppressLint("MissingPermission")
    fun updateProgress(notificationBuilder: NotificationCompat.Builder, id:Int, progress: Int) {
        notificationBuilder.setProgress(100, progress, false)
        if (isNotiGranted(context)) {
            notificationManager.notify(id, notificationBuilder.build())
        }
        log("updateProgress id= $id")

    }

    @SuppressLint("MissingPermission")
    fun showDownloadFailedNotification(id:Int, fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("Failed to download $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()
        if (isNotiGranted(context)) {

            notificationManager.notify(id, notification)
        }
    }
    @SuppressLint("MissingPermission")
    fun showDownloadSuccessNotification(id:Int, fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(fileName)
            .setContentIntent(getPendingIntent())
            .setSmallIcon(com.adm.framework.R.drawable.baseline_download_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()

        if (isNotiGranted(context)){
            notificationManager.notify(id, notification)
        }
    }

    fun cancelNotification(id:Int) {
        notificationManager.cancel(id)
    }
}

fun isNotiGranted(context: Context):Boolean{
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PERMISSION_GRANTED){
            return false
        }
    }
    return true
}