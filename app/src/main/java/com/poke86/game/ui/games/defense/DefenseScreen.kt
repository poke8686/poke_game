package com.poke86.game.ui.games.defense

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.poke86.game.ui.theme.GameVaultTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class DefensePhase { ROUND_SELECT, PLAYING, WAVE_CLEAR, ROUND_CLEAR, GAME_OVER }

enum class BalloonType {
    REGULAR,  // 기본
    TRAP,     // 10라운드: 탭 시 목숨 -1
    PAINT,    // 20라운드: 주변 풍선 연쇄 파괴
    SPLIT,    // 30라운드: 터지면 작은 풍선 2-4개 생성
    FAST,     // 40라운드: 속도 2.5배
    FREEZE,   // 50라운드: 터지면 1.5초 전체 동결
    SHIELD,   // 60라운드: HP 2 (2회 터치 필요)
    GHOST,    // 70라운드: 주기적으로 투명해짐
    BOMB,     // 80라운드: 통과 시 목숨 -3
    BOSS,     // 90라운드: HP 5, 사인파 이동
}

// ─── Data ─────────────────────────────────────────────────────────────────────

data class Balloon(
    val id: Int,
    val x: Float,
    val y: Float,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val type: BalloonType = BalloonType.REGULAR,
    val hp: Int = 1,
    val phase: Float = 0f,   // BOSS 수평 진동에 사용
)

data class PopEffect(
    val id: Int,
    val x: Float,
    val y: Float,
    val color: Color,
    val radius: Float,
    val progress: Float = 0f,
)

data class DefenseUiState(
    val phase: DefensePhase = DefensePhase.ROUND_SELECT,
    val round: Int = 1,
    val wave: Int = 1,
    val maxWaves: Int = 3,
    val balloons: List<Balloon> = emptyList(),
    val popEffects: List<PopEffect> = emptyList(),
    val lives: Int = 10,
    val score: Int = 0,
    val isFrozen: Boolean = false,
    val frozenUntil: Long = 0L,
    val tickCount: Long = 0L,
)

private fun wavesForRound(round: Int) = 3 + (round - 1) / 10

