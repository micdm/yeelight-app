package micdm.yeelight.di

import dagger.Module
import dagger.Provides
import micdm.yeelight.models.Address

@Module
class DeviceModule(private val address: Address) {

    @Provides
    fun provideDeviceAddress(): Address = address
}
