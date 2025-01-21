package com.adm.core.services.downloader

interface DownloaderTypeProvider {
    fun providerDownloader(url: String): MediaDownloader
}