package com.poke86.game.ui.games.spotdiff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.poke86.game.data.datasource.local.SpotDiffProgressStore
import com.poke86.game.network.SpotDiffApi
import com.poke86.game.network.SpotDiffPoint
import com.poke86.game.network.SpotDiffStageDetail
import com.poke86.game.network.SpotDiffStageSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.random.Random

// ─── Procedural fallback (server unavailable) ───────────────────────────────

enum class SpotShapeKind { CIRCLE, SQUARE, TRI }

data class SpotShape(
    val kind: SpotShapeKind, val cx: Float, val cy: Float,
    val size: Float, val argb: Int,
)

data class FallbackStage(
    val shapesA: List<SpotShape>,
    val shapesB: List<SpotShape>,
    val diffs: List<SpotDiffPoint>,
)

private val PALETTE = intArrayOf(
    0xFFE57373.toInt(), 0xFFBA68C8.toInt(), 0xFF7986CB.toInt(),
    0xFF4FC3F7.toInt(), 0xFF81C784.toInt(), 0xFFFFD54F.toInt(),
    0xFFFFB74D.toInt(), 0xFFA1887F.toInt(), 0xFF90A4AE.toInt(),
)

internal fun buildFallback(shapeCount: Int, diffCount: Int, seed: Long): FallbackStage {
    val rnd = Random(seed)
    val gridCols = 4
    val gridRows = (shapeCount + gridCols - 1) / gridCols
    val cellW = 1f / gridCols
    val cellH = 1f / gridRows
    val cells = (0 until gridCols * gridRows).shuffled(rnd).take(shapeCount)
    val base = cells.map { idx ->
        val col = idx % gridCols; val row = idx / gridCols; val pad = 0.18f
        SpotShape(
            SpotShapeKind.values()[rnd.nextInt(3)],
            (col + pad + rnd.nextFloat() * (1f - 2 * pad)) * cellW,
            (row + pad + rnd.nextFloat() * (1f - 2 * pad)) * cellH,
            0.04f + rnd.nextFloat() * 0.04f,
            PALETTE[rnd.nextInt(PALETTE.size)],
        )
    }
    val diffIdx = base.indices.shuffled(rnd).take(diffCount).toSet()
    val right = base.mapIndexed { i, s ->
        if (i !in diffIdx) s
        else when (rnd.nextInt(3)) {
            0 -> { var c = PALETTE[rnd.nextInt(PALETTE.size)]; while (c == s.argb) c = PALETTE[rnd.nextInt(PALETTE.size)]; s.copy(argb = c) }
            1 -> { val o = SpotShapeKind.values().filter { it != s.kind }; s.copy(kind = o[rnd.nextInt(o.size)]) }
            else -> { s.copy(size = (s.size * (if (rnd.nextBoolean()) 1.6f else 0.6f)).coerceIn(0.025f, 0.09f)) }
        }
    }
    return FallbackStage(base, right, diffIdx.map { SpotDiffPoint(right[it].cx, right[it].cy, 0.07f) })
}

private data class FallbackSpec(val shapes: Int, val diffs: Int, val timeSec: Int, val difficulty: Int)

private val FALLBACK_SPECS = listOf(
    FallbackSpec(8,  3, 45, 1), FallbackSpec(12, 4, 50, 2), FallbackSpec(16, 5, 60, 3),
    FallbackSpec(20, 6, 75, 4), FallbackSpec(24, 7, 90, 5),
)

// ─── State / ViewModel ──────────────────────────────────────────────────────

enum class SpotDiffPhase { LOADING, STAGE_SELECT, PLAYING, STAGE_CLEAR, OVER }

data class SpotDiffUiState(
    val phase: SpotDiffPhase = SpotDiffPhase.LOADING,
    val stages: List<SpotDiffStageSummary> = emptyList(),
    val clearedOrder: Int = 0,
    val currentOrder: Int = 1,
    val detail: SpotDiffStageDetail? = null,
    val fallback: FallbackStage? = null,
    val foundIdx: Set<Int> = emptySet(),
    val remainSec: Int = 60,
    val totalScore: Int = 0,
)

