package micdm.yeelight.models

data class DeviceState(val isEnabled: Boolean, val hue: Int, val saturation: Int)

val UNDEFINED_DEVICE_STATE = DeviceState(false, 0, 0)
