package com.poke86.game.ui.games.nunchigame.multi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poke86.game.network.NunchiWsClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class MultiPhase { SETUP, CONNECTING, LOBBY, WAITING, PLAYING, RESULTS, GAME_OVER }

data class MultiPlayer(val id: String, val name: String, val isHost: Boolean = false)
data class MultiSelection(val playerId: String, val playerName: String, val number: Int?)
data class PublicRoom(val roomCode: String, val hostName: String, val playerCount: Int, val maxPlayers: Int = 8)

data class NunchiMultiUiState(
    val phase: MultiPhase = MultiPhase.SETUP,
    val playerName: String = "",
    val playerId: String = "",
    val roomCode: String = "",
    val roomCodeInput: String = "",
    val isHost: Boolean = false,
    val players: List<MultiPlayer> = emptyList(),
    val round: Int = 1,
    val activePlayers: List<MultiPlayer> = emptyList(),
    val timerRemaining: Int = 10,
    val timerTotal: Int = 10,
    val hasSelected: Boolean = false,
    val selectionCount: Int = 0,
    val takenNumbers: Map<Int, String> = emptyMap(), // number → playerName
    val roundSelections: List<MultiSelection> = emptyList(),
    val eliminated: List<MultiPlayer> = emptyList(),
    val remaining: List<MultiPlayer> = emptyList(),
    val winner: MultiPlayer? = null,
    val totalRounds: Int = 0,
    val publicRooms: List<PublicRoom> = emptyList(),
    val error: String? = null
)

class NunchiMultiViewModel : ViewModel() {

