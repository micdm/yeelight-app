package micdm.yeelight

import android.app.Application
import micdm.yeelight.di.AppModule
import micdm.yeelight.di.DI
import micdm.yeelight.di.DaggerAppComponent

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        DI.appComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build()
    }
}
