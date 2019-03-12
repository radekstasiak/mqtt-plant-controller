package demo.maintenance.mqtt_plant_controller

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.UnsupportedEncodingException
import android.os.Vibrator


class MainActivity : AppCompatActivity() {
    val MQTT_SERVER_ADDRESS = "m24.cloudmqtt.com"
    val MQTT_USER = "vlfnelyd"
    val MQTT_PASS = "nXE1FVBVSJHn"
    val MQTT_PORT = 17236

    val MQTT_TOPIC_WATER_PUMP = "waterPump"
    val MQTT_TOPIC_WATER_PUMP_CMD = "waterPump"
    val MQTT_TOPIC_GET_WATER_PUMP_STATUS = "getWaterPumpStatus"
    val MQTT_TOPIC_WATER_PUMP_STATUS = "sendWaterPumpStatus"
    val MQTT_TOPIC_HUMIDITY_LEVEL = "hmdtLevel"
    val MQTT_TOPIC_HUMIDITY_LEVEL_CMD="hmdtLevelCmd"

    val MQTT_TOPIC_AUTOWATERING_CMD="autoWateringCmd";
    val MQTT_TOPIC_AUTOWATERING_STATUS="autoWateringStatus";

    val MQTT_CMD_START = "start"
    val MQTT_CMD_STOP = "stop"
    val MQTT_CMD_STATUS = "status"

    var isAutoModeEnabled = false
    lateinit var hmdtTv: TextView
    val mqttClientId = MqttClient.generateClientId()
    lateinit var mqttClient: MqttAndroidClient
    lateinit var vibrator:Vibrator
//TODO connect/disconnect on button
//TODO assign id to the client
//TODO get connected clients
//TODO reflect if pump is running or stopped
//TODO service to receive updates and create notifications

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mqttClient = MqttAndroidClient(
            this.applicationContext, "tcp://${MQTT_SERVER_ADDRESS}:${MQTT_PORT}",
            mqttClientId
        )
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        logo.setOnClickListener { view ->
            if (isAutoModeEnabled) {
                publish(mqttClient,MQTT_TOPIC_AUTOWATERING_CMD, MQTT_CMD_STOP)
                isAutoModeEnabled = false
                Log.d("file", "${MQTT_TOPIC_AUTOWATERING_CMD}: MQTT_CMD_STOP")
            } else {
                publish(mqttClient, MQTT_TOPIC_AUTOWATERING_CMD, MQTT_CMD_START)
                isAutoModeEnabled = true
                Log.d("file", "${MQTT_TOPIC_AUTOWATERING_CMD}: MQTT_CMD_START")
            }

        }

        plantStatusIv.setOnTouchListener(object: View.OnTouchListener{
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                val action = event?.action
                if(action == MotionEvent.ACTION_DOWN){
                    publish(mqttClient, MQTT_TOPIC_WATER_PUMP,MQTT_CMD_START)
//                    isWaterPumpRunning = false
                    Log.d("file", "${MQTT_TOPIC_WATER_PUMP}: MQTT_CMD_START")
                    val pattern = longArrayOf(0, 200, 500)

                    vibrator.vibrate(pattern,0)
//                    isWaterPumpOn(true)
                }else if(action == MotionEvent.ACTION_UP){
                    publish(mqttClient, MQTT_TOPIC_WATER_PUMP,MQTT_CMD_STOP)
//                    isWaterPumpRunning = true
                    Log.d("file", "${MQTT_TOPIC_WATER_PUMP}: MQTT_CMD_STOP")
                    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.cancel()
//                    isWaterPumpOn(false)
                }
                return true
            }
            }
        )
    }
    private fun updatePlantStatus(status:Int){
        if(status <= 25){
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.plantstate0))
        }else if(status > 25 && status <= 50){
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.plantstate1))
        }else if (status > 50 && status <=80){
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.plantstate2))
        }else{
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.plantstate3))

        }
    }

    override fun onResume() {
        super.onResume()
        if (!mqttClient.isConnected){
            Log.d("file","connecting to mqtt")
            connect()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun connect() {
        val options = MqttConnectOptions()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1
        options.isCleanSession = false
        options.isAutomaticReconnect = true
        options.userName = MQTT_USER
        options.password = MQTT_PASS.toCharArray()
        try {
            val token = mqttClient.connect(options)
            //IMqttToken token = client.connect();
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // We are connected
                    Log.d("file", "onSuccess")
                    //publish(client,"payloadd");
                    subscribe(mqttClient, MQTT_TOPIC_HUMIDITY_LEVEL)
                    subscribe(mqttClient, MQTT_TOPIC_WATER_PUMP_STATUS)
                    subscribe(mqttClient, MQTT_TOPIC_AUTOWATERING_STATUS)

                    publish(mqttClient,MQTT_TOPIC_GET_WATER_PUMP_STATUS,"")
                    publish(mqttClient,MQTT_TOPIC_HUMIDITY_LEVEL_CMD,"")
                    publish(mqttClient,MQTT_TOPIC_AUTOWATERING_CMD,"status")

                    mqttClient.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable) {
                            Snackbar.make(plantStatusIv.rootView, "Connectio Lost", Snackbar.LENGTH_SHORT).show()
                        }

                        @Throws(Exception::class)
                        override fun messageArrived(topic: String, message: MqttMessage) {
                            Log.d("file", "TOPIC: ${topic} MSG:${message.toString()}")
                            if (topic == MQTT_TOPIC_HUMIDITY_LEVEL) {
//                                vibrator.vibrate(100)
                                updatePlantStatus(message.toString().toInt())
                            } else if (topic == MQTT_TOPIC_WATER_PUMP_STATUS) {
                                if (message.toString().equals("1")) {
//                                    isWaterPumpRunning = true;
                                    isWaterPumpOn(true)

                                } else {
//                                    isWaterPumpRunning = false;
                                    isWaterPumpOn(false)
                                }

                                Log.d("file", "Water pump status: ${message.toString()}")
                            }else if(topic == MQTT_TOPIC_AUTOWATERING_STATUS){
                                if (message.toString().equals("1")) {
                                    isAutoWateringModeOn(true)
                                }else{
                                    isAutoWateringModeOn(false)

                                }
                            }


                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken) {
                            Log.d("file", "delivry completed ${token.toString()}")
                        }
                    })
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("file", "onFailure")

                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun isAutoWateringModeOn(isOn: Boolean) {
        if(isOn){
            root.setBackgroundColor(ContextCompat.getColor(this,R.color.black))
            isAutoModeEnabled = true
        }else{
            root.setBackgroundColor(ContextCompat.getColor(this,R.color.white))
            isAutoModeEnabled = false
        }

    }

    private fun isWaterPumpOn(isOn:Boolean){
        if(isOn){
            pumpControl.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.waterbutton))
        }else{
            pumpControl.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.nowater))
        }
    }
    fun publish(client: MqttAndroidClient,topic:String, payload: String) {
        if (client.isConnected) {
            var encodedPayload = ByteArray(0)
            try {
                encodedPayload = payload.toByteArray(charset("UTF-8"))
                val message = MqttMessage(encodedPayload)
                client.publish(topic, message)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }


    fun subscribe(client: MqttAndroidClient, topic: String) {
        val qos = 1
        try {
            val subToken = client.subscribe(topic, qos)
            subToken.actionCallback = object : IMqttActionListener {

                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // The message was published
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }
}