@HiltViewModel
class SpotDiffViewModel @Inject constructor(
    private val api: SpotDiffApi,
    private val progressStore: SpotDiffProgressStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SpotDiffUiState())
    val state: StateFlow<SpotDiffUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cleared = progressStore.loadClearedOrder()
            val stages = api.listStages().getOrElse { emptyList() }
            _state.update {
                it.copy(
                    stages = stages, clearedOrder = cleared,
                    phase = if (stages.isEmpty()) SpotDiffPhase.STAGE_SELECT
                            else SpotDiffPhase.STAGE_SELECT,
                )
            }
        }
    }

    fun selectStage(order: Int) {
        val st = _state.value
        if (order > st.clearedOrder + 1) return
        val summary = st.stages.find { it.order == order }
        viewModelScope.launch {
            _state.update { it.copy(phase = SpotDiffPhase.LOADING, currentOrder = order) }
            if (summary != null) {
                api.getStage(summary.id).fold(
                    onSuccess = { detail ->
                        _state.update {
                            it.copy(phase = SpotDiffPhase.PLAYING, detail = detail,
                                fallback = null, foundIdx = emptySet(),
                                remainSec = 45 + detail.difficulty * 15)
                        }
                        startTimer()
                    },
                    onFailure = { startFallback(order) }
                )
            } else {
                startFallback(order)
            }
        }
    }

    private fun startFallback(order: Int) {
        val spec = FALLBACK_SPECS[(order - 1).coerceAtMost(FALLBACK_SPECS.lastIndex)]
        _state.update {
            it.copy(phase = SpotDiffPhase.PLAYING, detail = null,
                fallback = buildFallback(spec.shapes, spec.diffs, System.currentTimeMillis()),
                foundIdx = emptySet(), remainSec = spec.timeSec)
        }
        startTimer()
    }

    private var timerJob: Job? = null
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.phase == SpotDiffPhase.PLAYING && _state.value.remainSec > 0) {
                delay(1000)
                _state.update { it.copy(remainSec = it.remainSec - 1) }
            }
            if (_state.value.phase == SpotDiffPhase.PLAYING) {
                _state.update { it.copy(phase = SpotDiffPhase.OVER) }
            }
        }
    }

    fun tap(norm: Offset) {
        val st = _state.value
        if (st.phase != SpotDiffPhase.PLAYING) return
        val diffs: List<SpotDiffPoint> = st.detail?.diffs ?: st.fallback?.diffs ?: return
        val candidate = diffs.mapIndexed { i, p ->
            i to hypot((p.x - norm.x).toDouble(), (p.y - norm.y).toDouble())
        }.filter { (i, _) -> i !in st.foundIdx }.minByOrNull { it.second }

        if (candidate != null && candidate.second < diffs[candidate.first].r.toDouble()) {
            val newFound = st.foundIdx + candidate.first
            val newScore = st.totalScore + 50 + st.remainSec
            if (newFound.size >= diffs.size) {
                timerJob?.cancel()
                viewModelScope.launch {
                    progressStore.saveClearedOrder(st.currentOrder)
                    val newCleared = progressStore.loadClearedOrder()
                    _state.update {
                        it.copy(foundIdx = newFound, totalScore = newScore,
                            phase = SpotDiffPhase.STAGE_CLEAR, clearedOrder = newCleared)
                    }
                }
            } else {
                _state.update { it.copy(foundIdx = newFound, totalScore = newScore) }
            }
        } else {
            _state.update {
                it.copy(totalScore = (st.totalScore - 5).coerceAtLeast(0),
                    remainSec = (st.remainSec - 3).coerceAtLeast(0))
            }
        }
    }

    fun goToStageSelect() {
        timerJob?.cancel()
        viewModelScope.launch {
            val cleared = progressStore.loadClearedOrder()
            _state.update { it.copy(phase = SpotDiffPhase.STAGE_SELECT, clearedOrder = cleared) }
        }
    }

    fun nextStage() {
        val st = _state.value
        val maxOrder = st.stages.maxOfOrNull { it.order } ?: FALLBACK_SPECS.size
        if (st.currentOrder + 1 > maxOrder) _state.update { it.copy(phase = SpotDiffPhase.OVER) }
        else selectStage(st.currentOrder + 1)
    }
}

