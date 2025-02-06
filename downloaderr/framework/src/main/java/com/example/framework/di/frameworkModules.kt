package com.example.framework.di


import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import com.example.framework.core.InternetController
val frameworkModules = module {
    factoryOf(::InternetController)

}