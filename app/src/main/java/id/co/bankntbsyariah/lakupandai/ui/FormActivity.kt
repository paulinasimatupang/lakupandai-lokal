package id.co.bankntbsyariah.lakupandai.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
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
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONException
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import id.co.bankntbsyariah.lakupandai.common.Mutation
import id.co.bankntbsyariah.lakupandai.ui.adapter.MutationAdapter
import okhttp3.internal.format
import java.text.NumberFormat

class FormActivity : AppCompatActivity() {

    private var formId = Constants.DEFAULT_ROOT_ID
    private val inputValues = mutableMapOf<String, String>()
    private var msg03Value: String? = null
    private var isOtpValidated = false
    private var otpDialog: AlertDialog? = null
    private var nikValue: String? = null
    private var nominalValue = 0.0
    private var feeValue = 0.0

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

        setInitialLayout()

        setupWindowInsets()

        handleBackPress()

        lifecycleScope.launch {
            val formValue = loadForm()
            setupScreen(formValue)
        }
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
                finish()
                startActivity(
                    Intent(this@FormActivity, MenuActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
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
            screenTitle.contains("Form", ignoreCase = true) -> R.layout.activity_form2
            screenTitle.contains("Review", ignoreCase = true) -> R.layout.activity_review
            screenTitle.contains("Bayar", ignoreCase = true) -> R.layout.activity_bayar
            screenTitle.contains("Pilih", ignoreCase = true) -> R.layout.pilihan_otp
            screenTitle.contains("Transfer", ignoreCase = true) -> R.layout.activity_transfer
            else -> R.layout.activity_form
        }
        if (layoutId != R.layout.activity_form) {
            setContentView(layoutId)
            Log.d("FormActivity", "Displaying layout with ID: $layoutId")
        }
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

        // lia
        var norekComponent: Component? = null
        var nominalComponent: Component? = null
        var namaDepan: String? = null
        var extraText = ""

        for (component in screen.comp) {
            when (component.id) {
                "TRF27", "AG001", "TRF26" -> norekComponent = component
                "TFR24", "AG002", "AG005" -> {
                    nominalComponent = component
                    val fullName = getComponentValue(component)
                    namaDepan = fullName?.split(" ")?.firstOrNull()?.take(1) ?: ""
                }
            }
            if (screen.id == "TF00003" && component.id == "ST003") {
                val transaksiBerhasilTextView = findViewById<TextView>(R.id.success)

                transaksiBerhasilTextView?.let {
                    val newText = getComponentValue(component)
                    if (!newText.isNullOrEmpty()) {
                        it.text = newText
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

            if (component.id == "TRF27" || component.id == "TFR24" || component.id == "AG001" ||
                component.id == "AG002" || component.id == "TRF26" || component.id == "AG005" ||
                (component.id == "ST003" && screen.id == "TF00003") || component.id == "D1004"
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
                    } else {
                        LinearLayout(this@FormActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            //aul
                            val componentValue = getComponentValue(component)
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

                                    val saldo = saldoStr.toDoubleOrNull()?: 0.0

                                    formatRupiah(saldo)
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
                                setPadding(3.dpToPx(), 3.dpToPx(), 16.dpToPx(), 2.dpToPx())
                            } else {
                                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                            }

                            if (component.id == "APY00") {
                                addView(TextView(this@FormActivity).apply {
                                    text = component.label
                                    textSize = 15f
                                    setTypeface(null, Typeface.BOLD)
                                    setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    setTextColor(
                                        ContextCompat.getColor(
                                            this@FormActivity,
                                            R.color.black
                                        )
                                    )
                                })
                            } else {
                                addView(TextView(this@FormActivity).apply {
                                    text = component.label
                                    textSize = 15f
                                    setTypeface(null, Typeface.NORMAL)
                                    if (screen.id == "TF00003") {
                                        setPadding(3.dpToPx(), 3.dpToPx(), 16.dpToPx(), 2.dpToPx())
                                    } else {
                                        setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    }
                                    setTextColor(
                                        ContextCompat.getColor(
                                            this@FormActivity,
                                            R.color.black
                                        )
                                    )
                                })
                                addView(TextView(this@FormActivity).apply {
                                    text = formattedValue
                                    textSize = 18f
                                    if (screen.id == "TF00003") {
                                        setPadding(3.dpToPx(), 0, 16.dpToPx(), 2.dpToPx())
                                    } else {
                                        setPadding(16.dpToPx(), 0, 16.dpToPx(), 10.dpToPx())
                                    }
                                })
                            }
                            if (screen.id != "TF00003") {
                                background = ContextCompat.getDrawable(
                                    this@FormActivity,
                                    R.drawable.text_view_background
                                )
                            }
                        }
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
                        }
                        inputValues[component.id] = ""
                        nikValue = null

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
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            setTypeface(null, Typeface.BOLD)
                        })
                        val editText = EditText(this@FormActivity).apply {
                            hint = component.label
                            inputType =
                                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                            background = getDrawable(R.drawable.pass_bg)
                            id = View.generateViewId()
                            textSize = 18f

                            // Adjust the padding to move the hint text slightly to the right
                            setPadding(
                                48,
                                paddingTop,
                                48,
                                paddingBottom
                            ) // Adjust left and right padding

                            // Set the eye icon to the right of the EditText
                            setCompoundDrawablesWithIntrinsicBounds(
                                0,
                                0,
                                R.drawable.ic_eye_closed,
                                0
                            )

                            // Add padding between the text and eye icon
                            setCompoundDrawablePadding(16)

                            // Toggle visibility on eye icon touch
                            setOnTouchListener { v, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    if (event.rawX >= (right - compoundDrawables[2].bounds.width() - paddingRight)) {
                                        if (inputType == android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_open, 0)
                                        } else {
                                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
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

                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            textSize = 16f
                            setTypeface(null, Typeface.BOLD)
                        })

                        val spinner = Spinner(this@FormActivity).apply {
                            background = getDrawable(R.drawable.combo_box)
                            val options = mutableListOf("Pilih ${component.label}") + component.values.map { it.first }
                            val adapter = ArrayAdapter(this@FormActivity, android.R.layout.simple_spinner_item, options)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            this.adapter = adapter
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
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

                                    val selectedValue = component.values[position - 1].first
                                    val branchid = component.compValues?.compValue?.find {
                                        it.print == selectedValue
                                    }?.value?.replace("[OI]", "")

                                    Log.d(
                                        "FormActivity",
                                        "Component ID: ${component.id}, Selected Value: $selectedValue, Position: $position"
                                    )

                                    when (component.id) {
                                        "CB001" -> {
                                            inputValues[component.id] = branchid ?: ""
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

                                        "Cif13" -> {
                                            val cif13Codes = mapOf(
                                                0 to "5201",
                                                1 to "5202",
                                                2 to "5203",
                                                3 to "5204",
                                                4 to "5205",
                                                5 to "5206",
                                                6 to "5207",
                                                7 to "5208",
                                                8 to "5271",
                                                9 to "5272"
                                            )
                                            inputValues[component.id] =
                                                cif13Codes[position] ?: "5201"
                                            Log.d(
                                                "FormActivity",
                                                "Value set to: ${cif13Codes[position] ?: "5201"}"
                                            )
                                        }

                                        "CIF06", "CIF25" -> inputValues[component.id] =
                                            (position + 1).toString()

                                        "CIF14" -> {
                                            val provinceCodes = mapOf(
                                                0 to "11",
                                                1 to "12",
                                                2 to "13",
                                                3 to "14",
                                                4 to "15",
                                                5 to "16",
                                                6 to "17",
                                                7 to "18",
                                                8 to "19",
                                                9 to "21",
                                                10 to "31",
                                                11 to "32",
                                                12 to "33",
                                                13 to "34",
                                                14 to "35",
                                                15 to "36",
                                                16 to "51",
                                                17 to "52",
                                                18 to "53",
                                                19 to "61",
                                                20 to "62",
                                                21 to "63",
                                                22 to "64",
                                                23 to "65",
                                                24 to "71",
                                                25 to "72",
                                                26 to "73",
                                                27 to "74",
                                                28 to "75",
                                                29 to "76",
                                                30 to "81",
                                                31 to "82",
                                                32 to "91",
                                                33 to "94",
                                                34 to "99"
                                            )
                                            inputValues[component.id] =
                                                provinceCodes[position] ?: "99"
                                            Log.d(
                                                "FormActivity",
                                                "Value set to: ${provinceCodes[position] ?: "99"}"
                                            )
                                        }

                                        else -> inputValues[component.id] = position.toString()
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
                5 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            textSize = 18f
                            setTypeface(null, Typeface.BOLD)
                        })

                        component.values.forEachIndexed { index, value ->
                            val checkBox = CheckBox(this@FormActivity).apply {
                                text = value.first
                            }
                            checkBox.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    inputValues[component.id] = index.toString()
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
                            textSize = 18f
                        })

                        val radioGroup = RadioGroup(this@FormActivity).apply {
                            orientation = RadioGroup.VERTICAL
                        }

                        component.values.forEachIndexed { index, value ->
                            val radioButton = RadioButton(this@FormActivity).apply {
                                text = value.first
                                textSize = 18f
                            }
                            radioButton.setOnCheckedChangeListener { _, isChecked ->
                                val valueToSave = if (component.id in listOf("CIF05", "CIF17", "CIF07", "CIF21")) {
                                    (index + 1).toString()
                                } else {
                                    index.toString()
                                }
                                if (isChecked) {
                                    inputValues[component.id] = valueToSave
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
                        val background = when (component.id) {
                            "IYA01" -> getDrawable(R.drawable.button_green)
                            "TDK01" -> getDrawable(R.drawable.button_red)
                            else -> getDrawable(R.drawable.button_yellow)
                        }
                        setBackground(background)
                        setOnClickListener {
                            Log.d("FormActivity", "Screen Type: ${screen.type}")
                            if (formId == "AU00001") {
                                loginUser()
                            }else{
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

                    otpDigits.forEachIndexed { index, digit ->
                        digit.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                val otpValue = otpDigits.joinToString(separator = "") { it.text.toString() }
                                inputValues["OTP"] = otpValue

                                s?.let {
                                    when {
                                        it.length == 1 -> {
                                            val nextIndex = index + 1
                                            if (nextIndex < otpDigits.size) {
                                                otpDigits[nextIndex].requestFocus()
                                            }
                                        }
                                        it.length == 0 -> {
                                            val prevIndex = index - 1
                                            if (prevIndex >= 0) {
                                                otpDigits[prevIndex].requestFocus()
                                            }
                                        }
                                        it.length > 1 -> {
                                            val nextIndex = index + 1
                                            if (nextIndex < otpDigits.size) {
                                                otpDigits[nextIndex].requestFocus()
                                            }
                                        }
                                    }
                                }
                            }

                            override fun afterTextChanged(s: Editable?) {}
                        })
                    }

                    otpView
                }

                16 -> {
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
                        inputType = android.text.InputType.TYPE_NULL
                        setOnClickListener {
                            Log.d("FormActivity", "EditText clicked: ${component.id}")
                            showDatePickerDialog(this) { selectedDate ->
                                inputValues[component.id as String] = selectedDate
                            }
                        }
                    }
                    addView(editText)
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

                when {
                    component.type == 7 && component.id == "KM001" -> {
                        if (buttonContainer == null) {
                            val newButtontf = LinearLayout(this).apply {
                                id = View.generateViewId()
                                orientation = LinearLayout.VERTICAL
                            }
                            container.addView(newButtontf)
                            buttonContainer = newButtontf
                        }
                        if (it is Button) {
                            buttontf?.addView(it)
                        } else {
                            Log.e("FormActivity", "View is not a Button, skipping addition to buttonContainer")
                        }
                    }
                    else -> {
                        container.addView(it)
                    }
                }


            }
        }

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
                    getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                val savedNorekening = sharedPreferences.getString("norekening", "").toString()
                if (currentValue == "null" && savedNorekening != "0") {
                    Log.d("FormActivity", "No Rekening Agen diisi dengan nilai: $savedNorekening")
                    component.compValues?.compValue?.firstOrNull()?.value = savedNorekening
                } else {
                    Log.d("FormActivity", "No Rekening Agen sudah terisi dengan: $savedNorekening")
                }
            }
            "Nama Rekening Agen" -> {
                val sharedPreferences =
                    getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
                    getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                val savedKodeAgen = sharedPreferences.getInt("merchant_id", 0).toString()
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

    private fun handleButtonClick(component: Component, screen: Screen?) {
//        val isComponentValid = validateComponent(component)

//        if (screen?.actionUrl == "CC0000" && component.id == "CF001" && !isComponentValid) {
//            Log.d("FormActivity", "Service ID CC0000, Component CF001 invalid, but proceeding to next screen")
//            navigateToCreate()
//            return
//        } else {
//            Toast.makeText(this, "Validasi gagal", Toast.LENGTH_SHORT).show()
//        }
        val allErrors = mutableListOf<String>()

        screen?.comp?.forEach { comp ->
            if (comp.type == 2) {
                allErrors.addAll(validateInput(comp))
            }
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

        if (component.id == "KM001") {
            startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_MENU_ID, "MN00000")
            })
            finish()
        } else if (component.id == "OUT00") {

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
        } else {
            val otpComponent = screen?.comp?.find { it.id == "OTP01" }
            if (otpComponent != null) {
                val otpValue = inputValues["OTP"]
                Log.e("OTP", "OTP: $otpValue")
                Log.e("MSG", "MSG: $msg03Value")
                if (msg03Value == otpValue) {
                    isOtpValidated = true
                    otpDialog?.dismiss()
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
                                    if (screen.id == "CCIF000" && newScreen.id == "000000F") {
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
                    findViewById<EditText>(R.id.otpDigit1)?.error = "OTP salah"
                }
            } else {
                val messageBody = screen?.let { createMessageBody(it) }
                if (messageBody != null) {
                    Log.d("FormActivity", "Message Body: $messageBody")
                    ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                        responseBody?.let { body ->
                            lifecycleScope.launch {
                                Log.e("FormActivity", "")
                                val screenJson = JSONObject(body)
                                val newScreen: Screen = ScreenParser.parseJSON(screenJson)
                                Log.e("FormActivity", "SCREEN ${screen.id} ")
                                Log.e("FormActivity", "NEW SCREEN ${newScreen.id} ")
                                if (screen.id == "CCIF000" && newScreen.id == "000000F") {
                                    newScreen.id = "CCIF001"
                                    var newScreenId = newScreen.id
                                    var formValue = StorageImpl(applicationContext).fetchForm(newScreenId)
                                    if (formValue.isNullOrEmpty()) {
                                        formValue = withContext(Dispatchers.IO) {
                                            ArrestCallerImpl(OkHttpClient()).fetchScreen(newScreenId)
                                        }
                                        Log.i("FormActivity", "Fetched formValue: $formValue")
                                    }
                                    setupScreen(formValue)
                                } else if (screen.id == "CCIF000" && newScreen.id != "000000F") {
                                    // Menampilkan pop-up gagal dengan pesan "NIK sudah terdaftar"
                                    val intent =
                                        Intent(this@FormActivity, PopupActivity::class.java).apply {
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
        }
    }

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
                if (!inputValue.matches(Regex("[a-zA-Z0-9]*"))) {
                    errors.add("${component.label} Harus terdiri dari huruf dan angka")
                }
            }
            1 -> {
                if (!inputValue.matches(Regex("[a-zA-Z]*"))) {
                    errors.add("${component.label} Harus terdiri dari huruf saja")
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

    private fun createMessageBody(screen: Screen): JSONObject? {
        return try {
            val msg = JSONObject()
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val savedNorekening = sharedPreferences.getString("norekening", "") ?: ""
            val savedKodeAgen = sharedPreferences.getInt("merchant_id", 0)
            val username = "lakupandai"
            Log.e("FormActivity", "Saved Username: $username")
            Log.e("FormActivity", "Saved Norekening: $savedNorekening")
            Log.e("FormActivity", "Saved Agen: $savedKodeAgen")

            // Get device Android ID
//            val msgUi = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
//
            // Generate timestamp in the required format
            val timestamp = SimpleDateFormat("MMddHHmmssSSS", Locale.getDefault()).format(Date())

            // Concatenate msg_ui with timestamp to generate msg_id
//            val msgId = msgUi + timestamp
//
//            // Use actionUrl from screen; if null, msg_si will be nul

//            val msgId = "353471045058692200995"
            val msgUi = "353471045058692"
            val msgId = msgUi + timestamp
            val msgSi = screen.actionUrl

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
                        Log.d(
                            "FormActivity",
                            "Updated componentValues with savedNorekening for Component ID: ${component.id}"
                        )
                    }
                    component.type == 1 && component.label == "Kode Agen" -> {
                        componentValues[component.id] = savedKodeAgen.toString()
                        Log.d("FormActivity", "Kode Agen : $savedKodeAgen")
                        Log.d(
                            "FormActivity",
                            "Updated componentValues with savedKodeAgen for Component ID: ${component.id}"
                        )
                    }
                    component.type == 1 && component.label == "NIK" -> {
                        // Use nikValue if component label is "NIK"
                        componentValues[component.id] = nikValue ?: ""
                        Log.d(
                            "FormActivity",
                            "Updated componentValues with nikValue for Component ID: ${component.id}"
                        )
                    }
                    component.type == 1 && component.label != "NIK" -> {
                        val value = (component.values.get(0)?.second ?: "") as String
                        componentValues[component.id] = value
                        Log.d(
                            "FormActivity",
                            "Updated componentValues with value for Component ID: ${component.id}"
                        )
                    }

                    else -> {
                        componentValues[component.id] = inputValues[component.id] ?: ""
                    }
                }
            }

            val msgDt = screen.comp.filter { it.type != 7 && it.type != 15 && it.id != "MSG03" }
                .joinToString("|") { component ->
                    componentValues[component.id] ?: ""
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
                .url("http://api.selada.id/api/auth/login")
                .post(formBody)
                .build()

            Log.d(TAG, "Sending login request to server...")

            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                Log.d(TAG, "Received response from server. Response code: ${response.code}, Response body: $responseData")

                if (response.isSuccessful && responseData != null) {
                    val jsonResponse = JSONObject(responseData)
                    val token = jsonResponse.optString("token")
                    val fullname = jsonResponse.optJSONObject("data").optString("fullname")
                    val merchantData = jsonResponse.optJSONObject("data")?.optJSONObject("merchant")

                    if (token.isNotEmpty() && merchantData != null) {
                        val sharedPreferences =
                            getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        // Menyimpan data pengguna
                        editor.putString("username", username)
                        editor.putString("token", token)
                        editor.putString("fullname", fullname)

                        // Menyimpan data merchant
                        editor.putInt("merchant_id", merchantData.optInt("id"))
                        editor.putString("merchant_name", merchantData.optString("name"))
                        editor.putString("norekening", merchantData.optString("no")) // Menyimpan "no" sebagai "norekening"
                        editor.putString("merchant_code", merchantData.optString("code"))
                        editor.putString("merchant_address", merchantData.optString("address"))
                        editor.putString("merchant_phone", merchantData.optString("phone"))
                        editor.putString("merchant_email", merchantData.optString("email"))
                        editor.putString("merchant_balance", merchantData.optString("balance"))
                        editor.putString("merchant_avatar", merchantData.optString("avatar"))
                        editor.putInt("merchant_status", merchantData.optInt("status"))

                        editor.apply()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FormActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                            navigateToScreen()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FormActivity, "Token atau data merchant tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, "Username atau password salah", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception occurred while logging in", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FormActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

//    private fun navigateToCreate() {
//        startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//            putExtra(Constants.KEY_MENU_ID, "CCIF004")
//        })
//    }

    private fun navigateToScreen() {
        startActivity(Intent(this@FormActivity, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_MENU_ID, "MN00000")
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

    fun parseMutasi(mutasiText: String): List<Mutation> {
        val mutasiList = mutasiText.trim().split("\n").filter { it.isNotEmpty() }
        val transactions = mutableListOf<Mutation>()

        val regex = Regex("""(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}) (.+?) (Rp \d+.\d{2})""")
        for (mutasi in mutasiList) {
            val match = regex.find(mutasi)
            if (match != null) {
                val (date, time, description, amount) = match.destructured
                transactions.add(Mutation(date, time, description, amount))
            }
        }
        return transactions
    }
}