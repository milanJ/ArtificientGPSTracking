package android.template.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Waypoint::class, Trip::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun waypointDao(): WaypointDao

    abstract fun tripDao(): TripDao
}
