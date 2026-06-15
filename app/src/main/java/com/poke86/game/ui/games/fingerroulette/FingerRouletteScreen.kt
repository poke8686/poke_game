package com.poke86.game.ui.games.fingerroulette

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Finger(
    val id: PointerId,
    val position: androidx.compose.ui.geometry.Offset,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerRouletteScreen(navController: NavController) {
    val fingers = remember { mutableStateMapOf<PointerId, Finger>() }
    var winnerId by remember { mutableStateOf<PointerId?>(null) }
    var isRouletteRunning by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFF4081), Color(0xFFE040FB), 
        Color(0xFF7C4DFF), Color(0xFF536DFE), Color(0xFF448AFF),
        Color(0xFF40C4FF), Color(0xFF18FFFF), Color(0xFF64FFDA),
        Color(0xFFB2FF59), Color(0xFFEEFF41), Color(0xFFFFFF00)
    )

    val scope = rememberCoroutineScope()

    // 룰렛 로직
    LaunchedEffect(fingers.size) {
        if (fingers.size >= 2 && !isRouletteRunning) {
            isRouletteRunning = true
            winnerId = null
            
            // 3초 대기
            for (i in 3 downTo 1) {
                countdown = i
                delay(1000)
                if (fingers.size < 2) break
            }
            
            if (fingers.size >= 2) {
                countdown = 0
                winnerId = fingers.keys.toList().random()
            }
            isRouletteRunning = false
        } else if (fingers.size < 2) {
            isRouletteRunning = false
            winnerId = null
            countdown = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("손가락 룰렛") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            // 모든 터치 정보 업데이트
                            event.changes.forEach { change: PointerInputChange ->
                                if (change.pressed) {
                                    if (!fingers.containsKey(change.id)) {
                                        fingers[change.id] = Finger(
                                            id = change.id,
                                            position = change.position,
                                            color = colors[Random.nextInt(colors.size)]
                                        )
                                    } else {
                                        fingers[change.id] = fingers[change.id]!!.copy(position = change.position)
                                    }
                                } else {
                                    fingers.remove(change.id)
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fingers.values.forEach { finger ->
                    val isWinner = finger.id == winnerId
                    
                    // 손가락 위치에 원 그리기
                    drawCircle(
                        color = if (winnerId != null && !isWinner) Color.LightGray else finger.color,
                        center = finger.position,
                        radius = if (isWinner) 120f else 100f,
                        style = if (isWinner) Stroke(width = 20f) else androidx.compose.ui.graphics.drawscope.Fill
                    )
                    
                    if (!isWinner) {
                        drawCircle(
                            color = finger.color,
                            center = finger.position,
                            radius = 100f
                        )
                    }
                }
            }

            if (winnerId == null && fingers.size >= 2 && countdown > 0) {
                Text(
                    text = countdown.toString(),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 100.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (fingers.size < 2 && winnerId == null) {
                Text(
                    text = "두 명 이상의 손가락을 대주세요",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 20.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
