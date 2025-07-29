package android.template.feature.settings.ui

import android.content.res.Configuration
import android.template.core.ui.MyApplicationTheme
import android.template.feature.settings.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val locationInterval by viewModel.locationInterval.collectAsStateWithLifecycle()
    val isBackgroundTrackingEnabled by viewModel.isBackgroundTrackingEnabled.collectAsStateWithLifecycle()

    SettingsScreen(
        modifier = modifier,
        locationInterval = locationInterval,
        isBackgroundTrackingEnabled = isBackgroundTrackingEnabled,
        onIntervalChange = viewModel::updateInterval,
        onBackgroundTrackingToggle = viewModel::toggleBackgroundTracking
    )
}

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    locationInterval: Int,
    isBackgroundTrackingEnabled: Boolean,
    onIntervalChange: (Int) -> Unit,
    onBackgroundTrackingToggle: (Boolean) -> Unit
) {
    val intervalOptions = listOf(1, 5, 10, 15, 30, 60) // seconds
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar() }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // Location Update Interval:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.location_update_interval_caption),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    OutlinedButton(
                        onClick = {
                            isDropdownExpanded = true
                        }) {
                        Text(stringResource(R.string.location_update_interval_value, locationInterval))
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = {
                            isDropdownExpanded = false
                        }
                    ) {
                        intervalOptions.forEach { interval ->
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.location_update_interval_value, interval))
                                },
                                onClick = {
                                    onIntervalChange(interval)
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Background Tracking:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.background_tracking_caption),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isBackgroundTrackingEnabled,
                    onCheckedChange = {
                        onBackgroundTrackingToggle(it)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar() {
    TopAppBar(
        title = {
            Text(stringResource(R.string.screen_title_settings))
        }
    )
}

// Previews

@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun LightPreview() {
    MyApplicationTheme {
        SettingsScreen(
            Modifier,
            1,
            false,
            onIntervalChange = {},
            onBackgroundTrackingToggle = {}
        )
    }
}

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkPreview() {
    MyApplicationTheme {
        SettingsScreen(
            Modifier,
            1,
            false,
            onIntervalChange = {},
            onBackgroundTrackingToggle = {}
        )
    }
}
