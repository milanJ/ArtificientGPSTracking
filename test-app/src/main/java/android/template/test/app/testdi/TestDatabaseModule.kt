package android.template.test.app.testdi

import android.template.core.data.TripsRepository
import android.template.core.data.di.DataModule
import android.template.test.app.FakeTripsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
interface FakeDataModule {

    @Binds
    abstract fun bindsTripsRepository(
        fakeRepository: FakeTripsRepository
    ): TripsRepository
}
