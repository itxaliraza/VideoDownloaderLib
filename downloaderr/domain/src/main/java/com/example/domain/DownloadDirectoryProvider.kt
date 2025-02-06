package com.example.domain

import com.example.framework.core.download.DownloadFolder
import com.example.framework.core.models.MediaType
import java.io.File

interface DownloadDirectoryProvider {
    fun getAppExternalDirectory(): File
    fun getFolderInsideDownloadsDirectory(mediaType: MediaType, folderName: String): File
    fun getDownloadDirectory(downloadFolder: DownloadFolder? = null): File
}