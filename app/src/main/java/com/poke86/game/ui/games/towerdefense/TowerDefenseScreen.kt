package com.poke86.game.ui.games.towerdefense

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.repository.TDSaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Path

// ─── Constants & Map ─────────────────────────────────────────────────────────

private const val GRID_COLS = 7
private const val GRID_ROWS = 9
private const val STARTING_LIVES = 20
private const val STARTING_GOLD = 150

// S-curve path: entry top-left, exit bottom-right
private val TD_PATH: List<Pair<Int, Int>> = listOf(
    1 to 0, 1 to 1, 1 to 2, 1 to 3,
    2 to 3, 3 to 3, 4 to 3, 5 to 3,
    5 to 4, 5 to 5, 4 to 5, 3 to 5,
    2 to 5, 1 to 5, 1 to 6, 1 to 7,
    2 to 7, 3 to 7, 4 to 7, 5 to 7,
    5 to 8
)
private val PATH_CELLS: Set<Pair<Int, Int>> = TD_PATH.toHashSet()

private val THEME_COLORS = listOf(
    Color(0xFF388E3C.toInt()), Color(0xFF546E7A.toInt()), Color(0xFFE65100.toInt()),
    Color(0xFF1565C0.toInt()), Color(0xFFBF360C.toInt()), Color(0xFF4A148C.toInt()),
    Color(0xFF37474F.toInt()), Color(0xFF2E7D32.toInt()), Color(0xFF1A237E.toInt()),
    Color(0xFF880E4F.toInt())
)
private val THEME_NAMES = listOf("숲", "광산", "사막", "설원", "용암", "묘지", "기계도시", "늪", "폭풍", "심연")
private val BOSS_NAMES = listOf(
    "나무정령", "강철골렘", "모래벌레", "서리거인", "화염드래곤",
    "리치킹", "파괴병기", "독히드라", "폭풍지배자", "최종마왕"
)

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class TDPhase { ROUND_SELECT, PREP, PLAYING, ROUND_END, GAME_OVER }

enum class TDEvolution(
    val label: String,
    val reqStar: Int,
    val reqLevel: Int,
    val cost: Int
) {
    F("F", 1, 10, 0),
    E("E", 1, 10, 500),
    D("D", 2, 20, 1200),
    C("C", 3, 30, 2500),
    B("B", 4, 40, 5000),
    A("A", 5, 50, 12000),
    S("S", 5, 50, 25000),
    S_PLUS("S+", 5, 50, 60000);

    fun next(): TDEvolution? = when (this) {
        F -> E
        E -> D
        D -> C
        C -> B
        B -> A
        A -> S
        S -> S_PLUS
        S_PLUS -> null
    }
}

enum class TDEffectKind { HIT, SPLASH, PROJECTILE, LASER }

enum class TDCharType(
    val label: String,
    val emoji: String,
    val baseAtk: Int,
    val baseRange: Float,
    val atkMs: Long,
    val charColor: Int,
    val isPhysical: Boolean,
) {
    SNIPER("저격수", "🎯", 50, 3.5f, 1500L, 0xFFF0E68C.toInt(), true),
    ARTILLERY("포병", "💣", 35, 2.2f, 2000L, 0xFFFF6600.toInt(), true),
    DUAL_PISTOLS("쌍권총", "🔫", 12, 1.8f, 350L, 0xFF9C27B0.toInt(), true),
    POISONER("독술사", "🧪", 8, 1.8f, 1200L, 0xFF4CAF50.toInt(), false),
    LASER_TURRET("레이저", "⚡", 22, 3.0f, 900L, 0xFF00BCD4.toInt(), false),
    CRYOMANCER("빙결사", "❄️", 10, 2.0f, 1000L, 0xFF90CAF9.toInt(), false),
    CORRUPTOR("부식술사", "☣️", 15, 1.8f, 1200L, 0xFFFF8C00.toInt(), false),
    REAPER("사신", "💀", 0, 1.8f, 3000L, 0xFF7B1FA2.toInt(), true),
    COMMANDER("지휘관", "🚩", 0, 2.5f, 0L, 0xFFFFD700.toInt(), false),
    MERCHANT("상인", "🪙", 5, 1.5f, 900L, 0xFFFFF176.toInt(), true),
}

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class TDMonster(
    val id: Int,
    val hp: Int,
    val maxHp: Int,
    val armor: Int,
    val baseSpeed: Float,
    val waypointIdx: Int = 0,
    val progress: Float = 0f,
    val isBoss: Boolean = false,
    val bossRound: Int = 0,
    val isSplitChild: Boolean = false,
    val poisonStacks: Int = 0,
    val poisonTickAt: Long = 0L,
    val slowUntil: Long = 0L,
    val armorBreak: Float = 0f,
    val armorBreakUntil: Long = 0L,
    val phase2Active: Boolean = false,
    val lastRegenAt: Long = 0L,
) {
    val gridCol: Float
        get() {
            if (waypointIdx >= TD_PATH.size - 1) return TD_PATH.last().first.toFloat()
            val c1 = TD_PATH[waypointIdx].first
            val c2 = TD_PATH[waypointIdx + 1].first
            return c1 + (c2 - c1) * progress
        }
    val gridRow: Float
        get() {
            if (waypointIdx >= TD_PATH.size - 1) return TD_PATH.last().second.toFloat()
            val r1 = TD_PATH[waypointIdx].second
            val r2 = TD_PATH[waypointIdx + 1].second
            return r1 + (r2 - r1) * progress
        }
    val effectiveArmor: Int
        get() = max(0, (armor * (1f - armorBreak)).toInt())
    val isSlowed: Boolean
        get() = System.currentTimeMillis() < slowUntil
    val currentSpeed: Float
        get() = when {
            phase2Active && bossRound == 70 -> baseSpeed * 2f
            isSlowed && bossRound != 90 && bossRound != 100 -> baseSpeed * 0.5f
            else -> baseSpeed
        }
}

data class TDCharacter(
    val id: Int,
    val type: TDCharType,
    val col: Int,
    val row: Int,
    val level: Int = 1,
    val star: Int = 1,
    val evolution: TDEvolution = TDEvolution.F,
    val totalInvested: Int,
    val lastAtkAt: Long = 0L,
    val precisionHits: Int = 0,
    val rapidFireHits: Int = 0,
    val rapidFireUntil: Long = 0L,
    val laserHeat: Float = 0f,
)

data class TDEffect(
    val id: Int,
    val kind: TDEffectKind = TDEffectKind.HIT,
    val fromCol: Float = 0f,
    val fromRow: Float = 0f,
    val col: Float = 0f,
    val row: Float = 0f,
    val progress: Float = 0f,
    val argb: Int = 0xFFFFFFFF.toInt(),
    val splashRadius: Float = 0f,
    val charType: TDCharType? = null,
)

data class TDRoundResult(
    val kills: Int, val escaped: Int,
    val goldEarned: Int, val interest: Int, val clearBonus: Int,
)

data class TDUiState(
    val phase: TDPhase = TDPhase.ROUND_SELECT,
    val round: Int = 1,
    val lives: Int = STARTING_LIVES,
    val gold: Int = STARTING_GOLD,
    val unlockedRounds: Int = 1,
    val score: Int = 0,
    val monsters: List<TDMonster> = emptyList(),
    val characters: List<TDCharacter> = emptyList(),
    val effects: List<TDEffect> = emptyList(),
    val selectedCell: Pair<Int, Int>? = null,
    val manualTargetId: Int? = null,
    val isMovingChar: Boolean = false,
    val spawningCount: Int = 0,
    val totalMonsters: Int = 0,
    val killedCount: Int = 0,
    val escapedCount: Int = 0,
    val roundGoldEarned: Int = 0,
    val lastResult: TDRoundResult? = null,
)

// ─── Balance Functions ────────────────────────────────────────────────────────

private fun monsterHp(round: Int): Int =
    (100.0 * Math.pow(1.15, (round - 1).toDouble())).toInt()

private fun bossHp(round: Int): Int =
    if (round >= 100) monsterHp(round) * 100 else monsterHp(round) * 30

