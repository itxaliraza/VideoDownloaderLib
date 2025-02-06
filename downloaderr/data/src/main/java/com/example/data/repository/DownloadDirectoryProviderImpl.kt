package com.adm.data.repository

import android.os.Environment
import android.util.Log
import com.example.domain.DownloadDirectoryProvider
import com.example.domain.repository.LocalMediaRepository
import com.example.framework.core.download.DownloadFolder
import com.example.framework.core.models.MediaType
import com.example.framework.core.models.getFolderName
import java.io.File

class DownloadDirectoryProviderImpl(
    private val localMediaRepository: LocalMediaRepository
) : DownloadDirectoryProvider {
    private val TAG = "DownloadDirectoryProviderImpl"
    override fun getAppExternalDirectory(): File {
        val rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(
            rootDir, "All Video Downloader - X"
        )
        localMediaRepository.createFolder(folder)
        return folder
    }

    override fun getFolderInsideDownloadsDirectory(mediaType: MediaType, folderName: String): File {
        val rootDir =
            File(getAppExternalDirectory().path + "/" + mediaType.getFolderName() + "/" + folderName)
        localMediaRepository.createFolder(rootDir)
        Log.d(TAG, "getFolderInsideDownloadsDirectory: ${rootDir.path}")
        return rootDir
    }

    override fun getDownloadDirectory(downloadFolder: DownloadFolder?): File {
        val rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder: File = if (downloadFolder != null) {
            File(
                rootDir, downloadFolder.mName
            )
        } else {
            rootDir
        }
        Log.d(TAG, "getDownloadDirectory:${folder.path} ")
        localMediaRepository.createFolder(folder)
        return folder
    }
}