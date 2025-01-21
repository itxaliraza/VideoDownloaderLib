package com.adm.core

import android.content.Context
import android.util.Log
import com.adm.core.base.DownloaderBase
import com.adm.core.components.DownloadingState
import com.adm.core.components.SupportedMimeTypes
import com.adm.core.services.downloader.MediaDownloader
import com.adm.core.services.downloader.MediaProgress
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloaderCoreImpl(
    private val url: String,
    private val fileName: String,
    private val destinationDirectory: String,
    private val headers: Map<String, String> = hashMapOf(),
    private val mimeType: String = SupportedMimeTypes.Video.mimeTye,
    private val showNotification: Boolean = true,
    private val supportChunks: Boolean = true,
    private val mediaDownloader: MediaDownloader
) : DownloaderBase(
    url = url,
    fileName = fileName,
    destinationDirectory = destinationDirectory,
    headers = headers,
    mimeType = mimeType,
    showNotification = showNotification
) {

    private var downloadingState: DownloadingState = DownloadingState.Idle
    private var mMediaDownloader: MediaDownloader = mediaDownloader


    override suspend fun startDownloading(context: Context) {
         Log.d("cvv", "startDownloading init url=$url")
        mediaDownloader.downloadMedia(
            url = url,
            fileName = fileName,
            directoryPath = destinationDirectory,
            mimeType = mimeType,
            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks
        )
//        Log.d("cvv", "startDownloading: $downloadingId")
    }

    override fun resumeDownloading(context: Context) {
        mMediaDownloader.resumeDownloading()
    }

    override fun pauseDownloading(context: Context) {
        mMediaDownloader.pauseDownloading()
    }

    override fun getProgress(): Flow<MediaProgress> {
         return mMediaDownloader.getProgress()
    }

    override fun getDownloadingState(): DownloadingState {
        downloadingState = mMediaDownloader.getCurrentStatus()
        return downloadingState
    }

    override fun getTotalSize(): Long {
        return mediaDownloader.getBytesInfo().second
    }

    override fun getDownloadedSize(): Long {
        return mediaDownloader.getBytesInfo().first
    }

    override fun getDestinationDirectory(): String {
        return destinationDirectory
    }

    override fun getFileName(): String {
        return fileName
    }


}

