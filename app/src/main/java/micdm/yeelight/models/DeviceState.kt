package micdm.yeelight.models

interface Color

class UndefinedColor : Color

data class TemperatureColor(val temperature: Int) : Color

data class HsvColor(val hue: Int, val saturation: Int) : Color

data class DeviceState(val isEnabled: Boolean, val color: Color)

val UNDEFINED_DEVICE_STATE = DeviceState(false, UndefinedColor())
