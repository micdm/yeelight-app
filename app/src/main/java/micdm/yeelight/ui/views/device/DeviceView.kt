package micdm.yeelight.ui.views.device

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.models.Address
import micdm.yeelight.models.DeviceState
import micdm.yeelight.models.UNDEFINED_DEVICE_STATE
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
    lateinit var toggleView: ImageView
    @BindView(R.id.v__device__pick_color)
    lateinit var pickColorView: PickColorView

    private val deviceController
        get() = deviceControllerStore.getDeviceController(deviceAddress)

    init {
        if (!isInEditMode) {
            DI.deviceComponent?.inject(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        deviceController.attach()
    }

    override fun onDetachedFromWindow() {
        deviceController.detach()
        super.onDetachedFromWindow()
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__device, this)
    }

    override fun setupViews() {
        pickColorView.visibility = View.GONE
    }

    override fun subscribeForEvents(): Disposable? {
        return CompositeDisposable(
            subscribeForConnectionState(),
            subscribeForDeviceState(),
            subscribeForConnect(),
            subscribeForToggle(),
            subscribeForColor()
        )
    }

    private fun subscribeForConnectionState(): Disposable {
        return deviceController.connectionState
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

    private fun subscribeForDeviceState(): Disposable {
        return deviceController.deviceState
            .filter { it !== UNDEFINED_DEVICE_STATE }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                connectedView.setBackgroundColor(resources.getColor(when (it.isEnabled) {
                    true -> R.color.device_enabled
                    else -> R.color.device_disabled
                }))
                toggleView.setColorFilter(resources.getColor(when (it.isEnabled) {
                    true -> R.color.device_enabled_inverse
                    else -> R.color.device_disabled_inverse
                }))
                pickColorView.setHue(it.hue)
                pickColorView.visibility = if (it.isEnabled) View.VISIBLE else View.GONE
            }
    }

    private fun subscribeForConnect(): Disposable {
        return cannotConnectView.getRetryRequests().subscribe {
            deviceController.connect()
        }
    }

    private fun subscribeForToggle(): Disposable {
        return RxView.clicks(toggleView)
            .switchMap {
                deviceController.toggle()
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
            .withLatestFrom(
                deviceController.deviceState,
                BiFunction { hue: Int, deviceState: DeviceState -> if (hue != deviceState.hue) hue else -1 }
            )
            .filter { it != -1 }
            .throttleLast(500, TimeUnit.MILLISECONDS)
            .switchMap {
                deviceController.setColor(it, 100)
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
}
