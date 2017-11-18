package micdm.yeelight.ui.views.device

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.tools.DeviceController
import micdm.yeelight.ui.views.BaseView
import javax.inject.Inject

class DeviceView(context: Context, attrs: AttributeSet): BaseView(context, attrs) {

    lateinit var DEVICE_HOST: String
    var DEVICE_PORT: Int = 0

    @Inject
    lateinit var layoutInflater: LayoutInflater

    @BindView(R.id.v__device__toggle)
    lateinit var toggleView: View

    init {
        if (!isInEditMode) {
            DI.activityComponent?.inject(this)
        }
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__device, this)
    }

    override fun subscribeForEvents(): Disposable? {
        return RxView.clicks(toggleView)
            .switchMap {
                DeviceController(DEVICE_HOST, DEVICE_PORT).toggle()
                    .toSingleDefault(true)
                    .onErrorReturnItem(false)
                    .toObservable()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it) {
                    Toast.makeText(context, "OK!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "FAIL!", Toast.LENGTH_LONG).show()
                }
            }
    }
}
