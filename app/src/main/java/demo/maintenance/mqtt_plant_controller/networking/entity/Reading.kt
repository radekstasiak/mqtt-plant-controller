package demo.maintenance.mqtt_plant_controller.networking.entity

import com.squareup.moshi.Json

/**
 * Created here and now by radek.
 * Peace and love.
 */
class Reading(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "moisture_level_status") val moistureLevelStatus: Int,
    @field:Json(name = "soil_moisture_status") val soilMoistureStatus: String,
    @field:Json(name = "water_pump_status") val waterPumpStatus: Int,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String
)