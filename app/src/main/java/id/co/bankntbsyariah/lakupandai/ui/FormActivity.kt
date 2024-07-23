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

class FormActivity : AppCompatActivity() {

    private var formId = Constants.DEFAULT_ROOT_ID
    private var isForm = false

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
                setupForm(screen)
            }
        }
    }

    private fun setupForm(screen: Screen) {
        val container = findViewById<LinearLayout>(R.id.menu_container)
        container.removeAllViews()

        for (component in screen.comp) {
            val view = when (component.type) {
                0 -> {
                    val inflater = layoutInflater
                    val view = inflater.inflate(R.layout.recycler_view_menu_item, null)

                    val title = view.findViewById<TextView>(R.id.title)
                    val subhead = view.findViewById<TextView>(R.id.subhead)
                    val body = view.findViewById<TextView>(R.id.body)

                    title.text = component.label

                    subhead.visibility = View.GONE
                    body.visibility = View.GONE

                    view
                }
                1 -> {
                    TextView(this).apply {
                        text = component.label
                    }
                }
                2 -> {
                    EditText(this).apply {
                        hint = component.label
                    }
                }
                3 -> {
                    EditText(this).apply {
                        hint = component.label
                        inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                }
                4 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                        })

                        addView(Spinner(this@FormActivity).apply {
                            val options = component.values.map { it.first }
                            val adapter = ArrayAdapter(this@FormActivity, android.R.layout.simple_spinner_item, options)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            this.adapter = adapter
                        })
                    }
                }
                5 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                        })

                        component.values.forEach { value ->
                            addView(CheckBox(this@FormActivity).apply {
                                text = value.first
                            })
                        }
                    }
                }
                6 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                        })

                        val radioGroup = RadioGroup(this@FormActivity).apply {
                            orientation = RadioGroup.VERTICAL
                        }

                        component.values.forEach { value ->
                            radioGroup.addView(RadioButton(this@FormActivity).apply {
                                text = value.first
                            })
                        }

                        addView(radioGroup)
                    }
                }
                7 -> {
                    Button(this).apply {
                        text = component.label
                        setOnClickListener {
                            // Handle button click
                        }
                    }
                }
                else -> {
                    null
                }
            }

            view?.let { container.addView(it) }
        }
    }
}
