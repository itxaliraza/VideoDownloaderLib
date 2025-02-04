package com.down.adm_core

import android.app.Application
import com.adm.core.m3u8.M3u8Downloader
import com.adm.core.m3u8.M3u8DownloaderParallel
import com.adm.core.m3u8.MaxParallelDownloadsImpl
import com.adm.core.m3u8.SimpleVideosMergerImpl
import com.adm.core.m3u8.VideosMerger
import com.adm.core.services.downloader.DownloaderTypeProvider
import com.adm.core.services.downloader.DownloaderTypeProviderImpl
import com.adm.core.services.logger.LoggerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class AppClass : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(applicationContext)
            modules(modules = module {
                viewModelOf(::MainScreenViewModel)

                single<DownloaderTypeProvider> {
                    DownloaderTypeProviderImpl(context = get())
                }
            })
        }
    }
}