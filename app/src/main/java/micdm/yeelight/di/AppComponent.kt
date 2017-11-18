package micdm.yeelight.di

import android.view.LayoutInflater
import dagger.Component
import micdm.yeelight.App
import micdm.yeelight.tools.DeviceFinder

@AppScope
@Component(modules = arrayOf(AppModule::class, MainModule::class))
interface AppComponent {

    fun getActivityComponentBuilder(): ActivityComponent.Builder

    fun getDeviceFinder(): DeviceFinder
    fun getLayoutInflater(): LayoutInflater

    fun inject(target: App)
}
