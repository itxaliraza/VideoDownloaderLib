package com.example.main.di

import com.adm.core.services.downloader.DownloaderTypeProviderImpl
import com.example.downloader.DownloadNotificationManager
import org.koin.dsl.module

val downloaderModule = module{
    single { DownloadNotificationManager(get(),get()) }
 }