package com.poke86.game.ui.home

import app.cash.turbine.test
import com.poke86.game.data.repository.GameRepositoryImpl
import com.poke86.game.domain.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel
    private val repository: GameRepository = GameRepositoryImpl()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial selectedCategory is all`() = runTest {
        assertEquals("all", viewModel.selectedCategory.value)
    }

    @Test
    fun `categories returns 5 items`() = runTest {
        assertEquals(5, viewModel.categories.value.size)
    }

    @Test
    fun `filteredGames shows all 8 games when category is all`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.filteredGames.value.size)
    }

    @Test
    fun `selecting party filters to party games only`() = runTest {
        viewModel.onCategorySelected("party")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.filteredGames.test {
            val games = awaitItem()
            assertTrue(games.isNotEmpty())
            assertTrue(games.all { "party" in it.categories })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting solo filters to solo games only`() = runTest {
        viewModel.onCategorySelected("solo")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.filteredGames.test {
            val games = awaitItem()
            assertTrue(games.isNotEmpty())
            assertTrue(games.all { "solo" in it.categories })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching back to all shows all games`() = runTest {
        viewModel.onCategorySelected("party")
        viewModel.onCategorySelected("all")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.filteredGames.value.size)
    }
}
