package demo.maintenance.mqtt_plant_controller

import com.squareup.moshi.Json

/**
 * Created here and now by radek.
 * Peace and love.
 */

class MqttDeviceState(
     @field:Json(name = "hmdt_raw_value") val hmdtRawValue: Int,
     @field:Json(name = "hmdt_percentage_value") val hmdtPercentageValue: Int,
     @field:Json(name = "water_pump_status") val waterPumpStatus: Int,
     @field:Json(name = "auto_watering_mode_status") val autoWateringModeStatus: Int
){

    override fun toString(): String {
        return "hmdtRawValue: ${hmdtRawValue},hmdtPercentageValue: ${hmdtPercentageValue}, waterPumpStatus: ${waterPumpStatus}, autoWateringModeStatus ${autoWateringModeStatus} "
    }
}
