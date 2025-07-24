package android.template.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.template.feature.mymodel.ui.TrackingScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "tracking"
    ) {
        composable("tracking") {
            TrackingScreen(
                modifier = Modifier.padding(0.dp)
            )
        }
        composable("tripHistory") {
            TrackingScreen(
                modifier = Modifier.padding(0.dp)
            )
        }
        composable("settings") {
            TrackingScreen(
                modifier = Modifier.padding(0.dp)
            )
        }
        // TODO: Add more destinations
    }
}
