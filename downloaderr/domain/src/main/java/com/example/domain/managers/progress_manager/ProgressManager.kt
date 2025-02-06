package com.example.domain.managers.progress_manager

import com.example.entities.InProgressVideoDB
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow


@OptIn(FlowPreview::class)
interface ProgressManager {
    val videosProgress: StateFlow<List<InProgressVideoUi>>

    suspend fun updateProgress(id: String, downloaded: Long, total: Long)
    suspend fun updateStatus(id: String, downState: DownloadingState)
    suspend fun deleteVideo(id: String)
    suspend fun addLocalVideo(db: InProgressVideoDB)

}


