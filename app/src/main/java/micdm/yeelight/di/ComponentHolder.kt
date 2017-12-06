package micdm.yeelight.di

class ComponentHolder {

    var appComponent: AppComponent? = null
    var mainActivityComponent: MainActivityComponent? = null
    var shortcutActivityComponent: ShortcutActivityComponent? = null
    var toggleLightActivityComponent: ToggleLightActivityComponent? = null
    var deviceComponent: DeviceComponent? = null
}

val DI = ComponentHolder()
