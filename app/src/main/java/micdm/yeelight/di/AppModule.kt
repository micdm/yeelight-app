package micdm.yeelight.di

import dagger.Module
import dagger.Provides
import micdm.yeelight.App
import micdm.yeelight.tools.DeviceControllerStore
import micdm.yeelight.tools.DeviceFinder

@Module
class AppModule(private val app: App) {

    @Provides
    @AppScope
    fun provideDeviceFinder(): DeviceFinder {
        val instance = DeviceFinder()
        instance.init()
        return instance
    }

    @Provides
    @AppScope
    fun provideDeviceControllerStore(): DeviceControllerStore = DeviceControllerStore()
}
