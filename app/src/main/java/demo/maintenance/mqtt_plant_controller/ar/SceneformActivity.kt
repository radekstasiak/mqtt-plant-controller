package demo.maintenance.mqtt_plant_controller.ar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.ar.sceneform.ux.ArFragment
import demo.maintenance.mqtt_plant_controller.R

class SceneformActivity : AppCompatActivity() {

    lateinit var fragment: ArFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sceneform)

        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
    }
}
