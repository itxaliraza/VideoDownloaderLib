package com.adm.framework.core.download

enum class DownloadFolder(val mName: String) {
    FB("Facebook"),
    Instagram("Instagram"),
    Tiktok("TikTok"),
    Website("Website"),
}

fun String.getDownloadFolder(): DownloadFolder {
    return if (this.contains("tiktok", true))
        DownloadFolder.Tiktok
    else if (this.contains("facebook", true) || this.contains("fb", true)) {
        DownloadFolder.FB

    } else if (this.contains("insta", true)) {
        DownloadFolder.Instagram

    } else
        DownloadFolder.Website


}