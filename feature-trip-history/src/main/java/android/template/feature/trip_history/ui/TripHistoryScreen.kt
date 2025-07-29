package android.template.feature.trip_history.ui

import android.app.Activity
import android.content.Intent
import android.template.core.ui.MyApplicationTheme
import android.template.feature.trip_history.R
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Displays the list of trips the user has recorded.
 */
@Composable
fun TripHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TripHistoryViewModel = hiltViewModel()
) {
    // This LaunchedEffect is used to show Intent chooser when the user clicks on a trip to export it.
    val context = LocalActivity.current!!
    LaunchedEffect(Unit) {
        viewModel.csvExport.collect { csvExportModel ->
            if (csvExportModel != null) {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TEXT, csvExportModel.waypointsCsv)
                    putExtra(Intent.EXTRA_SUBJECT, csvExportModel.tripName)
                }
                val shareIntent = Intent.createChooser(
                    sendIntent,
                    ContextCompat.getString(context, R.string.export_trip_data_intent_chooser_title)
                )
                context.startActivity(shareIntent)
            }
        }
    }

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
                TripContent(
                    modifier = modifier,
                    trips = trips,
                    onTripClick = { trip ->
                        viewModel.tripClicked(trip)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun TripContent(
    modifier: Modifier = Modifier,
    trips: List<TripUiModel>,
    onTripClick: (TripUiModel) -> Unit = {}
) {
    val snackBarHostState = remember { SnackbarHostState() }

    val windowSizeClass = calculateWindowSizeClass(LocalActivity.current as Activity)
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    Scaffold(
        topBar = { TopBar() },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->
        if (isExpanded) {
            // If wa are on a wide screen(tablet or phone in landscape mode) show trips in a grid.
            TripsGrid(
                modifier = modifier,
                paddingValues = paddingValues,
                snackBarHostState = snackBarHostState,
                trips = trips,
                onTripClick = onTripClick
            )
        } else {
            // If we are on a phone, show trips in a list.
            TripsList(
                modifier = modifier,
                paddingValues = paddingValues,
                snackBarHostState = snackBarHostState,
                trips = trips,
                onTripClick = onTripClick
            )
        }
    }
}

@Composable
internal fun TripsList(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    snackBarHostState: SnackbarHostState,
    trips: List<TripUiModel>,
    onTripClick: (TripUiModel) -> Unit = {}
) {
    val context = LocalActivity.current!!
    val scope = rememberCoroutineScope()

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
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = ContextCompat.getString(context, R.string.exporting_trip_data_snackbar_message),
                                actionLabel = ContextCompat.getString(context, R.string.dismiss)
                            )
                        }
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

@Composable
internal fun TripsGrid(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    snackBarHostState: SnackbarHostState,
    trips: List<TripUiModel>,
    onTripClick: (TripUiModel) -> Unit = {}
) {
    val context = LocalActivity.current!!
    val scope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(trips.size) { index ->
            TripCard(
                modifier,
                trips[index],
                onTripClick = {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = ContextCompat.getString(context, R.string.exporting_trip_data_snackbar_message),
                            actionLabel = ContextCompat.getString(context, R.string.dismiss)
                        )
                    }
                    onTripClick(trips[index])
                }
            )
        }
    }
}

@Composable
internal fun TripCard(
    modifier: Modifier = Modifier,
    trip: TripUiModel,
    onTripClick: (TripUiModel) -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable {
                onTripClick.invoke(trip)
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = trip.startTimeAndDate,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = trip.distance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = trip.duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
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
private fun TripsContentPreview() {
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
        TripContent(trips = trips)
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
