package com.adm.data.downloder

import android.content.Context
import android.util.Log
import com.example.domain.managers.progress_manager.DownloadingState
import com.example.domain.managers.progress_manager.MyDownloaderManager
import com.example.domain.managers.progress_manager.ProgressManager
import com.example.domain.repository.InProgressRepository
import com.example.entities.InProgressVideoDB
import com.example.main.DownloadingWorker
import com.example.main.WorkerDownloadingModel

class MyDownloaderManagerImpl(
    private val context: Context,
    private val inProgressRepository: InProgressRepository,
    private val progressManager: ProgressManager
) : MyDownloaderManager {

    companion object {
        const val TAG = "MyDownloadManager"
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
    }

    override suspend fun startDownloading(
        url: String,
        thumb: String,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ): Long {
        log("startDownloading")
//val url="https://vod3.cf.dmcdn.net/sec2(cg5dGbBp0Er0QX6e9iWo9VSI0SCIWbZZWSzsN7_kk58jFzFC44X--bE8neJPSZ3agC-HRgfYzAzcyeCbHo-WwnUOgMSTzY1GwB53G9uP-HNJ_DqedBpHQ0_1zBP40As6Fb31O4FHlAZItA2lJfHY0Z28hwBGSG2BJhTjY1gj1sxYNDDj43fX5DklSJmgdXqH)/video/663/218/566812366_mp4_h264_aac_fhd_1.m3u8#cell=cf3"
         val id = System.currentTimeMillis()
        insertIntoDB(
            id = id,
            url = url,
            fileName = fileName,
            thumb = thumb,
            directoryPath = directoryPath,
            mimeType = mimeType,
            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks,
        )


        val workerDownloadingModel = WorkerDownloadingModel(
            id = id.toString(),
            url = url,
            fileName = fileName,
            destinationDirectory = directoryPath,
            mimeType = mimeType,
            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks,
        )


        DownloadingWorker.startWorker(
            context = context,
            id = id.toString(),
            workerDownloadingModel = workerDownloadingModel
        )
        return id
    }


    override suspend fun pauseDownloading(id: Long) {
        progressManager.updateStatus(
            id.toString(),
            DownloadingState.Paused
        )
        DownloadingWorker.stopWorker(context, id.toString())
    }

    override suspend fun resumeDownloading(id: Long) {
        progressManager.updateStatus(
            id.toString(),
            DownloadingState.Progress
        )
        val video = inProgressRepository.getItemById(id)
        if (video != null) {

            val workerDownloadingModel = WorkerDownloadingModel(
                id = id.toString(),
                url = video.url,
                fileName = video.fileName,
                destinationDirectory = video.destinationDirectory,
                mimeType = video.mimeType,
                headers = video.headers,
                showNotification = video.showNotification,
                supportChunks = video.supportChunks,
            )

            DownloadingWorker.startWorker(context, id.toString(), workerDownloadingModel)

        }
    }

    override suspend fun deleteDownloading(id: Long) {
        progressManager.deleteVideo(id.toString())
    }

    override suspend fun cancelDownloading(id: Long) {
        progressManager.updateStatus(id.toString(), DownloadingState.Paused)
        DownloadingWorker.stopWorker(context, id.toString())
    }

    suspend fun insertIntoDB(
        id: Long,
        url: String,
        thumb: String?,
        fileName: String,
        directoryPath: String,
        mimeType: String,
        headers: Map<String, String>,
        showNotification: Boolean,
        supportChunks: Boolean
    ) {
        val inProgressVideoDB = InProgressVideoDB(
            downloadId = id,
            url = url,
            thumb = thumb ?: "",
            fileName = fileName,
            destinationDirectory = directoryPath,
            mimeType = mimeType,
            headers = headers,
            showNotification = showNotification,
            supportChunks = supportChunks,
            status = DownloadingState.Progress.name
        )
        progressManager.addLocalVideo(inProgressVideoDB)
    }

    override val MAX_RETRIES: Int
        get() = 5

}
