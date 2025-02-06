package com.example.domain.managers.progress_manager

interface MyDownloaderManager {
    companion object {
        const val TAG = "MyDownloadManager"
    }

    suspend fun startDownloading(
        url: String,
        thumb: String = "",
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Long

    suspend fun pauseDownloading(id: Long)
    suspend fun resumeDownloading(id: Long)
    suspend fun deleteDownloading(id: Long)
    suspend fun cancelDownloading(id: Long)

    val MAX_RETRIES:Int

}
