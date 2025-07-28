package android.template.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTimestamp: Long,
    val duration: Long,
    val distance: Int
)

@Dao
interface TripDao {

    @Query("SELECT * FROM trip ORDER BY startTimestamp DESC")
    fun getTrips(): Flow<List<Trip>>

    @Update
    suspend fun updateTrip(
        item: Trip
    )

    @Insert
    suspend fun insertTrip(
        item: Trip
    ): Long
}
