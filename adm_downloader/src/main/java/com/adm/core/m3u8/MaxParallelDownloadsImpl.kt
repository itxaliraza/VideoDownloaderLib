package com.adm.core.m3u8

class MaxParallelDownloadsImpl:MaxParallelDownloads {
    override fun getMaxParallelDownloadsCount(): Int {
        return 5
    }
}