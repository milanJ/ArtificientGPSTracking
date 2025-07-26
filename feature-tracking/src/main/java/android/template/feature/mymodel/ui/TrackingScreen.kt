package android.template.feature.mymodel.ui

import android.template.core.ui.MyApplicationTheme
import android.template.feature.mymodel.ui.TrackingUiState.Success
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val singapore = LatLng(1.35, 103.87)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(singapore, 10f)
        }
    ) {
        Marker(
            state = MarkerState(position = singapore),
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }

//    val items by viewModel.uiState.collectAsStateWithLifecycle()
//    if (items is Success) {
//        TrackingScreen(
//            items = (items as Success).data,
//            onSave = { name -> viewModel.addMyModel(name) },
//            modifier = modifier
//        )
//    }
}

@Composable
internal fun TrackingScreen(
    items: List<String>,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        var nameMyModel by remember { mutableStateOf("Compose") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = nameMyModel,
                onValueChange = { nameMyModel = it }
            )

            Button(modifier = Modifier.width(96.dp), onClick = { onSave(nameMyModel) }) {
                Text("Save")
            }
        }
        items.forEach {
            Text("Saved item: $it")
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    MyApplicationTheme {
        TrackingScreen(listOf("Compose", "Room", "Kotlin"), onSave = {})
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun PortraitPreview() {
    MyApplicationTheme {
        TrackingScreen(listOf("Compose", "Room", "Kotlin"), onSave = {})
    }
}
