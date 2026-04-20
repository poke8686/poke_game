package com.poke86.game.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.poke86.game.R
import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.model.GameTag
import com.poke86.game.ui.home.components.CategoryChips
import com.poke86.game.ui.home.components.GameCard
import com.poke86.game.ui.theme.GameVaultTheme

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val filteredGames by viewModel.filteredGames.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    HomeContent(
        categories = categories,
        filteredGames = filteredGames,
        selectedCategory = selectedCategory,
        onCategorySelected = viewModel::onCategorySelected,
        onGameClick = { navController.navigate(it.route) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    categories: List<Category>,
    filteredGames: List<Game>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onGameClick: (Game) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CategoryChips(
                categories = categories,
                selectedCategoryId = selectedCategory,
                onCategorySelected = onCategorySelected
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    GameCard(game = game, onClick = { onGameClick(game) })
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentPreview() {
    GameVaultTheme {
        HomeContent(
            categories = listOf(
                Category("all", "전체"), Category("party", "파티"),
                Category("solo", "혼자"), Category("reflex", "반응속도"),
                Category("brain", "두뇌")
            ),
            filteredGames = listOf(
                Game("nunchigame", "눈치 게임", "번호 겹치면 탈락!", "👁️",
                    listOf("party"), listOf(GameTag.MULTI), "game/nunchigame"),
                Game("reaction", "반응속도 대결", "화면 변화에 반응하라", "⚡",
                    listOf("party"), listOf(GameTag.MULTI, GameTag.QUICK), "game/reaction"),
                Game("memory", "숫자 기억", "단기 기억력 테스트", "🔢",
                    listOf("solo"), listOf(GameTag.SOLO, GameTag.BRAIN), "game/memory")
            ),
            selectedCategory = "all",
            onCategorySelected = {},
            onGameClick = {}
        )
    }
}
