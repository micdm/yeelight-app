package micdm.yeelight.ui.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import micdm.yeelight.R
import micdm.yeelight.di.ActivityModule
import micdm.yeelight.di.DI
import micdm.yeelight.tools.DeviceFinder
import javax.inject.Inject

class ShortcutActivity : AppCompatActivity() {

    @Inject
    lateinit var deviceFinder: DeviceFinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.shortcutActivityComponent = DI.appComponent!!.getShortcutActivityComponentBuilder().activityModule(ActivityModule(this)).build()
        DI.shortcutActivityComponent?.inject(this)
        setContentView(R.layout.a__shortcut)
    }

    override fun onPause() {
        super.onPause()
        deviceFinder.stop()
    }

    override fun onResume() {
        super.onResume()
        deviceFinder.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        DI.shortcutActivityComponent = null
    }
}
