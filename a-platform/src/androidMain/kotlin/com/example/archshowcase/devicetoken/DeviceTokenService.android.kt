// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.devicetoken

actual class DeviceTokenService actual constructor() {

    actual fun getUmid(): String? = DeviceTokenInitializer.getUmid()

    actual fun sign(fields: Map<String, String>): String? = DeviceTokenInitializer.sign(fields)

    actual fun reportAction(action: DeviceTokenAction) = DeviceTokenInitializer.reportAction(action)
}
