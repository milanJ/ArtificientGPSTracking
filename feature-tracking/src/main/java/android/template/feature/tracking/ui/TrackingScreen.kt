package android.template.feature.tracking.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.template.core.ui.MyApplicationTheme
import android.template.feature.tracking.LocationService
import android.template.feature.tracking.R
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Main entry point to the Tracking screen.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current!!
    val permissionsState = rememberMultiplePermissionsState(
        permissions = getAllRequiredPermissionsList()
    )

    // If the user hasn't granted all the required permissions yet, we show the permissions request screen. Otherwise we show the GoogleMaps tracking screen.
    if (permissionsState.allPermissionsGranted) {
        // Start the LocationService, we use it to get user location and store it into a DB. It must run whenever this screen is visible, regardless if tracking is on or not.
        // It will record trips if trip recording is turned on.
        LaunchedEffect(Unit) {
            activity.startService(Intent(activity, LocationService::class.java))
        }

        // We use this DisposableEffect to shut down the LocationService when user leaves this screen, if background location tracking is disabled.
        val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    Log.d(TAG, "Shutting down LocationService because the user left the TrackingScreen.")
                    val intent = Intent(activity, LocationService::class.java)
                        .apply {
                            action = LocationService.ACTION_STOP_SERVICE_IF_NOT_IN_FOREGROUND_MODE
                        }
                    activity.startService(intent)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Show the TrackingScreen UI.
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        TrackingScreen(
            modifier,
            uiState,
            onStartRecordingClick = {
                val intent = Intent(activity, LocationService::class.java)
                    .apply {
                        action = LocationService.ACTION_START_LOCATION_TRACKING
                    }
                activity.startService(intent)
                viewModel.startTracking()
            },
            onResumeRecordingClick = {
                val intent = Intent(activity, LocationService::class.java)
                    .apply {
                        action = LocationService.ACTION_RESUME_LOCATION_TRACKING
                    }
                activity.startService(intent)
                viewModel.resumeTracking()
            },
            onPauseRecordingClick = {
                val intent = Intent(activity, LocationService::class.java)
                    .apply {
                        action = LocationService.ACTION_PAUSE_LOCATION_TRACKING
                    }
                activity.startService(intent)
                viewModel.pauseTracking()
            },
            onStopRecordingClick = {
                val intent = Intent(activity, LocationService::class.java)
                    .apply {
                        action = LocationService.ACTION_STOP_LOCATION_TRACKING
                    }
                activity.startService(intent)
                viewModel.stopTracking()
            }
        )
    } else {
        PermissionsScreen(
            modifier = modifier,
            onProceedClick = {
                permissionsState.launchMultiplePermissionRequest()
            }
        )
    }
}

/**
 * Before we can show the tracking screen, we need to request the necessary permissions from the user.
 * This composable renders the UI needed to do this.
 */
@Composable
internal fun PermissionsScreen(
    modifier: Modifier = Modifier,
    onProceedClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.permissions_request_explanation),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onProceedClick
            ) {
                Text(
                    stringResource(R.string.proceed_with_permissions_request_button_caption),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * The actual Tracking screen that is shown to the user after he/she approves the permissions.
 */
@Composable
internal fun TrackingScreen(
    modifier: Modifier = Modifier,
    uiState: TrackingUIState,
    onStartRecordingClick: () -> Unit,
    onResumeRecordingClick: () -> Unit,
    onPauseRecordingClick: () -> Unit,
    onStopRecordingClick: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    cameraPositionState.position = if (uiState.hasLocation) {
        CameraPosition.fromLatLngZoom(LatLng(uiState.latitude!!, uiState.longitude!!), cameraPositionState.position.zoom)
    } else {
        CameraPosition.fromLatLngZoom(LatLng(50.42678, 19.41073), DEFAULT_GOOGLE_MAPS_ZOOM)
    }

    val userLocation = if (uiState.hasLocation) {
        LatLng(uiState.latitude!!, uiState.longitude!!)
    } else {
        LatLng(0.0, 0.0)
    }
    var userLocationMarkerPosition by remember { mutableStateOf(userLocation) }
    userLocationMarkerPosition = userLocation
    var userLocationMarkerVisible by remember { mutableStateOf(uiState.hasLocation) }
    userLocationMarkerVisible = uiState.hasLocation

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false
            )
        ) {
            Marker(
                state = MarkerState(position = userLocationMarkerPosition),
                icon = BitmapDescriptorFactory.fromResource(R.drawable.user_location_marker),
                anchor = Offset(0.5f, 0.5f),
                title = stringResource(R.string.user_position_marker_title),
                visible = userLocationMarkerVisible
            )
        }

        if (uiState.isExtraInfoVisible) {
            TripInfoCard(
                modifier = Modifier.align(Alignment.TopStart),
                speed = uiState.speed,
                distanceTraveled = uiState.distanceTraveled,
                elapsedTime = uiState.elapsedTime
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (uiState.isStartTrackingVisible) {
                FloatingActionButton(
                    onClick = onStartRecordingClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.start_recording),
                        contentDescription = stringResource(R.string.start_recording_button_caption)
                    )
                }
            }

            if (uiState.isResumeTrackingVisible) {
                FloatingActionButton(
                    onClick = onResumeRecordingClick,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.resume_recording),
                        contentDescription = stringResource(R.string.resume_recording_button_caption)
                    )
                }
            }

            if (uiState.isPauseTrackingVisible) {
                FloatingActionButton(
                    onClick = onPauseRecordingClick,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pause_recording),
                        contentDescription = stringResource(R.string.pause_recording_button_caption)
                    )
                }
            }

            if (uiState.isStopTrackingVisible) {
                FloatingActionButton(
                    onClick = onStopRecordingClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stop_recording),
                        contentDescription = stringResource(R.string.stop_recording_button_caption)
                    )
                }
            }
        }
    }
}

/**
 * A card that shows to the user the info about the current trip, such as speed, distance traveled and elapsed time.
 */
@Composable
internal fun TripInfoCard(
    modifier: Modifier = Modifier,
    speed: String,
    distanceTraveled: String,
    elapsedTime: String
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .padding(start = 16.dp, top = 32.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5F), shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.current_speed_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = speed,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.distance_traveled_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = distanceTraveled,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.elapsed_time_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = elapsedTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Previews:

@Preview(showBackground = true)
@Composable
private fun TrackingScreenPreview() {
    MyApplicationTheme {
        TrackingScreen(
            modifier = Modifier,
            uiState = TrackingUIState.INITIAL.copy(
                isStartTrackingVisible = true,
                isPauseTrackingVisible = true,
                isResumeTrackingVisible = true,
                isStopTrackingVisible = true,
                isExtraInfoVisible = true,
                speed = "0 km/h",
                distanceTraveled = "2.5 km",
                elapsedTime = "35 minutes"
            ),
            onStartRecordingClick = {},
            onResumeRecordingClick = {},
            onPauseRecordingClick = {},
            onStopRecordingClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionsScreenPreview() {
    MyApplicationTheme {
        PermissionsScreen(
            modifier = Modifier,
            onProceedClick = {}
        )
    }
}

// Utility functions and constants:

private fun getAllRequiredPermissionsList(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACTIVITY_RECOGNITION
    )
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACTIVITY_RECOGNITION
    )
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.ACTIVITY_RECOGNITION
    )
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE
    )
} else {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}

private const val DEFAULT_GOOGLE_MAPS_ZOOM = 4.5F
private const val TAG = "TrackingScreen"
