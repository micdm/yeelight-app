package micdm.yeelight.ui.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import micdm.yeelight.R

class DevicesController: Controller() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.c__devices, container, false)
}
