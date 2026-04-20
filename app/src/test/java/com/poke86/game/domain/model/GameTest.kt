package com.poke86.game.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GameTest {

    @Test
    fun `Game data class equality works correctly`() {
        val game1 = Game("id", "name", "desc", "🎮", listOf("party"), listOf(GameTag.MULTI), "route")
        val game2 = Game("id", "name", "desc", "🎮", listOf("party"), listOf(GameTag.MULTI), "route")
        assertEquals(game1, game2)
    }

    @Test
    fun `GameTag MULTI has correct label`() {
        assertEquals("멀티", GameTag.MULTI.label)
    }

    @Test
    fun `GameTag SOLO has correct label`() {
        assertEquals("혼자", GameTag.SOLO.label)
    }

    @Test
    fun `GameTag QUICK has correct label`() {
        assertEquals("빠른판", GameTag.QUICK.label)
    }

    @Test
    fun `GameTag BRAIN has correct label`() {
        assertEquals("두뇌", GameTag.BRAIN.label)
    }
}
