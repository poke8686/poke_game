package com.poke86.game.domain.repository

import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData
import kotlinx.coroutines.flow.Flow

/**
 * 타워 디펜스 저장 데이터 Repository 인터페이스.
 *
 * 현재 구현: 로컬 DataStore
 * 향후 구현: 로컬 우선(local-first) + 서버 동기화
 *
 * 섹션별 부분 저장 메서드는 서버 API PATCH 엔드포인트와 1:1 대응됩니다.
 */
interface TDSaveRepository {

    /** 저장 데이터를 Flow로 실시간 관찰 (DataStore → 향후 WebSocket/SSE 대응 가능). */
    fun observe(): Flow<TDSaveData>

    /** 저장 데이터 일회성 로드. 서버 API: GET /users/{id}/td/save */
    suspend fun load(): TDSaveData

    // ─── 전체 저장 ────────────────────────────────────────────────────────────

    /** 전체 저장. 서버 API: PUT /users/{id}/td/save */
    suspend fun saveAll(data: TDSaveData)

    // ─── 섹션별 부분 저장 ────────────────────────────────────────────────────

    /** 진행 상태(라운드·해금·점수)만 저장. 서버 API: PATCH /users/{id}/td/save/progress */
    suspend fun saveProgress(progress: TDProgressSave)

    /** 자원(골드·라이프)만 저장. 서버 API: PATCH /users/{id}/td/save/resources */
    suspend fun saveResources(resources: TDResourcesSave)

    /** 캐릭터 목록 저장. 서버 API: PATCH /users/{id}/td/save/characters */
    suspend fun saveCharacters(characters: List<TDCharacterSave>)
}
