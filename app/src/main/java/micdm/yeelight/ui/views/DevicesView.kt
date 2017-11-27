package micdm.yeelight.ui.views

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import io.reactivex.android.schedulers.AndroidSchedulers
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

    @BindView(R.id.v__devices__devices)
    lateinit var devicesView: RecyclerView

    val pickDeviceRequests
        get() = (devicesView.adapter as DevicesAdapter).pickDeviceRequests

    init {
        if (!isInEditMode) {
            DI.activityComponent?.inject(this)
        }
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__devices, this)
    }

    override fun setupViews() {
        val adapter = DevicesAdapter()
        DI.activityComponent?.inject(adapter)
        devicesView.adapter = adapter
        devicesView.layoutManager = LinearLayoutManager(context)
    }

    override fun subscribeForEvents(): Disposable? = subscribeForDevices()

    private fun subscribeForDevices(): Disposable {
        return deviceFinder.getDevices()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                (devicesView.adapter as DevicesAdapter).devices = it.toList().sortedBy { "${it.host}:${it.port}" }
            }
    }
}

class DevicesAdapter: RecyclerView.Adapter<ViewHolder>() {

    @Inject
    lateinit var layoutInflater: LayoutInflater

    private var items: List<Device> = emptyList()
    val pickDeviceRequests: Subject<Pair<String, Int>> = PublishSubject.create()

    var devices: List<Device> = items
        set(value) {
            items = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.v__devices_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.setOnClickListener { pickDeviceRequests.onNext(Pair(item.host, item.port)) }
        holder.addressView.text = "${item.host}:${item.port}"
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class ViewHolder(itemView: View): BaseViewHolder(itemView) {

    @BindView(R.id.v__devices_item__address)
    lateinit var addressView: TextView
}
