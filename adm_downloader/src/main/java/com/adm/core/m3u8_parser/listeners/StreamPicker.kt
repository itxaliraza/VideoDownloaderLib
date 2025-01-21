package com.adm.core.m3u8_parser.listeners

import com.adm.core.m3u8_parser.model.M3u8Stream

interface StreamPicker {
    suspend fun pickM3u8Stream(mainM3u8Link: String,streams: List<M3u8Stream>): M3u8Stream?
}