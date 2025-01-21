package com.adm.core.m3u8_parser.parsers

import com.adm.core.m3u8_parser.model.M3u8Stream

interface M3u8TextParsers {
    suspend fun parseM3u8Text(text: String): List<M3u8Stream>
    suspend fun filterLine(text: String): M3u8Stream?
}