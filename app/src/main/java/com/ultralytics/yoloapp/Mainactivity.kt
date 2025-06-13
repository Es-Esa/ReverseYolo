// In: /app/src/main/java/com/ultralytics/yoloapp/MainActivity.kt
package com.ultralytics.yoloapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ultralytics.yoloapp.yolo.YoloView

class MainActivity : AppCompatActivity() {

    private lateinit var yoloView: YoloView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        yoloView = findViewById(R.id.yolo_view)
        yoloView.setLifecycleOwner(this)
        
        findViewById<Button>(R.id.switch_camera_button).setOnClickListener {
            yoloView.switchCamera()
        }

        if (allPermissionsGranted()) {
            yoloView.setModel("models/yolov8n.tflite")
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                yoloView.setModel("models/yolov8n.tflite")
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}