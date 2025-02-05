package com.example.downloader

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adm.domain.repository.InProgressRepository
import com.example.downloader.DownloadingWorker.Companion.log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NetConnectedWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val inProgressRepository: InProgressRepository by inject()

    override suspend fun doWork(): Result {
        inProgressRepository.getAllQueVideosSinglePaused().forEach {

            val workerDownloadingModel = WorkerDownloadingModel(
                id = it.downloadId.toString(),
                url = it.url,
                fileName = it.fileName,
                destinationDirectory = it.destinationDirectory,
                mimeType = it.mimeType,
                headers = it.headers,
                showNotification = it.showNotification,
                supportChunks = it.supportChunks,
            )
            log("Tryin to start Download Worker")


            DownloadingWorker.startWorker(
                context = context,
                id = it.downloadId.toString(),
                workerDownloadingModel = workerDownloadingModel
            )

        }
        return Result.success()
    }

    companion object {
        fun startNetWorker(context: Context) {
            val downloadingBuilder = OneTimeWorkRequestBuilder<NetConnectedWorker>()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            downloadingBuilder.setConstraints(constraints)
            downloadingBuilder.addTag("net")
            WorkManager.getInstance(context)
                .beginUniqueWork("net", ExistingWorkPolicy.REPLACE, downloadingBuilder.build())
                .enqueue()
        }
    }
}