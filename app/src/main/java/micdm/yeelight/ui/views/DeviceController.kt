package micdm.yeelight.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.di.DeviceModule
import micdm.yeelight.models.Address

private val DEVICE_HOST_KEY: String = "deviceHost"
private val DEVICE_PORT_KEY: String = "devicePort"

class DeviceController(args: Bundle): Controller(args) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        DI.deviceComponent =
            DI.mainActivityComponent!!.getDeviceComponentBuilder()
                .deviceModule(DeviceModule(Address(args.getString(DEVICE_HOST_KEY), args.getInt(DEVICE_PORT_KEY))))
                .build()
        return inflater.inflate(R.layout.c__device, container, false)
    }

    override fun onDestroyView(view: View) {
        DI.deviceComponent = null
    }
}

fun newDeviceController(host: String, port: Int): DeviceController {
    val args = Bundle()
    args.putString(DEVICE_HOST_KEY, host)
    args.putInt(DEVICE_PORT_KEY, port)
    return DeviceController(args)
}
