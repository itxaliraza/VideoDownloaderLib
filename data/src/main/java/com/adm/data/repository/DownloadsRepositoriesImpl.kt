package com.adm.data.repository

import android.content.Context
import android.net.Uri
import com.adm.domain.DownloadDirectoryProvider
import com.adm.domain.repository.LocalMediaRepository
import com.adm.domain.repository.LocalRepository
import com.adm.framework.core.download.DownloadFolder
import com.adm.framework.core.models.DownloadsFolderModel
import com.adm.framework.core.models.DownloadsModel
import com.adm.framework.core.models.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext


class DownloadsRepositoriesImpl(
    private val context: Context,
    private val downloadDirectoryProvider: DownloadDirectoryProvider,
    private val localMediaRepository: LocalMediaRepository
) : LocalRepository {

    private var downloadsMap: HashMap<String, List<DownloadsModel>> = hashMapOf()
    override suspend fun getDownloadedVideoFolders(): List<DownloadsFolderModel> =
        withContext(Dispatchers.IO) {
            downloadsMap.clear()
            try {
                val result = DownloadFolder.entries.map {entry->
                    async {
                        val images = async {
                            localMediaRepository.getAllMediaItems(
                                downloadDirectoryProvider.getFolderInsideDownloadsDirectory(
                                    MediaType.Image,
                                    entry.mName
                                ),
                                mediaType = MediaType.Image
                            )
                        }
                        val videos =
                            async {
                                localMediaRepository.getAllMediaItems(
                                    downloadDirectoryProvider.getFolderInsideDownloadsDirectory(
                                        MediaType.Video,
                                        entry.mName
                                    ),
                                    mediaType = MediaType.Video

                                )
                            }
                        val audios =
                            async {
                                localMediaRepository.getAllMediaItems(
                                    downloadDirectoryProvider.getFolderInsideDownloadsDirectory(
                                        MediaType.Audio,
                                        entry.mName
                                    ),
                                    mediaType = MediaType.Audio
                                )
                            }

                        val items =
                            (images.await() + videos.await() + audios.await()).sortedByDescending { it.dateLong }
                        downloadsMap[entry.mName] = items.map { it.copy(folderName = entry.mName) }
                    }
                }
                result.awaitAll()
                return@withContext downloadsMap.mapNotNull {
                    if (it.value.isNotEmpty())
                        DownloadsFolderModel(
                            folderName = it.key,
                            folderThumb = it.value.getOrNull(0)?.path ?: "",
                            items = it.value
                        ) else
                        null
                }
            } catch (_: Exception) {
            }
            return@withContext emptyList()
        }

    override suspend fun getDownloadedVideosInFolder(folderName: String): List<DownloadsModel> {
        return downloadsMap.get(folderName) ?: emptyList()
    }

    override fun searchVideos(query: String): List<DownloadsModel> {
        return emptyList()
    }


    override suspend fun deleteVideo(uris: Uri)= withContext(Dispatchers.IO) {
        val newMap: java.util.HashMap<String, List<DownloadsModel>> = hashMapOf()
        downloadsMap.forEach { (s, downloadsModels) ->
          val index=  downloadsModels.indexOfFirst{
                it.uri==uris
            }
            val newModels=downloadsModels.toMutableList()
            if (index!=-1){
                newModels.removeAt(index)
            }
            newMap[s]=newModels
        }
        downloadsMap=newMap
     }

}