package com.example.domain.repository

import android.content.Context
import android.net.Uri
import com.example.framework.core.models.DownloadsModel
import com.example.framework.core.models.MediaType
import java.io.File

interface LocalMediaRepository {
    suspend fun getAllMediaItems(file: File,mediaType: MediaType): List<DownloadsModel>
    fun deleteFile(file: File): Boolean
    fun scanFile(file: String, mediaType: MediaType?)
    fun createFile(path: String): Boolean
    fun createFolder(folder: File): Boolean
    fun createParentFile(path: String): Boolean
    fun getUriFromPath(path: String): Uri?
    fun shareDownloadedVideos(context: Context,paths: List<String>, mediaType: MediaType)
}