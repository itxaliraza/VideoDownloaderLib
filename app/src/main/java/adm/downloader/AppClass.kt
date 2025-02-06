package com.down.adm_core

import adm.downloader.di.appModule
import android.app.Application
import com.adm.core.services.downloader.DownloaderTypeProvider
import com.adm.core.services.downloader.DownloaderTypeProviderImpl
import com.adm.data.di.downloaderDataModule
import com.adm.persistence.di.downloaderPersistenceModule
import com.example.framework.di.downloaderFrameworkModule
import com.example.main.di.downloaderModule
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
            modules(downloaderFrameworkModule)
            modules(downloaderDataModule)
            modules(downloaderPersistenceModule)
            modules(appModule)
            modules(downloaderModule)
        }
    }
}