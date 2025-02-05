package com.adm.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InProgressVideoDB(
    @PrimaryKey
    val downloadId: Long,
    val url: String = "",
    val thumb: String = "",
    var fileName: String = "",
    val destinationDirectory: String = "",
    val mimeType: String = "",
    val headers: Map<String,String> = emptyMap(),
    val showNotification: Boolean = false,
    val supportChunks: Boolean = false,
    val status: String = "",
    val downloadedSize: Long = 0,
    val totalSize: Long = 0,
)