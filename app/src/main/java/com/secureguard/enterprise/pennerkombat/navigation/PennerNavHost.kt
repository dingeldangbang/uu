package com.secureguard.enterprise.pennerkombat.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secureguard.enterprise.pennerkombat.model.FighterDatabase
import com.secureguard.enterprise.pennerkombat.model.GameMode
import com.secureguard.enterprise.pennerkombat.ui.screens.*

object PennerRoutes {
    const val MAIN_MENU = "main_menu"
    const val CHARACTER_SELECT = "char_select/{mode}"
    const val ARENA = "arena/{p1Id}/{p2Id}/{mode}/{difficulty}"
    const val STORY = "story"
    const val TROPHIES = "trophies"
    const val OPTIONS = "options"

    fun charSelect(mode: String) = "char_select/$mode"
    fun arena(p1Id: String, p2Id: String, mode: String, difficulty: Int) = "arena/$p1Id/$p2Id/$mode/$difficulty"
}

@Composable
fun PennerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var difficulty by remember { mutableStateOf(2) }

    NavHost(
        navController = navController,
        startDestination = PennerRoutes.MAIN_MENU,
        modifier = modifier
    ) {
        composable(PennerRoutes.MAIN_MENU) {
            MainMenuScreen(
                onPlayArcade = { navController.navigate(PennerRoutes.charSelect("arcade")) },
                onPlayVersus = { navController.navigate(PennerRoutes.charSelect("versus")) },
                onStory = { navController.navigate(PennerRoutes.STORY) },
                onTrophies = { navController.navigate(PennerRoutes.TROPHIES) },
                onOptions = { navController.navigate(PennerRoutes.OPTIONS) }
            )
        }

        composable(
            route = PennerRoutes.CHARACTER_SELECT,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "arcade"
            CharacterSelectScreen(
                mode = mode,
                onFight = { p1, p2 ->
                    navController.navigate(PennerRoutes.arena(p1.id, p2.id, mode, difficulty))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PennerRoutes.ARENA,
            arguments = listOf(
                navArgument("p1Id") { type = NavType.StringType },
                navArgument("p2Id") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val p1Id = backStackEntry.arguments?.getString("p1Id") ?: "le_binde"
            val p2Id = backStackEntry.arguments?.getString("p2Id") ?: "mell"
            val modeStr = backStackEntry.arguments?.getString("mode") ?: "arcade"
            val diff = backStackEntry.arguments?.getInt("difficulty") ?: 2
            val p1 = FighterDatabase.getById(p1Id)
            val p2 = FighterDatabase.getById(p2Id)
            val gameMode = if (modeStr == "versus") GameMode.VERSUS else GameMode.ARCADE

            ArenaScreen(
                p1Fighter = p1,
                p2Fighter = p2,
                gameMode = gameMode,
                difficulty = diff,
                onExit = {
                    navController.popBackStack(PennerRoutes.MAIN_MENU, false)
                }
            )
        }

        composable(PennerRoutes.STORY) {
            StoryModeScreen(
                onStartChapter = { chapter, playerFighter, opponent ->
                    // Start story fight
                    navController.navigate(PennerRoutes.arena(playerFighter.id, opponent.id, "story", difficulty))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(PennerRoutes.TROPHIES) {
            TrophyScreen(onBack = { navController.popBackStack() })
        }

        composable(PennerRoutes.OPTIONS) {
            OptionsScreen(
                onBack = { navController.popBackStack() },
                difficulty = difficulty,
                onDifficultyChange = { difficulty = it }
            )
        }
    }
}
