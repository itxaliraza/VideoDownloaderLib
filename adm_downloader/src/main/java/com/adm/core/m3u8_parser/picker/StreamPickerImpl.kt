package com.adm.core.m3u8_parser.picker

import com.adm.core.m3u8_parser.listeners.StreamPicker
import com.adm.core.m3u8_parser.model.M3u8Stream

class StreamPickerImpl : StreamPicker {
    override suspend fun pickM3u8Stream(
        mainM3u8Link: String, streams: List<M3u8Stream>
    ): M3u8Stream? {
        val stream = streams.getOrNull(0)
        return if (stream != null) {
            if (stream.url.startsWith("http")) {
                stream
            } else {
                val baseUrl = mainM3u8Link.substringBeforeLast("/")
                stream.copy(
                    url = baseUrl + "/${stream.url}"
                )
            }
         } else {
            null
        }
    }
}