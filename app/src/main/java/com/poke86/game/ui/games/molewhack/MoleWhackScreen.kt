package com.poke86.game.ui.games.molewhack

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val GRID_SIZE = 9            // 3x3
private const val GAME_DURATION_MS = 60_000L
private const val INITIAL_INTERVAL_MS = 900L
private const val MIN_INTERVAL_MS = 280L
private const val MOLE_LIFETIME_MS = 1100L

private data class MoleInstance(val cell: Int, val spawnedAt: Long, val expiresAt: Long)

private enum class MolePhase { READY, PLAYING, OVER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoleWhackScreen(navController: NavController) {
    var phase by remember { mutableStateOf(MolePhase.READY) }
    var score by remember { mutableStateOf(0) }
    var misses by remember { mutableStateOf(0) }
    var combo by remember { mutableStateOf(0) }
    var remainMs by remember { mutableStateOf(GAME_DURATION_MS) }
    var moles by remember { mutableStateOf<List<MoleInstance>>(emptyList()) }
    var bestScore by remember { mutableStateOf(0) }

    // Spawner — interval shrinks as elapsed time grows
    LaunchedEffect(phase) {
        if (phase != MolePhase.PLAYING) return@LaunchedEffect
        while (phase == MolePhase.PLAYING) {
            val elapsedRatio = 1f - (remainMs.toFloat() / GAME_DURATION_MS)
            val interval = (INITIAL_INTERVAL_MS - (INITIAL_INTERVAL_MS - MIN_INTERVAL_MS) * elapsedRatio).toLong()
            delay(interval.coerceAtLeast(120L))
            if (phase != MolePhase.PLAYING) break

            val occupied = moles.map { it.cell }.toSet()
            if (occupied.size >= GRID_SIZE - 1) continue
            val freeCells = (0 until GRID_SIZE).filterNot { it in occupied }
            if (freeCells.isEmpty()) continue
            val cell = freeCells[Random.nextInt(freeCells.size)]
            val now = System.currentTimeMillis()
            moles = moles + MoleInstance(cell, now, now + MOLE_LIFETIME_MS)
        }
    }

    // Game timer + expiry sweep
    LaunchedEffect(phase) {
        if (phase != MolePhase.PLAYING) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (phase == MolePhase.PLAYING) {
            val now = System.currentTimeMillis()
            val elapsed = now - startedAt
            remainMs = (GAME_DURATION_MS - elapsed).coerceAtLeast(0L)

            // Prune expired moles → 놓친 두더지는 콤보 초기화
            val expired = moles.filter { now >= it.expiresAt }
            if (expired.isNotEmpty()) {
                moles = moles - expired.toSet()
                combo = 0
            }

            if (remainMs <= 0L) {
                phase = MolePhase.OVER
                if (score > bestScore) bestScore = score
                break
            }
            delay(40)
        }
    }

    val activeCells = moles.map { it.cell }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("두더지잡기") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudCell("점수", score.toString(), MaterialTheme.colorScheme.primary)
                HudCell("콤보", "x$combo", Color(0xFFFF8F00.toInt()))
                HudCell(
                    "남은 시간", "${(remainMs + 500) / 1000}s",
                    if (remainMs < 10_000) Color(0xFFE53935.toInt()) else MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "최고점수: $bestScore",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (phase) {
                    MolePhase.READY -> ReadyPanel(onStart = {
                        score = 0; misses = 0; combo = 0
                        remainMs = GAME_DURATION_MS
                        moles = emptyList()
                        phase = MolePhase.PLAYING
                    })

                    MolePhase.PLAYING -> MoleGrid(
                        activeCells = activeCells,
                        onMoleTap = { cell ->
                            val hit = moles.firstOrNull { it.cell == cell }
                            if (hit != null) {
                                moles = moles - hit
                                combo += 1
                                score += 10 + (combo - 1) * 2
                            } else {
                                combo = 0
                                misses += 1
                                score = (score - 3).coerceAtLeast(0)
                            }
                        }
                    )

                    MolePhase.OVER -> ResultPanel(
                        score = score, misses = misses, best = bestScore,
                        onRestart = {
                            score = 0; misses = 0; combo = 0
                            remainMs = GAME_DURATION_MS
                            moles = emptyList()
                            phase = MolePhase.PLAYING
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HudCell(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun ReadyPanel(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🦫", fontSize = 80.sp)
        Spacer(Modifier.height(12.dp))
        Text("두더지를 빠르게 잡아라!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "60초 / 콤보 보너스 / 잘못 누르면 -3점",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onStart) { Text("시작") }
    }
}

@Composable
private fun ResultPanel(score: Int, misses: Int, best: Int, onRestart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "종료!", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text("점수: $score", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("실수: $misses 회", style = MaterialTheme.typography.bodyMedium)
        if (score == best && best > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "🏆 신기록!", fontSize = 20.sp, color = Color(0xFFFFB300.toInt()),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRestart) { Text("다시하기") }
    }
}

@Composable
private fun MoleGrid(activeCells: Set<Int>, onMoleTap: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items((0 until GRID_SIZE).toList()) { cell ->
            MoleHole(isActive = cell in activeCells, onTap = { onMoleTap(cell) })
        }
    }
}

@Composable
private fun MoleHole(isActive: Boolean, onTap: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.2f,
        animationSpec = tween(durationMillis = 140),
        label = "moleScale"
    )
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF6D4C41.toInt()),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(60.dp)
                    .background(Color(0xFF3E2723.toInt()), CircleShape)
            )
            if (isActive || scale > 0.25f) {
                Text("🦫", fontSize = 44.sp, modifier = Modifier.scale(scale))
            }
        }
    }
}
