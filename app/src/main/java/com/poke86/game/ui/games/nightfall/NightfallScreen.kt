package com.poke86.game.ui.games.nightfall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Context
import androidx.navigation.NavController
import com.poke86.game.game.nightfall.NightfallKorge
import com.poke86.game.game.nightfall.NightfallSave
import korlibs.korge.android.KorgeAndroidView

/**
 * Compose 래퍼: 기존 Navigation 패턴에 맞춰 Korge 게임 뷰를 임베드한다.
 *
 * M0 PoC — 빈 Korge 씬이 Compose AndroidView 안에서 렌더되는지(R1 게이트) 검증.
 * 실제 게임 로직은 [NightfallKorge] (순수 Korge 엔진 코드)에 위치한다.
 */
@Composable
fun NightfallScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val prefs = ctx.getSharedPreferences("nightfall", Context.MODE_PRIVATE)
                NightfallSave.saver = { s -> prefs.edit().putString("save", s).apply() }
                NightfallSave.loader = { prefs.getString("save", null) }
                KorgeAndroidView(ctx).apply {
                    loadModule(NightfallKorge.config())
                }
            },
            onRelease = { view -> view.unloadModule() }
        )

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = Color.White
            )
        }
    }
}
