package com.adm.core.services.downloader

import android.content.Context
import com.adm.core.m3u8.M3u8DownloaderParallel
import com.adm.core.m3u8.MaxParallelDownloads
import com.adm.core.m3u8.MaxParallelDownloadsImpl
import com.adm.core.m3u8.SimpleVideosMergerImpl
import com.adm.core.m3u8.TempDirProvider
import com.adm.core.m3u8.TempDirProviderImpl
import com.adm.core.m3u8.VideosMerger
import com.adm.core.services.logger.Logger
import com.adm.core.m3u8_parser.listeners.M3u8ChunksPicker
import com.adm.core.m3u8_parser.parsers.M3u8ChunksPickerImpl
import com.adm.core.services.logger.LoggerImpl

class DownloaderTypeProviderImpl(
    private val context: Context,
    private val tempDirProvider: TempDirProvider = TempDirProviderImpl(context = context),
    private val m3U8PlaylistParser: M3u8ChunksPicker = M3u8ChunksPickerImpl(),
    private val videosMerger: VideosMerger=SimpleVideosMergerImpl(LoggerImpl()),
    private val logger: Logger=LoggerImpl(),
    private val maxParallelDownloads: MaxParallelDownloads=MaxParallelDownloadsImpl(),
) : DownloaderTypeProvider {
    override fun providerDownloader(url: String): MediaDownloader {

        return if (url.trim().contains(".m3u8"))
            M3u8DownloaderParallel(
                context = context,
                tempDirProvider = tempDirProvider,
                m3U8PlaylistParser = m3U8PlaylistParser,
                videosMerger = videosMerger,
                logger = logger,
                maxParallelDownloads = maxParallelDownloads,

            ) else
            CustomDownloaderImpl(
                context = context,
                tempDirProvider = tempDirProvider,
                videosMerger = videosMerger,
                logger = logger,
                maxParallelDownloads = maxParallelDownloads,
            )

    }
}