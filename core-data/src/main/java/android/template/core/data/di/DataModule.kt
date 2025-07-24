package android.template.core.data.di

import android.template.core.data.DefaultTripsRepository
import android.template.core.data.TripsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsTripsRepository(
        tripsRepository: DefaultTripsRepository
    ): TripsRepository
}
