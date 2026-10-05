package com.karthik.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen(
                onPlayClick = { navController.navigate("game/0") },
                onCustomizeClick = { navController.navigate("customize") },
                onLeaderboardClick = { navController.navigate("leaderboard") }
            )
        }
        composable("customize") {
            CustomizeScreen(onBack = { navController.popBackStack() })
        }
        composable("leaderboard") {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable("game/{level}") {
            GameScreen(
                onBackToMenu = {
                    navController.popBackStack("home", inclusive = false)
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard")
                }
            )
        }
    }
}