private fun monsterArmor(round: Int): Int = (round * 1.5).toInt()
private fun monsterCount(round: Int): Int = min(15 + (round - 1) / 10 * 5, 60)
private fun monsterBaseSpeed(round: Int): Float = 0.7f + round * 0.004f
private fun killGold(round: Int, isBoss: Boolean): Int =
    if (isBoss) round * 10 else 1 + round / 5
private fun clearBonus(round: Int): Int = round * 5
private fun interest(gold: Int): Int = min((gold * 0.05).toInt(), 50)
private fun buyGold(placed: Int): Int =
    (50.0 * Math.pow(1.2, placed.toDouble())).toInt()
private fun levelUpGold(targetLevel: Int, star: Int): Int =
    Math.round(targetLevel * 10.0 * Math.pow(1.1, (star - 1).toDouble())).toInt()
private fun starUpGold(targetStar: Int): Int = targetStar * 200
private const val MAX_STAR = 5
private fun charBaseAtk(c: TDCharacter): Int =
    (c.type.baseAtk * (1.0 + (c.level - 1) * 0.08 + (c.star - 1) * 0.15)).toInt()
private fun charRange(c: TDCharacter): Float = c.type.baseRange + (c.star - 1) * 0.1f
private fun charAtkMs(c: TDCharacter): Long {
    var ms = c.type.atkMs
    if (c.type == TDCharType.DUAL_PISTOLS && System.currentTimeMillis() < c.rapidFireUntil)
        ms = (ms * 0.7f).toLong()
    return ms
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1; val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}

private fun calcDamage(atk: Int, armor: Int): Int =
    max(1, (atk * (100.0 / (100.0 + armor))).toInt())

