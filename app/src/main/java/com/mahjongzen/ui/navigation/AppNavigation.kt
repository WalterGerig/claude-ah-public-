package com.mahjongzen.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mahjongzen.AppContainer
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.ui.game.GameScreen
import com.mahjongzen.ui.game.GameViewModel
import com.mahjongzen.ui.home.HomeScreen
import com.mahjongzen.ui.levelselect.LevelSelectScreen
import com.mahjongzen.ui.settings.SettingsScreen
import com.mahjongzen.ui.stats.StatsScreen

private object Routes {
    const val HOME = "home"
    const val SELECT = "select"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val GAME = "game?mode={mode}&layout={layout}&difficulty={difficulty}&seed={seed}"

    /** Only non-empty arguments are added; missing ones fall back to their defaults. */
    fun game(mode: String, layout: LayoutType? = null, difficulty: Difficulty? = null, seed: Long = 0L) = buildString {
        append("game?mode=").append(mode)
        layout?.let { append("&layout=").append(it.name) }
        difficulty?.let { append("&difficulty=").append(it.name) }
        if (seed > 0) append("&seed=").append(seed)
    }
}

private fun NavHostController.startGame(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME)
        launchSingleTop = true
    }
}

@Composable
fun AppNavigation(container: AppContainer, settings: AppSettings) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                settings = settings,
                onContinue = { nav.startGame(Routes.game(GameViewModel.MODE_CONTINUE)) },
                onPlay = { nav.startGame(Routes.game(GameViewModel.MODE_NEW, LayoutType.TURTLE, settings.defaultDifficulty)) },
                onDaily = { nav.startGame(Routes.game(GameViewModel.MODE_DAILY)) },
                onLevelSelect = { nav.navigate(Routes.SELECT) },
                onStats = { nav.navigate(Routes.STATS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SELECT) {
            LevelSelectScreen(
                container = container,
                settings = settings,
                onBack = { nav.popBackStack() },
                onStart = { layout, difficulty, seed -> nav.startGame(Routes.game(GameViewModel.MODE_NEW, layout, difficulty, seed)) },
            )
        }
        composable(Routes.STATS) { StatsScreen(container = container, onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(container = container, settings = settings, onBack = { nav.popBackStack() }) }
        composable(
            Routes.GAME,
            arguments = listOf(
                navArgument(GameViewModel.ARG_MODE) { type = NavType.StringType; defaultValue = GameViewModel.MODE_CONTINUE },
                navArgument(GameViewModel.ARG_LAYOUT) { type = NavType.StringType; defaultValue = "" },
                navArgument(GameViewModel.ARG_DIFFICULTY) { type = NavType.StringType; defaultValue = "" },
                navArgument(GameViewModel.ARG_SEED) { type = NavType.LongType; defaultValue = 0L },
            ),
        ) {
            GameScreen(container = container, settings = settings, onExit = { nav.popBackStack(Routes.HOME, inclusive = false) })
        }
    }
}
