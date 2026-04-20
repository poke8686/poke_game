package com.poke86.game.data.repository

import com.poke86.game.data.datasource.local.TDLocalDataSource
import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData
import com.poke86.game.domain.repository.TDSaveRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 우선(Local-first) TDSaveRepository 구현.
 *
 * 서버 연동 시 변경 방법:
 *   1. TDRemoteDataSource를 주입합니다.
 *   2. 각 메서드에서 local 저장 완료 후 remote.upload*()를 비동기 호출합니다.
 *   3. load()에서 remote.fetch()와 local를 병합하는 전략을 추가합니다.
 *      (예: savedAt 비교 → 최신 데이터 우선)
 */
@Singleton
class TDSaveRepositoryImpl @Inject constructor(
    private val local: TDLocalDataSource,
    // private val remote: TDRemoteDataSource,  // 서버 연동 시 주입 해제
) : TDSaveRepository {

    override fun observe(): Flow<TDSaveData> = local.observe()

    override suspend fun load(): TDSaveData = local.load()
    // 서버 연동 시:
    // val remote = remote.fetch()
    // return if (remote != null && remote.savedAt > local.load().savedAt) remote else local.load()

    override suspend fun saveAll(data: TDSaveData) {
        val stamped = data.copy(savedAt = System.currentTimeMillis())
        local.saveAll(stamped)
        // TODO(server): remote.uploadAll(stamped)
    }

    override suspend fun saveProgress(progress: TDProgressSave) {
        local.saveProgress(progress)
        // TODO(server): remote.uploadProgress(progress)
    }

    override suspend fun saveResources(resources: TDResourcesSave) {
        local.saveResources(resources)
        // TODO(server): remote.uploadResources(resources)
    }

    override suspend fun saveCharacters(characters: List<TDCharacterSave>) {
        local.saveCharacters(characters)
        // TODO(server): remote.uploadCharacters(characters)
    }
}