private fun ghostAlpha(balloonId: Int, tickCount: Long): Float {
    val phase = (tickCount + balloonId * 17L) % 60L
    return when {
        phase < 20L -> phase.toFloat() / 20f
        phase < 42L -> 1f
        else -> 1f - (phase - 42L).toFloat() / 18f
    }
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class DefenseViewModel : ViewModel() {

    private val _state = MutableStateFlow(DefenseUiState())
    val state: StateFlow<DefenseUiState> = _state.asStateFlow()

    private var gameJob: Job? = null
    private var nextId = 0

    private val balloonColors = listOf(
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF1E88E5),
        Color(0xFF00897B), Color(0xFFF4511E), Color(0xFF3949AB),
        Color(0xFFFFB300), Color(0xFF6D4C41),
    )
    private val paintSplashColors = listOf(
        Color(0xFFFF1744), Color(0xFF00E676), Color(0xFF00B0FF),
        Color(0xFFD500F9), Color(0xFFFF6D00), Color(0xFF1DE9B6),
    )

    // ─ Sounds ──────────────────────────────────────────────────────────────────

    private val popBuffer: ShortArray by lazy {
        val sr = 22050; val n = sr * 80 / 1000
        ShortArray(n) { i ->
            val p = i.toFloat() / n
            val e = (1f - p).pow(1.2f)
            val noise = java.util.Random(i.toLong() * 1234567L).nextFloat() * 2f - 1f
            val tone = sin(2.0 * PI * 150.0 * i / sr).toFloat()
            ((noise * 0.65f + tone * 0.35f) * e * 28000f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
    private val buzzBuffer: ShortArray by lazy {
        val sr = 22050; val n = sr * 120 / 1000
        ShortArray(n) { i ->
            val p = i.toFloat() / n
            val freq = 380.0 - p * 180.0
            val e = (1f - p).pow(0.6f)
            (sin(2.0 * PI * freq * i / sr).toFloat() * e * 24000f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun playBuffer(buf: ShortArray) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(22050).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(buf.size * 2).build()
                track.write(buf, 0, buf.size); track.play(); delay(150L); track.release()
            } catch (_: Exception) {}
        }
    }

    // ─ Public ─────────────────────────────────────────────────────────────────

    fun selectRound(round: Int) {
        gameJob?.cancel(); nextId = 0
        _state.value = DefenseUiState(phase = DefensePhase.PLAYING, round = round, wave = 1, maxWaves = wavesForRound(round))
        spawnWave(round, 1); startLoop()
    }

    fun goToNextRound() = selectRound((_state.value.round + 1).coerceAtMost(99))

    fun goToRoundSelect() {
        gameJob?.cancel()
        _state.value = DefenseUiState(phase = DefensePhase.ROUND_SELECT)
    }

    fun tapAt(normX: Float, normY: Float) {
        val st = _state.value
        if (st.phase != DefensePhase.PLAYING && st.phase != DefensePhase.WAVE_CLEAR) return

        val hit = st.balloons
            .sortedByDescending { b -> when (b.type) { BalloonType.BOSS -> 4; BalloonType.TRAP -> 3; BalloonType.SHIELD -> 2; else -> 1 } }
            .firstOrNull { b ->
                if (b.type == BalloonType.GHOST && ghostAlpha(b.id, st.tickCount) < 0.15f) return@firstOrNull false
                val dx = b.x - normX; val dy = b.y - normY
                sqrt(dx * dx + dy * dy) <= b.radius * 1.35f
            } ?: return

        when (hit.type) {
            BalloonType.TRAP -> {
                playBuffer(buzzBuffer)
                val newLives = (st.lives - 1).coerceAtLeast(0)
                _state.update { it.copy(lives = newLives) }
                if (newLives <= 0) triggerGameOver()
            }
            else -> {
                if (hit.hp > 1) {
                    _state.update { s -> s.copy(balloons = s.balloons.map { if (it.id == hit.id) it.copy(hp = it.hp - 1) else it }) }
                    playBuffer(popBuffer)
                } else {
                    popBalloon(hit)
                }
            }
        }
    }

    fun sliceThrough(x1: Float, y1: Float, x2: Float, y2: Float) {
        val st = _state.value
        if (st.phase != DefensePhase.PLAYING && st.phase != DefensePhase.WAVE_CLEAR) return

        val hits = st.balloons.filter { b ->
            if (b.type == BalloonType.GHOST && ghostAlpha(b.id, st.tickCount) < 0.15f) return@filter false
            lineCircleIntersects(x1, y1, x2, y2, b.x, b.y, b.radius * 1.2f)
        }

        hits.forEach { b ->
            val current = _state.value
            if (current.phase == DefensePhase.GAME_OVER) return
            when (b.type) {
                BalloonType.TRAP -> {
                    if (current.balloons.none { it.id == b.id }) return@forEach
                    playBuffer(buzzBuffer)
                    val newLives = (current.lives - 1).coerceAtLeast(0)
                    _state.update { it.copy(lives = newLives) }
                    if (newLives <= 0) { triggerGameOver(); return }
                }
                else -> {
                    if (current.balloons.none { it.id == b.id }) return@forEach
                    if (b.hp > 1) {
                        _state.update { s -> s.copy(balloons = s.balloons.map { if (it.id == b.id) it.copy(hp = it.hp - 1) else it }) }
                        playBuffer(popBuffer)
                    } else {
                        if (_state.value.balloons.any { it.id == b.id }) popBalloon(b)
                    }
                }
            }
        }
    }

    // ─ Private ────────────────────────────────────────────────────────────────

    private fun lineCircleIntersects(x1: Float, y1: Float, x2: Float, y2: Float, cx: Float, cy: Float, r: Float): Boolean {
        val dx = x2 - x1; val dy = y2 - y1
        val fx = x1 - cx; val fy = y1 - cy
        val len2 = dx * dx + dy * dy
        if (len2 < 1e-9f) return fx * fx + fy * fy <= r * r
        val t = (-(fx * dx + fy * dy) / len2).coerceIn(0f, 1f)
        val px = fx + t * dx; val py = fy + t * dy
        return px * px + py * py <= r * r
    }

    private fun popBalloon(b: Balloon) {
        val round = _state.value.round
        val pts = when (b.type) { BalloonType.BOSS -> round * 50; BalloonType.SHIELD, BalloonType.GHOST -> round * 15; else -> round * 5 }
        _state.update { st ->
            st.copy(
                score = st.score + pts,
                balloons = st.balloons.filter { it.id != b.id },
                popEffects = st.popEffects + PopEffect(nextId++, b.x, b.y, b.color, b.radius)
            )
        }
        when (b.type) {
            BalloonType.PAINT -> {
                val chainR = b.radius * 3.5f
                val chained = _state.value.balloons.filter { o -> val dx = o.x - b.x; val dy = o.y - b.y; sqrt(dx * dx + dy * dy) <= chainR }
                if (chained.isNotEmpty()) {
                    _state.update { st ->
                        val fx = chained.map { o -> PopEffect(nextId++, o.x, o.y, o.color, o.radius) }
                        st.copy(score = st.score + chained.size * round * 3, balloons = st.balloons.filter { o -> chained.none { c -> c.id == o.id } }, popEffects = st.popEffects + fx)
                    }
                }
            }
            BalloonType.SPLIT -> {
                val cnt = 2 + Random.nextInt(3)
                val babies = (0 until cnt).map { i ->
                    val angle = 2.0 * PI * i / cnt
                    Balloon(id = nextId++, x = (b.x + cos(angle).toFloat() * b.radius * 2.2f).coerceIn(0.06f, 0.94f), y = (b.y + sin(angle).toFloat() * b.radius * 2.2f).coerceIn(0.06f, 0.94f), speed = b.speed * 1.1f, radius = b.radius * 0.55f, color = b.color, type = BalloonType.REGULAR)
                }
                _state.update { st -> st.copy(balloons = st.balloons + babies) }
            }
            BalloonType.FREEZE -> _state.update { st -> st.copy(isFrozen = true, frozenUntil = System.currentTimeMillis() + 1500L) }
            else -> {}
        }
        playBuffer(popBuffer)
    }

    private fun spawnWave(round: Int, wave: Int) {
        val regularCount = (6 + round / 4 + wave).coerceAtMost(28)
        val speed = { 0.0010f + round * 0.000045f + wave * 0.00006f + Random.nextFloat() * 0.00015f }

        val list = mutableListOf<Balloon>()

        repeat(regularCount) { i ->
            list += Balloon(id = nextId++, x = Random.nextFloat() * 0.80f + 0.10f, y = 1.05f + i * 0.10f, speed = speed(), radius = 0.062f, color = balloonColors[i % balloonColors.size])
        }
        if (round >= 10) {
            val cnt = 1 + (round - 10) / 15
            repeat(cnt) {
                list += Balloon(id = nextId++, x = Random.nextFloat() * 0.68f + 0.16f, y = 0.2f + Random.nextFloat() * 0.5f, speed = speed() * 0.5f, radius = 0.10f + Random.nextFloat() * 0.025f, color = Color(0xCC1A0A2E), type = BalloonType.TRAP)
            }
        }
        if (round >= 20) {
            list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f + Random.nextFloat() * 0.2f, speed = speed() * 0.85f, radius = 0.072f, color = paintSplashColors[Random.nextInt(paintSplashColors.size)], type = BalloonType.PAINT)
        }
        if (round >= 30) {
            repeat(round / 10) { i ->
                list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f + i * 0.15f, speed = speed(), radius = 0.075f, color = Color(0xFFFF6D00), type = BalloonType.SPLIT)
            }
        }
        if (round >= 40) {
            repeat(round / 20) { i ->
                list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f + i * 0.12f, speed = speed() * 2.5f, radius = 0.058f, color = Color(0xFF00E5FF), type = BalloonType.FAST)
            }
        }
        if (round >= 50) {
            repeat(max(1, round / 30 - 1)) { i ->
                list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f + i * 0.18f, speed = speed() * 0.8f, radius = 0.070f, color = Color(0xFF80DEEA), type = BalloonType.FREEZE)
            }
        }
        if (round >= 60) {
            list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f, speed = speed() * 0.75f, radius = 0.075f, color = Color(0xFFFFD700), type = BalloonType.SHIELD, hp = 2)
        }
        if (round >= 70) {
            repeat(max(1, round / 35)) { i ->
                list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f + i * 0.2f, speed = speed() * 1.1f, radius = 0.068f, color = Color(0xFFE0E0E0), type = BalloonType.GHOST)
            }
        }
        if (round >= 80) {
            list += Balloon(id = nextId++, x = Random.nextFloat() * 0.78f + 0.11f, y = 1.05f, speed = speed() * 0.65f, radius = 0.078f, color = Color(0xFFB71C1C), type = BalloonType.BOMB)
        }
        if (round >= 90) {
            list += Balloon(id = nextId++, x = 0.5f, y = 1.1f, speed = speed() * 0.5f, radius = 0.13f, color = Color(0xFF1A0A3E), type = BalloonType.BOSS, hp = 5)
        }

        _state.update { it.copy(balloons = list) }
    }

    private fun startLoop() {
        gameJob = viewModelScope.launch {
            while (true) { delay(16L); if (_state.value.phase == DefensePhase.PLAYING) tick() }
        }
    }

    private fun tick() {
        val st = _state.value
        val updatedPops = st.popEffects.map { it.copy(progress = it.progress + 0.05f) }.filter { it.progress < 1f }
        val newTick = st.tickCount + 1L

        if (st.isFrozen) {
            if (System.currentTimeMillis() < st.frozenUntil) { _state.update { it.copy(popEffects = updatedPops, tickCount = newTick) }; return }
            _state.update { it.copy(isFrozen = false) }
        }

        val moved = st.balloons.map { b ->
            if (b.type == BalloonType.BOSS) {
                val np = b.phase + 0.012f
                b.copy(y = b.y - b.speed, x = 0.5f + sin(np * 2 * PI).toFloat() * 0.33f, phase = np)
            } else b.copy(y = b.y - b.speed)
        }

        val escapedLives: Int = moved.filter { it.y + it.radius < 0f }.fold(0) { acc, b ->
            acc + when (b.type) { BalloonType.TRAP -> 0; BalloonType.BOMB -> 3; else -> 1 }
        }
        val remaining = moved.filter { it.y + it.radius >= 0f }
        val newLives = (st.lives - escapedLives).coerceAtLeast(0)

        if (newLives <= 0) { triggerGameOver(); return }

        if (remaining.none { it.type != BalloonType.TRAP }) {
            onWaveComplete(newLives); return
        }

        _state.update { it.copy(balloons = remaining, lives = newLives, popEffects = updatedPops, tickCount = newTick) }
    }

    private fun onWaveComplete(newLives: Int) {
        val st = _state.value
        if (st.wave >= st.maxWaves) {
            gameJob?.cancel()
            _state.update { it.copy(phase = DefensePhase.ROUND_CLEAR, balloons = emptyList(), lives = newLives, popEffects = emptyList()) }
        } else {
            val nw = st.wave + 1
            _state.update { it.copy(phase = DefensePhase.WAVE_CLEAR, balloons = emptyList(), lives = newLives, wave = nw, popEffects = emptyList()) }
            viewModelScope.launch {
                delay(1800L)
                if (_state.value.phase == DefensePhase.WAVE_CLEAR) {
                    spawnWave(_state.value.round, _state.value.wave)
                    _state.update { it.copy(phase = DefensePhase.PLAYING) }
                }
            }
        }
    }

    private fun triggerGameOver() {
        gameJob?.cancel()
        _state.update { it.copy(phase = DefensePhase.GAME_OVER, balloons = emptyList(), popEffects = emptyList()) }
    }

    override fun onCleared() { gameJob?.cancel(); super.onCleared() }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefenseScreen(navController: NavController, viewModel: DefenseViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val topTitle = when (state.phase) {
        DefensePhase.ROUND_SELECT -> "풍선 디펜스"
        DefensePhase.ROUND_CLEAR  -> "라운드 ${state.round} 클리어!"
        DefensePhase.GAME_OVER    -> "게임 오버"
        else -> "R${state.round}  W${state.wave}/${state.maxWaves}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.phase == DefensePhase.ROUND_SELECT) navController.popBackStack()
                        else viewModel.goToRoundSelect()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                DefensePhase.ROUND_SELECT -> RoundSelectContent(onSelectRound = viewModel::selectRound)
                DefensePhase.PLAYING, DefensePhase.WAVE_CLEAR -> PlayingContent(state, viewModel::tapAt, viewModel::sliceThrough)
                DefensePhase.ROUND_CLEAR -> RoundClearContent(state, viewModel::goToNextRound, viewModel::goToRoundSelect)
                DefensePhase.GAME_OVER -> GameOverContent(state, viewModel::selectRound, viewModel::goToRoundSelect)
            }
        }
    }
}

