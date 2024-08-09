package id.co.bankntbsyariah.lakupandai.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Component
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.common.Screen
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.iface.StorageImpl
import id.co.bankntbsyariah.lakupandai.utils.ScreenParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import org.json.JSONException
import java.util.Calendar

class FormActivity : AppCompatActivity() {

    private var formId = Constants.DEFAULT_ROOT_ID
    private var isForm = false
    private val inputValues = mutableMapOf<String, String>()
    private var msg03Value: String? = null
    private var isOtpValidated = false
    // coba
    private val formInputs = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve formId from intent extras
        formId = intent.extras?.getString(Constants.KEY_FORM_ID) ?: Constants.DEFAULT_ROOT_ID
        Log.d("FormActivity", "formId: $formId")

        setInitialLayout()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                startActivity(
                    Intent(this@FormActivity, MenuActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        })

        lifecycleScope.launch {
            var formValue = StorageImpl(applicationContext).fetchForm(formId)
            if (formValue.isNullOrEmpty()) {
                withContext(Dispatchers.IO) {
                    val fv = ArrestCallerImpl(OkHttpClient()).fetchScreen(formId)
                    formValue = fv
                }
                Log.i("FormActivity", "Fetched formValue: $formValue")
            }
            if (formValue.isNullOrEmpty()) {
                findViewById<TextView>(R.id.error_message).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.menu_container).visibility = View.GONE
            } else {
                val screenJson = JSONObject(formValue)
                val screen: Screen = ScreenParser.parseJSON(screenJson)

                val sType = screen.type
                when (sType) {
                    Constants.SCREEN_TYPE_MENU -> {
                        // Move to menu
                    }
                    Constants.SCREEN_TYPE_POPUP_GAGAL,
                    Constants.SCREEN_TYPE_POPUP_SUKSES,
                    Constants.SCREEN_TYPE_POPUP_LOGOUT -> {
                        // Show popup
                    }
                    Constants.SCREEN_TYPE_POPUP_OTP -> {
                        val dialogView = layoutInflater.inflate(R.layout.pop_up, null)
                        val dialog = AlertDialog.Builder(this@FormActivity)
                            .setView(dialogView)
                            .create()

                        setupForm(screen, dialogView)
                        dialog.show()
                    }
                    else -> {
                        // Pass form
                    }
                }
                handleScreenTitle(screen.title)
                if (sType != 7) {
                    handleScreenTitle(screen?.title ?: "")
                    screen?.let { setupForm(it) }
                }
            }
        }

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
                setContentView(R.layout.activity_form2)
                Log.d("FormActivity", "Displaying activity_form")
            }
        }
    }

    //coba
//    private lateinit var currentScreen: Screen
//    fun initializeScreen(data: JSONObject) {
//        currentScreen = ScreenParser.parseJSON(data)
//    }

    private fun handleScreenTitle(screenTitle: String) {
        val layoutId = when {
            //coba
            screenTitle.contains("Form", ignoreCase = true) -> {
//                if (currentScreen.id != "TF00001") { // Memeriksa ID dari objek Screen
//                    formInputs.clear()
//                }
                R.layout.activity_form2
            }
            screenTitle.contains("Review", ignoreCase = true) -> R.layout.activity_review
            screenTitle.contains("Bayar", ignoreCase = true) -> R.layout.activity_bayar
            screenTitle.contains("Pilih", ignoreCase = true) -> R.layout.pilihan_otp
            else -> R.layout.activity_form2
        }
        if (layoutId != R.layout.activity_form2) {
            setContentView(layoutId)
            Log.d("FormActivity", "Displaying layout with ID: $layoutId")
        }
    }

    private fun setupForm(screen: Screen, containerView: View? = null) {
        val container = containerView?.findViewById<LinearLayout>(R.id.menu_container)
            ?: findViewById(R.id.menu_container)
        var buttonContainer = containerView?.findViewById<LinearLayout>(R.id.button_type_7_container)

        if (container == null) {
            Log.e("FormActivity", "Container is null.")
            return
        }

        container.removeAllViews()
        buttonContainer?.removeAllViews()

        Log.d("FormActivity", "Screen components: ${screen.comp}")
        for (component in screen.comp) {
            Log.d("FormActivity", "Component: $component")

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
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            textSize = 20f
                            setTypeface(null, Typeface.BOLD)
                            setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + 7)
                        })
                        addView(TextView(this@FormActivity).apply {
                            text = component.compValues?.compValue?.firstOrNull()?.value ?: ""
                            textSize = 18f
                        })
                        background = getDrawable(R.drawable.text_view_background)
                    }
                }

                2 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })
                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            background = getDrawable(R.drawable.edit_text_background)
                            id = View.generateViewId()
                            tag = component.id
