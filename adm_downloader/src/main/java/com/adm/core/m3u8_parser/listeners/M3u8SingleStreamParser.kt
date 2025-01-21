package com.adm.core.m3u8_parser.listeners

import com.adm.core.m3u8_parser.model.M3u8Info
import com.adm.core.m3u8_parser.model.SingleStream


interface M3u8SingleStreamParser {
    suspend fun getM3u8Chunks(
        url: String,
        headers: Map<String, String>
    ): List<SingleStream>

    suspend fun getM3u8Chunks(text: String): List<SingleStream>
}