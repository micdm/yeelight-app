package micdm.yeelight.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.yeelight.R

class PickTemperatureView(context: Context, attrs: AttributeSet) : BaseView(context, attrs) {

    private val MIN_TEMPERATURE = 1700
    private val MAX_TEMPERATURE = 6500

    private val MAX_SATURATION = 100

    private val rect: RectF = RectF()
    private val paint: Paint = Paint()

    private val temperature = BehaviorSubject.createDefault<Int>(3000)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                             (resources.getDimension(R.dimen.color_picker_mark_radius) * 2.5f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val radius = resources.getDimension(R.dimen.color_picker_mark_radius)
        val stepX = (canvas.width - radius * 2) / MAX_SATURATION
        paint.style = Paint.Style.FILL
        for (saturation in 0..MAX_SATURATION) {
            rect.set(saturation * stepX + radius, radius, (saturation + 1) * stepX + radius, canvas.height.toFloat() - radius)
            paint.color = android.graphics.Color.HSVToColor(floatArrayOf(36f, 1f - saturation / 100f, 1f))
            canvas.drawRect(rect, paint)
        }
        if (temperature.hasValue()) {
            val saturation = ((temperature.value - MIN_TEMPERATURE).toFloat() / (MAX_TEMPERATURE - MIN_TEMPERATURE))
            paint.color = android.graphics.Color.HSVToColor(floatArrayOf(36f, maxOf(1f - saturation, 0.1f), 1f))
            canvas.drawCircle(saturation * 100 * stepX + radius, canvas.height / 2f, radius, paint)
        }
    }

    override fun setupViews() {
        setWillNotDraw(false)
        setOnTouchListener { v, event ->
            val radius = resources.getDimension(R.dimen.color_picker_mark_radius)
            temperature.onNext(MIN_TEMPERATURE + ((maxOf(minOf(event.x, width - radius), radius) - radius) / (width - radius * 2) * (MAX_TEMPERATURE - MIN_TEMPERATURE)).toInt())
            invalidate()
            true
        }
    }

    fun getTemperature(): Observable<Int> = temperature

    fun setTemperature(value: Int) {
        temperature.onNext(value)
        invalidate()
    }
}
