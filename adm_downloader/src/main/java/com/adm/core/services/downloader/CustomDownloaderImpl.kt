package com.adm.core.services.downloader

import android.content.Context
import com.adm.core.components.DownloadingState
import com.adm.core.m3u8.MaxParallelDownloads
import com.adm.core.m3u8.MaxParallelDownloadsImpl
import com.adm.core.m3u8.SimpleVideosMergerImpl
import com.adm.core.m3u8.TempDirProvider
import com.adm.core.m3u8.TempDirProviderImpl
import com.adm.core.m3u8.VideosMerger
import com.adm.core.m3u8.createNewFileIfNotExists
import com.adm.core.m3u8.createParentFileIfNotExists
import com.adm.core.model.CustomDownloaderModel
import com.adm.core.services.logger.Logger
import com.adm.core.services.logger.LoggerImpl
import com.adm.core.utils.DownloaderPathsHelper
import com.adm.core.utils.createUniqueFolderName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


class CustomDownloaderImpl(
    private val context: Context,
    private val tempDirProvider: TempDirProvider = TempDirProviderImpl(context = context),
    private val videosMerger: VideosMerger = SimpleVideosMergerImpl(LoggerImpl()),
    private val maxParallelDownloads: MaxParallelDownloads = MaxParallelDownloadsImpl(),
    private val logger: Logger = LoggerImpl()
) : MediaDownloader {
    private val TAG = "CustomDownloaderImpl"

    private var supportChunking = false
    private var isPaused = false

    private var model = CustomDownloaderModel()
    private var downloadedId: String = UUID.randomUUID().toString()
    private var totalBytesSize = 0L
    private var downloadStatus: DownloadingState = DownloadingState.Idle

    private var scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1000))

    val hashMap: HashMap<String, Long> = hashMapOf()
    private var filesToDownload = 1
    private var filesDownloaded = 0
    private var isDownloadingCompleted = false
    var mDestFile: File? = null
    private var tempDirFile: File? = null
    private val maxDownloadsCount: Int by lazy {
        maxParallelDownloads.getMaxParallelDownloadsCount()
    }
    private val _progressFlow = MutableStateFlow(MediaProgress(DownloadingState.Progress, 0L, 0L))
    override fun getProgress() = _progressFlow.asStateFlow()

    override suspend fun downloadMedia(
        url: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Result<String> {
        tempDirFile =
            tempDirProvider.provideTempDir(
                "mp4Videos/${url.createUniqueFolderName()}/${
                    fileName.substringBeforeLast(
                        "."
                    )
                }"
            )


        try {
            scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1000))
            isPaused = false
            supportChunking = supportChunks
            logger.logMessage(
                TAG,
                "downloadMedia(supportChunks=${supportChunks}) filename= $fileName:\nUrl=${url}\nPath=$directoryPath"
            )
            model =
                CustomDownloaderModel(
                    url = url,
                    fileName = fileName,
                    directoryPath = directoryPath,
                    mimeType = mimeType,
                    headers = headers,
                    showNotification = showNotification
                )
//        CoroutineScope(Dispatchers.IO.limitedParallelism(1000)).launch {
            updateStatus(DownloadingState.Progress)
            val result = downloadFile()
            result.getOrThrow()
