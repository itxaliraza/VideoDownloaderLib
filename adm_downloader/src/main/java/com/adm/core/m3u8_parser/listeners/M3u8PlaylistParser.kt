package com.adm.core.m3u8_parser.listeners

import com.adm.core.m3u8_parser.model.M3u8Info

interface M3u8PlaylistParser {
    suspend fun getM3u8Streams(
        url: String,
        headers: Map<String, String>
    ): M3u8Info?
    suspend fun parseM3u8Text(text: String): M3u8Info?
}