package micdm.yeelight.di

class ComponentHolder {

    var appComponent: AppComponent? = null
    var activityComponent: ActivityComponent? = null
    var deviceComponent: DeviceComponent? = null
}

val DI = ComponentHolder()
