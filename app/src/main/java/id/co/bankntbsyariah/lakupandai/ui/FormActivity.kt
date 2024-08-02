package id.co.bankntbsyariah.lakupandai.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
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
import android.graphics.Typeface
import androidx.core.widget.addTextChangedListener

class FormActivity : AppCompatActivity() {

    private var formId = Constants.DEFAULT_ROOT_ID
    private var isForm = false
    private val inputValues = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve formId from intent extras
        formId = intent.extras?.getString(Constants.KEY_FORM_ID) ?: Constants.DEFAULT_ROOT_ID
        Log.d("FormActivity", "formId: $formId")

        // Set the appropriate layout based on the formId
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
                setContentView(R.layout.activity_form)
                Log.d("FormActivity", "Displaying activity_form")
            }
        }

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
                    else -> {
                        // Pass form
                    }
                }
                handleScreenTitle(screen.title)
                setupForm(screen)
            }
        }
    }

    private fun handleScreenTitle(screenTitle: String) {
        when {
            screenTitle.contains("Form", ignoreCase = true) -> {
                setContentView(R.layout.activity_form2)
                Log.d("FormActivity", "Displaying activity_form2")
            }
            screenTitle.contains("Review", ignoreCase = true) -> {
                setContentView(R.layout.activity_review)
                Log.d("FormActivity", "Displaying activity_review")
            }
            screenTitle.contains("Bayar", ignoreCase = true) -> {
                setContentView(R.layout.activity_bayar)
                Log.d("FormActivity", "Displaying activity_bayar")
            }
            else -> {
                // Optionally handle the case where none of the keywords match
                setContentView(R.layout.activity_form)
                Log.d("FormActivity", "Displaying activity_form")
            }
        }
    }

    private fun setupForm(screen: Screen) {
        Log.d("FormActivity", "Screen components: ${screen.comp}")
        val container = findViewById<LinearLayout>(R.id.menu_container)
        val buttonContainer = findViewById<LinearLayout>(R.id.button_type_7_container)
        if (container == null || buttonContainer == null) {
            Log.e("FormActivity", "One of the containers is null. container: $container, buttonContainer: $buttonContainer")
            return
        }

        container.removeAllViews()
        buttonContainer.removeAllViews()

        Log.d("FormActivity", "Screen components: ${screen.comp}")

        for (component in screen.comp) {
            Log.d("FormActivity", "Component: $component")
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
                        }
                        inputValues[component.id] = ""
                        editText.addTextChangedListener {
                            inputValues[component.id] = it.toString()
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
                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
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
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })

                        val spinner = Spinner(this@FormActivity).apply {
                            val options = component.values.map { it.first }
                            val adapter = ArrayAdapter(this@FormActivity, android.R.layout.simple_spinner_item, options)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            this.adapter = adapter
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }

                        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                                inputValues[component.id] = component.values[position].first
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {
                                // Do nothing
                            }
                        }

                        addView(spinner)
                    }
                }
                5 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })

                        component.values.forEach { value ->
                            val checkBox = CheckBox(this@FormActivity).apply {
                                text = value.first
                            }
                            checkBox.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    inputValues[component.id] = value.first
                                } else {
                                    inputValues.remove(component.id)
                                }
                            }
                            addView(checkBox)
                        }
                    }
                }
                6 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })

                        val radioGroup = RadioGroup(this@FormActivity).apply {
                            orientation = RadioGroup.VERTICAL
                        }

                        component.values.forEach { value ->
                            val radioButton = RadioButton(this@FormActivity).apply {
                                text = value.first
                            }
                            radioButton.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    inputValues[component.id] = value.first
                                }
                            }
                            radioGroup.addView(radioButton)
                        }

                        addView(radioGroup)
                    }
                }
                7 -> {
                    Button(this).apply {
                        text = component.label
                        setTextColor(getColor(R.color.white))
                        textSize = 18f
                        background = getDrawable(R.drawable.button_yellow)
                        setOnClickListener {
                            val messageBody = createMessageBody(screen)
                            if (messageBody != null) {
                                Log.d("FormActivity", "Message Body: $messageBody")
                                ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                                    responseBody?.let { body ->
                                        lifecycleScope.launch {
                                            val screenJson = JSONObject(body)
                                            val screen: Screen = ScreenParser.parseJSON(screenJson)
                                            handleScreenTitle(screen.title)
                                            setupForm(screen)
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
                    buttonContainer.addView(it)
                } else {
                    container.addView(it)
                }
            }

        }
    }
    private fun createMessageBody(screen: Screen): JSONObject? {
        return try {
            val msg = JSONObject()
            val msgId = "353471045058692200995"
            val msgUi = "353471045058692"
            val msgSi = "N00001"
            val msgDt = screen.comp.filter { it.type != 7 }
                .joinToString("|") { inputValues[it.id] ?: "" }

            val msgObject = JSONObject()
            msgObject.put("msg_id", msgId)
            msgObject.put("msg_ui", msgUi)
            msgObject.put("msg_si", msgSi)
            msgObject.put("msg_dt", msgDt)

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

    private fun navigateToScreen(screenId: String) {
        val intent = Intent(this, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, screenId)
        }
        startActivity(intent)
    }

}