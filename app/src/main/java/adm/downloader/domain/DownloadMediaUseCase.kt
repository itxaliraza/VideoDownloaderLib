package adm.downloader.domain


import adm.downloader.model.SupportedMimeTypes
import adm.downloader.model.VideoModel
import android.util.Log
import com.example.domain.DownloadDirectoryProvider
import com.example.framework.core.download.getDownloadFolder
import com.example.framework.core.models.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class DownloadMediaUseCase(
    private val downloaderSdk: com.example.sdk.DownloaderSdk,
    private val downloadDirectoryProvider: DownloadDirectoryProvider
) {
    suspend operator fun invoke(video: VideoModel): Long {
        return withContext(Dispatchers.IO.limitedParallelism(1000)) {
            val quality = video.qualities?.get(0)
            if (quality != null) {
                repeat(30) {
                    Log.d("DownloadMediaUseCase","Startdownloading $it")
                    /* return@withContext */downloaderSdk.startDownloading(
                    url = quality.url,
                    thumb = video.thumbnail ?: quality.url,
                    fileName = "${System.currentTimeMillis()}" + ".mp4",
                    directoryPath = downloadDirectoryProvider.getFolderInsideDownloadsDirectory(
                        quality.mediaType,
                        video.sourceSite.getDownloadFolder().mName
                    ).absolutePath,
                    mimeType = quality.mediaType.getSupportedMimeType(),
                    headers = video.headers ?: emptyMap(),
                    supportChunks = true,
                    showNotification = true
                )
                    delay(200)
                }
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

