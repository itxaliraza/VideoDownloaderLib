package com.adm.framework.core.models

enum class MediaType {
    Image,
    Video,
    Audio
}

fun MediaType.getFolderName(): String {
    return when (this) {
        MediaType.Image -> "Images"
        MediaType.Video -> "Videos"
        MediaType.Audio -> "Audios"
    }
}
fun MediaType.getMimeType(): String {
    return when (this) {
        MediaType.Image -> "image/*"
        MediaType.Video -> "video/*"
        MediaType.Audio -> "audio/*"
    }
}

fun MediaType.getNormalName(): String {
    return when (this) {
        MediaType.Image -> "Image"
        MediaType.Video -> "Video"
        MediaType.Audio -> "Audio"
    }
}