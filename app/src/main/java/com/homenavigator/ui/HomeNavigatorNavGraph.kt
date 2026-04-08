package com.homenavigator.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.homenavigator.ui.screens.MapScreen
import com.homenavigator.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Map      : Screen("map")
    data object Settings : Screen("settings")
}

@Composable
fun HomeNavigatorNavGraph() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Screen.Map.route) {
        composable(Screen.Map.route) {
            MapScreen(onNavigateToSettings = { nav.navigate(Screen.Settings.route) })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}