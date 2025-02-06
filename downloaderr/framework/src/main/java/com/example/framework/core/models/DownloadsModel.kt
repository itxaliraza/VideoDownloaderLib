package com.example.framework.core.models

import android.net.Uri

data class DownloadsModel(
    val title: String,
    val path: String,
    val uri: Uri,
    val sizeLong: Long=0,
    val durationLong: Long=0,
    val dateLong: Long=0,
    val formattedSize: String="",
    val formattedDuration: String="",
    val mediaType: MediaType=MediaType.Video,
    val folderName:String=""
)