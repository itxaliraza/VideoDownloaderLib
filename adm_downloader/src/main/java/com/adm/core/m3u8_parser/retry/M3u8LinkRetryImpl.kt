package com.adm.core.m3u8_parser.retry

class M3u8LinkRetryImpl : M3u8LinkRetry {
    override fun getRetryCount(): Int {
        return 2
    }

    override fun delayAfterFailures(): Long {
        return 1500
    }
}