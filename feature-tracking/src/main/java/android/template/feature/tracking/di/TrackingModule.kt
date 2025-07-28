package android.template.feature.tracking.di

import android.template.feature.tracking.data.DefaultLocationRepository
import android.template.feature.tracking.data.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TrackingModule {

    @Singleton
    @Binds
    fun bindsLocationRepository(
        locationRepository: DefaultLocationRepository
    ): LocationRepository
}
