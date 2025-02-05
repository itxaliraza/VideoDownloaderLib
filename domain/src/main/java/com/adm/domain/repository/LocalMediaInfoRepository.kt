package com.adm.domain.repository

import android.content.Context

interface LocalMediaInfoRepository {

    suspend fun getMediaDurationByFilePath(context: Context, path: String): Long
}