package com.example.main

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adm.core.DownloaderCoreImpl
import com.adm.core.m3u8_parser.parsers.LinkMaker
import com.adm.core.services.downloader.DownloaderTypeProvider
import com.adm.core.services.downloader.DownloaderTypeProviderImpl
import com.example.domain.ScanFileUseCase
import com.example.domain.managers.progress_manager.DownloadingState
import com.example.domain.managers.progress_manager.ProgressManager
import com.example.framework.core.InternetController
import com.example.framework.debugToast
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException


private const val TAG = "DownloadingWorker"

class DownloadingWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val progressManager: ProgressManager by inject()
    private val downloaderSdk: com.example.sdk.DownloaderSdk by inject()
    private val internetController: InternetController by inject()
    private val scanFileUseCase:ScanFileUseCase by inject()
    private val linkMaker:LinkMaker by inject()
    private val downloaderTypeProvider: DownloaderTypeProvider = DownloaderTypeProviderImpl(
        context = context,
        linkMaker=linkMaker
    )
    private val downloadNotificationManager: DownloadNotificationManager by inject()
    private var notificationBuilder: NotificationCompat.Builder? = null

    private val listener: com.example.sdk.DownloadingListener?
        get() = DownloadingListenerManager.getDownloadListener()

    init {
        log("Doing worker created")
    }

    var currentRetries = 0

    private var workerDownloadingModel = WorkerDownloadingModel()

    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override suspend fun doWork(): Result {
        val inputData = inputData.getString(KEY_INPUT_DATA) ?: return Result.failure()
        workerDownloadingModel = inputData.convertToWorkerDownloadingModel()
        log("Doing worker created $workerDownloadingModel")

        if (workerDownloadingModel.id.isBlank()) {
            return Result.success()
        }
        downloadNotificationManager.cancelNotification(workerDownloadingModel.id.getInt() + 1)
        startForegroundNotification()
        startWorkerDownloading(workerDownloadingModel)
        mainCoroutineScope.cancel()
        return Result.success()
    }

    suspend fun startWorkerDownloading(
        workerDownloadingModel: WorkerDownloadingModel
    ) {
        log("startWorkerDownloading $workerDownloadingModel")

        val downloader = DownloaderCoreImpl(
            url = workerDownloadingModel.url,
            fileName = workerDownloadingModel.fileName,
            destinationDirectory = workerDownloadingModel.destinationDirectory,
            showNotification = workerDownloadingModel.showNotification,
            mediaDownloader = downloaderTypeProvider.providerDownloader(url = workerDownloadingModel.url),
            supportChunks = workerDownloadingModel.supportChunks,
            headers = workerDownloadingModel.headers ?: emptyMap()
        )
        try {
            coroutineScope {
                launch {
                    downloader.startDownloading(applicationContext)
                }
                checkProgress(downloader)
            }
        } catch (e: CancellationException) {
            log("checkProgress: Worker Cancelled")

            downloader.pauseDownloading(context)
            Result.failure()
        }

    }


    @OptIn(FlowPreview::class)
    private suspend fun checkProgress(downloader: DownloaderCoreImpl): Boolean {
        log("checkProgress: started")
        var isDownloaded = false
        progressManager.updateStatus(workerDownloadingModel.id, DownloadingState.Progress)
        downloader.getProgress().sample(1000).takeWhile {

            val downState = it.downStatus
            val downloaded = it.downSize
            val total = it.totalSize
            Log.d(
                "ProgressWorker",
                "${workerDownloadingModel.id} ${workerDownloadingModel.fileName} checkProgress(${downState})\ndownloaded=${downloaded},total=${total}"
            )
            progressManager.updateProgress(workerDownloadingModel.id, downloaded, total)

            updateNotification(
                workerDownloadingModel.id.toLong().toInt(),
                total, downloaded
            )

            when (downState) {
                com.adm.core.components.DownloadingState.Success -> {
                    log("DownloadingState.Success")
                    scanFileUseCase(context, filePath = workerDownloadingModel.destinationDirectory+"/"+workerDownloadingModel.fileName,workerDownloadingModel.mimeType)
                    progressManager.updateStatus(
                        workerDownloadingModel.id,
                        DownloadingState.Success
                    )
                    downloadNotificationManager.showDownloadSuccessNotification(
                        workerDownloadingModel.id.toLong().toInt() + 1,
                        workerDownloadingModel.fileName
                    )


                    listener?.onDownloadingCompleted(workerDownloadingModel.id)

                    isDownloaded = true
                    false
                }

                com.adm.core.components.DownloadingState.Failed -> {
                    if (!internetController.isInternetConnected) {
                        listener?.onDownloadingPaused(workerDownloadingModel.id,false)
                        NetConnectedWorker.startNetWorker(context)
                        progressManager.updateStatus(
                            workerDownloadingModel.id,
                            DownloadingState.PausedNetwork
                        )
                        updateNotification(
                            workerDownloadingModel.id.getInt() + 1,
                            total, downloaded
                        )
                        false
                    } else {

                        if (currentRetries >= downloaderSdk.MAX_RETRIES) {
                            listener?.onDownloadingFailed(workerDownloadingModel.id)
                            context.debugToast(it.exc?.message.toString())
                            progressManager.updateStatus(
                                workerDownloadingModel.id,
                                DownloadingState.Failed
                            )
                            false
                        } else {
                            currentRetries += 1
                            delay((downloaderSdk.MAX_RETRIES - (downloaderSdk.MAX_RETRIES - currentRetries)) * 1000.toLong())
                            log("Current retries = $currentRetries, delay=${downloaderSdk.MAX_RETRIES - (downloaderSdk.MAX_RETRIES - currentRetries)}")

                            downloader.startDownloading(applicationContext)
                            true
                        }
                    }
                }

                else -> {
                    true // Continue the flow
                }
            }
        }
            .collectLatest { }


        return isDownloaded

    }

    private fun updateNotification(id: Int, total: Long, downloaded: Long) {
        notificationBuilder?.let {
            val progress = if (total > 0) ((downloaded / total.toFloat()) * 100).toInt() else 0
            downloadNotificationManager.updateProgress(
                it,
                id,
                progress
            )
        }
    }


    private suspend fun startForegroundNotification() {
        notificationBuilder =
            downloadNotificationManager.createDownloadingNotification(workerDownloadingModel.fileName)
        notificationBuilder?.let {
            val foregroundInfo = ForegroundInfo(
                workerDownloadingModel.id.toLong().toInt(),
                it.build()
            )
            setForeground(foregroundInfo)

        }
    }

