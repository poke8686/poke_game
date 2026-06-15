package com.poke86.game.data.datasource.remote

import com.poke86.game.data.datasource.local.TDLocalDataSource
import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData
import com.poke86.game.network.TDApi
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TDRemoteDataSourceImpl @Inject constructor(
    private val api: TDApi,
    private val local: TDLocalDataSource,
) : TDRemoteDataSource {

    /** 저장된 userId를 가져오거나, 없으면 UUID 생성 후 서버 등록 및 로컬 저장. */
    private suspend fun requireUserId(): String {
        local.getUserId()?.let { return it }
        val newId = UUID.randomUUID().toString()
        api.register(newId).getOrThrow()
        local.saveUserId(newId)
        return newId
    }

    override suspend fun fetch(): TDSaveData? {
        val userId = requireUserId()
        return api.fetch(userId).getOrNull()
    }

    override suspend fun uploadAll(data: TDSaveData) {
        val userId = requireUserId()
        api.uploadAll(userId, data).getOrThrow()
    }

    override suspend fun uploadProgress(progress: TDProgressSave) {
        val userId = requireUserId()
        api.uploadProgress(userId, progress).getOrThrow()
    }

    override suspend fun uploadResources(resources: TDResourcesSave) {
        val userId = requireUserId()
        api.uploadResources(userId, resources).getOrThrow()
    }

    override suspend fun uploadCharacters(characters: List<TDCharacterSave>) {
        val userId = requireUserId()
        api.uploadCharacters(userId, characters).getOrThrow()
    }
}
