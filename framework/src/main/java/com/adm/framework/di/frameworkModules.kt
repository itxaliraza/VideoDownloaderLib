package com.adm.framework.compose.di


import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import com.adm.framework.compose.core.InternetController
val frameworkModules = module {
    factoryOf(::InternetController)

}