package demo.maintenance.mqtt_plant_controller

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.ArCoreApk
import com.squareup.moshi.Moshi
import demo.maintenance.mqtt_plant_controller.ar.AugmentedImageActivity
import demo.maintenance.mqtt_plant_controller.ar.SceneformActivity
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.UnsupportedEncodingException


class MainActivity : AppCompatActivity() {

    val MIN_OPENGL_VERSION = 3.0
    val MQTT_SERVER_ADDRESS = "m24.cloudmqtt.com"
    val MQTT_USER = "vlfnelyd"
    val MQTT_PASS = "nXE1FVBVSJHn"
    val MQTT_PORT = 17236

    val MQTT_TOPIC_WATER_PUMP = "waterPump"
    val MQTT_TOPIC_WATER_PUMP_CMD = "waterPump"
    val MQTT_TOPIC_GET_WATER_PUMP_STATUS = "getWaterPumpStatus"
    val MQTT_TOPIC_WATER_PUMP_STATUS = "sendWaterPumpStatus"
    val MQTT_TOPIC_HUMIDITY_LEVEL = "hmdtLevel"
    val MQTT_TOPIC_HUMIDITY_LEVEL_CMD = "hmdtLevelCmd"

    val MQTT_TOPIC_AUTOWATERING_CMD = "autoWateringCmd"
    val MQTT_TOPIC_AUTOWATERING_STATUS = "autoWateringStatus"
    val MQTT_TOPIC_CURRENT_STATE = "currentState"

    val MQTT_CMD_START = "start"
    val MQTT_CMD_STOP = "stop"
    val MQTT_CMD_STATUS = "status"

    var isAutoModeEnabled = false
    val mqttClientId = MqttClient.generateClientId()
    lateinit var mqttClient: MqttAndroidClient
    //    lateinit var vibrator: Vibrator
    lateinit var moshi: Moshi


    val FABRIC_EVENT_PUMP_STATE_CHANGED = "Pump state changed"
    val FABRIC_EVENT_ATTRIBUTE_HMDT_RAW_VALUE = "Pump state changed"
    val FABRIC_EVENT_ATTRIBUTE_HMDT_PERCENTAGE_VALUE = "Pump state changed"
    val FABRIC_EVENT_ATTRIBUTE_WTER_PUMP_STATUS = "Pump state changed"
    val FABRIC_EVENT_ATTRIBUTE_AUTO_WATERING_MODE_STATUS = "Pump state changed"

    var sendEventToFabric = false
    var mqttDeviceState =
        MqttDeviceState(hmdtPercentageValue = 0, hmdtRawValue = 0, waterPumpStatus = 0, autoWateringModeStatus = 0)


