package micdm.yeelight.ui.navigation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import micdm.yeelight.ui.views.device.newDeviceController
import micdm.yeelight.ui.views.devices.DevicesController
import javax.inject.Inject
import javax.inject.Named

class Navigator {

    @Inject
    lateinit var activity: AppCompatActivity
    @field:[Inject Named("push")]
    lateinit var pushChangeHandler: ControllerChangeHandler
    @field:[Inject Named("push")]
    lateinit var popChangeHandler: ControllerChangeHandler

    private lateinit var router: Router

    fun init(container: ViewGroup, savedInstanceState: Bundle?) {
        router = Conductor.attachRouter(activity, container, savedInstanceState)
        if (!router.hasRootController()) {
            goToDevices()
        }
    }

    fun goToDevices() {
        router.setRoot(RouterTransaction.with(DevicesController()))
    }

    fun goToDevice(host: String, port: Int) {
        router.pushController(
            RouterTransaction.with(newDeviceController(host, port))
                .pushChangeHandler(pushChangeHandler)
                .popChangeHandler(popChangeHandler)
        )
    }

    fun handleBack(): Boolean = router.handleBack()
}
