package com.adm.core.m3u8_parser.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ApiHitterImpl : ApiHitter {
    private val TAG = "ApiHitterDefault"
    override suspend fun get(link: String, headers: Map<String, String>): String? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(link)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // Add headers
                for ((key, value) in headers) {
                    connection.setRequestProperty(key, value)
                }

                connection.connect()
                // Check if the response is OK (HTTP 200)
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line).append("\n")
                    }
                    reader.close()
                    val finalResponse = response.toString().trim()
                    log("Response $finalResponse")
                    finalResponse.toString()
                } else {
                    log("HTTP Error: ${connection.responseCode}")
                    throw Exception("HTTP Error: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                log("HTTP Exception: ${e.message}")
                throw e
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun log(msg: String) {
        Log.d(TAG, "Api Hitter:$msg")
    }
}