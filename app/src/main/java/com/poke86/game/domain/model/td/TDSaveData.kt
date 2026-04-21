package com.poke86.game.domain.model.td

import kotlinx.serialization.Serializable

/**
 * 타워 디펜스 전체 저장 데이터.
 *
 * 서버 API 설계:
 *   GET  /users/{userId}/td/save          → 전체 로드
 *   PUT  /users/{userId}/td/save          → 전체 저장
 *
 * 각 섹션은 독립 PATCH 엔드포인트로 부분 업데이트 가능하도록 분리됩니다.
 */
@Serializable
data class TDSaveData(
    val progress: TDProgressSave = TDProgressSave(),
    val resources: TDResourcesSave = TDResourcesSave(),
    val characters: List<TDCharacterSave> = emptyList(),
    val schemaVersion: Int = 1,
    val savedAt: Long = 0L,
)

/**
 * 진행 상태 섹션.
 * 서버 API: PATCH /users/{userId}/td/save/progress
 */
@Serializable
data class TDProgressSave(
    val round: Int = 1,
    val unlockedRounds: Int = 1,
    val score: Int = 0,
)

/**
 * 자원 섹션 (골드·라이프).
 * 서버 API: PATCH /users/{userId}/td/save/resources
 */
@Serializable
data class TDResourcesSave(
    val gold: Int = 150,   // STARTING_GOLD
    val lives: Int = 20,   // STARTING_LIVES
)

/**
 * 배치된 캐릭터 한 명의 저장 데이터.
 * 서버 API: PATCH /users/{userId}/td/save/characters
 *
 * [type]은 서버 호환을 위해 TDCharType.name() 문자열로 저장합니다.
 * 클라이언트 enum 이름 변경 시 마이그레이션 필요.
 */
@Serializable
data class TDCharacterSave(
    val id: Int,
    val type: String,
    val col: Int,
    val row: Int,
    val level: Int = 1,
    val star: Int = 1,
    val evolutionGrade: String = "F",
    val baseAtkMult: Float = 1.0f,
    val totalInvested: Int = 0,
)
