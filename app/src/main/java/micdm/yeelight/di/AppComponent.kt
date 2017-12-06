package micdm.yeelight.di

import dagger.Component
import micdm.yeelight.App
import micdm.yeelight.tools.DeviceFinder

@AppScope
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {

    fun getMainActivityComponentBuilder(): MainActivityComponent.Builder
    fun getShortcutActivityComponentBuilder(): ShortcutActivityComponent.Builder
    fun getToggleLightActivityComponentBuilder(): ToggleLightActivityComponent.Builder

    fun getDeviceFinder(): DeviceFinder

    fun inject(target: App)
}
