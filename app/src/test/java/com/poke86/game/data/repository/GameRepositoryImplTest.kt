package com.poke86.game.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameRepositoryImplTest {

    private lateinit var repository: GameRepositoryImpl

    @Before
    fun setUp() {
        repository = GameRepositoryImpl()
    }

    @Test
    fun `getGames returns exactly 8 games`() {
        assertEquals(8, repository.getGames().size)
    }

    @Test
    fun `getCategories returns exactly 5 categories`() {
        assertEquals(5, repository.getCategories().size)
    }

    @Test
    fun `first category id is all`() {
        assertEquals("all", repository.getCategories().first().id)
    }

    @Test
    fun `all games have non-empty route`() {
        assertTrue(repository.getGames().all { it.route.isNotEmpty() })
    }

    @Test
    fun `all games have at least one tag`() {
        assertTrue(repository.getGames().all { it.tags.isNotEmpty() })
    }

    @Test
    fun `all games have at least one category`() {
        assertTrue(repository.getGames().all { it.categories.isNotEmpty() })
    }

    @Test
    fun `party games exist`() {
        assertFalse(repository.getGames().filter { "party" in it.categories }.isEmpty())
    }

    @Test
    fun `solo games exist`() {
        assertFalse(repository.getGames().filter { "solo" in it.categories }.isEmpty())
    }
}
