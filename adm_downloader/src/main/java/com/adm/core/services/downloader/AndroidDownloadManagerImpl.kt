package com.adm.core.services.downloader

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import com.adm.core.components.DownloadingState
import kotlinx.coroutines.flow.Flow

class AndroidDownloadManagerImpl(
    context: Context
) : MediaDownloader {

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private var downloadingId = -1L
    override suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Result<String> {
        val path = "$directoryPath/${fileName}"
//        AllVideoDownloader/.progress/Discover Popular Videos  Faceb/.1.mp4
//        Ishfaq/1736542208701.mp4
        val uri = url.toUri()
        Log.d("cvv", "downloadMedia: $path\nUri=${uri}")
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setMimeType(mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, path
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        headers.forEach { (key, value) ->
            request.addRequestHeader(key, value)
        }
        downloadingId = downloadManager.enqueue(request)
        return  Result.success("")
    }

    override fun getBytesInfo(): Pair<Long, Long> {
        val query = DownloadManager.Query()
            .setFilterById(downloadingId)
        val cursor = downloadManager.query(query)
        var bytesDownloaded = 0L
        var totalSize = 0L
        if (cursor.moveToFirst()) {
            bytesDownloaded =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            totalSize =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            Log.d("cvv", "getBytesInfo($downloadingId): ${bytesDownloaded}/${totalSize}")
        }
        return Pair(bytesDownloaded, totalSize)
    }

    override fun getProgress(): Flow<MediaProgress> {
        TODO("Not yet implemented")
    }
    override fun getCurrentStatus(): DownloadingState {
        var downloadStatus: DownloadingState = DownloadingState.Idle
        val query = DownloadManager.Query()
            .setFilterById(downloadingId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val status =
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    downloadStatus = DownloadingState.Success
                }

                DownloadManager.STATUS_FAILED -> {
                    downloadStatus = DownloadingState.Failed
                    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                    Log.d("cvv", "getCurrentStatus: reason $reason")
                }

                DownloadManager.STATUS_RUNNING -> {
                    downloadStatus = DownloadingState.Progress
                }

                DownloadManager.STATUS_PAUSED -> {
                    downloadStatus = DownloadingState.Paused
                }

                DownloadManager.STATUS_PENDING -> {
                    downloadStatus = DownloadingState.Paused
                }
            }
        }
        return downloadStatus
    }

    override fun cancelDownloading() {
        try {
            downloadManager.remove(downloadingId)
        } catch (e: Exception) {
        }
    }

    override fun resumeDownloading() {

    }

    override fun pauseDownloading() {

    }

}



