package micdm.yeelight.tools

import micdm.yeelight.models.Address

class DeviceControllerStore {

    private val controllers: MutableMap<Address, DeviceController> = hashMapOf()

    fun getDeviceController(address: Address): DeviceController {
        var controller = controllers[address]
        if (controller == null) {
            controller = DeviceController(address)
            controller.init()
            controllers[address] = controller
        }
        return controller
    }
}
