package com.adm.core.m3u8

import android.content.Context
import android.os.Environment
import java.io.File

class TempDirProviderImpl(private val context: Context):TempDirProvider {
    override fun provideTempDir(child:String): File {
        val folder= File(context.filesDir.absolutePath,child)
        if (folder.exists().not())
            folder.mkdirs()
        return folder
    }
}