//        }
            return Result.success(File(directoryPath, fileName).path)
        } catch (e: Exception) {
            logger.logMessage(TAG, "Error during download: Exception ${mDestFile?.name} $e")

            if (e is CancellationException) {

                updateStatus(DownloadingState.Failed)
                _progressFlow.update {
                    MediaProgress(getCurrentStatus(), getBytesInfo().first, totalBytesSize, e)
                }
                throw e
            }
            updateStatus(DownloadingState.Failed)

            _progressFlow.update {
                MediaProgress(getCurrentStatus(), getBytesInfo().first, totalBytesSize, e)
            }
            logger.logMessage(TAG, "Error during download: Exception final ${mDestFile?.name} $e")

            scope.cancel()
            return Result.failure(e)
        }
    }

    override fun resumeDownloading() {
        if (isPaused) {
            isPaused = false
            logger.logMessage(TAG, "Resuming download...")
            scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1000))

            scope.launch {
                updateStatus(DownloadingState.Progress)

                downloadFile()
            }
        }
    }

    private suspend fun downloadFile(): Result<Unit> {
        return withContext(Dispatchers.IO.limitedParallelism(1000)) {
            try {
                val destFile = File(
                    model.directoryPath,
                    model.fileName
                )
                destFile.createParentFileIfNotExists()
                logger.logMessage(TAG, "Start Downloading destFile path=${destFile.name}")
                mDestFile = destFile
                val jobs: ArrayList<Deferred<Result<Unit>>> = arrayListOf()

                val chunkSupportAndLengthResult = getVideoChunksSupportAndLength(url = model.url)
                chunkSupportAndLengthResult.onFailure {
                    throw Exception("Error Getting Length ${it.message}")
                }
                chunkSupportAndLengthResult.onSuccess { chunkSupportAndLength ->
//                    if (chunkSupportAndLength.second == -1L) {
//                        throw Exception("Error Getting Length Invalid")
//                    }
                    totalBytesSize = chunkSupportAndLength.second
                    logger.logMessage(
                        TAG,
                        "destFil ${destFile.name} totalBytesSize= $totalBytesSize"
                    )
                    if (supportChunking && chunkSupportAndLength.first && chunkSupportAndLength.second > 0) {
                        val chunks = getChunks(chunkSupportAndLength.second)
                        filesToDownload = chunks.size
                        logger.logMessage(TAG, "Chunks to download ${destFile.name}= $chunks")

                        chunks.forEachIndexed { index, chunk ->
                            jobs.add(
                                scope.async {
                                    val tempDestFile = File(
                                        tempDirFile,
                                        "${index}.${model.fileName.substringAfterLast(".")}"
                                    )
                                    tempDestFile.createParentFileIfNotExists()
                                    tempDestFile.createNewFileIfNotExists()
                                    logger.logMessage(
                                        TAG,
                                        "Chunk destFile orig= ${destFile.name} path=${tempDestFile.name}"
                                    )
                                    val result =
                                        downloadInternal(chunk.start, chunk.end, tempDestFile)

                                    logger.logMessage(
                                        TAG,
                                        "${destFile.name}Chunk Downloaded"
                                    )
                                    result

                                }
                            )
                        }
                        jobs.awaitAll()
                        scope.ensureActive()
                        logger.logMessage(TAG, "All chunks downloaded ${destFile.name}")

                        if (!isPaused) {
                            val result = videosMerger.mergeVideos(tempDirFile!!.path, destFile.path)
                            result.getOrThrow()
                            isDownloadingCompleted = true
                        }
                    } else {
                        val result = downloadInternal(0L, chunkSupportAndLength.second, destFile)
                        result.getOrThrow()
                        isDownloadingCompleted = true
                    }
                }
                emitProgress()


                return@withContext Result.success(Unit)

            } catch (e: CancellationException) {
                logger.logMessage(TAG, "Error during download: paused ${mDestFile?.name}")
                throw e
            } catch (e: Exception) {
                logger.logMessage(TAG, "Error during download: ${e.message} ${mDestFile?.name}")
                throw e
            }
        }

    }

    private suspend fun downloadInternal(
        startSize: Long,
        endSize: Long,
        destFile: File,
    ): Result<Unit> {
        var inputStream: InputStream? = null
        var connection: HttpURLConnection? = null
        var outputStream: RandomAccessFile? = null
        var downloadedBytesSize = 0L

        try {
            connection = URL(model.url).openConnection() as HttpURLConnection
            connection.readTimeout = 5000
            connection.connectTimeout = 5000
            if (destFile.length() >= (endSize - startSize) && destFile.length() > 0 && endSize > 0) {
                filesDownloaded += 1
                logger.logMessage(
                    TAG,
                    "Download already completed: ${destFile.name} ${destFile.length()}  ${endSize - startSize}"
                )
//                hashMap[destFile.path] = destFile.length()
                updateProgress(destFile, destFile.length())
                return Result.success(Unit)
            }
            var existingFileSize = if (destFile.exists()) destFile.length() else 0
            if (existingFileSize > 0) {
                existingFileSize += startSize
            } else
                existingFileSize = startSize


            if (supportChunking) {
                logger.logMessage(
                    TAG,
                    "Starting download. ${destFile.name} Total size: startbytes= $existingFileSize endbytes=$endSize $destFile"
                )
                connection.setRequestProperty("Range", "bytes=$existingFileSize-${endSize - 1}")
            }
            model.headers.forEach { t, u ->
                connection.setRequestProperty(t, u)
            }
            if (model.headers.containsKey("Accept-Encoding").not()) {
                connection.setRequestProperty("Accept-Encoding", "gzip")
            }
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_PARTIAL || connection.responseCode == HttpURLConnection.HTTP_OK) {
                val totalSize = connection.contentLength + existingFileSize
                logger.logMessage(
                    TAG,
                    "Starting download.  ${destFile.name}Total size: $totalSize bytes."
                )

                inputStream = connection.inputStream

                outputStream = RandomAccessFile(destFile, "rw")
                outputStream.seek(destFile.length())
                logger.logMessage(
                    TAG,
                    "Starting download.${destFile.name} destlength = ${destFile.length()} "
                )

                val buffer = ByteArray(1024 * 16) // 16 KB buffer
                var bytesRead: Int = 0


                while (true) {
                    withTimeout(5000) { // 5 seconds timeout
                        bytesRead =
                            inputStream.read(buffer).also { if (it == -1) return@withTimeout }
                    }
                    scope.ensureActive()
                    if (isPaused) {
                        logger.logMessage(TAG, "Download paused buffer. ${destFile.name}")
                        break
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    val downloadedSize = outputStream.length()
                    downloadedBytesSize += bytesRead
//                    hashMap[destFile.path] = downloadedSize
                    updateProgress(destFile, downloadedSize)
                    logger.logMessage(
                        "ProgressTracker",
                        "Downloaded  ${destFile.name} , $downloadedSize / $totalSize bytes.\nUrl=${model.url}"
                    )
                }
                logger.logMessage(TAG, "Download after ${destFile.name}")

                scope.ensureActive()
                updateProgress(destFile, destFile.length())
                filesDownloaded += 1

                logger.logMessage(
                    TAG,
                    "Download completed: ${destFile.name} ${destFile.length()} ${connection.contentLength}"
                )
                logger.logMessage(
                    TAG,
                    "cvrrrrr Server responded with code: ${destFile.name} $endSize ength = ${connection.contentLength} end-start= ${endSize - startSize} name= ${destFile.nameWithoutExtension}"
                )

                return Result.success(Unit)
            } else {
                logger.logMessage(
                    TAG,
                    "Server responded with code: ${destFile.name} ${connection.responseCode}"
                )
                throw Exception("Server Responded with not ok len=${destFile.length()} existingFileSize=$existingFileSize startSize=$startSize endSize=$endSize diff=${endSize - startSize}, exisdif=${endSize - existingFileSize} ${connection.responseCode} ${connection.responseMessage}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        } finally {
            inputStream?.close()
            connection?.disconnect()
            outputStream?.close()
        }
    }

    override fun pauseDownloading() {
        scope.cancel()
        isPaused = true
        updateStatus(DownloadingState.Paused)
    }

    override fun getBytesInfo(): Pair<Long, Long> {
        val sum = hashMap.values.sum()
        /* val value = if (supportChunking) {
             var result = tempDirFile?.listFiles()?.sumOf { it.length() } ?: 0
             if (result < 1) {
                 result = mDestFile?.length() ?: 0
             }
             result
         } else {
             mDestFile?.length() ?: 0L
         }*/
        return Pair(sum, totalBytesSize.coerceAtLeast(sum))
    }


    override fun getCurrentStatus(): DownloadingState {
        val downloadedBytesSize = hashMap.values.sum()

        if (downloadedBytesSize >= totalBytesSize && downloadedBytesSize > 0 && totalBytesSize > 0) {
            downloadStatus = DownloadingState.Success
        }
        val destFile = File(
            DownloaderPathsHelper.getDirInsideDownloads(model.directoryPath),
            model.fileName
        )
        if (destFile.exists() && destFile.length() >= totalBytesSize && totalBytesSize > 0)
            downloadStatus = DownloadingState.Success
        if (isDownloadingCompleted) {
            downloadStatus = DownloadingState.Success

        }
        return downloadStatus
    }

    override fun cancelDownloading() {

    }


    fun getChunks(totalSize: Long): List<Chunk> {
        var start = 0L
        val chunks: MutableList<Chunk> = mutableListOf()
        val singleChunk = totalSize / maxDownloadsCount
        while (start < totalSize) {
            val end =
                (start + singleChunk).coerceAtMost(totalSize) // Ensure end does not exceed totalSize

            chunks.add(Chunk(start, end))

            start = end
        }
        return chunks
    }


    private suspend fun getVideoChunksSupportAndLength(url: String): Result<Pair<Boolean, Long>> {
        return withContext(Dispatchers.IO.limitedParallelism(1000)) {
            val (supportChunks, length) = performHeadRequest(url).getOrElse {
                false to -1L
            }
            val (supportChunks2, length2) = if (supportChunks.not() || length == -1L) {
                performGetRequestWithRange(url).getOrElse { false to -1L }
            } else {
                supportChunks to length
            }
            val finalLength: Long = maxOf(length, length2)

            return@withContext Result.success(
                Pair(
                    supportChunks || supportChunks2,
                    finalLength.toLong()
                )
            )
        }
    }


    private suspend fun performHeadRequest(url: String): Result<Pair<Boolean, Long>> {
        return withContext(Dispatchers.IO.limitedParallelism(1000)) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.readTimeout = 5000
                connection.connectTimeout = 5000
                connection.requestMethod = "HEAD"
                model.headers.forEach { t, u ->
                    connection.setRequestProperty(t, u)
                }
                if (model.headers.containsKey("Accept-Encoding").not()) {
                    connection.setRequestProperty("Accept-Encoding", "gzip")
                }
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val acceptRanges = connection.getHeaderField("Accept-Ranges")
                    val supportsRange = acceptRanges?.equals("bytes", ignoreCase = true) ?: false

                    val contentLength =
                        connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L

                    return@withContext Result.success(Pair(supportsRange, contentLength))
                }

                Result.failure(Exception("HEAD request failed"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Performs a GET request with the Range header to check for range support and fetch Content-Length.
     */
    private suspend fun performGetRequestWithRange(url: String): Result<Pair<Boolean, Long>> {
        return withContext(Dispatchers.IO.limitedParallelism(1000)) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.readTimeout = 5000
                connection.connectTimeout = 5000
                connection.setRequestProperty(
                    "Range",
                    "bytes=0-1"
                ) // Request only the first 2 bytes
                model.headers.forEach { t, u ->
                    connection.setRequestProperty(t, u)
                }
                if (model.headers.containsKey("Accept-Encoding").not()) {
                    connection.setRequestProperty("Accept-Encoding", "gzip")
                }

                connection.connect()

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_PARTIAL -> {
                        // Server supports Range requests
                        val acceptRanges = connection.getHeaderField("Accept-Ranges")
                        val supportsRange =
                            acceptRanges?.equals("bytes", ignoreCase = true) ?: false

                        // Extract Content-Length from Content-Range header
                        val contentRange = connection.getHeaderField("Content-Range")
                        val contentLength =
                            contentRange?.substringAfterLast("/")?.toLongOrNull() ?: -1L

                        if (contentLength != -1L) {
                            return@withContext Result.success(Pair(supportsRange, contentLength))
                        }
                    }

                    HttpURLConnection.HTTP_OK -> {
                        // Server does not support Range requests but returns the full content
                        val contentLength =
                            connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
                        return@withContext Result.success(Pair(false, contentLength))
                    }
                }

                Result.failure(Exception("GET request with Range header failed"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun updateProgress(destFile: File, progress: Long) {
        hashMap[destFile.path] = progress
        logger.logMessage(
            "EmitProgress",
            "Downloaded ${mDestFile?.name} ${destFile.name} $progress / $totalBytesSize bytes.\nUrl=${model.url}"
        )
        emitProgress()
    }

    fun updateStatus(progress: DownloadingState) {
        downloadStatus = progress
        emitProgress()
    }

    fun emitProgress() {
        _progressFlow.update {
            MediaProgress(getCurrentStatus(), getBytesInfo().first, totalBytesSize)
        }
    }


}


data class Chunk(val start: Long, val end: Long)
