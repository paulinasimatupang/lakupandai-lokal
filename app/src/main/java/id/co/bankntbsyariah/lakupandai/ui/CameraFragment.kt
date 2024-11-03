package id.co.bankntbsyariah.lakupandai.ui

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import id.co.bankntbsyariah.lakupandai.R
import java.io.File
import java.io.FileOutputStream

class CameraFragment : Fragment() {

    private lateinit var imageViewPreview: ImageView
    private lateinit var titleCapture: TextView
    private var photoType: String? = null
    private var nikValue: String? = null
    var capturedFile: File? = null
    private var photoUri: Uri? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_CODE = 101
        private const val ARG_PHOTO_TYPE = "photo_type"

        fun newInstance(photoType: String, nikValue: String?): CameraFragment {
            val fragment = CameraFragment()
            val args = Bundle().apply {
                putString(ARG_PHOTO_TYPE, photoType)
                putString("nikValue", nikValue)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoType = it.getString(ARG_PHOTO_TYPE)
            nikValue = it.getString("nikValue")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        imageViewPreview = view.findViewById(R.id.imageViewPreview)
        val buttonCapture = view.findViewById<Button>(R.id.buttonCapture)
        titleCapture = view.findViewById(R.id.JudulFoto)

        // Set title and button text based on photo type
        titleCapture.text = if (photoType == "SIG02") "Foto Nasabah" else "Foto KTP"
        buttonCapture.text = if (photoType == "SIG02") "Ambil Gambar Nasabah" else "Ambil Gambar KTP"

        buttonCapture.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        return view
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun openCamera() {
        val fileName = if (photoType == "SIG02") {
            "FOTO_${nikValue ?: "unknown"}.png"
        } else {
            "KTP_${nikValue ?: "unknown"}.png"
        }

        capturedFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
        photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", capturedFile!!)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (capturedFile != null && capturedFile!!.exists()) {
                val fileSizeInBytes = capturedFile!!.length()
                val maxSizeInBytes = 2 * 1024 * 1024 // 2 MB
                Log.d("CameraFragment", "Captured photo size: ${fileSizeInBytes / 1024} KB")

                if (fileSizeInBytes > maxSizeInBytes) {
                    // Compress the image
                    compressImage(capturedFile!!)
                } else {
                    Log.d("CameraFragment", "Photo size is within the limit.")
                }

                val capturedFileSizeInBytes = capturedFile!!.length()
                Log.d("CameraFragment", "CAPTURED NOW photo size: ${capturedFileSizeInBytes / 1024} KB")

                // Load and display the image
                val bitmap = BitmapFactory.decodeFile(capturedFile!!.absolutePath)
                imageViewPreview.setImageBitmap(bitmap)
                Log.d("CameraFragment", "Photo saved and displayed: ${capturedFile!!.absolutePath}")
            } else {
                Log.e("CameraFragment", "Photo file not found or inaccessible")
                Toast.makeText(context, "Failed to access photo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("CameraFragment", "Photo capture failed or canceled")
        }
    }

    private fun compressImage(file: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            // Calculate the desired width and height
            val maxWidth = 800 // Set a max width
            val maxHeight = 800 // Set a max height
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

            var width = bitmap.width
            var height = bitmap.height

            if (width > height) {
                width = maxWidth
                height = (maxWidth / aspectRatio).toInt()
            } else {
                height = maxHeight
                width = (maxHeight * aspectRatio).toInt()
            }

            // Create a resized bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

            // Save the resized bitmap, overwriting the original file
            FileOutputStream(file).use { out ->  // Use the original file path
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // PNG compression is lossless
            }

            // Log the size of the compressed file
            val compressedFileSizeInBytes = file.length()
            Log.d("CameraFragment", "Compressed photo size: ${compressedFileSizeInBytes / 1024} KB")

            // Recycle the bitmaps to free memory
            bitmap.recycle()
            resizedBitmap.recycle()
        } catch (e: Exception) {
            Log.e("CameraFragment", "Failed to compress image: ${e.localizedMessage}")
        }
    }

    private fun saveImageToFile(bitmap: Bitmap?, fileName: String): File? {
        return try {
            val file = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                fileName
            )
            FileOutputStream(file).use { out ->
                bitmap?.compress(Bitmap.CompressFormat.PNG, 85, out)
            }
            Log.d("CameraFragment", "File disimpan: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("CameraFragment", "Gagal menyimpan file: ${e.localizedMessage}")
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }
}
