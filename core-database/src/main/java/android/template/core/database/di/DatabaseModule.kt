package android.template.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.template.core.database.AppDatabase
import android.template.core.database.TripDao
import android.template.core.database.WaypointDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    fun provideWaypointDao(
        appDatabase: AppDatabase
    ): WaypointDao = appDatabase.waypointDao()

    @Provides
    fun provideTripDao(
        appDatabase: AppDatabase
    ): TripDao = appDatabase.tripDao()

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext appContext: Context
    ): AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "WaypointsAndTripsDatabase"
    ).build()
}
