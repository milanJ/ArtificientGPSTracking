package android.template.core.di

import android.template.core.formaters.DateTimeFormatter
import android.template.core.formaters.IcuDateTimeFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object UIModule {

    @Singleton
    @Provides
    fun provideDateTimeFormatter(): DateTimeFormatter = IcuDateTimeFormatter()
}
