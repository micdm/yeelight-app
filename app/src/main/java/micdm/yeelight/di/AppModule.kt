package micdm.yeelight.di

import dagger.Module
import dagger.Provides
import micdm.yeelight.BuildConfig
import micdm.yeelight.tools.DeviceControllerStore
import micdm.yeelight.tools.DeviceFinder
import timber.log.Timber

@Module
class AppModule {

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

    @Provides
    @AppScope
    fun provideTimberTree(): Timber.Tree {
        if (BuildConfig.DEBUG) {
            return Timber.DebugTree()
        }
        return object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Nothing to do here
            }
        }
    }
}
