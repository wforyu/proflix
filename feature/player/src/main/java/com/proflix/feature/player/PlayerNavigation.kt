package com.proflix.feature.player

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val PLAYER_ROUTE = "player?episodeId={episodeId}&title={title}&contentId={contentId}"

fun NavController.navigateToPlayer(episodeId: String, title: String, contentId: String = "") {
    val encodedId = android.net.Uri.encode(episodeId, null)
    val encodedTitle = android.net.Uri.encode(title, null)
    val encodedContentId = android.net.Uri.encode(contentId, null)
    navigate("player?episodeId=$encodedId&title=$encodedTitle&contentId=$encodedContentId")
}

fun NavGraphBuilder.playerScreen(
    onBack: () -> Unit,
    onNavigateToEpisode: (String, String, String) -> Unit
) {
    composable(
        route = PLAYER_ROUTE,
        arguments = listOf(
            navArgument("episodeId") { type = NavType.StringType; defaultValue = "" },
            navArgument("title") { type = NavType.StringType; defaultValue = "" },
            navArgument("contentId") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->
        val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
        val title = backStackEntry.arguments?.getString("title") ?: ""
        val contentId = backStackEntry.arguments?.getString("contentId") ?: ""

        StreamPlayerScreen(
            episodeId = episodeId,
            title = title,
            contentId = contentId,
            onBack = onBack,
            onNavigateToEpisode = onNavigateToEpisode
        )
    }
}
