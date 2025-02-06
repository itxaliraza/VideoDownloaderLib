package com.adm.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.domain.repository.LocalMediaInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalMediaInfoRepositoryImpl : LocalMediaInfoRepository {
    override suspend fun getMediaDurationByFilePath(context: Context, path: String): Long {
        return withContext(Dispatchers.IO) {
            val file = File(path)
            try {
                if (!file.exists()) return@withContext 0
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(file.absolutePath))
                val duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                duration?.toLongOrNull() ?: 0
            } catch (e: Exception) {
                0
            }
        }
    }
}