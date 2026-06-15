package com.poke86.game.data.repository

import com.poke86.game.data.datasource.local.TDLocalDataSource
import com.poke86.game.data.datasource.remote.TDRemoteDataSource
import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData
import com.poke86.game.domain.repository.TDSaveRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TDSaveRepositoryImpl @Inject constructor(
    private val local: TDLocalDataSource,
    private val remote: TDRemoteDataSource,
) : TDSaveRepository {

    override fun observe(): Flow<TDSaveData> = local.observe()

    /** 서버 데이터가 더 최신이면 서버 우선, 아니면 로컬 반환. */
    override suspend fun load(): TDSaveData {
        val localData = local.load()
        val remoteData = runCatching { remote.fetch() }.getOrNull()
        return if (remoteData != null && remoteData.savedAt > localData.savedAt) {
            local.saveAll(remoteData)
            remoteData
        } else {
            localData
        }
    }

    override suspend fun saveAll(data: TDSaveData) {
        val stamped = data.copy(savedAt = System.currentTimeMillis())
        local.saveAll(stamped)
        runCatching { remote.uploadAll(stamped) }
    }

    override suspend fun saveProgress(progress: TDProgressSave) {
        local.saveProgress(progress)
        runCatching { remote.uploadProgress(progress) }
    }

    override suspend fun saveResources(resources: TDResourcesSave) {
        local.saveResources(resources)
        runCatching { remote.uploadResources(resources) }
    }

    override suspend fun saveCharacters(characters: List<TDCharacterSave>) {
        local.saveCharacters(characters)
        runCatching { remote.uploadCharacters(characters) }
    }
}