// ─── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDiffScreen(
    navController: NavController,
    vm: SpotDiffViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("틀린그림찾기") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.goToStageSelect()
                        navController.popBackStack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.phase == SpotDiffPhase.PLAYING) {
                val totalDiffs = (state.detail?.diffs?.size ?: state.fallback?.diffs?.size) ?: 0
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HudCell("스테이지", "★${state.detail?.difficulty ?: state.currentOrder} / ${state.currentOrder}",
                        MaterialTheme.colorScheme.primary)
                    HudCell("찾음", "${state.foundIdx.size}/$totalDiffs", Color(0xFF43A047.toInt()))
                    HudCell("시간", "${state.remainSec}s",
                        if (state.remainSec < 10) Color(0xFFE53935.toInt()) else MaterialTheme.colorScheme.tertiary)
                    HudCell("점수", state.totalScore.toString(), Color(0xFFFF8F00.toInt()))
                }
                Spacer(Modifier.height(6.dp))
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (state.phase) {
                    SpotDiffPhase.LOADING -> Text("불러오는 중…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    SpotDiffPhase.STAGE_SELECT -> StageSelectPanel(
                        stages = state.stages,
                        clearedOrder = state.clearedOrder,
                        onSelect = { vm.selectStage(it) },
                    )

                    SpotDiffPhase.PLAYING -> PlayPanel(
                        detail = state.detail,
                        fallback = state.fallback,
                        foundIdx = state.foundIdx,
                        onTap = { vm.tap(it) },
                    )

                    SpotDiffPhase.STAGE_CLEAR -> StageClearPanel(
                        order = state.currentOrder,
                        totalStages = state.stages.size.takeIf { it > 0 } ?: FALLBACK_SPECS.size,
                        score = state.totalScore,
                        onNext = { vm.nextStage() },
                        onSelect = { vm.goToStageSelect() },
                    )

                    SpotDiffPhase.OVER -> OverPanel(
                        score = state.totalScore,
                        onRestart = { vm.goToStageSelect() },
                    )
                }
            }
        }
    }
}

// ─── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun HudCell(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun StageSelectPanel(
    stages: List<SpotDiffStageSummary>,
    clearedOrder: Int,
    onSelect: (Int) -> Unit,
) {
    val totalCount = stages.size.takeIf { it > 0 } ?: FALLBACK_SPECS.size
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("스테이지 선택", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("클리어: $clearedOrder / $totalCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (stages.isEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(FALLBACK_SPECS) { spec ->
                    val locked = spec.difficulty > clearedOrder + 1
                    StageCard(order = spec.difficulty, title = "스테이지 ${spec.difficulty}",
                        stars = spec.difficulty, diffCount = spec.diffs, locked = locked,
                        onClick = { if (!locked) onSelect(spec.difficulty) })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(stages.sortedBy { it.order }) { s ->
                    val locked = s.order > clearedOrder + 1
                    StageCard(order = s.order, title = s.title, stars = s.difficulty,
                        diffCount = s.diffCount, locked = locked,
                        onClick = { if (!locked) onSelect(s.order) })
                }
            }
        }
    }
}

