package com.example.main.di

import com.adm.core.m3u8_parser.parsers.DefaultLinkMaker
import com.adm.core.m3u8_parser.parsers.LinkMaker
import com.example.main.DownloadNotificationManager
import org.koin.dsl.module

val downloaderModule = module{
    single { DownloadNotificationManager(get(),get()) }
    single<LinkMaker> { DefaultLinkMaker() }
 }