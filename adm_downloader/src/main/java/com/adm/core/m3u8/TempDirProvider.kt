package com.adm.core.m3u8

import java.io.File

interface TempDirProvider {
    fun provideTempDir(child:String="temp"):File
}