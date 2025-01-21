package com.adm.core.m3u8_parser.parsers

import android.util.Log
import com.adm.core.m3u8_parser.api.ApiHitter
import com.adm.core.m3u8_parser.api.ApiHitterImpl
import com.adm.core.m3u8_parser.listeners.M3u8PlaylistParser
import com.adm.core.m3u8_parser.model.M3u8Info
import com.adm.core.m3u8_parser.model.M3u8Stream
import com.adm.core.m3u8_parser.parsers.playlist.M3u8Parser1
import com.adm.core.m3u8_parser.retry.M3u8LinkRetry
import com.adm.core.m3u8_parser.retry.M3u8LinkRetryImpl
import kotlinx.coroutines.delay

class M3U8PlaylistParserImpl(
    private val apiHitter: ApiHitter = ApiHitterImpl(),
    private val retry: M3u8LinkRetry = M3u8LinkRetryImpl(),
    private val parsersList: List<M3u8TextParsers> = listOf(
        M3u8Parser1()
    )
) : M3u8PlaylistParser {
    private var currentRetries = 0
    private val TAG = "M3u8ParserImpl"

    override suspend fun parseM3u8Text(text: String): M3u8Info {
        val streams = HashMap<String, List<M3u8Stream>>()
        log("ParseM3u8Text=$text")
        parsersList.forEachIndexed { index, it ->
            val response = it.parseM3u8Text(text)
            log("Line($index)=$response")
            streams[index.toString()] = response
        }
        val bestStreams = streams.maxByOrNull { it.value.size }?.value ?: emptyList()
        return M3u8Info(
            streams = bestStreams
        )
    }

    override suspend fun getM3u8Streams(url: String, headers: Map<String, String>): M3u8Info? {
        return try {
            val response = apiHitter.get(url)
            if (response == null) {
                val retryCount = retry.getRetryCount()
                if (currentRetries < retryCount) {
                    delay(retry.delayAfterFailures())
                    currentRetries += 1
                    getM3u8Streams(url, headers)
                } else {
                    null
                }
            } else {
                parseM3u8Text(response)
            }
        } catch (e: Exception) {
            log("Exception ${e.message}")
            null
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, "M3u8ParserImpl:$msg")
    }
}