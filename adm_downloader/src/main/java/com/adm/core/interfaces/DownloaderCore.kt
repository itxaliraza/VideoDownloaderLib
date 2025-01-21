package com.adm.core.interfaces

import android.content.Context
import com.adm.core.components.DownloadingState
import com.adm.core.services.downloader.MediaProgress
import kotlinx.coroutines.flow.Flow

interface DownloaderCore {
    fun getDownloadingState(): DownloadingState
    fun getTotalSize(): Long
    fun getDownloadedSize(): Long
    fun getDestinationDirectory(): String
    fun getFileName(): String
    suspend fun startDownloading(context: Context)
    fun resumeDownloading(context: Context)
    fun pauseDownloading(context: Context)
    fun getProgress(): Flow<MediaProgress>

}