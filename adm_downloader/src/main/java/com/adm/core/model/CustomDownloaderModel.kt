package com.adm.core.model

data class CustomDownloaderModel(
    val url: String = "",
    val fileName: String = "",
    val directoryPath: String = "",
    val mimeType: String = "",
    val headers: Map<String, String> = mapOf(),
    val showNotification: Boolean = false
)