package micdm.yeelight.di

import android.support.v7.app.AppCompatActivity
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import dagger.Module
import dagger.Provides
import micdm.yeelight.ui.navigation.Navigator
import javax.inject.Named

@Module
class ActivityModule(private val activity: AppCompatActivity) {

    @Provides
    @ActivityScope
    fun provideAppCompatActivity(): AppCompatActivity = activity

    @Provides
    @ActivityScope
    fun provideNavigator(): Navigator {
        val instance = Navigator()
        DI.activityComponent?.inject(instance)
        return instance
    }

    @Provides
    @Named("push")
    @ActivityScope
    fun providePushChangeHandler(): ControllerChangeHandler = HorizontalChangeHandler()

    @Provides
    @Named("pop")
    @ActivityScope
    fun providePopChangeHandler(): ControllerChangeHandler = HorizontalChangeHandler()
}
