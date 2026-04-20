package com.poke86.game.data.repository

import com.poke86.game.Routes
import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.model.GameTag
import com.poke86.game.domain.repository.GameRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor() : GameRepository {

    override fun getGames(): List<Game> = listOf(
        Game(
            id = "nunchigame",
            name = "눈치 게임",
            description = "번호 겹치면 탈락!",
            icon = "👁️",
            categories = listOf("party", "reflex"),
            tags = listOf(GameTag.MULTI),
            route = Routes.NUNCHIGAME
        ),
        Game(
            id = "defense",
            name = "풍선 디펜스",
            description = "올라오는 풍선을 탭·슬라이스해서 터뜨려라",
            icon = "🎈",
            categories = listOf("solo", "reflex"),
            tags = listOf(GameTag.SOLO, GameTag.QUICK),
            route = Routes.DEFENSE
        ),
        Game(
            id = "towerdefense",
            name = "타워 디펜스",
            description = "캐릭터를 배치해 몬스터를 막아라",
            icon = "🏰",
            categories = listOf("solo", "brain"),
            tags = listOf(GameTag.SOLO, GameTag.BRAIN),
            route = Routes.TOWER_DEFENSE
        )
    )

    override fun getCategories(): List<Category> = listOf(
        Category(id = "all", label = "전체"),
        Category(id = "party", label = "파티"),
        Category(id = "solo", label = "혼자"),
        Category(id = "reflex", label = "반응속도"),
        Category(id = "brain", label = "두뇌")
    )
}
