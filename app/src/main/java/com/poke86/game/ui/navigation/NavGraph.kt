package com.poke86.game.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poke86.game.ui.games.balance.BalanceScreen
import com.poke86.game.ui.games.chosung.ChosungScreen
import com.poke86.game.ui.games.colortest.ColorTestScreen
import com.poke86.game.ui.games.memory.MemoryScreen
import com.poke86.game.ui.games.nunchigame.NunchiGameScreen
import com.poke86.game.ui.games.nunchigame.multi.NunchiMultiScreen
import com.poke86.game.ui.games.reaction.ReactionScreen
import com.poke86.game.ui.games.spy.SpyScreen
import com.poke86.game.ui.games.defense.DefenseScreen
import com.poke86.game.ui.games.towerdefense.TowerDefenseScreen
import com.poke86.game.ui.games.wordchain.WordChainScreen
import com.poke86.game.ui.home.HomeScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.NunchiGame.route) { NunchiGameScreen(navController) }
        composable(com.poke86.game.Routes.NUNCHI_MULTI) { NunchiMultiScreen(navController) }
        composable(Screen.Reaction.route) { ReactionScreen(navController) }
        composable(Screen.Balance.route) { BalanceScreen(navController) }
        composable(Screen.WordChain.route) { WordChainScreen(navController) }
        composable(Screen.Memory.route) { MemoryScreen(navController) }
        composable(Screen.ColorTest.route) { ColorTestScreen(navController) }
        composable(Screen.Spy.route) { SpyScreen(navController) }
        composable(Screen.Chosung.route) { ChosungScreen(navController) }
        composable(Screen.Defense.route) { DefenseScreen(navController) }
        composable(Screen.TowerDefense.route) { TowerDefenseScreen(navController) }
    }
}
