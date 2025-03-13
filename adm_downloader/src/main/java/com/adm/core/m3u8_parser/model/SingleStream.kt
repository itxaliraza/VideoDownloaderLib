package com.adm.core.m3u8_parser.model


data class SingleStream(
    val link: String,
    val size: Long? = null,
    val headers: Map<String, String>? = null
)