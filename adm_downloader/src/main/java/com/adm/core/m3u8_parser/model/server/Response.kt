package com.adm.core.m3u8_parser.model.server

import kotlinx.serialization.Serializable


@Serializable
data class Response(
    val link: String,
    val size: Int
)