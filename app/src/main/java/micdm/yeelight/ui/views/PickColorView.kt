package micdm.yeelight.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.yeelight.R
import micdm.yeelight.ui.views.BaseView

class PickColorView(context: Context, attrs: AttributeSet): BaseView(context, attrs) {

    private val MAX_HUE = 359

    private val rect: RectF = RectF()
    private val paint: Paint = Paint()

    private val hue = BehaviorSubject.create<Int>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                             (resources.getDimension(R.dimen.color_picker_mark_radius) * 2.5f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val radius = resources.getDimension(R.dimen.color_picker_mark_radius)
        val stepX = (canvas.width - radius * 2) / MAX_HUE
        paint.style = Paint.Style.FILL
        for (hue in 0..MAX_HUE) {
            rect.set(hue * stepX + radius, radius, (hue + 1) * stepX + radius, canvas.height.toFloat() - radius)
            paint.color = android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.7f, 1f))
            canvas.drawRect(rect, paint)
        }
        if (hue.hasValue()) {
            paint.color = android.graphics.Color.HSVToColor(floatArrayOf(hue.value.toFloat(), 0.7f, 1f))
            canvas.drawCircle(hue.value * stepX + radius, canvas.height / 2f, radius, paint)
        }
    }

    override fun setupViews() {
        setWillNotDraw(false)
        setOnTouchListener { v, event ->
            val radius = resources.getDimension(R.dimen.color_picker_mark_radius)
            hue.onNext(((maxOf(minOf(event.x, width - radius), radius) - radius) / (width - radius * 2) * MAX_HUE).toInt())
            invalidate()
            true
        }
    }

    fun getHue(): Observable<Int> = hue

    fun setHue(value: Int) {
        hue.onNext(value)
        invalidate()
    }
}
