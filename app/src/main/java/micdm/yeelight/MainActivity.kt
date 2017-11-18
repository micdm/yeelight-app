package micdm.yeelight

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import micdm.yeelight.di.ActivityModule
import micdm.yeelight.di.DI
import micdm.yeelight.tools.DeviceFinder
import micdm.yeelight.ui.navigation.Navigator
import javax.inject.Inject

class MainActivity: AppCompatActivity() {

    @Inject
    lateinit var deviceFinder: DeviceFinder
    @Inject
    lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.activityComponent = DI.appComponent!!.getActivityComponentBuilder().activityModule(ActivityModule(this)).build()
        DI.activityComponent?.inject(this)
        setContentView(R.layout.a__main)
        navigator.init(findViewById<ViewGroup>(R.id.a__main__root), savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        deviceFinder.stop()
    }

    override fun onResume() {
        super.onResume()
        deviceFinder.start()
    }

    override fun onBackPressed() {
        if (!navigator.handleBack()) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DI.activityComponent = null
    }
}
