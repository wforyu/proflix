package com.proflix.feature.player

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val PLAYER_ROUTE = "player?episodeId={episodeId}&title={title}&contentId={contentId}"

fun NavController.navigateToPlayer(episodeId: String, title: String, contentId: String = "") {
    val encodedId = java.net.URLEncoder.encode(episodeId, "UTF-8")
    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
    val encodedContentId = java.net.URLEncoder.encode(contentId, "UTF-8")
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
        val episodeId = java.net.URLDecoder.decode(
            backStackEntry.arguments?.getString("episodeId") ?: "", "UTF-8"
        )
        val title = java.net.URLDecoder.decode(
            backStackEntry.arguments?.getString("title") ?: "", "UTF-8"
        )
        val contentId = java.net.URLDecoder.decode(
            backStackEntry.arguments?.getString("contentId") ?: "", "UTF-8"
        )

        StreamPlayerScreen(
            episodeId = episodeId,
            title = title,
            contentId = contentId,
            onBack = onBack,
            onNavigateToEpisode = onNavigateToEpisode
        )
    }
}
