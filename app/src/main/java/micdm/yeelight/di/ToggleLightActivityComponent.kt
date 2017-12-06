package micdm.yeelight.di

import dagger.Subcomponent
import micdm.yeelight.ui.activities.ToggleLightActivity

@ActivityScope
@Subcomponent(modules = arrayOf(ActivityModule::class))
interface ToggleLightActivityComponent {

    @Subcomponent.Builder
    interface Builder {
        fun activityModule(module: ActivityModule): Builder
        fun build(): ToggleLightActivityComponent
    }

    fun inject(target: ToggleLightActivity)
}
