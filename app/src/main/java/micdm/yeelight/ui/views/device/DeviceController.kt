package micdm.yeelight.ui.views.device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import micdm.yeelight.R

private val DEVICE_HOST_KEY: String = "deviceHost"
private val DEVICE_PORT_KEY: String = "devicePort"

class DeviceController(args: Bundle): Controller(args) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.c__device, container, false) as DeviceView
        view.DEVICE_HOST = args.getString(DEVICE_HOST_KEY)
        view.DEVICE_PORT = args.getInt(DEVICE_PORT_KEY)
        return view
    }
}

fun newDeviceController(host: String, port: Int): DeviceController {
    val args = Bundle()
    args.putString(DEVICE_HOST_KEY, host)
    args.putInt(DEVICE_PORT_KEY, port)
    return DeviceController(args)
}
