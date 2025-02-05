package com.adm.domain.repository

import com.adm.domain.InProgressVideoDB
import com.adm.domain.managers.progress_manager.InProgressVideoUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InProgressRepository {
    suspend fun addInQue(inProgressVideo: InProgressVideoDB)
    fun getAllQueVideos(): Flow<List<InProgressVideoDB>>
   suspend fun getAllQueVideosSingle():  List<InProgressVideoDB>
    suspend fun getInProgressQueVideosSingle():  List<InProgressVideoDB>
    suspend fun getAllQueVideosSinglePaused():  List<InProgressVideoDB>
      suspend fun getItemById(id: Long): InProgressVideoDB?
    suspend fun deleteFromQue(id: Long)

}