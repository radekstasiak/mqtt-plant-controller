package demo.maintenance.mqtt_plant_controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import demo.maintenance.mqtt_plant_controller.networking.GrowApiService
import demo.maintenance.mqtt_plant_controller.networking.entity.Reading
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class GraphActivity : AppCompatActivity(), Callback<List<Reading>> {
    override fun onFailure(call: Call<List<Reading>>, t: Throwable) {
        Log.d(TAG, "failure")
    }

    override fun onResponse(call: Call<List<Reading>>, response: Response<List<Reading>>) {
        val readings = ArrayList(response.body())
        Log.d(TAG, readings.toString())
    }

    val TAG = GraphActivity::class.java.name
    val BASE_URL = "https://grow-mqtt-readings.herokuapp.com/"
    lateinit var apiService: GrowApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build();

        apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GrowApiService::class.java)

        val readings: Call<List<Reading>> = apiService.getReadings()
        val readingsResponse = readings.enqueue(this)
    }
}
