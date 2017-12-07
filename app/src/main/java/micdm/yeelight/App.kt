package micdm.yeelight

import android.app.Application
import micdm.yeelight.di.AppModule
import micdm.yeelight.di.DI
import micdm.yeelight.di.DaggerAppComponent
import timber.log.Timber
import javax.inject.Inject

class App: Application() {

    @Inject
    lateinit var tree: Timber.Tree

    override fun onCreate() {
        super.onCreate()
        DI.appComponent = DaggerAppComponent.builder().appModule(AppModule()).build()
        DI.appComponent!!.inject(this)
        Timber.plant(tree)
    }
}
