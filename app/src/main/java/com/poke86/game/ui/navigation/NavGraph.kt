package com.poke86.game.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poke86.game.ui.chat.ChatListScreen
import com.poke86.game.ui.chat.ChatRoomScreen
import com.poke86.game.ui.games.balance.BalanceScreen
import com.poke86.game.ui.games.bombpass.BombPassScreen
import com.poke86.game.ui.games.chosung.ChosungScreen
import com.poke86.game.ui.games.colortest.ColorTestScreen
import com.poke86.game.ui.games.defense.DefenseScreen
import com.poke86.game.ui.games.fingerroulette.FingerRouletteScreen
import com.poke86.game.ui.games.memory.MemoryScreen
import com.poke86.game.ui.games.molewhack.MoleWhackScreen
import com.poke86.game.ui.games.nightfall.NightfallScreen
import com.poke86.game.ui.games.nunchigame.NunchiGameScreen
import com.poke86.game.ui.games.nunchigame.multi.NunchiMultiScreen
import com.poke86.game.ui.games.onetofifty.OneToFiftyScreen
import com.poke86.game.ui.games.reaction.ReactionScreen
import com.poke86.game.ui.games.spotdiff.SpotDiffScreen
import com.poke86.game.ui.games.spy.SpyScreen
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
        composable(Screen.OneToFifty.route) { OneToFiftyScreen(navController) }
        composable(Screen.FingerRoulette.route) { FingerRouletteScreen(navController) }
        composable(Screen.BombPass.route) { BombPassScreen(navController) }
        composable(Screen.MoleWhack.route) { MoleWhackScreen(navController) }
        composable(Screen.SpotDiff.route) { SpotDiffScreen(navController) }
        composable(Screen.Nightfall.route) { NightfallScreen(navController) }
        composable(Screen.ChatList.route) { ChatListScreen(navController) }
        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val roomName = backStackEntry.arguments?.getString("roomName") ?: ""
            ChatRoomScreen(navController, roomId, roomName)
        }
    }
}