private fun pointToSegDist(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1; val dy = y2 - y1
    val len2 = dx * dx + dy * dy
    if (len2 < 1e-9f) return dist(px, py, x1, y1)
    val t = ((px - x1) * dx + (py - y1) * dy) / len2
    return dist(px, py, x1 + t.coerceIn(0f, 1f) * dx, y1 + t.coerceIn(0f, 1f) * dy)
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class TowerDefenseViewModel @Inject constructor(
    private val saveRepository: TDSaveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TDUiState())
    val state: StateFlow<TDUiState> = _state.asStateFlow()

    private var gameJob: Job? = null
    private var spawnJob: Job? = null
    private var nextId = 0
    private val spawnQueue = ArrayDeque<TDMonster>()

    init {
        viewModelScope.launch {
            val saved = saveRepository.load()
            val chars = saved.characters.mapNotNull { s ->
                runCatching {
                    TDCharacter(
                        id = s.id, type = TDCharType.valueOf(s.type),
                        col = s.col, row = s.row,
                        level = s.level, star = s.star,
                        evolution = runCatching { TDEvolution.valueOf(s.evolutionGrade) }.getOrDefault(TDEvolution.F),
                        totalInvested = s.totalInvested
                    )
                }.getOrNull()
            }
            nextId = (chars.maxOfOrNull { it.id } ?: -1) + 1
            _state.update {
                it.copy(
                    round = saved.progress.round,
                    unlockedRounds = saved.progress.unlockedRounds,
                    gold = saved.resources.gold,
                    lives = saved.resources.lives,
                    characters = chars,
                )
            }
        }
    }

    // ─ Public ────────────────────────────────────────────────────────────────

    fun selectRound(round: Int) {
        if (round > _state.value.unlockedRounds) return
        gameJob?.cancel(); spawnJob?.cancel()
        _state.update {
            it.copy(
                phase = TDPhase.PREP, round = round,
                monsters = emptyList(), effects = emptyList(),
                selectedCell = null, manualTargetId = null, lastResult = null,
                spawningCount = 0, totalMonsters = 0, killedCount = 0, escapedCount = 0, roundGoldEarned = 0
            )
        }
    }

    fun startRound() {
        val round = _state.value.round
        val isBoss = round % 10 == 0
        val count = if (isBoss) 1 else monsterCount(round)
        spawnQueue.clear()
        spawnQueue.addAll(buildMonsters(round, count))
        _state.update {
            it.copy(
                phase = TDPhase.PLAYING, monsters = emptyList(), effects = emptyList(),
                selectedCell = null, manualTargetId = null, spawningCount = count,
                totalMonsters = count, killedCount = 0, escapedCount = 0, roundGoldEarned = 0
            )
        }
        startLoop(); startSpawning()
    }

    fun tapCell(col: Int, row: Int) {
        if (col !in 0 until GRID_COLS || row !in 0 until GRID_ROWS) return
        if (PATH_CELLS.contains(col to row)) { clearSelection(); return }
        val st = _state.value
        if (st.isMovingChar) {
            val sel = st.selectedCell
            if (sel != null && st.characters.none { it.col == col && it.row == row }) {
                moveCharacter(sel.first, sel.second, col, row)
            } else {
                _state.update { it.copy(isMovingChar = false) }
            }
            return
        }
        _state.update { it.copy(selectedCell = col to row) }
    }

    fun clearSelection() = _state.update { it.copy(selectedCell = null, isMovingChar = false) }

    fun startMoveMode() = _state.update { it.copy(isMovingChar = true) }

    fun buyCharacter(type: TDCharType) {
        val st = _state.value
        val cell = st.selectedCell ?: return
        if (PATH_CELLS.contains(cell)) return
        if (st.characters.any { it.col == cell.first && it.row == cell.second }) return
        val cost = buyGold(st.characters.size)
        if (st.gold < cost) return
        _state.update {
            it.copy(
                gold = it.gold - cost,
                characters = it.characters + TDCharacter(
                    id = nextId++, type = type,
                    col = cell.first, row = cell.second, totalInvested = cost
                ),
                selectedCell = cell
            )
        }
        saveResources(); saveCharacters()
    }

    fun sellCharacter() {
        val st = _state.value
        val cell = st.selectedCell ?: return
        val char = st.characters.find { it.col == cell.first && it.row == cell.second } ?: return
        val refund = (char.totalInvested * 0.7).toInt()
        _state.update {
            it.copy(
                gold = it.gold + refund,
                characters = it.characters.filter { c -> c.id != char.id },
                selectedCell = null
            )
        }
        saveResources(); saveCharacters()
    }

    fun levelUpCharacter() {
        val st = _state.value
        val cell = st.selectedCell ?: return
        val char = st.characters.find { it.col == cell.first && it.row == cell.second } ?: return
        if (char.level >= char.star * 10) return
        val cost = levelUpGold(char.level + 1, char.star)
        if (st.gold < cost) return
        _state.update {
            it.copy(
                gold = it.gold - cost,
                characters = it.characters.map { c ->
                    if (c.id == char.id) c.copy(level = c.level + 1, totalInvested = c.totalInvested + cost) else c
                }
            )
        }
        saveResources(); saveCharacters()
    }

    fun starUpCharacter() {
        val st = _state.value
        val cell = st.selectedCell ?: return
        val char = st.characters.find { it.col == cell.first && it.row == cell.second } ?: return
        if (char.level < char.star * 10) return   // 최대 레벨 아님
        if (char.star >= MAX_STAR) return
        val cost = starUpGold(char.star + 1)
        if (st.gold < cost) return
        _state.update {
            it.copy(
                gold = it.gold - cost,
                characters = it.characters.map { c ->
                    if (c.id == char.id) c.copy(star = c.star + 1, totalInvested = c.totalInvested + cost) else c
                }
            )
        }
        saveResources(); saveCharacters()
    }

    fun evolveCharacter() {
        val st = _state.value
        val cell = st.selectedCell ?: return
        val char = st.characters.find { it.col == cell.first && it.row == cell.second } ?: return
        val next = char.evolution.next() ?: return
        if (char.star < next.reqStar || char.level < next.reqLevel) return
        if (st.gold < next.cost) return
        _state.update {
            it.copy(
                gold = it.gold - next.cost,
                characters = it.characters.map { c ->
                    if (c.id == char.id) c.copy(evolution = next, totalInvested = c.totalInvested + next.cost) else c
                }
            )
        }
        saveResources(); saveCharacters()
    }

    fun tapMonster(id: Int) =
        _state.update { it.copy(manualTargetId = if (it.manualTargetId == id) null else id) }

    fun startNextRound() {
        gameJob?.cancel(); spawnJob?.cancel(); spawnQueue.clear()
        val next = (_state.value.round + 1).coerceAtMost(100)
        _state.update {
            it.copy(
                phase = TDPhase.PREP, round = next, monsters = emptyList(), effects = emptyList(),
                selectedCell = null, manualTargetId = null, lastResult = null,
                spawningCount = 0, totalMonsters = 0, killedCount = 0, escapedCount = 0, roundGoldEarned = 0
            )
        }
    }

    fun replayRound() {
        gameJob?.cancel(); spawnJob?.cancel(); spawnQueue.clear()
        _state.update {
            it.copy(
                phase = TDPhase.PREP, monsters = emptyList(), effects = emptyList(),
                selectedCell = null, manualTargetId = null, lastResult = null,
                spawningCount = 0, totalMonsters = 0, killedCount = 0, escapedCount = 0, roundGoldEarned = 0
            )
        }
    }

    fun watchAd() {
        _state.update { it.copy(gold = it.gold + 100) }
        saveResources()
    }

    fun goToRoundSelect() {
        gameJob?.cancel(); spawnJob?.cancel(); spawnQueue.clear()
        _state.update {
            it.copy(
                phase = TDPhase.ROUND_SELECT,
                monsters = emptyList(), effects = emptyList(),
                selectedCell = null, manualTargetId = null
            )
        }
    }

    fun moveCharacter(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) {
        if (PATH_CELLS.contains(toCol to toRow)) return
        val st = _state.value
        val char = st.characters.find { it.col == fromCol && it.row == fromRow } ?: return
        if (st.characters.any { it.col == toCol && it.row == toRow }) return
        _state.update {
            it.copy(
                characters = it.characters.map { c ->
                    if (c.id == char.id) c.copy(col = toCol, row = toRow) else c
                },
                selectedCell = toCol to toRow,
                isMovingChar = false
            )
        }
        saveCharacters()
    }

    // ─ Private ───────────────────────────────────────────────────────────────

    private fun buildMonsters(round: Int, count: Int): List<TDMonster> {
        val isBoss = round % 10 == 0
        val hp = if (isBoss) bossHp(round) else monsterHp(round)
        var armor = monsterArmor(round)
        var speed = monsterBaseSpeed(round)
        if (isBoss) when (round) {
            20 -> armor += 20
            30 -> speed *= 1.5f
            10, 40, 50, 60, 70, 80, 90, 100 -> speed *= 0.7f // bosses are slower but tankier
        }
        return (0 until count).map {
            TDMonster(
                id = nextId++, hp = hp, maxHp = hp, armor = armor,
                baseSpeed = speed, isBoss = isBoss, bossRound = if (isBoss) round else 0
            )
        }
    }

    private fun startSpawning() {
        spawnJob = viewModelScope.launch {
            while (spawnQueue.isNotEmpty()) {
                val m = spawnQueue.removeFirst()
                _state.update { it.copy(monsters = it.monsters + m, spawningCount = it.spawningCount - 1) }
                if (!m.isBoss && spawnQueue.isNotEmpty()) delay(1000L)
            }
        }
    }

    private fun startLoop() {
        gameJob = viewModelScope.launch {
            while (true) {
                delay(16L)
                if (_state.value.phase == TDPhase.PLAYING) tick()
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        var st = _state.value

        st = moveMonsters(st, now)

        val escaped = st.monsters.filter { it.waypointIdx >= TD_PATH.size - 1 }
        if (escaped.isNotEmpty()) {
            val newLives = (st.lives - escaped.size).coerceAtLeast(0)
            st = st.copy(
                lives = newLives,
                monsters = st.monsters.filter { it.waypointIdx < TD_PATH.size - 1 },
                escapedCount = st.escapedCount + escaped.size
            )
            if (newLives <= 0) {
                gameJob?.cancel(); spawnJob?.cancel()
                _state.update { it.copy(phase = TDPhase.GAME_OVER, monsters = emptyList()) }
                return
            }
        }

        st = applyPoison(st, now)
        st = applyBossRegen(st, now)
        st = checkMechaPhase2(st)
        st = processAttacks(st, now)

        val effects = st.effects
            .map { e -> e.copy(progress = e.progress + when (e.kind) {
                TDEffectKind.PROJECTILE -> 0.13f
                TDEffectKind.LASER     -> 0.18f
                TDEffectKind.HIT       -> 0.10f
                TDEffectKind.SPLASH    -> 0.07f
            }) }
            .filter { it.progress < 1f }
        st = st.copy(effects = effects)

        if (st.monsters.isEmpty() && spawnQueue.isEmpty() && st.spawningCount <= 0) {
            endRound(st); return
        }
        _state.value = st
    }

    private fun moveMonsters(st: TDUiState, now: Long): TDUiState {
        val moved = st.monsters.map { m ->
            val delta = m.currentSpeed * 16f / 1000f
            var idx = m.waypointIdx
            var prog = m.progress + delta
            while (prog >= 1f && idx < TD_PATH.size - 1) { prog -= 1f; idx++ }
            if (idx >= TD_PATH.size - 1) prog = 1f
            m.copy(waypointIdx = idx, progress = prog)
        }
        return st.copy(monsters = moved)
    }

    private fun applyPoison(st: TDUiState, now: Long): TDUiState {
        val poisoned = st.monsters.filter { it.poisonStacks > 0 && now >= it.poisonTickAt }
        if (poisoned.isEmpty()) return st
        var cur = st; var extra = 0
        for (m in poisoned) {
            val target = cur.monsters.find { it.id == m.id } ?: continue
            val newHp = target.hp - target.poisonStacks * 3
            if (newHp <= 0) {
                val g = killGold(cur.round, target.isBoss)
                extra += g
                cur = cur.copy(
                    monsters = cur.monsters.filter { it.id != target.id },
                    killedCount = cur.killedCount + 1, score = cur.score + g * 10,
                    effects = cur.effects + TDEffect(nextId++, col = target.gridCol, row = target.gridRow, argb = 0xFF4CAF50.toInt())
                )
            } else {
                cur = cur.copy(monsters = cur.monsters.map {
                    if (it.id == target.id) it.copy(hp = newHp, poisonTickAt = now + 500L) else it
                })
            }
        }
        if (extra > 0) cur = cur.copy(gold = cur.gold + extra, roundGoldEarned = cur.roundGoldEarned + extra)
        return cur
    }

    private fun applyBossRegen(st: TDUiState, now: Long): TDUiState {
        val needRegen = st.monsters.any { it.bossRound == 50 && now - it.lastRegenAt >= 1000L }
        if (!needRegen) return st
        return st.copy(monsters = st.monsters.map { m ->
            if (m.bossRound == 50 && now - m.lastRegenAt >= 1000L)
                m.copy(hp = min(m.hp + (m.maxHp * 0.01).toInt().coerceAtLeast(1), m.maxHp), lastRegenAt = now)
            else m
        })
    }

    private fun checkMechaPhase2(st: TDUiState): TDUiState {
        val needs = st.monsters.any { it.bossRound == 70 && !it.phase2Active && it.hp < it.maxHp * 0.5f }
        if (!needs) return st
        return st.copy(monsters = st.monsters.map { m ->
            if (m.bossRound == 70 && !m.phase2Active && m.hp < m.maxHp * 0.5f) m.copy(phase2Active = true) else m
        })
    }

    private fun processAttacks(st: TDUiState, now: Long): TDUiState {
        var cur = st
        for (orig in st.characters) {
            val char = cur.characters.find { it.id == orig.id } ?: continue
            if (char.type == TDCharType.COMMANDER) continue
            val ms = charAtkMs(char)
            if (ms == 0L || now - char.lastAtkAt < ms) continue

            val range = charRange(char)
            val cmdBonus = commanderBonus(char, cur.characters)
            val cx = char.col.toFloat(); val cy = char.row.toFloat()
            val manual = cur.manualTargetId?.let { id -> cur.monsters.find { it.id == id } }

            val target = if (manual != null && dist(cx, cy, manual.gridCol, manual.gridRow) <= range) manual
            else cur.monsters
                .filter { m -> dist(cx, cy, m.gridCol, m.gridRow) <= range }
                .maxByOrNull { m -> m.waypointIdx * 1000 + (m.progress * 1000).toInt() }
                ?: continue

            val baseDmg = max(1, (charBaseAtk(char) * (1f + cmdBonus)).toInt())
            cur = applyAttack(cur, char, target, baseDmg, now)
            cur = cur.copy(characters = cur.characters.map { c ->
                if (c.id == char.id) c.copy(lastAtkAt = now) else c
            })
        }
        return cur
    }

    private fun applyAttack(st: TDUiState, char: TDCharacter, target: TDMonster, baseDmg: Int, now: Long): TDUiState {
        val physMult = if (target.bossRound == 60 && char.type.isPhysical) 0.1f else 1f
        val eff = max(1, (baseDmg * physMult).toInt())

        // Spawn attack visual (projectile or laser)
        val charCx = char.col + 0.5f; val charCy = char.row + 0.5f
        val attackEffect: TDEffect? = when (char.type) {
            TDCharType.LASER_TURRET -> TDEffect(
                nextId++, kind = TDEffectKind.LASER,
                fromCol = charCx, fromRow = charCy, col = target.gridCol, row = target.gridRow,
                argb = 0xFF00E5FF.toInt()
            )
            TDCharType.COMMANDER, TDCharType.ARTILLERY -> null
            else -> TDEffect(
                nextId++, kind = TDEffectKind.PROJECTILE,
                fromCol = charCx, fromRow = charCy, col = target.gridCol, row = target.gridRow,
                argb = char.type.charColor, charType = char.type
            )
        }
        val base = if (attackEffect != null) st.copy(effects = st.effects + attackEffect) else st

        return when (char.type) {
            TDCharType.SNIPER -> {
                val isPrec = char.precisionHits >= 2
                val dmg = calcDamage(if (isPrec) eff * 2 else eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFFF0E68C.toInt())
                ns = ns.copy(characters = ns.characters.map { c ->
                    if (c.id == char.id) c.copy(precisionHits = (c.precisionHits + 1) % 3) else c
                })
                ns
            }
            TDCharType.ARTILLERY -> {
                val splash = st.monsters.filter { m ->
                    dist(target.gridCol, target.gridRow, m.gridCol, m.gridRow) <= 1.2f
                }
                var ns = base.copy(effects = base.effects + TDEffect(
                    nextId++, kind = TDEffectKind.SPLASH,
                    col = target.gridCol, row = target.gridRow,
                    argb = 0xFFFF6600.toInt(), splashRadius = 1.2f
                ))
                for (t in splash) ns = dealDamage(ns, t.id, calcDamage(eff, t.effectiveArmor), now, 0xFFFF4400.toInt())
                ns
            }
            TDCharType.DUAL_PISTOLS -> {
                val dmg = calcDamage(eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFF9C27B0.toInt())
                val newHits = char.rapidFireHits + 1
                ns = ns.copy(characters = ns.characters.map { c ->
                    if (c.id == char.id) {
                        if (newHits >= 10) c.copy(rapidFireHits = 0, rapidFireUntil = now + 3000L)
                        else c.copy(rapidFireHits = newHits)
                    } else c
                })
                ns
            }
            TDCharType.POISONER -> {
                var ns = base.copy(monsters = base.monsters.map { m ->
                    if (m.id == target.id) {
                        val stacks = min(m.poisonStacks + 1, 3)
                        m.copy(poisonStacks = stacks, poisonTickAt = if (m.poisonStacks == 0) now + 500L else m.poisonTickAt)
                    } else m
                })
                ns.copy(effects = ns.effects + TDEffect(nextId++, col = target.gridCol, row = target.gridRow, argb = 0xFF4CAF50.toInt()))
            }
            TDCharType.LASER_TURRET -> {
                val lcx = char.col.toFloat(); val lcy = char.row.toFloat()
                val newHeat = (char.laserHeat + 0.1f).coerceAtMost(1f)
                val boosted = (eff * (1f + char.laserHeat * 0.5f)).toInt()
                val lineHits = st.monsters.filter { m ->
                    pointToSegDist(m.gridCol, m.gridRow, lcx, lcy, target.gridCol, target.gridRow) <= 0.55f
                }
                var ns = base
                for (t in lineHits) ns = dealDamage(ns, t.id, calcDamage(boosted, t.effectiveArmor), now, 0xFF00E5FF.toInt())
                ns.copy(characters = ns.characters.map { c -> if (c.id == char.id) c.copy(laserHeat = newHeat) else c })
            }
            TDCharType.CRYOMANCER -> {
                val dmg = calcDamage(eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFF90CAF9.toInt())
                if (target.bossRound != 90 && target.bossRound != 100) {
                    ns = ns.copy(monsters = ns.monsters.map { m ->
                        if (m.id == target.id) m.copy(slowUntil = now + 1500L) else m
                    })
                }
                ns
            }
            TDCharType.CORRUPTOR -> {
                val dmg = calcDamage(eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFFFF8C00.toInt())
                ns = ns.copy(monsters = ns.monsters.map { m ->
                    if (m.id == target.id) m.copy(armorBreak = 0.15f, armorBreakUntil = now + 3000L) else m
                })
                ns
            }
            TDCharType.REAPER -> {
                val execThresh = (target.maxHp * 0.15f).toInt()
                val dmg = if (target.hp <= execThresh) target.hp + 9999 else calcDamage(5, target.effectiveArmor)
                dealDamage(base, target.id, dmg, now, 0xFF7B1FA2.toInt())
            }
            TDCharType.COMMANDER -> base
            TDCharType.MERCHANT -> {
                val dmg = calcDamage(eff, target.effectiveArmor)
                val ns = dealDamage(base, target.id, dmg, now, 0xFFFFF176.toInt())
                if (ns.monsters.none { it.id == target.id } && st.monsters.any { it.id == target.id })
                    ns.copy(gold = ns.gold + 1, roundGoldEarned = ns.roundGoldEarned + 1)
                else ns
            }
        }
    }

    private fun dealDamage(st: TDUiState, mId: Int, dmg: Int, now: Long, argb: Int): TDUiState {
        val m = st.monsters.find { it.id == mId } ?: return st
        val newHp = m.hp - dmg
        return if (newHp <= 0) {
            val g = killGold(st.round, m.isBoss)
            var ns = st.copy(
                monsters = st.monsters.filter { it.id != mId },
                gold = st.gold + g, score = st.score + g * 10,
                killedCount = st.killedCount + 1, roundGoldEarned = st.roundGoldEarned + g,
                effects = st.effects + TDEffect(nextId++, col = m.gridCol, row = m.gridRow, argb = argb),
                manualTargetId = if (st.manualTargetId == mId) null else st.manualTargetId
            )
            // 80R Hydra: split on death
            if (m.bossRound == 80 && !m.isSplitChild) {
                val babies = (0 until 3).map {
                    m.copy(id = nextId++, hp = m.maxHp / 3, maxHp = m.maxHp / 3,
                        baseSpeed = m.baseSpeed * 1.3f, isSplitChild = true, poisonStacks = 0, slowUntil = 0L)
                }
                ns = ns.copy(monsters = ns.monsters + babies)
            }
            ns
        } else {
            st.copy(
                monsters = st.monsters.map { if (it.id == mId) it.copy(hp = newHp) else it },
                effects = st.effects + TDEffect(nextId++, col = m.gridCol, row = m.gridRow, argb = argb, progress = 0.6f)
            )
        }
    }

    private fun commanderBonus(char: TDCharacter, all: List<TDCharacter>): Float =
        all.filter { it.type == TDCharType.COMMANDER }
            .count { cmd -> dist(char.col.toFloat(), char.row.toFloat(), cmd.col.toFloat(), cmd.row.toFloat()) <= charRange(cmd) }
            .let { it * 0.10f }

    private fun endRound(st: TDUiState) {
        gameJob?.cancel(); spawnJob?.cancel(); spawnQueue.clear()
        val intr = interest(st.gold)
        val clr = clearBonus(st.round)
        val result = TDRoundResult(st.killedCount, st.escapedCount, st.roundGoldEarned, intr, clr)
        _state.update {
            it.copy(
                phase = if (it.round >= 100) TDPhase.GAME_OVER else TDPhase.ROUND_END,
                gold = it.gold + intr + clr,
                lastResult = result, monsters = emptyList(), effects = emptyList(), manualTargetId = null,
                unlockedRounds = max(it.unlockedRounds, it.round + 1).coerceAtMost(100)
            )
        }
        saveProgress(); saveResources()
    }

    // ─ Persistence helpers ───────────────────────────────────────────────────

    private fun saveResources() {
        val st = _state.value
        viewModelScope.launch { saveRepository.saveResources(TDResourcesSave(gold = st.gold, lives = st.lives)) }
    }

    private fun saveCharacters() {
        val chars = _state.value.characters.map { c ->
            TDCharacterSave(id = c.id, type = c.type.name, col = c.col, row = c.row,
                level = c.level, star = c.star, evolutionGrade = c.evolution.name, totalInvested = c.totalInvested)
        }
        viewModelScope.launch { saveRepository.saveCharacters(chars) }
    }

    private fun saveProgress() {
        val st = _state.value
        viewModelScope.launch {
            saveRepository.saveProgress(TDProgressSave(round = st.round, unlockedRounds = st.unlockedRounds, score = st.score))
        }
    }

    override fun onCleared() { gameJob?.cancel(); spawnJob?.cancel(); super.onCleared() }
}

// ─── Character Descriptions ──────────────────────────────────────────────────

private fun tdCharDesc(type: TDCharType): String = when (type) {
    TDCharType.SNIPER       -> "3타마다 치명타 2×"
    TDCharType.ARTILLERY    -> "착탄 반경 적 전체 피해"
    TDCharType.DUAL_PISTOLS -> "10타 후 공속 30%↑ 3초"
    TDCharType.POISONER     -> "독 최대 3중첩, 틱 피해"
    TDCharType.LASER_TURRET -> "직선 관통 + 연사 시 화력↑"
    TDCharType.CRYOMANCER   -> "명중 시 적 1.5초 슬로우"
    TDCharType.CORRUPTOR    -> "방어력 -15% 디버프 3초"
    TDCharType.REAPER       -> "HP 15% 이하 즉시 처형"
    TDCharType.COMMANDER    -> "주변 아군 공격력 +10%"
    TDCharType.MERCHANT     -> "처치 시 추가 골드 획득"
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TowerDefenseScreen(navController: NavController, vm: TowerDefenseViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val title = when (state.phase) {
        TDPhase.ROUND_SELECT -> "타워 디펜스"
        TDPhase.PREP -> "${state.round}라운드 준비"
        TDPhase.PLAYING -> "${state.round}R  ❤️${state.lives}  💰${state.gold}G"
        TDPhase.ROUND_END -> "${state.round}라운드 클리어!"
        TDPhase.GAME_OVER -> if (state.round >= 100 && state.lives > 0) "100라운드 클리어!" else "게임 오버"
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.phase == TDPhase.ROUND_SELECT) navController.popBackStack()
                        else vm.goToRoundSelect()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                TDPhase.ROUND_SELECT -> TDRoundSelectContent(state.unlockedRounds, state.gold, vm::selectRound, vm::watchAd)
                TDPhase.PREP, TDPhase.PLAYING -> TDGameContent(state, vm)
                TDPhase.ROUND_END -> TDRoundEndContent(state, vm::startNextRound, vm::replayRound, vm::goToRoundSelect)
                TDPhase.GAME_OVER -> TDGameOverContent(state, vm::goToRoundSelect)
            }
        }
    }
}

