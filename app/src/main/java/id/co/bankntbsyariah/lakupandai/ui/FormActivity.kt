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
import id.co.bankntbsyariah.lakupandai.ui.SignatureActivity
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
import kotlinx.coroutines.GlobalScope
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
import  id.co.bankntbsyariah.lakupandai.utils.createTextView
import android.widget.EditText
import org.json.JSONArray
import java.text.ParseException
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.style.StyleSpan
import android.graphics.Color
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import id.co.bankntbsyariah.lakupandai.common.CompValues
import id.co.bankntbsyariah.lakupandai.common.ComponentValue
import id.co.bankntbsyariah.lakupandai.ui.adapter.ProdukAdapter
import com.google.firebase.messaging.FirebaseMessaging
import id.co.bankntbsyariah.lakupandai.MyFirebaseMessagingService
import id.co.bankntbsyariah.lakupandai.api.ApiService
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody // Pastikan impor ini benar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import android.hardware.fingerprint.FingerprintManager
import android.media.MediaDrm
import android.os.CancellationSignal
import android.text.InputFilter
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.util.concurrent.Executor
import java.security.MessageDigest
import java.util.UUID

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

    data class Token(val token: String)

    private var otpTimer: CountDownTimer? = null
    private var lastMessageBody: JSONObject? = null
    private var otpAttempts = mutableListOf<Long>()
    private val otpCooldownTime = 3 * 60 * 1000L // 30 minutes in milliseconds
    private var otpScreen: Screen? = null

    private lateinit var executor: Executor

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

        findViewById<TextView>(R.id.forgot_password)?.setOnClickListener {
            val intent = Intent(this, FormActivity::class.java).apply {
                putExtra(Constants.KEY_FORM_ID, "LPW0000")
            }
            startActivity(intent)
        }
    }

    private fun initLoginRegisterUI() {
        setContentView(R.layout.activity_form_login) 
        executor = ContextCompat.getMainExecutor(this)

        findViewById<Button>(R.id.fingerprintLoginButton).setOnClickListener {
            authenticateFingerprint { encryptedFingerprint ->
                encryptedFingerprint?.let { loginWithFingerprint(it) }
            }
        }
        findViewById<Button>(R.id.fingerprintRegisterButton).setOnClickListener {
            registerFingerprint()
        }
    }

    private fun authenticateFingerprint(callback: (String?) -> Unit) {
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@FormActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    // Retrieve userId from session or other identifiable user data
                    val userId = getUserIdFromSession()

                    // Simulate fingerprint by hashing userId
                    val hashedFingerprint = userId?.let { hashUserId(it) }
                    callback(hashedFingerprint)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@FormActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Use fingerprint to authenticate")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun hashUserId(userId: String): String {
        // Use SHA-256 to hash the user ID
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(userId.toByteArray())
        // Convert to Base64 string for easy transmission
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    private fun loginWithFingerprint(fingerprintData: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val requestBody = FormBody.Builder()
                .add("finger_print", fingerprintData)
                .build()

            val request = Request.Builder()
                .url("http://reportntbs.selada.id/api/auth/loginWithFingerprint")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && responseData != null) {
                    val jsonResponse = JSONObject(responseData)
                    val token = jsonResponse.optString("token")
                    val user = jsonResponse.optJSONObject("data")
                    withContext(Dispatchers.Main) {
                        saveUserSession(user, token)
                        Toast.makeText(this@FormActivity, "Login successful!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@FormActivity, MenuActivity::class.java).apply {
                            putExtra("screen_id", "idMN00000")
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    handleFailedLoginAttempt(responseData)
                }
            } catch (e: Exception) {
                Log.e("FingerprintLogin", "Error: ${e.message}")
            }
        }
    }

    private fun registerFingerprint() {
        val userId = getUserIdFromSession()

        if (userId != null) {
            authenticateFingerprint { fingerprintData ->
                fingerprintData?.let { registerFingerprintWithServer(userId, it) }
            }
        } else {
            Toast.makeText(this, "You must be logged in to register a fingerprint", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerFingerprintWithServer(userId: String, fingerprintData: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val requestBody = FormBody.Builder()
                .add("id", userId)
                .add("finger_print", fingerprintData)
                .build()

            val request = Request.Builder()
                .url("http://reportntbs.selada.id/api/auth/registerFingerprint")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (response.isSuccessful && responseData != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Fingerprint registered successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleFailedRegistration(responseData)
                }
            } catch (e: Exception) {
                Log.e("FingerprintRegister", "Error: ${e.message}")
            }
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

    private fun saveUserSession(user: JSONObject?, token: String) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("id", user?.optString("id"))
        editor.putString("token", token)
        editor.apply()
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
                } else {
                    // Otherwise, navigate to MenuActivity
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

                setupForm(screen, dialogView)
                otpDialog?.window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                otpDialog?.show()
            }

            else -> {
                setupForm(screen)
            }
        }

        if (screen.type != Constants.SCREEN_TYPE_POPUP_GAGAL &&
            screen.type != Constants.SCREEN_TYPE_POPUP_SUKSES &&
            screen.type != Constants.SCREEN_TYPE_POPUP_OTP
        ) {
            handleScreenTitle(screen.title)
            setupForm(screen)
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
            screenTitle.contains("PIN", ignoreCase = true) -> R.layout.screen_pin
            screenTitle.contains("Berhasil", ignoreCase = true) ->
            {
                showSuccessPopup(screenTitle)
                R.layout.activity_berhasil
            }
            else -> R.layout.activity_form
        }
        if (layoutId != R.layout.activity_form) {
            setContentView(layoutId)
            Log.d("FormActivity", "Displaying layout with ID: $layoutId")
        }

        if (screenTitle.contains("Form", ignoreCase = true)) {
            val processedTitle = screenTitle.replace("FORM", "", ignoreCase = true).trim()
            val textView: TextView = findViewById(R.id.text_center)
            textView?.text = processedTitle
        }

        if (screenTitle.contains("Review", ignoreCase = true)) {
            val processedTitle = screenTitle.replace("Review", "", ignoreCase = true).trim()
            val textView: TextView = findViewById(R.id.text_center)
            textView?.text = processedTitle
        }

        if (screenTitle.contains("Berhasil", ignoreCase = true)) {
            val formattedTitle = screenTitle.replace("Berhasil", "").trim()
            Log.d("FormActivity", "Formatted title: $formattedTitle")

            val processedTitle = screenTitle.replace("Berhasil", "", ignoreCase = true).trim()
            val textView: TextView = findViewById(R.id.text_center)
            textView?.text = processedTitle
        }

        if (screenTitle.contains("Transfer", ignoreCase = true)) {
            val processedTitle = screenTitle.replace("Transfer", "", ignoreCase = true).trim()
            val textView: TextView = findViewById(R.id.text_center)
            textView?.text = processedTitle
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

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupForm(screen: Screen, containerView: View? = null) {
        val container = containerView?.findViewById<LinearLayout>(R.id.menu_container)
            ?: findViewById(R.id.menu_container)
        var buttonContainer = containerView?.findViewById<LinearLayout>(R.id.button_type_7_container) ?: null
        val buttontf = findViewById<LinearLayout>(R.id.button_type_7_container)


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
            if (screen.title.contains("Transfer") && component.id == "ST003") {
                val transaksiBerhasilTextView = findViewById<TextView>(R.id.success)
                val dateTransferTextView = findViewById<TextView>(R.id.dateTransfer)
                val timeTransferTextView = findViewById<TextView>(R.id.timeTransfer)
                val titleTransactionView = findViewById<TextView>(R.id.titleTransaction)

                transaksiBerhasilTextView?.let {
                    val newText = getComponentValue(component)
                    if (!newText.isNullOrEmpty()) {
                        it.text = newText
                        titleTransactionView?.text = getTransactionTitle(screen.id)
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
                (component.id == "ST003" && screen.title.contains("Transfer")) || component.id == "D1004"
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
                            }

                            groupedData.forEach { (date, nasabahList) ->
                                layout.addView(TextView(context).apply {
                                    text = date
                                    textSize = 15f
                                    setTypeface(null, Typeface.BOLD)
                                    setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    setTextColor(Color.parseColor("#808080"))
                                })

                                nasabahList.forEach { nasabah ->
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
                                        setMargins(0, 0, 0, 16.dpToPx()) // Add bottom margin (adjust as needed)
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
                                                            val messageId = msgId


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
                    }
                    else {
                        LinearLayout(this@FormActivity).apply {
                            orientation = LinearLayout.VERTICAL

                            val componentValue = getComponentValue(component)

                            if (screen.id == "CCIF001") {
                                inputRekening[component.id] = componentValue
                            }

                            val numericValue = componentValue.toDoubleOrNull() ?: 0.0
                            val formattedValue = when {
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
                                val totalFormatted = formatRupiah(totalValue)

                                component.values = component.values.mapIndexed { index, pair ->
                                    if (index == 0) pair.copy(second = totalFormatted) else pair
                                }

                                component.compValues?.compValue =
                                    component.compValues?.compValue?.mapIndexed { index, compVal ->
                                        if (index == 0) compVal.copy(value = totalFormatted) else compVal
                                    } ?: emptyList()
                            }

                            if (screen.id == "TF00003") {
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
                                    if (screen.id == "TF00003") {
                                        setPadding(0.dpToPx(), 3.dpToPx(), 16.dpToPx(), 2.dpToPx())
                                    } else {
                                        setPadding(0.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    }
                                    setTextColor(Color.parseColor("#0A6E44")) // Mengatur warna teks label
                                })
                                if (screen.id == "RCCIF02") {
                                    val rekeningIndex = inputRekeningIndex.coerceAtMost(inputRekening.size - 1) // Pastikan tidak melebihi ukuran inputRekening
                                    val rekeningValue = inputRekening.values.elementAtOrNull(rekeningIndex) ?: formattedValue
                                    inputRekeningIndex++ // Update ke index berikutnya
                                    addView(TextView(this@FormActivity).apply {
                                        text = rekeningValue
                                        textSize = 18f
                                        setPadding(0.dpToPx(), 0, 16.dpToPx(), 10.dpToPx())
                                    })
                                } else {
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
                            params.setMargins(0, 0, 0, 18) // Margin bawah 16dp
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
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                            override fun afterTextChanged(s: Editable?) {
                                val inputText = s.toString().replace(".", "")
                                inputValues[component.id] = inputText

                                // Format input as currency if the label is admin, nominal, or fee
                                if (component.label.contains("nominal", ignoreCase = true) ||
                                    component.label.contains("fee", ignoreCase = true) ||
                                    component.label.contains("nilai", ignoreCase = true)
                                ) {
                                    editText.removeTextChangedListener(this)

                                    val formattedText = if (inputText.isNotEmpty()) {
                                        val reversed = inputText.reversed()
                                        val chunked = reversed.chunked(3).joinToString(".")
                                        chunked.reversed()
                                    } else {
                                        ""
                                    }

                                    editText.setText(formattedText)
                                    editText.setSelection(formattedText.length) // Move cursor to end

                                    editText.addTextChangedListener(this)
                                }

                                if (screen.id == "CCIF001") {
                                    inputRekening[component.id] = inputText
                                    Log.d("FormActivity", "inputRekening[${component.id}] set to: $inputText")
                                }

                                if (component.label == "NIK") {
                                    nikValue = inputText
                                    Log.d("FormActivity", "NIK : $nikValue")
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
                            params.setMargins(0, 0, 0, 18) // Margin bawah 18dp
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
                        addView(editText)
                    }
                }

                4 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL

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

                        var compOption = component.compValues?.compValue

                        lifecycleScope.launch {
                            val options: List<Pair<String?, String?>> =
                                if (compOption.isNullOrEmpty() ||
                                    compOption.all { it.value == "" }
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
                                    } else {
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
                                            if (component.id == "PR006") {
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
                                    if (component.id == "PR006") {
                                        compOption.map { Pair(it.print, it.value) }
                                    } else {
                                        mutableListOf(Pair("Pilih ${component.label}", "")) + compOption.map { Pair(it.print, it.value) }
                                    }
                                }
                            if (component.id == "PR006") {
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

                            else {
                                val spinner = Spinner(this@FormActivity).apply {
                                    background = getDrawable(R.drawable.combo_box)
                                    val adapter = ArrayAdapter(
                                        this@FormActivity,
                                        android.R.layout.simple_spinner_item,
                                        options.map { it.first } // Display only the 'print' value
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
                                                Log.d(
                                                    "FormActivity",
                                                    "Component ID: ${component.id}, No Value Selected"
                                                )
                                                return
                                            }

                                            val selectedPair = options[position]
                                            val selectedValue = selectedPair.first
                                            val selectedCompValue = selectedPair.second

                                            if (screen.id == "CCIF001") {
                                                inputRekening[component.id] =
                                                    selectedValue ?: "" // Menyimpan selectedValue
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
                                                "PIL03" -> {
                                                    pickOTP = selectedValue
                                                    pickOTP = selectedValue
                                                    Log.d("Form", "PICK OTP: $selectedValue")

                                                    // Cek jika nilai tidak valid
                                                    if (selectedValue != "WA" && selectedValue != "SMS") {
                                                        // Tampilkan AlertDialog
                                                        AlertDialog.Builder(this@FormActivity)
                                                            .setTitle("Invalid Option")
                                                            .setMessage("Harus memilih WA atau SMS.")
                                                            .setPositiveButton("OK", null)
                                                            .show()

                                                        // Set pickOTP kembali ke nilai kosong atau default
                                                        pickOTP = ""

                                                        Toast.makeText(
                                                            this@FormActivity,
                                                            "Validasi gagal: Pilih WA atau SMS",
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
                                                            selectedCompValue.replace("[OI]", "")
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

                                                "CIF13" -> {
                                                    if (selectedCompValue != null) {
                                                        inputValues[component.id] =
                                                            selectedCompValue.replace("[OI]CK", "")
                                                    }
                                                    Log.d(
                                                        "FormActivity",
                                                        "Kab Kota set to: ${inputValues[component.id]}"
                                                    )
                                                }

                                                "CIF23" -> inputValues[component.id] =
                                                    (position - 1).toString()

                                                "CIF14" -> {
                                                    if (selectedCompValue != null) {
                                                        inputValues[component.id] =
                                                            selectedCompValue.replace(
                                                                "[OI]CIFP",
                                                                ""
                                                            )
                                                    }
                                                    Log.d(
                                                        "FormActivity",
                                                        "Provinsi set to: ${inputValues[component.id]}"
                                                    )
                                                }

                                                else -> inputValues[component.id] =
                                                    (position ?: selectedValue).toString()
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
                                id = View.generateViewId() // Assign a unique ID to each radio button
                            }

                            radioButton.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    val selectedValue = value.first

                                    if (screen.id == "CCIF001") {
                                        inputRekening[component.id] = selectedValue
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
                        setBackground(background)
                        setOnClickListener {
                            Log.d("FormActivity", "Screen Type: ${screen.type}")
                            val sharedPreferences =
                                getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                            val pinLogin = sharedPreferences.getInt("pin", 0).toString()
                            Log.e("PIN", "PIN LOGIN: $pinLogin")
                            if (component.id == "OK001") {
                                val pinValue = inputValues["PIN"]
                                Log.e("PIN", "PIN INPUT: $pinValue")

                                if (pinLogin != null && pinLogin == pinValue) {
                                    otpScreen?.let { screen ->
                                        handleScreenType(screen)
                                    }
                                } else {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        handleFailedPinAttempt()
                                    }
                                }
                            } else if (component.id == "OTP09") {
                                // Periksa apakah percobaan OTP kurang dari 3
                                if (otpAttempts.size < 3) {
                                    otpAttempts.add(System.currentTimeMillis())
                                    handleButtonClick(component, screen)
                                } else {
                                    val lastAttemptTime = otpAttempts.last()
                                    val currentTime = System.currentTimeMillis()

                                    if (currentTime - lastAttemptTime < otpCooldownTime) {
                                        Toast.makeText(
                                            this@FormActivity,
                                            "OTP send limit exceeded. Please wait 30 minutes.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        otpAttempts.clear()
                                        otpAttempts.add(currentTime)
                                        handleButtonClick(component, screen)
                                    }
                                }
                            }else if (formId == "AU00001" && component.id != "OTP10") {
                                loginUser()
                                requestAndHandleKodeCabang(screen) { kodeCabangResult ->
                                    if (kodeCabangResult != null) {
                                        // Perbarui properti kelas kodeCabang
                                        saveKodeCabangToPreferences(kodeCabangResult)
                                        Log.d("FORM", "KODE CABANG : $kodeCabangResult")

                                        Log.e("FORM", "KODE CABANGSS : $kodeCabangResult")
                                    } else {
                                        Log.e("FORM", "Failed to retrieve kodeCabang")
                                    }
                                }
                            } else if (formId == "LS00001") {
                                changePassword()
                            } else if (formId == "LS00002") {
                                changePin()
                            } else if (formId == "LPW0000" && component.id != "OTP10") {
                                forgotPassword()
                            }else {
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
                    val timerTextView = otpView.findViewById<TextView>(R.id.timerTextView)

                    val resendOtpTextView = otpView.findViewById<TextView>(R.id.tv_resend_otp)

                    // Setup Resend OTP TextView
                    resendOtpTextView?.let {
                        Log.e("FormActivity", "RESEND OTP NOT null.")
                        it.setOnClickListener { resendOtp() }

                        val text = "Resend OTP"
                        val spannable = SpannableString(text).apply {
                            setSpan(UnderlineSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        it.text = spannable
                    }

                    val countDownTimer = object : CountDownTimer(120000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val minutes = millisUntilFinished / 60000
                            val seconds = (millisUntilFinished % 60000) / 1000
                            val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                            timerTextView.text = timeFormatted
                        }

                        override fun onFinish() {
                            timerTextView.text = "00:00"
                            // Handle timeout scenario, e.g., disable inputs or show a message
                        }
                    }

                    countDownTimer.start()

                    // Setup OTP Digits
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
                            background = getDrawable(R.drawable.date_input)
                            id = View.generateViewId()
                            tag = component.id
                            inputType = InputType.TYPE_NULL // Menonaktifkan input keyboard
                            setTextSize(16f) // Ukuran teks untuk input
                            setTextColor(ContextCompat.getColor(this@FormActivity, R.color.black)) // Warna teks untuk input

                            setOnClickListener {
                                Log.d("FormActivity", "EditText clicked: ${component.id}")
                                showDatePickerDialog(this) { selectedDate ->
                                    inputValues[component.id as String] = selectedDate
                                    if (screen.id == "CCIF001") {
                                        inputRekening[component.id] = inputValues[component.id] ?: ""
                                    }
                                }
                            }
                        }
                        addView(editText)
                    }.let { view ->
                        // Ensure view is added with proper layout parameters
                        container.addView(view, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(20, 20, 20, 20) // Margin untuk seluruh view
                        })
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
                    val cameraView = layoutInflater.inflate(R.layout.camera_preview, null, false)
                    val imageViewPreview = cameraView.findViewById<ImageView>(R.id.imageViewPreview)
                    val buttonCapture = cameraView.findViewById<Button>(R.id.buttonCapture)
                    val titleCapture = cameraView.findViewById<TextView>(R.id.JudulFoto)

                    if (component.id == "SIG02" || component.id == "SIG03") {
                        // Mengatur teks button
                        buttonCapture.text = if (component.id == "SIG02") {
                            "Ambil Gambar Nasabah"  // Jika comp_id adalah SIG02
                        } else {
                            "Ambil Gambar KTP"      // Jika comp_id adalah SIG03
                        }

                        // Mengatur teks judul
                        titleCapture.text = if (component.id == "SIG02") {
                            "Foto Nasabah"  // Judul untuk foto nasabah
                        } else {
                            "Foto KTP"      // Judul untuk foto KTP
                        }
                    }

                    buttonCapture.setOnClickListener {
                        val fileName = if (photoCounter == 0) {
                            "FOTO_${nikValue ?: "unknown"}.png" // Nama file untuk foto Orang
                        } else {
                            "KTP_${nikValue ?: "unknown"}.png" // Nama file untuk foto KTP
                        }

                        if (currentImageView == null) {
                            imageViewOrang = imageViewPreview
                            Log.d("PhotoApp", "Foto Orang diambil: ${imageViewOrang != null} dengan nama file $fileName")
                        } else {
                            imageViewKTP = imageViewPreview
                            Log.d("PhotoApp", "Foto KTP diambil: ${imageViewKTP != null} dengan nama file $fileName")
                        }
                        photoCounter++ // Inkrementasi nilai photoCounter setelah foto diambil
                        currentImageView = imageViewPreview
                        Log.d("PhotoApp", "Current ImageView Updated: ${currentImageView != null}")

                        if (checkCameraPermission()) {
                            openCameraIntent()
                        } else {
                            requestCameraPermission()
                        }
                    }

                    container.addView(cameraView)
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
                                akadDescription.text = "Akad Mudharabah Muqayyadah merupakan jenis akad dimana penggunaannya bagi nasabah skala komersil yg membatasi Bank dalam mengelola simpanannya."
                            }
                            "AKD02" -> {
                                akadTitle.text = "Wadiah yad Ad dhamanah"
                                akadDescription.text = "Akad Wadi'ah yad Al Amanah merupakan jenis akad dimana nasabah BSA tidak membolehkan Bank mengelola dananya (safe deposit box)."
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
                                "Mudharabah Muthlaqah" -> "Mudharabah adalah"
                                "Wadiah yad Ad dhamanah" -> "Wadiah adalah"
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


    private fun requestAndHandleKodeCabang(screen: Screen, callback: (String?) -> Unit) {
        val messageBody = createMessageBody(screen)
        if (messageBody != null) {
            Log.d("FormActivity", "Message Body Login: $messageBody")

            ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                responseBody?.let { body ->
                    Log.d("FormActivity", "Response POST: $body")

                    val kodeCabangResult = parseKodeCabangFromResponse(body)
                    Log.d("FormActivity", "Kode Cabang: $kodeCabangResult")

                    // Call the callback with the updated kodeCabang
                    callback(kodeCabangResult)
                } ?: run {
                    Log.e("FormActivity", "Failed to fetch response body")
                    callback(null)
                }
            }
        } else {
            callback(null)
        }
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

        when (component.label) {
            "No Rekening Agen" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val savedNorekening = sharedPreferences.getString("norekening", "").toString()
                if (currentValue == "null" && savedNorekening != "0") {
                    Log.d("FormActivity", "No Rekening Agen diisi dengan nilai: $savedNorekening")
                    component.compValues?.compValue?.firstOrNull()?.value = savedNorekening
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
                    component.compValues?.compValue?.firstOrNull()?.value = selectedAkad
                } else {
                    Log.d("FormActivity", "Pilihan akad terisi dengan: $selectedAkad")
                }
            }
            "Penjelasan Akad" -> {
                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val akadDescriptionValue = sharedPreferences.getString("penjelasan", "")
                if (currentValue == "null" && akadDescriptionValue != "0") {
                    Log.d("FormActivity", "Nilai akad AKD04: $akadDescriptionValue")
                    component.compValues?.compValue?.firstOrNull()?.value = akadDescriptionValue
                } else {
                    Log.d("FormActivity", "Penjelasan akad terisi dengan: $akadDescriptionValue")
                }
            }
            "Nama Rekening Agen" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                val Userfullname = sharedPreferences.getString("fullname", "") ?: ""
                if (currentValue == "null" && Userfullname != "0") {
                    Log.d("FormActivity", "No Rekening Agen diisi dengan nilai: $Userfullname")
                    component.compValues?.compValue?.firstOrNull()?.value = Userfullname
                } else {
                    Log.d("FormActivity", "No Rekening Agen sudah terisi dengan: $Userfullname")
                }
            }
            "NIK" -> {
                if (currentValue == "null" && nikValue != null) {
                    Log.d("FormActivity", "NIK diisi dengan nilai: $nikValue")
                    component.compValues?.compValue?.firstOrNull()?.value = nikValue
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
                    component.compValues?.compValue?.firstOrNull()?.value = savedKodeAgen
                } else {
                    Log.d("FormActivity", "Kode Agen sudah terisi dengan: $savedKodeAgen")
                }
            }
            else -> {
                component.compValues?.compValue?.firstOrNull()?.value ?: ""
                Log.d("FormActivity", "Komponen tidak memerlukan pembaruan")
            }
        }

        // Ambil nilai di luar [OI] jika comp_id adalah CB001
        if (component.id == "CB001") {
            currentValue = currentValue?.let {
                Regex("""\[(?:OI|oi)\](\d+)""").find(it)?.groupValues?.get(1)
            }
        }

        return when (component.id) {
//            "CB001" -> {
//                if (currentValue != "OI") currentValue ?: "" else ""
//            }
            "CIF32" -> when (currentValue) {
                "1" -> "Laki-Laki"
                "2" -> "Perempuan"
                else -> ""
            }
            "CIF34" -> when (currentValue) {
                "1" -> "Kawin"
                "2" -> "Belum Kawin"
                "3" -> "Janda/Duda"
                else -> ""
            }
            "CIF33" -> when (currentValue) {
                "1" -> "Islam"
                "2" -> "Kristen Protestan"
                "3" -> "Katholik"
                "4" -> "Budha"
                "5" -> "Hindu"
                "6" -> "Konghucu"
                else -> ""
            }

            "CIF42" -> when (currentValue) {
                "0" -> "Tidak Menetap"
                "1" -> "Menetap"
                else -> ""
            }
            "CIF43" -> when (currentValue) {
                "1" -> "WNI"
                "2" -> "WNA"
                else -> ""
            }
            "CIF47" -> when (currentValue) {
                "1" -> "KTP"
                "2" -> "PASSPORT"
                else -> ""
            }
            "CIF49" -> when (currentValue) {
                "0" -> "A"
                "1" -> "AB"
                "2" -> "B"
                "3" -> "O"
                "4" -> "-"
                else -> ""
            }
            "CIF51" -> when (currentValue) {
                "1" -> "SD"
                "2" -> "SLTP"
                "3" -> "SMA"
                "4" -> "AKADEMI"
                "5" -> "S1"
                "6" -> "S2"
                "7" -> "S3"
                "8" -> "OTHERS"
                else -> ""
            }
            else -> currentValue ?: ""
        }

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

        screen?.comp?.forEach { comp ->
            if (comp.type == 2) {
                allErrors.addAll(validateInput(comp))
            }
        }
        screen?.comp?.forEach { comp ->
            if (comp.id == "PIL03" && pickOTP.isNullOrEmpty()) {
                allErrors.add("Harus memilih WA atau SMS.")
            }
        }
        if (allErrors.isNotEmpty()) {
            // Tampilkan semua pesan kesalahan
            Toast.makeText(
                this,
                "Validasi gagal:\n${allErrors.joinToString("\n")}",
                Toast.LENGTH_LONG
            ).show()
            return // Hentikan eksekusi jika ada kesalahan
        }

        if (screen?.id == "MB81120") {
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
                startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(Constants.KEY_MENU_ID, "CCIF000")
                })
                finish()
            } else {
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
        else if (screen?.id == "TF00001" || screen?.id == "FT0001" ||screen?.id == "ST001") {
            var norekComponent: Component? = null
            var nominalComponent: Component? = null
            var norek: String? = null

            when (screen.id) {
                "TF00001" -> {
                    norekComponent = screen.comp.find { it.label.contains("rekening pengirim", ignoreCase = true) }
                    nominalComponent = screen.comp.find { it.label.contains("nominal", ignoreCase = true) }
                    norek = inputValues[norekComponent?.id]
                }
                "FT0001" -> {
                    norekComponent = screen.comp.find { it.label.contains("rekening nasabah", ignoreCase = true) }
                    nominalComponent = screen.comp.find { it.label.contains("nominal", ignoreCase = true) }
                    norek = inputValues[norekComponent?.id]
                }
                "ST001" -> {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    norek = sharedPreferences.getString("norekening", "")
                    nominalComponent = screen.comp.find { it.label.contains("nominal", ignoreCase = true) }
                }
            }

            val nominalStr = inputValues[nominalComponent?.id]
            val nominal = nominalStr?.toDoubleOrNull()

            Log.d("FormActivity", "norek $norek")
            Log.d("FormActivity", "nominal $nominal")

            if (norek != null && nominal != null) {
                checkSaldo(nominal, norek, onSuccess = {
                    Log.d("FormActivity", "Saldo cukup, melanjutkan proses...")
                    createMessageBody(screen)
                    val messageBody = createMessageBody(screen)
                    if (messageBody != null) {
                        Log.d("FormActivity", "Message Body: $messageBody")
                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                            responseBody?.let { body ->
                                lifecycleScope.launch {
                                    val screenJson = JSONObject(body)
                                    val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                    Log.e("FormActivity", "SCREEN ${screen.id} ")
                                    Log.e("FormActivity", "NEW SCREEN ${newScreen.id} ")
                                    handleScreenType(newScreen)
                                }
                            }
                        }
                    }
                }, onError = { errorMsg ->
                    val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
                        putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                        putExtra("MESSAGE_BODY", errorMsg)
                        putExtra("RETURN_TO_ROOT", false)
                    }
                    startActivity(intentPopup)
                })
            } else {
                Toast.makeText(this, "Mohon masukkan norek dan nominal yang valid", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            val otpComponent = screen?.comp?.find { it.id == "OTP01" }
            if (otpComponent != null) {
                val otpValue = inputValues["OTP"]
                Log.e("OTP", "OTP: $otpValue")
                Log.e("MSG", "MSG: $msg03Value")
                if (msg03Value != null && msg03Value == otpValue) {
                    otpAttempts.clear()
                    isOtpValidated = true
                    otpDialog?.dismiss()
                    cancelOtpTimer()
                    Log.e("OTP", "SCREEN SEKARANG: $screen.id")
                    if (screen.id == "WS0001") {
                        Log.e("OTP", "Create Terminal: $otpValue")

                        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                        val storedImeiTerminal = sharedPreferences.getString("imei", null)
                        Log.e("Imei Terminal", "Imei Terminal Handle : $storedImeiTerminal")

                        // Ambil newPassword dari sharedPreferences
                        val newPassword = sharedPreferences.getString("new_password", null)
                        Log.d("FormActivity", "Saved New Password: $newPassword")

                        // Periksa newPassword terlebih dahulu
                        if (!newPassword.isNullOrEmpty()) {
                            Log.e("FormActivity", "New password is not null or empty, attempting to reset password.")
                            // Panggil forgotPassword
                            lifecycleScope.launch {
                                val username = sharedPreferences.getString("username", null) // Asumsikan username disimpan di sharedPreferences
                                if (!username.isNullOrEmpty()) {
                                    try {
                                        // Call forgotPassword asynchronously
                                        val response = withContext(Dispatchers.IO) {
                                            WebCallerImpl().forgotPassword(username, newPassword)
                                        }
                                        if (response != null) {
                                            Log.d("FormActivity", "Forgot Password success: ${response.string()}")

                                            navigateToLogin()
                                        } else {
                                            Log.e("FormActivity", "Failed to reset password")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("FormActivity", "Error during forgot password request", e)
                                    }
                                } else {
                                    Log.e("FormActivity", "Username is null or empty, unable to reset password.")
                                }
                            }
                        } else {
                            // Lakukan pemeriksaan untuk storedImeiTerminal jika newPassword valid
                            lifecycleScope.launch {
                                val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                                val status = sharedPreferences.getBoolean("statusImei", false)  // Mengambil status sebagai boolean
                                Log.d("STATUS IMEI", "Status: $status")
                                if (status) {
                                    updateImei()
                                } else {
                                    createTerminalAndLogin(screen)
                                }
                            }
                        }
                        
                    } else{
                        val messageBody = createMessageBody(screen)
                        if (messageBody != null) {
                            Log.d("FormActivity", "Message Body: $messageBody")
                            ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                responseBody?.let { body ->
                                    lifecycleScope.launch {
                                        val screenJson = JSONObject(body)
                                        val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                        Log.e("FormActivity", "SCREEN ${screen.id} ")
                                        Log.e("FormActivity", "NEW SCREEN ${newScreen.id} ")
                                        if (screen.id == "CCIF003" && newScreen.id == "000000D") {
                                            val message = newScreen?.comp?.find { it.id == "0000A" }
                                                ?.compValues?.compValue?.firstOrNull()?.value ?: "Unknown error"
                                            newScreen.id = "RCCIF02"
                                            var newScreenId = newScreen.id
                                            var formValue =
                                                StorageImpl(applicationContext).fetchForm(newScreenId)
                                            if (formValue.isNullOrEmpty()) {
                                                formValue = withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                        newScreenId
                                                    )
                                                }
                                                Log.i("FormActivity", "Fetched formValue: $formValue")
                                            }
                                            val url = "http://108.137.154.8:8080/ARRest/fileupload"
                                            if(fileFotoOrang != null){
                                                // Mengunggah foto orang
                                                fileFotoOrang?.let { file ->
                                                    uploadImageFile(file, url)
                                                    Log.d("Foto", "Foto Orang Ada : $fileFotoOrang")
                                                }
                                            }else{
                                                Log.d("Foto", "Foto Orang Kosong")
                                            }
                                            if(fileFotoKTP != null){
                                                // Mengunggah KTP
                                                fileFotoKTP?.let { file ->
                                                    uploadImageFile(file, url)
                                                    Log.d("Foto", "Foto KTP Ada : $fileFotoKTP")
                                                }
                                            }else{
                                                Log.d("Foto", "Foto KTP Kosong")
                                            }
                                            if(signatureFile != null){
                                                // Mengunggah tanda tangan
                                                signatureFile?.let { file ->
                                                    uploadImageFile(file, url)
                                                    Log.d("Foto", "Signature Ada : $signatureFile")
                                                }
                                            }else{
                                                Log.d("Foto", "Tanda Tangan Kosong")
                                            }
                                            setupScreen(formValue)

                                            val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", message)
                                                putExtra("RETURN_TO_ROOT", false)
                                            }
                                            startActivity(intent)
                                        } else if (screen.id == "CCIF000" && newScreen.id == "000000F") {
                                            newScreen.id = "CCIF001"
                                            var newScreenId = newScreen.id
                                            var formValue =
                                                StorageImpl(applicationContext).fetchForm(newScreenId)
                                            if (formValue.isNullOrEmpty()) {
                                                formValue = withContext(Dispatchers.IO) {
                                                    ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                        newScreenId
                                                    )
                                                }
                                                Log.i("FormActivity", "Fetched formValue: $formValue")
                                            }
                                            setupScreen(formValue)

                                        } else if (screen.id == "CCIF000" && newScreen.id != "000000F") {
                                            // Menampilkan pop-up gagal dengan pesan "NIK sudah terdaftar"
                                            val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                putExtra("MESSAGE_BODY", "NIK sudah terdaftar")
                                            }
                                            startActivity(intent)
                                        } else {
                                            handleScreenType(newScreen)
                                        }
                                    }
                                } ?: run {
                                    Log.e("FormActivity", "Failed to fetch response body")
                                }
                            }
                        } else {
                            Log.e("FormActivity", "Failed to create message body, request not sent")
                        }
                    }
                } else if (msg03Value == null) {
                    Toast.makeText(
                        this@FormActivity,
                        "Kode OTP telah kadaluarsa, mohon kirim ulang OTP.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
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
                val sendPengaduan = screen?.comp?.find { it.id == "G0002" }
                Log.d("Form", "Send Pengaduan : $sendPengaduan")
                if(sendPengaduan != null){
                    val bodyPengaduan = screen?.let { createBodyPengaduan(it) }
                    Log.d("FormActivity", "Create Message Pengaduan $bodyPengaduan")

                    bodyPengaduan?.let { sendPengaduanToApi(it) }
                }else if (messageBody != null) {
                    Log.d("FormActivity", "Message Body: $messageBody")
                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                        responseBody?.let { body ->
                            lifecycleScope.launch {
                                Log.e("FormActivity", "")
                                val screenJson = JSONObject(body)
                                val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                Log.e("FormActivity", "SCREEN ${screen.id} ")
                                Log.e("FormActivity", "NEW SCREEN ${newScreen.id} ")
                                if(sendOTPComponent != null) {
                                    var pinScreen = "P000001"
                                    var formValue =
                                        StorageImpl(applicationContext).fetchForm(pinScreen)
                                    if (formValue.isNullOrEmpty()) {
                                        formValue = withContext(Dispatchers.IO) {
                                            ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                pinScreen
                                            )
                                        }
                                        Log.i("FormActivity", "Fetched pinValue: $formValue")
                                    }
                                    val url = "http://108.137.154.8:8080/ARRest/fileupload"
                                    if(fileFotoOrang != null){
                                        // Mengunggah foto orang
                                        fileFotoOrang?.let { file ->
                                            uploadImageFile(file, url)
                                            Log.d("Foto", "Foto Orang Ada : $fileFotoOrang")
                                        }
                                    }else{
                                        Log.d("Foto", "Foto Orang Kosong")
                                    }
                                    if(fileFotoKTP != null){
                                        // Mengunggah KTP
                                        fileFotoKTP?.let { file ->
                                            uploadImageFile(file, url)
                                            Log.d("Foto", "Foto KTP Ada : $fileFotoKTP")
                                        }
                                    }else{
                                        Log.d("Foto", "Foto KTP Kosong")
                                    }
                                    if(signatureFile != null){
                                        // Mengunggah tanda tangan
                                        signatureFile?.let { file ->
                                            uploadImageFile(file, url)
                                            Log.d("Foto", "Signature Ada : $signatureFile")
                                        }
                                    }else{
                                        Log.d("Foto", "Tanda Tangan Kosong")
                                    }
                                    setupScreen(formValue)

                                    otpScreen = newScreen
                                }else {
                                    if (screen.id == "CCIF003" && newScreen.id == "000000D") {
                                        val message = newScreen?.comp?.find { it.id == "0000A" }
                                            ?.compValues?.compValue?.firstOrNull()?.value
                                            ?: "Unknown error"
                                        newScreen.id = "RCCIF02"
                                        var newScreenId = newScreen.id
                                        var formValue =
                                            StorageImpl(applicationContext).fetchForm(newScreenId)
                                        if (formValue.isNullOrEmpty()) {
                                            formValue = withContext(Dispatchers.IO) {
                                                ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                    newScreenId
                                                )
                                            }
                                            Log.i("FormActivity", "Fetched formValue: $formValue")
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
                                            StorageImpl(applicationContext).fetchForm(newScreenId)
                                        if (formValue.isNullOrEmpty()) {
                                            formValue = withContext(Dispatchers.IO) {
                                                ArrestCallerImpl(OkHttpClient()).fetchScreen(
                                                    newScreenId
                                                )
                                            }
                                            Log.i("FormActivity", "Fetched formValue: $formValue")
                                        }
                                        setupScreen(formValue)
                                    } else if (screen.id == "CCIF000" && newScreen.id != "000000F") {
                                        val intent =
                                            Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
                                                putExtra("MESSAGE_BODY", "NIK sudah terdaftar")
                                            }
                                        startActivity(intent)
                                    } else if (screen.id == "RCS0001" && newScreen.id != "000000F") {
                                        val intent =
                                            Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", "Pesan sudah teririm")
                                            }
                                        startActivity(intent)
                                    } else if (screen.id == "TF00003" && newScreen.id != "000000F") {
                                        val intent =
                                            Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", "Pesan sudah teririm")
                                            }
                                        startActivity(intent)
                                    } else if (screen.id == "BR001" && newScreen.id != "000000F") {
                                        val intent =
                                            Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", "Pesan sudah teririm")
                                            }
                                        startActivity(intent)
                                    } else if (screen.id == "BS001" && newScreen.id != "000000F") {
                                        val intent =
                                            Intent(
                                                this@FormActivity,
                                                PopupActivity::class.java
                                            ).apply {
                                                putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                                                putExtra("MESSAGE_BODY", "Pesan sudah teririm")
                                            }
                                        startActivity(intent)
                                    } else {
                                        handleScreenType(newScreen)
                                    }
                                }
                            }
                        } ?: run {
                            Log.e("FormActivity", "Failed to fetch response body")
                        }
                    }
                } else {
                    Log.e("FormActivity", "Failed to create message body, request not sent")
                }
            }
        }
    }

    private fun createMessageBodyForSaldoCheck(norek: String): JSONObject? {
        return try {
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val username = "admin"
            val imei = sharedPreferences.getString("imei", "") ?: ""
            val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
            val msgId = imei + timestamp
            val msgSi = "N00001"
            val msgDt = "$username|$norek|null|null"

            JSONObject().apply {
                put("msg", JSONObject().apply {
                    put("msg_id", msgId)
                    put("msg_ui", imei)
                    put("msg_si", msgSi)
                    put("msg_dt", msgDt)
                })
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Failed to create message body", e)
            null
        }
    }

    private fun checkSaldo(nominal: Double, norek: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val messageBody = createMessageBodyForSaldoCheck(norek)
        if (messageBody != null) {
            Log.d("FormActivity", "Requesting saldo with message: $messageBody")

            ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                responseBody?.let {
                    Log.d("FormActivity", "Received response: $it")
                    try {
                        val jsonResponse = JSONObject(it)
                        Log.d("FormActivity", "Parsed JSON response: $jsonResponse")
                        val msgObject = jsonResponse.optJSONObject("screen")

                        // Check if the screen object exists
                        if (msgObject != null) {
                            val comps = msgObject.optJSONObject("comps")
                            val compArray = comps?.optJSONArray("comp")
                            var saldo: Double? = null

                            compArray?.let {
                                for (i in 0 until it.length()) {
                                    val comp = it.getJSONObject(i)
                                    val label = comp.optString("comp_lbl")
                                    val value = comp.optJSONObject("comp_values")
                                        ?.optJSONArray("comp_value")?.optJSONObject(0)
                                        ?.optString("value")

                                    if (label == "Saldo Akhir") {
                                        saldo = value?.replace("-", "")?.replace(",", "")?.toDoubleOrNull()
                                    }
                                }
                            }

                            saldo?.let {
                                if (it >= nominal) {
                                    onSuccess()
                                } else {
                                    onError("Saldo tidak cukup")
                                }
                            } ?: run {
                                onError("No rekening tidak ditemukan")
                            }
                        } else {
                            onError("Gagal mengambil saldo")
                        }
                    } catch (e: Exception) {
                        Log.e("MenuActivity", "Failed to parse response", e)
                        onError("Gagal memproses respon saldo")
                    }
                } ?: run {
                    onError("Respon saldo kosong")
                }
            }
        } else {
            onError("Gagal membuat request body")
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
            val savedAkad = sharedPreferences.getString("akad", "") ?: ""
//            val imei = sharedPreferences.getString("imei", "")?: ""
            val username = "admin"
            Log.e("FormActivity", "Saved Username: $username")
            Log.e("FormActivity", "Saved Norekening: $savedNorekening")
            Log.e("FormActivity", "Saved Agen: $savedKodeAgen")
            Log.e("FormActivity", "Saved Nama Agen: $savedNamaAgen")
            val branchid = getKodeCabangFromPreferences()
            Log.e("FormActivity", "Saved Kode Cabang: $branchid")
            Log.e("FormActivity", "Saved Akad: $savedAkad")

            // Generate timestamp in the required format
            val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())

            val imei = sharedPreferences.getString("imei", "")?: ""
            Log.e("FormActivity", "Saved Imei: $imei")
            val msgUi = imei
//            val msgUi = "353471045058692"
            val msgId = msgUi + timestamp
            var msgSi = screen.actionUrl

            Log.d("FormActivity", "msgSi: $msgSi")
            Log.d("FormActivity", "PICK OTP: $pickOTP")

            // Kondisi untuk mengubah msgSi berdasarkan screen.id dan comp_id PIL03
            if (screen.comp.any { it.id == "PIL03" } && pickOTP == "SMS") {
                when (screen.id) {
                    // cek saldo
                    "CS00004" -> msgSi = "N00002"
                    // cek mutasi
                    "MB81124" -> msgSi = "E81122"
                    "TF00002" -> msgSi = "T00003"
                    "CCIF001" -> msgSi = "CC0002"
                    "RT001" -> msgSi = "RTT002"
                    "RS001" -> msgSi = "OTN002"
                    else -> msgSi = screen.actionUrl
                }
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
                    component.type == 1 && component.label == "NIK" -> {
                        componentValues[component.id] = nikValue ?: ""
                        Log.d("FormActivity", "Updated componentValues with nikValue for Component ID: ${component.id}")
                    }
                    component.type == 1 && component.label == "Kode Cabang" -> {
                        val branchid = getKodeCabangFromPreferences()
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

            }
            val unf01Value = componentValues["AG009"] ?: ""
            val rnr02Value = componentValues["RNR05"] ?: ""
            val unf03Value = componentValues["UNF05"] ?: ""
            val unf04Value = componentValues["SET10"] ?: ""
            val unf05Value = componentValues["NAR05"] ?: ""
            val rnr06Value = componentValues["TRF30"] ?: ""
            val rnr07Value = componentValues["TRT07 "] ?: ""
            val rnr08Value = componentValues["TRF31"] ?: ""
            val rnr09Value = componentValues["TRF30"] ?: ""
            val rnr10Value = componentValues["T0002"] ?: ""
            val rnr11Value = componentValues["SET20"] ?: ""

            when (screen.id) {
                "RCS0001" -> {
                    val currentDate = getCurrentDate()
                    val currentTime = getCurrentTime()
                    componentValues["MSG05"] = "Saldo no.rek $unf03Value a.n $unf05Value Rp.$rnr02Value pada $currentDate waktu: $currentTime."
                }
                "TF00003" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$rnr08Value , dengan nomor rekening: $rnr07Value . Berhasil melakukan transaksi transfer kepada $rnr09Value penerima dengan nominal $rnr10Value  ."
                }
                "BR001" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$unf01Value, dengan nomor rekening: $rnr11Value. Transaksi Tarik berhasil dilakukan."
                }
                "BS001" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$rnr06Value, dengan nomor rekening: $unf04Value. Transaksi Setor berhasil dilakukan."
                }
                else -> {
                    componentValues["MSG05"] = "Pesan tidak diketahui."
                }
            }
            val excludedCompIds = listOf("SIG01", "SIG02", "SIG03")

            var msgDt = ""
            Log.d("Screen", "SCREEN CREATE MESSAGE : ${screen.id}")
            if (screen.id == "AU00001") {
                msgDt = "$username|$savedNorekening|$savedNamaAgen|null"
            } else {
                msgDt = screen.comp
                    .filter { it.type != 7 && it.type != 15 && it.id != "MSG03" && it.id != "PIL03" && !excludedCompIds.contains(it.id) }
                    .joinToString("|") { component ->
                        componentValues[component.id] ?: ""
                    }
            }

            Log.d("Form", "Component : ${componentValues}")

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

    private fun createBodyPengaduan(screen: Screen): JSONObject? {
        return try {
            val body = JSONObject()
            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            val savedKodeAgen = sharedPreferences.getString("kode_agen", "") ?: ""
            val savedKategori = sharedPreferences.getString("kategori", "") ?: ""
            Log.d("FormActivity", "saved kategori: $savedKategori")
            val username = "admin"

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

    private fun loginUser() {
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

                Log.d(TAG, "Received response from server. Response code: ${response.code}, Response body: $responseData")

                if (response.isSuccessful && responseData != null) {
                    val jsonResponse = JSONObject(responseData)
                    val status = jsonResponse.optBoolean("status", false)

                    if (status) {
                        val token = jsonResponse.optString("token")
                        val userData = jsonResponse.optJSONObject("data")
                        val fullname = userData?.optString("fullname")
                        val id = userData?.optString("id")
                        val userStatus = userData?.optString("status")
                        val merchantData = userData?.optJSONObject("merchant")
                        val terminalArray = merchantData?.optJSONArray("terminal")

                        Log.d(TAG, "User status: $userStatus")
                        Log.d(TAG, "Terminal array: $terminalArray")

                        // Check user status
                        when (userStatus) {
                            "0" -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Akun Anda belum diaktivasi", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            "2" -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Akun Anda telah dinonaktifkan", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            "3" -> {
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
                            editor.putInt("pin", merchantData.optInt("pin"))
                            editor.putString("mid", merchantData.optString("mid"))

                            editor.apply()

                            val midTerminal = sharedPreferences.getString("mid", null)
                            Log.e ("MID TERMINAL", "MID TERMINAL : $midTerminal")
                            Log.e ("USERNAME", "USERNAME : $username")

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
                                createOTP { messageBody ->
                                    Log.d("LOGIN OTP", "Create OTP: $messageBody")
                                    if (messageBody != null) {
                                        Log.d("FormActivity", "Message Body OTP: $messageBody")
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                                    responseBody?.let { body ->
                                                        lifecycleScope.launch {
                                                            val screenJson = JSONObject(body)
                                                            val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                                            handleScreenType(newScreen)
                                                        }
                                                    } ?: run {
                                                        Log.e("FormActivity", "Failed to fetch response body")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Log.e("FormActivity", "Failed to create message body, request not sent")
                                    }
                                }

                            } else {
                                Log.d(TAG, "Terminal already exists.")
                                val terminalData = terminalArray.getJSONObject(0)
                                if (terminalData != null) {
                                    Log.d(TAG, "Terminal data found. Saving TID and IMEI.")

                                    val tid = terminalData.optString("tid")
                                    val terminalImei = terminalData.optString("imei")

                                    Log.d(TAG, "TID: $tid, IMEI: $terminalImei") // Log nilai TID dan IMEI

                                    editor.putString("tid", tid)
                                    editor.putString("imei", terminalImei)
                                    editor.apply()
                                } else {
                                    Log.d(TAG, "No terminal data found in the response.") // Log jika data terminal tidak ditemukan
                                }

                                editor.putInt("login_attempts", 0)
                                editor.apply()

//                                comment dulu biar bisa login
//                                val storedImeiTerminal = sharedPreferences.getString("imei", null)
//                                Log.d(TAG, "Stored IMEI: $storedImeiTerminal, Current IMEI: $imei") // Log IMEI yang tersimpan dan IMEI perangkat saat ini
//
//                                if (imei != null && imei != storedImeiTerminal) {
//                                    Log.d(TAG, "IMEI mismatch detected. Registered IMEI: $storedImeiTerminal, Current IMEI: $imei") // Log jika IMEI tidak cocok
//                                    val intentPopup = Intent(this@FormActivity, PopupActivity::class.java).apply {
//                                        putExtra("LAYOUT_ID", R.layout.pop_up_gagal)
//                                        putExtra("MESSAGE_BODY", "Perangkat yang digunakan tidak sesuai dengan yang didaftarkan.")
//                                        putExtra("RETURN_TO_ROOT", false)
//                                    }
//                                    startActivity(intentPopup)
//                                }else{
//                                    withContext(Dispatchers.Main) {
//                                        Toast.makeText(this@FormActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
//                                        navigateToScreen()
//                                    }
//                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@FormActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                                    navigateToScreen()
                                }

                                fun retrieveAuthToken(): String {
                                    // Return the stored auth token
                                    return "auth_token" // Replace this with the actual logic to retrieve the token
                                }

                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (!task.isSuccessful) {
                                        Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                                        return@addOnCompleteListener
                                    }

                                    // Get new FCM token
                                    val fcmToken = task.result
                                    Log.d("FCM", "FCM Token: $fcmToken")

                                    // Kirim token FCM ke server, memanggil fungsi dari MyFirebaseMessagingService
                                    val authToken = retrieveAuthToken() // Panggil fungsi untuk mendapatkan auth token
                                    MyFirebaseMessagingService.sendFCMTokenToServer(authToken, fcmToken)
                                }

//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(this@FormActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
//                                    navigateToScreen()
//                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@FormActivity, "Data User tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorMessage = jsonResponse.optString("message", "Login gagal.")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FormActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    handleFailedLoginAttempt()
                }
            } catch (e: Exception) {
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
        val currentAttempts = sharedPreferences.getInt("login_attempts", 0)

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
            editor.putInt("login_attempts", currentAttempts + 1)
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

        if (currentAttempts >= 3) {
            blockUserAccount()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FormActivity, "Akun Anda telah terblokir. Hubungi Call Center", Toast.LENGTH_SHORT).show()
            }
        } else {
            editor.putInt("login_attempts", currentAttempts + 1)
            editor.apply()
            val attemptsLeft = 3 - (currentAttempts + 1)
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

    private fun changePassword() {
        lifecycleScope.launch {
            try {
                val formBodyBuilder = FormBody.Builder()
                var oldPassword: String? = null
                var newPassword: String? = null

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
                        else -> formBodyBuilder.add(key, value)
                    }
                }

                val formBody = formBodyBuilder.build()

                val webCaller = WebCallerImpl()
                val fetchedValue = withContext(Dispatchers.IO) {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", "") ?: ""
                    val userId = sharedPreferences.getString("id", "") ?: ""
                    val response = webCaller.changePassword(userId, oldPassword ?: "", newPassword ?: "", token)
                    response?.string()
                }

                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        // Debug log for the raw response
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optBoolean("status", false) ?: JSONArray()
                        val message = jsonResponse.optString("message", "")
                        if (status as Boolean) {
                            Log.d(TAG, "Password changed successfully.")
                            Toast.makeText(this@FormActivity, message, Toast.LENGTH_SHORT).show()
                            navigateToScreen()  // Replace with your actual navigation function
                        } else {
                            Log.e(TAG, "Failed to change password.")
                            Toast.makeText(this@FormActivity, "Failed to change password: $message", Toast.LENGTH_SHORT).show()
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

                for ((key, value) in inputValues) {
                    when (key) {
                        "LSP01" -> {
                            oldPin = value
                            formBodyBuilder.add("old_password", value)
                        }
                        "LSP02" -> {
                            newPin = value
                            formBodyBuilder.add("new_password", value)
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
                    val response = webCaller.changePin(id, oldPin ?: "", newPin ?: "", token)
                    response?.string()
                }

                if (!fetchedValue.isNullOrEmpty()) {
                    try {
                        // Debug log for the raw response
                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                        val jsonResponse = JSONObject(fetchedValue)
                        val status = jsonResponse.optBoolean("status", false) ?: JSONArray()
                        val message = jsonResponse.optString("message", "")
                        if (status as Boolean) {
                            Log.d(TAG, "Password changed successfully.")
                            Toast.makeText(this@FormActivity, message, Toast.LENGTH_SHORT).show()
                            navigateToScreen()  // Replace with your actual navigation function
                        } else {
                            Log.e(TAG, "Failed to change password.")
                            Toast.makeText(this@FormActivity, "Failed to change password: $message", Toast.LENGTH_SHORT).show()
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val formBodyBuilder = FormBody.Builder()
                var newPassword: String? = null
                var username: String? = null

                // Mengumpulkan data dari inputValues
                for ((key, value) in inputValues) {
                    when (key) {
                        "LP001" -> {
                            newPassword = value
                            formBodyBuilder.add("new_password", value)
                        }
                        "UN001" -> {
                            username = value
                            formBodyBuilder.add("username", value)
                        }
                        else -> formBodyBuilder.add(key, value)
                    }
                }

                val formBody = formBodyBuilder.build()
                // Lanjutkan proses jika berhasil, simpan ke SharedPreferences
                if (newPassword != null && username != null) {
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putString("username", username)
                    editor.putString("new_password", newPassword)

                    editor.apply() // Simpan data
                }

                // Log form body for debugging
                Log.d(TAG, "Form body content: username=$username, newPassword=$newPassword")

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
                                Log.e("FormActivity", "Failed to fetch response body")
                            }
                        }
                    } else {
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
        val container = findViewById<LinearLayout>(R.id.menu_container)
        val editText = container.findViewWithTag<EditText>(component.id)

        if (editText == null) {
            return listOf("Input field for ${component.label} tidak ditemukan.")
        }

        val inputValue = inputValues[component.id] ?: ""
        val mandatory = component.opt.substring(0, 1).toIntOrNull() ?: 0
        val conType = component.opt.substring(2, 3).toIntOrNull() ?: 3
        val minLength = component.opt.substring(3, 6).toIntOrNull() ?: 0
        val maxLength = component.opt.substring(6).toIntOrNull() ?: Int.MAX_VALUE

        val errors = mutableListOf<String>()

        if (mandatory == 1 && inputValue.isEmpty()) {
            errors.add("${component.label} Wajib Diisi")
        } else if (inputValue.length < minLength) {
            errors.add("${component.label} Minimal $minLength karakter")
        } else if (inputValue.length > maxLength) {
            errors.add("${component.label} Maksimal $maxLength karakter")
        }

        when (conType) {
            0 -> {
                if (!inputValue.matches(Regex("[\\w\\s\\p{Punct}]*"))) {
                    errors.add("${component.label} Harus terdiri dari huruf, angka,dan simbol")
                }
            }
            1 -> {
                if (!inputValue.matches(Regex("[a-zA-Z\\s]*"))) {
                    errors.add("${component.label} Harus terdiri dari huruf")
                }
            }
            2 -> {
                if (!inputValue.matches(Regex("[0-9]*"))) {
                    errors.add("${component.label} Harus terdiri dari angka saja")
                }
            }
            4 -> {
                if (!inputValue.matches(Regex("\\d+(\\.\\d{1,2})?"))) {
                    errors.add("${component.label} Format uang tidak valid")
                }
            }
            3 -> {
                // No Constraint
            }
            else -> {
                errors.add("${component.label} Tipe validasi tidak dikenali")
            }
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
        val intent = Intent(this@FormActivity, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_MENU_ID, "AU00001")
        }
        startActivity(intent)
        finish()
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
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // Handle permission denial
            }
        }
    }

    private fun openCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            currentImageView?.setImageBitmap(photo)  // Update the correct ImageView

            // Simpan gambar ke file setelah foto diambil
            val fileName = if (photoCounter == 1) {
                "FOTO_${nikValue ?: "unknown"}.png" // Foto Orang
            } else {
                "KTP_${nikValue ?: "unknown"}.png" // Foto KTP
            }

            currentImageView?.let { imageView ->
                saveImageToFile(photo!!, fileName)?.let { file ->
                    if (photoCounter == 1) {
                        fileFotoOrang = file
                    } else {
                        fileFotoKTP = file
                    }
                }
            }
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
                Log.d("FormActivity", "OTP expired and cleared.")
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

    private fun resendOtp() {
        if (otpAttempts.size < 3) {
            otpAttempts.add(System.currentTimeMillis())
            val messageBody = lastMessageBody
            Log.d("FORM", "LAST MESSAGE : $messageBody")
            if (messageBody != null) {
                ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                    responseBody?.let { body ->
                        lifecycleScope.launch {
                            val screenJson = JSONObject(body)
                            Log.d("FormActivity", "Response POST: $body")
                            Log.d("FormActivity", "SCREEN JSON: $screenJson")

                            // Mendapatkan array komponen dari JSON
                            val compArray =
                                screenJson.getJSONObject("screen").getJSONObject("comps")
                                    .getJSONArray("comp")

                            // Mencari komponen dengan ID "MSG03"
                            var newMsg03Value: String? = null
                            for (i in 0 until compArray.length()) {
                                val compObject = compArray.getJSONObject(i)
                                if (compObject.getString("comp_id") == "MSG03") {
                                    newMsg03Value = compObject.getJSONObject("comp_values")
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
                                startOtpTimer()
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
                        Toast.makeText(
                            this@FormActivity,
                            "Gagal melakukan request ulang OTP",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Log.e("FormActivity", "Failed to create message body, request not sent")
                Toast.makeText(this@FormActivity, "Request body gagal dibuat", Toast.LENGTH_SHORT)
                    .show()
            }
        }else {
            val lastAttemptTime = otpAttempts.last()
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAttemptTime < otpCooldownTime) {
                Toast.makeText(
                    this@FormActivity,
                    "OTP send limit exceeded. Please wait 30 minutes.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                otpAttempts.clear()
                otpAttempts.add(System.currentTimeMillis())
                resendOtp()
            }
        }
    }

    private val webCallerImpl = WebCallerImpl()

    private fun createOTP(callback: (JSONObject?) -> Unit) {
        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                var merchantPhone = sharedPreferences.getString("merchant_phone", "") ?: ""
                val usernameLogin = sharedPreferences.getString("username", "") ?: ""
                val newPassword = sharedPreferences.getString("new_password", null)

                Log.d("FormActivity", "Saved New Password: $newPassword")
                Log.d("FormActivity", "Merchant OTP: $merchantPhone")

                if (!newPassword.isNullOrEmpty()) {
                    // Make the network call to get the phone by username
                    val response = withContext(Dispatchers.IO) {
                        webCallerImpl.getPhoneByUsername(usernameLogin)
                    }

                    Log.d("FormActivity", "Forgot password RESPONSE: $response")

                    if (response != null) {
                        try {
                            val jsonResponse = JSONObject(response)
                            val status = jsonResponse.optString("status")

                            if (status == "success") {
                                // Replace merchantPhone with phone from the response
                                merchantPhone = jsonResponse.optString("phone")
                                Log.d("FormActivity", "Phone replaced with: $merchantPhone")

                                // Save the updated phone to SharedPreferences
                                sharedPreferences.edit().putString("merchant_phone", merchantPhone).apply()

                                // IMEI, timestamp, and message details
                                val imei = "89b2c0aa8e0ac7c2" // Hardcoded IMEI for now
                                Log.e("FormActivity", "Saved IMEI: $imei")
                                val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
                                val msgUi = imei
                                val msgId = msgUi + timestamp
                                val msgSi = "SV0001"
                                val msgDt = "$usernameLogin|$merchantPhone"

                                val msgObject = JSONObject().apply {
                                    put("msg_id", msgId)
                                    put("msg_ui", msgUi)
                                    put("msg_si", msgSi)
                                    put("msg_dt", msgDt)
                                }

                                val msg = JSONObject().apply {
                                    put("msg", msgObject)
                                }

                                callback(msg) // Return the message object via callback
                            } else {
                                val message = jsonResponse.optString("message")
                                Toast.makeText(this@FormActivity, message, Toast.LENGTH_LONG).show()
                                Log.e("FormActivity", "Error from server: $message")
                                callback(null) // Return null on error
                            }
                        } catch (e: Exception) {
                            Log.e("FormActivity", "Failed to parse JSON response", e)
                            callback(null) // Return null on exception
                        }
                    } else {
                        Log.e("FormActivity", "Forgot password request failed")
                        callback(null) // Return null if response is null
                    }
                } else {
                    Log.e("FormActivity", "New password is null or empty.")
                    // Handle the case where newPassword is null or empty
                    val imei = "89b2c0aa8e0ac7c2" // Hardcoded IMEI for now
                    Log.e("FormActivity", "Saved IMEI: $imei")
                    val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())
                    val msgUi = imei
                    val msgId = msgUi + timestamp
                    val msgSi = "SV0001"
                    val msgDt = "$usernameLogin|$merchantPhone"

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

        Log.d(TAG, "Starting terminal creation process. IMEI: $imei, MID: $midTerminal")

        if (imei != null && midTerminal != null) {
            val createTerminalUrl = "http://reportntbs.selada.id/api/terminal/create/$imei/$midTerminal"
            Log.d(TAG, "Create terminal URL: $createTerminalUrl")

            val createTerminalRequest = Request.Builder()
                .url(createTerminalUrl)
                .post(FormBody.Builder().build())
                .addHeader("Authorization", "Bearer $token")
                .build()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val terminalResponse = client.newCall(createTerminalRequest).execute()
                    val terminalResponseData = terminalResponse.body?.string()

                    Log.d(TAG, "Terminal response received. Status code: ${terminalResponse.code}, Message: ${terminalResponse.message}")

                    if (terminalResponse.isSuccessful && terminalResponseData != null) {
                        Log.d(TAG, "Terminal response body: $terminalResponseData")

                        val terminalJsonResponse = JSONObject(terminalResponseData)
                        val terminalStatus = terminalJsonResponse.optBoolean("success", false)

                        if (terminalStatus) {
                            Log.d(TAG, "Terminal berhasil dibuat")

                            saveTerminalData(terminalJsonResponse)

                            val editor = sharedPreferences.edit()
                            editor.putInt("login_attempts", 0)
                            editor.apply()

                            val storedImeiTerminal = sharedPreferences.getString("imei", null)
                            if (imei != null && imei != storedImeiTerminal) {
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
                            Log.d(TAG, "Gagal Membuat Terminal Baru")
                        }
                    } else {
                        Log.d(TAG, "Gagal membuat terminal. HTTP Status Code: ${terminalResponse.code}, Message: ${terminalResponse.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error occurred while creating terminal", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d(TAG, "IMEI or MID is null. IMEI: $imei, MID: $midTerminal. Cannot create terminal.")
        }
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
        if (judul == "Ganti Perangkat") {
            sendRequestImei()
        }

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

                        val intent = Intent(this@FormActivity, PopupActivity::class.java).apply {
                            putExtra("LAYOUT_ID", R.layout.pop_up_berhasil)
                            putExtra("MESSAGE_BODY", "Pengaduan berhasil dikirim")
                            putExtra("RETURN_TO_ROOT", true)
                        }
                        startActivity(intent)
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


    private fun sendRequestImei() {
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
            "mid": "$midTerminal"
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
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/png".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()

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
}
