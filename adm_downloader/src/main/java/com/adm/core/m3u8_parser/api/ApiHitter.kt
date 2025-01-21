package com.adm.core.m3u8_parser.api

interface ApiHitter {
    suspend fun get(link: String, headers: Map<String, String> = mapOf()): String?
}