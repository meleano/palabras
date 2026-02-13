package com.example.palabras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.palabras.ui.screens.GameScreen
import com.example.palabras.ui.screens.HomeScreen
import com.example.palabras.ui.screens.StatsScreen
import com.example.palabras.ui.theme.PalabrasTheme
import com.example.palabras.ui.viewmodel.GameViewModel
import com.example.palabras.ui.viewmodel.GameViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PalabrasApplication
        val gameViewModel: GameViewModel by viewModels {
            GameViewModelFactory(app.dictionaryRepository, app.userStatsRepository)
        }

        setContent {
            PalabrasTheme {
                PalabrasApp(gameViewModel, app)
            }
        }
    }
}

@Composable
fun PalabrasApp(gameViewModel: GameViewModel, app: PalabrasApplication) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNewGame = {
                    gameViewModel.startNewGame()
                    navController.navigate("game")
                },
                onContinueGame = {
                    // continuar la partida actual (no reset)
                    navController.navigate("game")
                },
                onViewStats = { navController.navigate("stats") }
            )
        }
        composable("game") {
            GameScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("stats") {
            StatsScreen(
                repository = app.userStatsRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
