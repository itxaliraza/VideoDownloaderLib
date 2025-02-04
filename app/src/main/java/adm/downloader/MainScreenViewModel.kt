package com.down.adm_core

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adm.core.DownloaderCoreImpl
import com.adm.core.components.DownloadingState
import com.adm.core.m3u8.M3u8Downloader
import com.adm.core.m3u8.M3u8DownloaderParallel
import com.adm.core.services.downloader.DownloaderTypeProvider
import com.adm.core.services.downloader.DownloaderTypeProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject
import java.io.File

data class ScreenState(
    val progress: Float = 0f,
    val status: DownloadingState = DownloadingState.Idle
)

class MainScreenViewModel(val typeProvider: DownloaderTypeProvider) : ViewModel() {


    private val _state = MutableStateFlow(ScreenState())
    val state = _state.asStateFlow()
    private lateinit var downloader: DownloaderCoreImpl

     fun download(context: Context, fileName: String, textUrl: String) {
        val (_, directory) = getFileNameAndDirectory()
        val downloaderr=typeProvider.providerDownloader(url = textUrl)
        downloader = DownloaderCoreImpl(
            url = textUrl,
            fileName =fileName,
            destinationDirectory = directory,
            showNotification = true,
            mediaDownloader = downloaderr
        )
        viewModelScope.apply {
            launch {
                downloader.startDownloading(context)
            }
            launch {
                checkProgress(downloader)
            }
        }
    }

    fun pause(context: Context) {
        downloader.pauseDownloading(context)
    }

    fun resume(context: Context) {
        downloader.resumeDownloading(context)
    }

    private fun getFileNameAndDirectory(): Pair<String, String> {
        val directory = "Ishfaq"
//        val fileName = System.currentTimeMillis().toString() + ".mp4"
        val fileName = "Test" + ".mp4"
        val mainStorage =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        File(mainStorage, directory).mkdir()
        return Pair(fileName,  File(mainStorage, directory).absolutePath)
    }

    private suspend fun checkProgress(downloader: DownloaderCoreImpl) {
        Log.d("cvv", "checkProgress: started")
        var runLoop = true
        while (runLoop) {
            val downState = downloader.getDownloadingState()
            val downloaded = downloader.getDownloadedSize()
            val total = downloader.getTotalSize()
            Log.d("cvv", "checkProgress(${downState})\ndownloaded=${downloaded},total=${total}")
            val progress =
                calculateProgress(downloaded, total)
            _state.update {
                it.copy(
                    progress = progress,
                    status = downState
                )
            }
            delay(1000)
            if (downState == DownloadingState.Success) {
                val path =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/" + downloader.getDestinationDirectory() + "/" + downloader.getFileName()
                        .substringBeforeLast(".")
                val list = (File(path).listFiles()?.toList() ?: emptyList()).fastMap {
                    it.path
                }
                Log.d("cvv", "checkProgress: File Path=${path}")
                withContext(Dispatchers.IO) {
                    File("$path.mp4").createNewFile()
//                    M3u8VideosMerger().mergeVideos(list, path + ".mp4")
                }
                runLoop = false
            }
        }
    }

    private fun calculateProgress(downloadedBytes: Long, totalBytes: Long): Float {
        return if (totalBytes > 0L) {
            val percentageIn100 = ((downloadedBytes.toFloat() / totalBytes) * 100) / 100f
            Log.d("cvv", "calculateProgress(${downloadedBytes}/${totalBytes}): $percentageIn100")
            percentageIn100
        } else {
            0f // Return 0.0 if totalBytes is 0 to avoid division by zero
        }
    }

    fun merge(context: Context) {
        viewModelScope.launch {
            val path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/Ishfaq/Test"
            val file = File(path)
            val list = (file.listFiles()?.toList()?.mapNotNull { it } ?: emptyList())
            val sorted = list.sortedBy {
                it.name
            }
            Log.d(
                "cvv",
                "checkProgress(exists=${file.exists()})\nFile Path=${path}\nItems=${list.size}\nList=${sorted}"
            )
            withContext(Dispatchers.IO) {
                File("$path.mp4").createNewFile()
            }
        }
    }


}