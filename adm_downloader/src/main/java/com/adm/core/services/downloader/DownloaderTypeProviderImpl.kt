package com.adm.core.services.downloader

import android.content.Context
import com.adm.core.m3u8.AnalyticHelper
import com.adm.core.m3u8.M3u8DownloaderParallel
import com.adm.core.m3u8.MaxParallelDownloads
import com.adm.core.m3u8.TempDirProvider
import com.adm.core.m3u8.TempDirProviderImpl
import com.adm.core.m3u8.VideosMerger
import com.adm.core.services.logger.Logger
import com.adm.core.m3u8_parser.listeners.M3u8ChunksPicker
import com.adm.core.m3u8_parser.parsers.M3u8ChunksPickerImpl

class DownloaderTypeProviderImpl(
    private val context: Context,
    private val tempDirProvider: TempDirProvider = TempDirProviderImpl(context = context),
    private val m3U8PlaylistParser: M3u8ChunksPicker = M3u8ChunksPickerImpl(),
    private val videosMerger: VideosMerger,
    private val logger: Logger,
    private val maxParallelDownloads: MaxParallelDownloads,
    private val analyticHelper: AnalyticHelper
) : DownloaderTypeProvider {
    override fun providerDownloader(url: String): MediaDownloader {

        return if (url.trim().endsWith("m3u8"))
            M3u8DownloaderParallel(
                context = context,
                tempDirProvider = tempDirProvider,
                m3U8PlaylistParser = m3U8PlaylistParser,
                videosMerger = videosMerger,
                logger = logger,
                maxParallelDownloads = maxParallelDownloads,
                analyticHelper = analyticHelper

            ) else
            CustomDownloaderImpl(
                context = context,
                tempDirProvider = tempDirProvider,
                videosMerger = videosMerger,
                logger = logger,
                maxParallelDownloads = maxParallelDownloads,
                analyticHelper = analyticHelper
            )

    }
}