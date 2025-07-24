package android.template.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
data class Waypoint(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val speed: Float,
    val latitude: Double,
    val longitude: Double,
    val tripId: Int
)

@Dao
interface WaypointDao {

    @Query("SELECT * FROM waypoint WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getWaypoints(
        tripId: Int = 0
    ): Flow<List<Waypoint>>

    @Insert
    suspend fun insertWaypoint(
        item: Waypoint
    )
}
