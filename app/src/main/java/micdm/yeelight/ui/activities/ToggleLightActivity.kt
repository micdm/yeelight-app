package micdm.yeelight.ui.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import micdm.yeelight.R
import micdm.yeelight.di.ActivityModule
import micdm.yeelight.di.DI
import micdm.yeelight.models.Address
import micdm.yeelight.models.UNDEFINED_DEVICE_STATE
import micdm.yeelight.tools.DeviceControllerStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ToggleLightActivity : AppCompatActivity() {

    private val MIN_DELAY = 500

    @Inject
    lateinit var deviceControllerStore: DeviceControllerStore

    private val deviceController
        get() = deviceControllerStore.getDeviceController(Address(intent.getStringExtra("DEVICE_HOST"),
                                                                  intent.getIntExtra("DEVICE_PORT", -1)))

    private var subscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DI.activityComponent = DI.appComponent!!.getActivityComponentBuilder().activityModule(ActivityModule(this)).build()
        DI.activityComponent?.inject(this)
        setContentView(R.layout.a__toggle_light)
    }

    override fun onStart() {
        super.onStart()
        val now = System.currentTimeMillis()
        subscription = deviceController.deviceState
            .filter { it !== UNDEFINED_DEVICE_STATE }
            .take(1)
            .switchMap {
                deviceController.toggle()
                    .toSingleDefault(true)
                    .onErrorReturnItem(false)
                    .toObservable()
            }
            .timeout(5, TimeUnit.SECONDS, Observable.just(false))
            .delay {
                if (it) {
                    val delta = System.currentTimeMillis() - now
                    Observable.timer(maxOf(0, MIN_DELAY - delta), TimeUnit.MILLISECONDS)
                } else {
                    Observable.just(0L)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!it) {
                    Toast.makeText(this, R.string.a__toggle_light__fail, Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        deviceController.attach()
    }

    override fun onStop() {
        super.onStop()
        subscription?.dispose()
        deviceController.detach()
    }
}
