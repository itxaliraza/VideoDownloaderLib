package com.adm.core.m3u8_parser.model.server

import kotlinx.serialization.Serializable


@Serializable
data class ServerChunkSizeModel(
    val data: Data,
    val message: String,
    val success: Boolean
)