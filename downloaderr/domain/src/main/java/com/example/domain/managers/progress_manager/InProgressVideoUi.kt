package com.example.domain.managers.progress_manager

data class InProgressVideoUi(
    val id: String,
    val url: String = "",
    val thumb: String = "",
    var fileName: String = "",
    val destinationDirectory: String = "",
    val mimeType: String = "",
    val status: DownloadingState = DownloadingState.Idle,
    val downloadedSize: Long = 0,
    val totalSize: Long = 0,
    val progress: Float = 0f
)