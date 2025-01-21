package com.adm.core.m3u8

interface VideosMerger {
    suspend fun mergeVideos(folderPath: String, destPath: String): Result<Boolean>
}