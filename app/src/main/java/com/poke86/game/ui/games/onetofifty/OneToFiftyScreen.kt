package com.poke86.game.ui.games.onetofifty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneToFiftyScreen(navController: NavController) {
    var numbers by remember { mutableStateOf((1..25).shuffled()) }
    var nextNumbers by remember { mutableStateOf((26..50).shuffled()) }
    var nextTarget by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf(0L) }
    var currentTime by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            startTime = System.currentTimeMillis()
            while (isRunning) {
                currentTime = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("1 to 50") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이머 표시
            Text(
                text = String.format("%.2f", currentTime / 1000f) + "s",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isFinished) "완료!" else "다음 숫자: $nextTarget",
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5x5 그리드
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(numbers.indices.toList()) { index ->
                        val number = numbers[index]
                        NumberTile(
                            number = number,
                            isVisible = number != 0,
                            onClick = {
                                if (!isFinished) {
                                    if (!isRunning && nextTarget == 1) {
                                        isRunning = true
                                    }
                                    
                                    if (number == nextTarget) {
                                        val newNumbers = numbers.toMutableList()
                                        if (nextTarget <= 25) {
                                            // 1~25를 누르면 26~50 중 하나로 교체
                                            newNumbers[index] = nextNumbers[nextTarget - 1 - 0] // logic needs fix
                                            // Actually, nextNumbers has 25 items (26..50). 
                                            // If nextTarget is 1, we take nextNumbers[0] which is 26? No, nextNumbers is shuffled.
                                            // Let's use a simpler way to pick from nextNumbers.
                                        } else {
                                            newNumbers[index] = 0 // 26 이상이면 빈칸
                                        }
                                        
                                        // Fix the logic for picking next number
                                        val pickedNext = if (nextTarget <= 25) {
                                            nextNumbers[nextTarget - 1]
                                        } else {
                                            0
                                        }
                                        newNumbers[index] = pickedNext
                                        numbers = newNumbers
                                        
                                        nextTarget++
                                        if (nextTarget > 50) {
                                            isRunning = false
                                            isFinished = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (isFinished) {
                Button(
                    onClick = {
                        numbers = (1..25).shuffled()
                        nextNumbers = (26..50).shuffled()
                        nextTarget = 1
                        currentTime = 0L
                        isFinished = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("다시 하기")
                }
            }
        }
    }
}

@Composable
fun NumberTile(number: Int, isVisible: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = isVisible) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isVisible) {
                if (number <= 25) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.tertiaryContainer
            } else Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isVisible) {
                Text(
                    text = number.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