@Composable
private fun StageCard(
    order: Int, title: String, stars: Int, diffCount: Int,
    locked: Boolean, onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.alpha(if (locked) 0.45f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (locked) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primaryContainer
        ),
    ) {
        Column(
            Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (locked) "🔒" else "🔍", fontSize = 28.sp)
            Spacer(Modifier.size(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("★".repeat(stars.coerceAtMost(5)) + if (stars > 5) "+${stars - 5}" else "",
                fontSize = 11.sp, color = Color(0xFFFFB300.toInt()))
            Text("차이 ${diffCount}개", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayPanel(
    detail: SpotDiffStageDetail?,
    fallback: FallbackStage?,
    foundIdx: Set<Int>,
    onTap: (Offset) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (detail != null) {
            val foundOffsets = foundIdx.map { Offset(detail.diffs[it].x, detail.diffs[it].y) }
            PhotoBox(url = detail.imageAUrl, foundPoints = emptyList(), tappable = false, onTap = {})
            PhotoBox(url = detail.imageBUrl, foundPoints = foundOffsets, tappable = true, onTap = onTap)
        } else if (fallback != null) {
            val foundOffsets = foundIdx.map { Offset(fallback.diffs[it].x, fallback.diffs[it].y) }
            DrawingBox(shapes = fallback.shapesA, foundCircles = emptyList(), tappable = false, onTap = {})
            DrawingBox(shapes = fallback.shapesB, foundCircles = foundOffsets, tappable = true, onTap = onTap)
        }
    }
}

@Composable
private fun PhotoBox(
    url: String, foundPoints: List<Offset>, tappable: Boolean, onTap: (Offset) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
        shadowElevation = 2.dp,
    ) {
        Box {
            AsyncImage(model = url, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Canvas(modifier = Modifier.fillMaxSize().let { base ->
                if (tappable) base.pointerInput(Unit) {
                    detectTapGestures { off -> onTap(Offset(off.x / size.width, off.y / size.height)) }
                } else base
            }) {
                foundPoints.forEach { norm ->
                    drawCircle(Color(0xFFFF1744.toInt()), radius = size.width * 0.06f,
                        center = Offset(norm.x * size.width, norm.y * size.height),
                        style = Stroke(width = 5f))
                }
            }
        }
    }
}

@Composable
private fun DrawingBox(
    shapes: List<SpotShape>, foundCircles: List<Offset>,
    tappable: Boolean, onTap: (Offset) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
        color = Color(0xFFF5F5F5.toInt()), shadowElevation = 2.dp,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA.toInt())).let { base ->
            if (tappable) base.pointerInput(Unit) {
                detectTapGestures { off -> onTap(Offset(off.x / size.width, off.y / size.height)) }
            } else base
        }) {
            drawRect(Color(0xFFBDBDBD.toInt()), size = Size(size.width, size.height), style = Stroke(2f))
            shapes.forEach { s ->
                val cx = s.cx * size.width; val cy = s.cy * size.height; val sz = s.size * size.width
                val c = Color(s.argb)
                when (s.kind) {
                    SpotShapeKind.CIRCLE -> drawCircle(c, radius = sz, center = Offset(cx, cy))
                    SpotShapeKind.SQUARE -> drawRect(c, topLeft = Offset(cx - sz, cy - sz), size = Size(sz * 2, sz * 2))
                    SpotShapeKind.TRI -> drawPath(Path().apply {
                        moveTo(cx, cy - sz); lineTo(cx - sz, cy + sz); lineTo(cx + sz, cy + sz); close()
                    }, c)
                }
            }
            foundCircles.forEach { norm ->
                drawCircle(Color(0xFFFF1744.toInt()), radius = size.width * 0.06f,
                    center = Offset(norm.x * size.width, norm.y * size.height), style = Stroke(5f))
            }
        }
    }
}

@Composable
private fun StageClearPanel(
    order: Int, totalStages: Int, score: Int,
    onNext: () -> Unit, onSelect: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✅ 스테이지 $order 클리어!", fontSize = 22.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF43A047.toInt()))
        Spacer(Modifier.height(12.dp))
        Text("누적 점수: $score", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSelect) { Text("스테이지 선택") }
            if (order < totalStages) Button(onClick = onNext) { Text("다음 ▶") }
        }
    }
}

@Composable
private fun OverPanel(score: Int, onRestart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⏱️ 시간 초과", fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFE53935.toInt()))
        Spacer(Modifier.height(12.dp))
        Text("점수: $score", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRestart) { Text("스테이지 선택") }
    }
}
