package com.adm.data.repository


import com.adm.domain.InProgressVideoDB
import com.adm.domain.repository.InProgressRepository
import com.adm.persistence.inprogress.InProgressVideoDao
import kotlinx.coroutines.flow.Flow

class InProgressRepositoryImpl(
    private val dao: InProgressVideoDao
) : InProgressRepository {
    override suspend fun addInQue(inProgressVideo: InProgressVideoDB) {
        dao.addInQue(inProgressVideo)
    }

    override fun getAllQueVideos(): Flow<List<InProgressVideoDB>> {
        return dao.getAllQueVideos()
    }

    override suspend fun getAllQueVideosSingle(): List<InProgressVideoDB> {
        return dao.getAllQueVideosSingle()
    }
    override suspend fun getAllQueVideosSinglePaused(): List<InProgressVideoDB> {
        return dao.getAllQueVideosSinglePausedNetwork()
    }

    override suspend fun getInProgressQueVideosSingle(): List<InProgressVideoDB> {
        return dao.getInProgressQueVideosSingle()
    }


    override suspend fun getItemById(id: Long): InProgressVideoDB? {
        return dao.getItemById(id)
    }

    override suspend fun deleteFromQue(id: Long) {
        dao.deleteFromQue(id.toString())
    }


}