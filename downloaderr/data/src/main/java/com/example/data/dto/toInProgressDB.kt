package com.adm.data.dto

import com.example.domain.managers.progress_manager.InProgressVideoUi
import com.example.domain.managers.progress_manager.getDownloadingStatus
import com.example.entities.InProgressVideoDB


fun InProgressVideoUi.toInProgressDB(model: InProgressVideoDB): InProgressVideoDB {
    return model.copy(
        status = this.status.name,
        downloadedSize = this.downloadedSize,
        totalSize = this.totalSize
    )
}

fun InProgressVideoDB.toUiModel(): InProgressVideoUi {
    return InProgressVideoUi(
        id = this.downloadId.toString(),
        url = this.url,
        fileName = this.fileName,
        thumb = this.thumb,
        destinationDirectory = this.destinationDirectory,
        mimeType = this.mimeType,
        status = this.status.getDownloadingStatus(),
        downloadedSize = this.downloadedSize,
        totalSize = this.totalSize,
        progress = this.downloadedSize / this.totalSize.toFloat(),
    )
}