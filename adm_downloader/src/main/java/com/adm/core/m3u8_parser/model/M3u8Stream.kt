package com.adm.core.m3u8_parser.model

data class M3u8Stream(
    val url: String,
    var bandWidth: Long? = null,
    var frameRate: Float? = null,
    var resolution: String? = null,
    var codecs: String? = null,
    val totalSize: Long? = null
)
