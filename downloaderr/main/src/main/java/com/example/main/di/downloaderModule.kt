package com.example.main.di

import com.example.main.DownloadNotificationManager
import org.koin.dsl.module

val downloaderModule = module{
    single { DownloadNotificationManager(get(),get()) }
 }