package adm.downloader.model

import com.example.framework.core.models.MediaType
import java.util.UUID

data class VideoModel(
    val title: String,
    val thumbnail: String? = null,
    val duration: String? = null,
    val qualities: List<Quality>?,
    val sourceSite: String,
    val id: String = UUID.randomUUID().toString(),
    val headers: Map<String, String>?
) {
    fun isAllVideos() = qualities?.all { it.mediaType == MediaType.Video } ?: false
}

data class Quality(
    val name: String,
    val url: String,
    val mediaType: com.example.framework.core.models.MediaType,
    val size: Long? = null,
    val isSelected: Boolean = false,
)
