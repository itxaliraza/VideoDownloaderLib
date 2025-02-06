package com.example.framework.di


import com.example.framework.core.InternetController
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val downloaderFrameworkModule = module {
    factoryOf(::InternetController)

}