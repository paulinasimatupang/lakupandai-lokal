package id.co.bankntbsyariah.lakupandai.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import kotlinx.coroutines.*
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.gcacace.signaturepad.views.SignaturePad
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Component
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.common.Mutation
import id.co.bankntbsyariah.lakupandai.common.Screen
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.iface.StorageImpl
import id.co.bankntbsyariah.lakupandai.ui.adapter.MutationAdapter
import id.co.bankntbsyariah.lakupandai.utils.ScreenParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Environment
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.InputType
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import id.co.bankntbsyariah.lakupandai.iface.WebCallerImpl
import okhttp3.*
import android.widget.EditText
import org.json.JSONArray
import java.text.ParseException
import android.text.Spannable
import android.text.style.StyleSpan
import android.graphics.Color
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import androidx.recyclerview.widget.GridLayoutManager
import id.co.bankntbsyariah.lakupandai.common.CompValues
import id.co.bankntbsyariah.lakupandai.common.ComponentValue
import id.co.bankntbsyariah.lakupandai.ui.adapter.ProdukAdapter
import id.co.bankntbsyariah.lakupandai.local.NotificationDatabaseHelper
import com.google.firebase.messaging.FirebaseMessaging
import id.co.bankntbsyariah.lakupandai.MyFirebaseMessagingService
import okhttp3.RequestBody.Companion.asRequestBody
import android.media.MediaDrm
import android.text.InputFilter
import android.view.Gravity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentContainerView
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor
import java.security.MessageDigest
import java.util.UUID
import com.gu.toolargetool.TooLargeTool


class FormActivity : AppCompatActivity() {

    private var formId = Constants.DEFAULT_ROOT_ID
    private val inputValues = mutableMapOf<String, String>()
    private var msg03Value: String? = null
    private var isOtpValidated = false
    private var otpDialog: AlertDialog? = null
    private var nikValue: String? = null
    private var nominalValue = 0.0
    private var feeValue = 0.0
    private var inputRekening = mutableMapOf<String, String>()
    private var pickOTP: String? = null
    private var pickNorek: String? = null
    private var pickNRK: String? = null
    private val CAMERA_REQUEST_CODE = 1001
    private val CAMERA_PERMISSION_CODE = 1002
    private var photo: Bitmap? = null
    private lateinit var imageViewKTP: ImageView
    private lateinit var imageViewOrang: ImageView
    private var photoCounter: Int = 0
    private var currentImageView: ImageView? = null
    private var signatureFile: File? = null
    private var fileFotoKTP: File? = null
    private var fileFotoOrang: File? = null
    private val msgIdMap = mutableMapOf<String, MutableList<String>>()
    private val countLimitMap = mutableMapOf<String, Int>()
    private var isReversal = false
    private var isSvcBiller = false
    val valuesKabKot = ArrayList<String>()
    var currentLabelCIF13: TextView? = null
    var currentSpinnerCIF13: Spinner? = null
    private var countDownTimer: CountDownTimer? = null

    data class Token(val token: String)

    private var otpTimer: CountDownTimer? = null
    private var lastMessageBody: JSONObject? = null
    private var otpAttempts = mutableListOf<Long>()
    private val otpCooldownTime = 3 * 60 * 1000L // 30 minutes in milliseconds
    private var otpMessage: String? = null
    private var remainingMillis: Long = 0
    private var okButtonPressCount = 0
    lateinit var timerTextView: TextView
    lateinit var resendOtpTextView: TextView

    private lateinit var executor: Executor
    private val webCallerImpl = WebCallerImpl()
    private var lottieLoading: LottieAnimationView? = null
    private var loadingOverlay: View? = null
    private var processedTypes = mutableSetOf<String>()
    private var msgDtCreateRekening: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve formId from intent extras
        formId = intent.extras?.getString(Constants.KEY_FORM_ID) ?: Constants.DEFAULT_ROOT_ID
        Log.d("FormActivity", "formId: $formId")

        val imageViewKTP = findViewById<ImageView?>(R.id.imageViewKTP)
        if (imageViewKTP != null) {
            this.imageViewKTP = imageViewKTP
        }

        val imageViewOrang = findViewById<ImageView?>(R.id.imageViewOrang)
        if (imageViewOrang != null) {
            this.imageViewOrang = imageViewOrang
        }

        setInitialLayout()

        setupWindowInsets()

        handleBackPress()

        initLoginRegisterUI()

        lifecycleScope.launch {
            val formValue = loadForm()
            setupScreen(formValue)
        }

        findViewById<Button>(R.id.fingerprintRegisterButton).setOnClickListener {
            registerFingerprint(it)
        }

        findViewById<TextView>(R.id.forgot_password)?.setOnClickListener {
            val intent = Intent(this, FormActivity::class.java).apply {
                putExtra(Constants.KEY_FORM_ID, "LPW0000")
            }
            startActivity(intent)
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            navigateToLoginActivity()
            throwable.printStackTrace()

            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
//        val uri = intent?.data
//
//        uri?.let {
//            val exampleParam = it.getQueryParameter("example_param")
//            if (exampleParam != null) {
//                Log.d("FormActivity", "Received parameter: $exampleParam")
//            }
//        }
    }

