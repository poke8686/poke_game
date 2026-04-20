package com.poke86.game.data.datasource.remote

import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData

/**
 * 서버 API 통신 인터페이스 (추후 구현).
 *
 * 예정 엔드포인트:
 *   GET  /users/{userId}/td/save                → [fetch]
 *   PUT  /users/{userId}/td/save                → [uploadAll]
 *   PATCH /users/{userId}/td/save/progress      → [uploadProgress]
 *   PATCH /users/{userId}/td/save/resources     → [uploadResources]
 *   PATCH /users/{userId}/td/save/characters    → [uploadCharacters]
 *
 * 구현 시 OkHttp + kotlinx.serialization 으로 작성합니다.
 * Retrofit 또는 Ktor Client 사용을 권장합니다.
 */
interface TDRemoteDataSource {
    /** 서버에서 전체 저장 데이터를 가져옵니다. 없으면 null 반환. */
    suspend fun fetch(): TDSaveData?

    /** 전체 저장 데이터를 서버에 업로드합니다. */
    suspend fun uploadAll(data: TDSaveData)

    /** 진행 상태만 부분 업데이트합니다. */
    suspend fun uploadProgress(progress: TDProgressSave)

    /** 자원(골드·라이프)만 부분 업데이트합니다. */
    suspend fun uploadResources(resources: TDResourcesSave)

    /** 캐릭터 목록을 부분 업데이트합니다. */
    suspend fun uploadCharacters(characters: List<TDCharacterSave>)
}
