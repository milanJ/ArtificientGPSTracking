package android.template.feature.tracking.data

data class LocationModel(
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val distanceTraveled: Int,
    val elapsedTime: Long
)
