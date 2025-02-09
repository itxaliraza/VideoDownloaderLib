package com.adm.data.di

import com.adm.data.downloder.DownloaderSdkImpl
import com.adm.data.downloder.ProgressManagerImpl
import com.adm.data.repository.DownloadDirectoryProviderImpl
import com.adm.data.repository.DownloadsRepositoriesImpl
import com.adm.data.repository.InProgressRepositoryImpl
import com.adm.data.repository.LocalMediaInfoRepositoryImpl
import com.adm.data.repository.LocalMediaRepositoryImpl
import com.adm.data.repository.UrisHelper
import com.example.domain.DownloadDirectoryProvider
import com.example.domain.managers.progress_manager.ProgressManager
import com.example.domain.repository.InProgressRepository
import com.example.domain.repository.LocalMediaInfoRepository
import com.example.domain.repository.LocalMediaRepository
import com.example.domain.repository.LocalRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val downloaderDataModule = module {
    factoryOf(::LocalMediaInfoRepositoryImpl) { bind<LocalMediaInfoRepository>() }
    factoryOf(::LocalMediaRepositoryImpl) { bind<LocalMediaRepository>() }
    singleOf(::InProgressRepositoryImpl) { bind<InProgressRepository>() }
    singleOf(::ProgressManagerImpl) { bind<ProgressManager>() }
    singleOf(::DownloaderSdkImpl) { bind<com.example.sdk.DownloaderSdk>() }
    singleOf(::DownloadDirectoryProviderImpl) { bind<DownloadDirectoryProvider>() }
    singleOf(::DownloadsRepositoriesImpl) { bind<LocalRepository>() }
    factory {
        UrisHelper()
    }
}