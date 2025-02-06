package com.example.domain.repository

import android.net.Uri
import com.example.framework.core.models.DownloadsFolderModel
import com.example.framework.core.models.DownloadsModel

interface LocalRepository {
    suspend fun getDownloadedVideoFolders(): List<DownloadsFolderModel>
    suspend fun getDownloadedVideosInFolder(folderName:String): List<DownloadsModel>
    fun searchVideos(query: String): List<DownloadsModel>
  suspend  fun deleteVideo(uris: Uri)

}