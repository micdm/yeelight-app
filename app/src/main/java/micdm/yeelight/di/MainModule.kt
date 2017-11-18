package micdm.yeelight.di

import dagger.Module
import dagger.Provides
import micdm.yeelight.tools.DeviceFinder

@Module
class MainModule {

    @Provides
    @AppScope
    fun provideDeviceFinder(): DeviceFinder {
        val instance = DeviceFinder()
        instance.init()
        return instance
    }
}
