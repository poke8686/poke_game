package com.poke86.game.domain.repository

import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game

interface GameRepository {
    fun getGames(): List<Game>
    fun getCategories(): List<Category>
}
