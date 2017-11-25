package micdm.yeelight.di

import dagger.Subcomponent
import micdm.yeelight.MainActivity
import micdm.yeelight.ui.navigation.Navigator
import micdm.yeelight.ui.views.device.CannotConnectView
import micdm.yeelight.ui.views.devices.DevicesAdapter
import micdm.yeelight.ui.views.devices.DevicesView

@ActivityScope
@Subcomponent(modules = arrayOf(ActivityModule::class))
interface ActivityComponent {

    @Subcomponent.Builder
    interface Builder {
        fun activityModule(module: ActivityModule): Builder
        fun build(): ActivityComponent
    }

    fun getDeviceComponentBuilder(): DeviceComponent.Builder

    fun inject(target: MainActivity)
    fun inject(target: DevicesView)
    fun inject(target: DevicesAdapter)
    fun inject(target: Navigator)
    fun inject(target: CannotConnectView)
}
