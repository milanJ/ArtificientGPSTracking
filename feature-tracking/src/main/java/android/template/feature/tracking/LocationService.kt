package android.template.feature.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.template.core.data.SettingsRepository
import android.template.core.data.TripModel
import android.template.core.data.TripsRepository
import android.template.core.data.WaypointModel
import android.template.feature.tracking.data.LocationModel
import android.template.feature.tracking.data.LocationRepository
import android.template.feature.tracking.data.TrackingState
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class LocationService() : LifecycleService() {

    // Injected dependencies:
    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var tripsRepository: TripsRepository

    // Location related:
    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(
            result: LocationResult
        ) {
            lifecycleScope.launch {
                locationUpdatesFlow.emit(result.lastLocation)
            }
        }
    }
    private val locationUpdatesFlow = MutableSharedFlow<Location?>()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationTrackingStartTime: Long = 0L
    private var isTrackingLocation: Boolean = false
    private var tripDistanceTraveledTotal: Int = 0
    private var previousLocation: Location? = null
    private var isTrackingPaused: Boolean = false
    private var currentTrip: TripModel? = null

    // Settings related:
    private var isBackgroundTracking: Boolean = SettingsRepository.DEFAULT_BACKGROUND_TRACKING
    private var locationInterval: Int = SettingsRepository.DEFAULT_LOCATION_INTERVAL

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                isBackgroundTracking = settingsRepository.getBackgroundTracking()
                locationInterval = settingsRepository.getLocationInterval()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.locationInterval.collect { value ->
                    locationInterval = value
                    restartLocationTracking()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.backgroundTracking.collect { value ->
                    isBackgroundTracking = value
                    restartLocationTracking()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationUpdatesFlow.collect { value ->
                    processLocationUpdate(value)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        restartLocationTracking()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand() :: Command = ${intent?.action}")
        when (intent?.action) {
            ACTION_START_LOCATION_TRACKING -> {
                currentTrip = null
                previousLocation = null
                locationTrackingStartTime = SystemClock.elapsedRealtime()
                isTrackingPaused = false
                isTrackingLocation = true
                restartLocationTracking()
            }

            ACTION_RESUME_LOCATION_TRACKING -> {
                isTrackingPaused = false
            }

            ACTION_PAUSE_LOCATION_TRACKING -> {
                isTrackingPaused = true
            }

            ACTION_STOP_LOCATION_TRACKING -> {
                isTrackingPaused = false
                isTrackingLocation = false
                restartLocationTracking()
            }

            ACTION_STOP_SERVICE_IF_NOT_IN_FOREGROUND_MODE -> {
                if (!isBackgroundTracking) {
                    Log.d(TAG, "onStartCommand() :: Stopping service due to '${ACTION_STOP_SERVICE_IF_NOT_IN_FOREGROUND_MODE}' action because the service is not in foreground mode")
                    lifecycleScope.launch {
                        locationRepository.setTrackingState(TrackingState.STOPPED)
                    }
                    fusedLocationClient?.removeLocationUpdates(locationCallback)
                    currentTrip = null
                    previousLocation = null
                    locationTrackingStartTime = 0L
                    isTrackingPaused = false
                    isTrackingLocation = false
                    stopSelf()
                } else {
                    Log.d(TAG, "onStartCommand() :: Ignoring '${ACTION_STOP_SERVICE_IF_NOT_IN_FOREGROUND_MODE}' action because the service is in the foreground mode")
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isTrackingPaused = false
        isTrackingLocation = false
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        currentTrip = null
        previousLocation = null
        locationTrackingStartTime = 0L
        super.onDestroy()
    }

    private suspend fun processLocationUpdate(
        location: Location?
    ) {
        Log.d(TAG, "onLocationResult() :: location = $location")

        // If we are not tracking the user location just update the latitude and longitude of the user in the repository.
        if (!isTrackingLocation) {
            if (location == null) {
                locationRepository.setUserLocation(null)
                return
            }

            val locationModel = LocationModel(
                latitude = location.latitude,
                longitude = location.longitude,
                speed = 0F,
                0,
                0L
            )
            locationRepository.setUserLocation(locationModel)
            return
        }

        // If we are tracking the user location and the reported location is null, just update the repository.
        if (location == null) {
            locationRepository.setUserLocation(null)
            return
        }

        // If we are tracking the user location, and previous location was null, it means we have just started and this is our first captured location.
        // Calculate and update all needed info. Create a new trip, and save the captured location in the DB.
        if (previousLocation == null) {
            val locationModel = LocationModel(
                latitude = location.latitude,
                longitude = location.longitude,
                speed = location.speed,
                0,
                SystemClock.elapsedRealtime() - locationTrackingStartTime
            )
            locationRepository.setUserLocation(locationModel)

            if (isTrackingPaused) {
                // Special case. If the user pauses trip tracking right after starting it, before we manage the capture the first location.
                // Just update the user location, don't create the trip or anything until the user resumes the recording.
                return
            }

            previousLocation = location
            tripDistanceTraveledTotal = 0

            currentTrip = tripsRepository.createTrip(locationTrackingStartTime)

            val waypoint = WaypointModel(
                timestamp = System.currentTimeMillis(),
                speed = location.speed,
                latitude = location.latitude,
                longitude = location.longitude,
                tripId = currentTrip!!.id
            )
            tripsRepository.addWaypoint(waypoint)
            return
        }

        // If we are tracking the user location, and previous location was not null (the most common case), calculate and update all needed info.
        // Update the trip. And save the captured location in the DB.

        if (isTrackingPaused) {
            // Special case. If the user pauses trip tracking, we don't want to update the trip and waypoints, just the location info in the repository.
            val tripDurationAtThisMoment = SystemClock.elapsedRealtime() - locationTrackingStartTime
            val locationModel = LocationModel(
                latitude = location.latitude,
                longitude = location.longitude,
                speed = location.speed,
                tripDistanceTraveledTotal,
                tripDurationAtThisMoment
            )
            locationRepository.setUserLocation(locationModel)
            return
        }

        val distance = previousLocation!!.distanceTo(location)
        tripDistanceTraveledTotal += distance.roundToInt()

        previousLocation = location

        val tripDurationAtThisMoment = SystemClock.elapsedRealtime() - locationTrackingStartTime
        val locationModel = LocationModel(
            latitude = location.latitude,
            longitude = location.longitude,
            speed = location.speed,
            tripDistanceTraveledTotal,
            tripDurationAtThisMoment
        )
        locationRepository.setUserLocation(locationModel)

        currentTrip = currentTrip!!.copy(
            duration = tripDurationAtThisMoment,
            distance = tripDistanceTraveledTotal
        )
        tripsRepository.updateTrip(currentTrip!!)

        val waypoint = WaypointModel(
            timestamp = System.currentTimeMillis(),
            speed = location.speed,
            latitude = location.latitude,
            longitude = location.longitude,
            tripId = currentTrip!!.id
        )
        tripsRepository.addWaypoint(waypoint)
    }

    @SuppressLint("MissingPermission")
    private fun restartLocationTracking() {
        if (!areLocationPermissionsGranted()) {
            return
        }

        val fusedLocationClientLocal = fusedLocationClient ?: return

        fusedLocationClientLocal.removeLocationUpdates(locationCallback)

        val locationRequest = LocationRequest.Builder(locationInterval * 1000L)
            .apply {
                if (isTrackingLocation) {
                    setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                }
            }
            .build()

        fusedLocationClientLocal.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        if (isTrackingLocation && isBackgroundTracking) {
            Log.d(TAG, "restartLocationTracking() :: Moving the service to foreground.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } else {
            Log.d(TAG, "restartLocationTracking() :: Moving the service OUT of foreground.")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                ContextCompat.getString(this, R.string.location_service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(ContextCompat.getString(this, R.string.location_service_notification_content_title))
            .setContentText(ContextCompat.getString(this, R.string.location_service_notification_content_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun areLocationPermissionsGranted(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACTION_START_LOCATION_TRACKING = "start_location_tracking"
        const val ACTION_RESUME_LOCATION_TRACKING = "resume_location_tracking"
        const val ACTION_PAUSE_LOCATION_TRACKING = "pause_location_tracking"
        const val ACTION_STOP_LOCATION_TRACKING = "stop_location_tracking"
        const val ACTION_STOP_SERVICE_IF_NOT_IN_FOREGROUND_MODE = "stop_service_if_not_in_foreground_mode"
    }
}

private const val NOTIFICATION_CHANNEL_ID = "location_channel"
private const val NOTIFICATION_ID = 11238
private const val TAG = "LocationService"
