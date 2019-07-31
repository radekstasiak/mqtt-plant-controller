package demo.maintenance.mqtt_plant_controller.networking

import demo.maintenance.mqtt_plant_controller.networking.entity.Reading
import retrofit2.Call
import retrofit2.http.GET


/**
 * Created here and now by radek.
 * Peace and love.
 */
interface GrowApiService {
    @GET("/api/v1/readings")
    fun getReadings(): Call<List<Reading>>
}