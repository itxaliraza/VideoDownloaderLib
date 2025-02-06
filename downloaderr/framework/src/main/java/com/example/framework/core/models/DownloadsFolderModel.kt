package com.example.framework.core.models

import android.net.Uri

    data class DownloadsFolderModel(
    val folderName: String,
    val folderThumb: String,
    val items: List<DownloadsModel>
)