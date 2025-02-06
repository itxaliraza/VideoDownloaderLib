package adm.downloader.di

import adm.downloader.DownloadMediaUseCase
import adm.downloader.GetIntentMainImpl
import com.example.domain.managers.get_intent.GetIntentMain
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val  appModule = module{
    factoryOf(::DownloadMediaUseCase)
    factory <GetIntentMain>{GetIntentMainImpl(get())}
 }