//    fun reEnqueVideo(context: Context){
//        val downloadingBuilder = OneTimeWorkRequestBuilder<DownloadingWorker>()
//
//        val inputDataBuilder = Data.Builder()
//        inputDataBuilder.putString(KEY_INPUT_DATA, workerDownloadingModel.convertToString())
//        downloadingBuilder.setInputData(inputDataBuilder.build())
//
//        val constraints=Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//        downloadingBuilder.setConstraints(constraints)
//        WorkManager.getInstance(context)
//            .beginUniqueWork(workerDownloadingModel.id, ExistingWorkPolicy.KEEP, downloadingBuilder.build())
//            .enqueue()
//    }

    companion object {
        const val KEY_INPUT_DATA = "KEY_INPUT_DATA"

        fun startWorker(
            context: Context,
            id: String,
            workerDownloadingModel: WorkerDownloadingModel
        ) {
            log("Starting Download Worker")
            val downloadingBuilder = OneTimeWorkRequestBuilder<DownloadingWorker>()

            val inputDataBuilder = Data.Builder()
            inputDataBuilder.putString(KEY_INPUT_DATA, workerDownloadingModel.convertToString())
            downloadingBuilder.setInputData(inputDataBuilder.build())
            val constraints = Constraints.Builder()
                .build()
            downloadingBuilder.setConstraints(constraints)
//            downloadingBuilder.addTag(id.toString())
            WorkManager.getInstance(context)
                .beginUniqueWork(
                    id.toString(),
                    ExistingWorkPolicy.REPLACE,
                    downloadingBuilder.build()
                )
                .enqueue()

        }

        fun stopWorker(
            context: Context,
            id: String,
        ) {
            log("Starting Download Worker")
            WorkManager.getInstance(context).cancelUniqueWork(id.toString())


        }

        fun log(msg: String) {
            Log.d(TAG, msg)
        }
    }


}


@Keep
data class WorkerDownloadingModel(
    val id: String = "",
    val url: String = "",
    var fileName: String = "",
    val destinationDirectory: String = "",
    val mimeType: String = "",
    val headers: Map<String, String>? = null,
    val showNotification: Boolean = false,
    val supportChunks: Boolean = false,
)

fun WorkerDownloadingModel.convertToString(): String {
    return Gson().toJson(this) ?: ""
}

fun String.convertToWorkerDownloadingModel(): WorkerDownloadingModel {
    return Gson().fromJson(this, WorkerDownloadingModel::class.java)
}


fun String.getInt(): Int {
    return this.toLong().toInt()
}