package com.lollomarta.streamhub.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.ui.components.MessageState
import com.lollomarta.streamhub.ui.components.TopDest
import com.lollomarta.streamhub.ui.details.DetailsScreen
import com.lollomarta.streamhub.ui.favorites.FavoritesScreen
import com.lollomarta.streamhub.ui.home.HomeScreen
import com.lollomarta.streamhub.ui.search.SearchScreen
import com.lollomarta.streamhub.ui.services.ServicesScreen
import com.lollomarta.streamhub.ui.settings.ApiSetupScreen
import com.lollomarta.streamhub.ui.settings.SettingsScreen
import com.lollomarta.streamhub.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val API_SETUP = "api_setup"
    const val DETAILS = "details/{type}/{id}"
    fun details(item: MediaItem) = "details/${item.type.apiPath}/${item.id}"
}

@Composable
fun StreamHubNavHost(nav: NavHostController = rememberNavController()) {
    val navigateTop: (TopDest) -> Unit = { dest ->
        if (nav.currentDestination?.route != dest.route) {
            nav.navigate(dest.route) {
                popUpTo(TopDest.HOME.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val openDetails: (MediaItem) -> Unit = { nav.navigate(Routes.details(it)) }
    val openApiSetup: () -> Unit = { nav.navigate(Routes.API_SETUP) { launchSingleTop = true } }

    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            // Tasto Menu (telecomandi Fire TV e alcuni Android TV): scorciatoia per la ricerca.
            if (event.type == KeyEventType.KeyDown && event.key == Key.Menu && nav.currentDestination?.route != Routes.SPLASH) {
                navigateTop(TopDest.SEARCH); true
            } else false
        },
    ) {
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onDone = {
                    nav.navigate(TopDest.HOME.route) { popUpTo(Routes.SPLASH) { inclusive = true } }
                })
            }
            composable(TopDest.HOME.route) { HomeScreen(openDetails, navigateTop, openApiSetup) }
            composable(TopDest.SEARCH.route) { SearchScreen(openDetails, navigateTop, openApiSetup) }
            composable(TopDest.FAVORITES.route) { FavoritesScreen(openDetails, navigateTop) }
            composable(TopDest.SERVICES.route) { ServicesScreen(navigateTop) }
            composable(TopDest.SETTINGS.route) { SettingsScreen(navigateTop, openApiSetup) }
            composable(Routes.API_SETUP) { ApiSetupScreen(onDone = { nav.popBackStack() }) }
            composable(
                Routes.DETAILS,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.IntType },
                ),
            ) { entry ->
                val type = MediaType.fromApi(entry.arguments?.getString("type"))
                val id = entry.arguments?.getInt("id") ?: 0
                if (type == null || id <= 0) {
                    MessageState(
                        title = "Contenuto non disponibile",
                        message = "Non è stato possibile aprire questo contenuto.",
                        image = R.drawable.empty_search,
                        primaryLabel = "Torna indietro",
                        onPrimary = { nav.popBackStack() },
                    )
                } else {
                    DetailsScreen(type, id, onConfigureApi = openApiSetup)
                }
            }
        }
    }
}
