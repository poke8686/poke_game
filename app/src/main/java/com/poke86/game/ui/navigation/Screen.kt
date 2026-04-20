package com.poke86.game.ui.navigation

import com.poke86.game.Routes

sealed class Screen(val route: String) {
    object Home : Screen(Routes.HOME)
    object NunchiGame : Screen(Routes.NUNCHIGAME)
    object Reaction : Screen(Routes.REACTION)
    object Balance : Screen(Routes.BALANCE)
    object WordChain : Screen(Routes.WORDCHAIN)
    object Memory : Screen(Routes.MEMORY)
    object ColorTest : Screen(Routes.COLORTEST)
    object Spy : Screen(Routes.SPY)
    object Chosung : Screen(Routes.CHOSUNG)
    object Defense : Screen(Routes.DEFENSE)
    object TowerDefense : Screen(Routes.TOWER_DEFENSE)
}
