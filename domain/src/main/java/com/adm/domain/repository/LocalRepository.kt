package com.adm.domain.repository

import android.net.Uri
import com.adm.framework.core.models.DownloadsFolderModel
import com.adm.framework.core.models.DownloadsModel

interface LocalRepository {
    suspend fun getDownloadedVideoFolders(): List<DownloadsFolderModel>
    suspend fun getDownloadedVideosInFolder(folderName:String): List<DownloadsModel>
    fun searchVideos(query: String): List<DownloadsModel>
  suspend  fun deleteVideo(uris: Uri)

}