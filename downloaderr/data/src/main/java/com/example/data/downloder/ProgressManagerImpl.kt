package com.adm.data.downloder


import android.util.Log
import com.adm.data.dto.toUiModel
import com.example.domain.managers.progress_manager.DownloadingState
import com.example.domain.managers.progress_manager.InProgressVideoUi
import com.example.domain.managers.progress_manager.ProgressManager
import com.example.domain.repository.InProgressRepository
import com.example.entities.InProgressVideoDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


@OptIn(FlowPreview::class)
class ProgressManagerImpl(
    private val inProgressRepository: InProgressRepository
) : ProgressManager {
    val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1000))

    val inprogressMap = mutableMapOf<String, InProgressVideoUi>()

    private val mapMutex = Mutex()
    private val _videosProgress = MutableStateFlow<List<InProgressVideoUi>>(emptyList())
    override val videosProgress = _videosProgress.asStateFlow()

    init {

        scope.launch {
            _videosProgress.update {
                inProgressRepository.getAllQueVideosSingle().map {
                    val model = it.toUiModel()
                    inprogressMap[model.id] = model
                    model
                }
            }
           /* videosProgress
                .sample(1000)
                .collectLatest {
                    batchUpdateDatabase()
                    Log.d("cvrrr", "batchUpdateDatabase")
                }*/
        }
    }

    override suspend fun updateProgress(id: String, downloaded: Long, total: Long) {
        Log.d(
            "cvrrr",
            "update Progress,  id=$id progress=${downloaded / total.toFloat()}} downloaded= $downloaded ${total}"
        )

        mapMutex.withLock {
            inprogressMap[id]?.let { video ->
                val mdownloaded=downloaded.coerceAtLeast(video.downloadedSize)
                val updatedVideo = video.copy(
                    downloadedSize = mdownloaded,
                    totalSize = total,
                    progress = mdownloaded / total.toFloat(),
                )
                inprogressMap[id] = updatedVideo
                emitProgressUpdates()
            }
        }
    }

    override suspend fun updateStatus(id: String, downState: DownloadingState) {
        mapMutex.withLock {
            inprogressMap[id]?.let { video ->
                val updatedVideo = video.copy(
                    status = downState
                )
                inprogressMap[id] = updatedVideo
                emitProgressUpdates()

            }
        }
    }

    override suspend fun deleteVideo(id: String) {
        inprogressMap.remove(id.toString())
        inProgressRepository.deleteFromQue(id.toLong())
    }
    override suspend fun addLocalVideo(db: InProgressVideoDB) {
        val video = inProgressRepository.getItemById(db.downloadId)
        if (video == null) {
            Log.d("cvv", "addLocalVideo $video")
            inProgressRepository.addInQue(db)
            inprogressMap[db.downloadId.toString()] = db.toUiModel()
            emitProgressUpdates()

        }
    }


    private suspend fun batchUpdateDatabase() {
        mapMutex.withLock {
            val videosToUpdate = inprogressMap.values.toList()
            val dbMap: Map<String, InProgressVideoDB> = inProgressRepository.getInProgressQueVideosSingle()
                .associateBy { it.downloadId.toString() }

            if (videosToUpdate.isNotEmpty()) {
                videosToUpdate.forEach { uiModel ->
                    val inProgressVideo= dbMap[uiModel.id]
                    if (inProgressVideo != null ) {
                        val inProgressVideoNew = inProgressVideo.copy(
                            status = uiModel.status.name,
                            downloadedSize = uiModel.downloadedSize,
                            totalSize = uiModel.totalSize,
                        )
                        if (inProgressVideoNew!=inProgressVideo){
                            inProgressRepository.addInQue(inProgressVideoNew)
                        }
                    }
                }
            }
        }
    }

    fun emitProgressUpdates() {
        _videosProgress.value = inprogressMap.values.toList()

    }

//
//    val videosProgress = inProgressRepository.getAllQueVideos().map {
//        it.map {
//            InProgressVideoUi(
//                id = it.downloadId.toString(),
//                url = it.url,
//                fileName = it.fileName,
//                destinationDirectory = it.destinationDirectory,
//                mimeType = it.mimeType,
//                status = it.status.getDownloadingStatus(),
//                downloadedSize = it.downloadedSize,
//                totalSize = it.totalSize,
//                progress = it.downloadedSize / it.totalSize.toFloat(),
//            )
//        }
//    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
}

