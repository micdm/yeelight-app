package micdm.yeelight.di

import dagger.Component
import micdm.yeelight.App
import micdm.yeelight.tools.DeviceFinder

@AppScope
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {

    fun getActivityComponentBuilder(): ActivityComponent.Builder

    fun getDeviceFinder(): DeviceFinder

    fun inject(target: App)
}
