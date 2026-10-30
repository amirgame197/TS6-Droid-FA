package dev.tsdroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.tsdroid.data.TsServerLink

/**
 * [initialAnimeBackground] is what startup read from the settings before it let the app in. The
 * screens take it as the initial value of their own wallpaper setting so the first frame they draw
 * already has the background the splash prepared, instead of an empty one waiting for DataStore.
 * 
 * [serverLink] is a `ts3server://` link the system handed the activity. It is routed home from
 * here and poured into the connect form by the connection screen, which reports back through
 * [onServerLinkConsumed] once it has.
 */
@Composable
fun AppNavigation(
    initialAnimeBackground: Boolean,
    serverLink: TsServerLink? = null,
    onServerLinkConsumed: () -> Unit = { },
) {
    val navController = rememberNavController()

    // Back to the connection screen. It is deliberately kept on the back stack while a session is
    // running — the session lives in TsConnectionService, not in the server screen — so leaving the
    // server screen never has to drop the connection, and the back gesture keeps working.
    val backToHome: () -> Unit = {
        if (!navController.popBackStack(route = "connection", inclusive = false)) {
            navController.navigate("connection")
        }
    }
    
    LaunchedEffect(serverLink) {
        if (serverLink != null && navController.currentDestination?.route != "connection") {
            if (!navController.popBackStack(route = "connection", inclusive = false)) {
                navController.navigate("connection") { launchSingleTop = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "connection") {
        composable("connection") {
            ConnectionScreen(
                onConnected = {
                    navController.navigate("server") { launchSingleTop = true }
                },
                onNavigateToAbout = { navController.navigate("about") },
                initialAnimeBackground = initialAnimeBackground,
                serverLink = serverLink,
                onServerLinkConsumed = onServerLinkConsumed,
            )
        }
        composable("server") {
            ServerScreen(
                onBackToHome = backToHome,
                onNavigateToAbout = { navController.navigate("about") },
                initialAnimeBackground = initialAnimeBackground,
            )
        }
        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
