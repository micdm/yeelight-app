package micdm.yeelight.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import butterknife.BindView
import io.reactivex.disposables.Disposable
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.ui.navigation.Navigator
import javax.inject.Inject

class PickDeviceToControlView(context: Context, attrs: AttributeSet) : BaseView(context, attrs) {

    @Inject
    lateinit var layoutInflater: LayoutInflater
    @Inject
    lateinit var navigator: Navigator

    @BindView(R.id.v__pick_device_to_control__devices)
    lateinit var devicesView: DevicesView

    init {
        if (!isInEditMode) {
            DI.activityComponent?.inject(this)
        }
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__pick_device_to_control, this)
    }

    override fun subscribeForEvents(): Disposable? = subscribeForNavigation()

    private fun subscribeForNavigation(): Disposable {
        return devicesView.pickDeviceRequests.subscribe {
            navigator.goToDevice(it.first, it.second)
        }
    }
}
