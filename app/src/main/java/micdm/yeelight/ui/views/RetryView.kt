package micdm.yeelight.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import micdm.yeelight.R
import micdm.yeelight.di.DI
import javax.inject.Inject


class RetryView(context: Context, attrs: AttributeSet) : BaseView(context, attrs) {

    @Inject
    lateinit var layoutInflater: LayoutInflater

    @BindView(R.id.v__retry__label)
    lateinit var labelView: TextView
    @BindView(R.id.v__retry__button)
    lateinit var buttonView: View

    private val label: String

    val retryRequests: Observable<Any>
        get() = RxView.clicks(buttonView)

    init {
        if (!isInEditMode) {
            DI.activityComponent?.inject(this)
        }
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.RetryView)
        label = attributes.getString(R.styleable.RetryView_label)
        attributes.recycle()
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__retry, this)
    }

    override fun setupViews() {
        labelView.text = label
    }
}
