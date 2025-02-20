package adm.downloader.domain


import adm.downloader.model.SupportedMimeTypes
import adm.downloader.model.VideoModel
import com.example.domain.DownloadDirectoryProvider
import com.example.framework.core.download.getDownloadFolder
import com.example.framework.core.models.MediaType
import com.example.sdk.DownloaderSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadMediaUseCase(
    private val downloaderSdk: com.example.sdk.DownloaderSdk,
    private val downloadDirectoryProvider: DownloadDirectoryProvider
) {
    suspend operator fun invoke(video: VideoModel): Long {
        return withContext(Dispatchers.IO) {
            val quality = video.qualities?.get(0)
            if (quality != null) {
                return@withContext downloaderSdk.startDownloading(
                    url = quality.url,
                    thumb = video.thumbnail ?: quality.url,
                    fileName = quality.name + ".mp4",
                    directoryPath = downloadDirectoryProvider.getFolderInsideDownloadsDirectory(
                        quality.mediaType,
                        video.sourceSite.getDownloadFolder().mName
                    ).absolutePath,
                    mimeType = quality.mediaType.getSupportedMimeType(),
                    headers = video.headers ?: emptyMap(),
                    supportChunks = true,
                    showNotification = true
                )
            }
            return@withContext -1

        }


    }
}

fun MediaType.getSupportedMimeType(): String {
    return when (this) {
        MediaType.Image -> SupportedMimeTypes.Image.mimeTye
        MediaType.Video -> SupportedMimeTypes.Video.mimeTye
        MediaType.Audio -> SupportedMimeTypes.Audio.mimeTye
    }
}

