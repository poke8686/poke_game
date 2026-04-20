package com.poke86.game.ui.games.nunchigame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NunchiPhase { SETUP, PLAYER_TURN, RESULTS, GAME_OVER }

data class NunchiUiState(
    val phase: NunchiPhase = NunchiPhase.SETUP,
    val playerCount: Int = 4,
    val activePlayers: List<Int> = emptyList(),
    val round: Int = 1,
    val currentTurnIdx: Int = 0,
    val selections: Map<Int, Int> = emptyMap(), // playerId -> chosen number
    val justSelected: Int? = null,
    val newlyEliminated: List<Int> = emptyList(),
    val winner: Int? = null
) {
    val currentPlayer: Int get() = activePlayers.getOrNull(currentTurnIdx) ?: 0
    val allSelected: Boolean get() = selections.size == activePlayers.size
}

class NunchiGameViewModel : ViewModel() {

    private val _state = MutableStateFlow(NunchiUiState())
    val state: StateFlow<NunchiUiState> = _state.asStateFlow()

    fun updatePlayerCount(count: Int) {
        _state.update { it.copy(playerCount = count.coerceIn(2, 8)) }
    }

    fun startGame() {
        val count = _state.value.playerCount
        _state.update {
            NunchiUiState(
                phase = NunchiPhase.PLAYER_TURN,
                playerCount = count,
                activePlayers = (1..count).toList()
            )
        }
    }

    fun selectNumber(number: Int) {
        val s = _state.value
        if (s.phase != NunchiPhase.PLAYER_TURN || s.justSelected != null) return

        val newSelections = s.selections + (s.currentPlayer to number)
        _state.update { it.copy(selections = newSelections, justSelected = number) }

        viewModelScope.launch {
            delay(900)
            val updated = _state.value
            if (updated.allSelected) {
                processResults()
            } else {
                _state.update { it.copy(currentTurnIdx = it.currentTurnIdx + 1, justSelected = null) }
            }
        }
    }

    private fun processResults() {
        val s = _state.value
        val numberToPlayers = s.selections.entries.groupBy({ it.value }, { it.key })
        val eliminated = numberToPlayers.filter { it.value.size > 1 }.flatMap { it.value }
        _state.update { it.copy(phase = NunchiPhase.RESULTS, newlyEliminated = eliminated, justSelected = null) }
    }

    fun nextRound() {
        val s = _state.value
        val remaining = s.activePlayers.filter { it !in s.newlyEliminated }
        if (remaining.size <= 1) {
            _state.update {
                it.copy(
                    phase = NunchiPhase.GAME_OVER,
                    winner = remaining.firstOrNull(),
                    activePlayers = remaining
                )
            }
        } else {
            _state.update {
                it.copy(
                    phase = NunchiPhase.PLAYER_TURN,
                    activePlayers = remaining,
                    round = s.round + 1,
                    currentTurnIdx = 0,
                    selections = emptyMap(),
                    newlyEliminated = emptyList()
                )
            }
        }
    }

    fun resetGame() {
        _state.update { NunchiUiState() }
    }
}
