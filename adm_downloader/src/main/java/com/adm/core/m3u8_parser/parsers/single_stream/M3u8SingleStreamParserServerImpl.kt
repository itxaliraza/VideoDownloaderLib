package com.adm.core.m3u8_parser.parsers.single_stream

import android.util.Log
import com.adm.core.m3u8_parser.api.ApiHitter
import com.adm.core.m3u8_parser.api.ApiHitterImpl
import com.adm.core.m3u8_parser.listeners.M3u8SingleStreamParser
import com.adm.core.m3u8_parser.model.SingleStream

class M3u8SingleStreamParserServerImpl(
    private val apiHitter: ApiHitter = ApiHitterImpl(),
) : M3u8SingleStreamParser {

    private val TAG = "M3u8SingleStreamParserImpl"

    private val m3u8Starting = "#EXTM3U"
    private val infoLineStarting = "#EXTINF:"
    private var filterStarted = false
    private var pickNextLine = false

    override suspend fun getM3u8Chunks(
        url: String,
        headers: Map<String, String>
    ): List<SingleStream> {
        val response = apiHitter.get(link = url, headers)
        log("Response=${response}")
        return if (response == null) {
            emptyList()
        } else {
            if (response.contains("#EXTINF:")) {
                val chunks = getM3u8Chunks(response)
                log("Total Chunks : ${chunks.size}")
                chunks
            } else {
                log("Bad Response For Single Streams")
                emptyList()
            }
        }
    }

    override suspend fun getM3u8Chunks(text: String): List<SingleStream> {
        val streams = mutableListOf<SingleStream>()
        val lines = text.lines()
        lines.forEach { line ->
            if (line.startsWith(m3u8Starting)) {
                filterStarted = true
            } else if (filterStarted && line.startsWith(infoLineStarting)) {
                pickNextLine = true
            } else if (pickNextLine) {
                streams.add(
                    SingleStream(
                        link = line,
                    )
                )
                pickNextLine = false
            }
        }
        return streams
    }

    fun log(msg: String) {
        Log.d(TAG, "M3u8SingleStreamParserImpl:$msg")
    }
}