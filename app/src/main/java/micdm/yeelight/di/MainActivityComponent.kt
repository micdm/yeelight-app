package micdm.yeelight.di

import dagger.Subcomponent
import micdm.yeelight.ui.activities.MainActivity
import micdm.yeelight.ui.navigation.Navigator
import micdm.yeelight.ui.views.*

@ActivityScope
@Subcomponent(modules = arrayOf(ActivityModule::class))
interface MainActivityComponent {

    @Subcomponent.Builder
    interface Builder {
        fun activityModule(module: ActivityModule): Builder
        fun build(): MainActivityComponent
    }

    fun getDeviceComponentBuilder(): DeviceComponent.Builder

    fun inject(target: MainActivity)
    fun inject(target: DevicesView)
    fun inject(target: DevicesAdapter)
    fun inject(target: Navigator)
    fun inject(target: RetryView)
    fun inject(target: PickDeviceToControlView)
    fun inject(target: PickDeviceForShortcutView)
}
