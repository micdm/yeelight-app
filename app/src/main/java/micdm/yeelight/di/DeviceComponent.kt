package micdm.yeelight.di

import dagger.Subcomponent
import micdm.yeelight.ui.views.DeviceView

@Subcomponent(modules = arrayOf(DeviceModule::class))
interface DeviceComponent {

    @Subcomponent.Builder
    interface Builder {
        fun deviceModule(module: DeviceModule): Builder
        fun build(): DeviceComponent
    }

    fun inject(target: DeviceView)
}