    //TODO connect/disconnect on button
//TODO assign id to the client
//TODO get connected clients
//TODO service to receive updates and create notifications

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        maybeEnableArButton();
        Fabric.with(this, Crashlytics())
        moshi = Moshi.Builder()
            .build()
        mqttClient = MqttAndroidClient(
            this.applicationContext, "tcp://${MQTT_SERVER_ADDRESS}:${MQTT_PORT}",
            mqttClientId
        )
        //vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        logo.setOnClickListener { view ->
            if (isAutoModeEnabled) {
                publish(mqttClient, MQTT_TOPIC_AUTOWATERING_CMD, MQTT_CMD_STOP)
                isAutoModeEnabled = false
                Log.d("file", "${MQTT_TOPIC_AUTOWATERING_CMD}: MQTT_CMD_STOP")
            } else {
                publish(mqttClient, MQTT_TOPIC_AUTOWATERING_CMD, MQTT_CMD_START)
                isAutoModeEnabled = true
                Log.d("file", "${MQTT_TOPIC_AUTOWATERING_CMD}: MQTT_CMD_START")
            }

        }

        plantStatusIv.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                val action = event?.action
                if (action == MotionEvent.ACTION_DOWN) {
                    publish(mqttClient, MQTT_TOPIC_WATER_PUMP, MQTT_CMD_START)
//                    isWaterPumpRunning = false
                    Log.d("file", "${MQTT_TOPIC_WATER_PUMP}: MQTT_CMD_START")
                    val pattern = longArrayOf(0, 500, 100)
                    //vibrator.vibrate(pattern, 0)
//                    isWaterPumpOn(true)
                    Log.d("file", "Sending Event on water pump start")
                    Answers.getInstance().logCustom(
                        CustomEvent(FABRIC_EVENT_PUMP_STATE_CHANGED)
                            .putCustomAttribute(FABRIC_EVENT_ATTRIBUTE_HMDT_RAW_VALUE, mqttDeviceState.hmdtRawValue)
                            .putCustomAttribute(
                                FABRIC_EVENT_ATTRIBUTE_HMDT_PERCENTAGE_VALUE,
                                mqttDeviceState.hmdtPercentageValue
                            )
                            .putCustomAttribute(
                                FABRIC_EVENT_ATTRIBUTE_WTER_PUMP_STATUS,
                                mqttDeviceState.waterPumpStatus
                            )
                            .putCustomAttribute(
                                FABRIC_EVENT_ATTRIBUTE_AUTO_WATERING_MODE_STATUS,
                                mqttDeviceState.autoWateringModeStatus
                            )
                    )
                } else if (action == MotionEvent.ACTION_UP) {
                    publish(mqttClient, MQTT_TOPIC_WATER_PUMP, MQTT_CMD_STOP)
//                    isWaterPumpRunning = true
                    sendEventToFabric = true
                    Log.d("file", "${MQTT_TOPIC_WATER_PUMP}: MQTT_CMD_STOP")
                    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    //vibrator.cancel()
//                    isWaterPumpOn(false)
                } else if (action == MotionEvent.ACTION_MOVE) {
                    Log.d("file", "ACTION MOVE")
//                    timeCounter = 0
//                    getCurrentState()
                }
                return true
            }
        }
        )
    }


    override fun onResume() {
        super.onResume()
        if (!mqttClient.isConnected) {
            Log.d("file", "connecting to mqtt")
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
//                    subscribe(mqttClient, MQTT_TOPIC_HUMIDITY_LEVEL)
//                    subscribe(mqttClient, MQTT_TOPIC_WATER_PUMP_STATUS)
//                    subscribe(mqttClient, MQTT_TOPIC_AUTOWATERING_STATUS)
                    subscribe(mqttClient, MQTT_TOPIC_CURRENT_STATE)

//                    publish(mqttClient, MQTT_TOPIC_GET_WATER_PUMP_STATUS, "")
//                    publish(mqttClient, MQTT_TOPIC_HUMIDITY_LEVEL_CMD, "")
//                    publish(mqttClient, MQTT_TOPIC_AUTOWATERING_CMD, "status")
                    getCurrentState()
                    mqttClient.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable) {
                            Snackbar.make(plantStatusIv.rootView, "Connection Lost", Snackbar.LENGTH_SHORT).show()
                        }

                        @Throws(Exception::class)
                        override fun messageArrived(topic: String, message: MqttMessage) {
                            Log.d("file", "TOPIC: ${topic} MSG:${message.toString()}")

                            if (topic == MQTT_TOPIC_CURRENT_STATE) {
                                val jsonAdapter = moshi.adapter<MqttDeviceState>(MqttDeviceState::class.java)
                                val updatedState = jsonAdapter.fromJson(message.toString())
                                Log.d("file", "received state: ${updatedState.toString()}")
                                if (updatedState != null) {
                                    isAutoWateringModeOn(updatedState)
                                    isWaterPumpOn(updatedState)
                                    updatePlantStatus(updatedState)
                                    mqttDeviceState = updatedState
//                                    waitingForState = false
                                }

                            }
//                            if (topic == MQTT_TOPIC_HUMIDITY_LEVEL) {
////                                vibrator.vibrate(100)
//                                updatePlantStatus(message.toString().toInt())
//                            } else if (topic == MQTT_TOPIC_WATER_PUMP_STATUS) {
//                                if (message.toString().equals("1")) {
////                                    isWaterPumpRunning = true;
//                                    isWaterPumpOn(true)
//
//                                } else {
////                                    isWaterPumpRunning = false;
//                                    isWaterPumpOn(false)
//                                }
//
//                                Log.d("file", "Water pump status: ${message.toString()}")
//                            } else if (topic == MQTT_TOPIC_AUTOWATERING_STATUS) {
//                                if (message.toString().equals("1")) {
//                                    isAutoWateringModeOn(true)
//                                } else {
//                                    isAutoWateringModeOn(false)
//
//                                }
//                            }


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

    private fun getCurrentState() {
//        waitingForState = true
        publish(mqttClient, MQTT_TOPIC_CURRENT_STATE, MQTT_CMD_STATUS)

    }

    private fun updatePlantStatus(updatedState: MqttDeviceState) {
        if (updatedState.hmdtPercentageValue <= 25) {
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.plantstate0))
        } else if (updatedState.hmdtPercentageValue > 25 && updatedState.hmdtPercentageValue <= 50) {
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.plantstate1))
        } else if (updatedState.hmdtPercentageValue > 50 && updatedState.hmdtPercentageValue <= 80) {
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.plantstate2))
        } else {
            plantStatusIv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.plantstate3))

        }
    }


    private fun isAutoWateringModeOn(updatedState: MqttDeviceState) {
        if (updatedState.autoWateringModeStatus != mqttDeviceState.autoWateringModeStatus) {
            if (updatedState.autoWateringModeStatus == 1) {
                Log.d("file", "updating watering mode with: ${updatedState.autoWateringModeStatus}")
                root.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
            } else {
                Log.d("file", "updating watering mode with: ${updatedState.autoWateringModeStatus}")
                root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))

            }
        }

    }

    private fun isWaterPumpOn(updatedState: MqttDeviceState) {
        if (updatedState.waterPumpStatus != mqttDeviceState.waterPumpStatus) {
            if (updatedState.waterPumpStatus == 1) {
                Log.d("file", "updating water pump state with: ${updatedState.waterPumpStatus}")
                pumpControlOn.visibility = View.VISIBLE
                pumpControlOff.visibility = View.GONE
                val pattern = longArrayOf(0, 200, 500)
//                vibrator.cancel()
//                vibrator.vibrate(pattern, 0)
            } else {
                Log.d("file", "updating water pump state with: ${updatedState.waterPumpStatus}")
                pumpControlOn.visibility = View.GONE
                pumpControlOff.visibility = View.VISIBLE
                if (sendEventToFabric) {
                    Log.d("file", "Sending Event on water pump stop")
                    Answers.getInstance().logCustom(
                        CustomEvent(FABRIC_EVENT_PUMP_STATE_CHANGED)
                            .putCustomAttribute(FABRIC_EVENT_ATTRIBUTE_HMDT_RAW_VALUE, updatedState.hmdtRawValue)
                            .putCustomAttribute(
                                FABRIC_EVENT_ATTRIBUTE_HMDT_PERCENTAGE_VALUE,
                                updatedState.hmdtPercentageValue
                            )
                            .putCustomAttribute(FABRIC_EVENT_ATTRIBUTE_WTER_PUMP_STATUS, updatedState.waterPumpStatus)
                            .putCustomAttribute(
                                FABRIC_EVENT_ATTRIBUTE_AUTO_WATERING_MODE_STATUS,
                                updatedState.autoWateringModeStatus
                            )
                    )
                    sendEventToFabric = false
                }
            }
        }
    }

    fun publish(client: MqttAndroidClient, topic: String, payload: String) {
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

    private fun crashlytics() {
        // Set up Crashlytics, disabled for debug builds
        val crashlyticsKit = Crashlytics.Builder()
            .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
            .build()

// Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit)
    }

    fun viewGraphs(view: View) {
        val intent = Intent(this, GraphActivityJava::class.java)
        startActivity(intent)
    }

    fun maybeEnableArButton() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Re-query at 5Hz while compatibility is checked in the background.
            Handler().postDelayed(Runnable { maybeEnableArButton() }, 200)
        }
        val openGlVersionString = (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
        if (availability.isSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && openGlVersionString.toDouble() >= MIN_OPENGL_VERSION
        ) {
            btn_ar.setEnabled(true)
        } else {
            // Unsupported or unknown.
            btn_ar.setEnabled(false)
        }
    }

    fun openArView(view: View) {
        val intent = Intent(this, SceneformActivity::class.java)
        startActivity(intent)
    }

}
