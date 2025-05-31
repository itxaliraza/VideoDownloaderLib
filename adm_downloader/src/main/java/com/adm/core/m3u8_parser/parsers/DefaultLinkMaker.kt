package com.adm.core.m3u8_parser.parsers

import com.adm.core.m3u8_parser.model.SingleStream

class DefaultLinkMaker : LinkMaker {
    override fun makeLink(baseM3u8Link: String, stream: SingleStream): SingleStream {
        val baseUrl = baseM3u8Link.substringBeforeLast("/")
        val fullLink = if (stream.link.startsWith("http")) {
            stream.link
        } else {
            "$baseUrl/${stream.link}"
        }

        return stream.copy(link = fullLink)
    }
}