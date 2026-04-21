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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import kotlin.math.abs
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

import androidx.compose.ui.res.painterResource
import com.poke86.game.R

// ─── Constants & Map ───────────────────────────

private const val GRID_COLS = 9
private const val GRID_ROWS = 12
private const val STARTING_LIVES = 20
private const val STARTING_GOLD = 150

// S-curve path: entry top-left, exit bottom-right
private val TD_PATH: List<Pair<Int, Int>> = listOf(
    1 to 0, 1 to 1, 1 to 2, 1 to 3,
    2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3,
    7 to 4, 7 to 5, 7 to 6,
    6 to 6, 5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6,
    1 to 7, 1 to 8, 1 to 9,
    2 to 9, 3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9,
    7 to 10, 7 to 11
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
    ECLIPSE("에클립스", "🌑", 25, 3.5f, 900L, 0xFFE0E0E0.toInt(), false),
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
    val stunUntil: Long = 0L,
    val burnStacks: Int = 0,
    val burnTickAt: Long = 0L,
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
    val isStunned: Boolean
        get() = System.currentTimeMillis() < stunUntil
    val currentSpeed: Float
        get() = when {
            isStunned -> 0f
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
    val baseAtkMult: Float = 1.0f,
    val totalInvested: Int,
    val targetAngle: Float = 0f,
    val lastAtkAt: Long = 0L,
    val precisionHits: Int = 0,
    val rapidFireHits: Int = 0,
    val rapidFireUntil: Long = 0L,
    val laserHeat: Float = 0f,
    val muzzleFlashUntil: Long = 0L,
    val recoilOffset: Offset = Offset.Zero,
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
    val damage: Int = 0,
    val splashRadius: Float = 0f,
    val charType: TDCharType? = null,
)

data class TDFieldEffect(
    val id: Int,
    val col: Float,
    val row: Float,
    val radius: Float,
    val until: Long,
    val tickAt: Long,
    val dmg: Int,
    val isNapalm: Boolean = false,
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
    val fieldEffects: List<TDFieldEffect> = emptyList(),
    val selectedCell: Pair<Int, Int>? = null,
    val manualTargetId: Int? = null,
    val isMovingChar: Boolean = false,
    val spawningCount: Int = 0,
    val totalMonsters: Int = 0,
    val gameSpeed: Float = 1.0f,
    val showDamage: Boolean = true,
    val showCharGuide: Boolean = false,
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
private fun adReward(round: Int): Int =
    (100.0 * Math.pow(1.12, (round - 1).toDouble())).toInt()
private const val MAX_STAR = 5
private fun charBaseAtk(c: TDCharacter): Int {
    var base = (c.type.baseAtk * c.baseAtkMult * (1.0 + (c.level - 1) * 0.08 + (c.star - 1) * 0.15))
    if (c.type == TDCharType.ARTILLERY && c.evolution >= TDEvolution.A) base *= 1.3
    return base.toInt()
}
private fun charRange(c: TDCharacter): Float = c.type.baseRange + (c.star - 1) * 0.1f
private fun charAtkMs(c: TDCharacter): Long {
    var ms = c.type.atkMs
    if (c.type == TDCharType.DUAL_PISTOLS && System.currentTimeMillis() < c.rapidFireUntil)
        ms = (ms * 0.7f).toLong()
    if (c.type == TDCharType.SNIPER && c.evolution >= TDEvolution.A)
        ms = (ms * 0.8f).toLong()
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
                        baseAtkMult = s.baseAtkMult,
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

    fun summonCharacter() {
        val st = _state.value
        val cell = st.selectedCell ?: return
        if (PATH_CELLS.contains(cell)) return
        if (st.characters.any { it.col == cell.first && it.row == cell.second }) return
        val cost = buyGold(st.characters.size)
        if (st.gold < cost) return
        
        val randomType = if ((1..100).random() <= 5) {
            TDCharType.ECLIPSE
        } else {
            TDCharType.entries.filter { it != TDCharType.ECLIPSE }.random()
        }
        
        _state.update {
            it.copy(
                gold = it.gold - cost,
                characters = it.characters + TDCharacter(
                    id = nextId++, type = randomType,
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

        val currentMult = char.baseAtkMult * (1.0 + (char.level - 1) * 0.08 + (char.star - 1) * 0.15).toFloat()

        _state.update {
            it.copy(
                gold = it.gold - next.cost,
                characters = it.characters.map { c ->
                    if (c.id == char.id) c.copy(
                        evolution = next,
                        level = 1,
                        star = 1,
                        baseAtkMult = currentMult,
                        totalInvested = c.totalInvested + next.cost
                    ) else c
                }
            )
        }
        saveResources(); saveCharacters()
    }

    fun tapMonster(id: Int) =
        _state.update { it.copy(manualTargetId = if (it.manualTargetId == id) null else id) }

    fun startNextRound() {
        val next = (_state.value.round + 1).coerceAtMost(100)
        _state.update { it.copy(round = next) }
        saveProgress()
        startRound()
    }

    fun replayRound() {
        startRound()
    }

    fun toggleSpeed() {
        _state.update { 
            val nextSpeed = when(it.gameSpeed) {
                1.0f -> 2.0f
                2.0f -> 4.0f
                else -> 1.0f
            }
            it.copy(gameSpeed = nextSpeed)
        }
    }

    fun toggleDamage() {
        _state.update { it.copy(showDamage = !it.showDamage) }
    }

    fun toggleCharGuide() {
        _state.update { it.copy(showCharGuide = !it.showCharGuide) }
    }

    fun watchAd() {
        val reward = adReward(_state.value.unlockedRounds)
        _state.update { it.copy(gold = it.gold + reward) }
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

        // Update characters (decay recoil)
        val updatedChars = st.characters.map { c ->
            if (c.recoilOffset != Offset.Zero) {
                val nextRx = c.recoilOffset.x * 0.85f
                val nextRy = c.recoilOffset.y * 0.85f
                val nextRecoil = if (abs(nextRx) < 0.01f && abs(nextRy) < 0.01f) Offset.Zero else Offset(nextRx, nextRy)
                c.copy(recoilOffset = nextRecoil)
            } else c
        }
        st = st.copy(characters = updatedChars)

        st = moveMonsters(st, now)

        val escaped = st.monsters.filter { it.waypointIdx >= TD_PATH.size - 1 }
        if (escaped.isNotEmpty()) {
            val containsBoss = escaped.any { it.isBoss }
            val newLives = if (containsBoss) 0 else (st.lives - escaped.size).coerceAtLeast(0)
            st = st.copy(
                lives = newLives,
                monsters = st.monsters.filter { it.waypointIdx < TD_PATH.size - 1 },
                escapedCount = st.escapedCount + escaped.size
            )
            if (newLives <= 0) {
                gameJob?.cancel(); spawnJob?.cancel()
                _state.update { it.copy(phase = TDPhase.GAME_OVER, monsters = emptyList(), lives = 0) }
                return
            }
        }

        st = applyPoison(st, now)
        st = applyBossRegen(st, now)
        st = checkMechaPhase2(st)
        st = processAttacks(st, now)
        st = applyFieldEffects(st, now)

        val effects = st.effects
            .map { e -> 
                var step = (when (e.kind) {
                    TDEffectKind.PROJECTILE -> 0.13f
                    TDEffectKind.LASER     -> 0.18f
                    TDEffectKind.HIT       -> 0.10f
                    TDEffectKind.SPLASH    -> 0.07f
                }) * st.gameSpeed
                // Artillery A-grade: Projectile speed x2
                if (e.kind == TDEffectKind.PROJECTILE && e.charType == TDCharType.ARTILLERY) {
                    step *= 2f
                }
                e.copy(progress = e.progress + step)
            }
            .filter { it.progress < 1f }
        st = st.copy(effects = effects)

        val fieldEffects = st.fieldEffects
            .filter { it.until > now }
        st = st.copy(fieldEffects = fieldEffects)

        if (st.monsters.isEmpty() && spawnQueue.isEmpty() && st.spawningCount <= 0) {
            endRound(st); return
        }
        _state.value = st
    }

    private fun moveMonsters(st: TDUiState, now: Long): TDUiState {
        val moved = st.monsters.map { m ->
            val delta = m.currentSpeed * 16f / 1000f * st.gameSpeed
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

    private fun applyFieldEffects(st: TDUiState, now: Long): TDUiState {
        var cur = st
        val tcking = st.fieldEffects.filter { it.tickAt <= now }
        if (tcking.isEmpty()) return st

        var extraGold = 0
        for (fe in tcking) {
            val targets = cur.monsters.filter { dist(fe.col, fe.row, it.gridCol, it.gridRow) <= fe.radius }
            for (t in targets) {
                val armor = if (fe.isNapalm) 0 else t.effectiveArmor
                val dmg = if (fe.isNapalm) fe.dmg else calcDamage(fe.dmg, armor)
                val mId = t.id
                val m = cur.monsters.find { it.id == mId } ?: continue
                val newHp = m.hp - dmg
                if (newHp <= 0) {
                    val g = killGold(cur.round, m.isBoss)
                    extraGold += g
                    cur = cur.copy(
                        monsters = cur.monsters.filter { it.id != mId },
                        killedCount = cur.killedCount + 1, score = cur.score + g * 10,
                        effects = cur.effects + TDEffect(nextId++, col = m.gridCol, row = m.gridRow, argb = 0xFFFF4400.toInt(), damage = dmg)
                    )
                } else {
                    cur = cur.copy(monsters = cur.monsters.map {
                        if (it.id == mId) it.copy(hp = newHp) else it
                    })
                }
            }
            cur = cur.copy(fieldEffects = cur.fieldEffects.map {
                if (it.id == fe.id) it.copy(tickAt = now + 500L) else it
            })
        }
        if (extraGold > 0) cur = cur.copy(gold = cur.gold + extraGold, roundGoldEarned = cur.roundGoldEarned + extraGold)
        return cur
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

            val angle = Math.toDegrees(Math.atan2((target.gridRow - cy).toDouble(), (target.gridCol - cx).toDouble())).toFloat()
            val baseDmg = max(1, (charBaseAtk(char) * (1f + cmdBonus)).toInt())
            
            // Recoil calculation: move slightly in opposite direction of target
            val rad = Math.toRadians(angle.toDouble())
            val rx = -cos(rad).toFloat() * 0.12f
            val ry = -sin(rad).toFloat() * 0.12f
            
            cur = applyAttack(cur, char, target, baseDmg, now)
            cur = cur.copy(characters = cur.characters.map { c ->
                if (c.id == char.id) c.copy(
                    lastAtkAt = now, 
                    targetAngle = angle,
                    muzzleFlashUntil = now + 100L,
                    recoilOffset = Offset(rx, ry)
                ) else c
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
                argb = if (char.type == TDCharType.SNIPER && char.evolution >= TDEvolution.S_PLUS) 0xFFFF0000.toInt() else char.type.charColor,
                charType = char.type
            )
        }
        val base = if (attackEffect != null) st.copy(effects = st.effects + attackEffect) else st

        return when (char.type) {
            TDCharType.SNIPER -> {
                val probBonus = if (char.evolution >= TDEvolution.A) 5 else 0
                val evo = char.evolution
                
                // Headshot (S+)
                val isInstaKill = evo >= TDEvolution.S_PLUS && !target.isBoss && (0..99).random() < (2 + probBonus)
                
                if (isInstaKill) {
                    dealDamage(base, target.id, target.hp + 9999, now, 0xFFFF0000.toInt())
                } else {
                    // Precision Shot (D, C, S)
                    // F/E/D: 3타 1.5배, C/B/A: 3타 2.0배, S: 2타 2.0배
                    val precThreshold = if (evo >= TDEvolution.S) 1 else 2
                    val isPrec = char.precisionHits >= precThreshold
                    
                    // Damage Multiplier
                    var mult = 1.0f
                    if (isPrec) {
                        mult = when {
                            evo >= TDEvolution.C -> 2.0f
                            evo >= TDEvolution.D -> 1.5f
                            else -> 1.5f // F, E
                        }
                    }
                    
                    // Crit (E)
                    if (evo >= TDEvolution.E && (0..99).random() < (10 + probBonus)) mult *= 1.5f
                    
                    // Armor Ignore (C) - Only on Precision Shot
                    val armor = if (isPrec && evo >= TDEvolution.C) (target.effectiveArmor * 0.9f).toInt() else target.effectiveArmor
                    
                    var dmg = calcDamage((eff * mult).toInt(), armor)
                    
                    // Boss Max HP dmg (S+)
                    if (evo >= TDEvolution.S_PLUS && target.isBoss) dmg += (target.maxHp * 0.05f).toInt()
                    
                    var ns = dealDamage(base, target.id, dmg, now, 0xFFF0E68C.toInt())
                    
                    // Stun (B, S)
                    if (evo >= TDEvolution.B && (0..99).random() < (5 + probBonus)) {
                        val stunSec = if (evo >= TDEvolution.S) 2000L else 1000L
                        ns = ns.copy(monsters = ns.monsters.map { 
                            if (it.id == target.id) it.copy(stunUntil = now + stunSec) else it
                        })
                    }
                    
                    ns.copy(characters = ns.characters.map { c ->
                        if (c.id == char.id) c.copy(precisionHits = if (isPrec) 0 else c.precisionHits + 1) else c
                    })
                }
            }
            TDCharType.ARTILLERY -> {
                val probBonus = if (char.evolution >= TDEvolution.A) 5 else 0
                val evo = char.evolution
                
                // Base & E-grade Radius
                val radius = if (evo >= TDEvolution.E) 1.2f * 1.15f else 1.2f
                
                val splash = st.monsters.filter { m ->
                    dist(target.gridCol, target.gridRow, m.gridCol, m.gridRow) <= radius
                }
                
                var ns = base.copy(effects = base.effects + TDEffect(
                    nextId++, kind = TDEffectKind.SPLASH,
                    col = target.gridCol, row = target.gridRow,
                    argb = 0xFFFF6600.toInt(), splashRadius = radius
                ))
                
                // Deal splash damage
                for (t in splash) {
                    var dmg = calcDamage(eff, t.effectiveArmor)
                    
                    // B-grade Slow (Shockwave)
                    if (evo >= TDEvolution.B) {
                        ns = ns.copy(monsters = ns.monsters.map {
                            if (it.id == t.id) it.copy(slowUntil = now + 1000L) else it
                        })
                    }
                    
                    ns = dealDamage(ns, t.id, dmg, now, 0xFFFF4400.toInt())
                }
                
                // D/C-grade Shrapnel
                if (evo >= TDEvolution.D) {
                    val count = if (evo >= TDEvolution.C) 4 else 2
                    val power = if (evo >= TDEvolution.C) 0.7f else 0.5f
                    val extras = st.monsters
                        .filter { m -> m.id != target.id && dist(target.gridCol, target.gridRow, m.gridCol, m.gridRow) <= radius * 2f }
                        .shuffled()
                        .take(count)
                    for (e in extras) {
                        ns = dealDamage(ns, e.id, calcDamage((eff * power).toInt(), e.effectiveArmor), now, 0xFFFFCC00.toInt())
                    }
                }
                
                // S-grade Burn Zone
                if (evo >= TDEvolution.S) {
                    ns = ns.copy(fieldEffects = ns.fieldEffects + TDFieldEffect(
                        id = nextId++, col = target.gridCol, row = target.gridRow,
                        radius = radius * 0.8f, until = now + 2000L, tickAt = now + 500L,
                        dmg = (eff * 0.2f).toInt().coerceAtLeast(1),
                        isNapalm = evo >= TDEvolution.S_PLUS
                    ))
                }
                
                ns
            }
            TDCharType.DUAL_PISTOLS -> {
                val evo = char.evolution
                val probBonus = if (evo >= TDEvolution.A) 5 else 0
                
                // Dojo (D, C, S, S+)
                val dojoThreshold = if (evo >= TDEvolution.S) 7 else 9
                val isDojo = char.rapidFireHits >= dojoThreshold
                
                // Doan (B, S)
                val doanProb = if (evo >= TDEvolution.S) 25 else 15
                val canStun = evo >= TDEvolution.B && (0..99).random() < (doanProb + probBonus)
                
                var dmg = calcDamage(eff, target.effectiveArmor)
                // Crit damage bonus (E)
                if (evo >= TDEvolution.E && (0..99).random() < (15 + probBonus)) dmg = (dmg * 1.5f).toInt()
                
                var ns = dealDamage(base, target.id, dmg, now, 0xFF9C27B0.toInt())
                
                if (canStun) {
                    ns = ns.copy(monsters = ns.monsters.map {
                        if (it.id == target.id) it.copy(stunUntil = now + 500L) else it
                    })
                }
                
                // S+ Dance of Guns: Area attack during Dojo
                if (evo >= TDEvolution.S_PLUS && isDojo) {
                    val extras = st.monsters.filter { it.id != target.id && dist(char.col + 0.5f, char.row + 0.5f, it.gridCol, it.gridRow) <= charRange(char) }.take(2)
                    for (e in extras) ns = dealDamage(ns, e.id, (dmg * 0.5f).toInt(), now, 0xFF9C27B0.toInt())
                }

                ns = ns.copy(characters = ns.characters.map { c ->
                    if (c.id == char.id) {
                        if (isDojo) c.copy(rapidFireHits = 0, rapidFireUntil = now + 3000L)
                        else c.copy(rapidFireHits = c.rapidFireHits + 1)
                    } else c
                })
                ns
            }
            TDCharType.POISONER -> {
                val evo = char.evolution
                val durExt = if (evo >= TDEvolution.E) 2000L else 0L
                var ns = base.copy(monsters = base.monsters.map { m ->
                    if (m.id == target.id) {
                        val stacks = min(m.poisonStacks + 1, 3)
                        val armorMod = if (evo >= TDEvolution.C) 0.15f else 0f
                        m.copy(
                            poisonStacks = stacks, 
                            poisonTickAt = if (m.poisonStacks == 0) now + 500L else m.poisonTickAt,
                            armorBreak = m.armorBreak + armorMod,
                            armorBreakUntil = now + 4000L + durExt
                        )
                    } else m
                })
                
                // Gas Grenade (B, S)
                if (evo >= TDEvolution.B) {
                    val radius = if (evo >= TDEvolution.S) 2.0f else 1.0f
                    val splash = st.monsters.filter { it.id != target.id && dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= radius }
                    ns = ns.copy(monsters = ns.monsters.map { m ->
                        if (splash.any { s -> s.id == m.id }) {
                            val stacks = min(m.poisonStacks + 1, 3)
                            m.copy(poisonStacks = stacks, poisonTickAt = if (m.poisonStacks == 0) now + 500L else m.poisonTickAt)
                        } else m
                    })
                }
                
                // S+ Plague:handled in dealDamage check for on-death explosion
                ns.copy(effects = ns.effects + TDEffect(nextId++, col = target.gridCol, row = target.gridRow, argb = 0xFF4CAF50.toInt()))
            }
            TDCharType.LASER_TURRET -> {
                val evo = char.evolution
                val lcx = char.col.toFloat(); val lcy = char.row.toFloat()
                val newHeat = (char.laserHeat + (if (evo >= TDEvolution.A) 0.2f else 0.1f)).coerceAtMost(1f)
                val heatMax = if (evo >= TDEvolution.C) 1.0f else 0.5f
                val boosted = (eff * (1f + char.laserHeat * heatMax)).toInt()
                
                val lineHits = st.monsters.filter { m ->
                    pointToSegDist(m.gridCol, m.gridRow, lcx, lcy, target.gridCol, target.gridRow) <= (if (evo >= TDEvolution.E) 0.65f else 0.55f)
                }
                
                var ns = base
                for (t in lineHits) {
                    val armor = if (evo >= TDEvolution.S) 0 else t.effectiveArmor
                    var tDmg = calcDamage(boosted, armor)
                    
                    // High Heat (B)
                    if (evo >= TDEvolution.B && (0..99).random() < 20) {
                        ns = ns.copy(monsters = ns.monsters.map {
                            if (it.id == t.id) it.copy(burnStacks = min(it.burnStacks + 1, 5), burnTickAt = now + 500L) else it
                        })
                    }
                    ns = dealDamage(ns, t.id, tDmg, now, 0xFF00E5FF.toInt())
                }
                
                // S+ Destruction Beam: handled by adding a splash effect at target
                if (evo >= TDEvolution.S_PLUS) {
                    ns = ns.copy(effects = ns.effects + TDEffect(nextId++, kind = TDEffectKind.SPLASH, col = target.gridCol, row = target.gridRow, argb = 0xFF00E5FF.toInt(), splashRadius = 1.0f))
                    val splash = ns.monsters.filter { dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= 1.0f }
                    for (s in splash) ns = dealDamage(ns, s.id, boosted / 2, now, 0xFF00E5FF.toInt())
                }
                
                ns.copy(characters = ns.characters.map { c -> if (c.id == char.id) c.copy(laserHeat = newHeat) else c })
            }
            TDCharType.CRYOMANCER -> {
                val evo = char.evolution
                val freezeProb = if (evo >= TDEvolution.S) 20 else 10
                val freezeDur = if (evo >= TDEvolution.S) 2500L else 1500L
                val isFrozen = (0..99).random() < freezeProb && evo >= TDEvolution.D
                
                var dmg = calcDamage(eff, target.effectiveArmor)
                // C-grade: Bonus dmg to frozen
                if (evo >= TDEvolution.C && target.isStunned) dmg = (dmg * 1.5f).toInt()
                
                var ns = dealDamage(base, target.id, dmg, now, 0xFF90CAF9.toInt())
                
                // S+ Absolute Zero: check for map-wide freeze (rare event)
                if (evo >= TDEvolution.S_PLUS && (0..999).random() < 2) {
                    ns = ns.copy(monsters = ns.monsters.map { it.copy(stunUntil = now + 3000L) })
                } else if (isFrozen) {
                    ns = ns.copy(monsters = ns.monsters.map {
                        if (it.id == target.id) it.copy(stunUntil = now + freezeDur) else it
                    })
                }
                
                // B-grade Blizzard
                if (evo >= TDEvolution.B && (0..99).random() < 20) {
                    val splash = ns.monsters.filter { dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= 1.5f }
                    ns = ns.copy(monsters = ns.monsters.map { m ->
                        if (splash.any { s -> s.id == m.id }) m.copy(slowUntil = now + 2000L) else m
                    })
                }

                if (target.bossRound != 90 && target.bossRound != 100) {
                    ns = ns.copy(monsters = ns.monsters.map { m ->
                        if (m.id == target.id) {
                            val mod = if (evo >= TDEvolution.E) 0.6f else 0.5f
                            m.copy(slowUntil = now + (if (evo >= TDEvolution.A) 3000L else 1500L))
                        } else m
                    })
                }
                ns
            }
            TDCharType.CORRUPTOR -> {
                val evo = char.evolution
                val armorRed = if (evo >= TDEvolution.S) 0.5f else (if (evo >= TDEvolution.E) 0.20f else 0.15f)
                val dmg = calcDamage(eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFFFF8C00.toInt())
                ns = ns.copy(monsters = ns.monsters.map { m ->
                    if (m.id == target.id) m.copy(armorBreak = armorRed, armorBreakUntil = now + 3000L) else m
                })
                
                // B-grade: provide armor ignore to nearby allies (logic handled in processAttacks or calcDamage? let's do it in applyAttack ns update)
                // Actually easier to handle in calcDamage, but for now we skip complex aura logic.
                
                // S+ Black Hole
                if (evo >= TDEvolution.S_PLUS && (0..99).random() < 5) {
                    val blackHole = TDFieldEffect(nextId++, target.gridCol, target.gridRow, 2.0f, now + 3000L, now + 500L, (eff * 0.5f).toInt(), isNapalm = true)
                    ns = ns.copy(fieldEffects = ns.fieldEffects + blackHole)
                }
                ns
            }
            TDCharType.REAPER -> {
                val evo = char.evolution
                val execThresh = if (evo >= TDEvolution.S) 0.3f else (if (evo >= TDEvolution.E) 0.20f else 0.15f)
                val isExec = target.hp <= (target.maxHp * execThresh).toInt()
                
                val dmg = if (isExec) target.hp + 9999 else calcDamage(eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFF7B1FA2.toInt())
                
                // B-grade Horror (Stun nearby)
                if (evo >= TDEvolution.B && isExec) {
                    val splash = ns.monsters.filter { dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= 1.2f }
                    ns = ns.copy(monsters = ns.monsters.map { m ->
                        if (splash.any { s -> s.id == m.id }) m.copy(stunUntil = now + 1000L) else m
                    })
                }
                
                // D/C Harvest (Atk Speed buff) - simplified as instant extra turn chance
                if (evo >= TDEvolution.D && isExec && (0..99).random() < 50) {
                    // Logic to reset lastAtkAt to allow immediate next shot
                    ns = ns.copy(characters = ns.characters.map { if (it.id == char.id) it.copy(lastAtkAt = 0L) else it })
                }
                
                ns
            }
            TDCharType.COMMANDER -> {
                // Aura based buff (mostly handled in processAttacks via cmdBonus)
                base
            }
            TDCharType.MERCHANT -> {
                val evo = char.evolution
                val bonus = if (evo >= TDEvolution.C) 2 else 1
                val dmg = calcDamage(eff, target.effectiveArmor)
                
                // S-grade Bribery
                if (evo >= TDEvolution.S && !target.isBoss && (0..99).random() < 1) {
                    // Turn into self-destructing ally: handled as instant kill with splash
                    val ns = dealDamage(base, target.id, target.hp + 9999, now, 0xFFFFD700.toInt())
                    return ns.copy(effects = ns.effects + TDEffect(nextId++, kind = TDEffectKind.SPLASH, col = target.gridCol, row = target.gridRow, argb = 0xFFFFD700.toInt(), splashRadius = 1.5f))
                }
                
                // S+ Jackpot
                if (evo >= TDEvolution.S_PLUS && (0..99).random() < 3) {
                    return dealDamage(base, target.id, target.hp + 9999, now, 0xFFFFD700.toInt()).let { it.copy(gold = it.gold + 50, roundGoldEarned = it.roundGoldEarned + 50) }
                }

                val ns = dealDamage(base, target.id, dmg, now, 0xFFFFF176.toInt())
                if (ns.monsters.none { it.id == target.id } && st.monsters.any { it.id == target.id })
                    ns.copy(gold = ns.gold + bonus, roundGoldEarned = ns.roundGoldEarned + bonus)
                else ns
            }
            TDCharType.ECLIPSE -> {
                val evo = char.evolution
                val probBonus = if (evo >= TDEvolution.A) 5 else 0
                val hits = char.precisionHits
                val threshold = if (evo >= TDEvolution.B) 3 else 4
                val isProc = hits >= threshold - 1
                
                var dmg = calcDamage(eff, target.effectiveArmor)
                var ns = dealDamage(base, target.id, dmg, now, 0xFFE0E0E0.toInt())
                
                // S-grade Singularity: Current HP % dmg to nearby
                if (evo >= TDEvolution.S) {
                    val splash = ns.monsters.filter { dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= 1.5f }
                    for (s in splash) ns = dealDamage(ns, s.id, (s.hp * 0.01f).toInt().coerceAtLeast(1), now, 0xFF64FFDA.toInt())
                }
                
                // D/C/B Proc effects
                if (isProc) {
                    // Lunar (D, B): Slow nearby
                    if (evo >= TDEvolution.D) {
                        val splash = ns.monsters.filter { dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= 2.0f }
                        ns = ns.copy(monsters = ns.monsters.map { m ->
                            if (splash.any { it.id == m.id }) m.copy(slowUntil = now + 1500L) else m
                        })
                    }
                    // Solar (C, B): Burn nearby (Armor Ignore)
                    if (evo >= TDEvolution.C) {
                        val splash = ns.monsters.filter { dist(target.gridCol, target.gridRow, it.gridCol, it.gridRow) <= 1.5f }
                        for (s in splash) ns = dealDamage(ns, s.id, (eff * 0.5f).toInt(), now, 0xFFFF3D00.toInt())
                    }
                    ns = ns.copy(effects = ns.effects + TDEffect(nextId++, kind = TDEffectKind.SPLASH, col = target.gridCol, row = target.gridRow, argb = 0xFF64FFDA.toInt(), splashRadius = 2.0f))
                }
                
                // S+ Ultimate: Event Horizon (Black Hole)
                if (evo >= TDEvolution.S_PLUS && (0..99).random() < (3 + probBonus)) {
                    val bh = TDFieldEffect(nextId++, target.gridCol, target.gridRow, 2.5f, now + 2000L, now + 500L, (eff * 0.8f).toInt(), isNapalm = true)
                    ns = ns.copy(fieldEffects = ns.fieldEffects + bh, monsters = ns.monsters.map { it.copy(stunUntil = now + 2000L) })
                }
                
                ns.copy(characters = ns.characters.map { c ->
                    if (c.id == char.id) c.copy(precisionHits = if (isProc) 0 else hits + 1) else c
                })
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
                effects = st.effects + TDEffect(nextId++, col = m.gridCol, row = m.gridRow, argb = argb, damage = dmg),
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
                effects = st.effects + TDEffect(nextId++, col = m.gridCol, row = m.gridRow, argb = argb, progress = 0.6f, damage = dmg)
            )
        }
    }

    private fun commanderBonus(char: TDCharacter, all: List<TDCharacter>): Float =
        all.filter { it.type == TDCharType.COMMANDER }
            .count { cmd -> dist(char.col.toFloat(), char.row.toFloat(), cmd.col.toFloat(), cmd.row.toFloat()) <= charRange(cmd) }
            .let { it * 0.10f }

    private fun endRound(st: TDUiState) {
        gameJob?.cancel(); spawnJob?.cancel(); spawnQueue.clear()
        
        val merchants = st.characters.filter { it.type == TDCharType.MERCHANT }
        val intrBonus = merchants.filter { it.evolution >= TDEvolution.E }.size * 0.10f
        val maxIntr = if (merchants.any { it.evolution >= TDEvolution.A }) 70 else 50
        
        val baseIntr = interest(st.gold)
        val intr = min((baseIntr * (1f + intrBonus)).toInt(), maxIntr)
        
        // B-grade Merchant Investment
        val investBonus = if (merchants.any { it.evolution >= TDEvolution.B }) (st.gold * 0.02f).toInt() else 0
        
        val clr = clearBonus(st.round)
        val result = TDRoundResult(st.killedCount, st.escapedCount, st.roundGoldEarned, intr + investBonus, clr)
        
        _state.update {
            it.copy(
                phase = if (it.round >= 100) TDPhase.GAME_OVER else TDPhase.ROUND_END,
                gold = it.gold + intr + investBonus + clr,
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
                level = c.level, star = c.star, evolutionGrade = c.evolution.name,
                baseAtkMult = c.baseAtkMult, totalInvested = c.totalInvested)
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
    TDCharType.COMMANDER    -> "주변 아군 공격력/공속 버프"
    TDCharType.MERCHANT     -> "처치 시 추가 골드 획득"
    TDCharType.ECLIPSE      -> "빛과 어둠의 하이브리드 지배자"
    }

private fun tdCharSkills(type: TDCharType): List<Pair<TDEvolution, String>> = when (type) {
    TDCharType.SNIPER -> listOf(
        TDEvolution.F to "[기본] 긴 사거리 단발 사격",
        TDEvolution.E to "[패시브] 치명타 확률 10% 증가",
        TDEvolution.D to "(1차) 정밀 사격: 3타마다 1.5배 피해",
        TDEvolution.C to "[강화] 정밀 2배 피해 + 방어 10% 무시",
        TDEvolution.B to "(2차) 마취탄: 5% 확률 1초 기절",
        TDEvolution.A to "[스탯] 공속 20%↑, 발동 확률 5%↑",
        TDEvolution.S to "[마스터] 정밀 2타마다, 기절 2초",
        TDEvolution.S_PLUS to "(궁극기) 헤드샷: 2% 즉사(보스 제외)"
    )
    TDCharType.ARTILLERY -> listOf(
        TDEvolution.F to "[기본] 광역 포탄 사격",
        TDEvolution.E to "[패시브] 폭발 반경 15% 증가",
        TDEvolution.D to "(1차) 파편탄: 주변 2명 50% 피해",
        TDEvolution.C to "[강화] 파편 4명 + 피해 70%",
        TDEvolution.B to "(2차) 충격파: 이속 1초 20% 감소",
        TDEvolution.A to "[스탯] 공격력 30%↑, 투사체 속도 2배",
        TDEvolution.S to "[마스터] 폭발 자리 2초 화상 장판",
        TDEvolution.S_PLUS to "(궁극기) 네이팜: 방어무시 지대 생성"
    )
    TDCharType.DUAL_PISTOLS -> listOf(
        TDEvolution.F to "[기본] 근거리 고속 연사",
        TDEvolution.E to "[패시브] 치명타 피해 50% 추가",
        TDEvolution.D to "(1차) 도조: 10타마다 3초간 공속 2배",
        TDEvolution.C to "[강화] 도조 발동 시 이동속도 1.5배",
        TDEvolution.B to "(2차) 도안: 15% 확률 0.5초 기절",
        TDEvolution.A to "[스탯] 사거리 1칸 증가, 공속 10%↑",
        TDEvolution.S to "[마스터] 도조 8타마다, 도안 확률 25%",
        TDEvolution.S_PLUS to "(궁극기) 총의 춤: 도조 시 광역 공격"
    )
    TDCharType.POISONER -> listOf(
        TDEvolution.F to "[기본] 독 투사체 발사",
        TDEvolution.E to "[패시브] 독 지속시간 2초 연장",
        TDEvolution.D to "(1차) 맹독: 독 데미지 2배 강화",
        TDEvolution.C to "[강화] 독 중첩 시 적 방어력 15% 감소",
        TDEvolution.B to "(2차) 가스탄: 폭발 시 주변 독 전염",
        TDEvolution.A to "[스탯] 기본 공격력 25%↑, 사거리 0.5칸↑",
        TDEvolution.S to "[마스터] 가스 폭발 범위 2배 증가",
        TDEvolution.S_PLUS to "(궁극기) 전염병: 처치 시 주변 독 폭발"
    )
    TDCharType.LASER_TURRET -> listOf(
        TDEvolution.F to "[기본] 관통 레이저 발사",
        TDEvolution.E to "[패시브] 레이저 굵기(판정) 20% 증가",
        TDEvolution.D to "(1차) 가열: 쏠수록 데미지 최대 50%↑",
        TDEvolution.C to "[강화] 가열 한도 100%로 증가",
        TDEvolution.B to "(2차) 고열: 타격 시 20% 확률 2초 화상",
        TDEvolution.A to "[스탯] 사거리 1.5칸↑, 가열 속도 2배",
        TDEvolution.S to "[마스터] 레이저 타격 시 적 방어무시",
        TDEvolution.S_PLUS to "(궁극기) 파괴광선: 타겟 지점 대폭발"
    )
    TDCharType.CRYOMANCER -> listOf(
        TDEvolution.F to "[기본] 둔화 마법 발사",
        TDEvolution.E to "[패시브] 둔화 효과 10% 추가",
        TDEvolution.D to "(1차) 빙결: 10% 확률 1.5초 완전 정지",
        TDEvolution.C to "[강화] 빙결된 적에게 데미지 1.5배",
        TDEvolution.B to "(2차) 눈보라: 5타마다 주변 광역 둔화",
        TDEvolution.A to "[스탯] 공속 25%↑, 둔화 지속 2배",
        TDEvolution.S to "[마스터] 빙결 확률 20%, 정지 2.5초",
        TDEvolution.S_PLUS to "(궁극기) 절대영도: 맵 전체 3초 정지"
    )
    TDCharType.CORRUPTOR -> listOf(
        TDEvolution.F to "[기본] 방어력 약화 탄환",
        TDEvolution.E to "[패시브] 방어 감소 수치 5% 증가",
        TDEvolution.D to "(1차) 부패: 방어 감소 시 도트 피해",
        TDEvolution.C to "[강화] 부패 피해 3배로 증가",
        TDEvolution.B to "(2차) 오염: 주변 아군 방무 5% 부여",
        TDEvolution.A to "[스탯] 사거리 1칸↑, 공속 15%↑",
        TDEvolution.S to "[마스터] 방어 감소 50%로 고정 적용",
        TDEvolution.S_PLUS to "(궁극기) 심연의 구멍: 블랙홀 생성"
    )
    TDCharType.REAPER -> listOf(
        TDEvolution.F to "[기본] 낮은 체력 적 우선 처형",
        TDEvolution.E to "[패시브] 처형 기준 HP 20%로 상향",
        TDEvolution.D to "(1차) 수확: 처치 시 공속 5%↑(최대 50%)",
        TDEvolution.C to "[강화] 수확 한도 100%로 증가",
        TDEvolution.B to "(2차) 공포: 타격 시 주변 적 1초 경직",
        TDEvolution.A to "[스탯] 기본 공격력 50%↑, 이속 2배",
        TDEvolution.S to "[마스터] 처형 기준 HP 30%로 상향",
        TDEvolution.S_PLUS to "(궁극기) 영혼 수확: 처치 시 골드 2배"
    )
    TDCharType.COMMANDER -> listOf(
        TDEvolution.F to "[기본] 주변 아군 공격력 10% 버프",
        TDEvolution.E to "[패시브] 버프 범위 15% 증가",
        TDEvolution.D to "(1차) 격려: 주변 아군 공속 10% 추가",
        TDEvolution.C to "[강화] 공격력 버프 20%로 상향",
        TDEvolution.B to "(2차) 보급: 주변 아군 스킬확률 +3%",
        TDEvolution.A to "[스탯] 버프 범위 2배, 자신의 공속 30%↑",
        TDEvolution.S to "[마스터] 공속 버프 20%, 스킬확률 +7%",
        TDEvolution.S_PLUS to "(궁극기) 총공격: 전맵 아군 5초 폭주"
    )
    TDCharType.MERCHANT -> listOf(
        TDEvolution.F to "[기본] 적 처치 시 1G 추가 획득",
        TDEvolution.E to "[패시브] 라운드 이자 10% 증가",
        TDEvolution.D to "(1차) 할인: 캐릭터 구매비용 10% 감소",
        TDEvolution.C to "[강화] 처치 시 추가 골드 2G로 상향",
        TDEvolution.B to "(2차) 투자: 매 라운드 보유골드 2% 보너스",
        TDEvolution.A to "[스탯] 이자 최대 한도 70G로 상향",
        TDEvolution.S to "[마스터] 1% 확률 적을 아군 자폭병으로",
        TDEvolution.S_PLUS to "(궁극기) 잭팟: 3% 확률 즉사(황금 동상)"
    )
    TDCharType.ECLIPSE -> listOf(
        TDEvolution.F to "[기본] 빛과 어둠의 구체 발사",
        TDEvolution.E to "[패시브] 공격 속도 15% 증가",
        TDEvolution.D to "(1차) 루나: 4타마다 주변 둔화",
        TDEvolution.C to "(2차) 솔라: 4타마다 방무 폭발",
        TDEvolution.B to "[패시브] 천체균형: 3타마다 동시발동",
        TDEvolution.A to "[스탯] 공격 40%↑, 사거리 1.5칸↑",
        TDEvolution.S to "[마스터] 타격 시 주변 적 현재HP 1% 피해",
        TDEvolution.S_PLUS to "(궁극기) 이벤트호라이즌: 3% 블랙홀"
    )
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
                TDPhase.ROUND_SELECT -> TDRoundSelectContent(state.unlockedRounds, state.gold, state.unlockedRounds, vm::selectRound, vm::watchAd, vm::toggleCharGuide)
                TDPhase.PREP, TDPhase.PLAYING -> TDGameContent(state, vm)
                TDPhase.ROUND_END -> TDRoundEndContent(state, vm::startNextRound, vm::replayRound, vm::goToRoundSelect)
                TDPhase.GAME_OVER -> TDGameOverContent(state, vm::goToRoundSelect)
            }

            if (state.showCharGuide) {
                TDCharGuideDialog(onDismiss = vm::toggleCharGuide)
            }
        }
    }
}

// ─── Round Select ─────────────────────────────────────────────────────────────

@Composable
private fun TDRoundSelectContent(
    unlockedRounds: Int,
    gold: Int,
    currentRound: Int,
    onSelect: (Int) -> Unit,
    onWatchAd: () -> Unit,
    onShowGuide: () -> Unit,
) {
    val reward = adReward(currentRound)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "💰 ${gold}G",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = Color(0xFFFFD700.toInt())
                    )
                    OutlinedButton(
                        onClick = onShowGuide,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("📖 도감", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = onWatchAd,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("+${reward}G  📺 광고", style = MaterialTheme.typography.labelSmall)
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
                tdDrawFieldEffects(cs, ox, oy, state)
                tdDrawCharacters(cs, ox, oy, state)
                tdDrawMonsters(cs, ox, oy, state)
                tdDrawEffects(cs, ox, oy, state)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${THEME_NAMES[themeIdx]}${if (isBoss) "👑" else ""} [${state.killedCount}/${state.totalMonsters}]",
                        style = MaterialTheme.typography.bodySmall, color = THEME_COLORS[themeIdx],
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = vm::toggleSpeed,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("${state.gameSpeed.toInt()}×", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = vm::toggleDamage,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(if (state.showDamage) "DMG ON" else "DMG OFF", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = vm::toggleCharGuide,
                    modifier = Modifier.height(28.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Text("📖 도감", style = MaterialTheme.typography.labelSmall)
                }
                Text("💰 ${state.gold}G", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700.toInt()))
            }
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

private fun DrawScope.tdDrawFieldEffects(cs: Float, ox: Float, oy: Float, state: TDUiState) {
    for (fe in state.fieldEffects) {
        val cx = ox + fe.col * cs + cs * 0.5f
        val cy = oy + fe.row * cs + cs * 0.5f
        val r = fe.radius * cs
        
        // Base Zone
        val color = if (fe.isNapalm) Color(0x88FF3D00.toInt()) else Color(0x66FF9800.toInt())
        drawCircle(color, r, Offset(cx, cy))
        
        // Dynamic Fire/Lava effects
        val pulse = (System.currentTimeMillis() % 1000) / 1000f
        if (fe.isNapalm) {
            // Hot Napalm/Lava
            repeat(3) { i ->
                val angle = (pulse * 2 * PI + i * 2 * PI / 3).toFloat()
                val offR = r * 0.4f
                drawCircle(
                    Color(0xCCFFD600.toInt()), 
                    r * 0.25f, 
                    Offset(cx + cos(angle) * offR, cy + sin(angle) * offR)
                )
            }
            drawCircle(Color(0x66FFFFFF), r * 0.15f * (1f + pulse), Offset(cx, cy))
        } else {
            // Burn Zone
            drawCircle(Color(0x44FFEB3B.toInt()), r * 0.3f * pulse, Offset(cx, cy))
        }
    }
}

private fun DrawScope.tdDrawCharacters(cs: Float, ox: Float, oy: Float, state: TDUiState) {
    val now = System.currentTimeMillis()
    for (c in state.characters) {
        val cx = ox + (c.col + 0.5f + c.recoilOffset.x) * cs; val cy = oy + (c.row + 0.5f + c.recoilOffset.y) * cs
        val r = cs * 0.38f
        
        // --- Ground Plate (Grade-based) ---
        tdDrawGradePlate(cx, cy, r, c.evolution)

        if (c.type == TDCharType.DUAL_PISTOLS && System.currentTimeMillis() < c.rapidFireUntil)
            drawCircle(Color(0x669C27B0.toInt()), r * 1.4f, Offset(cx, cy))
        
        tdDrawCharBody(c.type, cx, cy, r, c.targetAngle, c.evolution)
        
        // Muzzle Flash / Magic Firing Aura
        if (now < c.muzzleFlashUntil) {
            val flashColor = if (c.type.isPhysical) Color(0xFFFFF176.toInt()) else Color(0xFF80D8FF.toInt())
            rotate(c.targetAngle, pivot = Offset(cx, cy)) {
                drawCircle(flashColor.copy(alpha = 0.8f), r * 0.5f, Offset(cx + r * 1.8f, cy))
                drawCircle(Color.White, r * 0.25f, Offset(cx + r * 1.8f, cy))
            }
        }
        
        // Star rating display (bottom of character)
        val starOutline = Color.Black.copy(alpha = 0.4f)
        repeat(c.star) { i ->
            val starCount = c.star
            val starSize = cs * 0.08f
            val spacing = cs * 0.12f
            val startX = cx - (starCount - 1) * spacing / 2f
            val sx = startX + i * spacing
            val sy = cy + r * 0.9f
            drawCircle(Color(0xFFFFD700.toInt()), starSize, Offset(sx, sy))
            drawCircle(starOutline, starSize, Offset(sx, sy), style = Stroke(1f))
        }
        
        if (c.level > 1) {
            // Level indicator (top right)
            drawCircle(Color(0xFFE0E0E0.toInt()), r * 0.22f, Offset(cx + r * 0.75f, cy - r * 0.75f))
            drawCircle(starOutline, r * 0.22f, Offset(cx + r * 0.75f, cy - r * 0.75f), style = Stroke(1f))
        }
    }
}

private fun DrawScope.tdDrawGradePlate(cx: Float, cy: Float, r: Float, evolution: TDEvolution) {
    val time = (System.currentTimeMillis() % 2000) / 2000f
    val pulse = sin(time * 2 * PI.toFloat())
    
    val plateColor = when (evolution) {
        TDEvolution.F -> Color.LightGray.copy(alpha = 0.3f)
        TDEvolution.E -> Color(0xFF81C784).copy(alpha = 0.4f) // Green
        TDEvolution.D -> Color(0xFF4FC3F7).copy(alpha = 0.4f) // Blue
        TDEvolution.C -> Color(0xFF9575CD).copy(alpha = 0.4f) // Purple
        TDEvolution.B -> Color(0xFFFFB74D).copy(alpha = 0.5f) // Orange
        TDEvolution.A -> Color(0xFFE57373).copy(alpha = 0.5f) // Red
        TDEvolution.S -> Color(0xFFFFD54F).copy(alpha = 0.6f) // Gold
        TDEvolution.S_PLUS -> Color(0xFFF06292).copy(alpha = 0.7f) // Pink/Magenta
    }
    
    val plateRadius = when (evolution) {
        TDEvolution.F -> r * 1.05f
        TDEvolution.E -> r * 1.15f
        TDEvolution.D -> r * 1.25f
        TDEvolution.C -> r * 1.35f
        TDEvolution.B -> r * 1.45f
        TDEvolution.A -> r * 1.55f
        TDEvolution.S -> r * (1.7f + 0.05f * pulse)
        TDEvolution.S_PLUS -> r * (1.9f + 0.1f * pulse)
    }

    // Main plate
    drawCircle(plateColor, plateRadius, Offset(cx, cy))
    
    // Outline
    drawCircle(
        color = plateColor.copy(alpha = 0.8f),
        radius = plateRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )

    // Fancy effects for higher grades
    if (evolution >= TDEvolution.A) {
        // Outer faint ring
        drawCircle(
            color = plateColor.copy(alpha = 0.2f),
            radius = plateRadius * 1.2f,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )
    }

    if (evolution >= TDEvolution.S) {
        // Inner glowing ring
        drawCircle(
            color = Color.White.copy(alpha = 0.4f + 0.1f * pulse),
            radius = plateRadius * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f + pulse)
        )
    }

    if (evolution == TDEvolution.S_PLUS) {
        // Extra magic rings for S+
        rotate(time * 360f, pivot = Offset(cx, cy)) {
            repeat(3) { i ->
                rotate(i * 120f, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = plateColor.copy(alpha = 0.3f),
                        radius = plateRadius * 1.1f,
                        center = Offset(cx + plateRadius * 0.1f, cy),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
        
        // Final outermost pulse
        drawCircle(
            color = plateColor.copy(alpha = 0.15f * (1f - time)),
            radius = plateRadius * (1f + time),
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.tdDrawCharBody(type: TDCharType, cx: Float, cy: Float, r: Float, angle: Float, evolution: TDEvolution) {
    val body    = Color(type.charColor)
    val dark    = body.copy(alpha = 0.9f)
    val outline = Color(0xFF000000.toInt()).copy(alpha = 0.65f)
    
    // Universal evolution glow for A-S+ grades
    if (evolution >= TDEvolution.A) {
        val glowColor = if (evolution >= TDEvolution.S) Color(0xFFFFD600.toInt()).copy(alpha = 0.3f) 
                        else Color.White.copy(alpha = 0.2f)
        drawCircle(glowColor, r * 1.5f, Offset(cx, cy))
    }
    
    when (type) {
        TDCharType.SNIPER -> {
            val cloakColor = when {
                evolution >= TDEvolution.S -> Color(0xFF1B5E20.toInt())
                evolution >= TDEvolution.C -> Color(0xFF2E7D32.toInt())
                else -> Color(0xFF43A047.toInt())
            }
            drawCircle(cloakColor, r * 1.1f, Offset(cx, cy))
            if (evolution >= TDEvolution.C) {
                drawCircle(cloakColor.copy(alpha = 0.8f), r * 0.5f, Offset(cx - r * 0.7f, cy))
                drawCircle(cloakColor.copy(alpha = 0.8f), r * 0.5f, Offset(cx + r * 0.7f, cy))
            }
            drawCircle(Color(0xFFFFCCBC.toInt()), r * 0.6f, Offset(cx, cy - r * 0.1f))
            rotate(angle, pivot = Offset(cx, cy)) {
                val rifleColor = if (evolution >= TDEvolution.S) Color(0xFF212121.toInt()) else Color(0xFF3E2723.toInt())
                val rifleLen = r * (if (evolution >= TDEvolution.B) 2.6f else 2.2f)
                drawLine(rifleColor, Offset(cx + r * 0.3f, cy), Offset(cx + rifleLen, cy), strokeWidth = r * (if (evolution >= TDEvolution.S) 0.3f else 0.2f))
                drawRect(Color(0xFF212121.toInt()), topLeft = Offset(cx + r * 0.8f, cy - r * (if (evolution >= TDEvolution.D) 0.55f else 0.45f)), 
                    size = Size(r * (if (evolution >= TDEvolution.D) 0.8f else 0.5f), r * 0.15f))
            }
            val hatPath = Path().apply {
                moveTo(cx - r * 1.0f, cy - r * 0.3f); lineTo(cx + r * 1.0f, cy - r * 0.3f)
                lineTo(cx + r * 0.6f, cy - r * 0.8f); lineTo(cx - r * 0.6f, cy - r * 0.8f); close()
            }
            drawPath(hatPath, if (evolution >= TDEvolution.S) Color.Black else Color(0xFF4E342E.toInt()))
            if (evolution >= TDEvolution.S_PLUS) drawCircle(Color(0xFFFFD600.toInt()), r * 0.2f, Offset(cx, cy - r * 0.9f))
            drawCircle(Color.Black, r * 0.12f, Offset(cx + r * 0.25f, cy - r * 0.2f))
            drawCircle(outline, r * 1.1f, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.ARTILLERY -> {
            val armorColor = when {
                evolution >= TDEvolution.S -> Color(0xFF212121.toInt())
                evolution >= TDEvolution.C -> Color(0xFF37474F.toInt())
                else -> Color(0xFF455A64.toInt())
            }
            drawCircle(armorColor, r * 1.15f, Offset(cx, cy))
            if (evolution >= TDEvolution.S) {
                repeat(4) { i ->
                    rotate(i * 90f + 45f, pivot = Offset(cx, cy)) {
                        drawLine(Color.Gray, Offset(cx + r * 0.8f, cy), Offset(cx + r * 1.4f, cy), strokeWidth = r * 0.2f)
                    }
                }
            }
            rotate(angle, pivot = Offset(cx, cy)) {
                val cannonColor = if (evolution >= TDEvolution.S) Color.Black else Color(0xFF212121.toInt())
                val barrelLen = r * (if (evolution >= TDEvolution.C) 2.2f else 1.8f)
                if (evolution >= TDEvolution.S_PLUS) {
                    drawRect(cannonColor, topLeft = Offset(cx - r * 0.1f, cy - r * 0.6f), size = Size(barrelLen, r * 0.5f))
                    drawRect(cannonColor, topLeft = Offset(cx - r * 0.1f, cy + r * 0.1f), size = Size(barrelLen, r * 0.5f))
                } else {
                    drawRect(cannonColor, topLeft = Offset(cx - r * 0.1f, cy - r * 0.35f), size = Size(barrelLen, r * 0.7f))
                }
                drawRect(if (evolution >= TDEvolution.S) Color(0xFFFF3D00.toInt()) else Color(0xFFFF6D00.toInt()), 
                    topLeft = Offset(cx + r * 0.3f, cy - r * 0.1f), size = Size(r * 0.6f, r * 0.15f))
            }
            drawCircle(Color(0xFF37474F.toInt()), r * 0.6f, Offset(cx - r * 0.2f, cy))
            drawCircle(outline, r * 1.15f, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.DUAL_PISTOLS -> {
            val suitColor = if (evolution >= TDEvolution.S) Color(0xFF311B92.toInt()) else Color(0xFF7B1FA2.toInt())
            drawCircle(suitColor, r, Offset(cx, cy))
            if (evolution >= TDEvolution.D) drawRect(Color(0xFFB71C1C.toInt()), topLeft = Offset(cx - r * 0.8f, cy + r * 0.2f), size = Size(r * 1.6f, r * 0.3f))
            drawCircle(Color(0xFFFFCCBC.toInt()), r * 0.55f, Offset(cx, cy))
            rotate(angle, pivot = Offset(cx, cy)) {
                val gunLen = r * (if (evolution >= TDEvolution.C) 1.5f else 1.1f)
                drawRect(Color(0xFF263238.toInt()), topLeft = Offset(cx + r * 0.2f, cy - r * 0.55f), size = Size(gunLen, r * 0.3f))
                drawRect(Color(0xFF263238.toInt()), topLeft = Offset(cx + r * 0.2f, cy + r * 0.25f), size = Size(gunLen, r * 0.3f))
            }
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.POISONER -> {
            drawCircle(if (evolution >= TDEvolution.S) Color(0xFF1A237E.toInt()) else Color(0xFF1B5E20.toInt()), r, Offset(cx, cy))
            drawCircle(if (evolution >= TDEvolution.S) Color(0xFF311B92.toInt()) else Color(0xFF4A148C.toInt()), r * 0.75f, Offset(cx, cy + r * 0.2f))
            val bubbleColor = if (evolution >= TDEvolution.S) Color(0xFFB2FF59.toInt()) else Color(0xFF00E676.toInt())
            drawCircle(bubbleColor, r * 0.3f, Offset(cx - r * 0.2f, cy + r * 0.1f))
            if (evolution >= TDEvolution.D) drawCircle(bubbleColor, r * 0.2f, Offset(cx + r * 0.3f, cy + r * 0.3f))
            rotate(angle, pivot = Offset(cx, cy)) {
                drawLine(if (evolution >= TDEvolution.C) Color(0xFF212121.toInt()) else Color(0xFF5D4037.toInt()), Offset(cx, cy), Offset(cx + r * 1.6f, cy), strokeWidth = r * 0.2f)
                drawCircle(bubbleColor, r * 0.35f, Offset(cx + r * 1.6f, cy))
            }
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.LASER_TURRET -> {
            drawRect(if (evolution >= TDEvolution.S) Color.Black else Color(0xFF37474F.toInt()), topLeft = Offset(cx - r, cy - r), size = Size(r * 2f, r * 2f), style = Stroke(r * (if (evolution >= TDEvolution.C) 0.4f else 0.3f)))
            drawCircle(Color(0xFF00E5FF.toInt()).copy(alpha = 0.6f), r * (if (evolution >= TDEvolution.S) 0.7f else 0.5f), Offset(cx, cy))
            rotate(angle, pivot = Offset(cx, cy)) {
                val bLen = r * (if (evolution >= TDEvolution.B) 2.8f else 2.2f)
                drawRect(Color(0xFF212121.toInt()), topLeft = Offset(cx, cy - r * 0.35f), size = Size(bLen, r * 0.7f))
                if (evolution >= TDEvolution.S_PLUS) {
                    drawRect(Color(0xFF00E5FF.toInt()), topLeft = Offset(cx + bLen - r * 0.4f, cy - r * 0.5f), size = Size(r * 0.4f, r * 0.3f))
                    drawRect(Color(0xFF00E5FF.toInt()), topLeft = Offset(cx + bLen - r * 0.4f, cy + r * 0.2f), size = Size(r * 0.4f, r * 0.3f))
                }
            }
            drawCircle(outline, r * 1.1f, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.CRYOMANCER -> {
            drawCircle(Color(0xFF0D47A1.toInt()), r, Offset(cx, cy))
            val hPath = Path().apply { moveTo(cx - r * 1.1f, cy); lineTo(cx + r * 1.1f, cy); lineTo(cx, cy - r * (if (evolution >= TDEvolution.C) 1.8f else 1.4f)); close() }
            drawPath(hPath, if (evolution >= TDEvolution.S) Color(0xFF01579B.toInt()) else Color(0xFF1976D2.toInt()))
            rotate(angle, pivot = Offset(cx, cy)) {
                val sLen = r * (if (evolution >= TDEvolution.B) 2.2f else 1.8f)
                drawLine(Color(0xFFE3F2FD.toInt()), Offset(cx, cy), Offset(cx + sLen, cy), strokeWidth = r * 0.2f)
                drawCircle(Color(0xFF80D8FF.toInt()), r * (if (evolution >= TDEvolution.S) 0.6f else 0.4f), Offset(cx + sLen, cy))
            }
            drawCircle(outline, r, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.CORRUPTOR -> {
            drawCircle(Color(0xFF311B92.toInt()), r * 1.1f, Offset(cx, cy))
            val tCount = if (evolution >= TDEvolution.S) 8 else (if (evolution >= TDEvolution.C) 6 else 4)
            repeat(tCount) { i ->
                rotate(i * (360f / tCount) + (System.currentTimeMillis() % 2000) / 2000f * 360f, pivot = Offset(cx, cy)) {
                    val tLen = r * (if (evolution >= TDEvolution.S) 1.6f else 1.3f)
                    drawLine(Color(0xFFB00020.toInt()), Offset(cx + r * 0.5f, cy), Offset(cx + tLen, cy), strokeWidth = r * 0.25f)
                    drawCircle(Color(0xFFB00020.toInt()), r * 0.15f, Offset(cx + tLen, cy))
                }
            }
            drawCircle(Color.Black, r * 0.5f, Offset(cx, cy))
            drawCircle(outline, r * 1.1f, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.REAPER -> {
            drawCircle(Color(0xFF212121.toInt()), r * 1.1f, Offset(cx, cy))
            if (evolution >= TDEvolution.S) drawCircle(Color(0xFF4A148C.toInt()).copy(alpha = 0.5f), r * 1.4f, Offset(cx, cy), style = Stroke(2f))
            rotate(angle, pivot = Offset(cx, cy)) {
                val sLen = r * (if (evolution >= TDEvolution.C) 2.5f else 2.0f)
                val bLen = r * (if (evolution >= TDEvolution.S) 2.0f else 1.5f)
                val sPath = Path().apply { moveTo(cx, cy); lineTo(cx + sLen, cy); lineTo(cx + sLen - r * 0.8f, cy + bLen) }
                drawPath(sPath, Color(0xFF757575.toInt()), style = Stroke(r * 0.25f))
            }
            drawCircle(Color(0xFFB71C1C.toInt()), r * 0.15f, Offset(cx - r * 0.3f, cy - r * 0.2f))
            drawCircle(outline, r * 1.1f, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.COMMANDER -> {
            drawCircle(if (evolution >= TDEvolution.S) Color(0xFF1A237E.toInt()) else Color(0xFF0D47A1.toInt()), r * 1.2f, Offset(cx, cy))
            if (evolution >= TDEvolution.D) {
                val cPath = Path().apply { moveTo(cx - r * 1.2f, cy); lineTo(cx + r * 1.2f, cy); lineTo(cx + r * 1.5f, cy + r * 1.2f); lineTo(cx - r * 1.5f, cy + r * 1.2f); close() }
                drawPath(cPath, Color(0xFFB71C1C.toInt()))
            }
            val hPath = Path().apply { moveTo(cx - r * 1.1f, cy - r * 0.2f); lineTo(cx + r * 1.1f, cy - r * 0.2f); lineTo(cx + r * 0.2f, cy - r * 1.2f); lineTo(cx - r * 0.2f, cy - r * 1.2f); close() }
            drawPath(hPath, if (evolution >= TDEvolution.S) Color.Black else Color(0xFF1A237E.toInt()))
            val fHeight = if (evolution >= TDEvolution.C) 2.4f else 1.8f
            drawLine(Color(0xFFF57F17.toInt()), Offset(cx + r * 0.4f, cy), Offset(cx + r * 0.4f, cy - r * fHeight), strokeWidth = r * 0.15f)
            drawRect(Color(0xFFD32F2F.toInt()), topLeft = Offset(cx + r * 0.4f, cy - r * fHeight), size = Size(r * (if (evolution >= TDEvolution.S) 1.8f else 1.2f), r * 0.7f))
            drawCircle(outline, r * 1.2f, Offset(cx, cy), style = Stroke(2f))
        }
        TDCharType.MERCHANT -> {
            drawCircle(Color(0xFFFFD600.toInt()), r * 1.1f, Offset(cx, cy))
            val cColor = if (evolution >= TDEvolution.S) Color(0xFF3E2723.toInt()) else Color(0xFF795548.toInt())
            drawRect(cColor, topLeft = Offset(cx - r * 0.8f, cy + r * 0.4f), size = Size(r * 1.6f, r * 0.8f))
            if (evolution >= TDEvolution.S) drawRect(Color(0xFFFFD600.toInt()), topLeft = Offset(cx - r * 0.8f, cy + r * 0.4f), size = Size(r * 1.6f, r * 0.15f))
            drawCircle(Color.Black, r * 0.25f, Offset(cx - r * 0.5f, cy + r * 1.1f))
            drawCircle(Color.Black, r * 0.25f, Offset(cx + r * 0.5f, cy + r * 1.1f))
            if (evolution >= TDEvolution.S) drawRect(Color(0xFF5D4037.toInt()), topLeft = Offset(cx - r * 0.4f, cy - r * 0.6f), size = Size(r * 0.8f, r * 0.5f))
            else drawCircle(Color(0xFFF57F17.toInt()), r * 0.4f, Offset(cx, cy - r * 0.2f))
            drawCircle(outline, r * 1.1f, Offset(cx, cy), style = Stroke(1.5f))
        }
        TDCharType.ECLIPSE -> {
            rotate(angle, pivot = Offset(cx, cy)) {
                drawArc(Color.White, startAngle = 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - r, cy - r), size = Size(r * 2f, r * 2f))
                drawArc(Color(0xFF212121.toInt()), startAngle = 270f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - r, cy - r), size = Size(r * 2f, r * 2f))
            }
            val cColor = when { evolution >= TDEvolution.S -> Color(0xFFFFEB3B.toInt()); evolution >= TDEvolution.C -> Color(0xFF64FFDA.toInt()); else -> Color.Gray }
            drawCircle(cColor, r * 0.3f, Offset(cx, cy))
            val rot = (System.currentTimeMillis() % 3000) / 3000f * 360f
            val oCount = if (evolution >= TDEvolution.B) 4 else 2
            rotate(rot, pivot = Offset(cx, cy)) {
                repeat(oCount) { i ->
                    rotate(i * (360f / oCount), pivot = Offset(cx, cy)) {
                        drawCircle(if (i % 2 == 0) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f), r * 0.18f, Offset(cx + r * 1.4f, cy))
                    }
                }
            }
            if (evolution >= TDEvolution.S_PLUS) {
                drawCircle(Color.White.copy(alpha = 0.2f), r * 1.8f, Offset(cx, cy), style = Stroke(2f))
                drawCircle(Color.Black.copy(alpha = 0.2f), r * 2.1f, Offset(cx, cy), style = Stroke(2f))
            }
            drawCircle(outline, r * 1.1f, Offset(cx, cy), style = Stroke(1.5f))
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

        tdDrawMonsterBody(themeIdx, m, cx, cy, r)
    }
}

private fun DrawScope.tdDrawMonsterBody(themeIdx: Int, m: TDMonster, cx: Float, cy: Float, r: Float) {
    val themeColor = THEME_COLORS[themeIdx]
    val darkColor = themeColor.copy(alpha = 0.8f)
    
    if (m.isBoss) {
        // Boss specialized visuals
        drawCircle(themeColor, r, Offset(cx, cy))
        drawCircle(Color.Black.copy(alpha = 0.6f), r, Offset(cx, cy), style = Stroke(r * 0.2f))
        // Boss Eyes (Glowing)
        val pulse = (System.currentTimeMillis() % 1000) / 1000f
        val eyeColor = Color.White.copy(alpha = 0.7f + 0.3f * pulse)
        drawCircle(eyeColor, r * 0.15f, Offset(cx - r * 0.3f, cy - r * 0.1f))
        drawCircle(eyeColor, r * 0.15f, Offset(cx + r * 0.3f, cy - r * 0.1f))
        
        // Boss Name/Label
        drawIntoCanvas { canvas ->
            val p = NativePaint().apply {
                setColor(android.graphics.Color.WHITE); textSize = r * 0.45f
                textAlign = NativePaint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
            }
            val bIdx = ((m.bossRound / 10) - 1).coerceIn(0, BOSS_NAMES.size - 1)
            canvas.nativeCanvas.drawText(BOSS_NAMES[bIdx], cx, cy + r * 0.6f, p)
        }
        return
    }

    // Regular Monsters by Theme
    when (themeIdx) {
        0 -> { // Forest - Slime
            drawCircle(themeColor, r, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.4f), r * 0.3f, Offset(cx - r * 0.3f, cy - r * 0.3f))
        }
        1 -> { // Mine - Rock
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy - r * 0.2f)
                lineTo(cx + r * 0.6f, cy + r)
                lineTo(cx - r * 0.6f, cy + r)
                lineTo(cx - r, cy - r * 0.2f)
                close()
            }
            drawPath(path, themeColor)
        }
        2 -> { // Desert - Mummy/Scorpion
            drawRect(themeColor, topLeft = Offset(cx - r * 0.8f, cy - r * 0.8f), size = Size(r * 1.6f, r * 1.6f))
            drawLine(Color.Black.copy(alpha = 0.3f), Offset(cx - r * 0.8f, cy), Offset(cx + r * 0.8f, cy), strokeWidth = 2f)
        }
        3 -> { // Ice - Crystal
            rotate(45f, pivot = Offset(cx, cy)) {
                drawRect(themeColor, topLeft = Offset(cx - r * 0.7f, cy - r * 0.7f), size = Size(r * 1.4f, r * 1.4f))
            }
            drawCircle(Color.White.copy(alpha = 0.5f), r * 0.2f, Offset(cx, cy))
        }
        4 -> { // Lava - Fire Slime
            drawCircle(themeColor, r, Offset(cx, cy))
            drawCircle(Color(0xFFFFD600.toInt()), r * 0.5f, Offset(cx, cy + r * 0.2f))
        }
        5 -> { // Graveyard - Ghost
            drawCircle(themeColor, r, Offset(cx, cy - r * 0.2f))
            drawRect(themeColor, topLeft = Offset(cx - r, cy - r * 0.2f), size = Size(r * 2f, r * 0.8f))
            // Ghost tail bits
            repeat(3) { i ->
                drawCircle(themeColor, r * 0.3f, Offset(cx - r * 0.6f + i * r * 0.6f, cy + r * 0.6f))
            }
        }
        6 -> { // Machine - Robot
            drawRect(themeColor, topLeft = Offset(cx - r, cy - r), size = Size(r * 2f, r * 2f))
            drawRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(cx - r * 0.6f, cy - r * 0.4f), size = Size(r * 1.2f, r * 0.3f))
        }
        7 -> { // Swamp - Mud Monster
            drawCircle(themeColor, r * 1.1f, Offset(cx, cy))
            drawCircle(Color(0xFF3E2723.toInt()).copy(alpha = 0.5f), r * 0.4f, Offset(cx + r * 0.3f, cy + r * 0.2f))
        }
        8 -> { // Storm - Cloud/Spark
            repeat(5) { i ->
                val angle = i * 72f
                val offX = cos(Math.toRadians(angle.toDouble())).toFloat() * r * 0.5f
                val offY = sin(Math.toRadians(angle.toDouble())).toDouble().toFloat() * r * 0.5f
                drawCircle(themeColor, r * 0.6f, Offset(cx + offX, cy + offY))
            }
        }
        9 -> { // Abyss - Shadow
            drawCircle(Color.Black, r, Offset(cx, cy))
            drawCircle(themeColor, r * 0.8f, Offset(cx, cy), style = Stroke(2f))
            drawCircle(themeColor, r * 0.2f, Offset(cx, cy))
        }
        else -> drawCircle(themeColor, r, Offset(cx, cy))
    }
    
    // Eyes for all monsters
    drawCircle(Color.Black, r * 0.12f, Offset(cx - r * 0.35f, cy - r * 0.2f))
    drawCircle(Color.Black, r * 0.12f, Offset(cx + r * 0.35f, cy - r * 0.2f))
}

private fun DrawScope.tdDrawEffects(cs: Float, ox: Float, oy: Float, state: TDUiState) {
    for (e in state.effects) {
        val alpha = (1f - e.progress).coerceIn(0f, 1f)
        when (e.kind) {
            TDEffectKind.PROJECTILE -> {
                val t = e.progress.coerceIn(0f, 1f)
                val fromX = ox + e.fromCol * cs; val fromY = oy + e.fromRow * cs
                val toX   = ox + (e.col + 0.5f) * cs; val toY = oy + (e.row + 0.5f) * cs
                val curX  = fromX + (toX - fromX) * t; val curY = fromY + (toY - fromY) * t
                val a = (if (t > 0.78f) (1f - t) / 0.22f else 1f).coerceIn(0f, 1f)
                
                // Enhanced Trail (Glow)
                if (t > 0.15f) {
                    val t2 = (t - 0.15f).coerceAtLeast(0f)
                    val trX = fromX + (toX - fromX) * t2; val trY = fromY + (toY - fromY) * t2
                    drawLine(Color(e.argb).copy(alpha = a * 0.4f),
                        Offset(trX, trY), Offset(curX, curY), strokeWidth = cs * 0.12f)
                    drawLine(Color.White.copy(alpha = a * 0.3f),
                        Offset(trX, trY), Offset(curX, curY), strokeWidth = cs * 0.04f)
                }
                
                drawCircle(Color(e.argb).copy(alpha = a), cs * 0.16f, Offset(curX, curY))
                drawCircle(Color.White.copy(alpha = a * 0.7f), cs * 0.08f, Offset(curX, curY))
            }
            TDEffectKind.LASER -> {
                val a = alpha * 0.95f
                val fromX = ox + e.fromCol * cs; val fromY = oy + e.fromRow * cs
                val toX   = ox + (e.col + 0.5f) * cs; val toY = oy + (e.row + 0.5f) * cs
                // Thick outer glow
                drawLine(Color(e.argb).copy(alpha = a * 0.4f), Offset(fromX, fromY), Offset(toX, toY), strokeWidth = cs * 0.14f)
                // Core beam
                drawLine(Color(e.argb).copy(alpha = a), Offset(fromX, fromY), Offset(toX, toY), strokeWidth = cs * 0.07f)
                drawLine(Color.White.copy(alpha = a * 0.8f), Offset(fromX, fromY), Offset(toX, toY), strokeWidth = cs * 0.03f)
            }
            TDEffectKind.HIT -> {
                val cx = ox + (e.col + 0.5f) * cs; val cy = oy + (e.row + 0.5f) * cs
                drawCircle(Color(e.argb).copy(alpha = alpha * 0.85f),
                    cs * 0.22f * (1f + e.progress * 0.6f), Offset(cx, cy))
                
                if (state.showDamage && e.damage > 0) {
                    drawIntoCanvas { canvas ->
                        val p = NativePaint().apply {
                            color = android.graphics.Color.WHITE
                            setAlpha((alpha * 255).toInt())
                            textSize = cs * 0.32f
                            textAlign = NativePaint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        val ty = cy - cs * 0.4f - e.progress * cs * 0.4f
                        canvas.nativeCanvas.drawText("${e.damage}", cx, ty, p)
                    }
                }
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
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (cellSelected) {
            Button(
                onClick = vm::summonCharacter,
                enabled = state.gold >= cost,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300.toInt()))
            ) {
                Text("🎲 캐릭터 랜덤 소환 (${cost}G)", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = vm::clearSelection) {
                Text("소환 취소", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text(
                "빈 타일을 선택하여 캐릭터를 소환하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun TDCharBuyCard(type: TDCharType, cost: Int, enabled: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(85.dp).aspectRatio(0.72f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(
                Modifier.size(38.dp).padding(top = 2.dp).drawBehind {
                    val r = size.minDimension * 0.4f
                    tdDrawCharBody(type, size.width / 2f, size.height / 2f, r, angle = -45f, evolution = TDEvolution.F)
                }
            )
            Text(type.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                "${cost}G",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (enabled) Color(0xFFFFB300.toInt()) else MaterialTheme.colorScheme.onSurfaceVariant
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
        }
        
        // Skill Tree
        val skills = tdCharSkills(char.type)
        if (skills.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(6.dp)) {
                    Text("등급별 스킬 트리", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    skills.forEach { (evo, desc) ->
                        val isActive = char.evolution >= evo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "● [${evo.label}]",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isActive) Color(0xFF4CAF50.toInt()) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                textDecoration = if (isActive) null else TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
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
private fun TDCharGuideDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📖 캐릭터 도감", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(androidx.compose.material.icons.Icons.Default.Close, "닫기")
                    }
                }
                
                // Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(TDCharType.entries) { type ->
                        TDCharGuideItem(type)
                    }
                }
            }
        }
    }
}

@Composable
private fun TDCharGuideItem(type: TDCharType) {
    val skills = tdCharSkills(type)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        tdDrawCharBody(type, center.x, center.y, size.minDimension * 0.4f, angle = -45f, evolution = TDEvolution.F)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(type.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(tdCharDesc(type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("기본 공격력: ${type.baseAtk}", style = MaterialTheme.typography.labelSmall)
                Text("사거리: ${type.baseRange}", style = MaterialTheme.typography.labelSmall)
                Text("공격속도: ${String.format("%.1f", type.atkMs / 1000.0)}s", style = MaterialTheme.typography.labelSmall)
            }
            
            if (skills.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Text("등급별 스킬", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                skills.forEach { (evo, desc) ->
                    Row(Modifier.padding(vertical = 1.dp)) {
                        Text("[${evo.label}]", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        Text(desc, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
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
