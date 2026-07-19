package com.proflix.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object History : Screen("history")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object Detail : Screen("detail/{contentId}") {
        fun createRoute(contentId: String) = "detail/${java.net.URLEncoder.encode(contentId, "UTF-8")}"
    }
    data object Player : Screen("player?videoUrl={videoUrl}&title={title}")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, Screen.Home),
    BottomNavItem("Search", Icons.Default.Search, Screen.Search),
    BottomNavItem("History", Icons.Default.History, Screen.History),
    BottomNavItem("Favorites", Icons.Default.Favorite, Screen.Favorites),
    BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings)
)