// ─── Round Select ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundSelectContent(onSelectRound: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp, 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Color(0xFF388E3C) to "1~9",
                Color(0xFF1976D2) to "10~29",
                Color(0xFFE64A19) to "30~59",
                Color(0xFF880E4F) to "60~99",
            ).forEach { (c, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(99) { index ->
                val round = index + 1
                val milestoneEmoji = when (round) {
                    10 -> "🌑"; 20 -> "🎨"; 30 -> "✂️"
                    40 -> "⚡"; 50 -> "❄️"; 60 -> "🛡️"
                    70 -> "👻"; 80 -> "💣"; 90 -> "👑"
                    else -> null
                }
                val bgColor = when {
                    round < 10 -> Color(0xFF388E3C)
                    round < 30 -> Color(0xFF1976D2)
                    round < 60 -> Color(0xFFE64A19)
                    else -> Color(0xFF880E4F)
                }
                val isMilestone = round % 10 == 0
                ElevatedCard(
                    onClick = { onSelectRound(round) },
                    modifier = Modifier.aspectRatio(1f),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isMilestone) bgColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(if (isMilestone) 3.dp else 1.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$round",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isMilestone) FontWeight.Black else FontWeight.Normal,
                                color = if (isMilestone) bgColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (milestoneEmoji != null) {
                                Text(milestoneEmoji, fontSize = 10.sp)
                            } else {
                                Text(
                                    "${wavesForRound(round)}W",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Playing ──────────────────────────────────────────────────────────────────

@Composable
private fun PlayingContent(state: DefenseUiState, onTap: (Float, Float) -> Unit, onSlice: (Float, Float, Float, Float) -> Unit) {
    var canvasW by remember { mutableStateOf(1f) }
    var canvasH by remember { mutableStateOf(1f) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("❤️", fontSize = 18.sp)
                Text("${state.lives}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (state.lives <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("웨이브 ${state.wave} / ${state.maxWaves}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("${state.score}점", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .onSizeChanged { canvasW = it.width.toFloat().coerceAtLeast(1f); canvasH = it.height.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val nx = down.position.x / canvasW
                            val ny = down.position.y / canvasH
                            onTap(nx, ny)
                            var lastX = nx; var lastY = ny
                            do {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull() ?: break
                                val cx = ch.position.x / canvasW
                                val cy = ch.position.y / canvasH
                                if (ch.position != ch.previousPosition) {
                                    onSlice(lastX, lastY, cx, cy)
                                    lastX = cx; lastY = cy
                                }
                                ch.consume()
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFBBDEFB), Color(0xFFE3F2FD), Color(0xFFF1F8E9)), startY = 0f, endY = size.height))
                drawRect(color = Color(0xFFC62828), topLeft = Offset(0f, 0f), size = Size(size.width, 8f))
                drawRect(brush = Brush.verticalGradient(listOf(Color(0x33C62828), Color(0x00C62828)), startY = 8f, endY = size.height * 0.12f), topLeft = Offset(0f, 8f), size = Size(size.width, size.height * 0.12f))

                // Draw order: regular → special → traps/boss on top
                val sorted = state.balloons.sortedWith(compareBy { b ->
                    when (b.type) { BalloonType.BOSS -> 4; BalloonType.TRAP -> 3; BalloonType.SHIELD -> 2; else -> 1 }
                })
                sorted.forEach { b ->
                    val cx = b.x * size.width; val cy = b.y * size.height; val r = b.radius * size.width
                    when (b.type) {
                        BalloonType.REGULAR -> drawRegularBalloon(cx, cy, r, b.color)
                        BalloonType.TRAP    -> drawTrapBalloon(cx, cy, r)
                        BalloonType.PAINT   -> drawPaintBalloon(cx, cy, r, b.color)
                        BalloonType.SPLIT   -> drawSplitBalloon(cx, cy, r)
                        BalloonType.FAST    -> drawFastBalloon(cx, cy, r)
                        BalloonType.FREEZE  -> drawFreezeBalloon(cx, cy, r)
                        BalloonType.SHIELD  -> drawShieldBalloon(cx, cy, r, b.hp)
                        BalloonType.GHOST   -> drawGhostBalloon(cx, cy, r, ghostAlpha(b.id, state.tickCount))
                        BalloonType.BOMB    -> drawBombBalloon(cx, cy, r)
                        BalloonType.BOSS    -> drawBossBalloon(cx, cy, r, b.hp)
                    }
                }

                // Pop effects
                state.popEffects.forEach { pop ->
                    val cx = pop.x * size.width; val cy = pop.y * size.height; val r = pop.radius * size.width
                    val ao = (pop.id % 7) * 0.45
                    for (i in 0 until 8) {
                        val angle = 2.0 * PI * i / 8.0 + ao
                        val dist = r * (0.8f + pop.progress * 3.2f)
                        val alpha = (1f - pop.progress * 1.2f).coerceIn(0f, 1f)
                        drawCircle(pop.color.copy(alpha = alpha), radius = (r * 0.26f * (1f - pop.progress)).coerceAtLeast(1f), center = Offset(cx + cos(angle).toFloat() * dist, cy + sin(angle).toFloat() * dist))
                    }
                    val fa = (1f - pop.progress * 4f).coerceAtLeast(0f)
                    if (fa > 0f) drawCircle(Color.White.copy(alpha = fa), radius = r * 0.9f, center = Offset(cx, cy))
                    val ra = (1f - pop.progress * 2.5f).coerceAtLeast(0f)
                    if (ra > 0f) drawCircle(pop.color.copy(alpha = ra), radius = r * (0.8f + pop.progress * 2.5f), center = Offset(cx, cy), style = Stroke((4f * (1f - pop.progress)).coerceAtLeast(0.5f)))
                }

                // Frozen blue tint overlay
                if (state.isFrozen) {
                    drawRect(Color(0x4000B0FF), size = size)
                    drawRect(brush = Brush.radialGradient(listOf(Color(0x0000B0FF), Color(0x6000B0FF)), center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension / 2), size = size)
                }
            }

            // Wave clear overlay
            if (state.phase == DefensePhase.WAVE_CLEAR) {
                Box(Modifier.fillMaxSize().background(Color(0x55000000)), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp) {
                        Column(Modifier.padding(40.dp, 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 44.sp)
                            Text("웨이브 ${state.wave - 1} 클리어!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            if (state.wave <= state.maxWaves) {
                                Spacer(Modifier.height(4.dp))
                                Text("${state.wave}/${state.maxWaves} 웨이브 시작 준비 중...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Round Clear ──────────────────────────────────────────────────────────────

@Composable
private fun RoundClearContent(state: DefenseUiState, onNextRound: () -> Unit, onRoundSelect: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🏆", fontSize = 88.sp)
        Spacer(Modifier.height(16.dp))
        Text("라운드 ${state.round} 클리어!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text("${state.maxWaves}웨이브 완료  ·  목숨 ${state.lives}개 남음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(40.dp, 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("획득 점수", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                Text("${state.score}점", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(40.dp))
        if (state.round < 99) {
            Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(29.dp)) {
                Text("라운드 ${state.round + 1} 도전! →", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onRoundSelect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
            Text("라운드 선택으로", fontSize = 16.sp)
        }
    }
}

// ─── Game Over ────────────────────────────────────────────────────────────────

@Composable
private fun GameOverContent(state: DefenseUiState, onRetry: (Int) -> Unit, onRoundSelect: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("💀", fontSize = 88.sp)
        Spacer(Modifier.height(16.dp))
        Text("게임 오버", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text("라운드 ${state.round}  ·  웨이브 ${state.wave}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(40.dp, 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("최종 점수", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                Text("${state.score}점", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(44.dp))
        Button(onClick = { onRetry(state.round) }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(29.dp)) {
            Text("같은 라운드 재도전", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRoundSelect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
            Text("라운드 선택으로", fontSize = 16.sp)
        }
    }
}

// ─── Balloon Drawing (DrawScope extensions) ───────────────────────────────────

private fun DrawScope.drawRegularBalloon(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(Color(0x22000000), r * 0.9f, Offset(cx + 3f, cy + 3f))
    drawCircle(color, r, Offset(cx, cy))
    drawCircle(Color(0x60FFFFFF), r * 0.35f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    drawCircle(color.copy(alpha = 0.4f), r, Offset(cx, cy), style = Stroke(2f))
    drawCircle(color.copy(alpha = 0.8f), r * 0.12f, Offset(cx, cy + r))
    drawLine(Color(0xFF795548), Offset(cx, cy + r * 1.1f), Offset(cx + 5f, cy + r + r * 0.6f), 1.5f)
}

private fun DrawScope.drawTrapBalloon(cx: Float, cy: Float, r: Float) {
    // Multi-layer neon glow
    drawCircle(Color(0x0CFF1744), r * 1.65f, Offset(cx, cy))
    drawCircle(Color(0x18FF1744), r * 1.35f, Offset(cx, cy))
    drawCircle(Color(0x28FF1744), r * 1.12f, Offset(cx, cy))
    // Deep dark body with radial gradient
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF2D0050), Color(0xFF0D0018)), center = Offset(cx - r * 0.15f, cy - r * 0.15f), radius = r * 1.1f), radius = r, center = Offset(cx, cy))
    // Neon rim: glow + bright + thin inner
    drawCircle(Color(0x50FF1744), r, Offset(cx, cy), style = Stroke(7f))
    drawCircle(Color(0xBBFF4D6D), r, Offset(cx, cy), style = Stroke(2.5f))
    drawCircle(Color(0x60FF80A0), r * 0.92f, Offset(cx, cy), style = Stroke(1f))
    // Sheen
    drawCircle(Color(0x28FFFFFF), r * 0.32f, Offset(cx - r * 0.26f, cy - r * 0.29f))
    // ✕ layered (shadow → outer glow → inner glow → bright core)
    val x = r * 0.4f
    drawLine(Color(0x30000000), Offset(cx - x + 2f, cy - x + 2f), Offset(cx + x + 2f, cy + x + 2f), 8f, StrokeCap.Round)
    drawLine(Color(0x30000000), Offset(cx + x + 2f, cy - x + 2f), Offset(cx - x + 2f, cy + x + 2f), 8f, StrokeCap.Round)
    drawLine(Color(0x50FF1744), Offset(cx - x, cy - x), Offset(cx + x, cy + x), 10f, StrokeCap.Round)
    drawLine(Color(0x50FF1744), Offset(cx + x, cy - x), Offset(cx - x, cy + x), 10f, StrokeCap.Round)
    drawLine(Color(0x80FF4060), Offset(cx - x, cy - x), Offset(cx + x, cy + x), 6f, StrokeCap.Round)
    drawLine(Color(0x80FF4060), Offset(cx + x, cy - x), Offset(cx - x, cy + x), 6f, StrokeCap.Round)
    drawLine(Color(0xFFFF6080), Offset(cx - x, cy - x), Offset(cx + x, cy + x), 3f, StrokeCap.Round)
    drawLine(Color(0xFFFF6080), Offset(cx + x, cy - x), Offset(cx - x, cy + x), 3f, StrokeCap.Round)
    drawLine(Color(0xFF6200EA), Offset(cx, cy + r * 1.02f), Offset(cx + 4f, cy + r + r * 0.52f), 1.5f)
}

private fun DrawScope.drawPaintBalloon(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(Color(0x22000000), r * 0.9f, Offset(cx + 3f, cy + 3f))
    drawCircle(brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0.7f)), center = Offset(cx - r * 0.2f, cy - r * 0.2f), radius = r * 1.3f), radius = r, center = Offset(cx, cy))
    // Paint drops (6 colors around rim)
    val drops = listOf(Color(0xFFFF1744), Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9), Color(0xFFFF6D00), Color(0xFF1DE9B6))
    for (i in 0 until 6) {
        val angle = 2.0 * PI * i / 6.0
        drawCircle(drops[i], r * 0.17f, Offset(cx + cos(angle).toFloat() * r * 0.78f, cy + sin(angle).toFloat() * r * 0.78f))
    }
    drawCircle(Color(0x80FFFFFF), r * 0.22f, Offset(cx, cy))   // white center sparkle
    drawCircle(color.copy(alpha = 0.6f), r, Offset(cx, cy), style = Stroke(2.5f))
    drawCircle(Color(0x50FFFFFF), r * 0.32f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    drawLine(Color(0xFF795548), Offset(cx, cy + r * 1.1f), Offset(cx + 5f, cy + r + r * 0.6f), 1.5f)
}

private fun DrawScope.drawSplitBalloon(cx: Float, cy: Float, r: Float) {
    drawCircle(Color(0x22000000), r * 0.9f, Offset(cx + 3f, cy + 3f))
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFF8C00), Color(0xFFE65100)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0x50FFFFFF), r * 0.28f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    drawCircle(Color(0x60FF8C00), r, Offset(cx, cy), style = Stroke(2f))
    // 3 outward arrows
    for (i in 0 until 3) {
        val angle = 2.0 * PI * i / 3.0 - PI / 6
        val ax = cos(angle).toFloat(); val ay = sin(angle).toFloat()
        val sx = cx + ax * r * 0.2f; val sy = cy + ay * r * 0.2f
        val ex = cx + ax * r * 0.72f; val ey = cy + ay * r * 0.72f
        drawLine(Color.White.copy(alpha = 0.9f), Offset(sx, sy), Offset(ex, ey), 3f, StrokeCap.Round)
        val perp = Pair(-ay * r * 0.14f, ax * r * 0.14f)
        drawLine(Color.White.copy(alpha = 0.9f), Offset(ex, ey), Offset(ex - ax * r * 0.2f + perp.first, ey - ay * r * 0.2f + perp.second), 2.5f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f), Offset(ex, ey), Offset(ex - ax * r * 0.2f - perp.first, ey - ay * r * 0.2f - perp.second), 2.5f, StrokeCap.Round)
    }
    drawLine(Color(0xFF795548), Offset(cx, cy + r * 1.1f), Offset(cx + 5f, cy + r + r * 0.6f), 1.5f)
}

private fun DrawScope.drawFastBalloon(cx: Float, cy: Float, r: Float) {
    drawCircle(Color(0x22000000), r * 0.9f, Offset(cx + 3f, cy + 3f))
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF006064)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0x50FFFFFF), r * 0.28f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    drawCircle(Color(0x80FFFFFF), r, Offset(cx, cy), style = Stroke(2.5f))
    drawCircle(Color(0x40FFFF00), r * 1.08f, Offset(cx, cy), style = Stroke(1.5f))   // electric halo
    // Lightning bolt
    val lx = r * 0.16f; val ly = r * 0.32f
    val bolt = listOf(Offset(cx + lx, cy - ly * 1.2f), Offset(cx - lx * 0.5f, cy - ly * 0.1f), Offset(cx + lx * 0.7f, cy - ly * 0.1f), Offset(cx - lx, cy + ly * 1.2f))
    for (i in 0 until bolt.size - 1) drawLine(Color(0xCCFFFF00), bolt[i], bolt[i + 1], 3f, StrokeCap.Round)
    for (i in 0 until bolt.size - 1) drawLine(Color.White.copy(alpha = 0.8f), bolt[i], bolt[i + 1], 1.5f, StrokeCap.Round)
    drawLine(Color(0xFF006064), Offset(cx, cy + r * 1.1f), Offset(cx + 5f, cy + r + r * 0.6f), 1.5f)
}

private fun DrawScope.drawFreezeBalloon(cx: Float, cy: Float, r: Float) {
    drawCircle(Color(0x22000000), r * 0.9f, Offset(cx + 3f, cy + 3f))
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFE0F7FA), Color(0xFF0097A7)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0x70FFFFFF), r * 0.28f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    drawCircle(Color(0xFF80DEEA), r, Offset(cx, cy), style = Stroke(2.5f))
    drawCircle(Color(0x40FFFFFF), r * 0.95f, Offset(cx, cy), style = Stroke(1f))
    // Snowflake: 6 spokes + branches
    val sc = Color.White.copy(alpha = 0.9f)
    for (i in 0 until 6) {
        val a = PI.toFloat() * i / 3
        val ex = cos(a) * r * 0.55f; val ey = sin(a) * r * 0.55f
        drawLine(sc, Offset(cx, cy), Offset(cx + ex, cy + ey), 2f)
        val bx = cx + ex * 0.6f; val by_ = cy + ey * 0.6f
        val ba1 = a + PI.toFloat() / 4; val ba2 = a - PI.toFloat() / 4
        drawLine(sc, Offset(bx, by_), Offset(bx + cos(ba1) * r * 0.18f, by_ + sin(ba1) * r * 0.18f), 1.5f)
        drawLine(sc, Offset(bx, by_), Offset(bx + cos(ba2) * r * 0.18f, by_ + sin(ba2) * r * 0.18f), 1.5f)
    }
    drawLine(Color(0xFF0097A7), Offset(cx, cy + r * 1.1f), Offset(cx + 5f, cy + r + r * 0.6f), 1.5f)
}

private fun DrawScope.drawShieldBalloon(cx: Float, cy: Float, r: Float, hp: Int) {
    drawCircle(Color(0x22000000), r * 0.9f, Offset(cx + 3f, cy + 3f))
    val gold = if (hp >= 2) Color(0xFFFFD700) else Color(0xFFB8860B)
    drawCircle(brush = Brush.radialGradient(listOf(gold.copy(alpha = 0.9f), gold.copy(alpha = 0.6f)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0x70FFFFFF), r * 0.28f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    drawCircle(Color(0xCCFFF176), r, Offset(cx, cy), style = Stroke(if (hp >= 2) 3f else 2f))
    // Shield shape
    val sc = if (hp >= 2) Color.White.copy(0.9f) else Color.White.copy(0.55f)
    val sh = r * 0.52f; val sw = r * 0.4f
    drawLine(sc, Offset(cx - sw, cy - sh * 0.55f), Offset(cx, cy - sh), 2.5f, StrokeCap.Round)
    drawLine(sc, Offset(cx + sw, cy - sh * 0.55f), Offset(cx, cy - sh), 2.5f, StrokeCap.Round)
    drawLine(sc, Offset(cx - sw, cy - sh * 0.55f), Offset(cx - sw, cy + sh * 0.22f), 2.5f, StrokeCap.Round)
    drawLine(sc, Offset(cx + sw, cy - sh * 0.55f), Offset(cx + sw, cy + sh * 0.22f), 2.5f, StrokeCap.Round)
    drawLine(sc, Offset(cx - sw, cy + sh * 0.22f), Offset(cx, cy + sh), 2.5f, StrokeCap.Round)
    drawLine(sc, Offset(cx + sw, cy + sh * 0.22f), Offset(cx, cy + sh), 2.5f, StrokeCap.Round)
    if (hp == 1) drawLine(Color(0x998B0000), Offset(cx - r * 0.1f, cy - r * 0.3f), Offset(cx + r * 0.18f, cy + r * 0.35f), 2.5f, StrokeCap.Round)
    drawLine(Color(0xFFB8860B), Offset(cx, cy + r * 1.1f), Offset(cx + 5f, cy + r + r * 0.6f), 1.5f)
}

private fun DrawScope.drawGhostBalloon(cx: Float, cy: Float, r: Float, alpha: Float) {
    if (alpha < 0.02f) return
    drawCircle(Color(0xFFE0E0E0).copy(alpha = alpha * 0.18f), r * 1.25f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = alpha * 0.85f), r, Offset(cx, cy))
    drawCircle(Color(0xFFB0BEC5).copy(alpha = alpha * 0.6f), r, Offset(cx, cy), style = Stroke(1.5f))
    val eyeA = alpha * 0.9f
    drawCircle(Color(0xFF546E7A).copy(alpha = eyeA), r * 0.15f, Offset(cx - r * 0.28f, cy - r * 0.08f))
    drawCircle(Color(0xFF546E7A).copy(alpha = eyeA), r * 0.15f, Offset(cx + r * 0.28f, cy - r * 0.08f))
    drawCircle(Color.White.copy(alpha = eyeA * 0.7f), r * 0.06f, Offset(cx - r * 0.24f, cy - r * 0.12f))
    drawCircle(Color.White.copy(alpha = eyeA * 0.7f), r * 0.06f, Offset(cx + r * 0.32f, cy - r * 0.12f))
    drawLine(Color(0xFFB0BEC5).copy(alpha = alpha * 0.4f), Offset(cx, cy + r * 1.05f), Offset(cx + 5f, cy + r + r * 0.5f), 1.5f)
}

private fun DrawScope.drawBombBalloon(cx: Float, cy: Float, r: Float) {
    drawCircle(Color(0x20FF5252), r * 1.3f, Offset(cx, cy))
    drawCircle(Color(0x30FF1744), r * 1.12f, Offset(cx, cy))
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF3D0000), Color(0xFF1A0000)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
    drawCircle(Color(0x80FF5252), r, Offset(cx, cy), style = Stroke(3f))
    drawCircle(Color(0x40FF8A80), r * 0.92f, Offset(cx, cy), style = Stroke(1.5f))
    drawCircle(Color(0x28FFFFFF), r * 0.28f, Offset(cx - r * 0.28f, cy - r * 0.3f))
    for (i in -1..1 step 2) drawLine(Color(0x60FF5252), Offset(cx - r * 0.55f, cy + i * r * 0.25f), Offset(cx + r * 0.55f, cy + i * r * 0.25f), 2f)
    // Fuse
    val fx = cx + r * 0.3f; val fy = cy - r
    drawLine(Color(0xFFFF8F00), Offset(fx, fy), Offset(fx + r * 0.22f, fy - r * 0.32f), 2.5f, StrokeCap.Round)
    drawLine(Color(0xFFFFCC00), Offset(fx + r * 0.22f, fy - r * 0.32f), Offset(fx + r * 0.38f, fy - r * 0.18f), 2f, StrokeCap.Round)
    drawCircle(Color(0xFFFFEA00), r * 0.09f, Offset(fx + r * 0.38f, fy - r * 0.18f))
    drawCircle(Color(0xCCFF8F00), r * 0.15f, Offset(fx + r * 0.38f, fy - r * 0.18f))
}

private fun DrawScope.drawBossBalloon(cx: Float, cy: Float, r: Float, hp: Int) {
    // Multi-layer aura
    drawCircle(Color(0x12FFD700), r * 1.6f, Offset(cx, cy))
    drawCircle(Color(0x20FFD700), r * 1.35f, Offset(cx, cy))
    drawCircle(Color(0x30A020D0), r * 1.16f, Offset(cx, cy))
    // Body
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF2D1B7E), Color(0xFF0A0620)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
    // Concentric golden rings
    drawCircle(Color(0xCCFFD700), r, Offset(cx, cy), style = Stroke(4f))
    drawCircle(Color(0x80FFF176), r * 0.94f, Offset(cx, cy), style = Stroke(1.5f))
    drawCircle(Color(0x30FFD700), r * 0.72f, Offset(cx, cy), style = Stroke(1f))
    drawCircle(Color(0x20FFD700), r * 0.48f, Offset(cx, cy), style = Stroke(1f))
    drawCircle(Color(0x20FFFFFF), r * 0.28f, Offset(cx - r * 0.28f, cy - r * 0.28f))
    // Crown
    val cY = cy - r * 0.82f; val cW = r * 0.58f; val cH = r * 0.34f; val cc = Color(0xFFFFD700)
    drawLine(cc, Offset(cx - cW, cY + cH), Offset(cx + cW, cY + cH), 3f)
    drawLine(cc, Offset(cx - cW, cY + cH), Offset(cx - cW, cY), 3f)
    drawLine(cc, Offset(cx + cW, cY + cH), Offset(cx + cW, cY), 3f)
    drawLine(cc, Offset(cx - cW, cY), Offset(cx - cW * 0.38f, cY - cH), 3f)
    drawLine(cc, Offset(cx - cW * 0.38f, cY - cH), Offset(cx, cY), 3f)
    drawLine(cc, Offset(cx, cY), Offset(cx, cY - cH), 3f)
    drawLine(cc, Offset(cx, cY - cH), Offset(cx + cW * 0.38f, cY), 3f)
    drawLine(cc, Offset(cx + cW * 0.38f, cY), Offset(cx + cW, cY - cH), 3f)
    drawLine(cc, Offset(cx + cW, cY - cH), Offset(cx + cW, cY), 3f)
    // HP dots (5 max, below balloon)
    val dotR = r * 0.08f; val dSp = r * 0.24f
    for (i in 0 until 5) {
        drawCircle(if (i < hp) Color(0xFFFFD700) else Color(0x40808080), dotR, Offset(cx - dSp * 2f + dSp * i, cy + r + r * 0.24f))
    }
}

@Preview(showBackground = true)
@Composable
private fun DefenseScreenPreview() {
    GameVaultTheme { DefenseScreen(rememberNavController()) }
}
