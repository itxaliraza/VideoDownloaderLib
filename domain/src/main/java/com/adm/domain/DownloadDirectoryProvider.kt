package com.adm.domain

import com.adm.framework.core.download.DownloadFolder
import com.adm.framework.core.models.MediaType
import java.io.File

interface DownloadDirectoryProvider {
    fun getAppExternalDirectory(): File
    fun getFolderInsideDownloadsDirectory(mediaType: MediaType, folderName: String): File
    fun getDownloadDirectory(downloadFolder: DownloadFolder? = null): File
}