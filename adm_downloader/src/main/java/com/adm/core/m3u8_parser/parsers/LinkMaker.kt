package com.adm.core.m3u8_parser.parsers

import com.adm.core.m3u8_parser.model.SingleStream

interface LinkMaker {
    fun makeLink(baseM3u8Link: String, stream: SingleStream): SingleStream
}