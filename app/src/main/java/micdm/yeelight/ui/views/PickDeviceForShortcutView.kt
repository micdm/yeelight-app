package micdm.yeelight.ui.views

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import butterknife.BindView
import io.reactivex.disposables.Disposable
import micdm.yeelight.R
import micdm.yeelight.di.DI
import micdm.yeelight.ui.activities.ToggleLightActivity
import javax.inject.Inject

class PickDeviceForShortcutView(context: Context, attrs: AttributeSet) : BaseView(context, attrs) {

    @Inject
    lateinit var activity: AppCompatActivity
    @Inject
    lateinit var layoutInflater: LayoutInflater

    @BindView(R.id.v__pick_device_for_shortcut__devices)
    lateinit var devicesView: DevicesView

    init {
        if (!isInEditMode) {
            DI.activityComponent?.inject(this)
        }
    }

    override fun createStructure() {
        layoutInflater.inflate(R.layout.v__pick_device_for_shortcut, this)
    }

    override fun subscribeForEvents(): Disposable? = subscribeForNavigation()

    private fun subscribeForNavigation(): Disposable {
        return devicesView.pickDeviceRequests.subscribe {
            val launchIntent = Intent(context, ToggleLightActivity::class.java)
            launchIntent.putExtra("DEVICE_HOST", it.first)
            launchIntent.putExtra("DEVICE_PORT", it.second)
            val intent = Intent()
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, resources.getString(R.string.shortcut_label))
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher_foreground))
            activity.setResult(AppCompatActivity.RESULT_OK, intent)
            activity.finish()
        }
    }
}