    private val wsClient = NunchiWsClient()
    private val _state = MutableStateFlow(NunchiMultiUiState())
    val state: StateFlow<NunchiMultiUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var connectTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            wsClient.events.collect { handleMessage(it) }
        }
        viewModelScope.launch {
            wsClient.connectionEvent.collect { connected ->
                if (connected && _state.value.phase == MultiPhase.CONNECTING) {
                    connectTimeoutJob?.cancel()
                    _state.update { it.copy(phase = MultiPhase.LOBBY) }
                } else if (!connected) {
                    when (_state.value.phase) {
                        MultiPhase.CONNECTING ->
                            _state.update { it.copy(phase = MultiPhase.SETUP, error = "연결 실패: ${wsClient.lastError}") }
                        MultiPhase.SETUP, MultiPhase.GAME_OVER -> Unit
                        else ->
                            _state.update { it.copy(error = "서버 연결이 끊겼습니다") }
                    }
                }
            }
        }
    }

    fun updatePlayerName(name: String) = _state.update { it.copy(playerName = name) }
    fun updateRoomCodeInput(code: String) = _state.update { it.copy(roomCodeInput = code.filter { c -> c.isDigit() }.take(4)) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun connect() {
        val name = _state.value.playerName.trim()
        if (name.isEmpty()) { _state.update { it.copy(error = "이름을 입력해주세요") }; return }
        _state.update { it.copy(phase = MultiPhase.CONNECTING) }
        wsClient.connect(viewModelScope)
        connectTimeoutJob = viewModelScope.launch {
            delay(10_000)
            if (_state.value.phase == MultiPhase.CONNECTING) {
                wsClient.disconnect()
                _state.update { it.copy(phase = MultiPhase.SETUP, error = "연결 시간이 초과되었습니다") }
            }
        }
    }

    fun createRoom() {
        wsClient.send("CREATE_ROOM", "playerName" to _state.value.playerName.trim())
    }

    fun joinRoom() {
        val code = _state.value.roomCodeInput
        if (code.length != 4) { _state.update { it.copy(error = "4자리 코드를 입력해주세요") }; return }
        wsClient.send("JOIN_ROOM", "playerName" to _state.value.playerName.trim(), "roomCode" to code)
    }

    fun startGame() = wsClient.send("START_GAME")

    fun selectNumber(number: Int) {
        wsClient.send("SELECT_NUMBER", "number" to number)
        _state.update { it.copy(hasSelected = true) }
    }

    fun nextRound() = wsClient.send("NEXT_ROUND")

    fun createPublicRoom() {
        wsClient.send("CREATE_ROOM", "playerName" to _state.value.playerName.trim(), "isPublic" to true)
    }

    fun getRooms() = wsClient.send("GET_ROOMS")

    fun joinPublicRoom(roomCode: String) {
        wsClient.send("JOIN_ROOM", "playerName" to _state.value.playerName.trim(), "roomCode" to roomCode)
    }

    fun leaveRoom() {
        timerJob?.cancel()
        wsClient.send("LEAVE_ROOM")
        val name = _state.value.playerName
        _state.value = NunchiMultiUiState(phase = MultiPhase.LOBBY, playerName = name)
        getRooms()
    }

    fun resetGame() {
        timerJob?.cancel()
        connectTimeoutJob?.cancel()
        wsClient.disconnect()
        _state.value = NunchiMultiUiState()
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _state.update { it.copy(timerRemaining = seconds, timerTotal = seconds) }
        timerJob = viewModelScope.launch {
            for (remaining in (seconds - 1) downTo 0) {
                delay(1000)
                _state.update { it.copy(timerRemaining = remaining) }
            }
        }
    }

    private fun handleMessage(msg: Map<String, Any?>) {
        when (msg.str("type")) {
            "ROOM_CREATED" -> {
                val code = msg.str("roomCode") ?: return
                val playerId = msg.str("playerId") ?: return
                _state.update { it.copy(
                    phase = MultiPhase.WAITING,
                    roomCode = code,
                    playerId = playerId,
                    isHost = true,
                    players = parsePlayers(msg["players"])
                )}
            }
            "ROOM_JOINED" -> {
                _state.update { it.copy(
                    phase = MultiPhase.WAITING,
                    roomCode = msg.str("roomCode") ?: "",
                    playerId = msg.str("playerId") ?: "",
                    isHost = false,
                    players = parsePlayers(msg["players"])
                )}
            }
            "PLAYER_JOINED" -> _state.update { it.copy(players = parsePlayers(msg["players"])) }
            "PLAYER_LEFT"   -> _state.update { it.copy(players = parsePlayers(msg["players"])) }
            "GAME_STARTED"  -> {
                val players = parsePlayers(msg["players"])
                _state.update { it.copy(phase = MultiPhase.PLAYING, activePlayers = players, round = 1, hasSelected = false, takenNumbers = emptyMap(), selectionCount = 0) }
                startTimer(msg.int("timerSeconds") ?: 10)
            }
            "ROUND_START" -> {
                val active = parseSimplePlayers(msg["activePlayers"])
                val timer = msg.int("timerSeconds") ?: 10
                _state.update { it.copy(
                    phase = MultiPhase.PLAYING,
                    round = msg.int("round") ?: _state.value.round,
                    activePlayers = active,
                    hasSelected = false,
                    selectionCount = 0,
                    takenNumbers = emptyMap()
                )}
                startTimer(timer)
            }
            "NUMBER_TAKEN" -> {
                val number = msg.int("number") ?: return
                val playerName = msg.str("playerName") ?: ""
                val selectionCount = msg.int("selectionCount") ?: 0
                val isMe = msg.str("playerId") == _state.value.playerId
                _state.update { it.copy(
                    takenNumbers = it.takenNumbers + (number to playerName),
                    selectionCount = selectionCount,
                    hasSelected = it.hasSelected || isMe
                )}
                startTimer(1)  // switch to 1s reaction timer after each press
            }
            "SELECTION_PROGRESS" -> _state.update { it.copy(selectionCount = msg.int("selected") ?: 0) }
            "ROUND_RESULT" -> {
                timerJob?.cancel()
                _state.update { it.copy(
                    phase = MultiPhase.RESULTS,
                    roundSelections = parseSelections(msg["selections"]),
                    eliminated = parseSimplePlayers(msg["eliminated"]),
                    remaining = parseSimplePlayers(msg["remaining"])
                )}
            }
            "GAME_OVER" -> {
                timerJob?.cancel()
                _state.update { it.copy(
                    phase = MultiPhase.GAME_OVER,
                    winner = parseWinner(msg["winner"]),
                    totalRounds = msg.int("totalRounds") ?: _state.value.round
                )}
            }
            "ROOM_LIST" -> {
                val arr = msg["rooms"] as? JSONArray ?: return
                val rooms = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    PublicRoom(
                        roomCode = o.optString("roomCode"),
                        hostName = o.optString("hostName"),
                        playerCount = o.optInt("playerCount"),
                        maxPlayers = o.optInt("maxPlayers", 8)
                    )
                }
                _state.update { it.copy(publicRooms = rooms) }
            }
            "ERROR" -> _state.update { it.copy(error = msg.str("message") ?: "오류가 발생했습니다") }
        }
    }

    private fun parsePlayers(raw: Any?): List<MultiPlayer> {
        val arr = raw as? JSONArray ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MultiPlayer(o.optString("id"), o.optString("name"), o.optBoolean("isHost"))
        }
    }

    private fun parseSimplePlayers(raw: Any?): List<MultiPlayer> {
        val arr = raw as? JSONArray ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MultiPlayer(o.optString("id"), o.optString("name"))
        }
    }

    private fun parseSelections(raw: Any?): List<MultiSelection> {
        val arr = raw as? JSONArray ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MultiSelection(
                playerId = o.optString("playerId"),
                playerName = o.optString("playerName"),
                number = if (o.isNull("number")) null else o.optInt("number")
            )
        }
    }

    private fun parseWinner(raw: Any?): MultiPlayer? {
        val o = raw as? org.json.JSONObject ?: return null
        return MultiPlayer(o.optString("playerId"), o.optString("playerName"))
    }

    private fun Map<String, Any?>.str(key: String) = this[key] as? String
    private fun Map<String, Any?>.int(key: String) = (this[key] as? Number)?.toInt()

    override fun onCleared() {
        wsClient.disconnect()
        super.onCleared()
    }
}
