package demo.maintenance.mqtt_plant_controller.networking.entity

import com.squareup.moshi.Json

/**
 * Created here and now by radek.
 * Peace and love.
 */
class Reading(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "moisture_level_status") val moistureLevelStatus: Int,
    @Json(name = "soil_moisture_status") val soilMoistureStatus: String,
    @Json(name = "water_pump_status") val waterPumpStatus: Int,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)