// ─── Round Select ─────────────────────────────────────────────────────────────

@Composable
private fun TDRoundSelectContent(
    unlockedRounds: Int,
    gold: Int,
    onSelect: (Int) -> Unit,
    onWatchAd: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                THEME_NAMES.take(5).forEachIndexed { i, name ->
                    Surface(shape = RoundedCornerShape(4.dp), color = THEME_COLORS[i]) {
                        Text(name, Modifier.padding(5.dp, 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💰 ${gold}G",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700.toInt())
                )
                OutlinedButton(
                    onClick = onWatchAd,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("+100G  📺 광고", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(count = 100) { i ->
                val round = i + 1
                val isLocked = round > unlockedRounds
                val isBoss = round % 10 == 0
                val themeIdx = min((round - 1) / 10, THEME_COLORS.size - 1)
                ElevatedCard(
                    onClick = { if (!isLocked) onSelect(round) },
                    modifier = Modifier.aspectRatio(1f),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isLocked)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else
                            THEME_COLORS[themeIdx].copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when {
                            isLocked -> Text("🔒", fontSize = 9.sp, lineHeight = 11.sp)
                            isBoss   -> Text("👑", fontSize = 10.sp, lineHeight = 12.sp)
                        }
                        Text(
                            "$round",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isBoss && !isLocked) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                isBoss   -> THEME_COLORS[themeIdx]
                                else     -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Game Screen ──────────────────────────────────────────────────────────────

@Composable
private fun TDGameContent(state: TDUiState, vm: TowerDefenseViewModel) {
    var canvasW by remember { mutableStateOf(1f) }
    var canvasH by remember { mutableStateOf(1f) }

    Column(Modifier.fillMaxSize()) {
        TDHudBar(state, vm)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize()
                    .onSizeChanged {
                        canvasW = it.width.toFloat().coerceAtLeast(1f)
                        canvasH = it.height.toFloat().coerceAtLeast(1f)
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val cs = min(canvasW / GRID_COLS, canvasH / GRID_ROWS)
                            val ox = (canvasW - cs * GRID_COLS) / 2
                            val oy = (canvasH - cs * GRID_ROWS) / 2
                            val col = ((offset.x - ox) / cs).toInt()
                            val row = ((offset.y - oy) / cs).toInt()
                            val hitMonster = state.monsters.firstOrNull { m ->
                                dist(offset.x, offset.y,
                                    m.gridCol * cs + cs * 0.5f + ox,
                                    m.gridRow * cs + cs * 0.5f + oy) <= cs * 0.5f
                            }
                            if (hitMonster != null) vm.tapMonster(hitMonster.id)
                            else vm.tapCell(col, row)
                        }
                    }
            ) {
                val cs = min(size.width / GRID_COLS, size.height / GRID_ROWS)
                val ox = (size.width - cs * GRID_COLS) / 2
                val oy = (size.height - cs * GRID_ROWS) / 2
                tdDrawGrid(cs, ox, oy, state)
                tdDrawCharacters(cs, ox, oy, state)
                tdDrawMonsters(cs, ox, oy, state)
                tdDrawEffects(cs, ox, oy, state.effects)
            }
        }

        TDBottomPanel(state, vm)
    }
}

@Composable
private fun TDHudBar(state: TDUiState, vm: TowerDefenseViewModel) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "❤️ ${state.lives}",
                fontWeight = FontWeight.Bold,
                color = if (state.lives <= 5) Color(0xFFE53935.toInt()) else MaterialTheme.colorScheme.onSurface
            )
            if (state.phase == TDPhase.PREP) {
                Button(onClick = vm::startRound) { Text("▶ 시작") }
            } else {
                val themeIdx = min((state.round - 1) / 10, THEME_NAMES.size - 1)
                val isBoss = state.round % 10 == 0
                Text(
                    "${THEME_NAMES[themeIdx]}${if (isBoss) "👑" else ""} [${state.killedCount}/${state.totalMonsters}]",
                    style = MaterialTheme.typography.bodySmall, color = THEME_COLORS[themeIdx],
                    fontWeight = FontWeight.Bold
                )
            }
            Text("💰 ${state.gold}G", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700.toInt()))
        }
    }
}

