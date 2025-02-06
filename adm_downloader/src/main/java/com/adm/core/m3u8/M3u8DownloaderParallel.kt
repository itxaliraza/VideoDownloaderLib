package com.adm.core.m3u8

import android.content.Context
import android.util.Log
import com.adm.core.components.DownloadingState
import com.adm.core.m3u8_parser.listeners.M3u8ChunksPicker
import com.adm.core.m3u8_parser.model.SingleStream
import com.adm.core.m3u8_parser.parsers.M3u8ChunksPickerImpl
import com.adm.core.services.downloader.CustomDownloaderImpl
import com.adm.core.services.downloader.MediaDownloader
import com.adm.core.services.downloader.MediaProgress
import com.adm.core.services.logger.Logger
import com.adm.core.utils.createUniqueFolderName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

class M3u8DownloaderParallel(
    private val context: Context,
    private val tempDirProvider: TempDirProvider = TempDirProviderImpl(context = context),
    private val m3U8PlaylistParser: M3u8ChunksPicker = M3u8ChunksPickerImpl(),
    private val videosMerger: VideosMerger,
    private val logger: Logger,
    private val maxParallelDownloads: MaxParallelDownloads

) : MediaDownloader {

    private val TAG = "M3u8Downloader"
    private var downloadingId = ""
    private var download = 0L
    private var totalChunks = 0L
    val scope = CoroutineScope(Dispatchers.IO)
    var isFailed = false
    var isCompleted = false
    var isPaused = false
    private var tempDirPath: File? = null
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
    ): Result<String> = withContext(Dispatchers.IO) {
        tempDirPath =
            tempDirProvider.provideTempDir(
                "m3u8/${url.createUniqueFolderName()}/${
                    fileName.substringBeforeLast(
                        "."
                    )
                }"
            )
        tempDirPath?.let { tempDirPath ->
            try {
                isFailed = false
                isPaused = false
                isCompleted = false
                val streams: List<SingleStream> =
                    m3U8PlaylistParser.getChunks(m3u8Link = url, headers = headers)
                if (streams.isEmpty())
                    throw Exception("Invalid Url")
                totalChunks = streams.size.toLong()
                logger.logMessage(TAG, "New Save Directory = $tempDirPath")
                tempDirPath.createThisFolderIfNotExists()
                val channel = Channel<Unit>(16)
                val downloadJobs = mutableListOf<Deferred<Long>>()
                streams.forEachIndexed { index, stream ->
                    val job = scope.async {
                        channel.send(Unit)  // Wait for a slot in the channel
                        Log.d("Cvrrr", "Base Url = sending $index")
                        val mediaDownloader = CustomDownloaderImpl(
                            context = context,
                            tempDirProvider = tempDirProvider,
                            videosMerger = videosMerger,
                            maxParallelDownloads = maxParallelDownloads,
                            logger = logger,
                        )
                        val baseUrl = url.substringBeforeLast("/")
                        val urlToDownload = if (stream.link.startsWith("http")) {
                            stream.link
                        } else {
                            baseUrl + "/${stream.link}"
                        }
                        logger.logMessage(
                            "TAG",
                            "Base Url = ${baseUrl}\nstreamLink=${stream.link}\nUrlToDownload = ${urlToDownload}"
                        )

                        val result = mediaDownloader.downloadMedia(
                            url = urlToDownload,
                            fileName = "${index}.${fileName.substringAfterLast(".")}",
                            directoryPath = tempDirPath.absolutePath,
                            mimeType = mimeType,
                            headers = headers,
                            showNotification = showNotification,
                            supportChunks = false
                        )
                        result.getOrThrow()
                        Log.d("Cvrrr", "Base Url = receiving $index")
                        channel.receive()
                        download += 1
                        emitProgress()
                        download
                    }
                    downloadJobs.add(job)
                }

                downloadJobs.awaitAll()  // Wait for all downloads to finish
                Log.d(
                    "Cvrrr", "Base Url ${tempDirPath.path} ${
                        File(directoryPath, fileName).path
                    }"
                )


                val destFile = File(directoryPath, fileName).path

                destFile.createParentFileIfNotExists()

                val result = videosMerger.mergeVideos(
                    tempDirPath.absolutePath,
                    destFile
                )
                result.getOrThrow()
                logger.logMessage("TAG", "Streams(${streams.size}) = " + streams.toString())
                isCompleted = true
                emitProgress()
                return@withContext Result.success(File(directoryPath, fileName).path)
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                isFailed = true
                emitProgress(e)
                Log.d("Cvrrr", "Base Url  Exception $e")
                scope.cancel()
                return@withContext Result.failure(e)
            }
        }
        return@withContext Result.failure(Exception("Directory Not Found"))
    }

    override fun getBytesInfo(): Pair<Long, Long> {
        return Pair(download * 1024, totalChunks * 1024)
    }

    override fun getCurrentStatus(): DownloadingState {
        Log.d(TAG, "getCurrentStatus: download=${download},totalChunks=${totalChunks}")
        return if (isPaused)
            DownloadingState.Paused
        else if (isFailed)
            DownloadingState.Failed
        else if (isCompleted) {
            DownloadingState.Success
        } else {
            DownloadingState.Progress
        }
    }

    override fun cancelDownloading() {
        isFailed = true
        emitProgress()
        scope.cancel()
    }

    override fun resumeDownloading() {
        if (isPaused) {
            isPaused = false

        }
        emitProgress()

    }


    private fun emitProgress(exception: java.lang.Exception? = null) {
        _progressFlow.update {
            MediaProgress(getCurrentStatus(), download, totalChunks, exception)
        }
    }

    override fun pauseDownloading() {
        isPaused = true
        emitProgress()
        scope.cancel()
    }

}



