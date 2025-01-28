package com.adm.core.services.downloader

import com.adm.core.components.DownloadingState
import kotlinx.coroutines.flow.Flow

interface MediaDownloader {
    suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Result<String>

    fun getBytesInfo(): Pair<Long, Long>
    fun getProgress(): Flow<MediaProgress>
    fun getCurrentStatus(): DownloadingState
    fun cancelDownloading()
    fun resumeDownloading()
    fun pauseDownloading()
}

data class MediaProgress(val downStatus: DownloadingState, val downSize: Long, val totalSize: Long,val exc:Exception?=null)