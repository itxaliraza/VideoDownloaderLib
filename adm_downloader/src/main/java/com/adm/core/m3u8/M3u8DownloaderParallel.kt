package com.adm.core.m3u8
import android.content.Context
import com.adm.core.components.DownloadingState
import com.adm.core.m3u8_parser.api.ApiHitter
import com.adm.core.m3u8_parser.api.ApiHitterImpl
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
    private val linkMaker: LinkMaker,
    private val tempDirProvider: TempDirProvider = TempDirProviderImpl(context = context),
    private val m3U8PlaylistParser: M3u8ChunksPicker = M3u8ChunksPickerImpl(linkMaker = linkMaker),
    private val videosMerger: VideosMerger,
    private val logger: Logger,
    private val maxParallelDownloads: MaxParallelDownloads,
) : MediaDownloader {

    companion object {
        private const val TAG = "M3u8DownloaderParallel"
    }

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
        supportChunks: Boolean,
    ): Result<String> = withContext(Dispatchers.IO) {
        tempDirPath = tempDirProvider.provideTempDir(
            "m3u8/${url.createUniqueFolderName()}/${fileName.substringBeforeLast(".")}"
        )
        tempDirPath?.let { tempDirPath ->
            try {
                isFailed = false
                isPaused = false
                isCompleted = false

                val streams: List<SingleStream> =
                    m3U8PlaylistParser.getChunks(m3u8Link = url, headers = headers)
                if (streams.isEmpty()) throw Exception("Invalid Url")
                totalChunks = streams.size.toLong()
                logger.logMessage(TAG, "New Save Directory = $tempDirPath")
                tempDirPath.createThisFolderIfNotExists()

                // Detect segment type
                val isFmp4Segments = streams.firstOrNull()?.link
                    ?.lowercase()
                    ?.contains(".m4s", true) == true

                logger.logMessage(
                    TAG,
                    "Detected segment type: ${if (isFmp4Segments) "fMP4 (.m4s)" else "TS (.ts)"}"
                )

                // For fMP4, download the SINGLE shared init segment once
                var sharedInitFile: File? = null
                if (isFmp4Segments) {
                    val initUrl = extractInitSegmentUrl(url, headers)
                    if (initUrl != null) {
                        logger.logMessage(TAG, "Downloading shared init segment from: $initUrl")
                        sharedInitFile = downloadSharedInitSegment(
                            initUrl = initUrl,
                            tempDir = tempDirPath,
                            headers = headers
                        )
                        if (sharedInitFile != null) {
                            logger.logMessage(
                                TAG,
                                "Shared init segment downloaded: ${sharedInitFile.length()} bytes"
                            )
                        }
                    } else {
                        logger.logMessage(TAG, "No init segment found in m3u8 playlist")
                    }
                }

                val channel = Channel<Unit>(maxParallelDownloads.getMaxParallelDownloadsCount())
                val downloadJobs = mutableListOf<Deferred<Long>>()

                logger.logMessage("M3U8Links", "MainLink: $url")
                streams.forEachIndexed { index, stream ->
                    logger.logMessage("M3U8Links", "Index: $index, Stream: ${stream.link}")
                }

                // Download all media segments in parallel
                streams.forEachIndexed { index, stream ->
                    val job = scope.async {
                        channel.send(Unit)

                        val segmentExtension =
                            if (isFmp4Segments) "m4s" else fileName.substringAfterLast(".")

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

                        val result = mediaDownloader.downloadMedia(
                            url = urlToDownload,
                            fileName = "${index}.$segmentExtension",
                            directoryPath = tempDirPath.absolutePath,
                            mimeType = mimeType,
                            headers = headers,
                            showNotification = showNotification,
                            supportChunks = false
                        )
                        result.getOrThrow()

                        channel.receive()
                        download += 1
                        emitProgress()
                        download
                    }
                    downloadJobs.add(job)
                }

                downloadJobs.awaitAll()

                val destFile = File(directoryPath, fileName).path
                destFile.createParentFileIfNotExists()

                val mergeResult = if (isFmp4Segments) {
                    // Use MediaMuxer-based remuxing with the shared init segment
                    videosMerger.mergeFragmentedMp4Segments(
                        tempDirPath.absolutePath,
                        destFile,
                        sharedInitFile
                    )
                } else {
                    // Legacy TS flow – simple binary concat
                    videosMerger.mergeVideos(
                        tempDirPath.absolutePath,
                        destFile
                    )
                }

                if (mergeResult.isSuccess) {
                    logger.logMessage(TAG, "Streams(${streams.size}) merged successfully")
                    isCompleted = true
                    emitProgress()
                    logger.logMessage(TAG, "✅ Download + Merge completed at $destFile")
                    return@withContext Result.success(destFile)
                } else {
                    val error: Exception = (mergeResult.exceptionOrNull() as? Exception)
                        ?: Exception("Unknown merge error")
                    isFailed = true
                    emitProgress(error)
                    return@withContext Result.failure(error)
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    isFailed = true
                    emitProgress(e)
                    throw e
                }
                isFailed = true
                emitProgress(e)
                scope.cancel()
                return@withContext Result.failure(e)
            }
        }
        return@withContext Result.failure(Exception("Directory Not Found"))
    }

    override fun getBytesInfo(): Pair<Long, Long> = Pair(download * 1024, totalChunks * 1024)

    override fun getCurrentStatus(): DownloadingState {
        return when {
            isPaused -> DownloadingState.Paused
            isFailed -> DownloadingState.Failed
            isCompleted -> DownloadingState.Success
            else -> DownloadingState.Progress
        }
    }

    override fun cancelDownloading() {
        isFailed = true
        emitProgress()
        scope.cancel()
    }

    override fun resumeDownloading() {
        if (isPaused) isPaused = false
        emitProgress()
    }

    private fun emitProgress(exception: Exception? = null) {
        _progressFlow.update {
            MediaProgress(getCurrentStatus(), download, totalChunks, exception)
        }
    }

    override fun pauseDownloading() {
        isPaused = true
        emitProgress()
        scope.cancel()
    }

    /**
     * Extracts the SINGLE shared init segment URL from the m3u8 playlist.
     * This init segment is used by ALL media segments.
     */
    private suspend fun extractInitSegmentUrl(
        m3u8Url: String,
        headers: Map<String, String>,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val apiHitter: ApiHitter = ApiHitterImpl()
            val m3u8Content = apiHitter.get(m3u8Url, headers)

            if (m3u8Content == null) {
                logger.logMessage(TAG, "Failed to fetch m3u8 content")
                return@withContext null
            }

            // Parse #EXT-X-MAP tag which specifies the shared init segment
            val lines = m3u8Content.lines()
            for (line in lines) {
                if (line.startsWith("#EXT-X-MAP")) {
                    val uriMatch = Regex("URI=\"([^\"]+)\"").find(line)
                    if (uriMatch != null) {
                        val uri = uriMatch.groupValues[1]
                        logger.logMessage(TAG, "Found EXT-X-MAP with URI: $uri")

                        return@withContext if (uri.startsWith("http")) {
                            uri
                        } else {
                            m3u8Url.substringBeforeLast("/") + "/$uri"
                        }
                    }
                }
            }

            logger.logMessage(TAG, "No #EXT-X-MAP tag found in m3u8 playlist")
            null
        } catch (e: Exception) {
            logger.logMessage(TAG, "Error extracting init segment URL: ${e.message}")
            null
        }
    }

    /**
     * Downloads the SINGLE shared init segment file that will be used for all media segments.
     */
    private suspend fun downloadSharedInitSegment(
        initUrl: String,
        tempDir: File,
        headers: Map<String, String>,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val initFile = File(tempDir, "init.mp4")
            if (initFile.exists()) {
                initFile.delete()
            }

            val mediaDownloader = CustomDownloaderImpl(
                context = context,
                tempDirProvider = tempDirProvider,
                videosMerger = videosMerger,
                maxParallelDownloads = maxParallelDownloads,
                logger = logger,
            )

            val result = mediaDownloader.downloadMedia(
                url = initUrl,
                fileName = "init.mp4",
                directoryPath = tempDir.absolutePath,
                mimeType = "video/mp4",
                headers = headers,
                showNotification = false,
                supportChunks = false
            )

            result.getOrNull()?.let {
                val downloadedFile = File(it)
                if (downloadedFile.exists() && downloadedFile.length() > 0) {
                    return@withContext downloadedFile
                }
            }

            logger.logMessage(TAG, "Failed to download init segment")
            null
        } catch (e: Exception) {
            logger.logMessage(TAG, "Error downloading init segment: ${e.message}")
            null
        }
    }
}



