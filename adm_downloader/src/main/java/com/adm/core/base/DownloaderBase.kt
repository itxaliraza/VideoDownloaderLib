package com.adm.core.base

import com.adm.core.interfaces.DownloaderCore

abstract class DownloaderBase(
    private val url: String,
    private val fileName: String,
    private val destinationDirectory: String,
    private val headers: Map<String, String> = hashMapOf(),
    private val mimeType: String,
    private val showNotification: Boolean
) : DownloaderCore {
}