package com.adm.core.m3u8_parser.parsers.playlist

import android.util.Log
import com.adm.core.m3u8_parser.model.M3u8Stream
import com.adm.core.m3u8_parser.parsers.M3u8TextParsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M3u8Parser1 : M3u8TextParsers {
    private val TAG = "M3u8Parser1"

    private val starting = "#EXTM3U"
    private val lineStarting = "#EXT-X-STREAM-INF:"

    private var filterStarted = false
    private var lastStream: M3u8Stream? = null

    override suspend fun parseM3u8Text(text: String): List<M3u8Stream> {
        log("parseM3u8Text:$text")
        return withContext(Dispatchers.IO) {
            val lines = text.lines()
            val streams = mutableListOf<M3u8Stream>()
            lines.forEach {
                val stream = filterLine(it)
                log("parseM3u8Text\nLines:$it\nStream=${stream}")
                if (stream != null) {
                    streams.add(stream)
                }
            }
            Log.d(TAG, "Total Streams:${streams.size}")
            Log.d(TAG, "Total Streamss:${streams}")
            streams
        }
    }

    override suspend fun filterLine(text: String): M3u8Stream? {
        return if (text.startsWith(starting)) {
            filterStarted = true
            log("filterStarted ${text}")
            null
        } else if (text.startsWith(lineStarting) && filterStarted) {
            lastStream = extractJuiceFromLink(text)
            log("Juice Info=$lastStream")
            null
        } else if (lastStream != null) {
            val juice = lastStream?.copy(
                url = text
            )
            log("Juice=$juice")
            lastStream = null
            juice
        } else {
            null
        }
    }

    private fun extractJuiceFromLink(line: String): M3u8Stream {
        val stream = M3u8Stream("")
        val information = line.replace(lineStarting, "").split(",")
        information.forEach { info ->
            val keyValue = info.split("=")
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                when (key) {
                    "BANDWIDTH" -> {
                        stream.bandWidth = value.toLong()
                    }

                    "RESOLUTION" -> {
                        stream.resolution = value
                    }

                    "CODECS" -> {
                        stream.codecs = value
                    }

                    "FRAME-RATE" -> {
                        stream.frameRate = value.toFloat()
                    }
                }
            }
        }
        return stream
    }

    fun log(msg: String) {
        Log.d(TAG, "M3u8Parser1:$msg")
    }
}