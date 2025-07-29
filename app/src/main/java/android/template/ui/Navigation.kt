package android.template.ui

import android.app.Activity
import android.template.R
import android.template.feature.tracking.ui.TrackingScreen
import android.template.feature.settings.ui.SettingsScreen
import android.template.feature.trip_history.ui.TripHistoryScreen
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val windowSizeClass = calculateWindowSizeClass(LocalActivity.current as Activity)
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route ?: Screen.TRACKING.route

    if (isExpanded) {
        // If wa are on a wide screen(tablet or phone in landscape mode), use NavigationRail.
        Row {
            NavigationRail {
                Screen.entries.forEach { screen ->
                    NavigationRailItem(
                        selected = currentDestination == screen.route,
                        onClick = {
                            if (currentDestination != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = stringResource(screen.titleResId)
                            )
                        },
                        label = {
                            Text(stringResource(screen.titleResId))
                        }
                    )
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    MainNavHost(navController = navController)
                }
            }
        }
    } else {
        // If we are on a phone, use NavigationBar.
        Row {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    MainNavHost(navController = navController)
                }
                NavigationBar {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentDestination == screen.route,
                            onClick = {
                                if (currentDestination != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = stringResource(screen.titleResId)
                                )
                            },
                            label = {
                                Text(stringResource(screen.titleResId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TRACKING.route
    ) {
        composable(Screen.TRACKING.route) {
            TrackingScreen(
                modifier = Modifier.padding(0.dp)
            )
        }
        composable(Screen.TRIP_HISTORY.route) {
            TripHistoryScreen(
                modifier = Modifier.padding(0.dp)
            )
        }
        composable(Screen.SETTINGS.route) {
            SettingsScreen(
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}

enum class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    TRACKING("tracking", R.string.screen_name_tracking, Icons.Default.LocationOn),
    TRIP_HISTORY("tripHistory", R.string.screen_name_trip_history, Icons.Default.Menu),
    SETTINGS("settings", R.string.screen_name_settings, Icons.Default.Settings)
}
