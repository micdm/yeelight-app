package micdm.yeelight.ui.views.device

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.ui.views.BaseView
import javax.inject.Inject

class CannotConnectView(context: Context, attrs: AttributeSet) : BaseView(context, attrs) {

    @Inject
    lateinit var layoutInflater: LayoutInflater

    @BindView(R.id.v__device__cannot_connect__button)
    lateinit var buttonView: View

    init {
        if (!isInEditMode) {
            DI.activityComponent?.inject(this)
        }
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__device__cannot_connect, this)
    }

    fun getRetryRequests(): Observable<Any> = RxView.clicks(buttonView)
}