    private fun navigateToLoginActivity() {
        finish()
        startActivity(Intent(this, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, "AU00001")
        })
    }

    private fun initLoginRegisterUI() {
        setContentView(R.layout.activity_form_login)
        executor = ContextCompat.getMainExecutor(this)

        if (isFingerprintSupported()) {
            findViewById<Button>(R.id.fingerprintLoginButton).visibility = View.VISIBLE
            findViewById<Button>(R.id.fingerprintLoginButton).setOnClickListener {
                loginFingerprint(it)
            }
        } else {
            Log.d("FormActivity", "Fingerprint not supported")
        }
    }

    fun loginFingerprint(view: View) {
        authenticateFingerprintForLogin { fingerprintData ->
            if (fingerprintData != null) {
                Log.d("FormActivity", "Fingerprint data received: $fingerprintData")
                loginWithFingerprint(fingerprintData)
            } else {
                Log.e("FormActivity", "Fingerprint data is null")
                Toast.makeText(this, "Failed to retrieve fingerprint data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun authenticateFingerprintForLogin(callback: (String?) -> Unit) {
        if (!isFingerprintSupported()) {
            Toast.makeText(this, "Fingerprint authentication not supported.", Toast.LENGTH_SHORT).show()
            return
        }

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val hashedFingerprint = hashFingerprintData(result.cryptoObject?.signature?.toString() ?: "")
                callback(hashedFingerprint)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.e("BiometricAuth", "Authentication failed")
                Toast.makeText(this@FormActivity, "Authentication failed, please try again.", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Use your fingerprint to login")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun hashFingerprintData(fingerprintData: String): String {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val dataToHash = fingerprintData + deviceId // Combine fingerprint data with device ID
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(dataToHash.toByteArray())
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    private fun authenticateFingerprint(callback: (String?) -> Unit) {
        if (!isFingerprintSupported()) {
            Toast.makeText(this, "Fingerprint authentication not supported.", Toast.LENGTH_SHORT).show()
            return
        }

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val userId = getUserIdFromSession()
                    val hashedFingerprint = hashFingerprintData(result.cryptoObject?.signature?.toString() ?: "")
                    callback(hashedFingerprint)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e("BiometricAuth", "Authentication failed")
                    Toast.makeText(this@FormActivity, "Authentication failed, please try again.", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Use your fingerprint to authenticate")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun isFingerprintSupported(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun generateMsgUi(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    private fun loginWithFingerprint(fingerprintData: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val hashedFingerprint = hashFingerprintData(fingerprintData)
            Log.d("FingerprintLogin", "Hashed fingerprint for login: $hashedFingerprint")

            val requestBody = FormBody.Builder()
                .add("finger_print", hashedFingerprint)
                .add("msg_ui", generateMsgUi())
                .build()

            val request = Request.Builder()
                .url("http://reportntbs.selada.id/api/auth/loginWithFingerprint")
                .post(requestBody)
                .build()

            try {
                Log.d("FingerprintLogin", "Sending login request with fingerprint data...")
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("FingerprintLogin", "Raw response body: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val status = jsonResponse.optBoolean("status", false)

                    if (status) {
                        val token = jsonResponse.optString("token")
                        val user = jsonResponse.optJSONObject("data")

                        if (user != null) {
                            withContext(Dispatchers.Main) {
                                saveUserSession(user, token)

                                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                val terminalImei = sharedPreferences.getString("imei", "") ?: ""
                                Log.d(TAG, "IMEI Login Finger: $terminalImei")

                                if (terminalImei.isNotEmpty()) {
                                    Toast.makeText(this@FormActivity, "Login berhasil dengan sidik jari", Toast.LENGTH_SHORT).show()
                                    navigateToScreen()
                                } else {
                                    Log.e(TAG, "IMEI not found in SharedPreferences")
                                    Toast.makeText(this@FormActivity, "IMEI tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Log.e("FingerprintLogin", "User data is null")
                        }
                    } else {
                        Log.e("FingerprintLogin", "Login failed with status false")
                        handleFailedLoginAttempt(responseBody)
                    }
                } else {
                    Log.e("FingerprintLogin", "Login failed with response code: ${response.code}")
                    handleFailedLoginAttempt(responseBody)
                }
            } catch (e: Exception) {
                Log.e("FingerprintLogin", "Error during login: ${e.message}")
            }
        }
    }

    private fun registerFingerprintWithServer(userId: String, fingerprintData: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val hashedFingerprint = hashFingerprintData(fingerprintData)
            Log.d("FingerprintRegister", "Hashed fingerprint for registration: $hashedFingerprint")

            val requestBody = FormBody.Builder()
                .add("id", userId)
                .add("finger_print", hashedFingerprint)
                .build()

            val request = Request.Builder()
                .url("http://reportntbs.selada.id/api/auth/registerFingerprint")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    handleFailedRegistration(response.body?.string())
                    return@launch
                }

                val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                val status = jsonResponse.optBoolean("status", false)
                if (status) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Fingerprint registered successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleFailedRegistration(jsonResponse.toString())
                }
            } catch (e: Exception) {
                Log.e("FingerprintRegister", "Error: ${e.message}")
            }
        }
    }

    fun registerFingerprint(view: View) {
        val userId = getUserIdFromSession()
        if (userId != null) {
            authenticateFingerprint { fingerprintData ->
                if (fingerprintData != null) {
                    registerFingerprintWithServer(userId, fingerprintData)
                    Log.e("FingerprintRegist", "Fingerprint registration value: $fingerprintData")
                } else {
                    Toast.makeText(this, "Fingerprint data is null", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "You must be logged in to register a fingerprint", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserIdFromSession(): String? {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("id", null)
    }

    private fun handleFailedLoginAttempt(responseData: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            val jsonResponse = JSONObject(responseData ?: "{}")
            val errorMessage = jsonResponse.optString("message", "Login failed.")
            Log.e("LoginAttempt", errorMessage)
            Toast.makeText(this@FormActivity, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFailedRegistration(responseData: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            val jsonResponse = JSONObject(responseData ?: "{}")
            val errorMessage = jsonResponse.optString("message", "Registration failed.")
            Toast.makeText(this@FormActivity, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserSession(user: JSONObject, token: String) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("username", user.optString("username", "Unknown"))
        editor.putString("token", token)
        editor.putString("fullname", user.optString("fullname", "Unknown"))
        editor.putString("id", user.optString("id", "Unknown"))
        editor.putString("branchid", user.optString("branchid", "Unknown"))

        val merchantData = user.optJSONObject("merchant")
        val terminalArray = merchantData?.optJSONArray("terminal")

        if (merchantData != null) {
            editor.putString("merchant_id", merchantData.optString("id", "Unknown"))
            editor.putString("merchant_name", merchantData.optString("name", "Unknown"))
            editor.putString("norekening", merchantData.optString("no", "Unknown"))
            editor.putString("merchant_code", merchantData.optString("code", "Unknown"))
            editor.putString("merchant_address", merchantData.optString("address", "Unknown"))
            editor.putString("merchant_phone", merchantData.optString("phone", "Unknown"))
            editor.putString("merchant_email", merchantData.optString("email", "Unknown"))
            editor.putString("merchant_balance", merchantData.optString("balance", "0"))
            editor.putString("merchant_avatar", merchantData.optString("avatar", "Unknown"))
            editor.putInt("merchant_status", merchantData.optInt("status", 0))
            editor.putString("kode_agen", merchantData.optString("mid"))
            editor.putString("pin", merchantData.optString("pin"))
            editor.putString("mid", merchantData.optString("mid"))
        }

        val terminalData = terminalArray?.getJSONObject(0)
        if (terminalData != null) {
            val terminalImei = terminalData.optString("imei", "Unknown")
            editor.putString("imei", terminalImei)
            Log.d(TAG, "IMEI Finger: $terminalImei")
        }

        editor.apply()
        Log.d(TAG, "IMEI saved in SharedPreferences: ${sharedPreferences.getString("imei", "null")}")
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (formId == "AU00001") {
                    // If formId is "AU000001", do not navigate to MenuActivity
                    finish()
                }else if(formId == "LPW0000"){
                    navigateToLogin()
                }else {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.remove("norek_biller")
                    editor.remove("namarek_biller")
                    editor.remove("nomor_biller")
                    editor.apply()
                    finish()
                    startActivity(
                        Intent(this@FormActivity, MenuActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
            }
        })
    }

    private suspend fun loadForm(): String? {
        var formValue = StorageImpl(applicationContext).fetchForm(formId)
        if (formValue.isNullOrEmpty()) {
            formValue = withContext(Dispatchers.IO) {
                ArrestCallerImpl(OkHttpClient()).fetchScreen(formId)
            }
            Log.i("FormActivity", "Fetched formValue: $formValue")
        }
        return formValue
    }

    private fun setupScreen(formValue: String?) {
        if (formValue.isNullOrEmpty()) {
            findViewById<TextView>(R.id.error_message).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.menu_container).visibility = View.GONE
        } else {
            val screenJson = JSONObject(formValue)
            val screen: Screen = ScreenParser.parseJSON(screenJson)
            msg03Value = extractMsg03Value(screenJson)
            handleScreenType(screen)
        }
    }

    private fun extractMsg03Value(screenJson: JSONObject): String? {
        return try {
            if (screenJson.has("comps")) {
                screenJson.getJSONObject("comps")
                    .getJSONArray("comp")
                    .let { compsArray ->
                        (0 until compsArray.length()).map { i ->
                            compsArray.getJSONObject(i)
                        }
                    }
                    .find { it.getString("comp_id") == "MSG03" }
                    ?.getJSONObject("comp_values")
                    ?.getJSONArray("comp_value")
                    ?.getJSONObject(0)
                    ?.getString("value")
            } else {
                null
            }
        } catch (e: JSONException) {
            Log.e("FormActivity", "JSONException occurred: ${e.message}")
            null
        }
    }

    private fun handleScreenType(screen: Screen) {
        when (screen.type) {
            Constants.SCREEN_TYPE_MENU -> {
                // Move to menu
            }

            Constants.SCREEN_TYPE_POPUP_GAGAL -> {
                when (screen.id) {
                    "000000F" -> {
                        // Handle failure case
                        val failureMessage = screen.comp.firstOrNull { it.id == "0000A" }
                            ?.compValues?.compValue?.firstOrNull()?.value ?: "Unknown error"
                        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                            putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                            putExtra("MESSAGE_BODY", failureMessage)
                            putExtra("RETURN_TO_ROOT", false)
                        }
                        startActivity(intent)
                    }
                    "PR00011" -> {
                        // Handle failure case
                        val failureMessage = screen.comp.firstOrNull { it.id == "0000B" }
                            ?.compValues?.compValue?.firstOrNull()?.value ?: "Unknown error"
                        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                            putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                            putExtra("MESSAGE_BODY", failureMessage)
                            putExtra("RETURN_TO_ROOT", true)
                        }
                        startActivity(intent)
                    }

                    else -> {
                        // Handle other failure cases if necessary
                    }
                }
            }

            Constants.SCREEN_TYPE_POPUP_SUKSES -> {
                when (screen.id) {
                    "000000D" -> {
                        // Handle success case
                        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                            putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                            putExtra("MESSAGE_BODY", "Operation successful.")
                            putExtra("RETURN_TO_ROOT", false)
                        }
                        startActivity(intent)
                    }

                    else -> {
                        // Handle other success cases if necessary
                    }
                }
            }

            Constants.SCREEN_TYPE_POPUP_LOGOUT -> {
                // Handle logout popup
            }

            Constants.SCREEN_TYPE_POPUP_OTP -> {
                val dialogView = layoutInflater.inflate(R.layout.pop_up, null)
                otpDialog = AlertDialog.Builder(this@FormActivity)
                    .setView(dialogView)
                    .create()

                // Prevent the dialog from being canceled when clicked outside
                otpDialog?.setCanceledOnTouchOutside(false)

                setupForm(screen, dialogView)
                otpDialog?.window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                otpDialog?.show()
            }


            else -> {
                handleScreenTitle(screen.title)
                setupForm(screen)
            }
        }
    }

    private fun getSequenceOptionsForComponent(componentId: String): List<Pair<String, String>> {
        // Replace with actual logic to fetch sequence options based on componentId
        return listOf() // Placeholder
    }

    private fun setInitialLayout() {
        when (formId) {
            "AWL0000" -> {
                setContentView(R.layout.activity_awal)
                Log.d("FormActivity", "Displaying activity_awal")
            }
            "AU00001" -> {
                setContentView(R.layout.activity_form_login)
                Log.d("FormActivity", "Displaying activity_form_login")
            }
            else -> {
                // Optionally handle the case where none of the keywords match
                setContentView(R.layout.activity_form)
                Log.d("FormActivity", "Displaying activity_form")
            }
        }
    }

    private fun handleScreenTitle(screenTitle: String) {
        val layoutId = when {
            screenTitle.contains("Form", ignoreCase = true) -> R.layout.header
            screenTitle.contains("Review", ignoreCase = true) -> R.layout.header
//            screenTitle.contains("Bayar", ignoreCase = true) -> R.layout.activity_bayar
            screenTitle.contains("Pilih", ignoreCase = true) -> R.layout.pilihan_otp
            screenTitle.contains("Transfer", ignoreCase = true) -> R.layout.activity_transfer
            screenTitle.contains("Cek Saldo Berhasil", ignoreCase = true) -> R.layout.activity_saldo
            screenTitle.contains("Lupa PIN", ignoreCase = true) -> R.layout.forgot_pin
            screenTitle.contains("PIN", ignoreCase = true) -> R.layout.screen_pin
            screenTitle.contains("Biometrik", ignoreCase = true) -> R.layout.activity_bio
            screenTitle.contains("Berhasil", ignoreCase = true) ->
            {
                showSuccessPopup(screenTitle)
                R.layout.activity_berhasil
            }
            screenTitle.contains("Gagal", ignoreCase = true) ->
            {
                showFailedPopup(screenTitle)
                R.layout.activity_gagal
            }
            else -> R.layout.activity_form
        }
        if (layoutId != R.layout.activity_form) {
            setContentView(layoutId)
            Log.d("FormActivity", "Displaying layout with ID: $layoutId")
        }

        val textView: TextView? = findViewById(R.id.text_center)

        textView?.let {
            val processedTitle = when {
                screenTitle.contains("Form BL", ignoreCase = true) -> screenTitle.replace("FORM BL", "", ignoreCase = true).trim()
                screenTitle.contains("Form", ignoreCase = true) -> screenTitle.replace("FORM", "", ignoreCase = true).trim()
                screenTitle.contains("Review", ignoreCase = true) -> screenTitle.replace("Review", "", ignoreCase = true).trim()
                screenTitle.contains("Berhasil", ignoreCase = true) -> screenTitle.replace("Berhasil", "", ignoreCase = true).trim()
                screenTitle.contains("Gagal", ignoreCase = true) -> screenTitle.replace("Gagal", "", ignoreCase = true).trim()
                screenTitle.contains("Transfer", ignoreCase = true) -> screenTitle.replace("Transfer", "", ignoreCase = true).trim()
                else -> screenTitle
            }

            it.text = processedTitle
            Log.d("FormActivity", "Processed title: $processedTitle")
        }
    }

    private fun getTransactionTitle(formId: String): String {
        val titleView = findViewById<TextView>(R.id.titleTransaction)
        val title = when (formId) {
            "TF00003" -> {
                getString(R.string.transfer)
            }
            "BS001" -> {
                getString(R.string.setor_tunai)
            }
            else -> {
                getString(R.string.tarik_tunai)
            }
        }
        titleView?.text = title

        return title
    }

    private fun showSuccessPopup(message: String) {
        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
            putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
            putExtra("MESSAGE_BODY", message)
        }
        startActivity(intent)
    }

    private fun showFailedPopup(message: String) {
        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
            putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
            putExtra("MESSAGE_BODY", message)
        }
        startActivity(intent)
    }

    private fun startTimer(timeInMillis: Long): CountDownTimer {
        val countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                timerTextView.text = timeFormatted
            }

            override fun onFinish() {
                remainingMillis = 0
                msg03Value = null
                timerTextView.text = "00:00"
                Log.e("Timer", "Waktu habis. msg03Value di-set null.")
            }
        }

        countDownTimer.start()
        return countDownTimer
    }

    private fun resetTimer(timeInMillis: Long): CountDownTimer {
        remainingMillis = timeInMillis // Atur waktu awal
        timerTextView.text = ""        // Reset tampilan timer

        // Batalkan dan hapus timer sebelumnya jika ada
        countDownTimer?.cancel()

        // Mulai timer baru
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished

                // Format menit dan detik
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                timerTextView.text = timeFormatted

                // Sembunyikan "Resend OTP" saat timer berjalan
                resendOtpTextView.visibility = View.GONE
            }

            override fun onFinish() {
                remainingMillis = 0
                timerTextView.text = "00:00"

                // Tampilkan "Resend OTP" setelah timer habis
                resendOtpTextView.visibility = View.VISIBLE
                Log.e("Time Check", "Waktu habis. msg03Value di-set null. (Reset Timer)")
                msg03Value = null
            }
        }.start()

        return countDownTimer!!
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupForm(screen: Screen, containerView: View? = null) {
        Log.e("FormActivity", "Start FormActivity")
        val container = containerView?.findViewById<LinearLayout>(R.id.menu_container)
            ?: findViewById(R.id.menu_container)
        var buttonContainer = containerView?.findViewById<LinearLayout>(R.id.button_type_7_container) ?: null
        val buttontf = findViewById<LinearLayout>(R.id.button_type_7_container)

        lottieLoading = findViewById(R.id.lottie_loading) ?: null
        loadingOverlay = findViewById(R.id.loading_overlay) ?: null

        if (loadingOverlay == null) {
            Log.e("FormActivity", "No loading overlay (${screen.title})")
        }else{
            Log.e("FormActivity", "Loading overlay (${screen.title})")
        }

        val screenTransfer = mutableListOf<String>()
        if (screen.title.contains("Transfer", ignoreCase = true)) {
            screenTransfer.add(screen.id)
        } else {
            screenTransfer.clear()
        }


        if (container == null) {
            Log.e("FormActivity", "Container is null.")
            return
        }

        container.removeAllViews()
        buttonContainer?.removeAllViews()

        var norekComponent: Component? = null
        var nominalComponent: Component? = null
        var namaDepan: String? = null
        var extraText = ""
        var inputRekeningIndex = 0
        val title = getTransactionTitle(formId)

        for (component in screen.comp) {
            when {
                component.id == "TRF27" ||
                        (component.id == "AG001" && screen.title.contains("Form")) ||
                        component.id == "TRF26" || component.id == "NAG02"  -> {
                    norekComponent = component
                }
                component.id == "TFR24" ||
                        (component.id == "AG002" && screen.title.contains("Form") ||
                                (component.id == "AG005" && screen.title.contains("Form"))) -> {
                    nominalComponent = component
                    val fullName = getComponentValue(component)
                    namaDepan = fullName?.split(" ")?.firstOrNull()?.take(1) ?: ""
                }
            }
            if (screen.title.contains("Transfer") && (component.id == "ST003" || component.id == "STS01")) {
                val transaksiBerhasilTextView = findViewById<TextView>(R.id.success)
                val dateTransferTextView = findViewById<TextView>(R.id.dateTransfer)
                val timeTransferTextView = findViewById<TextView>(R.id.timeTransfer)
                val titleTransactionView = findViewById<TextView>(R.id.titleTransaction)

                transaksiBerhasilTextView?.let {
                    val newText = getComponentValue(component)
                    if (!newText.isNullOrEmpty()) {
                        if(component.id == "STS01"){
                            it.text = "TRANSAKSI BERHASIL"
                        }else{
                            it.text = newText
                        }
                        titleTransactionView?.let { titleView ->
                            if (screen.title.contains("Transfer", ignoreCase = true)) {
                                titleView.text = screen.title.replace("Transfer", "", ignoreCase = true).trim()
                            } else {
                                titleView.text = screen.title
                            }
                        } ?: Log.e("FormActivity", "TextView with ID titleTransaction not found")
                        dateTransferTextView?.text = getCurrentDate()
                        timeTransferTextView?.text = getCurrentTime()
                    } else {
                        Log.e("FormActivity", "Value for component ST003 is null or empty")
                    }
                } ?: run {
                    Log.e("FormActivity", "TextView with ID success not found")
                }
            }

            if (component.id == "TRF27" && component.label.contains("Penerima")) {
                extraText = "Penerima"
            } else if ((component.id == "AG001" || component.id == "TRF26") && component.label.contains(
                    "Agen"
                )
            ) {
                extraText = "Agen"
            }

            if (component.id == "D1004") {  // ID komponen untuk jenis mutasi
                // Dapatkan nilai mutasi asli dari komponen
                val originalMutasiValue =
                    component.compValues?.compValue?.firstOrNull()?.value ?: ""

                // Parse mutasi menjadi list dari objek Mutation
                val mutationList = parseMutasi(originalMutasiValue)

                // Siapkan RecyclerView untuk menampilkan daftar mutasi
                val recyclerView = RecyclerView(this@FormActivity).apply {
                    layoutManager = LinearLayoutManager(this@FormActivity)
                    adapter = MutationAdapter(mutationList)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                    }
                }

                // Tambahkan RecyclerView ke dalam container
                container.addView(recyclerView)
            }
        }

        if (norekComponent != null && nominalComponent != null) {
            val inflater = LayoutInflater.from(this@FormActivity)
            val combinedView = inflater.inflate(R.layout.coba_tf, null).apply {
                findViewById<TextView>(R.id.norekTextView).text = getComponentValue(norekComponent)
                findViewById<TextView>(R.id.nominalTextView).text =
                    getComponentValue(nominalComponent)
                findViewById<TextView>(R.id.namaDepanTextView).text = namaDepan
                findViewById<TextView>(R.id.extraTextViewTop).text = extraText
            }
            container.addView(combinedView)
        }

        Log.d("FormActivity", "Screen components: ${screen.comp}")
        for (component in screen.comp) {
            Log.d("FormActivity", "Component: $component")

            if (component.id == "TRF27" || component.id == "TFR24" || (component.id == "AG001" && screen.title.contains("Form")) || (component.id == "NAG02" && screen.title.contains("Form")) ||
                (component.id == "AG002" && screen.title.contains("Form")) || component.id == "TRF26" || (component.id == "AG005" && screen.title.contains("Form")) ||
                (((component.id == "ST003" || component.id == "STS01")) && screen.title.contains("Transfer")) || component.id == "D1004"
            ) continue

            if (component.id == "MSG03") {
                val value = component.compValues?.compValue?.firstOrNull()?.value
                Log.d("FormActivity", "Value of MSG03: $value")
                msg03Value = value
            } else {
                Log.d("FormActivity", "Component: ${component.id}")
            }

            if (component.visible == false) {
                continue
            }

            fun formatRupiah(amount: Double): String {
                val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                format.minimumFractionDigits = 0
                format.maximumFractionDigits = 0
                return format.format(amount)
            }

            val view = when (component.type) {
                0 -> {
                    val inflater = layoutInflater
                    val view = inflater.inflate(R.layout.recycler_view_menu_item, null)

                    val title = view.findViewById<TextView>(R.id.title)
                    val subhead = view.findViewById<TextView>(R.id.subhead)
                    val body = view.findViewById<TextView>(R.id.body)

                    title.text = component.label
                    title.setTextColor(getColor(R.color.black))
                    subhead.visibility = View.GONE
                    body.visibility = View.GONE

                    view
                }

                1 -> {
                    if (component.id == "TRF27" || component.id == "TFR24") {
                        val inflater = LayoutInflater.from(this@FormActivity)
                        inflater.inflate(R.layout.coba_tf, null).apply {
                            findViewById<TextView>(R.id.norekTextView).text =
                                getComponentValue(component)
                            findViewById<TextView>(R.id.nominalTextView).text =
                                getComponentValue(component)
                        }
                    } else if (component.id == "HR002") {
                        val context = this@FormActivity
                        val layout = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL

                            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())

                            addView(TextView(context).apply {
                                text = component.label
                                textSize = 15f
                                setTypeface(null, Typeface.BOLD)
                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                                setTextColor(Color.parseColor("#0A6E44"))
                            })

                            addView(TextView(context).apply {
                                text = "Loading..."
                                textSize = 18f
                                setTypeface(null, Typeface.NORMAL)
                                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 10.dpToPx())
                                setTextColor(Color.parseColor("#0A6E44"))
                            })
                        }
                        val searchLayout = LayoutInflater.from(context).inflate(R.layout.history_create, container, false) as LinearLayout
                        container.addView(searchLayout)
                        val searchBar = searchLayout.findViewById<EditText>(R.id.searchBar)
                        val sortSpinner = searchLayout.findViewById<Spinner>(R.id.sortSpinner)
                        val searchContainer = searchLayout.findViewById<LinearLayout>(R.id.container)
                        container.addView(layout)

                        lifecycleScope.launch {
                            val fetchedValue = withContext(Dispatchers.IO) {
                                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                val kodeAgen = sharedPreferences.getString("kode_agen", "") ?: ""
                                val token = sharedPreferences.getString("token", "") ?: ""

                                Log.d("FormActivity", "BSA Kode Agen: $kodeAgen")

                                if (kodeAgen.isNotEmpty()) {
                                    WebCallerImpl().fetchNasabahList(kodeAgen, token)?.string()
                                } else {
                                    null
                                }
                            }

                            if (fetchedValue.isNullOrEmpty()) {
                                Log.e("FormActivity", "Failed to fetch nasabah list")
                                return@launch
                            }

                            val jsonResponse = JSONObject(fetchedValue)
                            val dataArray = jsonResponse.getJSONArray("data")
                            val dataList = List(dataArray.length()) { i -> dataArray.getJSONObject(i) }
                            layout.removeAllViews()

                            val groupedData = dataList.groupBy {
                                val requestTime = it.getString("request_time")
                                requestTime.split(" ").getOrNull(0) ?: "Unknown Date"
                            }.toSortedMap(compareByDescending { it })

                            groupedData.forEach { (date, nasabahList) ->
                                layout.addView(TextView(context).apply {
                                    text = date
                                    textSize = 15f
                                    setTypeface(null, Typeface.BOLD)
                                    setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    setTextColor(Color.parseColor("#808080"))
                                })

                                nasabahList.sortedByDescending { nasabah ->
                                    nasabah.getString("request_time")
                                }.forEach { nasabah ->
                                    val itemView = LayoutInflater.from(context).inflate(R.layout.nasabah_item, null) as LinearLayout
                                    val namaLengkap = nasabah.getString("nama_lengkap")
                                    val noIdentitas = nasabah.getString("no_identitas")
                                    val status = nasabah.getString("status")
                                    val waktu = nasabah.getString("request_time").split(" ").getOrNull(1) ?: "Unknown Time"

                                    itemView.findViewById<TextView>(R.id.textViewNamaLengkap).text = namaLengkap
                                    itemView.findViewById<TextView>(R.id.textViewNoIdentitas).text = noIdentitas
                                    itemView.findViewById<TextView>(R.id.textViewWaktu).text = waktu

                                    val statusTextView = itemView.findViewById<TextView>(R.id.textViewStatus)
                                    statusTextView.text = when (status) {
                                        "0", "1" -> "Sedang Diproses"
                                        "2" -> "Disetujui"
                                        "3" -> "Ditolak"
                                        else -> "Status Tidak Diketahui"
                                    }
                                    statusTextView.setTextColor(getStatusColor(context, status))

                                    val layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        setMargins(0, 0, 0, 16.dpToPx())
                                    }
                                    itemView.layoutParams = layoutParams
                                    layout.addView(itemView)
                                }
                            }


                            searchBar.addTextChangedListener(object : TextWatcher {
                                override fun afterTextChanged(s: Editable?) {
                                    val searchText = s.toString().lowercase()
                                    val filteredDataList = dataList.filter {
                                        it.getString("nama_lengkap").lowercase().contains(searchText) ||
                                                it.getString("no_identitas").lowercase().contains(searchText)
                                    }
                                    refreshData(filteredDataList, layout, context)
                                }

                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            })

                            sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    val sortOption = parent?.getItemAtPosition(position) as String
                                    val sortedDataList = when (sortOption) {
                                        "Sort by Name" -> dataList.sortedBy { it.getString("nama_lengkap").lowercase() }
                                        "Sort by Time" -> dataList.sortedBy { it.getString("request_time") }
                                        "Sort by Status" -> dataList.sortedBy { it.getString("status") }
                                        else -> dataList
                                    }
                                    refreshData(sortedDataList, layout, context)
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                            }
                        }
                    }

                    else if (component.id == "HY001") {
                        val context = this@FormActivity
                        val searchLayout = LayoutInflater.from(context).inflate(R.layout.history_create, container, false) as LinearLayout
                        container.addView(searchLayout)

                        val errorLayout = LayoutInflater.from(context).inflate(R.layout.error_history_transaksi, container, false) as LinearLayout
                        val bodyMessageTextView = errorLayout.findViewById<TextView>(R.id.body_message)
                        errorLayout.visibility = View.GONE
                        container.addView(errorLayout)

                        // Initialize search and sort views
                        val searchBar = searchLayout.findViewById<EditText>(R.id.searchBar)
                        val sortSpinner = searchLayout.findViewById<Spinner>(R.id.sortSpinner)
                        val searchContainer = searchLayout.findViewById<LinearLayout>(R.id.container)

                        // Initialize sort options
                        val sortOptions = resources.getStringArray(R.array.sort_options1)
                        val sortAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sortOptions)
                        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        sortSpinner.adapter = sortAdapter

                        val dataList = mutableListOf<JSONObject>() // Use mutableListOf to store data
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                        }.also { layout ->
                            lifecycleScope.launch {
                                val webCaller = WebCallerImpl()
                                val fetchedValue = withContext(Dispatchers.IO) {
                                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                    val token = sharedPreferences.getString("token", "") ?: ""
                                    val terminalId = sharedPreferences.getString("tid", "") ?: ""
                                    val response = webCaller.fetchHistory(terminalId, token)
                                    response?.string()
                                }

                                if (!fetchedValue.isNullOrEmpty()) {
                                    try {
                                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                                        val jsonResponse = JSONObject(fetchedValue)
                                        val status = jsonResponse.optBoolean("status", true)

                                        if (!status) {
                                            val message = jsonResponse.optString("message", "Tidak Ada Transaksi Hari Ini")
                                            withContext(Dispatchers.Main) {
                                                errorLayout.visibility = View.VISIBLE
                                                bodyMessageTextView.text = message
                                            }
                                        } else {
                                            errorLayout.visibility = View.GONE
                                        }

                                        val dataArray = jsonResponse.optJSONArray("data") ?: JSONArray()
                                        val dataList = List(dataArray.length()) { i -> dataArray.getJSONObject(i) }
                                        layout.removeAllViews()

                                        val groupedData = dataList.groupBy {
                                            val replyTime = it.optString("reply_time", "")
                                            replyTime.split(" ").getOrNull(0) ?: "Unknown Date"
                                        }

                                        // Add grouped data to the layout
                                        groupedData.forEach { (date, historyList) ->
                                            layout.addView(TextView(context).apply {
                                                text = date
                                                textSize = 15f
                                                setTypeface(null, Typeface.BOLD)
                                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                                                setTextColor(Color.parseColor("#808080"))
                                            })

                                            historyList.forEach { history ->
                                                val replyTime = history.optString("reply_time", "")
                                                val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                                val targetFormat = SimpleDateFormat("dd MM yyyy HH:mm", Locale.getDefault())
                                                val formattedReplyTime: String = try {
                                                    val date = originalFormat.parse(replyTime)
                                                    targetFormat.format(date)
                                                } catch (e: ParseException) {
                                                    Log.e("FormActivity", "Error parsing date: ${e.message}")
                                                    replyTime
                                                }

                                                val status = history.optString("status", "")
                                                val requestMessage = history.getString("request_message")

                                                val nominal = history.getString("nominal")

                                                val nominalDouble = nominal?.toDoubleOrNull()
                                                val nominal_rupiah = nominalDouble?.let {
                                                    formatRupiah(
                                                        it
                                                    )
                                                }

                                                val formatTrans = "$nominal_rupiah"

                                                val requestMessageJson = JSONObject(requestMessage.trim())
                                                val msgObject = requestMessageJson.getJSONObject("msg")

                                                val msgId = msgObject.getString("msg_id")
                                                val msgSi = msgObject.getString("msg_si")

                                                val actionText = when (msgSi) {
                                                    "T00002" -> "Transfer"
                                                    "OTT001" -> "Tarik Tunai"
                                                    "OT0001" -> "Setor Tunai"
                                                    else -> msgSi
                                                }

                                                val statusTrans = when (status) {
                                                    "00" -> "Berhasil"
                                                    else -> "Gagal"
                                                }

                                                val statusColor = when (status) {
                                                    "00" -> ContextCompat.getColor(context, R.color.green)
                                                    else -> ContextCompat.getColor(context, R.color.red)
                                                }

                                                // Inflate the item layout
                                                val itemView = LayoutInflater.from(context).inflate(R.layout.item_history, null).apply{
                                                    setOnClickListener {
                                                        lifecycleScope.launch {
                                                            val preferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                                            val token = preferences.getString("token", "") ?: ""
                                                            val terminalId = preferences.getString("tid", "") ?: ""
                                                            val messageId = msgId.replace("+", "%2B")


                                                            Log.d("FormActivity", "token: $token, tid: $terminalId, msg: $messageId")

                                                            val responseBodyString = withContext(Dispatchers.IO) {
                                                                try {
                                                                    val responseBody = webCaller.fetchHistoryDetail(terminalId, messageId, token)

                                                                    responseBody?.string()
                                                                } catch (e: Exception) {
                                                                    Log.e("FormActivity", "Error fetching history detail: ${e.message}", e)
                                                                    null
                                                                }
                                                            }

                                                            if (!responseBodyString.isNullOrEmpty()) {
                                                                try {
                                                                    val jsonResponse = JSONObject(responseBodyString)
                                                                    val dataArray = jsonResponse.optJSONArray("data")

                                                                    if (dataArray != null && dataArray.length() > 0) {
                                                                        val dataObject = dataArray.getJSONObject(0)
                                                                        val responseMessageString = dataObject.optString("response_message", "")
                                                                        Log.d("FormActivity", "Response message ${responseMessageString}")
                                                                        lifecycleScope.launch {
                                                                            val screenJson =
                                                                                JSONObject(responseMessageString)
                                                                            val newScreen: Screen =
                                                                                ScreenParser.parseJSON(
                                                                                    screenJson
                                                                                )
                                                                            handleScreenType(newScreen)
                                                                        }
                                                                    } else {
                                                                        Log.d("FormActivity", "The JSON array is empty.")
                                                                    }
                                                                } catch (e: JSONException) {
                                                                    Log.e("FormActivity", "Detail JSON parsing error: ${e.message}", e)
                                                                    withContext(Dispatchers.Main) {
                                                                        Toast.makeText(this@FormActivity, "Error parsing detail JSON response", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            } else {
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(this@FormActivity, "Failed to fetch detail", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                            searchBar.addTextChangedListener(object : TextWatcher {
                                                                override fun afterTextChanged(s: Editable?) {
                                                                    val searchText = s.toString().lowercase()
                                                                    val filteredDataList = dataList.filter {
                                                                        it.optString("nama_rek", "").lowercase().contains(searchText) ||
                                                                                it.optString("status", "").lowercase().contains(searchText)
                                                                    }
                                                                    refreshDataTransfer(filteredDataList.groupBy {
                                                                        val replyTime = it.optString("reply_time", "")
                                                                        replyTime.split(" ").getOrNull(0) ?: "Unknown Date"
                                                                    }, searchContainer, context)
                                                                }

                                                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                                            })

                                                            sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                                                    val sortOption1 = parent?.getItemAtPosition(position) as String
                                                                    val sortedDataList = when (sortOption1) {
                                                                        "Sort by Date" -> dataList.sortedBy {
                                                                            try {
                                                                                val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                                                                val date = originalFormat.parse(it.optString("reply_time", ""))
                                                                                date ?: Date(0) // Default to epoch if parsing fails
                                                                            } catch (e: ParseException) {
                                                                                Date(0)
                                                                            }
                                                                        }
                                                                        "Sort by Status" -> dataList.sortedBy { it.optString("status", "") }
                                                                        "Sort by Transaction Type" -> dataList.sortedBy {
                                                                            val requestMessage = it.optString("request_message", "")
                                                                            val requestMessageJson = JSONObject(requestMessage.trim())
                                                                            val msgObject = requestMessageJson.getJSONObject("msg")
                                                                            msgObject.optString("msg_si", "")
                                                                        }
                                                                        else -> dataList
                                                                    }
                                                                    refreshDataTransfer(sortedDataList.groupBy {
                                                                        val replyTime = it.optString("reply_time", "")
                                                                        replyTime.split(" ").getOrNull(0) ?: "Unknown Date"
                                                                    }, searchContainer, context)
                                                                }

                                                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                                                            }
                                                        }
                                                    }
                                                }

                                                // Populate the item view with data
                                                itemView.findViewById<TextView>(R.id.text_action).text = actionText
                                                itemView.findViewById<TextView>(R.id.text_format_trans).text = formatTrans
                                                itemView.findViewById<TextView>(R.id.text_status).apply {
                                                    text = statusTrans
                                                    setTextColor(statusColor)
                                                }
                                                itemView.findViewById<TextView>(R.id.text_reply_time).text = formattedReplyTime

                                                // Add margin to the item view
                                                val params = LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                                ).apply {
                                                    setMargins(0, 0, 0, 16.dpToPx()) // Add bottom margin for spacing between items
                                                }
                                                itemView.layoutParams = params

                                                // Add the populated view to the layout
                                                layout.addView(itemView)
                                            }
                                        }
                                    } catch (e: JSONException) {
                                        Log.e("FormActivity", "JSON parsing error: ${e.message}")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@FormActivity, "Error parsing JSON response", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@FormActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                    else if (component.id == "HY002") {
                        val context = this@FormActivity
                        val searchLayout = LayoutInflater.from(context).inflate(R.layout.history_create, container, false) as LinearLayout
                        container.addView(searchLayout)

                        val searchBar = searchLayout.findViewById<EditText>(R.id.searchBar)
                        val sortSpinner = searchLayout.findViewById<Spinner>(R.id.sortSpinner)
                        val searchContainer = searchLayout.findViewById<LinearLayout>(R.id.container)

                        val sortOptions = resources.getStringArray(R.array.sort_options1)
                        val sortAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sortOptions)
                        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        sortSpinner.adapter = sortAdapter

                        lifecycleScope.launch {
                            val webCaller = WebCallerImpl()
                            val fetchedValue = withContext(Dispatchers.IO) {
                                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                val token = sharedPreferences.getString("token", "") ?: ""
                                val mid = sharedPreferences.getString("kode_agen", "") ?: ""
                                val response = webCaller.historyPengaduan(mid, token)
                                response?.string()
                            }

                            if (!fetchedValue.isNullOrEmpty()) {
                                try {
                                    val jsonResponse = JSONObject(fetchedValue)
                                    val dataArray = jsonResponse.optJSONArray("data") ?: JSONArray()
                                    val dataList = List(dataArray.length()) { i -> dataArray.getJSONObject(i) }

                                    searchContainer.removeAllViews()

                                    val groupedData = dataList.groupBy {
                                        it.optString("request_time", "").split(" ").getOrNull(0) ?: "Unknown Date"
                                    }

                                    groupedData.forEach { (date, historyList) ->
                                        searchContainer.addView(TextView(context).apply {
                                            text = date
                                            textSize = 15f
                                            setTypeface(null, Typeface.BOLD)
                                            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                                            setTextColor(Color.parseColor("#808080"))
                                        })

                                        historyList.forEach { history ->
                                            val request_time = history.optString("request_time", "")
                                            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val targetFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                                            val formattedTime = try {
                                                val date = originalFormat.parse(request_time)
                                                targetFormat.format(date)
                                            } catch (e: ParseException) {
                                                request_time
                                            }

                                            val statusPengaduan = when (history.optString("status", "")) {
                                                "0" -> "Pending"
                                                "1" -> "Sedang Diproses"
                                                "2" -> "Selesai"
                                                else -> "Gagal"
                                            }

                                            val statusColor = when (history.optString("status", "")) {
                                                "0" -> ContextCompat.getColor(context, R.color.dark_grey)
                                                "1" -> ContextCompat.getColor(context, R.color.yellow)
                                                "2" -> ContextCompat.getColor(context, R.color.green)
                                                else -> ContextCompat.getColor(context, R.color.red)
                                            }

                                            val itemView = LayoutInflater.from(context).inflate(R.layout.item_history, null).apply {
                                                findViewById<TextView>(R.id.text_action).text = history.optString("kategori")
                                                findViewById<TextView>(R.id.text_format_trans).text = history.optString("judul", "N/A")
                                                findViewById<TextView>(R.id.text_status).apply {
                                                    text = statusPengaduan
                                                    setTextColor(statusColor)
                                                }
                                                findViewById<TextView>(R.id.text_reply_time).text = formattedTime
                                            }

                                            val params = LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                            ).apply {
                                                setMargins(0, 0, 0, 16.dpToPx())
                                            }

                                            itemView.layoutParams = params

                                            searchContainer.addView(itemView)
                                        }
                                    }

                                    searchBar.addTextChangedListener(object : TextWatcher {
                                        override fun afterTextChanged(s: Editable?) {
                                            val searchText = s.toString().lowercase()
                                            val filteredDataList = dataList.filter {
                                                it.optString("kategori", "").lowercase().contains(searchText)
                                            }
                                            refreshData(filteredDataList, searchContainer, context)
                                        }
                                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                    })

                                    sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                            val sortOption = parent?.getItemAtPosition(position) as String
                                            val sortedDataList = when (sortOption) {
                                                "Sort by Date" -> dataList.sortedBy { it.optString("request_time") }
                                                "Sort by Status" -> dataList.sortedBy { it.optString("status") }
                                                else -> dataList
                                            }
                                            refreshData(sortedDataList, searchContainer, context)
                                        }
                                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                                    }

                                } catch (e: JSONException) {
                                    Log.e("FormActivity", "JSON parsing error: ${e.message}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@FormActivity, "Error parsing JSON response", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }else if (component.id == "NOT01") {
                        Log.d("FormActivity", "Tombol NOT00 ditekan, memulai proses insert dan pengambilan notifikasi...")

                        // Mengambil layout atau container di mana data akan ditampilkan
                        val container = findViewById<LinearLayout>(R.id.menu_container)

                        if (container != null) {
                            Log.d("FormActivity", "Container ditemukan, memulai pengaturan...")
                            container.removeAllViews()
                        } else {
                            Log.e("FormActivity", "Container tidak ditemukan, cek layout!")
                        }

                        // Inisialisasi Database Helper untuk menyimpan dan mengambil data dari SQLite
                        val notificationDbHelper = NotificationDatabaseHelper(this)

                        // Simpan notifikasi untuk tes insert
//                        val testTitle = "Contoh Notifikasi"
//                        val testMessage = "Ini adalah pesan notifikasi untuk pengujian"
//                        val testTimestamp = System.currentTimeMillis()
//
//                        Log.d("InsertNotification", "Menyimpan notifikasi: Title: $testTitle, Message: $testMessage")
//                        val insertResult = notificationDbHelper.insertNotification(testTitle, testMessage, testTimestamp)

//                        if (insertResult) {
//                            Log.d("InsertNotification", "Notifikasi berhasil disimpan")
//                        } else {
//                            Log.e("InsertNotification", "Gagal menyimpan notifikasi")
//                        }

                        // Mengambil data notifikasi dari database
                        val notifications = notificationDbHelper.getAllNotifications()

                        Log.d("NotificationFetch", "Total notifikasi ditemukan: ${notifications.size}")

                        // Jika tidak ada notifikasi, tampilkan pesan kosong
                        if (notifications.isEmpty()) {
                            val emptyView = TextView(this).apply {
                                text = "Tidak ada riwayat notifikasi."
                                textSize = 16f
                                setTextColor(ContextCompat.getColor(this@FormActivity, android.R.color.black))
                                setPadding(16, 16, 16, 16)
                                gravity = Gravity.CENTER
                            }

                            // Menggunakan FrameLayout sebagai container untuk menempatkan emptyView di tengah
                            val frameLayout = FrameLayout(this).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                addView(emptyView, FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    Gravity.CENTER
                                ))
                            }

                            container.addView(frameLayout)
                        }
                        else {
                            // Loop melalui daftar notifikasi
                            for (notification in notifications) {
                                Log.d("NotificationFetch", "Notifikasi ditemukan: ${notification.title}, ${notification.message}, ${notification.timestamp}")

                                // Inflate tampilan item untuk setiap notifikasi
                                val itemView = LayoutInflater.from(this).inflate(R.layout.notification, container, false)

                                // Set judul, pesan, dan timestamp dari notifikasi
                                val titleView = itemView.findViewById<TextView>(R.id.notification_title)
                                val messageView = itemView.findViewById<TextView>(R.id.notification_message)
                                val timestampView = itemView.findViewById<TextView>(R.id.notification_timestamp)

                                titleView.text = notification.title
                                messageView.text = notification.message
                                timestampView.text = android.text.format.DateFormat.format("dd-MM-yyyy hh:mm", notification.timestamp)

                                // Tambahkan item ke container
                                container.addView(itemView)
                            }
                        }
                    }
                    else {
                        LinearLayout(this@FormActivity).apply {
                            orientation = LinearLayout.VERTICAL

                            val componentValue = getComponentValue(component)

                            if (screen.id == "CCIF001") {
                                inputRekening[component.label] = componentValue
                            }

                            val numericValue = componentValue.toDoubleOrNull() ?: 0.0
                            var formattedValue = when {
                                component.label.contains("nominal", ignoreCase = true)|| component.label.contains("nilai transfer", ignoreCase = true) -> {
                                    nominalValue = numericValue
                                    formatRupiah(nominalValue)
                                }

                                component.label.contains(
                                    "fee",
                                    ignoreCase = true
                                ) || component.label.contains("admin bank", ignoreCase = true) -> {
                                    feeValue = numericValue
                                    formatRupiah(feeValue)
                                }
                                component.label.contains("saldo", ignoreCase = true) -> {
                                    var saldoStr = getComponentValue(component)

                                    if (saldoStr.contains("-")) {
                                        saldoStr = saldoStr.replace("-", "")
                                    }

                                    if (saldoStr.contains(",")) {
                                        saldoStr = saldoStr.replace(",", "")
                                    }

                                    val saldo = saldoStr.toDoubleOrNull() ?: 0.0

                                    // Format saldo
                                    val formattedSaldo = formatRupiah(saldo)

                                    // Menggunakan SpannableString untuk menambahkan gaya bold
                                    val spannable = SpannableString(formattedSaldo)
                                    val boldSpan = StyleSpan(Typeface.BOLD)

                                    // Misalkan kita ingin membuat seluruh teks saldo menjadi tebal
                                    spannable.setSpan(boldSpan, 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                                    spannable
                                }
                                else -> componentValue
                            }

                            if (component.id == "T0002") {
                                val totalValue = nominalValue + feeValue
                                Log.d("FormActivity", "Nominal : $nominalValue")
                                Log.d("FormActivity", "Fee : $feeValue")
                                Log.d("FormActivity", "Total : $totalValue")
                                formattedValue = formatRupiah(totalValue)

                                component.values = component.values.mapIndexed { index, pair ->
                                    if (index == 0) pair.copy(second = formattedValue) else pair
                                }

                                component.compValues?.compValue =
                                    component.compValues?.compValue?.mapIndexed { index, compVal ->
                                        if (index == 0) compVal.copy(value = formattedValue) else compVal
                                    } ?: emptyList()
                            }

                            if (screenTransfer.contains(screen.id)) {
                                setPadding(0.dpToPx(), 0.dpToPx(), 0.dpToPx(), 0.dpToPx())
                            } else {
                                setPadding(0.dpToPx(), 0.dpToPx(), 0.dpToPx(), 0.dpToPx())
                            }

                            if (component.id == "APY00") {
                                addView(TextView(this@FormActivity).apply {
                                    text = component.label
                                    textSize = 15f
                                    setTypeface(null, Typeface.BOLD)
                                    setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    setTextColor(Color.parseColor("#0A6E44")) // Mengatur warna teks label
                                })
                            } else {
                                addView(TextView(this@FormActivity).apply {
                                    text = component.label
                                    textSize = 15f
                                    setTypeface(null, Typeface.NORMAL)
                                    if (screenTransfer.contains(screen.id)) {
                                        setPadding(0.dpToPx(), 0.dpToPx(), 16.dpToPx(), 0.dpToPx())
                                    } else {
                                        setPadding(0.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    }
                                    setTextColor(Color.parseColor("#0A6E44")) // Mengatur warna teks label
                                })
                                if (screen.id == "RCCIF02") {
                                    val rekeningValue = findValueInInputRekening(component.label) ?: componentValue

                                    addView(TextView(this@FormActivity).apply {
                                        text = rekeningValue
                                        textSize = 18f
                                        setPadding(0.dpToPx(), 0, 16.dpToPx(), 10.dpToPx())
                                    })
                                } else if (screenTransfer.contains(screen.id)) {
                                    addView(TextView(this@FormActivity).apply {
                                        text = formattedValue
                                        textSize = 18f
                                        setPadding(0.dpToPx(), 0, 16.dpToPx(), 0.dpToPx())
                                    })
                                }else{
                                    addView(TextView(this@FormActivity).apply {
                                        text = formattedValue
                                        textSize = 18f
                                        setPadding(0.dpToPx(), 0, 16.dpToPx(), 10.dpToPx())
                                    })
                                }

                            }
                            if (screen.id != "TF00003") {
//                                background = ContextCompat.getDrawable(
//                                    this@FormActivity,
//                                    R.drawable.text_view_background
//                                )
                            }
                        }
                    }
                }

                2 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        val labelTextView = TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                            setTextSize(18f) // Ukuran teks untuk label
                            setTextColor(Color.parseColor("#0A6E44")) // Warna teks untuk label

                            // Atur jarak antara label dan EditText di bawahnya
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(20, 0, 0, 18) // Margin bawah 16dp
                            layoutParams = params
                        }
                        addView(labelTextView)

                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            background = getDrawable(R.drawable.edit_text_background)
                            id = View.generateViewId()
                            tag = component.id
                            setTextSize(16f) // Ukuran teks untuk input
                            setTextColor(ContextCompat.getColor(this@FormActivity, R.color.black)) // Warna teks untuk input
                        }
                        inputValues[component.id] = ""

                        setKeyboard(editText, component)
                        editText.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                            override fun afterTextChanged(s: Editable?) {
                                val inputText = s.toString()

                                inputValues[component.id] = inputText

                                // Format angka jika label mengandung "nominal", "fee", atau "nilai"
                                if (component.label.contains("nominal", ignoreCase = true) ||
                                    component.label.contains("fee", ignoreCase = true) ||
                                    component.label.contains("nilai", ignoreCase = true)
                                ) {
                                    editText.removeTextChangedListener(this)

                                    val numericInput = inputText.replace(".", "")
                                    val formattedText = if (numericInput.isNotEmpty()) {
                                        numericInput.reversed().chunked(3).joinToString(".").reversed()
                                    } else {
                                        ""
                                    }

                                    editText.setText(formattedText)
                                    editText.setSelection(formattedText.length)
                                    inputValues[component.id] = numericInput
                                    editText.addTextChangedListener(this)
                                }

                                if (screen.id == "CCIF001") {
                                    inputRekening[component.label] = inputText
                                    Log.d("FormActivity", "inputRekening[${component.id}] set to: $inputText")
                                }

                                if (component.label == "NIK") {
                                    nikValue = inputText
                                    Log.d("FormActivity", "NIK : $nikValue")
                                }

                                val errors = validateInput(component)
                                if (errors.isNotEmpty()) {
                                    editText.error = errors.first()
                                    editText.background = ContextCompat.getDrawable(this@FormActivity, R.drawable.edit_text_wrong)
                                } else {
                                    editText.error = null
                                    editText.background = ContextCompat.getDrawable(this@FormActivity, R.drawable.edit_text_background)
                                }
                            }
                        })


                        addView(editText)
                    }
                }

                3 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL

                        // TextView untuk label
                        val labelTextView = TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                            setTextSize(18f) // Ukuran teks untuk label
                            setTextColor(Color.parseColor("#0A6E44")) // Warna teks untuk label

                            // Atur jarak antara label dan EditText di bawahnya
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(20, 0, 0, 18) // Margin bawah 18dp
                            layoutParams = params
                        }
                        addView(labelTextView)

                        // EditText untuk input password
                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            background = getDrawable(R.drawable.pass_bg)
                            id = View.generateViewId()
                            tag = component.id
                            setTextSize(16f) // Ukuran teks untuk input
                            setTextColor(ContextCompat.getColor(this@FormActivity, R.color.black)) // Warna teks untuk input

                            // Adjust the padding to move the hint text slightly to the right
                            setPadding(48, paddingTop, 48, paddingBottom) // Adjust left and right padding

                            // Set the eye icon to the right of the EditText
                            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0)

                            // Add padding between the text and eye icon
                            setCompoundDrawablePadding(16)

                            // Toggle visibility on eye icon touch
                            setOnTouchListener { v, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    if (event.rawX >= (right - compoundDrawables[2].bounds.width() - paddingRight)) {
                                        if (inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_open, 0)
                                        } else {
                                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                                            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0)
                                        }
                                        setSelection(text.length)  // Move cursor to end
                                        return@setOnTouchListener true
                                    }
                                }
                                false
                            }
                        }

                        // Increase the size of the EditText box
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 16, 0, 16) // Add margins if needed
                        editText.layoutParams = params

                        inputValues[component.id] = ""
                        editText.addTextChangedListener {
                            inputValues[component.id] = it.toString()

                        }
                        editText.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                            override fun afterTextChanged(s: Editable?) {
                                val inputText = s.toString()
                                inputValues[component.id] = inputText

                                // Validasi input
                                val errors = validateInput(component)
                                if (errors.isNotEmpty()) {
                                    editText.error = errors.first()
                                    editText.background = ContextCompat.getDrawable(this@FormActivity, R.drawable.edit_text_wrong)
                                } else {
                                    editText.error = null
                                    editText.background = ContextCompat.getDrawable(this@FormActivity, R.drawable.edit_text_background)
                                }
                            }
                        })
                        addView(editText)
                    }
                }

                4 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        if (component.id == "CIF13"){

                        }
                        else{
                            // TextView untuk label
                            val labelTextView = TextView(this@FormActivity).apply {
                                text = component.label
                                textSize = 16f
                                setTypeface(null, Typeface.BOLD)
                                setTextSize(18f) // Ukuran teks untuk label
                                setTextColor(Color.parseColor("#0A6E44")) // Warna teks untuk label

                                // Atur jarak antara label dan Spinner di bawahnya
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(0, 0, 0, 18) // Margin bawah 18dp
                                layoutParams = params
                            }
                            addView(labelTextView)
                        }
                        var compOption = component.compValues?.compValue

                        lifecycleScope.launch {
                            val options: List<Pair<String?, String?>> =
                                if (compOption.isNullOrEmpty() ||
                                    compOption.all { it.value.isNullOrBlank() || it.value == "null" }
                                ) {

                                    var formValue =
                                        StorageImpl(applicationContext).fetchForm(screen.id)

                                    if (formValue.isNullOrEmpty()) {
                                        formValue = withContext(Dispatchers.IO) {
                                            ArrestCallerImpl(OkHttpClient()).fetchScreen(screen.id)
                                        }
                                        Log.i("FormActivity", "Fetched formValue: $formValue")
                                    }

                                    if (formValue.isNullOrEmpty()) {
                                        Log.e(
                                            "FormActivity",
                                            "Failed to fetch form data or form data is empty for screen ID: ${screen.id}"
                                        )
                                        mutableListOf(
                                            Pair(
                                                "Pilih ${component.label}",
                                                ""
                                            )
                                        ) // Return a list with default placeholder
                                    }
                                    else {
                                        try {
                                            val screenJson = JSONObject(formValue)
                                            val screen: Screen = ScreenParser.parseJSON(screenJson)
                                            val selectedOptions =
                                                screen.comp.firstOrNull { it.id == component.id }
                                                    ?.compValues?.compValue?.mapNotNull {
                                                        Pair(
                                                            it.print,
                                                            it.value
                                                        )
                                                    }
                                                    ?: emptyList()

                                            Log.d(
                                                "FormActivity",
                                                "SELECTED OPTIONS : $selectedOptions")
                                            if (component.id.startsWith("PR")) {
                                                selectedOptions
                                            } else {
                                                mutableListOf(
                                                    Pair(
                                                        "Pilih ${component.label}",
                                                        ""
                                                    )
                                                ) + selectedOptions
                                            }
                                        } catch (e: JSONException) {
                                            Log.e(
                                                "FormActivity",
                                                "JSON Parsing error: ${e.message}"
                                            )
                                            mutableListOf(
                                                Pair(
                                                    "Pilih ${component.label}",
                                                    ""
                                                )
                                            ) // Return a list with default placeholder
                                        }
                                    }
                                } else {
                                    if (component.id.startsWith("PR")) {
                                        compOption.map { Pair(it.print, it.value) }
                                    } else {
                                        mutableListOf(Pair("Pilih ${component.label}", "")) + compOption.map { Pair(it.print, it.value) }
                                    }
                                }
                            if (component.id.startsWith("PR")) {
                                val recyclerView = RecyclerView(this@FormActivity).apply {
                                    layoutManager = GridLayoutManager(this@FormActivity, 2)
                                    adapter = ProdukAdapter(options) { selectedItem ->
                                        val selectedValue = selectedItem.first
                                        val selectedCompValue = selectedItem.second

                                        inputValues[component.id] = selectedCompValue ?: ""
                                        Log.d("FormActivity", "Component ID: ${component.id}, Selected Value: $selectedValue")
                                    }
                                }

                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 0, 0, 20)
                                }

                                addView(recyclerView)
                            }
                            else if (component.id == "CIF13") {
                                component.values?.forEach { pair ->
                                    // Menggabungkan elemen pertama dan kedua dari Pair dengan pemisah, misalnya "-"
                                    val combinedValue = "${pair.first} - ${pair.second}"
                                    valuesKabKot.add(combinedValue)
                                }

                                Log.d("FormActivity", "valuesCIF13: $valuesKabKot")
                            }

                            else {
                                val spinner = Spinner(this@FormActivity).apply {
                                    background = getDrawable(R.drawable.combo_box)
                                    val adapter = ArrayAdapter(
                                        this@FormActivity,
                                        android.R.layout.simple_spinner_item,
                                        options.map { option ->
                                            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                            val savedNorekening = sharedPreferences.getString("norekening", "") ?: ""
                                            val userFullname = sharedPreferences.getString("fullname", "") ?: ""
                                            if (option.first == "Rekening Agen") {
                                                "$savedNorekening - $userFullname (AGEN)"
                                            } else if (option.first == "Rekening Nasabah"){
                                                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                                val norekBillerNasabah = sharedPreferences.getString("norek_biller", null)
                                                val namarekBillerNasabah = sharedPreferences.getString("namarek_biller", null)
                                                if (!norekBillerNasabah.isNullOrEmpty() && !namarekBillerNasabah.isNullOrEmpty()) {
                                                    "$norekBillerNasabah - $namarekBillerNasabah (NASABAH)"
                                                } else {
                                                    option.first ?: ""
                                                }
                                            } else {
                                                option.first ?: "" // Tampilkan print, jika null tampilkan string kosong
                                            }
                                        }
                                    )
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    this.adapter = adapter

                                    // Set margin untuk spinner
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        setMargins(
                                            0,
                                            0,
                                            0,
                                            20
                                        ) // Margin bawah 20dp untuk jarak antar elemen
                                    }
                                }

                                spinner.onItemSelectedListener =
                                    object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(
                                            parent: AdapterView<*>,
                                            view: View,
                                            position: Int,
                                            id: Long
                                        ) {
                                            if (position == 0) {
                                                inputValues[component.id] = ""
                                                pickOTP = null
                                                Log.d("OTP", "Pick OTP: '$pickOTP'")
                                                Log.d(
                                                    "FormActivity",
                                                    "Component ID: ${component.id}, No Value Selected"
                                                )
                                                spinner.background = ContextCompat.getDrawable(
                                                    this@FormActivity,
                                                    R.drawable.spinner_error_background
                                                )

                                                val textView = view as TextView
                                                textView.setTextColor(
                                                    ContextCompat.getColor(
                                                        this@FormActivity,
                                                        android.R.color.holo_red_dark
                                                    )
                                                )
                                            } else {
                                                spinner.background = ContextCompat.getDrawable(
                                                    this@FormActivity,
                                                    R.drawable.combo_box
                                                )

                                                val textView = view as TextView
                                                textView.setTextColor(ContextCompat.getColor(this@FormActivity, android.R.color.black))

                                                val selectedPair = options[position]
                                                val selectedValue = selectedPair.first
                                                val selectedCompValue = selectedPair.second
                                                var spinner: Spinner? = null
                                                if (screen.id == "CCIF001") {
                                                    inputRekening[component.label] =
                                                        selectedValue
                                                            ?: "" // Menyimpan selectedValue
                                                }

                                                Log.d(
                                                    "FormActivity",
                                                    "Component ID: ${component.id}, Selected Value: $selectedValue, Position: $position"
                                                )
                                                Log.d(
                                                    "FormActivity",
                                                    "selectedPair: ${selectedPair}, Selected Value: $selectedValue, selectedCompValue: $selectedCompValue"
                                                )

                                                when (component.id) {
                                                    "NRK01" -> {
                                                        pickNRK = selectedValue
                                                        Log.d(
                                                            "Form",
                                                            "Norekening yang dipilih: $selectedValue"
                                                        )

                                                        val sharedPreferences =
                                                            getSharedPreferences(
                                                                "MyAppPreferences",
                                                                MODE_PRIVATE
                                                            )
                                                        val savedNorekening =
                                                            sharedPreferences.getString(
                                                                "norekening",
                                                                ""
                                                            ) ?: ""
                                                        val userFullname =
                                                            sharedPreferences.getString(
                                                                "fullname",
                                                                ""
                                                            ) ?: ""
                                                        val noAgen = sharedPreferences.getString(
                                                            "merchant_phone",
                                                            ""
                                                        ) ?: ""

                                                        Log.d(
                                                            "FormActivity",
                                                            "Fetched Userfullname: '$userFullname'"
                                                        )

                                                        var norekToSave: String? = null
                                                        var namaToSave: String? = null
                                                        var nomorToSave: String? = null

                                                        if (selectedValue == "Rekening Nasabah") {
                                                            val norekBillerNasabah =
                                                                sharedPreferences.getString(
                                                                    "norek_biller",
                                                                    null
                                                                )
                                                            val namarekBillerNasabah =
                                                                sharedPreferences.getString(
                                                                    "namarek_biller",
                                                                    null
                                                                )
                                                            val nomorBillerNasabah =
                                                                sharedPreferences.getString(
                                                                    "nomor_biller",
                                                                    null
                                                                )
                                                            if (norekBillerNasabah != null && namarekBillerNasabah != null && norekBillerNasabah.isNotEmpty() && namarekBillerNasabah.isNotEmpty()) {
                                                                norekToSave = norekBillerNasabah
                                                                namaToSave = namarekBillerNasabah
                                                                nomorToSave = nomorBillerNasabah
                                                                Log.d(
                                                                    "FormActivity",
                                                                    "NRK01 set to saved No Rekening: $norekBillerNasabah, Account Name: $namarekBillerNasabah"
                                                                )
                                                            } else {
                                                                lifecycleScope.launch {
                                                                    val editor =
                                                                        sharedPreferences.edit()
                                                                    val gson = Gson()
                                                                    val screenJson =
                                                                        gson.toJson(screen) // mengubah objek Screen menjadi JSON String
                                                                    editor.putString(
                                                                        "screen_biller",
                                                                        screenJson
                                                                    )
                                                                    editor.apply()
                                                                    var newScreenId = "PRP0006"
                                                                    var formValue = StorageImpl(
                                                                        applicationContext
                                                                    ).fetchForm(newScreenId)
                                                                    if (formValue.isNullOrEmpty()) {
                                                                        formValue =
                                                                            withContext(Dispatchers.IO) {
                                                                                ArrestCallerImpl(
                                                                                    OkHttpClient()
                                                                                ).fetchScreen(
                                                                                    newScreenId
                                                                                )
                                                                            }
                                                                        Log.i(
                                                                            "FormActivity",
                                                                            "Fetched formValue: $formValue"
                                                                        )
                                                                    }
                                                                    setupScreen(formValue)
                                                                }
                                                            }
                                                        } else if (selectedValue == "Rekening Agen") {
                                                            if (savedNorekening.isNotEmpty() && userFullname.isNotEmpty()) {
                                                                norekToSave = savedNorekening
                                                                namaToSave = userFullname
                                                                nomorToSave = noAgen
                                                                Log.d(
                                                                    "FormActivity",
                                                                    "NRK01 set to saved No Rekening: $savedNorekening, Account Name: $userFullname"
                                                                )
                                                            } else {
                                                                Log.d(
                                                                    "FormActivity",
                                                                    "NRK01 set to saved No Rekening: $savedNorekening, Account Name: $userFullname"
                                                                )
                                                            }
                                                        }

                                                        // Set inputValues hanya di akhir
                                                        if (norekToSave != null && namaToSave != null) {
                                                            inputValues[component.id] =
                                                                "$norekToSave|$namaToSave|$nomorToSave"
                                                            Log.e(
                                                                "FormActivity",
                                                                "Norek dan Namarek Nasabah dihapus"
                                                            )
                                                            val sharedPreferences =
                                                                getSharedPreferences(
                                                                    "MyAppPreferences",
                                                                    MODE_PRIVATE
                                                                )
                                                            val editor = sharedPreferences.edit()
                                                            editor.remove("norek_biller")
                                                            editor.remove("namarek_biller")
                                                            editor.remove("nomor_biller")
                                                            editor.apply()
                                                        }
                                                    }

                                                    "PIL03" -> {
                                                        pickOTP = selectedValue
                                                        Log.d(
                                                            "Form",
                                                            "PICK OTP PIL03: $selectedValue"
                                                        )

                                                        // Cek jika nilai tidak valid
                                                        if (selectedValue != "WA" && selectedValue != "SMS") {
                                                            pickOTP = null
                                                            Toast.makeText(
                                                                this@FormActivity,
                                                                "Harus memilih WA atau SMS.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }

                                                    "KOM05" -> {
                                                        val pickJudul = selectedValue
                                                        if (pickJudul != null) {
                                                            Log.d("Form", "PICK OTP: $pickJudul")
                                                            inputValues[component.id] = pickJudul
                                                        } else {
                                                            Log.d("Form", "PICK OTP is null")
                                                        }
                                                    }

                                                    "CB001" -> {
                                                        if (selectedCompValue != null) {
                                                            inputValues[component.id] =
                                                                selectedCompValue.replace(
                                                                    "[OI]",
                                                                    ""
                                                                )
                                                        }
                                                        Log.d(
                                                            "FormActivity",
                                                            "Branch Code set to: ${inputValues[component.id]}"
                                                        )
                                                    }

                                                    "CR002" -> if (selectedValue == "BSA Lakupandai") {
                                                        inputValues[component.id] = "36"
                                                        Log.d(
                                                            "FormActivity",
                                                            "Special case for CR002: Value set to 36"
                                                        )
                                                    }

                                                    "CIF14" -> {
                                                        val selectedCIF14Value =
                                                            selectedCompValue ?: ""
                                                        Log.d(
                                                            "FormActivity",
                                                            "Selected CIF14 Value: $selectedCIF14Value"
                                                        )

                                                        // Ekstraksi nilai dari CIF14
                                                        val extractedValueCIF14 =
                                                            if (selectedCIF14Value.startsWith("[OI]") && selectedCIF14Value.length >= 6) {
                                                                selectedCIF14Value.takeLast(2)
                                                            } else {
                                                                ""
                                                            }
                                                        Log.d(
                                                            "FormActivity",
                                                            "Extracted Value from CIF14: $extractedValueCIF14"
                                                        )
                                                        Log.d(
                                                            "FormActivity",
                                                            "Extracted Value from CIF14: ${selectedPair.first}"
                                                        )

                                                        // Simpan nilai terpilih untuk CIF14
                                                        inputValues[component.id] =
                                                            selectedCIF14Value ?: ""
                                                        inputRekening[component.label] =
                                                            selectedPair.first ?: ""
                                                        Log.d(
                                                            "FormActivity",
                                                            "CIF14 set to: ${inputValues[component.id]}"
                                                        )

                                                        // Cek jika inputValues untuk CIF14 sudah ada isinya
                                                        if (!inputValues[component.id].isNullOrEmpty()) {
                                                            val filteredValues =
                                                                valuesKabKot.filter { option ->
                                                                    option.contains("- [OI]") && option.substringAfter(
                                                                        "- [OI]"
                                                                    ).take(2) == extractedValueCIF14
                                                                }.map { option ->
                                                                    val label =
                                                                        option.substringBefore(" - [OI]")
                                                                    val code =
                                                                        option.substringAfter("[OI]")
                                                                            .trim()
                                                                    Pair(label, code)
                                                                }
                                                            Log.d(
                                                                "FormActivity",
                                                                "Filtered Values: $filteredValues"
                                                            )

                                                            currentLabelCIF13?.let { existingLabel ->
                                                                removeView(existingLabel)
                                                            }
                                                            currentSpinnerCIF13?.let { existingSpinner ->
                                                                removeView(existingSpinner)
                                                            }

                                                            val labelTextView =
                                                                TextView(this@FormActivity).apply {
                                                                    text = "Kabupaten/Kota"
                                                                    textSize = 16f
                                                                    setTypeface(null, Typeface.BOLD)
                                                                    setTextSize(18f)
                                                                    setTextColor(Color.parseColor("#0A6E44"))

                                                                    // Atur jarak antara label dan Spinner di bawahnya
                                                                    val params =
                                                                        LinearLayout.LayoutParams(
                                                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                                                        )
                                                                    params.setMargins(
                                                                        0,
                                                                        0,
                                                                        0,
                                                                        18
                                                                    ) // Margin bawah 18dp
                                                                    layoutParams = params
                                                                }
                                                            addView(labelTextView)
                                                            currentLabelCIF13 = labelTextView

                                                            val spinnerCIF13 =
                                                                Spinner(this@FormActivity).apply {
                                                                    background =
                                                                        getDrawable(R.drawable.combo_box)

                                                                    // Menambahkan opsi pertama sebagai "Pilih Kabupaten/Kota"
                                                                    val initialOptions =
                                                                        mutableListOf("Pilih Kabupaten/Kota")
                                                                    initialOptions.addAll(
                                                                        filteredValues.map { it.first })

                                                                    // Buat adapter dengan initialOptions yang berisi opsi awal dan filteredValues
                                                                    val adapter = ArrayAdapter(
                                                                        this@FormActivity,
                                                                        android.R.layout.simple_spinner_item,
                                                                        initialOptions
                                                                    )
                                                                    adapter.setDropDownViewResource(
                                                                        android.R.layout.simple_spinner_dropdown_item
                                                                    )
                                                                    this.adapter = adapter

                                                                    // Set margin untuk spinner
                                                                    layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                                                        ).apply {
                                                                            setMargins(
                                                                                0,
                                                                                0,
                                                                                0,
                                                                                20
                                                                            ) // Margin bawah 20dp
                                                                        }
                                                                }


                                                            addView(spinnerCIF13)
                                                            currentSpinnerCIF13 = spinnerCIF13
                                                            spinnerCIF13.onItemSelectedListener =
                                                                object :
                                                                    AdapterView.OnItemSelectedListener {
                                                                    override fun onItemSelected(
                                                                        parent: AdapterView<*>,
                                                                        view: View,
                                                                        position: Int,
                                                                        id: Long
                                                                    ) {
                                                                        if (position > 0) { // Abaikan "Pilih Kabupaten/Kota"
                                                                            val selectedItem =
                                                                                filteredValues[position - 1]
                                                                            val realValue =
                                                                                selectedItem.first
                                                                            val extractedCIF13Value =
                                                                                selectedItem.second
                                                                            inputRekening["Kabupaten/Kota"] =
                                                                                realValue
                                                                            Log.d(
                                                                                "FormActivity",
                                                                                "Selected item from new CIF14 ComboBox Real Value: $realValue"
                                                                            )
                                                                            Log.d(
                                                                                "FormActivity",
                                                                                "Selected item from new CIF14 ComboBox Code: $extractedCIF13Value"
                                                                            )
                                                                            Log.d(
                                                                                "FormActivity",
                                                                                "Selected item from new CIF14 ComboBox: $selectedItem"
                                                                            )
                                                                            inputValues["CIF13"] =
                                                                                extractedCIF13Value
                                                                            Log.d(
                                                                                "FormActivity",
                                                                                "Input Values $inputValues"
                                                                            )
                                                                        } else {
                                                                            Log.d(
                                                                                "FormActivity",
                                                                                "No valid item selected in CIF14 ComboBox"
                                                                            )
                                                                        }
                                                                    }

                                                                    override fun onNothingSelected(
                                                                        parent: AdapterView<*>
                                                                    ) {
                                                                        Log.d(
                                                                            "FormActivity",
                                                                            "Nothing selected for new CIF14 ComboBox"
                                                                        )
                                                                    }
                                                                }

                                                        }
                                                    }

                                                    else -> inputValues[component.id] =
                                                        selectedCompValue ?: ""
                                                }
                                            }
                                        }
                                        override fun onNothingSelected(parent: AdapterView<*>) {
                                            Log.d(
                                                "FormActivity",
                                                "Nothing selected for Component ID: ${component.id}"
                                            )
                                        }
                                    }

                                addView(spinner)
                            }
                        }
                    }.let { view ->
                        container.addView(view, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(20, 20, 20, 20) // Margin untuk seluruh view
                        })
                    }

                }

                5 -> {
                    val inflater = LayoutInflater.from(this@FormActivity)
                    val newLayout = inflater.inflate(R.layout.checkbox_layout, null) as LinearLayout

                    val titleView = newLayout.findViewById<TextView>(R.id.checkbox_title) // Correct ID
                    val checkboxContainer = newLayout.findViewById<LinearLayout>(R.id.checkbox_container)

                    titleView.text = component.label

                    val selectedValues = mutableSetOf<Int>()

                    component.values.forEachIndexed { index, value ->
                        val checkBox = CheckBox(this@FormActivity).apply {
                            text = value.first
                            textSize = 16f
                            setPadding(16, 8, 16, 8)
                        }

                        checkBox.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                selectedValues.add(index)
                            } else {
                                selectedValues.remove(index)
                            }

                            inputValues[component.id] = selectedValues.joinToString(",") { it.toString() }

                            Log.d(
                                "FormActivity",
                                "Component ID: ${component.id}, Selected Values: ${inputValues[component.id]}"
                            )
                        }

                        checkboxContainer.addView(checkBox)
                    }
                    val parentLayout = findViewById<LinearLayout>(R.id.menu_container)
                    parentLayout?.addView(newLayout)

                }
                6 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL

                        // Label TextView
                        val labelTextView = TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                            setTextSize(18f) // Ukuran teks untuk label
                            setTextColor(Color.parseColor("#0A6E44")) // Warna teks untuk label

                            // Atur jarak antara label dan RadioGroup di bawahnya
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(0, 0, 0, 18) // Margin bawah 18dp
                            layoutParams = params
                        }
                        addView(labelTextView)

                        // RadioGroup
                        val radioGroup = RadioGroup(this@FormActivity).apply {
                            orientation = RadioGroup.VERTICAL
                        }

                        component.values.forEachIndexed { index, value ->
                            val radioButton = RadioButton(this@FormActivity).apply {
                                text = value.first
                                textSize = 16f // Ukuran teks untuk RadioButton
                                id = View.generateViewId()
                                buttonTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
                                setTextColor(Color.parseColor("#D32F2F"))
                            }

                            radioButton.setOnCheckedChangeListener { _, isChecked ->
                                // Loop untuk mengubah warna seluruh radio button dalam RadioGroup
                                for (i in 0 until radioGroup.childCount) {
                                    val rb = radioGroup.getChildAt(i) as RadioButton
                                    if (isChecked) {
                                        rb.buttonTintList = ColorStateList.valueOf(Color.BLACK) // Mengubah warna tombol radio button menjadi hitam
                                        rb.setTextColor(Color.BLACK) // Mengubah warna teks menjadi hitam
                                    }
                                }

                                // Jika radio button dipilih, simpan nilai yang dipilih
                                if (isChecked) {
                                    val selectedValue = value.first
                                    if (screen.id == "CCIF001") {
                                        inputRekening[component.label] = selectedValue
                                    }

                                    val valueToSave = when (component.id) {
                                        "CIF05", "CIF17", "CIF07", "CIF21" -> (index + 1).toString()
                                        else -> index.toString()
                                    }
                                    inputValues[component.id] = valueToSave

                                    Log.d(
                                        "FormActivity",
                                        "Component ID: ${component.id}, Selected Value: $selectedValue, Saved Value: $valueToSave"
                                    )
                                }
                            }

                            radioGroup.addView(radioButton)
                        }

                        addView(radioGroup)
                    }
                }

                7 -> {
                    Log.d("FormActivity", "Form ID BUTTON: $formId")
                    val button = Button(this).apply {
                        text = component.label
                        setTextColor(getColor(R.color.white))
                        textSize = 18f
                        val background = when (component.id) {
                            "IYA01" -> getDrawable(R.drawable.button_green)
                            "TDK01" -> getDrawable(R.drawable.button_red)
                            "OTP10" -> getDrawable(R.drawable.button_green)
                            "MSG10" -> getDrawable(R.drawable.button_green)
                            else -> getDrawable(R.drawable.button_yellow)
                        }
                        Log.d("FormActivity", "Screen Button Form: $formId")
                        setBackground(background)
                        setOnClickListener {
                            showLoading()
                            Log.d("FormActivity", "Screen Type: ${screen.type}")
                            val sharedPreferences =
                                getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                            val pinLogin = sharedPreferences.getString("pin", "") ?: ""
                            Log.e("PIN", "PIN LOGIN: $pinLogin")
                            if (component.id == "OK001") {
                                val pinValue = inputValues["PIN"]
                                Log.e("PIN", "PIN INPUT: $pinValue")

                                if (pinLogin != null && pinLogin == pinValue) {
                                    Log.d("FormActivity", "OTP attempts: ${otpAttempts.size}")
                                    if (otpAttempts.size == 0) {
                                        otpAttempts.add(System.currentTimeMillis())
//                                        startOtpTimer()
                                        otpMessage?.let {
                                            try {
                                                // Konversi otpMessage ke JSONObject
                                                val jsonMessage = JSONObject(otpMessage)

                                                ArrestCallerImpl(OkHttpClient()).requestPost(jsonMessage) { responseBody ->
                                                    responseBody?.let { body ->
                                                        lifecycleScope.launch {
                                                            hideLoading()
                                                            Log.e("FormActivity", "")
                                                            val screenJson = JSONObject(body)
                                                            val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                                            handleScreenType(newScreen)
                                                        }
                                                    }?: run {
                                                        Log.e("FormActivity", "Response gagal")
                                                        lifecycleScope.launch {
                                                            withContext(Dispatchers.Main) {
                                                                hideLoading()
                                                            }
                                                        }
                                                        showPopupGagal(
                                                            "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                        )
                                                    }
                                                }
                                            } catch (e: JSONException) {
                                                // Tangani kesalahan parsing jika otpMessage bukan JSON yang valid
                                                Log.e("FormActivity", "Invalid JSON format: ${e.message}")
                                            }
                                        }
                                    } else if (otpAttempts.size > 0 && otpAttempts.size < 2) {
                                        resendOtp(screen)
                                        otpMessage?.let {
                                            try {
                                                // Konversi otpMessage ke JSONObject
                                                val jsonMessage = JSONObject(otpMessage)

                                                ArrestCallerImpl(OkHttpClient()).requestPost(jsonMessage) { responseBody ->
                                                    responseBody?.let { body ->
                                                        lifecycleScope.launch {
                                                            hideLoading()
                                                            Log.e("FormActivity", "")
                                                            val screenJson = JSONObject(body)
                                                            val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                                            handleScreenType(newScreen)
                                                        }
                                                    }?: run {
                                                        Log.e("FormActivity", "Response gagal")
                                                        lifecycleScope.launch {
                                                            withContext(Dispatchers.Main) {
                                                                hideLoading()
                                                            }
                                                        }
                                                        showPopupGagal(
                                                            "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                        )
                                                    }
                                                }
                                            } catch (e: JSONException) {
                                                // Tangani kesalahan parsing jika otpMessage bukan JSON yang valid
                                                Log.e("FormActivity", "Invalid JSON format: ${e.message}")
                                            }
                                        }
                                    }else if (otpAttempts.size == 2) {
                                        otpAttempts.add(System.currentTimeMillis())
                                        otpMessage?.let {
                                            try {
                                                // Konversi otpMessage ke JSONObject
                                                val jsonMessage = JSONObject(otpMessage)

                                                ArrestCallerImpl(OkHttpClient()).requestPost(jsonMessage) { responseBody ->
                                                    responseBody?.let { body ->
                                                        lifecycleScope.launch {
                                                            hideLoading()
                                                            Log.e("FormActivity", "")
                                                            val screenJson = JSONObject(body)
                                                            val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                                            handleScreenType(newScreen)
                                                        }
                                                    }?: run {
                                                        Log.e("FormActivity", "Response gagal")
                                                        lifecycleScope.launch {
                                                            withContext(Dispatchers.Main) {
                                                                hideLoading()
                                                            }
                                                        }
                                                        showPopupGagal(
                                                            "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                        )
                                                    }
                                                }
                                            } catch (e: JSONException) {
                                                // Tangani kesalahan parsing jika otpMessage bukan JSON yang valid
                                                Log.e("FormActivity", "Invalid JSON format: ${e.message}")
                                            }
                                        }
                                    }else if (otpAttempts.size == 3) {
                                        resetTimer(5 * 60 * 1000)
                                        otpAttempts.add(System.currentTimeMillis())
                                        val remainingTime = remainingMillis
                                        if (remainingTime > 0) {
                                            hideLoading()
                                            if (okButtonPressCount >= 4 || otpAttempts.size >= 3) {
                                                val minutesRemaining = remainingTime / 60000
                                                val secondsRemaining =
                                                    (remainingTime % 60000) / 1000
                                                hideLoading()
                                                val intentPopup = Intent(
                                                    this@FormActivity,
                                                    PopupActivity::class.java
                                                ).apply {
                                                    putExtra("LAYOUT_ID", R.layout.pop_up_warning)
                                                    putExtra(
                                                        "MESSAGE_BODY",
                                                        "Anda sudah melebihi batas limit pengiriman OTP! Mohon tunggu $minutesRemaining menit dan $secondsRemaining detik sebelum mengirim OTP kembali."
                                                    )
                                                    putExtra("RETURN_TO_ROOT", false)
                                                }
                                                startActivity(intentPopup)
                                            }
                                        }
                                    } else {
                                        val currentTime = System.currentTimeMillis()
                                        Log.d("FormActivity", "Current time: $currentTime")
                                        val remainingTime = remainingMillis

                                        if (remainingTime > 0) {
                                            if (okButtonPressCount >= 4 || otpAttempts.size >= 3) {
                                                val minutesRemaining = remainingTime / 60000
                                                val secondsRemaining =
                                                    (remainingTime % 60000) / 1000
                                                hideLoading()
                                                val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                                    putExtra("LAYOUT_ID", R.layout.pop_up_warning)
                                                    putExtra("MESSAGE_BODY", "Anda sudah melebihi batas limit pengiriman OTP! Mohon tunggu $minutesRemaining menit dan $secondsRemaining detik sebelum mengirim OTP kembali.")
                                                    putExtra("RETURN_TO_ROOT", false)
                                                }
                                                startActivity(intentPopup)
                                            }
                                        } else {
                                            otpAttempts.clear()
                                            otpAttempts.add(System.currentTimeMillis())
                                            otpMessage?.let {
                                                try {
                                                    // Konversi otpMessage ke JSONObject
                                                    val jsonMessage = JSONObject(otpMessage)

                                                    ArrestCallerImpl(OkHttpClient()).requestPost(jsonMessage) { responseBody ->
                                                        responseBody?.let { body ->
                                                            lifecycleScope.launch {
                                                                hideLoading()
                                                                Log.e("FormActivity", "")
                                                                val screenJson = JSONObject(body)
                                                                val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                                                handleScreenType(newScreen)
                                                            }
                                                        }?: run {
                                                            Log.e("FormActivity", "Response gagal")
                                                            lifecycleScope.launch {
                                                                withContext(Dispatchers.Main) {
                                                                    hideLoading()
                                                                }
                                                            }
                                                            showPopupGagal(
                                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                            )
                                                        }
                                                    }
                                                } catch (e: JSONException) {
                                                    // Tangani kesalahan parsing jika otpMessage bukan JSON yang valid
                                                    Log.e("FormActivity", "Invalid JSON format: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    hideLoading()
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        handleFailedPinAttempt()
                                    }
                                }

                            }else if(component.id == "OK002"){
                                val pinValue = inputValues["PIN"]
                                Log.e("PIN", "PIN INPUT: $pinValue")
                                Log.e("PIN", "PIN Login: $pinLogin")
                                if (pinLogin != null && pinLogin == pinValue) {
                                    handleButtonClick(component, screen)
                                } else {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        handleFailedPinAttempt()
                                    }
                                }
                            } else if (formId == "LS00001") {
                                val allErrors = mutableListOf<String>()
                                screen?.comp?.forEach { comp ->
                                    if (comp.type == 2 || comp.type == 3) {
                                        Log.d ("Validate", "Validate")
                                        allErrors.addAll(validateInput(comp))
                                    }
                                }
                                if (allErrors.isNotEmpty()) {
                                    hideLoading()
                                }else{
                                    hideLoading()
                                    changePassword()
                                }
                            } else if (formId == "LS00002") {
                                val allErrors = mutableListOf<String>()
                                screen?.comp?.forEach { comp ->
                                    if (comp.type == 2 || comp.type == 3) {
                                        Log.d ("Validate", "Validate")
                                        allErrors.addAll(validateInput(comp))
                                    }
                                }
                                if (allErrors.isNotEmpty()) {
                                    hideLoading()
                                }else{
                                    hideLoading()
                                    changePin()
                                }
                            } else if (formId == "CHD0001") {
                                val allErrors = mutableListOf<String>()
                                screen?.comp?.forEach { comp ->
                                    if (comp.type == 2 || comp.type == 3) {
                                        Log.d ("Validate", "Validate")
                                        allErrors.addAll(validateInput(comp))
                                    }
                                }
                                if (allErrors.isNotEmpty()) {
                                    hideLoading()
                                }else{
                                    hideLoading()
                                    changeDevice()
                                }
                            } else if (formId == "LPW0000" && component.id != "OTP10") {
                                forgotPassword()
                            } else {
                                handleButtonClick(component, screen)
                            }
                        }
                    }
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(20, 20, 20, 20)
                    }

                    if (component.id == "KM005" || component.id == "MSG10") {
                        // Ensure buttontf is properly initialized and add button to it
                        val buttontf = findViewById<LinearLayout>(R.id.button_type_7_container)
                            ?: LinearLayout(this).apply {
                                id = R.id.button_type_7_container
                                orientation = LinearLayout.VERTICAL
                                container.addView(this)
                            }

                        buttontf.addView(button, params)
                    } else {
                        container.addView(button, params)
                    }
                }
                15 -> {
                    val inflater = layoutInflater
                    val otpView = inflater.inflate(R.layout.pop_up_otp, container, false)
                    timerTextView = otpView.findViewById<TextView>(R.id.timerTextView)
                    resendOtpTextView  = otpView.findViewById<TextView>(R.id.tv_resend_otp)
                    val messageOTP = otpView.findViewById<TextView>(R.id.messageOTP)

                    when (screen.id) {
                        "CS00003", "TF00004", "OT002" -> {
                            messageOTP.text = "OTP Berhasil terkirim kepada Nasabah"
                        }
                        "ON001" -> {
                            messageOTP.text = "OTP Berhasil terkirim kepada Agen"
                        }
                        "WS0001" -> {
                            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                            val savedNoAgen = sharedPreferences.getString("merchant_phone", "") ?: ""

                            val displayNumber = when {
                                savedNoAgen.length < 4 -> savedNoAgen
                                else -> {
                                    val startDigits = savedNoAgen.substring(0, 3)
                                    val endDigits = savedNoAgen.substring(savedNoAgen.length - 2)
                                    val middleLength = savedNoAgen.length - 4
                                    val middleStars = "*".repeat(middleLength)
                                    "$startDigits$middleStars$endDigits"
                                }
                            }

                            messageOTP.text = "OTP Berhasil Terkirim ke Nomor $displayNumber"
                        }
                        else -> {
                            messageOTP.text = ""
                        }
                    }

                    resendOtpTextView?.let {
                        Log.e("FormActivity", "RESEND OTP NOT null.")
                        it.setOnClickListener {
                            Log.d("FormActivity", "OTP attempts2 : ${otpAttempts.size}")
                            if (otpAttempts.size < 3) {
                                resendOtp(screen)
                                resetTimer(120000)
                            } else if (otpAttempts.size == 3) {
                                otpAttempts.add(System.currentTimeMillis())
                                Toast.makeText(this@FormActivity, "Anda sudah melebihi batas limit OTP. Mohon tunggu 5 menit.", Toast.LENGTH_LONG).show()
                                resetTimer(5 * 60 * 1000)
                            } else {
                                Toast.makeText(this@FormActivity, "Mohon tunggu hingga waktu limit berakhir.", Toast.LENGTH_LONG).show()
                            }
                        }

                        val text = "Resend OTP"
                        val spannable = SpannableString(text).apply {
                            setSpan(UnderlineSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        it.text = spannable
                    }

                    Log.e("FormActivity", "OTP ATTEMPTS RESET : ${otpAttempts.size}")
                    if(otpAttempts.size < 4){
                        resetTimer(120000)
                    } else if (otpAttempts.size >= 4) {
                        resetTimer(5 * 60 * 1000)
                    }

                    val otpDigits = listOf(
                        otpView.findViewById<EditText>(R.id.otpDigit1),
                        otpView.findViewById<EditText>(R.id.otpDigit2),
                        otpView.findViewById<EditText>(R.id.otpDigit3),
                        otpView.findViewById<EditText>(R.id.otpDigit4),
                        otpView.findViewById<EditText>(R.id.otpDigit5),
                        otpView.findViewById<EditText>(R.id.otpDigit6)
                    )

                    otpDigits.forEachIndexed { index, digit ->
                        digit.addTextChangedListener(object : TextWatcher {
                            private var isSelfChange = false
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                if (isSelfChange) return
                                if (s?.isNotEmpty() == true) {
                                    isSelfChange = true

                                    if (count > 0 && index < otpDigits.size - 1) {
                                        otpDigits[index + 1].requestFocus()
                                    }
                                    val otpValue = otpDigits.joinToString(separator = "") { it.text.toString() }
                                    inputValues["OTP"] = otpValue

                                    isSelfChange = false
                                }
                            }

                            override fun afterTextChanged(s: Editable?) {}
                        })

                        digit.setOnKeyListener { _, keyCode, event ->
                            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                                if (digit.text.isEmpty() && index > 0) {
                                    otpDigits[index - 1].requestFocus()
                                    otpDigits[index - 1].setText("")
                                }
                            }
                            false
                        }
                    }
                    container.addView(otpView)
                }


                16 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL

                        // TextView untuk label
                        val labelTextView = TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                            setTextSize(18f) // Ukuran teks untuk label
                            setTextColor(Color.parseColor("#0A6E44")) // Warna teks untuk label

                            // Atur jarak antara label dan EditText di bawahnya
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(0, 0, 0, 18) // Margin bawah 18dp
                            layoutParams = params
                        }
                        addView(labelTextView)

                        // EditText untuk input tanggal
                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            background = getDrawable(R.drawable.date_input_error)
                            id = View.generateViewId()
                            tag = component.id
                            inputType = InputType.TYPE_NULL // Menonaktifkan input keyboard
                            setTextSize(16f) // Ukuran teks untuk input
                            setTextColor(ContextCompat.getColor(this@FormActivity, R.color.black)) // Warna teks untuk input

                            setOnClickListener {
                                val calendar = Calendar.getInstance()
                                val year = calendar.get(Calendar.YEAR)
                                val month = calendar.get(Calendar.MONTH)
                                val day = calendar.get(Calendar.DAY_OF_MONTH)

                                val datePickerDialog = DatePickerDialog(
                                    this@FormActivity,
                                    { _, selectedYear, selectedMonth, selectedDay ->
                                        val selectedDate = String.format(
                                            "%04d-%02d-%02d",
                                            selectedYear,
                                            selectedMonth + 1,
                                            selectedDay
                                        )
                                        setText(selectedDate)
                                        inputValues[component.id as String] = selectedDate

                                        if (screen.id == "CCIF001") {
                                            inputRekening[component.label] = inputValues[component.id] ?: ""
                                        }
                                        background = getDrawable(R.drawable.date_input)
                                    },
                                    year, month, day
                                )

                                // Mengecek apakah component.id adalah CIF04
                                if (component.id == "CIF04" || screen.id == "CC0000") {
                                    // Set maksimal tanggal yang bisa dipilih adalah 4 tahun yang lalu
                                    val maxDate = Calendar.getInstance().apply {
                                        add(Calendar.YEAR, -4) // Kurangi 4 tahun dari hari ini
                                    }.timeInMillis
                                    datePickerDialog.datePicker.maxDate = maxDate // Tanggal yang lebih baru dari ini tidak bisa dipilih
                                }

                                // Menampilkan dialog pemilih tanggal
                                datePickerDialog.show()
                            }
                        }
                        addView(editText)
                    }.let { view ->
                        // Tambahkan view dengan parameter tata letak yang sesuai
                        container.addView(
                            view, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(20, 20, 20, 20) // Margin untuk seluruh view
                            }
                        )
                    }
                }

                17 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)

                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        this.layoutParams = layoutParams

                        val signaturePadView = layoutInflater.inflate(R.layout.activity_signature, this, false)
                        val signaturePad = signaturePadView.findViewById<SignaturePad>(R.id.signature_pad)
                        val clearButton = signaturePadView.findViewById<Button>(R.id.clear_button)
                        val saveButton = signaturePadView.findViewById<Button>(R.id.save_button)
                        val titleCapture = signaturePadView.findViewById<TextView>(R.id.JudulTTD)

                        addView(signaturePadView)
                        titleCapture.text = "Tanda Tangan Nasabah"
                        clearButton.setOnClickListener {
                            signaturePad.clear()
                            Log.d("FormActivity", "SignaturePad cleared")
                        }

                        saveButton.setOnClickListener {
                            if (!signaturePad.isEmpty) {
                                val signatureBitmap = signaturePad.signatureBitmap
                                Log.d("FormActivity", "Signature bitmap width: ${signatureBitmap.width}, height: ${signatureBitmap.height}")

                                // Buat file dengan nama TTD_NIK
                                val fileName = "TTD_${nikValue ?: "unknown"}.png"
                                signatureFile = saveImageToFile(signatureBitmap, fileName)

                                if (signatureFile != null && signatureFile!!.exists()) {
                                    Log.d("FormActivity", "File tanda tangan berhasil disimpan: ${signatureFile?.absolutePath}")
                                    Toast.makeText(this@FormActivity, "Signature saved successfully: $fileName", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e("FormActivity", "Gagal menyimpan file tanda tangan.")
                                    Toast.makeText(this@FormActivity, "Failed to save signature.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this@FormActivity, "Please provide a signature.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                18 -> {
                    val fragmentContainerId = View.generateViewId()

                    // Create FragmentContainerView programmatically
                    val fragmentContainerView = FragmentContainerView(this).apply {
                        id = fragmentContainerId
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(20, 20, 20, 20)
                        }
                    }

                    // Add FragmentContainerView to the container layout
                    container.addView(fragmentContainerView)

                    // Add CameraFragment to the FragmentContainerView
                    if (supportFragmentManager.findFragmentByTag(component.id) == null) {
                        val cameraFragment = CameraFragment.newInstance(component.id, nikValue)
                        supportFragmentManager.beginTransaction()
                            .replace(fragmentContainerId, cameraFragment, component.id)
                            .commit()
                    }else{
                        null
                    }
                }

                19 -> {
                    val inflater = layoutInflater

                    val pinView = inflater.inflate(R.layout.activity_pin, container, false)

                    val pinDigit1 = pinView.findViewById<EditText>(R.id.pinDigit1)
                    val pinDigit2 = pinView.findViewById<EditText>(R.id.pinDigit2)
                    val pinDigit3 = pinView.findViewById<EditText>(R.id.pinDigit3)
                    val pinDigit4 = pinView.findViewById<EditText>(R.id.pinDigit4)
                    val pinDigit5 = pinView.findViewById<EditText>(R.id.pinDigit5)
                    val pinDigit6 = pinView.findViewById<EditText>(R.id.pinDigit6)

                    val pinDigits = listOf(pinDigit1, pinDigit2, pinDigit3, pinDigit4, pinDigit5, pinDigit6)

                    pinDigits.forEach { digit ->
                        digit.transformationMethod = PasswordTransformationMethod.getInstance()
                        digit.filters = arrayOf(InputFilter.LengthFilter(1))
                    }

                    fun handlePinInput(index: Int, pinDigits: List<EditText>) {
                        val digit = pinDigits[index]

                        digit.addTextChangedListener(object : TextWatcher {
                            private var isSelfChange = false
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                            override fun afterTextChanged(s: Editable?) {
                                if (isSelfChange) return
                                if (s?.isNotEmpty() == true) {
                                    isSelfChange = true
                                    digit.transformationMethod = null
                                    digit.setText(s.toString())
                                    digit.transformationMethod = PasswordTransformationMethod.getInstance()
                                    digit.setSelection(digit.text.length)

                                    if (index < pinDigits.size - 1) {
                                        pinDigits[index + 1].requestFocus()
                                    }
                                    isSelfChange = false
                                }
                                val pinValue = pinDigits.joinToString(separator = "") { it.text.toString() }
                                inputValues["PIN"] = pinValue
                            }
                        })
                        digit.setOnKeyListener { _, keyCode, event ->
                            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                                if (digit.text.isEmpty() && index > 0) {
                                    val previousDigit = pinDigits[index - 1]
                                    previousDigit.requestFocus()
                                    previousDigit.setText("")
                                }
                            }
                            false
                        }
                    }
                    pinDigits.forEachIndexed { index, _ ->
                        handlePinInput(index, pinDigits)
                    }
                    pinDigit6.setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                            if (pinDigit6.text.isEmpty()) {
                                pinDigit5.requestFocus()
                                pinDigit5.setText("")
                            }
                        }
                        false
                    }
                    val resendPinTextView = pinView.findViewById<TextView>(R.id.tvForgotPin)
                    resendPinTextView?.let {
                        Log.e("FormActivity", "Forget PIN NOT null.")
                        it.setOnClickListener {
                            lifecycleScope.launch {
                                var forgotScreen = "P000002"
                                var formValue =
                                    StorageImpl(applicationContext).fetchForm(forgotScreen)
                                if (formValue.isNullOrEmpty()) {
                                    formValue = withContext(Dispatchers.IO) {
                                        ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                            forgotScreen
                                        )
                                    }
                                    Log.i("FormActivity", "Fetched pinValue: $formValue")
                                }
                                pickOTP = null
                                Log.i("FormActivity", "setup pick otp saat ini: $pickOTP")
                                setupScreen(formValue)
                            }
                        }
                        val text = "Lupa PIN?"
                        val spannable = SpannableString(text).apply {
                            setSpan(UnderlineSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        it.text = spannable
                    }

                    container.addView(pinView)

                }

                22 -> {
                    val inflater = LayoutInflater.from(this@FormActivity)
                    val newLayout = inflater.inflate(R.layout.activity_akad, null) as LinearLayout

                    val akadTitle = newLayout.findViewById<TextView>(R.id.akad_title)
                    val akadDescription = newLayout.findViewById<TextView>(R.id.akad_description)
                    val btnPilih = newLayout.findViewById<Button>(R.id.btn_pilih)
                    val toggleButton = newLayout.findViewById<ImageView>(R.id.toggle_button)

                    if (akadTitle != null && akadDescription != null && btnPilih != null && toggleButton != null) {
                        when (component.id) {
                            "AKD01" -> {
                                akadTitle.text = "Mudharabah Muthlaqah"
                                akadDescription.text = "Akad Mudharabah Muthlaqah adalah jenis akad Mudharabah dimana Nasabah " +
                                        "selaku shahibul maal memberikan kebebasan penuh bagi Bank selaku mudharib (pengelola dana) " +
                                        "untuk menginvestasikan dananya sesuai prinsip syariah dengan perjanjian nisbah bagi hasil yang disepakati di awal."
                            }
                            "AKD02" -> {
                                akadTitle.text = "Wadi’ah yad Ad Dhamanah"
                                akadDescription.text = "Akad Wadi’ah yad Ad Dhamanah Jenis akad Wadiah " +
                                        "dimana Nasabah selaku pemilik dana menitipkan dananya kepada Bank " +
                                        "selaku penerima titipan dimana Bank diberikan kebebasan untuk " +
                                        "memanfaatkan titipan tersebut dengan memperhatikan prinsip kehati-hatian dan sesuai prinsip syariah"
                            }
                            else -> {
                                akadTitle.text = "Unknown"
                                akadDescription.text = "Description not available"
                            }
                        }

                        // Adjust margins and paddings
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.setMargins(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 16.dpToPx())
                        newLayout.layoutParams = layoutParams

                        val titleParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        titleParams.setMargins(0, 0, 0, 8.dpToPx())
                        akadTitle.layoutParams = titleParams

                        val descriptionParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        descriptionParams.setMargins(0, 0, 0, 16.dpToPx())
                        akadDescription.layoutParams = descriptionParams

                        val buttonParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        buttonParams.setMargins(0, 8.dpToPx(), 0, 0)
                        btnPilih.layoutParams = buttonParams

                        toggleButton.setOnClickListener {
                            if (akadDescription.visibility == View.GONE) {
                                akadDescription.visibility = View.VISIBLE
                                btnPilih.visibility = View.VISIBLE
                                toggleButton.setImageResource(R.mipmap.arrow)
                            } else {
                                akadDescription.visibility = View.GONE
                                btnPilih.visibility = View.GONE
                                toggleButton.setImageResource(R.mipmap.arrow)
                            }
                        }

                        btnPilih.setOnClickListener {
                            val selectedAkad = akadTitle.text.toString()

                            val akadDescriptionValue = when (selectedAkad) {
                                "Mudharabah Muthlaqah" -> "Mudharabah adalah akad kerja sama dimana Nasabah selaku pemilik dana (shahibul maal) dan Bank " +
                                        "sebagai mudharib (pengelola dana) dengan perjanjian nisbah bagi hasil yang disepakati di awal."
                                "Wadiah yad Ad dhamanah" -> "Wadiah adalah akad titipan dimana Nasabah " +
                                        "selaku pemilik dana menitipkan dananya dan Bank selaku pihak yang menerima " +
                                        "amanah titipan dari nasabah untuk menjaga dana yang dititip nasabah tersebut"
                                else -> "Description not available"
                            }

                            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putString("akad", selectedAkad)
                            editor.putString("penjelasan", akadDescriptionValue) // Save akadDescriptionValue
                            editor.apply()

                            val compValueList: MutableList<ComponentValue> = component.compValues?.compValue?.toMutableList() ?: mutableListOf()

                            if (component.id == "AKD03") {
                                val savedAkad = sharedPreferences.getString("akad", "") ?: ""
                                val existingCompValue = compValueList.firstOrNull { it.print == "AKD03" }
                                if (existingCompValue != null) {
                                    existingCompValue.value = savedAkad
                                } else {
                                    compValueList.add(ComponentValue(print = "AKD03", value = savedAkad))
                                }
                                component.compValues = CompValues(compValueList)
                                Log.d("FormActivity", "Updated AKD03 with selected Akad: $savedAkad")
                            }

                            if (component.id == "AKD04") {
                                val penjelasanAkad = sharedPreferences.getString("penjelasan", "") ?: ""
                                val existingCompValue = compValueList.firstOrNull { it.print == "AKD04" }
                                if (existingCompValue != null) {
                                    existingCompValue.value = penjelasanAkad
                                } else {
                                    compValueList.add(ComponentValue(print = "AKD04", value = penjelasanAkad))
                                }
                                component.compValues = CompValues(compValueList)
                                Log.d("FormActivity", "Updated AKD04 with description: $penjelasanAkad")
                            }

                            val intent = Intent(this@FormActivity, FormActivity::class.java).apply {
                                putExtra(Constants.KEY_FORM_ID, "CACIF01")
                                putExtra("SELECTED_AKAD", selectedAkad)
                            }
                            startActivity(intent)
                            Log.d("Selected Value", "SELECTED VALUE : $selectedAkad")
                        }

                        val parentLayout = findViewById<LinearLayout>(R.id.menu_container)
                        parentLayout?.addView(newLayout)
                    } else {
                        Log.e("FormActivity", "One or more views are null in activity_akad layout")
                    }
                }

                else -> {
                    null
                }
            }.let { view ->
                (view as? View)?.let {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(20, 20, 20, 20)
                    }
                    it.layoutParams = params

                    when {
                        component.type == 7 && (component.id == "KM005"||component.id == "MSG10") -> {
                            if (buttonContainer == null) {
                                val newButtontf = LinearLayout(this@FormActivity).apply {
                                    id = View.generateViewId()
                                    orientation = LinearLayout.VERTICAL
                                }
                                container.addView(newButtontf)
                                buttonContainer = newButtontf
                            }
                            if (it is Button) {
                                buttonContainer?.addView(it)
                            } else {
                                Log.e(
                                    "FormActivity",
                                    "View is not a Button, skipping addition to buttonContainer"
                                )
                            }
                        }

                        else -> {
                            container.addView(it)
                        }
                    }
                }
            }
            Log.d("INPUT REKENING", "INPUT REKENING : $inputRekening")
        }
        processedTypes.clear()
        Log.d("Proses", "proses clear : $processedTypes")
    }

    fun findValueInInputRekening(label: String): String? {
        return inputRekening.entries.find { it.key == label }?.value
    }

    private fun getStatusColor(context: Context, status: String): Int {
        return when (status) {
            "0", "1"-> ContextCompat.getColor(context, R.color.blue)
            "2" -> ContextCompat.getColor(context, R.color.green)
            "3" -> ContextCompat.getColor(context, R.color.red)
            else -> ContextCompat.getColor(context, R.color.black)
        }
    }

    private fun refreshDataTransfer(data: Map<String, List<JSONObject>>, container: LinearLayout, context: Context) {
        container.removeAllViews()

        data.forEach { (date, historyList) ->
            container.addView(TextView(context).apply {
                text = date
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                setTextColor(Color.parseColor("#0A6E44"))
            })

            historyList.forEach { history ->
                val replyTime = history.optString("reply_time", "")
                val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                val targetFormat = SimpleDateFormat("dd MM yyyy HH:mm", Locale.getDefault())
                val formattedReplyTime: String = try {
                    val date = originalFormat.parse(replyTime)
                    targetFormat.format(date)
                } catch (e: ParseException) {
                    Log.e("FormActivity", "Error parsing date: ${e.message}")
                    replyTime
                }

                val status = history.optString("status", "")
                val requestMessage = history.getString("request_message")

                val no_rek = history.getString("no_rek")
                val nama_rek = history.getString("nama_rek")
                val nominal = history.getString("nominal")

                val formatTrans = "$no_rek - $nama_rek \n $nominal"

                val requestMessageJson = JSONObject(requestMessage.trim())
                val msgObject = requestMessageJson.getJSONObject("msg")

                val msgId = msgObject.getString("msg_id")
                val msgSi = msgObject.getString("msg_si")

                val actionText = when (msgSi) {
                    "T00002" -> "Transfer"
                    "OTT001" -> "Tarik Tunai"
                    "OT0001" -> "Setor Tunai"
                    else -> msgSi
                }

                val statusTrans = when (status) {
                    "00" -> "Berhasil"
                    else -> "Gagal"
                }

                val statusColor = when (status) {
                    "00" -> ContextCompat.getColor(context, R.color.green)
                    else -> ContextCompat.getColor(context, R.color.red)
                }

                val itemView = LayoutInflater.from(context).inflate(R.layout.item_history, null).apply {
                    setOnClickListener {
                        // Handle item click event
                    }
                }

                itemView.findViewById<TextView>(R.id.text_action).text = actionText
                itemView.findViewById<TextView>(R.id.text_format_trans).text = formatTrans
                itemView.findViewById<TextView>(R.id.text_status).apply {
                    text = statusTrans
                    setTextColor(statusColor)
                }
                itemView.findViewById<TextView>(R.id.text_reply_time).text = formattedReplyTime

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16.dpToPx()) // Add margin between items
                }

                container.addView(itemView, params)
            }
        }
    }

    private fun refreshData(filteredDataList: List<JSONObject>, layout: LinearLayout, context: Context) {
        layout.removeAllViews() // Clear existing views

        // Group data by date
        val groupedData = filteredDataList.groupBy {
            val requestTime = it.getString("request_time")
            requestTime.split(" ").getOrNull(0) ?: "Unknown Date"
        }

        // Add grouped data to the layout
        groupedData.forEach { (date, nasabahList) ->
            layout.addView(TextView(context).apply {
                text = date
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                setTextColor(Color.parseColor("#0A6E44")) // Warna teks date
            })

            nasabahList.forEach { nasabah ->
                // Inflate the item view layout
                val itemView = LayoutInflater.from(context).inflate(R.layout.nasabah_item, null) as LinearLayout

                val namaLengkap = nasabah.getString("nama_lengkap")
                val noIdentitas = nasabah.getString("no_identitas")
                val status = nasabah.getString("status")
                val waktu = nasabah.getString("request_time").split(" ").getOrNull(1) ?: "Unknown Time"

                itemView.findViewById<TextView>(R.id.textViewNamaLengkap).text = namaLengkap
                itemView.findViewById<TextView>(R.id.textViewNoIdentitas).text = noIdentitas
                itemView.findViewById<TextView>(R.id.textViewWaktu).text = waktu

                val statusTextView = itemView.findViewById<TextView>(R.id.textViewStatus)
                statusTextView.text = when (status) {
                    "0", "1" -> "Sedang Diproses"
                    "2" -> "Disetujui"
                    "3" -> "Ditolak"
                    else -> "Status Tidak Diketahui"
                }
                statusTextView.setTextColor(getStatusColor(context, status))

                // Add margin to the item view
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16.dpToPx()) // Add bottom margin (adjust as needed)
                }
                itemView.layoutParams = layoutParams

                // Add the item view to the layout
                layout.addView(itemView)
            }
        }
    }

    private fun createFileFromBitmap(bitmap: Bitmap, fileName: String): File {
        val file = File(cacheDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("FormActivity", "File created at: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("FormActivity", "Error saving bitmap to file: ${e.message}")
        }
        return file
    }


    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }


    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        return timeFormat.format(Date())
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    fun getComponentValue(component: Component): String {
        var currentValue = component.compValues?.compValue?.firstOrNull()?.value
        Log.d("FormActivity", "CURRENT VALUE SEBELUMNYA: $currentValue")

        when (component.label) {
            "No Rekening Agen" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val savedNorekening = sharedPreferences.getString("norekening", "").toString()
                if (currentValue == "null" && savedNorekening != "0") {
                    Log.d("FormActivity", "No Rekening Agen diisi dengan nilai: $savedNorekening")
                    currentValue = savedNorekening
                } else {
                    Log.d("FormActivity", "No Rekening Agen sudah terisi dengan: $savedNorekening")
                }
            }
            "Pilihan Akad" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val selectedAkad = sharedPreferences.getString("akad", "") ?: ""
                if (currentValue == "null" && selectedAkad != "0") {
                    Log.d("FormActivity", "Nilai akad AKD03: $selectedAkad")
                    currentValue = selectedAkad
                } else {
                    Log.d("FormActivity", "Pilihan akad terisi dengan: $selectedAkad")
                }
            }
            "Penjelasan Akad" -> {
                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val akadDescriptionValue = sharedPreferences.getString("penjelasan", "")
                if (currentValue == "null" && akadDescriptionValue != "0") {
                    Log.d("FormActivity", "Nilai akad AKD04: $akadDescriptionValue")
                    currentValue = akadDescriptionValue
                } else {
                    Log.d("FormActivity", "Penjelasan akad terisi dengan: $akadDescriptionValue")
                }
            }
            "Nama Rekening Agen" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val Userfullname = sharedPreferences.getString("fullname", "") ?: ""
                if (currentValue == "null" && Userfullname != "0") {
                    Log.d("FormActivity", "Nama Rekening Agen diisi dengan nilai: $Userfullname")
                    currentValue = Userfullname
                } else {
                    Log.d("FormActivity", "Nama Rekening Agen sudah terisi dengan: $Userfullname")
                }
            }
            "NIK" -> {
                if (currentValue == "null" && nikValue != null) {
                    Log.d("FormActivity", "NIK diisi dengan nilai: $nikValue")
                    currentValue = nikValue
                } else {
                    Log.d("FormActivity", "NIK sudah terisi dengan: $currentValue")
                }
            }
            "Kode Agen" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val savedKodeAgen = sharedPreferences.getString("kode_agen", "")?: ""
                if (currentValue == "null" && savedKodeAgen != "0") {
                    Log.d("FormActivity", "Kode Agen diisi dengan nilai: $savedKodeAgen")
                    currentValue = savedKodeAgen
                } else {
                    Log.d("FormActivity", "Kode Agen sudah terisi dengan: $savedKodeAgen")
                }
            }
            else -> {
                currentValue
                Log.d("FormActivity", "Komponen tidak memerlukan pembaruan")
            }
        }

        // Ambil nilai di luar [OI] jika comp_id adalah CB001
        if (component.id == "CB001") {
            currentValue = currentValue?.let {
                Regex("""\[(?:OI|oi)\](\d+)""").find(it)?.groupValues?.get(1)
            }
        }
        Log.d("FormActivity", "CURRENT VALUE : ${currentValue.toString()}")
        return currentValue.toString()
    }

    private fun parseKodeCabangFromResponse(response: String): String? {
        return try {
            val jsonObject = JSONObject(response)
            val screenObject = jsonObject.getJSONObject("screen")
            val compsObject = screenObject.getJSONObject("comps")
            val compsArray = compsObject.getJSONArray("comp")

            for (i in 0 until compsArray.length()) {
                val compObject = compsArray.getJSONObject(i)
                val compId = compObject.getString("comp_id")

                // Check if the component is Kode Cabang
                if (compId == "KC001") {
                    val compValues = compObject.getJSONObject("comp_values")
                    val compValueArray = compValues.getJSONArray("comp_value")
                    if (compValueArray.length() > 0) {
                        return compValueArray.getJSONObject(0).getString("value")
                    }
                    // Return null if no value is present
                    return null
                }
            }
            null
        } catch (e: JSONException) {
            Log.e("FormActivity", "Failed to parse response for Kode Cabang", e)
            null
        }
    }

    private fun areCheckBoxesChecked(): Boolean {
        val checkBoxContainer = findViewById<LinearLayout>(R.id.checkbox_container)
        var isChecked = false

        for (i in 0 until checkBoxContainer.childCount) {
            val child = checkBoxContainer.getChildAt(i)
            if (child is CheckBox && child.isChecked) {
                isChecked = true
                break
            }
        }

        return isChecked
    }

    private fun handleButtonClick(component: Component, screen: Screen?) {
        val allErrors = mutableListOf<String>()
        val allErrorsPassword = mutableListOf<String>()
        screen?.comp?.forEach { comp ->
            Log.d("PICKOTP", "PICK OTP SAAT INI: $pickOTP")
            if (comp.id == "PIL03" && pickOTP.isNullOrEmpty()) {
                allErrors.add("Harus memilih WA atau SMS.")
                Toast.makeText(this, "Harus memilih WA atau SMS.", Toast.LENGTH_SHORT).show()
            }
        }
        if (formId == "AU00001") {
            screen?.comp?.forEach { comp ->
                if (comp.type == 2) {
                    hideLoading()
                    allErrors.addAll(validateInput(comp))
                }
                if (comp.type == 3) {
                    hideLoading()
                    allErrorsPassword.addAll(validateInput(comp))

                    Log.d("PasswordErrors", "All password errors: ${allErrorsPassword.joinToString(", ")}")
                }
            }
            if (allErrors.contains("Password Wajib Diisi") || allErrorsPassword.contains("Password Wajib Diisi")) {
                hideLoading()
                return
            }
        }
        else {
            screen?.comp?.forEach { comp ->
                if (comp.id == "PIL03" && pickOTP.isNullOrEmpty()) {
                    allErrors.add("Harus memilih WA atau SMS.")
                }
                if (comp.type == 2 && screen.id != "PRP0006") {
                    Log.d ("Validate", "Validate")
                    allErrors.addAll(validateInput(comp))
                }
            }
            if (allErrors.isNotEmpty()) {
                hideLoading()
                return
            }
        }

        if (lottieLoading == null) {
            Log.d("FormActivity", "PROGRESS NULL")
        }else{
            Log.d("FormActivity", "PROGRESS NOT NULL")
        }

        if (formId == "AU00001" && component.id != "OTP10") {
            loginUser { isSuccess ->
                if (!isSuccess) {
                    hideLoading()
                    return@loginUser
                }
            }
            return
        }

        else if (screen?.id == "MB81120") {
            val startDateComponent = screen.comp.find { it.id == "SD001" }
            val endDateComponent = screen.comp.find { it.id == "ED001" }

            if (startDateComponent != null && endDateComponent != null) {
                val startDateStr = inputValues[startDateComponent.id]
                val endDateStr = inputValues[endDateComponent.id]
                Log.d("FormActivity", "Start: $startDateStr")
                Log.d("FormActivity", "End: $endDateStr")

                if (startDateStr != null && endDateStr != null) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startDate = dateFormat.parse(startDateStr)
                    val endDate = dateFormat.parse(endDateStr)

                    Log.d("FormActivity", "Parsed Start: $startDate")
                    Log.d("FormActivity", "Parsed End: $endDate")

                    if (startDate != null && endDate != null) {
                        val diffInMillis = endDate.time - startDate.time
                        Log.d("FormActivity", "Difference in Milliseconds: $diffInMillis")
                        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                        Log.d("FormActivity", "Difference in Days: $diffInDays")

                        if (diffInDays > 7) {
                            hideLoading()
                            Toast.makeText(
                                this,
                                "Maksimal Periode Tanggal 7 hari",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                    } else {
                        Log.d("FormActivity", "One or both dates are null after parsing.")
                    }
                } else {
                    Log.d("FormActivity", "Start or end date string is null.")
                }
            } else {
                Log.d("FormActivity", "StartDateComponent or EndDateComponent is null.")
            }
        }

        if (component.id == "KM001"||component.id == "KM005") {
            startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_MENU_ID, "MN00000")
            })
            finish()
        } else if (component.id == "STJ01") {
            if (areCheckBoxesChecked()) {
                lifecycleScope.launch{
                    var pinScreen = "CCIF000"
                    var formValue =
                        StorageImpl(applicationContext).fetchForm(pinScreen)
                    if (formValue.isNullOrEmpty()) {
                        formValue = withContext(Dispatchers.IO) {
                            ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                pinScreen
                            )
                        }
                        Log.i(
                            "FormActivity",
                            "Fetched pinValue: $formValue"
                        )
                    }
                    setupScreen(formValue)
                }
            } else {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                    }
                }
                Toast.makeText(this, "Harap centang setidaknya satu opsi sebelum melanjutkan.", Toast.LENGTH_SHORT).show()
            }

        }else if (component.id == "OUT00") {

            startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_MENU_ID, "LOG0001")
            })
            finish()

        } else if (component.id == "TDK01") {

            startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_MENU_ID, "MN00000")
            })
            finish()
        } else if (component.id == "IYA01") {

            startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_MENU_ID, "AU00001")
            })
            finish()
        }
        else {
            val otpComponent = screen?.comp?.find { it.id == "OTP01" }
            val verifComponent = screen?.comp?.find { it.id == "OTP10" }
            if (otpComponent != null) {
                val otpValue = inputValues["OTP"]
                Log.e("OTP", "OTP: $otpValue")
                Log.e("MSG", "MSG: $msg03Value")
                val currentTime = System.currentTimeMillis()
                Log.e("Time Check", "Current Time: $currentTime")
                val remainingTime = remainingMillis
                Log.e("Time Check", "Remaining Time: $remainingTime")
                if (remainingTime <= 0) {
                    Log.e("Time Check", "Waktu habis. msg03Value di-set null.")
                    msg03Value = null
                }
                if (msg03Value != null && msg03Value == otpValue) {
                    otpAttempts.clear()
                    isOtpValidated = true
                    otpDialog?.dismiss()
                    cancelOtpTimer()
                    Log.e("OTP", "SCREEN SEKARANG: $screen.id")
                    if (screen.id == "WS0001") {
                        Log.e("OTP", "Create Terminal: $otpValue")

                        val sharedPreferences =
                            getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                        val storedImeiTerminal = sharedPreferences.getString("imei", null)
                        Log.e("Imei Terminal", "Imei Terminal Handle : $storedImeiTerminal")

                        // Ambil newPassword dari sharedPreferences
                        val usernameLogin = sharedPreferences.getString("username", "")
                        val emailLogin = sharedPreferences.getString("email", "")
                        val uid = getUniqueID()

                        Log.e("FormActivity", "Emaillll : $emailLogin")
                        if (!usernameLogin.isNullOrEmpty() && !emailLogin.isNullOrEmpty()) {
                            lifecycleScope.launch {
                                try {
                                    showLoading() // Tampilkan loading saat memproses

                                    val responseBody = withContext(Dispatchers.IO) {
                                        try {
                                            webCallerImpl.forgotPassword(usernameLogin, emailLogin, uid)
                                        } catch (e: Exception) {
                                            Log.e("FormActivity", "Error during forgotPassword call", e)
                                            null
                                        }
                                    }

                                    hideLoading() // Sembunyikan loading setelah respons diterima

                                    if (responseBody != null) {
                                        try {
                                            val responseString = responseBody.string()
                                            Log.d("FormActivity", "Full Response Body: $responseString")

                                            val jsonObject = JSONObject(responseString)

                                            // Parsing status dan message dari respons
                                            val status = jsonObject.optBoolean("status", false)
                                            val message = jsonObject.optString("message", "Terjadi kesalahan tidak diketahui.")

                                            if (status) {
                                                Log.d("FormActivity", "Forgot Password success: $message")

                                                // Hapus data di SharedPreferences
                                                sharedPreferences.edit().apply {
                                                    remove("username")
                                                    remove("email")
                                                    remove("nik")
                                                    remove("no_rek")
                                                    apply()
                                                }

                                                // Tampilkan popup berhasil
                                                val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                                    putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                    putExtra("MESSAGE_BODY", message)
                                                    putExtra("RETURN_TO_ROOT", true)
                                                    putExtra(Constants.KEY_FORM_ID, "AU00001")
                                                }
                                                startActivity(intentPopup)
                                            } else {
                                                Log.e("FormActivity", "Failed to reset password: $message")
                                                sharedPreferences.edit().apply {
                                                    remove("username")
                                                    remove("email")
                                                    remove("nik")
                                                    remove("no_rek")
                                                    apply()
                                                }
                                                showPopupGagal(message)
                                            }
                                        } catch (e: JSONException) {
                                            Log.e("FormActivity", "Error parsing JSON response", e)
                                            showPopupGagal("Terjadi kesalahan saat membaca data. Silakan coba lagi.")
                                        }
                                    } else {
                                        Log.e("FormActivity", "No response received from server")
                                        showPopupGagal("Gagal memproses permintaan. Silakan coba lagi.")
                                    }
                                } catch (e: Exception) {
                                    Log.e("FormActivity", "Unexpected error occurred", e)
                                    showPopupGagal("Terjadi kesalahan: ${e.message}")
                                } finally {
                                    hideLoading() // Pastikan loading disembunyikan
                                }
                            }

                        }
                        else {
                            // Lakukan pemeriksaan untuk storedImeiTerminal jika newPassword valid
                            lifecycleScope.launch {
                                val sharedPreferences =
                                    getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                                val status = sharedPreferences.getBoolean(
                                    "statusImei",
                                    false
                                )  // Mengambil status sebagai boolean
                                Log.d("STATUS IMEI", "Status: $status")
                                if (status) {
                                    updateImei()
                                } else {
                                    createTerminalAndLogin(screen)
                                }
                            }
                        }
                    }
                    else if (screen.id == "P000003"){
                        lifecycleScope.launch {
                            try {
                                val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                                val mid = sharedPreferences.getString("mid", null)
                                val token = sharedPreferences.getString("token", "")

                                if (!mid.isNullOrEmpty() && !token.isNullOrEmpty()) {
                                    Log.e("FormActivity", "Mengirim Request Forgot PIN")
                                    WebCallerImpl().forgotPin(mid, token) { success, message ->
                                        val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                            if (success) {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", "PIN Baru Telah Dikirim Melalui Email. Silahkan Cek Email dan Lakukan Login Kembali.")
                                                putExtra("RETURN_TO_ROOT", true)
                                                putExtra(Constants.KEY_FORM_ID, "AU00001")
                                            } else {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                putExtra("MESSAGE_BODY", message ?: "Terjadi Kesalahan Dalam Proses Lupa PIN. Silahkan Coba Kembali atau Hubungi Call Center.")
                                                putExtra("RETURN_TO_ROOT", true)
                                            }
                                        }
                                        startActivity(intentPopup)
                                    }
                                } else {
                                    Log.e("FormActivity", "MID or Token is null or empty")
                                }
                            } catch (e: Exception) {
                                Log.e("FormActivity", "Error during change PIN request", e)
                                val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                    putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                    putExtra("MESSAGE_BODY", "Terjadi Kesalahan Dalam Proses Lupa PIN. Silahkan Coba Kembali atau Hubungi Call Center.")
                                    putExtra("RETURN_TO_ROOT", true)
                                }
                                startActivity(intentPopup)
                            }
                        }
                    }else{
                        val messageBody = createMessageBody(screen)
                        if (messageBody != null) {
                            Log.d("FormActivity", "Message Body: $messageBody")
                            ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                responseBody?.let { body ->
                                    lifecycleScope.launch {
                                        hideLoading()
                                        val screenJson = JSONObject(body)
                                        val newScreen: Screen =
                                            ScreenParser.parseJSON(screenJson)
                                        Log.e("FormActivity", "SCREEN ${screen.id} ")
                                        Log.e("FormActivity", "NEW SCREEN ${newScreen.id} ")
                                        if (screen.id == "CCIF003" && newScreen.id == "000000D") {
                                            val message =
                                                newScreen?.comp?.find { it.id == "0000A" }
                                                    ?.compValues?.compValue?.firstOrNull()?.value
                                                    ?: "Unknown error"
                                            newScreen.id = "RCCIF02"
                                            var newScreenId = newScreen.id
                                            var formValue =
                                                StorageImpl(applicationContext).fetchForm(
                                                    newScreenId
                                                )
                                            if (formValue.isNullOrEmpty()) {
                                                formValue = withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                        newScreenId
                                                    )
                                                }
                                                Log.i(
                                                    "FormActivity",
                                                    "Fetched formValue: $formValue"
                                                )
                                            }
//                                            val url =
//                                                "http://16.78.84.90:8080/ARRest/fileupload"
                                            val url =
                                                "https://lakupandai.bankntbsyariah.co.id/ARRest/fileupload"
                                            // Find instances of CameraFragment to access captured files
                                            val photoFragment = supportFragmentManager.findFragmentByTag("SIG02") as? CameraFragment
                                            val ktpFragment = supportFragmentManager.findFragmentByTag("SIG03") as? CameraFragment

                                            // Upload Foto Nasabah
                                            photoFragment?.capturedFile?.let { file ->
                                                uploadImageFile(file, url)
                                                Log.d("Upload", "Foto Orang uploaded: ${file.absolutePath}")
                                            } ?: Log.d("Upload", "Foto Orang is null")

                                            // Upload Foto KTP
                                            ktpFragment?.capturedFile?.let { file ->
                                                uploadImageFile(file, url)
                                                Log.d("Upload", "KTP uploaded: ${file.absolutePath}")
                                            } ?: Log.d("Upload", "KTP is null")
                                            if (signatureFile != null) {
                                                // Mengunggah tanda tangan
                                                signatureFile?.let { file ->
                                                    uploadImageFile(file, url)
                                                    Log.d(
                                                        "Foto",
                                                        "Signature Ada : $signatureFile"
                                                    )
                                                }
                                            } else {
                                                Log.d("Foto", "Tanda Tangan Kosong")
                                            }
                                            setupScreen(formValue)

                                            val intent = Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", message)
                                                putExtra("RETURN_TO_ROOT", false)
                                            }
                                            startActivity(intent)
                                        } else if (screen.id == "CCIF000" && newScreen.id == "000000F") {
                                            newScreen.id = "CCIF001"
                                            var newScreenId = newScreen.id
                                            var formValue =
                                                StorageImpl(applicationContext).fetchForm(
                                                    newScreenId
                                                )
                                            if (formValue.isNullOrEmpty()) {
                                                formValue = withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                        newScreenId
                                                    )
                                                }
                                                Log.i(
                                                    "FormActivity",
                                                    "Fetched formValue: $formValue"
                                                )
                                            }
                                            setupScreen(formValue)

                                        } else if (screen.id == "CCIF000" && newScreen.id != "000000F") {
                                            // Menampilkan pop-up gagal dengan pesan "NIK sudah terdaftar"
                                            val intent = Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                putExtra("MESSAGE_BODY", "NIK sudah terdaftar")
                                            }
                                            startActivity(intent)
                                        } else if (isSvcBiller == true) {
                                            Log.e("FormActivity", "Masuk Service Biller")
                                            if (newScreen.id != "000000F") {
                                                Log.e("FormActivity", "Debit Berhasil")
                                                val messageBody = createMessageBody(newScreen)
                                                if (messageBody != null) {
                                                    var billerScreen = newScreen
                                                    Log.d(
                                                        "FormActivity",
                                                        "Message Body Biller: $messageBody"
                                                    )
                                                    Log.d(
                                                        "FormActivity",
                                                        "MAU NEW SCREEN: $newScreen"
                                                    )
                                                    Log.d(
                                                        "FormActivity",
                                                        "MAU NEW SCREEN.ID: ${newScreen.id}"
                                                    )
                                                    ArrestCallerImpl(OkHttpClient()).requestPost(
                                                        messageBody
                                                    ) { responseBody ->
                                                        Log.d(
                                                            "FormActivity",
                                                            "Callback diterima: $responseBody"
                                                        )
                                                        responseBody?.let { body ->
                                                            Log.d(
                                                                "FormActivity",
                                                                "Response Body: $body, Response : $responseBody"
                                                            )
                                                            lifecycleScope.launch {
                                                                val screenJson =
                                                                    JSONObject(body)
                                                                val newScreen: Screen =
                                                                    ScreenParser.parseJSON(
                                                                        screenJson
                                                                    )
                                                                Log.e(
                                                                    "FormActivity",
                                                                    "SCREEN Biller ${screen.id} "
                                                                )
                                                                Log.e(
                                                                    "FormActivity",
                                                                    "NEW SCREEN Biller ${newScreen.id} "
                                                                )
                                                                if (newScreen.id == "000000F") {
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Biller gagal, reversal"
                                                                    )
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Screen Reversal:${screen.id}"
                                                                    )
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "IS Reversal:$isReversal"
                                                                    )
                                                                    isReversal = true
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "After IS Reversal:$isReversal"
                                                                    )
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Biller Screen:${billerScreen.id}"
                                                                    )
                                                                    val messageBody =
                                                                        createMessageBody(
                                                                            billerScreen
                                                                        )
                                                                    if (messageBody != null) {
                                                                        ArrestCallerImpl(
                                                                            OkHttpClient()
                                                                        ).requestPost(
                                                                            messageBody
                                                                        ) { responseBody ->

                                                                            responseBody?.let { body ->
                                                                                lifecycleScope.launch {
                                                                                    lottieLoading?.visibility =
                                                                                        View.GONE
                                                                                    val screenJson =
                                                                                        JSONObject(
                                                                                            body
                                                                                        )
                                                                                    val newScreen: Screen =
                                                                                        ScreenParser.parseJSON(
                                                                                            screenJson
                                                                                        )
                                                                                    Log.e(
                                                                                        "FormActivity",
                                                                                        "NEW SCREEN Biller ${newScreen.id} "
                                                                                    )
                                                                                    handleScreenType(
                                                                                        newScreen
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    hideLoading()
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Biller berhasil"
                                                                    )
                                                                    handleScreenType(
                                                                        newScreen
                                                                    )
                                                                }
                                                            }
                                                        } ?: run {
                                                            Log.e(
                                                                "FormActivity",
                                                                "Response null atau gagal"
                                                            )
                                                            lifecycleScope.launch {
                                                                withContext(Dispatchers.Main) {
                                                                    hideLoading()
                                                                }
                                                            }
                                                            showPopupGagal(
                                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            handleScreenType(newScreen)
                                        }
                                    }
                                } ?: run {
                                    showPopupGagal(
                                        "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                    )
                                    Log.e("FormActivity", "Failed to fetch response body")
                                }
                            }
                        } else {
                            lifecycleScope.launch {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                            }
                            showPopupGagal(
                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                            )
                            Log.e("FormActivity", "Failed to create message body, request not sent")
                        }
                    }
                } else if (msg03Value == null) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                        }
                    }
                    Toast.makeText(
                        this@FormActivity,
                        "Kode OTP telah kadaluarsa, mohon kirim ulang OTP.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                        }
                    }
                    Toast.makeText(
                        this@FormActivity,
                        "Kode OTP yang dimasukkan salah",
                        Toast.LENGTH_SHORT
                    ).show()
                    findViewById<EditText>(R.id.otpDigit1)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit2)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit3)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit4)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit5)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit6)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit1)?.error = "OTP salah"
                }
            } else {
                val messageBody = screen?.let { createMessageBody(it) }
                val sendOTPComponent = screen?.comp?.find { it.id == "OTP09" }
                val sendPIN = screen?.comp?.find { it.id == "P0005" }
                val sendPengaduan = screen?.comp?.find { it.id == "G0002" }
                Log.d("Form", "Send Pengaduan : $sendPengaduan")
                Log.d("Form", "OTP Component : $sendOTPComponent")
                val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                if(sendPengaduan != null){
                    val bodyPengaduan = screen?.let { createBodyPengaduan(it) }
                    Log.d("FormActivity", "Create Message Pengaduan $bodyPengaduan")

                    bodyPengaduan?.let { sendPengaduanToApi(it) }
                } else if (messageBody != null) {
                    Log.d("FormActivity", "Message Body: $messageBody")
                    if (screen.id == "CCIF001") {
                        lifecycleScope.launch {
                            var cameraScreen = "TEST001"
                            var formValue =
                                StorageImpl(applicationContext).fetchForm(cameraScreen)
                            if (formValue.isNullOrEmpty()) {
                                formValue = withContext(Dispatchers.IO) {
                                    ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                        cameraScreen
                                    )
                                }
                                Log.i(
                                    "FormActivity",
                                    "Fetched cameraScreen: $formValue"
                                )
                            }
                            setupScreen(formValue)
                        }
                    }else if (sendOTPComponent != null || sendPIN != null) {
                        Log.i(
                            "FormActivity",
                            "Masuk PIN"
                        )
                            var pinScreen = "P000001"
                            lifecycleScope.launch {
                                var formValue =
                                    StorageImpl(applicationContext).fetchForm(pinScreen)
                                if (formValue.isNullOrEmpty()) {
                                    formValue = withContext(Dispatchers.IO) {
                                        ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                            pinScreen
                                        )
                                    }
                                    Log.i(
                                        "FormActivity",
                                        "Fetched pinValue: $formValue"
                                    )
                                }
                                setupScreen(formValue)

                                otpMessage = messageBody.toString()
                            }
                    }else{
                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                            responseBody?.let { body ->
                                lifecycleScope.launch {
                                    hideLoading()
                                    Log.e("FormActivity", "")
                                    val screenJson = JSONObject(body)
                                    val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                    Log.e("FormActivity", "SCREEN ${screen.id} ")
                                    Log.e("FormActivity", "NEW SCREEN BUKAN OTP ${newScreen.id} ")
                                    Log.e("FormActivity", "NEW SCREEN ${newScreen} ")
                                    val prodIDComponent = newScreen?.comp?.find { it.id == "PD001" }
                                    // Pengecekkan Rekening BSA
                                    if (prodIDComponent != null) {
                                        Log.e("FormActivity", "ADA PRODID ")
                                        val prodIDvalue =
                                            prodIDComponent.compValues?.compValue?.firstOrNull()?.value
                                        Log.d("FormActivity", "Value of PROID: $prodIDvalue")
                                        when (screen.id) {
                                            // Transfer Sesama Bank & Tarik Tunai & Cek Saldo
                                            "TF00001","FT0001","CS00001" -> {
                                                if (prodIDvalue != "36") {
                                                    val intentPopup = Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Rekening yang digunakan harus rekening BSA."
                                                        )
                                                        putExtra("RETURN_TO_ROOT", false)
                                                    }
                                                    startActivity(intentPopup)
                                                } else {
                                                    handleScreenType(newScreen)
                                                }
                                            }
//                                         Setor Tunai Sekolah
                                            "STS0001" -> {
                                                if (prodIDvalue != "34") {
                                                    val intentPopup = Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Rekening yang digunakan harus rekening sekolah."
                                                        )
                                                        putExtra("RETURN_TO_ROOT", false)
                                                    }
                                                    startActivity(intentPopup)
                                                } else {
                                                    handleScreenType(newScreen)
                                                }
                                            }
                                            // No Rek Nasabah Biller
                                            "PRP0006" -> {
                                                if (prodIDvalue != "36") {
                                                    val intentPopup = Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Rekening yang digunakan harus rekening BSA."
                                                        )
                                                        putExtra("RETURN_TO_ROOT", false)
                                                    }
                                                    startActivity(intentPopup)
                                                } else {
                                                    if (newScreen.id == "PRP0007") {
                                                        Log.e("FormActivity", "Masuk PRP0007")

                                                        var nomorRekening: String? = null
                                                        var namaRekening: String? = null
                                                        var nomorHP: String? = null

                                                        // Loop melalui komponen untuk menemukan Nomor Rekening dan Nama Rekening
                                                        for (component in newScreen.comp) {
                                                            when (component.id) {
                                                                "NR006" -> { // Untuk Nomor Rekening
                                                                    nomorRekening =
                                                                        component.compValues?.compValue?.firstOrNull()?.value
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Nomor Rekening Biller: $nomorRekening"
                                                                    )
                                                                }

                                                                "UNF04" -> { // Untuk Nama Rekening
                                                                    namaRekening =
                                                                        component.compValues?.compValue?.firstOrNull()?.value
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Nama Rekening Biller: $namaRekening"
                                                                    )
                                                                }

                                                                "CIF45" -> { // Untuk Nomor
                                                                    nomorHP =
                                                                        component.compValues?.compValue?.firstOrNull()?.value
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "Nomor Biller: $nomorHP"
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        // Jika kedua nilai sudah ditemukan, simpan ke SharedPreferences
                                                        val sharedPreferences =
                                                            getSharedPreferences(
                                                                "MyAppPreferences",
                                                                Context.MODE_PRIVATE
                                                            )
                                                        if (nomorRekening != null && namaRekening != null) {
                                                            val editor = sharedPreferences.edit()
                                                            editor.putString(
                                                                "norek_biller",
                                                                nomorRekening
                                                            )
                                                            editor.putString(
                                                                "namarek_biller",
                                                                namaRekening
                                                            )
                                                            editor.putString(
                                                                "nomor_biller",
                                                                nomorHP
                                                            )
                                                            editor.apply()
                                                        }
                                                        otpDialog?.dismiss()
                                                        val gson = Gson()
                                                        val screenJson =
                                                            sharedPreferences.getString(
                                                                "screen_biller",
                                                                null
                                                            )
                                                        val screenBiller: Screen =
                                                            gson.fromJson(
                                                                screenJson,
                                                                Screen::class.java
                                                            )
                                                        handleScreenType(screenBiller)
                                                    } else {
                                                        handleScreenType(newScreen)
                                                    }
                                                }
                                            }

                                            else -> {
                                                handleScreenType(newScreen)
                                            }
                                        }
                                    } else {
                                        Log.e("FormActivity", "GAK ADA PRODID ")

                                            if (screen.id == "CCIF003" && newScreen.id == "000000D") {
                                                val message =
                                                    newScreen?.comp?.find { it.id == "0000A" }
                                                        ?.compValues?.compValue?.firstOrNull()?.value
                                                        ?: "Unknown error"
                                                newScreen.id = "RCCIF02"
                                                var newScreenId = newScreen.id
                                                var formValue =
                                                    StorageImpl(applicationContext).fetchForm(
                                                        newScreenId
                                                    )
                                                if (formValue.isNullOrEmpty()) {
                                                    formValue = withContext(Dispatchers.IO) {
                                                        ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                            newScreenId
                                                        )
                                                    }
                                                    Log.i(
                                                        "FormActivity",
                                                        "Fetched formValue: $formValue"
                                                    )
                                                }
                                                setupScreen(formValue)
                                                val intent = Intent(
                                                    this@FormActivity,
                                                    PopupActivity::class.java
                                                ).apply {
                                                    putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                    putExtra("MESSAGE_BODY", message)
                                                    putExtra("RETURN_TO_ROOT", false)
                                                }
                                                startActivity(intent)
                                            } else if (screen.id == "CCIF000" && newScreen.id == "000000F") {
                                                newScreen.id = "CCIF001"
                                                var newScreenId = newScreen.id
                                                var formValue =
                                                    StorageImpl(applicationContext).fetchForm(
                                                        newScreenId
                                                    )
                                                if (formValue.isNullOrEmpty()) {
                                                    formValue = withContext(Dispatchers.IO) {
                                                        ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                            newScreenId
                                                        )
                                                    }
                                                    Log.i(
                                                        "FormActivity",
                                                        "Fetched formValue: $formValue"
                                                    )
                                                }
                                                setupScreen(formValue)
                                            } else if (screen.id == "CCIF000" && newScreen.id != "000000F") {
                                                val intent =
                                                    Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "NIK sudah terdaftar"
                                                        )
                                                    }
                                                startActivity(intent)
                                            } else if (screen.id == "RCS0001" && newScreen.id != "000000F") {
                                                val intent =
                                                    Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra(
                                                            "LAYOUT_ID",
                                                            R.layout.pop_up_berhasil
                                                        )
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Pesan sudah teririm"
                                                        )
                                                    }
                                                startActivity(intent)
                                            } else if (screen.id == "TF00003" && newScreen.id != "000000F") {
                                                val intent =
                                                    Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra(
                                                            "LAYOUT_ID",
                                                            R.layout.pop_up_berhasil
                                                        )
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Pesan sudah teririm"
                                                        )
                                                    }
                                                startActivity(intent)
                                            } else if (screen.id == "BR001" && newScreen.id != "000000F") {
                                                val intent =
                                                    Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra(
                                                            "LAYOUT_ID",
                                                            R.layout.pop_up_berhasil
                                                        )
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Pesan sudah teririm"
                                                        )
                                                    }
                                                startActivity(intent)
                                            } else if (screen.id == "BS001" && newScreen.id != "000000F") {
                                                val intent =
                                                    Intent(
                                                        this@FormActivity,
                                                        PopupActivity::class.java
                                                    ).apply {
                                                        putExtra(
                                                            "LAYOUT_ID",
                                                            R.layout.pop_up_berhasil
                                                        )
                                                        putExtra(
                                                            "MESSAGE_BODY",
                                                            "Pesan sudah teririm"
                                                        )
                                                    }
                                                startActivity(intent)
                                                // BILLER
                                            } else if (isSvcBiller == true) {
                                                showLoading()
                                                Log.e("FormActivity", "Masuk Service Biller")
                                                if (newScreen.id != "000000F") {
                                                    Log.e("FormActivity", "Debit Berhasil")
                                                    val messageBody = createMessageBody(newScreen)
                                                    if (messageBody != null) {
                                                        Log.d(
                                                            "FormActivity",
                                                            "Message Body Biller: $messageBody"
                                                        )
                                                        Log.d(
                                                            "FormActivity",
                                                            "MAU NEW SCREEN: $newScreen"
                                                        )
                                                        Log.d(
                                                            "FormActivity",
                                                            "MAU NEW SCREEN.ID: ${newScreen.id}"
                                                        )
                                                        var billerScreen = newScreen
                                                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                                            Log.d("FormActivity", "Response Body Biller: $responseBody")
                                                            responseBody?.let { body ->
                                                                Log.d("FormActivity", "Body Biller: $body")
                                                                lifecycleScope.launch {
                                                                    hideLoading()
                                                                    val screenJson =
                                                                        JSONObject(body)
                                                                    val newScreen: Screen =
                                                                        ScreenParser.parseJSON(
                                                                            screenJson
                                                                        )
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "SCREEN Biller ${screen.id} "
                                                                    )
                                                                    Log.e(
                                                                        "FormActivity",
                                                                        "NEW SCREEN Biller ${newScreen.id} "
                                                                    )
                                                                    if (newScreen.id == "000000F") {
                                                                        Log.e(
                                                                            "FormActivity",
                                                                            "Biller gagal, reversal"
                                                                        )
                                                                        Log.e(
                                                                            "FormActivity",
                                                                            "Screen Reversal:${screen.id}"
                                                                        )
                                                                        Log.e(
                                                                            "FormActivity",
                                                                            "IS Reversal:$isReversal"
                                                                        )
                                                                        isReversal = true
                                                                        Log.e(
                                                                            "FormActivity",
                                                                            "After IS Reversal:$isReversal"
                                                                        )
                                                                        Log.e(
                                                                            "FormActivity",
                                                                            "Biller Screen:${billerScreen.id}"
                                                                        )
                                                                        val messageBody =
                                                                            createMessageBody(
                                                                                billerScreen
                                                                            )
                                                                        if (messageBody != null) {
                                                                            ArrestCallerImpl(
                                                                                OkHttpClient()
                                                                            ).requestPost(
                                                                                messageBody
                                                                            ) { responseBody ->

                                                                                responseBody?.let { body ->
                                                                                    lifecycleScope.launch {
                                                                                        lottieLoading?.visibility =
                                                                                            View.GONE
                                                                                        val screenJson =
                                                                                            JSONObject(
                                                                                                body
                                                                                            )
                                                                                        val newScreen: Screen =
                                                                                            ScreenParser.parseJSON(
                                                                                                screenJson
                                                                                            )
                                                                                        Log.e(
                                                                                            "FormActivity",
                                                                                            "NEW SCREEN Biller ${newScreen.id} "
                                                                                        )
                                                                                        handleScreenType(
                                                                                            newScreen
                                                                                        )
                                                                                    }
                                                                                }?: run {
                                                                                    Log.e("FormActivity", "Response gagal")
                                                                                    lifecycleScope.launch {
                                                                                        withContext(Dispatchers.Main) {
                                                                                            hideLoading()
                                                                                        }
                                                                                    }
                                                                                    showPopupGagal(
                                                                                        "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Log.e(
                                                                            "FormActivity",
                                                                            "Biller berhasil"
                                                                        )
                                                                        handleScreenType(newScreen)
                                                                    }
                                                                }
                                                            } ?: run {
                                                                Log.e("FormActivity", "Biller tidak ada response")
                                                                lifecycleScope.launch {
                                                                    withContext(Dispatchers.Main) {
                                                                        hideLoading()
                                                                    }
                                                                }
                                                                showPopupGagal(
                                                                    "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    handleScreenType(newScreen)
                                                }
                                            } else {
                                                handleScreenType(newScreen)
                                            }
                                    }
                                }
                            } ?: run {
                                showPopupGagal(
                                    "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                )
                                Log.e("FormActivity", "Failed to fetch response body")
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                        }
                    }
                    showPopupGagal(
                        "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                    )
                    Log.e("FormActivity", "Failed to create message body, request not sent")
                }
            }
        }
    }

    private fun showDatePickerDialog(editText: EditText, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate =
                    String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                editText.setText(selectedDate)
                onDateSelected(selectedDate)
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun getKodeCabangFromPreferences(): String? {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("KODE_CABANG", null)
    }

    private fun saveKodeCabangToPreferences(kodeCabang: String) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("KODE_CABANG", kodeCabang)
        editor.apply() // atau editor.commit()
    }

    private fun createMessageBody(screen: Screen): JSONObject? {
        return try {
            val msg = JSONObject()
            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            val savedNorekening = sharedPreferences.getString("norekening", "") ?: ""
            val savedNamaAgen = sharedPreferences.getString("fullname", "") ?: ""
            val savedKodeAgen = sharedPreferences.getString("kode_agen", "")?: ""

            val savedBranchid = sharedPreferences.getString("branchid", "")?: ""

            val savedAkad = sharedPreferences.getString("akad", "") ?: ""
            val savedToken = sharedPreferences.getString("token", "") ?: ""
            val savedNoAgen = sharedPreferences.getString("merchant_phone", "") ?: ""
//            val imei = sharedPreferences.getString("imei", "")?: ""
            val username = sharedPreferences.getString("username_param", "") ?: ""
            Log.e("FormActivity", "Saved Username: $username")
            Log.e("FormActivity", "Saved Norekening: $savedNorekening")
            Log.e("FormActivity", "Saved Agen: $savedKodeAgen")
            Log.e("FormActivity", "Saved Nama Agen: $savedNamaAgen")
            Log.e("FormActivity", "Saved Kode Cabang: $savedBranchid")
            Log.e("FormActivity", "Saved Akad: $savedAkad")
            Log.e("FormActivity", "Saved Token: $savedToken")
            Log.e("FormActivity", "Saved No Agen: $savedNoAgen")
            Log.e("FormActivity", "Saved Branchid: $savedBranchid")

            // Generate timestamp in the required format
            val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())

            val imei = sharedPreferences.getString("imei", "")?: ""
            Log.e("FormActivity", "Saved Imei: $imei")
            val msgUi = imei
//            val msgUi = "353471045058692"
//            var msgId = msgUi + timestamp
            var msgSi = screen.actionUrl

            Log.d("FormActivity", "msgSi: $msgSi")
            Log.d("FormActivity", "PICK OTP: $pickOTP")
            Log.d("FormActivity", "PICK NRK Create: $pickNRK")

            // Kondisi untuk mengubah msgSi berdasarkan screen.id dan comp_id PIL03
            if (screen.comp.any { it.id == "PIL03" } && pickOTP == "SMS") {
                when (screen.id) {
                    // cek saldo
                    "CS00004" -> msgSi = "N00002"
                    // cek mutasi
                    "MB81124" -> msgSi = "E81122"
                    "TF00002" -> msgSi = "T00003"
                    "TEST001" -> msgSi = "CC0002"
                    "RT001" -> msgSi = "RTT002"
                    "RS001" -> msgSi = "OTN002"
                    "PR00010" -> msgSi = "PPR003"
                    "P000002" -> msgSi = "LPI002"
                    "OPLN000" -> msgSi = "OPLN02"
                    "OBPJS00" -> msgSi = "OBPJS2"
                    "OIND000" -> msgSi = "OIND02"
                    else -> msgSi = screen.actionUrl
                }
            }

            if (screen.title.contains("BL") && pickNRK == "Rekening Nasabah") {
                Log.d("FormActivity", "Ganti MSG SI Rekening Nasabah")
                when (msgSi) {
                    "PLN001" -> msgSi = "OPLN00"
                    "BPJS01" -> msgSi = "OBPJS0"
                    "IND001" -> msgSi = "OIND00"
                    else -> msgSi = "OPR002"
                }
            }

            // Reversal
            Log.d("FormActivity", "MSG Reversal : $isReversal")
            if (isReversal) {
                Log.d("FormActivity", "Ganti MSG Reversal")
                msgSi = "RPR001"
            }

            // Menyimpan nilai komponen ke dalam map
            val componentValues = mutableMapOf<String, String>()
            screen.comp.filter { it.type != 7 && it.type != 15 }.forEach { component ->
                Log.d("FormActivity", "Component: $component")

                when {
                    component.type == 1 && component.label == "Username" -> {
                        componentValues[component.id] = username
                        Log.d("FormActivity", "Updated componentValues with savedUsername for Component ID: ${component.id}")
                    }
                    component.type == 1 && component.label == "No Rekening Agen" -> {
                        componentValues[component.id] = savedNorekening
                        Log.d("FormActivity", "Updated componentValues with savedNorekening for Component ID: ${component.id}")
                    }
                    component.type == 1 && component.label == "Pilihan Akad" -> {
                        componentValues[component.id] = savedAkad
                        Log.d("FormActivity", "Updated componentValues with akad for Component ID: ${component.id}")
                    }
                    component.type == 1 && component.label == "Nama Rekening Agen" -> {
                        componentValues[component.id] = savedNamaAgen
                        Log.d(
                            "FormActivity",
                            "Updated componentValues with savedNamaAgen for Component ID: ${component.id}"
                        )
                    }
                    component.type == 1 && component.label == "Kode Agen" -> {
                        componentValues[component.id] = savedKodeAgen.toString()
                        Log.d("FormActivity", "Kode Agen : $savedKodeAgen")
                    }
                    component.type == 1 && component.label == "No Handphone Agen" -> {
                        componentValues[component.id] = savedNoAgen
                        Log.d("FormActivity", "No Agen : $savedNoAgen")
                    }
                    component.type == 1 && component.label == "NIK" -> {
                        componentValues[component.id] = nikValue ?: ""
                        Log.d("FormActivity", "Updated componentValues with nikValue for Component ID: ${component.id}")
                    }
                    component.type == 1 && component.label == "Kode Cabang" -> {
                        val branchid = sharedPreferences.getString("branchid", "")?: ""
                        componentValues[component.id] = branchid ?: ""
                        Log.d("FormActivity", "Updated componentValues with branchid : ${branchid}")
                    }
                    component.type == 1 && component.label != "NIK" -> {
                        val value = (component.values.get(0)?.second ?: "") as String
                        componentValues[component.id] = value
                        Log.d("FormActivity", "Updated componentValues with value for Component ID: ${component.id}")
                    }
                    else -> {
                        componentValues[component.id] = inputValues[component.id] ?: ""
                    }
                }
                if (component.id == "NRK01") {
                    componentValues["norekening"] = savedNorekening
                    componentValues["namaAgen"] = savedNamaAgen
                }

            }
            val unf01Value = componentValues["AG009"] ?: ""
            val rnr02Value = componentValues["RNR05"] ?: ""
            val sisaSaldo = if (rnr02Value.startsWith("-")) rnr02Value.replace("-", "") else rnr02Value
            val unf03Value = componentValues["UNF05"] ?: ""
            val unf04Value = componentValues["SET10"] ?: ""
            val unf05Value = componentValues["NAR05"] ?: ""
            val rnr06Value = componentValues["TRF30"] ?: ""
            val rnr07Value = componentValues["TRT07"] ?: ""
            val rnr08Value = componentValues["TRF31"] ?: ""
            val rnr09Value = componentValues["TRF30"] ?: ""
            val rnr10Value = componentValues["T0002"] ?: ""
            val rnr11Value = componentValues["SET20"] ?: ""

            when (screen.id) {
                "RCS0001" -> {
                    val currentDate = getCurrentDate()
                    val currentTime = getCurrentTime()
                    componentValues["MSG05"] = "Saldo No. Rek $unf03Value a.n $unf05Value Rp.$sisaSaldo pada $currentDate Waktu: $currentTime."
                }
                "TF00003" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$rnr08Value, dengan No. Rekening: $rnr07Value. Berhasil melakukan Transaksi Transfer kepada $rnr09Value penerima dengan nominal $rnr10Value."
                }
                "BR001" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$unf01Value, dengan No. Rekening: $rnr11Value. Transaksi Tarik berhasil dilakukan."
                }
                "BS001" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$rnr06Value, dengan No. Rekening: $unf04Value. Transaksi Setor berhasil dilakukan."
                }
                else -> {
                    componentValues["MSG05"] = "Pesan tidak diketahui."
                }
            }
            val excludedCompIds = listOf("SIG01", "SIG02", "SIG03", "P0000")

            var msgDt = ""
            Log.d("Screen", "SCREEN CREATE MESSAGE : ${screen.id}")
            if (screen.id == "AU00001") {
                msgDt = "$username|$savedNorekening|$savedNamaAgen|null"
            }else if(screen.id == "TEST001" && msgDtCreateRekening != null){
                msgDt = msgDtCreateRekening as String
            } else {
                msgDt = screen.comp
                    .filter { it.type != 7 && it.type != 15 && it.id != "MSG03" && it.id != "PIL03" && !excludedCompIds.contains(it.id) }
                    .joinToString("|") { component ->
                        componentValues[component.id] ?: ""
                    }
            }
            Log.d("Form", "Component : ${componentValues}")

            // Create Rekening BSA
            if (screen.id == "CCIF001") {
                msgDtCreateRekening = msgDt
            }

            val msgId = manageMsgId(msgSi ?: "", imei, savedToken, timestamp)

            Log.d("FormActivity", "Generated Message ID: $msgId")
            val msgObject = JSONObject().apply {
                put("msg_id", msgId)
                put("msg_ui", msgUi)
                put("msg_si", msgSi)
                put("msg_dt", msgDt)
            }

            msg.put("msg", msgObject)
            lastMessageBody = msg

            // Logging the JSON message details
            Log.d("FormActivity", "Message ID: $msgId")
            Log.d("FormActivity", "Message UI: $msgUi")
            Log.d("FormActivity", "Message SI: $msgSi")
            Log.d("FormActivity", "Message DT: $msgDt")
            Log.d("FormActivity", "Message JSON: ${msg.toString()}")

            msg
        } catch (e: Exception) {
            Log.e("FormActivity", "Failed to create message body", e)
            null
        }
    }

    fun manageMsgId(serviceId: String, imei: String, token:String, timestamp: String): String {
        val msgUi = imei

        // Call the webCaller synchronously (blocking)
        val response = runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    webCallerImpl.getParam2(serviceId, token)

                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error fetching param2: ${e.message}")
                null
            }
        }

        Log.d("FormActivity", "getParam2 RESPONSE: $response")

        var msgId = msgUi + timestamp // Default if no param2 or failure
        if (response != null) {
            val param2 = response.optString("param2")
            val countLimit = response.optInt("count", 0)
            Log.d("GETPARAM2", "param2 response: $param2")
            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            if (param2 != "null" && param2.isNotEmpty()) {
                Log.d("GETPARAM2", "MASUK")
                // Fetch existing msgIds for param2
                val msgIds = msgIdMap[param2] ?: mutableListOf()
                val currentCountLimit = countLimitMap[param2] ?: countLimit

                if (msgIds.isNotEmpty() && currentCountLimit > 0) {
                    Log.e("FormActivity", "CURRENT LIMIT = $currentCountLimit")
                    msgId = msgIds.last()
                    countLimitMap[param2] = currentCountLimit - 1
                    if (currentCountLimit - 1 == 0) {
                        msgIdMap.remove(param2)
                        countLimitMap.remove(param2)

                        Log.e("FormActivity", "Biller dihapus")
                        isSvcBiller = false
                    }
                    if (currentCountLimit - 1 == 1) {
                        Log.e("FormActivity", "Biller Aktif")
                        isSvcBiller = true
                    }
                } else {
                    msgId = msgUi + timestamp
                    msgIds.add(msgId)
                    msgIdMap[param2] = msgIds
                    countLimitMap[param2] = countLimit - 1

                    if (currentCountLimit - 1 == 1) {
                        Log.e("FormActivity", "Biller Aktif")
                        isSvcBiller = true
                    }
                }
            }else{
                isSvcBiller = false
                Log.e("FormActivity", "Biller NonAktif")
                Log.d("GETPARAM2", "Tidak punya param2 atau param2 null")
            }
        } else {
            Log.d("FormActivity", "API call failed, created msgId: $msgId")
        }

        Log.d("FormActivity", "Generated msgId: $msgId")
        return msgId
    }

    private fun createBodyPengaduan(screen: Screen): JSONObject? {
        return try {
            val body = JSONObject()
            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            val savedKodeAgen = sharedPreferences.getString("kode_agen", "") ?: ""
            val savedKategori = sharedPreferences.getString("kategori", "") ?: ""
            Log.d("FormActivity", "saved kategori: $savedKategori")
            val username = sharedPreferences.getString("username_param", "") ?: ""

            val componentValues = mutableMapOf<String, String>()
            screen.comp.filter { it.type != 7 && it.type != 15 }.forEach { component ->
                Log.d("FormActivity", "Component: $component")

                when {
                    component.type == 1 && component.label == "Username" -> {
                        componentValues[component.id] = username
                        Log.d("FormActivity", "Updated componentValues with savedUsername for Component ID: ${component.id}")
                    }
                    component.type == 1 && component.label == "Kode Agen" -> {
                        componentValues[component.id] = savedKodeAgen
                        Log.d("FormActivity", "Kode Agen : $savedKodeAgen")
                    }
                    component.type == 1 && component.label == "Kategori" -> {
                        componentValues[component.id] = savedKategori
                        Log.d("FormActivity", "Kategori : $savedKategori")
                    }
                    component.type == 1 && component.label != "NIK" -> {
                        val value = (component.values.get(0)?.second ?: "") as String
                        componentValues[component.id] = value
                        Log.d("FormActivity", "Updated componentValues with value for Component ID: ${component.id}")
                    }
                    else -> {
                        componentValues[component.id] = inputValues[component.id] ?: ""
                    }
                }
            }

            // Membuat msgDtParts dari komponen yang dipisahkan dengan '|'
            val msgDtString = screen.comp
                .filter { it.type != 7 && it.type != 15 && it.id != "MSG03" && it.id != "PIL03" }
                .joinToString("|") { component -> componentValues[component.id] ?: "" }

            // Memisahkan string msgDtString menjadi list menggunakan split
            val msgDtParts = msgDtString.split("|")

            // Memetakan msg_dt ke dalam format JSON
            if (msgDtParts.size >= 4) {
                body.put("mid", msgDtParts[0])
                body.put("judul", msgDtParts[1])
                body.put("deskripsi", msgDtParts[2])
                body.put("kategori", msgDtParts[3])
            }

            lastMessageBody = body

            // Logging the JSON message details
            Log.d("FormActivity", "Message JSON: ${body.toString()}")

            body
        } catch (e: Exception) {
            Log.e("FormActivity", "Failed to create message body", e)
            null
        }
    }

    fun getUniqueID(): String {
        val wideVineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        var encodedUid: String = ""
        try {
            val wvDrm = MediaDrm(wideVineUuid)
            val wideVineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            encodedUid = Base64.encodeToString(wideVineId, Base64.NO_WRAP)  // Using Android's Base64.encodeToString
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return encodedUid
    }

    private fun loginUser(callback: (Boolean) -> Unit){
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            val formBodyBuilder = FormBody.Builder()
            var username: String? = null
            var password: String? = null

            for ((key, value) in inputValues) {
                when (key) {
                    "UN001" -> {
                        username = value
                        formBodyBuilder.add("username", value)
                    }
                    "A0002" -> {
                        password = value
                        formBodyBuilder.add("password", value)
                    }
                    else -> formBodyBuilder.add(key, value)
                }
            }

            val formBody = formBodyBuilder.build()

            Log.d(TAG, "Form body content: username=$username, password=$password")

            val request = Request.Builder()
                .url("http://reportntbs.selada.id/api/auth/login")
                .post(formBody)
                .addHeader("Accept", "application/json")
                .build()

            Log.d(TAG, "Sending login request to server...")

            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                val jsonResponse = JSONObject(responseData)
                val status = jsonResponse.optBoolean("status", false)

                Log.d(TAG, "Received response from server. Response code: ${response.code}, Response body: $responseData")

                if (response.isSuccessful && responseData != null) {

                    if (status) {
                        val token = jsonResponse.optString("token")
                        val usernameParam = jsonResponse.optString("username_param")
                        val userData = jsonResponse.optJSONObject("data")
                        val fullname = userData?.optString("fullname")
                        val id = userData?.optString("id")
                        val userStatus = userData?.optString("status")
                        val merchantData = userData?.optJSONObject("merchant")
                        val terminalArray = merchantData?.optJSONArray("terminal")
                        val branchid = userData?.optString("branchid")

                        Log.d(TAG, "User status: $userStatus")
                        Log.d(TAG, "Terminal array: $terminalArray")

                        // Check user status

                        when (userStatus) {
                            "0" -> {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Akun Anda belum diaktivasi", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            "2" -> {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Akun Anda telah dinonaktifkan", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            "3" -> {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Akun Anda terblokir", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                        }

                        if (token.isNotEmpty() && merchantData != null) {
                            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()

                            editor.putString("username", username)
                            editor.putString("token", token)
                            editor.putString("fullname", fullname)
                            editor.putString("id", id)
                            editor.putString("branchid", branchid)
                            editor.putString("username_param", usernameParam)

                            // Save merchant data
                            editor.putString("merchant_id", merchantData.optString("id"))
                            editor.putString("merchant_name", merchantData.optString("name"))
                            editor.putString("norekening", merchantData.optString("no"))
                            editor.putString("merchant_code", merchantData.optString("code"))
                            editor.putString("merchant_address", merchantData.optString("address"))
                            editor.putString("merchant_phone", merchantData.optString("phone"))
                            editor.putString("merchant_email", merchantData.optString("email"))
                            editor.putString("merchant_balance", merchantData.optString("balance"))
                            editor.putString("merchant_avatar", merchantData.optString("avatar"))
                            editor.putInt("merchant_status", merchantData.optInt("status"))
                            editor.putString("kode_agen", merchantData.optString("mid"))
                            editor.putString("pin", merchantData.optString("pin"))
                            editor.putString("mid", merchantData.optString("mid"))
                            val terminalData = terminalArray?.getJSONObject(0)
                            if (terminalData != null) {
                                editor.putString("tid", terminalData.optString("tid"))
                                editor.putString("imei", terminalData.optString("imei"))
                            }

                            editor.apply()

                            val midTerminal = sharedPreferences.getString("mid", null)
                            val tidTerminal = sharedPreferences.getString("tid", null)
                            val tokenTerminal = sharedPreferences.getString("token", null)
                            val usernameParamTerminal = sharedPreferences.getString("username_param", null)
                            Log.e ("MID TERMINAL", "MID TERMINAL : $midTerminal")
                            Log.e ("TID TERMINAL", "TID TERMINAL : $tidTerminal")
                            Log.e ("T0KENT TERMINAL", "TOKEN TERMINAL : $tokenTerminal")
                            Log.e ("USERNAME TERMINAL", "USERNAME : $username")
                            Log.e ("USERNAME PARAM TERMINAL", "USERNAME PARAM : $usernameParamTerminal")

//                            val imei = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) // ambil IMEI dari perangkat

                            val imei = getUniqueID()
                            Log.d(TAG, "Unique Device ID: $imei")

                            val storedImeiTerminal = sharedPreferences.getString("imei", null)
                            Log.e ("Imei Terminal", "Imei Terminal : $storedImeiTerminal")
                            val status = checkStatus()
                            editor.putBoolean("statusImei", status)
                            editor.apply()
                            Log.e("status", "STATUS LOGIN : $status")
                            if (terminalArray == null || terminalArray.length() == 0 || status == true) {
                                Log.d(TAG, "Terminal array is empty or null. Attempting to create terminal.")
                                Log.d("FormActivity", "OTP attempts LOGIN: ${otpAttempts.size}")
                                if (otpAttempts.size >= 2) {
                                    val currentTime = System.currentTimeMillis()
                                    val timeSinceLastAttempt = currentTime - otpAttempts.last()

                                    // Jika attempt ke-4 dan lebih harus reset waktu ke 30 menit
                                    if (otpAttempts.size == 3) {
                                        otpAttempts.add(System.currentTimeMillis())
                                        remainingMillis = 5 * 60 * 1000 // Set ke 30 menit
                                        Log.d("FormActivity", "Reset remainingMillis to 30 minutes")
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            resetTimer(remainingMillis) // Jika Anda memiliki metode resetTimer
                                        }
                                    }

                                    // Jika attempt ke-5 atau lebih lanjutkan waktu tanpa reset
                                    if (otpAttempts.size > 4) {
                                        if (timeSinceLastAttempt < remainingMillis) {
                                            // Jika masih dalam batas waktu
                                            Log.d("FormActivity", "Timer still running for attempts: ${otpAttempts.size}")
                                        }
                                    }
                                }
                                if (otpAttempts.size == 0) {
                                    otpAttempts.add(System.currentTimeMillis())
                                    createOTP { messageBody ->
                                        Log.d("LOGIN OTP", "Create OTP: $messageBody")
                                        if (messageBody != null) {
                                            Log.d("FormActivity", "Message Body OTP: $messageBody")
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    hideLoading()
                                                }
                                                withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                                        responseBody?.let { body ->
                                                            lifecycleScope.launch {

                                                                val screenJson =
                                                                    JSONObject(body)
                                                                val newScreen: Screen =
                                                                    ScreenParser.parseJSON(
                                                                        screenJson
                                                                    )
                                                                handleScreenType(
                                                                    newScreen
                                                                )
                                                            }
                                                        } ?: run {
                                                            showPopupGagal(
                                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                            )
                                                            Log.e("FormActivity", "Failed to fetch response body")
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    hideLoading()
                                                }
                                            }
                                            showPopupGagal(
                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                            )
                                            Log.e("FormActivity", "Failed to create message body, request not sent")
                                        }
                                    }
                                    Log.e("FormActivity", "Error create OTP")
                                } else if (otpAttempts.size > 0 && otpAttempts.size < 2) {
                                    otpAttempts.add(System.currentTimeMillis())
                                    createOTP { messageBody ->
                                        Log.d("LOGIN OTP", "Create OTP: $messageBody")
                                        if (messageBody != null) {
                                            Log.d("FormActivity", "Message Body OTP: $messageBody")
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    hideLoading()
                                                }
                                                withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                                        responseBody?.let { body ->
                                                            lifecycleScope.launch {

                                                                val screenJson =
                                                                    JSONObject(body)
                                                                val newScreen: Screen =
                                                                    ScreenParser.parseJSON(
                                                                        screenJson
                                                                    )
                                                                handleScreenType(
                                                                    newScreen
                                                                )
                                                            }
                                                        } ?: run {
                                                            showPopupGagal(
                                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                            )
                                                            Log.e("FormActivity", "Failed to fetch response body")
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    hideLoading()
                                                }
                                            }
                                            showPopupGagal(
                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                            )
                                            Log.e("FormActivity", "Failed to create message body, request not sent")
                                        }
                                    }

                                }else if (otpAttempts.size == 2) {
                                    otpAttempts.add(System.currentTimeMillis())
                                    createOTP { messageBody ->
                                        Log.d("LOGIN OTP", "Create OTP: $messageBody")
                                        if (messageBody != null) {
                                            Log.d("FormActivity", "Message Body OTP: $messageBody")
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    hideLoading()
                                                }
                                                withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                                        responseBody?.let { body ->
                                                            lifecycleScope.launch {

                                                                val screenJson =
                                                                    JSONObject(body)
                                                                val newScreen: Screen =
                                                                    ScreenParser.parseJSON(
                                                                        screenJson
                                                                    )
                                                                handleScreenType(
                                                                    newScreen
                                                                )
                                                            }
                                                        } ?: run {
                                                            showPopupGagal(
                                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                            )
                                                            Log.e("FormActivity", "Failed to fetch response body")
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    hideLoading()
                                                }
                                            }
                                            showPopupGagal(
                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                            )
                                            Log.e("FormActivity", "Failed to create message body, request not sent")
                                        }
                                    }
                                } else {
                                    Log.d("FormActivity", "REMAINING MILS : $remainingMillis")
                                    if (remainingMillis  > 0) {
                                        Log.d("FormActivity", "Masuk Remaining time")
                                        if (okButtonPressCount >= 3 || otpAttempts.size >= 3) {
                                            Log.d("FormActivity", "Masuk IF REMAINING time")
                                            val minutesRemaining = remainingMillis  / 60000
                                            val secondsRemaining = (remainingMillis % 60000) / 1000
                                            withContext(Dispatchers.Main) {
                                                hideLoading()
                                            }
                                            val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_warning)
                                                putExtra("MESSAGE_BODY", "Anda sudah melebihi batas limit pengiriman OTP! Mohon tunggu $minutesRemaining menit dan $secondsRemaining detik sebelum mengirim OTP kembali.")
                                                putExtra("RETURN_TO_ROOT", false)
                                            }
                                            startActivity(intentPopup)
                                        }
                                    } else {
                                        Log.d("LOGIN OTP", "OTP Lebih 3: ${otpAttempts.size}")
                                        otpAttempts.clear()
                                        otpAttempts.add(System.currentTimeMillis())
                                        createOTP { messageBody ->
                                            Log.d("LOGIN OTP", "Create OTP: $messageBody")
                                            if (messageBody != null) {
                                                Log.d("FormActivity", "Message Body OTP: $messageBody")
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        hideLoading()
                                                    }
                                                    withContext(Dispatchers.IO) {
                                                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                                            responseBody?.let { body ->
                                                                lifecycleScope.launch {

                                                                    val screenJson =
                                                                        JSONObject(body)
                                                                    val newScreen: Screen =
                                                                        ScreenParser.parseJSON(
                                                                            screenJson
                                                                        )
                                                                    handleScreenType(
                                                                        newScreen
                                                                    )
                                                                }
                                                            } ?: run {
                                                                showPopupGagal(
                                                                    "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                                )
                                                                Log.e("FormActivity", "Failed to fetch response body")
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        hideLoading()
                                                    }
                                                }
                                                showPopupGagal(
                                                    "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                                )
                                                Log.e("FormActivity", "Failed to create message body, request not sent")
                                            }
                                        }
                                    }
                                }
                            } else {
                                Log.d(TAG, "Terminal already exists.")
                                val terminalData = terminalArray.getJSONObject(0)
                                if (terminalData != null) {
                                    Log.d(TAG, "Terminal data found. Saving TID and IMEI.")

                                    val tid = terminalData.optString("tid")
                                    val terminalImei = terminalData.optString("imei")

                                    Log.d("LOGINUSER", "TID: $tid, IMEI: $terminalImei") // Log nilai TID dan IMEI

                                    editor.putString("tid", tid)
                                    editor.putString("imei", terminalImei)
                                    editor.apply()
                                } else {
                                    Log.d(TAG, "No terminal data found in the response.") // Log jika data terminal tidak ditemukan
                                }

                                editor.putInt("login_attempts", 0)
                                editor.apply()

//                                comment dulu biar bisa login
                                val storedImeiTerminal = sharedPreferences.getString("imei", null)
                                Log.d(TAG, "Stored IMEI: $storedImeiTerminal, Current IMEI: $imei") // Log IMEI yang tersimpan dan IMEI perangkat saat ini

                                if ((imei != null || imei != "null") && imei != storedImeiTerminal) {
                                    withContext(Dispatchers.Main) {
                                        hideLoading()
                                    }
                                    Log.d(TAG, "IMEI mismatch detected. Registered IMEI: $storedImeiTerminal, Current IMEI: $imei") // Log jika IMEI tidak cocok
                                    val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                        putExtra("LAYOUT_ID", R.layout.pop_up_change_device)
                                        putExtra("MESSAGE_BODY", "Perangkat yang digunakan tidak sesuai dengan yang didaftarkan.")
                                        putExtra("RETURN_TO_ROOT", false)
                                    }
                                    startActivity(intentPopup)
                                }else{
                                    fun retrieveAuthToken(): String {
                                        // Return the stored auth token
                                        return "auth_token" // Replace this with the actual logic to retrieve the token
                                    }

                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                        if (!task.isSuccessful) {
                                            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                                            return@addOnCompleteListener
                                        }

                                        val fcmToken = task.result
                                        Log.d("FCM", "FCM Token: $fcmToken")

                                        // Send FCM token and user_id to server
                                        val authToken = sharedPreferences.getString("token", "") ?: ""
                                        MyFirebaseMessagingService.sendFCMTokenToServer(authToken, fcmToken, id ?: "")
                                    }

                                createCheckSaldo { messageBody ->
                                    if (messageBody != null) {
                                        Log.d("FormActivity", "Message Body Check Saldo: $messageBody")
                                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                            responseBody?.let {
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@FormActivity,
                                                            "Login berhasil",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        navigateToScreen()
                                                        callback(true)
                                                    }
                                                }
                                            } ?: run {
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        hideLoading()
                                                    }
                                                }
                                                showPopupGagal(
                                                    "Mohon maaf, aplikasi sedang dalam perbaikan."
                                                )
                                                Log.e("FormActivity", "Failed to fetch response body")
                                            }
                                        }
                                    } else {
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.Main) {
                                                hideLoading()
                                            }
                                        }
                                        showPopupGagal(
                                            "Mohon maaf, aplikasi sedang dalam perbaikan."
                                        )
                                        Log.e("FormActivity", "Failed to create message body, request not sent")
                                    }
                                }


                                }

                                //                                ini di comment nanti kalo mau berdasarkan perangkat
