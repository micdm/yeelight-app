package micdm.yeelight.ui.views.device

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.models.Address
import micdm.yeelight.tools.DeviceController
import micdm.yeelight.tools.DeviceControllerStore
import micdm.yeelight.ui.views.BaseView
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeviceView(context: Context, attrs: AttributeSet) : BaseView(context, attrs) {

    @Inject
    lateinit var deviceControllerStore: DeviceControllerStore
    @Inject
    lateinit var deviceAddress: Address
    @Inject
    lateinit var layoutInflater: LayoutInflater

    @BindView(R.id.v__device__connecting)
    lateinit var connectingView: View
    @BindView(R.id.v__device__connected)
    lateinit var connectedView: View
    @BindView(R.id.v__device__cannot_connect)
    lateinit var cannotConnectView: CannotConnectView

    @BindView(R.id.v__device__toggle)
    lateinit var toggleView: View
    @BindView(R.id.v__device__pick_color)
    lateinit var pickColorView: PickColorView

    init {
        if (!isInEditMode) {
            DI.deviceComponent?.inject(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        deviceControllerStore.getDeviceController(deviceAddress).attach()
    }

    override fun onDetachedFromWindow() {
        deviceControllerStore.getDeviceController(deviceAddress).detach()
        super.onDetachedFromWindow()
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__device, this)
    }

    override fun subscribeForEvents(): Disposable? {
        return CompositeDisposable(
            subscribeForState(),
            subscribeForConnect(),
            subscribeForToggle(),
            subscribeForColor()
        )
    }

    private fun subscribeForState(): Disposable {
        return deviceControllerStore.getDeviceController(deviceAddress).state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                connectingView.visibility = View.GONE
                connectedView.visibility = View.GONE
                cannotConnectView.visibility = View.GONE
                (when (it) {
                    is DeviceController.ConnectingState -> connectingView
                    is DeviceController.ConnectedState -> connectedView
                    is DeviceController.DisconnectedState -> cannotConnectView
                    else -> throw IllegalStateException("not supposed to happen")
                }).visibility = View.VISIBLE
            }
    }

    private fun subscribeForConnect(): Disposable {
        return cannotConnectView.getRetryRequests().subscribe {
            deviceControllerStore.getDeviceController(deviceAddress).connect()
        }
    }

    private fun subscribeForToggle(): Disposable {
        return RxView.clicks(toggleView)
            .switchMap {
                deviceControllerStore.getDeviceController(deviceAddress).toggle()
                    .toSingleDefault(true)
                    .onErrorReturnItem(false)
                    .toObservable()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!it) {
                    Toast.makeText(context, "FAIL!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun subscribeForColor(): Disposable {
        return pickColorView.getHue()
            .throttleLast(500, TimeUnit.MILLISECONDS)
            .switchMap {
                deviceControllerStore.getDeviceController(deviceAddress).setColor(it, 100)
                    .toSingleDefault(true)
                    .onErrorReturnItem(false)
                    .toObservable()
            }
            .subscribe {
                if (!it) {
                    Toast.makeText(context, "FAIL!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
