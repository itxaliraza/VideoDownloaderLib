package com.down.adm_core

import adm.downloader.DownloadMediaUseCase
import adm.downloader.Quality
import adm.downloader.VideoModel
import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adm.core.DownloaderCoreImpl
import com.adm.core.components.DownloadingState
import com.example.domain.managers.progress_manager.InProgressVideoUi
import com.example.domain.managers.progress_manager.MyDownloaderManager
import com.example.domain.managers.progress_manager.ProgressManager
import com.example.framework.core.models.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

data class ScreenState(
    val progress: Float = 0f,
    val status: DownloadingState = DownloadingState.Idle
)

class MainScreenViewModel(
    private val downloadMediaUseCase: DownloadMediaUseCase,
    private val progressManager: ProgressManager,
    private val downloaderManager: MyDownloaderManager
) : ViewModel() {


    val videos = progressManager.videosProgress.map {
        val list: List<InProgressVideoUi> = it.sortedByDescending { it.id.toLong() }
        /*  list.filter {
              it.status!=com.example.domain.managers.progress_manager.DownloadingState.Success
          }*/
        list
    }
    private val _state = MutableStateFlow(ScreenState())
    val state = _state.asStateFlow()
    private lateinit var downloader: DownloaderCoreImpl

    fun download(context: Context, fileName: String, textUrl: String) {
        val (_, directory) = getFileNameAndDirectory()
        viewModelScope.launch {

            val quality = Quality(
                name = fileName,
                url = textUrl,
                mediaType = MediaType.Video,
                size = 0,
                isSelected = true
            )
            val video = VideoModel(
                title = fileName,
                thumbnail = null,
                duration = "",
                qualities = listOf(quality),
                sourceSite = "FB",
                headers = emptyMap()
            )
            downloadMediaUseCase(
                video = video
            )
        }
    }


    private fun getFileNameAndDirectory(): Pair<String, String> {
        val directory = "Ishfaq"
//        val fileName = System.currentTimeMillis().toString() + ".mp4"
        val fileName = "Test" + ".mp4"
        val mainStorage =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        File(mainStorage, directory).mkdir()
        return Pair(fileName, File(mainStorage, directory).absolutePath)
    }


    fun playPauseVideo(inProgressVideoUi: InProgressVideoUi) {
        viewModelScope.launch {
            if (inProgressVideoUi.status == com.example.domain.managers.progress_manager.DownloadingState.Progress) {
                downloaderManager.pauseDownloading(inProgressVideoUi.id.toLong())
            } else {
                downloaderManager.resumeDownloading(inProgressVideoUi.id.toLong())
            }
        }
    }


}