package micdm.yeelight.di

import dagger.Subcomponent
import micdm.yeelight.ui.activities.MainActivity
import micdm.yeelight.ui.activities.ShortcutActivity
import micdm.yeelight.ui.activities.ToggleLightActivity
import micdm.yeelight.ui.navigation.Navigator
import micdm.yeelight.ui.views.*

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
    fun inject(target: ShortcutActivity)
    fun inject(target: ToggleLightActivity)
    fun inject(target: DevicesView)
    fun inject(target: DevicesAdapter)
    fun inject(target: Navigator)
    fun inject(target: CannotConnectView)
    fun inject(target: PickDeviceToControlView)
    fun inject(target: PickDeviceForShortcutView)
}