//                            if (component.id in listOf(
//                                    "CIF04",
//                                    "D0001",
//                                    "D0002",
//                                    "ED001",
//                                    "B1007",
//                                    "SD001"
//                                )
//                            ) {
//                                inputType = android.text.InputType.TYPE_NULL
//                                setOnClickListener {
//                                    Log.d("FormActivity", "EditText clicked: ${component.id}")
//                                    showDatePickerDialog(this)
//                                }
//                            }
                        }
                        inputValues[component.id] = ""
                        editText.addTextChangedListener {
                            val input = it.toString()
                            inputValues[component.id] = input
                            formInputs[component.id] = input

                            Log.d("FormActivity", "Type 2: ${component.id} -> $input")
                        }
                        addView(editText)
                    }
                }

                3 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })
                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            inputType =
                                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                            background = getDrawable(R.drawable.edit_text_background)
                            id = View.generateViewId()
                        }
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

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })

                        val spinner = Spinner(this@FormActivity).apply {
                            val options = component.values.map { it.first }
                            val adapter = ArrayAdapter(
                                this@FormActivity,
                                android.R.layout.simple_spinner_item,
                                options
                            ).apply {
                                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            }
                            setAdapter(adapter)
                            setSelection(0, false)
                        }

                        addView(spinner)
                    }
                }

                6 -> {
                    LinearLayout(this@FormActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })
                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            inputType = android.text.InputType.TYPE_CLASS_PHONE
                            background = getDrawable(R.drawable.edit_text_background)
                            id = View.generateViewId()
                        }
                        inputValues[component.id] = ""
                        editText.addTextChangedListener {
                            inputValues[component.id] = it.toString()
                        }
                        addView(editText)
                    }
                }

                7 -> {
                    Button(this).apply {
                        text = component.label
                        setTextColor(getColor(R.color.white))
                        textSize = 18f
                        background = getDrawable(R.drawable.button_yellow)
                        setOnClickListener {
                            Log.d("FormActivity", "Screen Type: ${screen.type}")
                            if (screen.type == 7) {
                                val dialogView = layoutInflater.inflate(R.layout.pop_up, null)
                                val dialog = AlertDialog.Builder(this@FormActivity)
                                    .setView(dialogView)
                                    .create()

                                setupForm(screen, dialogView)
                                dialog.show()
                                dialogView.findViewById<Button>(R.id.button_type_7_container)
                                    ?.setOnClickListener {
                                        handleButtonClick(component, screen)
                                        dialog.dismiss()
                                    }
                            } else {
                                handleButtonClick(component, screen)
                            }
                        }
                    }
                }
                15 -> {
                    val inflater = layoutInflater
                    val otpView = inflater.inflate(R.layout.pop_up_otp, container, false)

                    val otpDigit1 = otpView.findViewById<EditText>(R.id.otpDigit1)
                    val otpDigit2 = otpView.findViewById<EditText>(R.id.otpDigit2)
                    val otpDigit3 = otpView.findViewById<EditText>(R.id.otpDigit3)
                    val otpDigit4 = otpView.findViewById<EditText>(R.id.otpDigit4)

                    val otpDigits = listOf(otpDigit1, otpDigit2, otpDigit3, otpDigit4)

                    otpDigits.forEach { digit ->
                        digit.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                val otpValue = otpDigits.joinToString(separator = "") { it.text.toString() }
                                inputValues["OTP"] = otpValue
                            }

                            override fun afterTextChanged(s: Editable?) {}
                        })
                    }
                    otpView
                }

                else -> {
                    null
                }
            }

            view?.let {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(20, 20, 20, 20)
                it.layoutParams = params

                if (component.type == 7) {
                    if (buttonContainer == null) {
                        buttonContainer = LinearLayout(this).apply {
                            id = View.generateViewId()  // Generate a new ID for the buttonContainer
                            orientation = LinearLayout.VERTICAL
                        }
                        container.addView(buttonContainer)
                        buttonContainer!!.addView(it)
                    }
                } else {
                    container.addView(it)
                }
            }
        }

    }

    private fun handleButtonClick(component: Component, screen: Screen?) {
        if (component.id == "KM001") {
            startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_MENU_ID, "MN00000")
            })
            finish()
        } else {
            val otpComponent = screen?.comp?.find { it.id == "OTP01" }
            if (otpComponent != null) {
                val otpValue = inputValues["OTP"]
                Log.e("OTP", "OTP: $otpValue")
                Log.e("MSG", "MSG: $msg03Value")
                if (msg03Value == otpValue) {
                    isOtpValidated = true
                    val messageBody = createMessageBody(screen)
                    if (messageBody != null) {
                        Log.d("FormActivity", "Message Body: $messageBody")
                        ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                            responseBody?.let { body ->
                                lifecycleScope.launch {
                                    val screenJson = JSONObject(body)
                                    val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                    handleScreenTitle(newScreen.title)
                                    if (newScreen.type == 7) {
                                        showPopup(newScreen, component)
                                    } else {
                                        setupForm(newScreen)
                                    }
                                }
                            } ?: run {
                                Log.e("FormActivity", "Failed to fetch response body")
                            }
                        }
                    } else {
                        Log.e("FormActivity", "Failed to create message body, request not sent")
                    }
                } else {
                    Toast.makeText(this@FormActivity, "Kode OTP yang dimasukkan salah", Toast.LENGTH_SHORT).show()
                    findViewById<EditText>(R.id.otpDigit1)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit2)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit3)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit4)?.text?.clear()
                    findViewById<EditText>(R.id.otpDigit1)?.error = "OTP salah"
                }
            } else {
                val messageBody = screen?.let { createMessageBody(it) }
                if (messageBody != null) {
                    Log.d("FormActivity", "Message Body: $messageBody")
                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                        responseBody?.let { body ->
                            lifecycleScope.launch {
                                val screenJson = JSONObject(body)
                                val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                handleScreenTitle(newScreen.title)
                                if (newScreen.type == 7) {
                                    showPopup(newScreen, component)
                                } else {
                                    setupForm(newScreen)
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

    private fun showPopup(screen: Screen, component: Component) {
        val dialogView = layoutInflater.inflate(R.layout.pop_up, null)
        val dialog = AlertDialog.Builder(this@FormActivity)
            .setView(dialogView)
            .create()

        setupForm(screen, dialogView)
        dialog.show()

        val buttonContainer = dialogView.findViewById<LinearLayout>(R.id.button_type_7_container)
        val button = Button(this).apply {
            text = component.label
            setTextColor(getColor(R.color.white))
            textSize = 18f
            background = getDrawable(R.drawable.button_yellow)
            setOnClickListener {
                handleButtonClick(component, screen)
                if (isOtpValidated) {
                    dialog.dismiss()
                }
            }
        }
        buttonContainer.addView(button)
    }



    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear)
            editText.setText(formattedDate)
            editText.error = null // Clear any previous error message
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun createMessageBody(screen: Screen): JSONObject? {
        return try {
            val msg = JSONObject()
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val savedUsername = sharedPreferences.getString("username", "") ?: ""
            Log.e("FormActivity", "Saved Username: $savedUsername")

            // Get device Android ID
//            val msgUi = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
//
            // Generate timestamp in the required format
            val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())

            // Concatenate msg_ui with timestamp to generate msg_id
//            val msgId = msgUi + timestamp

//            val msgId = "353471045058692200995"
            val msgUi = "353471045058692"
            val msgId = msgUi + timestamp
            val msgSi = screen.actionUrl

            val componentValues = mutableMapOf<String, String>()
            screen.comp.filter { it.type != 7 && it.type != 15 }.forEach { component ->
                Log.d("FormActivity", "Component: $component")

                when {
                    component.type == 1 && component.label == "Username" -> {
                        componentValues[component.id] = savedUsername
                        Log.d("FormActivity", "Updated componentValues with savedUsername for Component ID: ${component.id}")
                    }
                    component.type == 1 -> {
                        val value = (component.values.get(0)?.second ?: "") as String
                        componentValues[component.id] = value
                        Log.d("FormActivity", "Updated componentValues with value for Component ID: ${component.id}")
                    }
                    else -> {
                        componentValues[component.id] = inputValues[component.id] ?: ""
                    }
                }
            }

//            val msgDt = screen.comp.filter { it.type != 7 && it.type != 15 }
//                .joinToString("|") { component ->
//                    componentValues[component.id] ?: ""
//                }

            // coba
            val otpinput = inputValues["OTP"]
            Log.d("FormActivity", "OTP: $otpinput")
            Log.d("FormActivity", "msg03Value: $msg03Value")
            val msgDt = if (!msg03Value.isNullOrEmpty() && !inputValues["OTP"].isNullOrEmpty() && inputValues["OTP"] == msg03Value) {
                //coba
//                Log.d("FormActivity", "Hello")
//                Log.d("FormActivity", "Form Inputs : $formInputs")
//                val savedValues = mutableListOf(savedUsername)
//                savedValues.addAll(formInputs.values) // Fix here
//                savedValues.joinToString("|")
                Log.d("FormActivity", "Gak Hello")
                screen.comp.filter { it.type != 7 && it.type != 15 && it.id != "MSG03" }
                    .joinToString("|") { component ->
                        componentValues[component.id] ?: ""
                    }
            } else {
                Log.d("FormActivity", "Gak Hello")
                screen.comp.filter { it.type != 7 && it.type != 15 && it.id != "MSG03" }
                    .joinToString("|") { component ->
                        componentValues[component.id] ?: ""
                    }
            }


            val msgObject = JSONObject().apply {
                put("msg_id", msgId)
                put("msg_ui", msgUi)
                put("msg_si", msgSi)
                put("msg_dt", msgDt)
            }

            msg.put("msg", msgObject)

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

}
