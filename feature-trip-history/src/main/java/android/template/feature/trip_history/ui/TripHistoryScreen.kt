package android.template.feature.trip_history.ui

import android.template.core.ui.MyApplicationTheme
import android.template.feature.trip_history.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Displays the list of trips the user has recorded.
 */
@Composable
fun TripHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TripHistoryViewModel = hiltViewModel()
) {
    val trips by viewModel.uiState.collectAsStateWithLifecycle()
    when (trips) {
        is TripHistoryUiState.Loading -> {
            LoadingState()
        }

        is TripHistoryUiState.Error -> {
            ErrorState(
                errorMessage = stringResource(R.string.loading_trips_error_message)
            )
        }

        is TripHistoryUiState.Success -> {
            val trips = (trips as TripHistoryUiState.Success).data
            if (trips.isEmpty()) {
                EmptyState()
            } else {
                TripsList(
                    modifier = modifier,
                    trips = trips,
                    onTripClick = { trip ->
                        // TODO: XXXX Handle on trip click
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripsList(
    modifier: Modifier = Modifier,
    trips: List<TripUiModel>,
    onTripClick: (TripUiModel) -> Unit = {}
) {
    Scaffold(
        topBar = { TopBar() },
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            items(trips.size) { index ->
                val item = trips[index]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTripClick.invoke(item)
                        }
                        .background(Color.Transparent)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.startTimeAndDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = item.distance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = item.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
internal fun EmptyState() {
    Scaffold(
        topBar = { TopBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.clearAndSetSemantics { /* Not relevant for accessibility. */ },
                imageVector = Icons.Outlined.Info,
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.no_trips_recorded_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun LoadingState() {
    Scaffold(
        topBar = { TopBar() }
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag("Loading")
            )
        }
    }
}

@Composable
internal fun ErrorState(
    errorMessage: String
) {
    Scaffold(
        topBar = { TopBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar() {
    TopAppBar(
        title = {
            Text(stringResource(R.string.screen_title_trip_history))
        }
    )
}

// Previews:

@Preview(showBackground = true)
@Composable
private fun TripsListPreview() {
    val trips = listOf(
        TripUiModel(
            id = 1,
            startTimeAndDate = "January 12, 2025 at 3:30:32pm",
            duration = "1 minute",
            distance = "500 m"
        ),
        TripUiModel(
            id = 2,
            startTimeAndDate = "February 01, 2025 at 7:30:32pm",
            duration = "15 minutes",
            distance = "7 km"
        ),
        TripUiModel(
            id = 3,
            startTimeAndDate = "March 24, 2025 at 1:30:32pm",
            duration = "25 minutes",
            distance = "15.2 km"
        ),
        TripUiModel(
            id = 4,
            startTimeAndDate = "July 5, 2025 at 4:30:32pm",
            duration = "35 minutes",
            distance = "50.3 km"
        ),
    )

    MyApplicationTheme {
        TripsList(trips = trips)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MyApplicationTheme {
        EmptyState()
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    MyApplicationTheme {
        ErrorState(
            errorMessage = stringResource(R.string.loading_trips_error_message)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    MyApplicationTheme {
        LoadingState()
    }
}
