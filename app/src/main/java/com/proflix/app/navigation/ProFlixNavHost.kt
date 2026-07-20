package com.proflix.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proflix.feature.detail.DetailScreen
import com.proflix.feature.favorite.FavoriteScreen
import com.proflix.feature.history.HistoryScreen
import com.proflix.feature.home.HomeScreen
import com.proflix.feature.player.navigateToPlayer
import com.proflix.feature.player.playerScreen
import com.proflix.feature.search.SearchScreen
import com.proflix.feature.settings.SettingsScreen
import java.net.URLDecoder

@Composable
fun ProFlixNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.let { route ->
        bottomNavItems.any { it.screen.route == route }
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFF0D0D0D)
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onContentClick = { contentId ->
                        navController.navigate(Screen.Detail.createRoute(contentId))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onContentClick = { contentId ->
                        navController.navigate(Screen.Detail.createRoute(contentId))
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onContentClick = { contentId ->
                        navController.navigate(Screen.Detail.createRoute(contentId))
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoriteScreen(
                    onContentClick = { contentId ->
                        navController.navigate(Screen.Detail.createRoute(contentId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("contentId") { type = NavType.StringType }
                )
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { contentId, episodeId, episodeTitle ->
                        navController.navigateToPlayer(episodeId, episodeTitle, contentId)
                    }
                )
            }

            playerScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEpisode = { episodeId, title, contentId ->
                    navController.navigate(
                        com.proflix.feature.player.PLAYER_ROUTE
                            .replace("{episodeId}", java.net.URLEncoder.encode(episodeId, "UTF-8"))
                            .replace("{title}", java.net.URLEncoder.encode(title, "UTF-8"))
                            .replace("{contentId}", java.net.URLEncoder.encode(contentId, "UTF-8"))
                    ) {
                        popUpTo(com.proflix.feature.player.PLAYER_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}
