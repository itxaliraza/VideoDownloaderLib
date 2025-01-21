package com.adm.core.m3u8_parser.model.server

import kotlinx.serialization.Serializable

@Serializable
data class Data(
    val link: String,
    val response: List<Response>,
    val totalSize: Int
)