package com.adm.core.components

enum class DownloadingState {
    Idle,
    Progress,
    PausedNetwork,
    Failed,
    Paused,
    Success
}


fun String.getDownloadingStatus(): DownloadingState {
    return DownloadingState.entries.firstOrNull { it.name == this } ?: DownloadingState.Idle
}