package micdm.yeelight.ui.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import butterknife.ButterKnife
import butterknife.Unbinder
import io.reactivex.disposables.Disposable

abstract class BaseView(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {

    private var viewUnbinder: Unbinder? = null
    private var subscription: Disposable? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        createViewHierarchy()
    }

    private fun createViewHierarchy() {
        createStructure()
        viewUnbinder = ButterKnife.bind(this)
        setupViews()
    }

    open fun createStructure() {}

    open fun setupViews() {}

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            subscription = subscribeForEvents()
        }
    }

    open fun subscribeForEvents(): Disposable? = null

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscription?.dispose()
        viewUnbinder?.unbind()
    }
}

abstract class BaseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    init {
        ButterKnife.bind(this, itemView)
    }
}
