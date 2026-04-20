package com.poke86.game.domain.model

data class Game(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val categories: List<String>,
    val tags: List<GameTag>,
    val route: String
)

enum class GameTag(val label: String) {
    MULTI("멀티"),
    SOLO("혼자"),
    QUICK("빠른판"),
    BRAIN("두뇌")
}