//                                fun retrieveAuthToken(): String {
//                                    // Return the stored auth token
//                                    return "auth_token" // Replace this with the actual logic to retrieve the token
//                                }
//
//                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//                                    if (!task.isSuccessful) {
//                                        Log.w("FCM", "Fetching FCM registration token failed", task.exception)
//                                        return@addOnCompleteListener
//                                    }
//
//                                    val fcmToken = task.result
//                                    Log.d("FCM", "FCM Token: $fcmToken")
//
//                                    // Send FCM token and user_id to server
//                                    val authToken = sharedPreferences.getString("token", "") ?: ""
//                                    MyFirebaseMessagingService.sendFCMTokenToServer(authToken, fcmToken, id ?: "")
//                                }
//
//                                createCheckSaldo { messageBody ->
//                                    if (messageBody != null) {
//                                        Log.d("FormActivity", "Message Body Check Saldo: $messageBody")
//                                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
//                                            responseBody?.let {
//                                                lifecycleScope.launch {
//                                                    withContext(Dispatchers.Main) {
//                                                        Toast.makeText(
//                                                            this@FormActivity,
//                                                            "Login berhasil",
//                                                            Toast.LENGTH_SHORT
//                                                        ).show()
//                                                        navigateToScreen()
//                                                        callback(true)
//                                                    }
//                                                }
//                                            } ?: run {
//                                                lifecycleScope.launch {
//                                                    withContext(Dispatchers.Main) {
//                                                        hideLoading()
//                                                    }
//                                                }
//                                                showPopupGagal(
//                                                    "Mohon maaf, aplikasi sedang dalam perbaikan."
//                                                )
//                                                Log.e("FormActivity", "Failed to fetch response body")
//                                            }
//                                        }
//                                    } else {
//                                        lifecycleScope.launch {
//                                            withContext(Dispatchers.Main) {
//                                                hideLoading()
//                                            }
//                                        }
//                                        showPopupGagal(
//                                            "Mohon maaf, aplikasi sedang dalam perbaikan."
//                                        )
//                                        Log.e("FormActivity", "Failed to create message body, request not sent")
//                                    }
//                                }
//                                comment sampai sini
                            }
                        }
                        else {
                            lifecycleScope.launch {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@FormActivity, "Data User tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                            callback(false)
                        }
                    }
                    else {
                        callback(false)
                    }
                }
                else {
                    val errorMessage = jsonResponse.optString("message", "Login gagal.")

                    withContext(Dispatchers.Main) {
                        when {
                            errorMessage.contains("username", ignoreCase = true) -> {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        hideLoading()
                                    }
                                }
                                Toast.makeText(this@FormActivity, "Username tidak terdaftar", Toast.LENGTH_SHORT).show()
                            }
                            errorMessage.contains("incorrect", ignoreCase = true) -> {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        hideLoading()
                                    }
                                }
                                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("username_input", username)
                                editor.apply()
                                handleFailedLoginAttempt()
                            }
                            else -> {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        hideLoading()
                                    }
                                }
                                Toast.makeText(this@FormActivity, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    callback(false)
                }
            } catch (e: Exception) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                    }
                }
                Log.e(TAG, "Exception occurred while logging in", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun handleFailedPinAttempt() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentAttempts = sharedPreferences.getInt("pin_attempts", 0)

        if (currentAttempts >= 3) {
            blockUserAccount()
            val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                putExtra("MESSAGE_BODY", "Akun Anda telah terblokir. Hubungi Call Center.")
                putExtra("RETURN_TO_ROOT", true)
                putExtra(Constants.KEY_FORM_ID, "AU00001")
            }
            startActivity(intentPopup)
        } else {
            editor.putInt("pin_attempts", currentAttempts + 1)
            editor.apply()
            val attemptsLeft = 3 - (currentAttempts + 1)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FormActivity, "PIN Salah! Percobaan tersisa: $attemptsLeft", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("BlockAgen", "Current pin_attempts: ${currentAttempts + 1}")
    }


    private suspend fun handleFailedLoginAttempt() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentAttempts = sharedPreferences.getInt("login_attempts", 0)
        val maxLoginAttempt = sharedPreferences.getInt("max_login_attempt", 0)
        val username_input = sharedPreferences.getString("username_input", "")

        if (currentAttempts > maxLoginAttempt) {
            if (username_input != null) {
                blockUserAccountLogin(username_input)
                withContext(Dispatchers.Main) {
                    hideLoading()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FormActivity, "Akun Anda telah terblokir. Hubungi Call Center", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            editor.putInt("login_attempts", currentAttempts + 1)
            editor.apply()
            val attemptsLeft = maxLoginAttempt - (currentAttempts + 1)
            withContext(Dispatchers.Main) {
                hideLoading()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FormActivity, "Username atau password salah. Kesalahan ke-${currentAttempts + 1}. Percobaan tersisa: $attemptsLeft", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("BlockAgen", "Current login_attempts: ${currentAttempts + 1}")
    }




    private fun blockUserAccount() {
        lifecycleScope.launch {
            try {
                val formBodyBuilder = FormBody.Builder()

                val formBody = formBodyBuilder.build()

                val webCaller = WebCallerImpl()
                val fetchedValue = withContext(Dispatchers.IO) {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", "") ?: ""
                    val id = sharedPreferences.getString("merchant_id", "") ?: ""
                    val response = webCaller.blockAgen(id, token)
                    response?.string()
                }

                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        // Debug log for the raw response
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optString("status", "") ?: JSONArray()
                        val message = jsonResponse.optBoolean("message", false)
                        if (status as Boolean) {
                            Toast.makeText(this@FormActivity, "$message", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@FormActivity, "$message", Toast.LENGTH_SHORT).show()
                        }
                    } catch (jsonException: JSONException) {
                        Log.e("FormActivity", "JSON parsing error: ${jsonException.localizedMessage}")
                    }
                } else {
                    Log.e("FormActivity", "Fetched value is null or empty")
                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error : ${e.localizedMessage}")
            }
        }
    }

    private fun blockUserAccountLogin(username : String) {
        lifecycleScope.launch {
            try {
                val formBodyBuilder = FormBody.Builder()

                val formBody = formBodyBuilder.build()

                val webCaller = WebCallerImpl()
                val fetchedValue = withContext(Dispatchers.IO) {
                    val response = webCaller.blockAgenLogin(username)
                    response?.string()
                }

                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        // Debug log for the raw response
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optBoolean("status", false)
                        val message = jsonResponse.optString("message", "gagal")
                        if (!status) {
                            Log.e(TAG, "Failed to change password.")
                            Toast.makeText(this@FormActivity, "$message", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d(TAG, "Password changed successfully.")
                            Toast.makeText(this@FormActivity, message, Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                            Log.e(TAG, "Failed to change password.")
                            Toast.makeText(this@FormActivity, "$message", Toast.LENGTH_SHORT).show()
                        }
                    } catch (jsonException: JSONException) {
                        Log.e("FormActivity", "JSON parsing error: ${jsonException.localizedMessage}")
                    }
                } else {
                    Log.e("FormActivity", "Fetched value is null or empty")
                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error : ${e.localizedMessage}")
            }
        }
    }

    private fun changePassword() {
        lifecycleScope.launch {
            try {
                val formBodyBuilder = FormBody.Builder()
                var oldPassword: String? = null
                var newPassword: String? = null
                var confirmNewPassword: String? = null

                for ((key, value) in inputValues) {
                    when (key) {
                        "LSN01" -> {
                            oldPassword = value
                            formBodyBuilder.add("old_password", value)
                        }
                        "LSN02" -> {
                            newPassword = value
                            formBodyBuilder.add("new_password", value)
                        }
                        "LSN03" -> {
                            confirmNewPassword = value
                            formBodyBuilder.add("confirm_new_password", value)
                        }
                        else -> formBodyBuilder.add(key, value)
                    }
                }
                val formBody = formBodyBuilder.build()

                val webCaller = WebCallerImpl()
                val fetchedValue = withContext(Dispatchers.IO) {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", "") ?: ""
                    val userId = sharedPreferences.getString("id", "") ?: ""
                    val response = webCaller.changePassword(userId, oldPassword ?: "", newPassword ?: "", confirmNewPassword ?: "", token)
                    response?.string()
                }

                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        // Debug log for the raw response
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optBoolean("status", false)
                        val message = jsonResponse.optString("message")
                        if (!status) {
                            Log.e(TAG, "Failed to change password. $message")
                            showPopupGagal(
                                message
                            )
                        } else {
                            Log.d(TAG, "Password changed successfully.")
                            Toast.makeText(this@FormActivity, message, Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                        }
                    } catch (jsonException: JSONException) {
                        Log.e("FormActivity", "JSON parsing error: ${jsonException.localizedMessage}")
                    }
                } else {
                    Log.e("FormActivity", "Fetched value is null or empty")
                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error changing password: ${e.localizedMessage}")
            }
        }
    }
    private fun changePin() {
        lifecycleScope.launch {
            try {
                val formBodyBuilder = FormBody.Builder()
                var oldPin: String? = null
                var newPin: String? = null
                var confirmNewPin: String? = null

                for ((key, value) in inputValues) {
                    when (key) {
                        "LSP01" -> {
                            oldPin = value
                            formBodyBuilder.add("old_pin", value)
                        }
                        "LSP02" -> {
                            newPin = value
                            formBodyBuilder.add("new_pin", value)
                        }"LSP03" -> {
                        confirmNewPin = value
                        formBodyBuilder.add("confirm_new_pin", value)
                    }
                        else -> formBodyBuilder.add(key, value)
                    }
                }

                val formBody = formBodyBuilder.build()

                val webCaller = WebCallerImpl()
                val fetchedValue = withContext(Dispatchers.IO) {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", "") ?: ""
                    val id = sharedPreferences.getString("merchant_id", "") ?:""
                    val response = webCaller.changePin(id, oldPin ?: "", newPin ?: "",confirmNewPin ?: "", token)
                    response?.string()
                }

                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optBoolean("status", false)
                        val message = jsonResponse.optString("message", "")
                        if (status) {
                            Log.d(TAG, "PIN changed successfully.")
                            Toast.makeText(this@FormActivity, message, Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                        } else {
                            showPopupGagal(
                                message
                            )
                        }
                    } catch (jsonException: JSONException) {
                        Log.e("FormActivity", "JSON parsing error: ${jsonException.localizedMessage}")
                    }
                } else {
                    Log.e("FormActivity", "Fetched value is null or empty")
                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error changing password: ${e.localizedMessage}")
            }
        }
    }
    private fun changeDevice() {
        lifecycleScope.launch {
            try {
                val formBodyBuilder = FormBody.Builder()
                var username: String? = null
                var password: String? = null
                var nik: String? = null
                var deskripsi: String? = null
                var imei: String? = null

                for ((key, value) in inputValues) {
                    when (key) {
                        "UN001" -> {
                            username = value
                            formBodyBuilder.add("username", value)
                        }
                        "UN002" -> {
                            password = value
                            formBodyBuilder.add("password", value)
                        }"NIK01" -> {
                        nik = value
                        formBodyBuilder.add("nik", value)
                    }"KOM02" -> {
                        deskripsi = value
                        formBodyBuilder.add("deskripsi", value)
                    }
                        else -> formBodyBuilder.add(key, value)
                    }
                }

                val formBody = formBodyBuilder.build()

                val webCaller = WebCallerImpl()
                val fetchedValue = withContext(Dispatchers.IO) {
                    imei = getUniqueID()
                    val response = webCaller.changeDevice(username ?:"", password ?: "", nik ?: "",deskripsi ?: "", imei ?:"")
                    response?.string()
                }

                Log.d("FormActivity", "username: $username, password: $password,nik: $nik,deskripsi: $deskripsi,imei: $imei,")
                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optBoolean("status", false)
                        val message = jsonResponse.optString("message", "")
                        if (status) {
                            Log.d(TAG, "Request Change Device successfully.")
                            val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                putExtra("MESSAGE_BODY", message)
                                putExtra("RETURN_TO_ROOT", true)
                                putExtra(Constants.KEY_FORM_ID, "AU00001")
                            }
                            startActivity(intentPopup)
                        } else {
                            showPopupGagal(
                                message
                            )
                        }
                    } catch (jsonException: JSONException) {
                        Log.e("FormActivity", "JSON parsing error: ${jsonException.localizedMessage}")
                    }
                } else {
                    Log.e("FormActivity", "Fetched value is null or empty")
                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error changing password: ${e.localizedMessage}")
            }
        }
    }

    private fun forgotPassword() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                showLoading()
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val formBodyBuilder = FormBody.Builder()
                var username: String? = null
                var email: String? = null
                var nik: String? = null
                var no_rek: String? = null

                // Mengumpulkan data dari inputValues
                for ((key, value) in inputValues) {
                    when (key) {
                        "UN001" -> {
                            username = value
                            formBodyBuilder.add("username", value)
                        }
                        "UN005" -> {
                            email = value
                            formBodyBuilder.add("email", value)
                        }
                        "NIK01" -> {
                            nik = value
                            formBodyBuilder.add("nik", value)
                        }
                        "LP004" -> {
                            no_rek = value
                            formBodyBuilder.add("no_rek", value)
                        }
                        else -> formBodyBuilder.add(key, value)
                    }
                }

                val formBody = formBodyBuilder.build()
                // Lanjutkan proses jika berhasil, simpan ke SharedPreferences
                if (nik != null && username != null && no_rek != null && email != null) {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putString("username", username)
                    editor.putString("email", email)
                    editor.putString("nik", nik)
                    editor.putString("no_rek", no_rek)

                    editor.apply() // Simpan data
                }

                // Cek apakah perlu mengirim OTP terlebih dahulu
                val webCaller = WebCallerImpl()

                // Panggilan untuk membuat OTP sebelum reset password
                createOTP { messageBody ->
                    if (messageBody != null) {
                        Log.d("FormActivity", "Message Body OTP: $messageBody")
                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                            responseBody?.let { body ->
                                lifecycleScope.launch {
                                    val screenJson = JSONObject(body)
                                    val newScreen: Screen = ScreenParser.parseJSON(screenJson)

                                    // Jika OTP berhasil dibuat, arahkan ke halaman OTP
                                    handleScreenType(newScreen)
                                }
                            } ?: run {
                                showPopupGagal(
                                    "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                )
                                Log.e("FormActivity", "Failed to fetch response body")
                            }
                        }
                    } else {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                hideLoading()
                            }
                        }
//                        showPopupGagal(
//                            "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
//                        )
                        Log.e("FormActivity", "Failed to create message body, request not sent")
                    }
                }

            } catch (e: Exception) {
                Log.e("FormActivity", "Error changing password: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setKeyboard(editText: EditText, component: Component) {
        val conType = component.opt.substring(2, 3).toIntOrNull() ?: 3

        when (conType) {
            2 -> {
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            4 -> {
                editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            else -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT
            }
        }
    }
    //    private fun navigateToCreate() {
//        startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//            putExtra(Constants.KEY_MENU_ID, "CCIF004")
//        })
//    }
    private fun validateInput(component: Component): List<String> {
        Log.d ("Validate", "Masuk function")
        val container = findViewById<LinearLayout>(R.id.menu_container)
        val editText = container.findViewWithTag<EditText>(component.id)

        if (editText == null) {
            Log.d ("Validate", "Tidak ada edit text")
            return listOf("Input field for ${component.label} tidak ditemukan.")
        }

        val inputValue = inputValues[component.id] ?: ""
        val mandatory = component.opt.substring(0, 1).toIntOrNull() ?: 0
        val conType = component.opt.substring(2, 3).toIntOrNull() ?: 3
        val minLength = component.opt.substring(3, 6).toIntOrNull() ?: 0
        val maxLength = component.opt.substring(6).toIntOrNull() ?: Int.MAX_VALUE

        val errors = mutableListOf<String>()
        var lengthError = false
        val characterTypeErrors = mutableListOf<String>()

        if (mandatory == 1 && inputValue.isEmpty()) {
            errors.add("${component.label} Wajib Diisi")
        } else if (inputValue.length < minLength || inputValue.length > maxLength) {
            lengthError = true
        }

        when (conType) {
            0 -> {
                if (!inputValue.matches(Regex("^[\\w\\W]+$"))) {
                    characterTypeErrors.add("dan harus berupa string, bisa huruf, angka, atau simbol.")
                }
            }
            1 -> {
                if (!inputValue.matches(Regex("[a-zA-Z\\s]*"))) {
                    characterTypeErrors.add("dan mengandung huruf saja")
                }
            }
            2 -> {
                if (!inputValue.matches(Regex("[0-9]*"))) {
                    characterTypeErrors.add("dan mengandung angka saja")
                }
            }
            4 -> {
                if (!inputValue.matches(Regex("\\d+(\\.\\d{1,2})?"))) {
                    characterTypeErrors.add("dan format uang tidak valid")
                }
            }
            3 -> {
                // No Constraint
            }
            else -> {

            }
        }

        if(component.id == "UN005"){
            if (!inputValue.contains("@")) {
                characterTypeErrors.add("dan harus berformat email yang mengandung karakter '@'")
            }
        }

        if (lengthError || characterTypeErrors.isNotEmpty()) {
            var lengthErrorMessage = ""
            if(minLength == maxLength){
                lengthErrorMessage = "${component.label} harus terdiri dari $maxLength karakter"
            }else{
                lengthErrorMessage = "${component.label} harus terdiri dari $minLength - $maxLength karakter"
            }
            val combinedErrors = if (characterTypeErrors.isNotEmpty()) {
                "$lengthErrorMessage ${characterTypeErrors.joinToString()}"
            } else {
                lengthErrorMessage
            }
            errors.add(combinedErrors)
        }

        editText.error = null

        if (errors.isNotEmpty()) {
            editText.error = errors.first()
            editText.background = ContextCompat.getDrawable(this, R.drawable.edit_text_wrong)
            return errors
        } else {
            editText.background = ContextCompat.getDrawable(this, R.drawable.edit_text_background)
            return emptyList()
        }
    }

    private fun navigateToScreen() {
        startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_MENU_ID, "MN00000")
        })
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, "AU00001") // Ensure you use the correct key for your intent
        })
    }

    fun formatMutasi(mutasiText: String): String {
        val mutasiList = mutasiText.trim().split("\n").filter { it.isNotEmpty() }
        val formattedMutasi = StringBuilder()

        for (mutasi in mutasiList) {
            val parts = mutasi.split(" ")
            if (parts.size >= 5) {
                val date = parts[0]
                val time = parts[1]
                val description = parts.subList(2, parts.size - 2).joinToString(" ")
                val amount = parts[parts.size - 2] + " " + parts[parts.size - 1]

                formattedMutasi.append("Tanggal: $date\n")
                formattedMutasi.append("Waktu: $time\n")
                formattedMutasi.append("Deskripsi: $description\n")
                formattedMutasi.append("Jumlah: $amount\n\n")
            }
        }
        return formattedMutasi.toString()
    }

    fun extractArchiveNumber(mutasi: String): String {
        return try {
            // Parse the JSON string
            val jsonObject = JSONObject(mutasi)

            // Retrieve the array of comp_values
            val compValues = jsonObject.getJSONObject("comp_values")
            val compValueArray = compValues.getJSONArray("comp_value")

            // Loop through the array to find the object with the specified comp_id
            for (i in 0 until compValueArray.length()) {
                val compValueObject = compValueArray.getJSONObject(i)
                val compId = jsonObject.optString("comp_id", "")

                // Check if the comp_id matches TF008
                if (compId == "TF008") {
                    return compValueObject.optString("value", "")
                }
            }

            // Return empty string if not found
            ""
        } catch (e: JSONException) {
            e.printStackTrace()
            ""
        }
    }

    // Modifikasi parseMutasi untuk mengambil 'no arsip'
    fun parseMutasi(mutasiText: String): List<Mutation> {
        val mutasiList = mutasiText.trim().split("\n").filter { it.isNotEmpty() }
        val transactions = mutableListOf<Mutation>()

        // Sesuaikan regex untuk menangkap 'no arsip' jika diperlukan
        val regex = Regex("""(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}) (.+?) (Rp \d+.\d{2}) (CREDIT|DEBIT)""")
        for (mutasi in mutasiList) {
            val match = regex.find(mutasi)
            if (match != null) {
                val (date, time, description, amount, transactionType) = match.destructured
                val archiveNumber = extractArchiveNumber(mutasi) // Fungsi untuk mengekstrak 'no arsip'
                transactions.add(Mutation(date, time, description, amount, transactionType, archiveNumber))
            }
        }
        return transactions
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

    private fun openCameraIntent() {
        if (checkCameraPermission()) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            Log.e("FormActivity", "No camera app found")
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    // Tampilkan pesan kepada pengguna bahwa izin diperlukan
                    Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            currentImageView?.setImageBitmap(photo)

            // Simpan gambar ke file setelah foto diambil
            val fileName = if (photoCounter == 0) {
                "FOTO_${nikValue ?: "unknown"}.png" // Foto Orang
            } else {
                "KTP_${nikValue ?: "unknown"}.png" // Foto KTP
            }

            currentImageView?.let { imageView ->
                saveImageToFile(photo!!, fileName)?.let { file ->
                    if (photoCounter == 0) {
                        fileFotoOrang = file
                    } else {
                        fileFotoKTP = file
                    }
                }
            }
        } else {
            Log.e("FormActivity", "Pengambilan foto gagal atau dibatalkan")
        }
    }

    // Fungsi untuk menyimpan gambar sebagai file PNG
    private fun saveImageToFile(bitmap: Bitmap, fileName: String): File? {
        return try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("PhotoApp", "File disimpan: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("PhotoApp", "Gagal menyimpan file: ${e.localizedMessage}")
            null
        }
    }

    private fun startOtpTimer() {
        otpTimer = object : CountDownTimer(60000, 1000) { // 2 minutes countdown
            override fun onTick(millisUntilFinished: Long) {
                // You can show the remaining time to the user if needed
            }
            override fun onFinish() {
                msg03Value = null
                Log.d("Time ChecK", "OTP expired and cleared. (Start OTP Timer)")
            }
        }.start()
    }

    private fun cancelOtpTimer() {
        otpTimer?.cancel()
        otpTimer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelOtpTimer() // Cancel the timer if activity is destroyed
    }

    private fun resendOtp(screen: Screen?) {
        if (otpAttempts.size < 3) {
            otpAttempts.add(System.currentTimeMillis())
            if (screen != null) {
                if(screen.id != "WS0001"){
                    val messageBody = lastMessageBody
                    Log.d("FORM", "LAST MESSAGE : $messageBody")
                    if (messageBody != null) {
                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                            responseBody?.let { body ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        hideLoading()
                                    }

                                    val screenJson = JSONObject(body)
                                    Log.d("FormActivity", "Response POST: $body")
                                    Log.d("FormActivity", "SCREEN JSON: $screenJson")

                                    // Mendapatkan array komponen dari JSON
                                    val compArray =
                                        screenJson.getJSONObject("screen")
                                            .getJSONObject("comps")
                                            .getJSONArray("comp")

                                    // Mencari komponen dengan ID "MSG03"
                                    var newMsg03Value: String? = null
                                    for (i in 0 until compArray.length()) {
                                        val compObject = compArray.getJSONObject(i)
                                        if (compObject.getString("comp_id") == "MSG03") {
                                            newMsg03Value =
                                                compObject.getJSONObject("comp_values")
                                                    .getJSONArray("comp_value")
                                                    .getJSONObject(0)
                                                    .optString("value")
                                            break
                                        }
                                    }

                                    if (newMsg03Value != null) {
                                        msg03Value = newMsg03Value
                                        Log.d("FormActivity", "NEW MSG03 : $msg03Value")
                                        Toast.makeText(
                                            this@FormActivity,
                                            "OTP baru telah dikirim",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        cancelOtpTimer()
//                                        startOtpTimer()
                                    } else {
                                        Toast.makeText(
                                            this@FormActivity,
                                            "Gagal mendapatkan OTP baru",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } ?: run {
                                Log.e("FormActivity", "Failed to fetch response body")
                                showPopupGagal(
                                    "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                )
                            }
                        }
                    } else {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                hideLoading()
                            }
                        }
                        Log.e("FormActivity", "Failed to create message body, request not sent")
                        showPopupGagal(
                            "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                        )
                    }
                }else{
                    createOTP { messageBody ->
                        Log.d("LOGIN OTP", "Create OTP: $messageBody")
                        if (messageBody != null) {
                            Log.d("FormActivity", "Message Body OTP: $messageBody")
                            lifecycleScope.launch {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                                withContext(Dispatchers.IO) {
                                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                        responseBody?.let { body ->
                                            lifecycleScope.launch {

                                                val screenJson = JSONObject(body)
                                                Log.d("FormActivity", "Response POST: $body")
                                                Log.d(
                                                    "FormActivity",
                                                    "SCREEN JSON: $screenJson"
                                                )

                                                // Mendapatkan array komponen dari JSON
                                                val compArray =
                                                    screenJson.getJSONObject("screen")
                                                        .getJSONObject("comps")
                                                        .getJSONArray("comp")

                                                // Mencari komponen dengan ID "MSG03"
                                                var newMsg03Value: String? = null
                                                for (i in 0 until compArray.length()) {
                                                    val compObject = compArray.getJSONObject(i)
                                                    if (compObject.getString("comp_id") == "MSG03") {
                                                        newMsg03Value =
                                                            compObject.getJSONObject("comp_values")
                                                                .getJSONArray("comp_value")
                                                                .getJSONObject(0)
                                                                .optString("value")
                                                        break
                                                    }
                                                }

                                                if (newMsg03Value != null) {
                                                    msg03Value = newMsg03Value
                                                    Log.d(
                                                        "FormActivity",
                                                        "NEW MSG03 : $msg03Value"
                                                    )
                                                    Toast.makeText(
                                                        this@FormActivity,
                                                        "OTP baru telah dikirim",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    cancelOtpTimer()
//                                                    startOtpTimer()
                                                } else {
                                                    Toast.makeText(
                                                        this@FormActivity,
                                                        "Gagal mendapatkan OTP baru",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } ?: run {
                                            showPopupGagal(
                                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                                            )
                                            Log.e("FormActivity", "Failed to fetch response body")
                                        }
                                    }
                                }
                            }
                        } else {
                            lifecycleScope.launch {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                            }
                            showPopupGagal(
                                "Terjadi Kesalahan Dalam Proses. Silahkan Coba Kembali atau Hubungi Call Center."
                            )
                            Log.e("FormActivity", "Failed to create message body, request not sent")
                        }
                    }
                    Log.e("FormActivity", "Error create OTP")
                }
            }
        }else {
            val lastAttemptTime = otpAttempts.last()
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAttemptTime < otpCooldownTime) {
                Toast.makeText(
                    this@FormActivity,
                    "Anda sudah melebihi batas pengiriman OTP. Silahkan tunggu selama 5 menit.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                otpAttempts.clear()
                otpAttempts.add(System.currentTimeMillis())
                resendOtp(screen)
            }
        }
    }
    private fun createOTP(callback: (JSONObject?) -> Unit) {
        lifecycleScope.launch {
            hideLoading()
            try {
                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                var merchantPhone = sharedPreferences.getString("merchant_phone", "") ?: ""
                val usernameLogin = sharedPreferences.getString("username", "") ?: ""
                val username = sharedPreferences.getString("username_param", "") ?: ""
                val emailLogin = sharedPreferences.getString("email", "") ?: ""
                val nikLogin = sharedPreferences.getString("nik", "") ?: ""
                val noRekLogin = sharedPreferences.getString("no_rek", "") ?: ""
                val uid = getUniqueID()
                val modifiedUid = uid.replace("+", "%2B").replace("=", "%3D")

                Log.d("FormActivity", "Merchant OTP: $merchantPhone")

                // Ensure we don't proceed if email is empty
                if (emailLogin.isNotEmpty()) {
                    val responseBody = withContext(Dispatchers.IO) {
                        webCallerImpl.getPhoneByUsername(usernameLogin, emailLogin, nikLogin, noRekLogin, uid)
                    }

                    Log.d("FormActivity", "Request Data: username=$usernameLogin, email=$emailLogin, nik=$nikLogin, no_rek=$noRekLogin, uid=$uid, modified uid:$modifiedUid")

                    if (responseBody != null) {
                        try {
                            val responseString = responseBody.string()
                            Log.d("FormActivity", "Response: $responseString")

                            val jsonResponse = JSONObject(responseString)
                            val status = jsonResponse.optBoolean("status")

                            if (status) {
                                merchantPhone = jsonResponse.optString("phone")
                                Log.d("FormActivity", "Phone replaced with: $merchantPhone")

                                sharedPreferences.edit().putString("merchant_phone", merchantPhone).apply()

                                val imei = "89b2c0aa8e0ac7c2" // Hardcoded IMEI untuk sekarang
                                val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
                                val msgUi = imei
                                val msgId = msgUi + timestamp
                                val msgSi = "SV0001"
                                val msgDt = "$username|$merchantPhone"

                                // Buat JSON pesan
                                val msgObject = JSONObject().apply {
                                    put("msg_id", msgId)
                                    put("msg_ui", msgUi)
                                    put("msg_si", msgSi)
                                    put("msg_dt", msgDt)
                                }

                                val msg = JSONObject().apply {
                                    put("msg", msgObject)
                                }
                                callback(msg)
                            } else {
                                // Tampilkan pesan error jika status gagal
                                val message = jsonResponse.optString("message", "Terjadi kesalahan")
                                showPopupGagal(message)
                                Log.e("FormActivity", "Error from server: $message")
                                callback(null) // Return null on error
                            }
                        } catch (e: Exception) {
                            Log.e("FormActivity", "Failed to parse JSON response", e)
                            callback(null) // Return null on exception
                        }
                    } else {
                        Log.e("FormActivity", "Response body is null")
                        callback(null) // Response body kosong
                    }
                } else {
                    // Handle case where email is empty
                    Log.e("FormActivity", "Email is null or empty")
                    val imei = "89b2c0aa8e0ac7c2" // Hardcoded IMEI for now
                    Log.e("FormActivity", "Saved IMEI: $imei")
                    val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
                    val msgUi = imei
                    val msgId = msgUi + timestamp
                    val msgSi = "SV0001"
                    val msgDt = "$username|$merchantPhone"

                    val msgObject = JSONObject().apply {
                        put("msg_id", msgId)
                        put("msg_ui", msgUi)
                        put("msg_si", msgSi)
                        put("msg_dt", msgDt)
                    }

                    val msg = JSONObject().apply {
                        put("msg", msgObject)
                    }
                    callback(msg) // Return the message object
                }
            } catch (e: Exception) {
                Log.e("MenuActivity", "Failed to create message body", e)
                callback(null) // Return null on any other exception
            }
        }
    }

    private fun createTerminalAndLogin(screen: Screen?) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val midTerminal = sharedPreferences.getString("mid", null)
        val token = sharedPreferences.getString("token", "")
        val imei = getUniqueID()

        Log.d(TAG, "Starting terminal creation process. IMEI: $imei, MID: $midTerminal, Token: $token")

        if (imei != null && midTerminal != null) {
            val createTerminalUrl = "http://reportntbs.selada.id/api/terminal/create"
            val requestBody = FormBody.Builder()
                .add("imei", imei)
                .add("merchant_id", midTerminal)
                .build()

            val createTerminalRequest = Request.Builder()
                .url(createTerminalUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val terminalResponse = client.newCall(createTerminalRequest).execute()
                    val responseData = terminalResponse.body?.string()

                    // Check if response is JSON
                    if (terminalResponse.isSuccessful && responseData != null) {
                        if (isValidJson(responseData)) {
                            val jsonResponse = JSONObject(responseData)
                            val success = jsonResponse.optBoolean("success", false)

                            if (success) {
                                val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("imei", imei)
                                editor.apply()
                                val storedImeiTerminal = sharedPreferences.getString("imei", null)
                                Log.d(TAG, "Emei After Create Terminal : $storedImeiTerminal")
                                Log.d(TAG, "Terminal created successfully")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Terminal created", Toast.LENGTH_SHORT).show()
                                    navigateToScreen()
                                }
                            } else {
                                val message = jsonResponse.optString("message", "Gagal membuat terminal.")
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                                showPopupGagal(message)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                hideLoading()
                            }
                            showPopupGagal("Unauthorized: Server response is not in JSON format.")
                        }
                    } else {
                        val message = parseErrorMessage(responseData)
                        withContext(Dispatchers.Main) {
                            hideLoading()
                        }
                        showPopupGagal(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error occurred while creating terminal", e)
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        showPopupGagal("Terjadi kesalahan: ${e.message}")
                    }
                }
            }
        } else {
            Log.d(TAG, "IMEI or MID is null. Cannot create terminal.")
        }
    }

    private fun isValidJson(string: String): Boolean {
        return try {
            JSONObject(string)
            true
        } catch (e: JSONException) {
            try {
                JSONArray(string)
                true
            } catch (e: JSONException) {
                false
            }
        }
    }

    private fun parseErrorMessage(responseData: String?): String {
        return try {
            val jsonResponse = responseData?.let { JSONObject(it) }
            jsonResponse?.optString("message", "Terjadi kesalahan yang tidak diketahui.") ?: "Server tidak merespons."
        } catch (e: JSONException) {
            "Gagal memproses respons dari server."
        }
    }

    private fun showPopupGagal(message: String) {
        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
            putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
            putExtra("MESSAGE_BODY", message)
            putExtra("RETURN_TO_ROOT", false)
        }
        startActivity(intent)
    }

    private fun updateImei() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val midTerminal = sharedPreferences.getString("mid", null)
        val tidTerminal = sharedPreferences.getString("tid", null)
        val token = sharedPreferences.getString("token", "")
        val imei = getUniqueID()

        Log.d(TAG, "Attempting to update IMEI.")

        if (imei != null && midTerminal != null && tidTerminal != null) {
            val updateImeiUrl = "http://reportntbs.selada.id/api/imei/update" // Sesuaikan URL sesuai dengan route di Laravel

            val jsonBody = """
        {
            "tid": "$tidTerminal",
            "imei": "$imei",
            "mid": "$midTerminal"
        }
        """.trimIndent()

            // Buat RequestBody untuk JSON
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

            // Membangun request dengan body JSON dan header Authorization
            val updateImeiRequest = Request.Builder()
                .url(updateImeiUrl)
                .put(requestBody) // Menggunakan PUT request untuk update
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json") // Pastikan menambahkan header Content-Type
                .build()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = client.newCall(updateImeiRequest).execute()
                    val responseData = response.body?.string()

                    if (response.isSuccessful && responseData != null) {
                        val jsonResponse = JSONObject(responseData)
                        val success = jsonResponse.optBoolean("success", false)

                        if (success) {
                            Log.d(TAG, "IMEI berhasil diperbarui")

                            // Jika perlu, simpan data atau lakukan tindakan lain
                            saveTerminalData(jsonResponse)

                            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putInt("login_attempts", 0)
                            editor.apply()

//                                comment dulu biar bisa login
                            val storedImeiTerminal = sharedPreferences.getString("imei", null)
                            if (imei != null && imei != storedImeiTerminal) {
                                withContext(Dispatchers.Main) {
                                    hideLoading()
                                }
                                val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                    putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                    putExtra("MESSAGE_BODY", "Perangkat yang digunakan tidak sesuai dengan yang didaftarkan.")
                                    putExtra("RETURN_TO_ROOT", false)
                                }
                                startActivity(intentPopup)
                            }else{
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                                    navigateToScreen()
                                }
                            }
                        } else {
                            Log.d(TAG, "Gagal memperbarui IMEI: ${jsonResponse.optString("message", "No message")}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@FormActivity, "Gagal memperbarui IMEI", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.d(TAG, "Gagal melakukan request update IMEI: ${response.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FormActivity, "Request gagal: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error occurred while updating IMEI", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d(TAG, "IMEI, MID, atau TID null, tidak bisa update IMEI.")
            Toast.makeText(this, "IMEI, MID, atau TID tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPengaduanToApi(bodyPengaduan: JSONObject) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "")

        val judul = bodyPengaduan.optString("judul", "")
        Log.d("FormActivity", "Judul: $judul")

        // Definisikan URL API
        val createPengaduanUrl = "http://reportntbs.selada.id/api/pengaduan/create"

        // Ubah JSONObject menjadi string dan buat RequestBody
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), bodyPengaduan.toString())

        // Buat Request POST
        val createPengaduanRequest = Request.Builder()
            .url(createPengaduanUrl)
            .post(requestBody) // Menggunakan POST request
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        // Menggunakan lifecycleScope untuk mengirim request secara asynchronous
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(createPengaduanRequest).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && responseData != null) {
                    val jsonResponse = JSONObject(responseData)
                    val success = jsonResponse.optBoolean("success", true)

                    if (success) {
                        Log.d("FormActivity", "Pengaduan berhasil dikirim")
                        val message = jsonResponse.optString("message", "Pengaduan berhasil dikirim")
                        val data = jsonResponse.optJSONObject("data")


                        data?.let {
                            val id = it.optInt("id")
                            if (judul == "Pengaduan Ganti Perangkat") {
                                sendRequestImei(id.toString())
                            }
                            val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                putExtra("MESSAGE_BODY", message)
                                putExtra("RETURN_TO_ROOT", true)
                            }
                            startActivity(intent)
                        } ?: run {
                            Log.e("FormActivity", "Data field is null")
                        }
                    } else {
                        Log.d("FormActivity", "Gagal mengirim pengaduan: ${jsonResponse.optString("message", "No message")}")

                        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                            putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                            putExtra("MESSAGE_BODY", "Gagal mengirim pengaduan")
                            putExtra("RETURN_TO_ROOT", false)
                        }
                        startActivity(intent)
                    }
                } else {
                    Log.d("FormActivity", "Gagal melakukan request pengaduan: ${response.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Request gagal: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("FormActivity", "Error occurred while sending pengaduan", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun sendRequestImei(id: String) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val midTerminal = sharedPreferences.getString("mid", null)
        val tidTerminal = sharedPreferences.getString("tid", null)
        val token = sharedPreferences.getString("token", "")
        val imei = getUniqueID()

        // Log the start of the process
        Log.d(TAG, "sendRequestImei called with IMEI: $imei, MID: $midTerminal, TID: $tidTerminal")

        if (imei != null && midTerminal != null && tidTerminal != null) {
            val createTerminalUrl = "http://reportntbs.selada.id/api/imei/store"

            // Create the JSON body and log it for debugging
            val jsonBody = """
        {
            "tid": "$tidTerminal",
            "imei": "$imei",
            "mid": "$midTerminal",
            "id_pengaduan": "$id"
        }
        """.trimIndent()
            Log.d(TAG, "Request body: $jsonBody")

            // Create RequestBody for JSON
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

            // Build the request and log headers
            val createTerminalRequest = Request.Builder()
                .url(createTerminalUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
            Log.d(TAG, "Sending request to $createTerminalUrl with token: Bearer $token")

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Log when request is about to be executed
                    Log.d(TAG, "Executing IMEI request...")

                    val imeiResponse = client.newCall(createTerminalRequest).execute()
                    val terminalResponseData = imeiResponse.body?.string()

                    // Log the response status and body
                    Log.d(TAG, "Response code: ${imeiResponse.code}")
                    Log.d(TAG, "Response message: ${imeiResponse.message}")
                    Log.d(TAG, "Response body: $terminalResponseData")

                    if (imeiResponse.isSuccessful && terminalResponseData != null) {
                        val imeiJsonResponse = JSONObject(terminalResponseData)
                        val terminalStatus = imeiJsonResponse.optBoolean("success", false)

                        // Log based on the success status
                        if (terminalStatus) {
                            Log.d(TAG, "IMEI successfully stored")
                        } else {
                            Log.e(TAG, "Failed to store IMEI, response status is false")
                        }
                    } else {
                        Log.e(TAG, "Failed to create IMEI, response not successful: ${imeiResponse.message}")
                    }
                } catch (e: Exception) {
                    // Log the error with a detailed message
                    Log.e(TAG, "Error occurred while creating IMEI request", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Log if any of the required parameters are null
            Log.e(TAG, "IMEI, MID, or TID is null. Cannot send request.")
        }
    }

    private suspend fun checkStatus(): Boolean {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val midTerminal = sharedPreferences.getString("mid", null)
        val tidTerminal = sharedPreferences.getString("tid", null)
        val token = sharedPreferences.getString("token", "")

        return if (midTerminal != null && tidTerminal != null && token != null) {
            val checkStatusUrl = "http://reportntbs.selada.id/api/terminal/checkStatus?tid=$tidTerminal&mid=$midTerminal"

            // Membangun request GET dengan header Authorization
            val checkStatusRequest = Request.Builder()
                .url(checkStatusUrl)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            Log.d("CHECKSTATUS", "SEND REQUEST")
            Log.d("CHECKSTATUS", "MID $midTerminal")
            Log.d("CHECKSTATUS", "TID $tidTerminal")
            Log.d("CHECKSTATUS", "TOKEN $token")

            try {
                val response = client.newCall(checkStatusRequest).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && responseData != null) {
                    val jsonResponse = JSONObject(responseData)
                    val success = jsonResponse.optBoolean("success", false)

                    if (success) {
                        val status = jsonResponse.optBoolean("status", false)
                        if (status) {
                            Log.d("CHECKSTATUS", "Data ditemukan pada table imei dan status true")
                            true
                        } else {
                            Log.d("CHECKSTATUS", "Data ditemukan pada table imei tetapi status false")
                            false
                        }
                    } else {
                        Log.d("CHECKSTATUS", "Gagal: ${jsonResponse.optString("message", "No message")}")
                        false
                    }
                } else {
                    Log.d("CHECKSTATUS", "Gagal melakukan request check status: ${response.message}")
                    false
                }
            } catch (e: Exception) {
                Log.e("CHECKSTATUS", "Error occurred while checking status", e)
                false
            }
        } else {
            Log.d("CHECKSTATUS", "MID, TID, atau token null, tidak bisa cek status.")
            false
        }
    }

    private fun saveTerminalData(terminalJsonResponse: JSONObject) {
        val terminalData = terminalJsonResponse.optJSONObject("data")
        val tid = terminalData?.optString("tid") ?: ""
        val imeiTerminal = terminalData?.optString("imei") ?: ""

        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("tid", tid)
        editor.putString("imei", imeiTerminal)
        editor.apply()

        Log.d(TAG, "Saved terminal TID and IMEI to SharedPreferences")
    }

    // Fungsi untuk mengunggah file ke server
    private fun uploadImageFile(file: File, url: String) {
        Log.d("UploadFile", "Memulai pengunggahan file: ${file.name}, ukuran: ${file.length() / 1024} KB, URL: $url")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/png".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        // Configure timeouts
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("UploadFile", "Gagal mengupload file: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    Log.d("UploadFile", "File berhasil diupload ke $url")
                } else {
                    Log.e("UploadFile", "Gagal mengupload file: ${response.code}")
                }
            }
        })
    }

    fun checkSaldo(onSaldoFetched: (String?, String?, String?) -> Unit) {
        val messageBody = createCekSaldo() // Misalnya, Anda sudah memiliki fungsi untuk membuat body request
        if (messageBody != null) {
            ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                responseBody?.let {
                    try {
                        val jsonResponse = JSONObject(it)
                        val msgObject = jsonResponse.optJSONObject("screen")
                        if (msgObject != null) {
                            val comps = msgObject.optJSONObject("comps")
                            val compArray = comps?.optJSONArray("comp")
                            if (compArray != null) {
                                var accountNumber: String? = null
                                var saldo: String? = null

                                for (i in 0 until compArray.length()) {
                                    val comp = compArray.getJSONObject(i)
                                    val label = comp.optString("comp_lbl")
                                    val value = comp.optJSONObject("comp_values")
                                        ?.optJSONArray("comp_value")?.optJSONObject(0)
                                        ?.optString("value")

                                    if (label == "No Rekening") {
                                        accountNumber = value
                                    } else if (label == "Saldo Akhir") {
                                        saldo = value
                                        saldo = saldo?.replace("-", "")?.replace(",", "")
                                        saldo = formatRupiah(saldo?.toDoubleOrNull() ?: 0.0)
                                    }
                                }
                                // Mengirimkan hasil saldo dan nomor rekening
                                onSaldoFetched(accountNumber, saldo, null)
                            } else {
                                // Jika compArray null, kirimkan error
                                onSaldoFetched(null, null, "Komponen saldo tidak ditemukan.")
                            }
                        } else {
                            onSaldoFetched(null, null, "Objek screen tidak ditemukan.")
                        }
                    } catch (e: Exception) {
                        onSaldoFetched(null, null, "Gagal mem-parsing response: ${e.message}")
                    }
                } ?: run {
                    onSaldoFetched(null, null, "Response body adalah null.")
                }
            }
        } else {
            onSaldoFetched(null, null, "Gagal membuat body request.")
        }
    }

    private fun createCekSaldo(): JSONObject? {
        return try {
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val norekening = sharedPreferences.getString("norekening", "") ?: ""
            val merchant_name = sharedPreferences.getString("merchant_name", "") ?: ""
            val username = sharedPreferences.getString("username_param", "") ?: ""
            val msg = JSONObject()

            val imei = sharedPreferences.getString("imei", "")?: ""
            Log.e("FormActivity", "Saved Imei: $imei")
            val msgUi = imei
//            val msgUi = "353471045058692"
            val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
            val msgId = msgUi + timestamp

            val msgSi = "N00001"
            val accountNumber = norekening
            val name = merchant_name
            val msgDt = "$username|$accountNumber|$name|null"

            val msgObject = JSONObject().apply {
                put("msg_id", msgId)
                put("msg_ui", msgUi)
                put("msg_si", msgSi)
                put("msg_dt", msgDt)
            }

            msg.put("msg", msgObject)

            // Logging the JSON message details
            Log.d("MenuActivity", "Message ID: $msgId")
            Log.d("MenuActivity", "Message UI: $msgUi")
            Log.d("MenuActivity", "Message SI: $msgSi")
            Log.d("MenuActivity", "Message DT: $msgDt")
            Log.d("MenuActivity", "Message JSON: ${msg.toString()}")
            msg
        } catch (e: Exception) {
            Log.e("MenuActivity", "Failed to create message body", e)
            null
        }
    }

    private fun createCheckSaldo(callback: (JSONObject?) -> Unit) {
        lifecycleScope.launch {
            try {

                val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                val norekening = sharedPreferences.getString("norekening", "") ?: ""
                val merchant_name = sharedPreferences.getString("merchant_name", "") ?: ""
                val username = sharedPreferences.getString("username_param", "") ?: ""

                val imei = sharedPreferences.getString("imei", "")?: ""
                Log.e("FormActivity", "Saved Imei: $imei")
                val msgUi = imei
//            val msgUi = "353471045058692"
                val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
                val msgId = msgUi + timestamp

                val msgSi = "N00001"
                val accountNumber = norekening
                val name = merchant_name
                val msgDt = "$username|$accountNumber|$name|null"


                val msgObject = JSONObject().apply {
                    put("msg_id", msgId)
                    put("msg_ui", msgUi)
                    put("msg_si", msgSi)
                    put("msg_dt", msgDt)
                }

                val msg = JSONObject().apply {
                    put("msg", msgObject)
                }
                callback(msg) // Return the message object
            } catch (e: Exception) {
                Log.e("MenuActivity", "Failed to create message body", e)
                callback(null) // Return null on any other exception
            }
        }
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    fun showLoading() {
        loadingOverlay?.visibility = View.VISIBLE
        lottieLoading?.visibility = View.VISIBLE
    }

    fun hideLoading() {
        loadingOverlay?.visibility = View.GONE
        lottieLoading?.visibility = View.GONE
    }


}