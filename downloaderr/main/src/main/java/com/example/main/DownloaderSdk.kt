package com.example.main

interface DownloaderSdk {
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
     fun setDownloadingListener(listener: DownloadingListener?)

    val MAX_RETRIES: Int

}

interface DownloadingListener {
    fun onDownloadingFailed(id: String)
    fun onDownloadingCompleted(id: String)
    fun onDownloadingPaused(id: String, isFromUser: Boolean)
}