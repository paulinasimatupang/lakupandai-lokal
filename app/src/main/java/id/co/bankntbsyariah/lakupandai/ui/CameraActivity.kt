package id.co.bankntbsyariah.lakupandai.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import id.co.bankntbsyariah.lakupandai.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException

class CameraActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 1001
    private val CAMERA_PERMISSION_CODE = 1002
    private lateinit var imageView: ImageView
    private lateinit var buttonCapture: Button
    private lateinit var buttonSave: Button
    private var photo: Bitmap? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val photoType = intent.getStringExtra("photo_type")
        supportActionBar?.title = if (photoType == "KTP") "Ambil Foto KTP" else "Ambil Foto Orang"

        imageView = findViewById(R.id.imageView)
        buttonCapture = findViewById(R.id.buttonCapture)
        buttonSave = findViewById(R.id.buttonSave)

        buttonCapture.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        buttonSave.setOnClickListener {
            photo?.let {
                uploadImage(it, photoType ?: "unknown")
            } ?: run {
                Toast.makeText(this, "No photo to save!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            Log.e("CameraActivity", "No camera app found")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Log.e("CameraActivity", "Camera permission denied")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            photo = data?.extras?.get("data") as? Bitmap
            if (photo != null) {
                imageView.setImageBitmap(photo)
            } else {
                Log.e("CameraActivity", "Failed to get photo")
            }
        }
    }

    private fun uploadImage(bitmap: Bitmap, photoType: String) {
        // URL endpoint tetap sama
        val url = "http://108.137.154.8:8081/ARRest/images"
        val client = OkHttpClient()

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        val fileName = if (photoType == "KTP") "foto_ktp.jpg" else "foto_orang.jpg"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image", fileName,
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
                Log.e("CameraActivity", "Failed to upload image", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CameraActivity, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CameraActivity, "Failed to upload image. Server response: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
                Log.d("CameraActivity", "Server Response: $responseBody")
            }
        })
    }
}
