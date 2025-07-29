package android.template.feature.trip_history.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [TripHistoryScreen].
 */
@RunWith(AndroidJUnit4::class)
class TripHistoryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {

    }

    @Test
    fun errorState_showsErrorMessage() {
        val errorMessage = "Connection error"

        composeTestRule.setContent {
            ErrorState(errorMessage = errorMessage)
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            LoadingState()
        }

        composeTestRule.onNodeWithTag("Loading").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        composeTestRule.setContent {
            EmptyState()
        }

        composeTestRule.onNodeWithTag("EmptyState").assertIsDisplayed()
    }

    @Test
    fun tripContent_displaysAllTrips() {
        val trips = listOf(
            TripUiModel(id = 1, startTimeAndDate = "01. 01. 2021. 11:00", duration = "01:00", distance = "100 m"),
            TripUiModel(id = 2, startTimeAndDate = "02. 02. 2022. 12:00", duration = "02:00", distance = "200 m"),
            TripUiModel(id = 3, startTimeAndDate = "03. 03. 2023. 13:00", duration = "03:00", distance = "300 m"),
            TripUiModel(id = 4, startTimeAndDate = "04. 04. 2024. 14:00", duration = "04:00", distance = "400 m"),
        )

        composeTestRule.setContent {
            TripContent(
                trips = trips
            )
        }

        trips.forEach { trip ->
            composeTestRule.onNodeWithText(trip.startTimeAndDate).assertIsDisplayed()
        }
    }

    @Test
    fun tripContent_clickTrip_invokesCallback() {
        var clickedTrip: TripUiModel? = null
        val trips = listOf(
            TripUiModel(id = 1, startTimeAndDate = "01. 01. 2021. 11:00", duration = "01:00", distance = "100 m"),
            TripUiModel(id = 2, startTimeAndDate = "02. 02. 2022. 12:00", duration = "02:00", distance = "200 m"),
            TripUiModel(id = 3, startTimeAndDate = "03. 03. 2023. 13:00", duration = "03:00", distance = "300 m"),
            TripUiModel(id = 4, startTimeAndDate = "04. 04. 2024. 14:00", duration = "04:00", distance = "400 m"),
        )

        composeTestRule.setContent {
            TripContent(
                trips = trips,
                onTripClick = { trip ->
                    clickedTrip = trip
                }
            )
        }

        // Click on the second trip
        composeTestRule.onNodeWithText(trips[1].startTimeAndDate).performClick()

        // Verify the callback was invoked with the correct trip
        assert(clickedTrip?.id == trips[1].id)
        assert(clickedTrip?.startTimeAndDate == trips[1].startTimeAndDate)
        assert(clickedTrip?.duration == trips[1].duration)
        assert(clickedTrip?.distance == trips[1].distance)
    }

    @Test
    fun tripContent_isDisplayed() {
        // Using resource ID is better, but for the simplicity of the test
        val expectedTitle = "Trip History"

        composeTestRule.setContent {
            TopBar()
        }

        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
    }
}
