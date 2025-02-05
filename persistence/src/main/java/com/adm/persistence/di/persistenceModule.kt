package com.adm.persistence.di

import androidx.room.Room
import com.adm.persistence.db.AppDatabase
import com.adm.persistence.inprogress.InProgressVideoDao
import org.koin.dsl.module

val persistenceModule = module {

    single<InProgressVideoDao> {
        get<AppDatabase>().getInProgressDao()
    }

    single<AppDatabase> {
        Room.databaseBuilder(get(), AppDatabase::class.java, "app_db")
            .build()
    }
}