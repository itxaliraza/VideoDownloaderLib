package com.adm.persistence.di

import androidx.room.Room
import com.example.persistence.db.AppDatabase
import com.adm.persistence.inprogress.InProgressVideoDao
import org.koin.dsl.module

val downloaderPersistenceModule = module {

    single<InProgressVideoDao> {
        get<AppDatabase>().getInProgressDao()
    }

    single<AppDatabase> {
        Room.databaseBuilder(get(), AppDatabase::class.java, "downloaderdb")
            .build()
    }
}