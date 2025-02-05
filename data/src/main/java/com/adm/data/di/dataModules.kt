package com.adm.data.di

import com.adm.data.downloder.MyDownloaderManagerImpl
import com.adm.data.downloder.ProgressManagerImpl
import com.adm.data.repository.DownloadDirectoryProviderImpl
import com.adm.data.repository.DownloadsRepositoriesImpl
import com.adm.data.repository.InProgressRepositoryImpl
 import com.adm.data.repository.LocalMediaInfoRepositoryImpl
import com.adm.data.repository.LocalMediaRepositoryImpl
import com.adm.data.repository.UrisHelper
import com.adm.domain.DownloadDirectoryProvider
import com.adm.domain.managers.progress_manager.MyDownloaderManager
import com.adm.domain.managers.progress_manager.ProgressManager
import com.adm.domain.repository.InProgressRepository
import com.adm.domain.repository.LocalMediaInfoRepository
import com.adm.domain.repository.LocalMediaRepository
import com.adm.domain.repository.LocalRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModules = module {
    factoryOf(::LocalMediaInfoRepositoryImpl) { bind<LocalMediaInfoRepository>() }
    factoryOf(::LocalMediaRepositoryImpl) { bind<LocalMediaRepository>() }
   singleOf(::InProgressRepositoryImpl) { bind<InProgressRepository>() }
    singleOf(::ProgressManagerImpl) { bind<ProgressManager>() }
    singleOf(::MyDownloaderManagerImpl) { bind<MyDownloaderManager>() }
    singleOf(::DownloadDirectoryProviderImpl) { bind<DownloadDirectoryProvider>() }
    singleOf(::DownloadsRepositoriesImpl) { bind<LocalRepository>() }
    factory {
        UrisHelper()
    }
}