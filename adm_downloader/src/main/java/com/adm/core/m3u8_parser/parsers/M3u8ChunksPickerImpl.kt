package com.adm.core.m3u8_parser.parsers

import android.util.Log
import com.adm.core.m3u8_parser.listeners.M3u8ChunksPicker
import com.adm.core.m3u8_parser.listeners.M3u8PlaylistParser
import com.adm.core.m3u8_parser.listeners.M3u8SingleStreamParser
import com.adm.core.m3u8_parser.listeners.StreamPicker
import com.adm.core.m3u8_parser.model.SingleStream
import com.adm.core.m3u8_parser.parsers.single_stream.M3u8SingleStreamParserImpl
import com.adm.core.m3u8_parser.picker.StreamPickerImpl

class M3u8ChunksPickerImpl(
    private val playlistParser: M3u8PlaylistParser = M3U8PlaylistParserImpl(),
    private val singleStreamParser: M3u8SingleStreamParser = M3u8SingleStreamParserImpl(),
    private val streamPicker: StreamPicker = StreamPickerImpl(),
    private val linkMaker: LinkMaker
) : M3u8ChunksPicker {
    private val TAG = "M3u8ChunksPickerImpl"
    override suspend fun getChunks(
        m3u8Link: String,
        headers: Map<String, String>
    ): List<SingleStream> {
        val chunks = singleStreamParser.getM3u8Chunks(url = m3u8Link, headers = headers).map {
            linkMaker.makeLink(baseM3u8Link = m3u8Link,it)
        }
        log("First URL(total=${chunks.size})=${chunks.getOrNull(0)}")
        return chunks.ifEmpty {
            val streams = playlistParser.getM3u8Streams(url = m3u8Link, headers)?.streams
            if (streams.isNullOrEmpty().not()) {
                val pickedStream = streamPicker.pickM3u8Stream(
                    mainM3u8Link = m3u8Link,
                    streams = streams ?: emptyList()
                )
                log("pickedStream=${pickedStream}")
                if (pickedStream != null) {
                    getChunks(m3u8Link = pickedStream.url, headers = headers)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun log(msg: String) {
        Log.d(TAG, "M3u8ChunksPickerImpl:$msg")
    }
}