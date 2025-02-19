package com.adm.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.domain.repository.LocalMediaInfoRepository
import com.example.domain.repository.LocalMediaRepository
import com.example.framework.core.Commons.formatDuration
import com.example.framework.core.Commons.formatSizeToMbs
import com.example.framework.core.models.DownloadsModel
import com.example.framework.core.models.MediaType
import com.example.framework.core.models.getMimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class LocalMediaRepositoryImpl(
    private val context: Context, private val mediaInfoRepository: LocalMediaInfoRepository, private val urisHelper: UrisHelper
) : LocalMediaRepository {
    override suspend fun getAllMediaItems(file: File, mediaType: MediaType): List<DownloadsModel> {
        return withContext(Dispatchers.IO.limitedParallelism(1000)) {
            val files = file.listFiles()?.map { file ->
                async {
                    val duration = try {
                        mediaInfoRepository.getMediaDurationByFilePath(context, file.path)
                    } catch (e: Exception) {
                        0
                    }
                    var uri =urisHelper.getURi(file.path,context)
                    Log.d("cvrrr", "Get uri from simple= $uri")

                    if (uri == null) {
                        uri = getUriAfterMediaScanner(file.path, mediaType.getMimeType())
                        Log.d("cvrrr", "Get uri from mediacanneer= $uri")
                    }
                    DownloadsModel(
                        title = file.name,
                        path = file.path,
                        uri = uri ?: file.path.toUri(),
                        sizeLong = file.length(),
                        durationLong = duration,
                        dateLong = file.lastModified(),
                        formattedSize = file.length().formatSizeToMbs(context),
                        formattedDuration = duration.formatDuration(),
                        mediaType=mediaType,
                        folderName = ""
                    )
                }
            }?.awaitAll() ?: emptyList()
            files.fastFilter {
                it.durationLong > 0
            }.sortedByDescending {
                it.dateLong
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private suspend fun getUriAfterMediaScanner(filePath: String, mimeType: String) =
        suspendCancellableCoroutine<Uri?> { cont ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                arrayOf(mimeType)
            )
            { p0, p1 ->
                scope.launch {
                    val uri = urisHelper.getURi(filePath,context)
                    cont.resume(uri)
                }
            }
        }


    override fun createParentFile(path: String): Boolean {
        val file = File(path)
        with(file) {
            if (this.parentFile?.exists()?.not() == true) {
                return this.parentFile?.mkdirs() ?: false
            } else {
                return false
            }
        }
    }

    override fun createFile(path: String): Boolean {
        val file = File(path)
        return if (file.exists().not()) {
            file.createNewFile()
        } else {
            false
        }
    }

    override fun createFolder(folder: File): Boolean {
        with(folder) {
            return if (this.exists().not()) {
                this.mkdirs()
            } else {
                false
            }
        }
    }

    override fun scanFile(file: String, mediaType: MediaType?) {
        val mimeType = when (mediaType) {
            MediaType.Image -> {
                "image/png"
            }

            MediaType.Video -> {
                "video/mp4"
            }

            MediaType.Audio -> {
                "audio/*"
            }

            null -> {
                "*/*"
            }
        }
        try {
            MediaScannerConnection.scanFile(/* context = */ context, /* paths = */
                arrayOf(file), /* mimeTypes = */
                arrayOf(mimeType), /* callback = */
                null
            )
        } catch (_: Exception) {

        }
    }

    override fun deleteFile(file: File): Boolean {
        return try {
            File(file.path).delete()
        } catch (_: Exception) {
            false
        }
    }

    override fun getUriFromPath(path: String): Uri? {
        return FileProvider.getUriForFile(
            context, "${context.applicationContext.packageName}.provider", File(path)
        )
    }

    override fun shareDownloadedVideos(
        context: Context,
        paths: List<String>,
        mediaType: MediaType
    ) {
        try {
            val uris = paths.map { getUriFromPath(path = it) }
            Intent().apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                action = Intent.ACTION_SEND_MULTIPLE
                type = when (mediaType) {
                    MediaType.Image -> "image/*"
                    MediaType.Video -> "video/*"
                    MediaType.Audio -> "audio/*"
                }
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }.also {
                context.startActivity(Intent.createChooser(it, "Share via"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
