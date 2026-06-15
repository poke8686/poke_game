package com.poke86.game.ui.games.bombpass

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BombPassScreen(navController: NavController) {
    val topics = listOf(
        "과일 이름", "나라 이름", "드라마 제목", "가수/그룹 이름", 
        "무한도전 멤버", "편의점 상품", "치킨 브랜드", "채소 이름",
        "운동 종목", "컴퓨터 부품", "애니메이션 제목", "삼글자 단어",
        "색깔 이름", "학용품", "가전제품", "스마트폰 앱 이름"
    )

    var currentTopic by remember { mutableStateOf(topics.random()) }
    var isRunning by remember { mutableStateOf(false) }
    var isExploded by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(0f) }
    var totalTime by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "bomb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (timeLeft < 5f) 200 else 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(isRunning) {
        if (isRunning) {
            isExploded = false
            totalTime = Random.nextInt(20, 45).toFloat() // 20~45초 사이 랜덤
            timeLeft = totalTime
            
            while (timeLeft > 0) {
                delay(100)
                timeLeft -= 0.1f
                if (timeLeft <= 0) {
                    isExploded = true
                    isRunning = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시한폭탄") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isRunning && !isExploded) {
                Text(
                    "스마트폰을 들고 단어를 말한 뒤\n다음 사람에게 넘기세요!",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        currentTopic = topics.random()
                        isRunning = true
                    },
                    modifier = Modifier.height(60.dp).fillMaxWidth(0.7f)
                ) {
                    Text("게임 시작", fontSize = 20.sp)
                }
            } else if (isRunning) {
                Text(
                    "현재 주제",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    currentTopic,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(60.dp))
                
                // 폭탄 아이콘 (단순 원으로 대체)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(if (timeLeft < 5f) Color.Red else Color.Black)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "💣",
                        fontSize = (60 * scale).sp
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    "단어를 말하고 넘기세요!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (isExploded) {
                Text(
                    "펑!!!",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Red
                )
                Text(
                    "당신이 탈락입니다!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = {
                        isExploded = false
                    },
                    modifier = Modifier.height(60.dp).fillMaxWidth(0.7f)
                ) {
                    Text("다시 하기", fontSize = 20.sp)
                }
            }
        }
    }
}