// ─── Draw Functions ───────────────────────────────────────────────────────────

private fun DrawScope.tdDrawGrid(cs: Float, ox: Float, oy: Float, state: TDUiState) {
    for (row in 0 until GRID_ROWS) {
        for (col in 0 until GRID_COLS) {
            val x = ox + col * cs; val y = oy + row * cs
            val isPath = PATH_CELLS.contains(col to row)
            val isEntry = (col to row) == TD_PATH.first()
            val isExit = (col to row) == TD_PATH.last()
            val isSelected = state.selectedCell == (col to row)
            val hasChar = state.characters.any { it.col == col && it.row == row }
            val isMoveTarget = state.isMovingChar && !isPath && !hasChar && !isEntry && !isExit

            val bg = when {
                isEntry -> Color(0xFF66BB6A.toInt())
                isExit -> Color(0xFFEF5350.toInt())
                isPath -> Color(0xFF8D6E63.toInt())
                isSelected && hasChar -> Color(0xFFFFEE58.toInt())
                isSelected -> Color(0xFFFFF9C4.toInt())
                isMoveTarget -> Color(0xFF26A69A.toInt())
                hasChar -> Color(0xFF455A64.toInt())
                else -> Color(0xFF37474F.toInt())
            }
            drawRect(bg, topLeft = Offset(x + 1f, y + 1f), size = Size(cs - 2f, cs - 2f))

            if (isEntry || isExit) {
                drawIntoCanvas { c ->
                    val p = NativePaint().apply {
                        color = android.graphics.Color.WHITE; textSize = cs * 0.28f
                        textAlign = NativePaint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
                    }
                    c.nativeCanvas.drawText(if (isEntry) "IN" else "OUT", x + cs / 2f, y + cs * 0.68f, p)
                }
            }
        }
    }
    // Path direction lines
    for (i in 0 until TD_PATH.size - 1) {
        val (c1, r1) = TD_PATH[i]; val (c2, r2) = TD_PATH[i + 1]
        drawLine(
            Color(0x44FFFFFF),
            Offset(ox + (c1 + 0.5f) * cs, oy + (r1 + 0.5f) * cs),
            Offset(ox + (c2 + 0.5f) * cs, oy + (r2 + 0.5f) * cs),
            strokeWidth = 1.5f
        )
    }
    // Range ring for selected character
    state.selectedCell?.let { (col, row) ->
        val char = state.characters.find { it.col == col && it.row == row } ?: return@let
        val cx = ox + (col + 0.5f) * cs; val cy = oy + (row + 0.5f) * cs
        val rng = charRange(char) * cs
        drawCircle(Color(0x22FFFFFF), rng, Offset(cx, cy))
        drawCircle(Color(0x66FFFFFF), rng, Offset(cx, cy), style = Stroke(2f))
    }
}

