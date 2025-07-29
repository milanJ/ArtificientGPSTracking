package android.template.feature.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.DetectedActivity
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

    // Activity recognition related:
    private val activityRecognitionBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val detectedActivityType = intent.getIntExtra(ACTIVITY_RECOGNITION_ACTION_EXTRA_ACTIVITY_TYPE, DetectedActivity.UNKNOWN)
            val detectedActivityConfidence = intent.getIntExtra(ACTIVITY_RECOGNITION_ACTION_EXTRA_ACTIVITY_CONFIDENCE, 0)
            Log.d(TAG, "onReceive() :: = detectedActivityType = $detectedActivityType, detectedActivityConfidence = $detectedActivityConfidence")

            if (detectedActivityType == DetectedActivity.STILL && detectedActivityConfidence >= 75) {
                if (activityRecognitionStillStartTimestamp == null) {
                    activityRecognitionStillStartTimestamp = SystemClock.elapsedRealtime()
                }
            } else {
                activityRecognitionStillStartTimestamp = null
            }
        }
    }
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var activityRecognitionPendingIntent: PendingIntent
    private var activityRecognitionStillStartTimestamp: Long? = null

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

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        // Start location fetching:
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        restartLocationTracking()

        // Start activity recognition:
        activityRecognitionPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, ActivityRecognitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        activityRecognitionClient = ActivityRecognition.getClient(this)
        activityRecognitionClient.requestActivityUpdates(ACTIVITY_RECOGNITION_DETECTION_INTERVAL, activityRecognitionPendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "onCreate() :: Activity recognition updates requested")
            }
            .addOnFailureListener { e: Exception ->
                Log.e(TAG, "onCreate() :: Error requesting activity recognition updates", e)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(activityRecognitionBroadcastReceiver, IntentFilter(ACTIVITY_RECOGNITION_ACTION), RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(activityRecognitionBroadcastReceiver, IntentFilter(ACTIVITY_RECOGNITION_ACTION))
        }
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
                activityRecognitionStillStartTimestamp = null
                currentTrip = null
                previousLocation = null
                locationTrackingStartTime = SystemClock.elapsedRealtime()
                isTrackingPaused = false
                isTrackingLocation = true
                restartLocationTracking()
            }

            ACTION_RESUME_LOCATION_TRACKING -> {
                activityRecognitionStillStartTimestamp = null
                isTrackingPaused = false
            }

            ACTION_PAUSE_LOCATION_TRACKING -> {
                isTrackingPaused = true
            }

            ACTION_STOP_LOCATION_TRACKING -> {
                stopTracking()
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

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        // Stop Activity recognition:
        unregisterReceiver(activityRecognitionBroadcastReceiver)
        activityRecognitionClient.removeActivityUpdates(activityRecognitionPendingIntent)

        // Stop location fetching:
        isTrackingPaused = false
        isTrackingLocation = false
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        currentTrip = null
        previousLocation = null
        locationTrackingStartTime = 0L

        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private suspend fun processLocationUpdate(
        location: Location?
    ) {
        Log.d(TAG, "onLocationResult() :: location = $location, activityRecognitionStillStartTimestamp = $activityRecognitionStillStartTimestamp")

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

            currentTrip = tripsRepository.createTrip(System.currentTimeMillis())

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

        // The user has remained still for ACTIVITY_RECOGNITION_STILLNESS_TIMEOUT seconds, stop the tracking.
        val activityRecognitionStillStartTimestampLocal = activityRecognitionStillStartTimestamp
        if (activityRecognitionStillStartTimestampLocal != null
            && (SystemClock.elapsedRealtime() - activityRecognitionStillStartTimestampLocal) > ACTIVITY_RECOGNITION_STILLNESS_TIMEOUT
        ) {
            // Show notification.
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID_TRACKING_STOPPED_DUE_TO_STILLNESS, createTrackingStoppedDueStillnessNotification())

            // Stop tracking.
            locationRepository.setTrackingState(TrackingState.STOPPED)
            stopTracking()
        }
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
                startForeground(NOTIFICATION_ID_FOREGROUND, createForegroundServiceNotification(), FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID_FOREGROUND, createForegroundServiceNotification())
            }
        } else {
            Log.d(TAG, "restartLocationTracking() :: Moving the service OUT of foreground.")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun stopTracking() {
        activityRecognitionStillStartTimestamp = null
        isTrackingPaused = false
        isTrackingLocation = false
        restartLocationTracking()
    }

    private fun createForegroundServiceNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(ContextCompat.getString(this, R.string.location_service_notification_content_title))
            .setContentText(ContextCompat.getString(this, R.string.location_service_notification_content_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun createTrackingStoppedDueStillnessNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(ContextCompat.getString(this, R.string.location_service_notification_content_title))
            .setContentText(ContextCompat.getString(this, R.string.location_service_tracking_stopped_notification_content_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                ContextCompat.getString(this, R.string.location_service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
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

const val ACTIVITY_RECOGNITION_ACTION_EXTRA_ACTIVITY_CONFIDENCE = "android.template.feature.tracking.LocationService.ACTIVITY_RECOGNITION.activity_confidence"
const val ACTIVITY_RECOGNITION_ACTION_EXTRA_ACTIVITY_TYPE = "android.template.feature.tracking.LocationService.ACTIVITY_RECOGNITION.activity_type"
const val ACTIVITY_RECOGNITION_ACTION = "android.template.feature.tracking.LocationService.ACTIVITY_RECOGNITION"

private const val ACTIVITY_RECOGNITION_STILLNESS_TIMEOUT = 3L * 60L * 1000L // 3 minutes
private const val ACTIVITY_RECOGNITION_DETECTION_INTERVAL = 10000L // 10 seconds

private const val NOTIFICATION_CHANNEL_ID = "location_channel"
private const val NOTIFICATION_ID_FOREGROUND = 11238
private const val NOTIFICATION_ID_TRACKING_STOPPED_DUE_TO_STILLNESS = 11239

private const val TAG = "LocationService"
