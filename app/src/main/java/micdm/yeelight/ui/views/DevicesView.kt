package micdm.yeelight.ui.views

import android.content.Context
import android.content.res.Resources
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.models.Device
import micdm.yeelight.tools.DeviceFinder
import javax.inject.Inject

class DevicesView(context: Context, attrs: AttributeSet): BaseView(context, attrs) {

    @Inject
    lateinit var deviceFinder: DeviceFinder
    @Inject
    lateinit var layoutInflater: LayoutInflater

    @BindView(R.id.v__devices__discovering)
    lateinit var discoveringView: View
    @BindView(R.id.v__devices__discovered)
    lateinit var discoveredView: View
    @BindView(R.id.v__devices__status)
    lateinit var statusView: TextView
    @BindView(R.id.v__devices__devices)
    lateinit var devicesView: RecyclerView
    @BindView(R.id.v__devices__cannot_discover)
    lateinit var cannotDiscoverView: RetryView

    val pickDeviceRequests
        get() = (devicesView.adapter as DevicesAdapter).pickDeviceRequests

    init {
        if (!isInEditMode) {
            DI.mainActivityComponent?.inject(this)
        }
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__devices, this)
    }

    override fun setupViews() {
        val adapter = DevicesAdapter()
        DI.mainActivityComponent?.inject(adapter)
        devicesView.adapter = adapter
        devicesView.layoutManager = LinearLayoutManager(context)
    }

    override fun subscribeForEvents(): Disposable? =
        CompositeDisposable(
            subscribeForState(),
            subscribeForDiscoverRequests()
        )

    private fun subscribeForState(): Disposable {
        return deviceFinder.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it is DeviceFinder.DiscoveringState) {
                    discoveringView.visibility = VISIBLE
                    discoveredView.visibility = GONE
                    cannotDiscoverView.visibility = GONE
                }
                if (it is DeviceFinder.DiscoveredState) {
                    discoveringView.visibility = GONE
                    discoveredView.visibility = VISIBLE
                    statusView.text = resources.getString(R.string.v__devices__discovering)
                    (devicesView.adapter as DevicesAdapter).devices = it.devices.toList().sortedBy { "${it.host}:${it.port}" }
                    cannotDiscoverView.visibility = GONE
                }
                if (it is DeviceFinder.FinishedState) {
                    discoveringView.visibility = GONE
                    discoveredView.visibility = VISIBLE
                    statusView.text = resources.getString(R.string.v__devices__finished, it.devices.size.toString(),
                                                          resources.getQuantityString(R.plurals.device, it.devices.size))
                    (devicesView.adapter as DevicesAdapter).devices = it.devices.toList().sortedBy { "${it.host}:${it.port}" }
                    cannotDiscoverView.visibility = GONE
                }
                if (it is DeviceFinder.FailedState) {
                    discoveringView.visibility = GONE
                    discoveredView.visibility = GONE
                    cannotDiscoverView.visibility = VISIBLE
                }
            }
    }

    private fun subscribeForDiscoverRequests(): Disposable =
        cannotDiscoverView.retryRequests.subscribe {
            deviceFinder.discover()
        }
}

class DevicesAdapter: RecyclerView.Adapter<ViewHolder>() {

    @Inject
    lateinit var layoutInflater: LayoutInflater
    @Inject
    lateinit var resources: Resources

    private var items: List<Device> = emptyList()
    val pickDeviceRequests: Subject<Pair<String, Int>> = PublishSubject.create()

    var devices: List<Device> = items
        set(value) {
            items = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(layoutInflater.inflate(R.layout.v__devices_item, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.setOnClickListener { pickDeviceRequests.onNext(Pair(item.host, item.port)) }
        holder.addressView.text = resources.getString(R.string.v__devices_item__text, item.host, item.port.toString())
    }

    override fun getItemCount(): Int = items.size
}

class ViewHolder(itemView: View): BaseViewHolder(itemView) {

    @BindView(R.id.v__devices_item__address)
    lateinit var addressView: TextView
}