private fun DrawScope.tdDrawCharacters(cs: Float, ox: Float, oy: Float, state: TDUiState) {
    for (c in state.characters) {
        val cx = ox + (c.col + 0.5f) * cs; val cy = oy + (c.row + 0.5f) * cs
        val r = cs * 0.38f
        if (c.type == TDCharType.DUAL_PISTOLS && System.currentTimeMillis() < c.rapidFireUntil)
            drawCircle(Color(0x669C27B0.toInt()), r * 1.4f, Offset(cx, cy))
        tdDrawCharBody(c.type, cx, cy, r)
        if (c.level > 1)
            drawCircle(Color(0xFFFFD700.toInt()), r * 0.28f, Offset(cx + r * 0.7f, cy - r * 0.7f))
    }
}

private fun DrawScope.tdDrawCharBody(type: TDCharType, cx: Float, cy: Float, r: Float) {
    val body    = Color(type.charColor)
    val dark    = body.copy(alpha = 0.9f)
    val outline = Color(0xFF000000.toInt()).copy(alpha = 0.65f)
    when (type) {
        TDCharType.SNIPER -> {
            drawCircle(dark, r, Offset(cx, cy))
            val bLen = r * 1.65f; val cos30 = 0.866f; val sin30 = 0.5f
            drawLine(Color(0xFF424242.toInt()),
                Offset(cx - r * 0.12f, cy + r * 0.08f),
                Offset(cx + bLen * cos30, cy - bLen * sin30), strokeWidth = r * 0.22f)
            drawCircle(Color(0xFF212121.toInt()), r * 0.18f,
                Offset(cx + bLen * 0.46f * cos30, cy - bLen * 0.46f * sin30))
            drawCircle(Color.White.copy(alpha = 0.4f), r * 0.08f,
                Offset(cx + bLen * 0.46f * cos30, cy - bLen * 0.46f * sin30))
            drawCircle(Color.White.copy(alpha = 0.14f), r * 0.55f, Offset(cx - r * 0.15f, cy - r * 0.15f))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.ARTILLERY -> {
            drawCircle(dark, r, Offset(cx, cy))
            drawLine(Color(0xFF3E2723.toInt()),
                Offset(cx - r * 0.1f, cy), Offset(cx + r * 1.9f, cy), strokeWidth = r * 0.36f)
            drawCircle(Color(0xFF3E2723.toInt()), r * 0.19f, Offset(cx + r * 1.9f, cy))
            drawCircle(Color(0xFF4E342E.toInt()), r * 0.24f, Offset(cx - r * 0.42f, cy + r * 0.88f))
            drawCircle(Color(0xFF4E342E.toInt()), r * 0.24f, Offset(cx + r * 0.38f, cy + r * 0.88f))
            drawCircle(Color.White.copy(alpha = 0.14f), r * 0.5f, Offset(cx - r * 0.15f, cy - r * 0.18f))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.DUAL_PISTOLS -> {
            drawCircle(dark, r, Offset(cx, cy))
            val gl = r * 1.3f
            drawLine(Color(0xFF1A1A2E.toInt()),
                Offset(cx - r * 0.18f, cy), Offset(cx - gl * 0.72f, cy - gl * 0.72f), strokeWidth = r * 0.18f)
            drawLine(Color(0xFF1A1A2E.toInt()),
                Offset(cx + r * 0.18f, cy), Offset(cx + gl * 0.72f, cy - gl * 0.72f), strokeWidth = r * 0.18f)
            drawCircle(Color(0xFFFFF176.toInt()), r * 0.12f, Offset(cx - gl * 0.72f, cy - gl * 0.72f))
            drawCircle(Color(0xFFFFF176.toInt()), r * 0.12f, Offset(cx + gl * 0.72f, cy - gl * 0.72f))
            drawCircle(Color.White.copy(alpha = 0.14f), r * 0.5f, Offset(cx - r * 0.1f, cy - r * 0.12f))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.POISONER -> {
            val hatPath = Path().apply {
                moveTo(cx, cy - r * 1.88f)
                lineTo(cx - r * 0.72f, cy - r * 0.84f)
                lineTo(cx + r * 0.72f, cy - r * 0.84f)
                close()
            }
            drawPath(hatPath, Color(0xFF1B5E20.toInt()))
            drawLine(Color(0xFF2E7D32.toInt()),
                Offset(cx - r * 0.9f, cy - r * 0.84f),
                Offset(cx + r * 0.9f, cy - r * 0.84f), strokeWidth = r * 0.23f)
            drawCircle(dark, r, Offset(cx, cy))
            for (j in -1..1) {
                val dx = j * r * 0.36f
                drawLine(Color(0xFF66BB6A.toInt()),
                    Offset(cx + dx, cy + r * 0.88f), Offset(cx + dx, cy + r * 1.32f), strokeWidth = r * 0.14f)
                drawCircle(Color(0xFF4CAF50.toInt()), r * 0.1f, Offset(cx + dx, cy + r * 1.32f))
            }
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.LASER_TURRET -> {
            val half = r * 0.93f
            drawRect(dark.copy(alpha = 0.9f),
                topLeft = Offset(cx - half, cy - half), size = Size(half * 2f, half * 2f))
            drawLine(Color(0xFF006064.toInt()), Offset(cx, cy),
                Offset(cx + half * 1.75f, cy - half * 0.28f), strokeWidth = r * 0.3f)
            drawCircle(Color(0xFF006064.toInt()), r * 0.17f, Offset(cx + half * 1.75f, cy - half * 0.28f))
            drawCircle(Color(0xFF00E5FF.toInt()).copy(alpha = 0.45f), r * 0.42f, Offset(cx, cy))
            drawCircle(Color(0xFFE0FFFF.toInt()), r * 0.2f, Offset(cx, cy))
            drawRect(outline, topLeft = Offset(cx - half, cy - half), size = Size(half * 2f, half * 2f), style = Stroke(1.5f))
        }
        TDCharType.CRYOMANCER -> {
            drawCircle(dark, r, Offset(cx, cy))
            for (i in 0 until 6) {
                val angle = i * 60.0 * PI / 180.0
                val ex = cx + r * 1.26f * cos(angle).toFloat()
                val ey = cy + r * 1.26f * sin(angle).toFloat()
                drawLine(Color(0xFFE3F2FD.toInt()), Offset(cx, cy), Offset(ex, ey), strokeWidth = r * 0.14f)
                drawCircle(Color(0xFFBBDEFB.toInt()), r * 0.1f, Offset(ex, ey))
            }
            drawCircle(Color(0xFFE3F2FD.toInt()), r * 0.33f, Offset(cx, cy))
            drawCircle(Color(0xFF1565C0.toInt()), r, Offset(cx, cy), style = Stroke(2f))
        }
        TDCharType.CORRUPTOR -> {
            drawCircle(dark, r, Offset(cx, cy))
            for (i in 0 until 3) {
                drawArc(color = Color(0xFF212121.toInt()).copy(alpha = 0.72f),
                    startAngle = i * 120f - 50f, sweepAngle = 76f, useCenter = true,
                    topLeft = Offset(cx - r * 0.62f, cy - r * 0.62f), size = Size(r * 1.24f, r * 1.24f))
            }
            drawCircle(Color(0xFF1A1A1A.toInt()), r * 0.22f, Offset(cx, cy))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.REAPER -> {
            drawCircle(dark, r, Offset(cx, cy))
            drawLine(Color(0xFF4A148C.toInt()),
                Offset(cx - r * 0.45f, cy + r * 1.05f), Offset(cx + r * 0.75f, cy - r * 0.95f),
                strokeWidth = r * 0.22f)
            drawArc(color = Color(0xFFCE93D8.toInt()),
                startAngle = -140f, sweepAngle = 88f, useCenter = false,
                topLeft = Offset(cx - r * 0.18f, cy - r * 1.38f), size = Size(r * 1.12f, r * 1.12f),
                style = Stroke(r * 0.32f))
            drawArc(color = Color(0xFF1A001A.toInt()).copy(alpha = 0.5f),
                startAngle = 185f, sweepAngle = 170f, useCenter = true,
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2f, r))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.COMMANDER -> {
            drawCircle(dark, r, Offset(cx, cy))
            drawLine(Color(0xFF827717.toInt()),
                Offset(cx + r * 0.06f, cy - r * 0.28f), Offset(cx + r * 0.06f, cy - r * 1.78f),
                strokeWidth = r * 0.18f)
            val flagPath = Path().apply {
                moveTo(cx + r * 0.06f, cy - r * 1.78f)
                lineTo(cx + r * 0.86f, cy - r * 1.52f)
                lineTo(cx + r * 0.06f, cy - r * 1.26f)
                close()
            }
            drawPath(flagPath, Color(0xFFFF5722.toInt()))
            drawCircle(Color.White.copy(alpha = 0.14f), r * 0.5f, Offset(cx - r * 0.14f, cy - r * 0.16f))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.MERCHANT -> {
            drawCircle(dark, r, Offset(cx, cy))
            drawCircle(Color(0xFFFF8F00.toInt()), r * 0.36f, Offset(cx + r * 0.74f, cy - r * 0.1f))
            drawCircle(Color(0xFFFFF8E1.toInt()), r * 0.19f, Offset(cx + r * 0.74f, cy - r * 0.1f))
            drawLine(Color(0xFF5D4037.toInt()),
                Offset(cx + r * 0.74f, cy - r * 0.3f), Offset(cx + r * 0.74f, cy + r * 0.1f),
                strokeWidth = r * 0.12f)
            drawCircle(Color.White.copy(alpha = 0.14f), r * 0.5f, Offset(cx - r * 0.1f, cy - r * 0.16f))
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
    }
}

private fun DrawScope.tdDrawMonsters(cs: Float, ox: Float, oy: Float, state: TDUiState) {
    val themeIdx = min((state.round - 1) / 10, THEME_COLORS.size - 1)
    for (m in state.monsters) {
        val cx = ox + m.gridCol * cs + cs * 0.5f; val cy = oy + m.gridRow * cs + cs * 0.5f
        val r = cs * (if (m.isBoss) 0.43f else 0.30f)

        if (m.isSlowed) drawCircle(Color(0x6690CAF9.toInt()), r * 1.4f, Offset(cx, cy))
        if (m.poisonStacks > 0) drawCircle(Color(0x554CAF50.toInt()), r * 1.25f, Offset(cx, cy))
        if (m.phase2Active) drawCircle(Color(0x88FF4500.toInt()), r * 1.55f, Offset(cx, cy))

        val color = when {
            m.isBoss -> THEME_COLORS[themeIdx]
            m.isSplitChild -> Color(0xFFFF9800.toInt())
            m.poisonStacks > 0 -> Color(0xFF388E3C.toInt())
            else -> Color(0xFFE53935.toInt())
        }
        drawCircle(color, r, Offset(cx, cy))
        drawCircle(Color.Black.copy(alpha = 0.5f), r, Offset(cx, cy), style = Stroke(1.5f))

        val hpFrac = m.hp.toFloat() / m.maxHp
        val bw = cs * 0.78f; val bh = 4f
        val bx = cx - bw / 2; val by = cy - r - 7f
        drawRect(Color(0x88000000.toInt()), topLeft = Offset(bx, by), size = Size(bw, bh))
        val hpColor = if (hpFrac > 0.5f) Color(0xFF4CAF50.toInt()) else if (hpFrac > 0.25f) Color(0xFFFFEB3B.toInt()) else Color(0xFFF44336.toInt())
        drawRect(hpColor, topLeft = Offset(bx, by), size = Size(bw * hpFrac, bh))

        if (state.manualTargetId == m.id)
            drawCircle(Color(0xFFFFEB3B.toInt()), r + 4f, Offset(cx, cy), style = Stroke(3f))

        if (m.isBoss) {
            drawIntoCanvas { canvas ->
                val p = NativePaint().apply {
                    setColor(android.graphics.Color.WHITE); textSize = r * 0.52f
                    textAlign = NativePaint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
                }
                val bIdx = ((m.bossRound / 10) - 1).coerceIn(0, BOSS_NAMES.size - 1)
                canvas.nativeCanvas.drawText(BOSS_NAMES[bIdx].take(3), cx, cy + p.textSize / 3f, p)
            }
        }
    }
}

private fun DrawScope.tdDrawEffects(cs: Float, ox: Float, oy: Float, effects: List<TDEffect>) {
    for (e in effects) {
        val alpha = (1f - e.progress).coerceIn(0f, 1f)
        when (e.kind) {
            TDEffectKind.PROJECTILE -> {
                val t = e.progress.coerceIn(0f, 1f)
                val fromX = ox + e.fromCol * cs; val fromY = oy + e.fromRow * cs
                val toX   = ox + (e.col + 0.5f) * cs; val toY = oy + (e.row + 0.5f) * cs
                val curX  = fromX + (toX - fromX) * t; val curY = fromY + (toY - fromY) * t
                val a = (if (t > 0.78f) (1f - t) / 0.22f else 1f).coerceIn(0f, 1f)
                if (t > 0.12f) {
                    val t2 = t - 0.12f
                    val trX = fromX + (toX - fromX) * t2; val trY = fromY + (toY - fromY) * t2
                    drawLine(Color(e.argb).copy(alpha = a * 0.32f),
                        Offset(trX, trY), Offset(curX, curY), strokeWidth = cs * 0.07f)
                }
                drawCircle(Color(e.argb).copy(alpha = a), cs * 0.13f, Offset(curX, curY))
                drawCircle(Color.White.copy(alpha = a * 0.55f), cs * 0.06f, Offset(curX, curY))
            }
            TDEffectKind.LASER -> {
                val a = alpha * 0.9f
                val fromX = ox + e.fromCol * cs; val fromY = oy + e.fromRow * cs
                val toX   = ox + (e.col + 0.5f) * cs; val toY = oy + (e.row + 0.5f) * cs
                drawLine(Color(e.argb).copy(alpha = a), Offset(fromX, fromY), Offset(toX, toY), strokeWidth = cs * 0.055f)
                drawLine(Color.White.copy(alpha = a * 0.5f), Offset(fromX, fromY), Offset(toX, toY), strokeWidth = cs * 0.022f)
            }
            TDEffectKind.HIT -> {
                val cx = ox + (e.col + 0.5f) * cs; val cy = oy + (e.row + 0.5f) * cs
                drawCircle(Color(e.argb).copy(alpha = alpha * 0.85f),
                    cs * 0.22f * (1f + e.progress * 0.6f), Offset(cx, cy))
            }
            TDEffectKind.SPLASH -> {
                val cx = ox + (e.col + 0.5f) * cs; val cy = oy + (e.row + 0.5f) * cs
                val splashR = e.splashRadius * cs * (0.8f + e.progress * 0.5f)
                drawCircle(Color(e.argb).copy(alpha = alpha * 0.38f), splashR, Offset(cx, cy))
                drawCircle(Color(e.argb).copy(alpha = alpha * 0.7f), splashR, Offset(cx, cy), style = Stroke(2f))
            }
        }
    }
}

// ─── Bottom Panel ─────────────────────────────────────────────────────────────

@Composable
private fun TDBottomPanel(state: TDUiState, vm: TowerDefenseViewModel) {
    val sel = state.selectedCell
    val selChar = if (sel != null) state.characters.find { it.col == sel.first && it.row == sel.second } else null

    Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        if (selChar != null) {
            TDCharInfoPanel(selChar, state, vm)
        } else {
            TDShopPanel(state, vm, cellSelected = sel != null)
        }
    }
}

@Composable
private fun TDShopPanel(state: TDUiState, vm: TowerDefenseViewModel, cellSelected: Boolean) {
    val cost = buyGold(state.characters.size)
    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (cellSelected) "캐릭터 선택 (${cost}G)" else "빈 칸 탭 → 배치",
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (cellSelected) TextButton(onClick = vm::clearSelection) { Text("취소") }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TDCharType.entries.forEach { type ->
                TDCharBuyCard(type = type, cost = cost, enabled = cellSelected && state.gold >= cost) {
                    vm.buyCharacter(type)
                }
            }
        }
    }
}

@Composable
private fun TDCharBuyCard(type: TDCharType, cost: Int, enabled: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(80.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (enabled) Color(type.charColor).copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.emoji, fontSize = 18.sp, lineHeight = 20.sp)
            Text(type.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                "ATK ${type.baseAtk}  ${type.baseRange}칸",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                tdCharDesc(type),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                color = Color(type.charColor).copy(alpha = if (enabled) 1f else 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 10.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${cost}G", style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Color(0xFF4CAF50.toInt()) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TDCharInfoPanel(char: TDCharacter, state: TDUiState, vm: TowerDefenseViewModel) {
    val maxLv = char.star * 10
    val canLvUp = char.level < maxLv
    val canStarUp = !canLvUp && char.star < MAX_STAR
    val lvCost = if (canLvUp) levelUpGold(char.level + 1, char.star) else 0
    val starCost = if (canStarUp) starUpGold(char.star) else 0
    val sellVal = (char.totalInvested * 0.7).toInt()
    val atkSpd = if (char.type.atkMs > 0) String.format("%.1f", char.type.atkMs / 1000.0) + "s" else "—"

    val nextEvo = char.evolution.next()
    val canEvolve = nextEvo != null && char.star >= nextEvo.reqStar && char.level >= nextEvo.reqLevel
    val evoCost = nextEvo?.cost ?: 0

    if (state.isMovingChar) {
        // Move-mode banner
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF26A69A.toInt())).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("이동할 빈 칸을 탭하세요", Modifier.weight(1f),
                color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = vm::clearSelection) { Text("취소", color = Color.White) }
        }
        return
    }

    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        // Header row
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("[${char.evolution.label}급] ${char.type.emoji} ${char.type.label}",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("Lv.${char.level}/$maxLv  ⭐${char.star}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::clearSelection, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("닫기", style = MaterialTheme.typography.labelSmall)
            }
        }
        // Stats row
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("ATK ${charBaseAtk(char)}", style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFEF9A9A.toInt()))
            Text("사거리 ${String.format("%.1f", charRange(char))}칸", style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF90CAF9.toInt()))
            Text("공속 ${atkSpd}", style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA5D6A7.toInt()))
            Text(tdCharDesc(char.type), style = MaterialTheme.typography.labelSmall,
                color = Color(char.type.charColor).copy(alpha = 0.9f))
        }
        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (canLvUp) {
                Button(onClick = vm::levelUpCharacter, enabled = state.gold >= lvCost,
                    modifier = Modifier.weight(1f)) {
                    Text("강화 ${lvCost}G", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            } else if (canStarUp) {
                Button(onClick = vm::starUpCharacter, enabled = state.gold >= starCost,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300.toInt()))) {
                    Text("⭐${char.star}→${char.star + 1}성 ${starCost}G",
                        maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            } else if (nextEvo != null) {
                Button(onClick = vm::evolveCharacter, enabled = canEvolve && state.gold >= evoCost,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50.toInt()))) {
                    val label = if (canEvolve) "진화 ${evoCost}G" else "진화(${nextEvo.reqStar}성/${nextEvo.reqLevel}L)"
                    Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Box(Modifier.weight(1f))
            }
            OutlinedButton(onClick = vm::startMoveMode, modifier = Modifier.weight(1f)) {
                Text("이동", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = { vm.sellCharacter() }, modifier = Modifier.weight(1f)) {
                Text("+${sellVal}G 판매", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─── Round End / Game Over ────────────────────────────────────────────────────

@Composable
private fun TDRoundEndContent(state: TDUiState, onNext: () -> Unit, onReplay: () -> Unit, onSelect: () -> Unit) {
    val result = state.lastResult
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val themeIdx = min((state.round - 1) / 10, THEME_NAMES.size - 1)
        Text("${state.round}라운드 클리어!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = THEME_COLORS[themeIdx])
        Spacer(Modifier.height(20.dp))
        if (result != null) {
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("처치"); Text("${result.kills}마리", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("탈출"); Text("${result.escaped}마리",
                            fontWeight = FontWeight.Bold,
                            color = if (result.escaped > 0) Color(0xFFE53935.toInt()) else MaterialTheme.colorScheme.onSurface)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("킬 골드"); Text("+${result.goldEarned}G", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700.toInt()))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("이자"); Text("+${result.interest}G", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50.toInt()))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("클리어 보너스"); Text("+${result.clearBonus}G", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50.toInt()))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("현재 골드", fontWeight = FontWeight.Bold)
                        Text("${state.gold}G", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700.toInt()))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("${state.round + 1}라운드 도전!")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onReplay, modifier = Modifier.fillMaxWidth()) {
            Text("🔁  ${state.round}라운드 반복")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
            Text("라운드 선택으로")
        }
    }
}

@Composable
private fun TDGameOverContent(state: TDUiState, onSelect: () -> Unit) {
    val isVictory = state.round >= 100 && state.lives > 0
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isVictory) "🏆 정복 완료!" else "💀 게임 오버",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("라운드 ${state.round} | 점수 ${state.score}점",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) { Text("라운드 선택으로") }
    }
}
