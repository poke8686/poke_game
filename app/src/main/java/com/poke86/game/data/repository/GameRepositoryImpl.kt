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
        ),
        Game(
            id = "one_to_fifty",
            name = "1 to 50",
            description = "1부터 50까지 빛의 속도로 터치!",
            icon = "🔢",
            categories = listOf("solo", "reflex"),
            tags = listOf(GameTag.SOLO, GameTag.QUICK),
            route = Routes.ONE_TO_FIFTY
        ),
        Game(
            id = "finger_roulette",
            name = "손가락 룰렛",
            description = "운명의 손가락 하나를 뽑아라",
            icon = "👆",
            categories = listOf("party"),
            tags = listOf(GameTag.MULTI, GameTag.QUICK),
            route = Routes.FINGER_ROULETTE
        ),
        Game(
            id = "bomb_pass",
            name = "시한폭탄",
            description = "터지기 전에 다음 사람에게!",
            icon = "💣",
            categories = listOf("party"),
            tags = listOf(GameTag.MULTI, GameTag.QUICK),
            route = Routes.BOMB_PASS
        ),
        Game(
            id = "mole_whack",
            name = "두더지잡기",
            description = "60초 안에 최대한 많이 잡아라!",
            icon = "🦫",
            categories = listOf("solo", "reflex"),
            tags = listOf(GameTag.SOLO, GameTag.QUICK),
            route = Routes.MOLE_WHACK
        ),
        Game(
            id = "spot_diff",
            name = "틀린그림찾기",
            description = "두 그림의 다른 점을 찾아내라",
            icon = "🔍",
            categories = listOf("solo", "brain"),
            tags = listOf(GameTag.SOLO, GameTag.BRAIN),
            route = Routes.SPOT_DIFF
        ),
        Game(
            id = "nightfall",
            name = "왕국 국경 디펜스",
            description = "낮엔 건설, 밤엔 방어! 국경을 지켜라",
            icon = "🌃",
            categories = listOf("solo", "brain"),
            tags = listOf(GameTag.SOLO, GameTag.BRAIN),
            route = Routes.NIGHTFALL
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
