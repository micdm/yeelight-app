package micdm.yeelight.di

import dagger.Subcomponent
import micdm.yeelight.ui.activities.ShortcutActivity

@ActivityScope
@Subcomponent(modules = arrayOf(ActivityModule::class))
interface ShortcutActivityComponent {

    @Subcomponent.Builder
    interface Builder {
        fun activityModule(module: ActivityModule): Builder
        fun build(): ShortcutActivityComponent
    }

    fun inject(target: ShortcutActivity)
}
