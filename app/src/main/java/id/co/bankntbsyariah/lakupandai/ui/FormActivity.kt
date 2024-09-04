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
import android.view.ViewGroup
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import okhttp3.internal.format
import android.Manifest
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.InputType
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import id.co.bankntbsyariah.lakupandai.iface.WebCallerImpl
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import  id.co.bankntbsyariah.lakupandai.utils.createTextView
import android.widget.EditText
import org.json.JSONArray
import java.text.ParseException
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.style.StyleSpan


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
    private var photoType: String? = null
    private var currentImageView: ImageView? = null
    private var otpTimer: CountDownTimer? = null
    private var lastMessageBody: JSONObject? = null
    private var otpAttempts = mutableListOf<Long>()
    private val otpCooldownTime = 3 * 60 * 1000L // 30 minutes in milliseconds
    private var otpScreen: Screen? = null

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

                startOtpTimer()
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
        if (screenTitle.contains("Berhasil", ignoreCase = true)) {
            val formattedTitle = screenTitle.replace("Berhasil", "").trim()
            Log.d("FormActivity", "Formatted title: $formattedTitle")

            val cardTitleTextView = findViewById<TextView>(R.id.card_title)
            if (cardTitleTextView != null) {
                cardTitleTextView.text = formattedTitle
                Log.d("FormActivity", "card_title successfully updated: ${cardTitleTextView.text}")
            } else {
                Log.e("FormActivity", "Error: TextView with ID card_title not found.")
            }
        }
    }

    private fun getTransactionTitle(formId: String): String {
        val titleView = findViewById<TextView>(R.id.titleTransaction)
        val title = when (formId) {
            "CCIF001" -> {
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
                        component.id == "TRF26" -> {
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

            if (component.id == "TRF27" || component.id == "TFR24" || (component.id == "AG001" && screen.title.contains("Form")) ||
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

                        // Create and configure the main layout
                        val layout = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(createTextView(context, component.label, 15f, Typeface.BOLD, R.color.black, 16.dpToPx(), 8.dpToPx()))
                            addView(createTextView(context, "Loading...", 18f, Typeface.NORMAL, null, 16.dpToPx(), 10.dpToPx()))
                        }

                        // Add the layout to the container
                        container.addView(layout)

                        // Perform data fetching asynchronously
                        lifecycleScope.launch {
                            val fetchedValue = withContext(Dispatchers.IO) {
                                val branchid = getKodeCabangFromPreferences()
                                val token = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE).getString("token", "") ?: ""
                                branchid?.let { WebCallerImpl().fetchNasabahList(it, token) }?.string()
                            }

                            if (fetchedValue.isNullOrEmpty()) {
                                Log.e("FormActivity", "Failed to fetch nasabah list")
                                return@launch
                            }

                            val jsonResponse = JSONObject(fetchedValue)
                            val dataArray = jsonResponse.getJSONArray("data")

                            // Convert JSONArray to List<JSONObject>
                            val dataList = List(dataArray.length()) { i -> dataArray.getJSONObject(i) }

                            // Clear existing views in the layout
                            layout.removeAllViews()

                            // Group data by date
                            val groupedData = dataList.groupBy {
                                val requestTime = it.getString("request_time")
                                requestTime.split(" ").getOrNull(0) ?: "Unknown Date"
                            }

                            // Add grouped data to the layout
                            groupedData.forEach { (date, nasabahList) ->
                                layout.addView(createTextView(context, date, 20f, Typeface.BOLD, R.color.gray, 16.dpToPx(), 8.dpToPx()))

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
                    }

                    else if (component.id == "HY001") {
                        val context = this@FormActivity
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
                        }.also { layout ->
                            lifecycleScope.launch {
                                val webCaller = WebCallerImpl()
                                val fetchedValue = withContext(Dispatchers.IO) {
                                    val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                                    val token = sharedPreferences.getString("token", "") ?: ""
                                    val terminalId = sharedPreferences.getString("tid", "") ?: ""
                                    val response = webCaller.fetchHistory(terminalId, token)
                                    response?.string()
                                }

                                if (!fetchedValue.isNullOrEmpty()) {
                                    try {
                                        // Debug log for the raw response
                                        Log.d("FormActivity", "Fetched JSON: $fetchedValue")

                                        val jsonResponse = JSONObject(fetchedValue)
                                        val dataArray = jsonResponse.optJSONArray("data") ?: JSONArray()

                                        // Clear existing views before adding new data
                                        layout.removeAllViews()

                                        for (i in 0 until dataArray.length()) {
                                            val item = dataArray.getJSONObject(i)

                                            val replyTime = item.optString("reply_time", "")
                                            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                            val targetFormat = SimpleDateFormat("dd MM yyyy HH:mm", Locale.getDefault())
                                            val formattedReplyTime: String = try {
                                                val date = originalFormat.parse(replyTime)
                                                targetFormat.format(date)
                                            } catch (e: ParseException) {
                                                Log.e("FormActivity", "Error parsing date: ${e.message}")
                                                replyTime
                                            }

                                            val status = item.optString("status", "")
                                            val requestMessage = item.getString("request_message")

                                            val no_rek = item.getString("no_rek")
                                            val nama_rek = item.getString("nama_rek")
                                            val nominal = item.getString("nominal")

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

                                            val gridLayout = GridLayout(context).apply {
                                                layoutParams = LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                                )
                                                rowCount = 2
                                                columnCount = 2
                                                // Set margins and padding if needed
                                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())

//                                                setOnClickListener {
//                                                    lifecycleScope.launch {
//                                                        val detailResponse = withContext(Dispatchers.IO) {
//                                                            val preferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
//                                                            val token = preferences.getString("token", "") ?: ""
//                                                            val terminalId = preferences.getString("tid", "") ?: ""
//                                                            val messageId = item.optString("message_id", "")
//
//                                                            try {
//                                                                val response = webCaller.fetchHistoryDetail(terminalId, messageId, token)
//                                                                response?.string()
//                                                            } catch (e: Exception) {
//                                                                Log.e("FormActivity", "Error fetching history detail: ${e.message}", e)
//                                                                null
//                                                            }
//                                                        }
//
//                                                        if (!detailResponse.isNullOrEmpty()) {
//                                                            try {
//                                                                val jsonResponse = JSONObject(detailResponse)
//                                                                val dataString = jsonResponse.optString("data", "{}")
//                                                                val dataJson = JSONObject(dataString)
//
//                                                                // Extract `screen` object from `dataJson`
//                                                                val screenJson = dataJson.optJSONObject("screen")
//
//                                                                // Convert `screenJson` to `Screen` object
//                                                                val screen = screenJson?.let { json ->
//                                                                    Screen(
//                                                                        type = json.optInt("type"), // Adjust this to match the type in JSON
//                                                                        title = json.optString("title"),
//                                                                        id = json.optString("id"),
//                                                                        ver = json.optString("ver"),
//                                                                        comp = parseComponents(json.optJSONObject("comps")?.optJSONArray("comp") ?: JSONArray()),
//                                                                        actionUrl = json.optString("action_url")
//                                                                    )
//                                                                }

                                                                // Setup the form with the parsed `Screen` data
//                                                                if (screen != null) {
//                                                                    setupForm(screen)
//                                                                } else {
//                                                                    withContext(Dispatchers.Main) {
//                                                                        Toast.makeText(this@FormActivity, "Invalid screen data", Toast.LENGTH_SHORT).show()
//                                                                    }
//                                                                }
//                                                            } catch (e: JSONException) {
//                                                                Log.e("FormActivity", "Detail JSON parsing error: ${e.message}", e)
//                                                                withContext(Dispatchers.Main) {
//                                                                    Toast.makeText(this@FormActivity, "Error parsing detail JSON response", Toast.LENGTH_SHORT).show()
//                                                                }
//                                                            }
//                                                        } else {
//                                                            withContext(Dispatchers.Main) {
//                                                                Toast.makeText(this@FormActivity, "Failed to fetch detail", Toast.LENGTH_SHORT).show()
//                                                            }
//                                                        }
//                                                    }
//
//                                                }
                                            }

                                            gridLayout.addView(TextView(context).apply {
                                                text = actionText
                                                textSize = 16f
                                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                                                layoutParams = GridLayout.LayoutParams().apply {
                                                    columnSpec = GridLayout.spec(0) // Column 1
                                                    rowSpec = GridLayout.spec(0)    // Row 1
                                                    setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                                                }
                                            })

                                            gridLayout.addView(TextView(context).apply {
                                                text = formatTrans
                                                textSize = 12f
                                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                                                layoutParams = GridLayout.LayoutParams().apply {
                                                    columnSpec = GridLayout.spec(0) // Column 1
                                                    rowSpec = GridLayout.spec(1)    // Row 2
                                                    setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                                                }
                                            })

                                            gridLayout.addView(TextView(context).apply {
                                                text = statusTrans
                                                setTextColor(statusColor)
                                                textSize = 16f
                                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                                                layoutParams = GridLayout.LayoutParams().apply {
                                                    columnSpec = GridLayout.spec(1) // Column 2
                                                    rowSpec = GridLayout.spec(0)    // Row 1
                                                    setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                                                }
                                            })

                                            gridLayout.addView(TextView(context).apply {
                                                text = formattedReplyTime
                                                textSize = 12f
                                                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 16.dpToPx())
                                                layoutParams = GridLayout.LayoutParams().apply {
                                                    columnSpec = GridLayout.spec(1) // Column 2
                                                    rowSpec = GridLayout.spec(1)    // Row 2
                                                    setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                                                }
                                            })
                                            layout.addView(gridLayout)
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
                                        setPadding(0.dpToPx(), 3.dpToPx(), 16.dpToPx(), 2.dpToPx())
                                    } else {
                                        setPadding(0.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                                    }
                                    setTextColor(
                                        ContextCompat.getColor(
                                            this@FormActivity,
                                            R.color.black
                                        )
                                    )
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

                        var compOption = component.compValues?.compValue

                        lifecycleScope.launch {
                            val options: List<Pair<String?, String?>> = if (compOption.isNullOrEmpty() ||
                                compOption.all { it.value == "" }) {

                                var formValue = StorageImpl(applicationContext).fetchForm(screen.id)

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
                                    mutableListOf(Pair("Pilih ${component.label}", "")) // Return a list with default placeholder
                                } else {
                                    try {
                                        val screenJson = JSONObject(formValue)
                                        val screen: Screen = ScreenParser.parseJSON(screenJson)
                                        val selectedOptions = screen.comp.firstOrNull { it.id == component.id }
                                            ?.compValues?.compValue?.mapNotNull { Pair(it.print, it.value) }
                                            ?: emptyList()

                                        Log.d("FormActivity", "SELECTED OPTIONS : $selectedOptions")
                                        mutableListOf(Pair("Pilih ${component.label}", "")) + selectedOptions
                                    } catch (e: JSONException) {
                                        Log.e("FormActivity", "JSON Parsing error: ${e.message}")
                                        mutableListOf(Pair("Pilih ${component.label}", "")) // Return a list with default placeholder
                                    }
                                }
                            } else {
                                mutableListOf(Pair("Pilih ${component.label}", "")) + compOption.map { Pair(it.print, it.value) }
                            }

                            val spinner = Spinner(this@FormActivity).apply {
                                background = getDrawable(R.drawable.combo_box)
                                val adapter = ArrayAdapter(
                                    this@FormActivity,
                                    android.R.layout.simple_spinner_item,
                                    options.map { it.first } // Display only the 'print' value
                                )
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

                                        val selectedPair = options[position]
                                        val selectedValue = selectedPair.first
                                        val selectedCompValue = selectedPair.second

                                        if (screen.id == "CCIF001") {
                                            inputRekening[component.id] = selectedValue ?: "" // Now storing selectedValue
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
                                            "PIL03"-> {
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

                                                    Toast.makeText(this@FormActivity, "Validasi gagal: Pilih WA atau SMS", Toast.LENGTH_SHORT).show()
                                                }                                            }
                                            "CB001" -> {
                                                if (selectedCompValue != null) {
                                                    inputValues[component.id] = selectedCompValue.replace("[OI]", "")
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
                                                    inputValues[component.id] = selectedCompValue.replace("[OI]CK", "")
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
                                                    inputValues[component.id] = selectedCompValue.replace("[OI]CIFP", "")
                                                }
                                                Log.d(
                                                    "FormActivity",
                                                    "Provinsi set to: ${inputValues[component.id]}"
                                                )
                                            }
                                            else -> inputValues[component.id] = (position ?: selectedValue).toString()
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
                }

                5 -> {
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@FormActivity).apply {
                            text = component.label
                            textSize = 16f
                            setTypeface(null, Typeface.BOLD)
                        })

                        component.values.forEachIndexed { index, value ->
                            val checkBox = CheckBox(this@FormActivity).apply {
                                text = value.first
                            }
                            checkBox.setOnCheckedChangeListener { _, isChecked ->
                                if (screen.id == "CCIF001") {
                                    inputRekening[component.id] = inputValues[component.id] ?: ""
                                }
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
                            textSize = 16f
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
                                if (isChecked) {
                                    val selectedValue = value.first

                                    if (screen.id == "CCIF001") {
                                        inputRekening[component.id] = selectedValue
                                    }

                                    val valueToSave = if (component.id in listOf("CIF05", "CIF17", "CIF07", "CIF21")) {
                                        (index + 1).toString()
                                    } else {
                                        index.toString()
                                    }
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
                            "OTP10" -> getDrawable(R.drawable.button_green)
                            "MSG10" -> getDrawable(R.drawable.button_green)
                            else -> getDrawable(R.drawable.button_yellow)
                        }
                        setBackground(background)
                        setOnClickListener {
                            Log.d("FormActivity", "Screen Type: ${screen.type}")
                            val sharedPreferences =
                                getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
                            }else if (formId == "AU00001") {
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
                            }else {
                                handleButtonClick(component, screen)
                            }
                        }
                    }.let { view ->
                        // Ensure view is added with proper layout parameters
                        container.addView(view, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(20, 20, 20, 20)
                        })
                    }
                }
                15 -> {
                    val inflater = layoutInflater
                    val otpView = inflater.inflate(R.layout.pop_up_otp, container, false)
                    val timerTextView = otpView.findViewById<TextView>(R.id.timerTextView)

                    val resendOtpTextView = otpView.findViewById<TextView>(R.id.tv_resend_otp)

                    if (resendOtpTextView != null) {
                        Log.e("FormActivity", "RESEND OTP NOT null.")
                        resendOtpTextView.setOnClickListener {
                            resendOtp()
                        }

                        val text = "Resend OTP"
                        val spannable = SpannableString(text)
                        spannable.setSpan(UnderlineSpan(), 0, text.length, 0)
                        resendOtpTextView.text = spannable
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

                    val otpDigit1 = otpView.findViewById<EditText>(R.id.otpDigit1)
                    val otpDigit2 = otpView.findViewById<EditText>(R.id.otpDigit2)
                    val otpDigit3 = otpView.findViewById<EditText>(R.id.otpDigit3)
                    val otpDigit4 = otpView.findViewById<EditText>(R.id.otpDigit4)
                    val otpDigit5 = otpView.findViewById<EditText>(R.id.otpDigit5)
                    val otpDigit6 = otpView.findViewById<EditText>(R.id.otpDigit6)


                    val otpDigits = listOf(otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6)

                    otpDigits.forEachIndexed { index, digit ->
                        digit.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                if (s != null) {
                                    when {
                                        count > 0 -> {
                                            val nextIndex = index + 1
                                            if (nextIndex < otpDigits.size) {
                                                otpDigits[nextIndex].requestFocus()
                                            }
                                        }

                                        before > 0 -> {
                                            val prevIndex = index - 1
                                            if (prevIndex >= 0) {
                                                otpDigits[prevIndex].requestFocus()
                                            }
                                        }
                                    }
                                    val otpValue = otpDigits.joinToString(separator = "") { it.text.toString() }
                                    inputValues["OTP"] = otpValue
                                }
                            }
                            override fun afterTextChanged(s: Editable?) {}
                        })
                    }
                    container.addView(otpView)
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
                            background = getDrawable(R.drawable.date_input)
                            id = View.generateViewId()
                            tag = component.id
                            inputType = android.text.InputType.TYPE_NULL
                            setOnClickListener {
                                Log.d("FormActivity", "EditText clicked: ${component.id}")
                                showDatePickerDialog(this) { selectedDate ->
                                    inputValues[component.id as String] = selectedDate
                                    if (screen.id == "CCIF001") {
                                        inputRekening[component.id] =
                                            inputValues[component.id] ?: ""
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
                            setMargins(20, 20, 20, 20)
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

                        addView(signaturePadView)

                        clearButton.setOnClickListener {
                            signaturePad.clear()
                            Log.d("FormActivity", "SignaturePad cleared")
                        }

                        saveButton.setOnClickListener {
                            if (!signaturePad.isEmpty) {
                                val signatureBitmap = signaturePad.signatureBitmap
                                Log.d("FormActivity", "Signature bitmap width: ${signatureBitmap.width}, height: ${signatureBitmap.height}")

                                val file = createFileFromBitmap(signatureBitmap, "coba1.png")
//                                saveSignatureToServer(file)
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

                    buttonCapture.setOnClickListener {
                        currentImageView = imageViewPreview
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

                    pinDigits.forEachIndexed { index, digit ->
                        digit.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                if (s != null) {
                                    when {
                                        count > 0 -> {
                                            val nextIndex = index + 1
                                            if (nextIndex < pinDigits.size) {
                                                pinDigits[nextIndex].requestFocus()
                                            }
                                        }
                                        before > 0 -> {
                                            val prevIndex = index - 1
                                            if (prevIndex >= 0) {
                                                pinDigits[prevIndex].requestFocus()
                                            }
                                        }
                                    }
                                    val pinValue = pinDigits.joinToString(separator = "") { it.text.toString() }
                                    inputValues["PIN"] = pinValue
                                }
                            }

                            override fun afterTextChanged(s: Editable?) {}
                        })
                    }

                    container.addView(pinView)
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


//    private fun saveSignatureToServer(file: File) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val client = OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
//                .writeTimeout(30, TimeUnit.SECONDS) // Increase write timeout
//                .readTimeout(30, TimeUnit.SECONDS) // Increase read timeout
//                .build()
//
//            val mediaType = "image/png".toMediaTypeOrNull()
//            val requestBody = MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("file", file.name,
//                    RequestBody.create(mediaType, file))
//                .build()
//
//            val request = Request.Builder()
//                .url("http://108.137.154.8:8081/ARRest/images")
//                .post(requestBody)
//                .build()
//
//            try {
//                val response = client.newCall(request).execute()
//                Log.d("FormActivity", "Response code: ${response.code}")
//                Log.d("FormActivity", "Response body: ${response.body?.string()}")
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Log.d("FormActivity", "Signature uploaded successfully")
//                        Toast.makeText(this@FormActivity, "Signature uploaded successfully", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Log.e("FormActivity", "Failed to upload signature: ${response.message}")
//                        Toast.makeText(this@FormActivity, "Failed to upload signature", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("FormActivity", "Error uploading signature: ${e.message}")
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@FormActivity, "Error uploading signature", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    private fun getStatusColor(context: Context, status: String): Int {
        return when (status) {
            "0", "1"-> ContextCompat.getColor(context, R.color.blue)
            "2" -> ContextCompat.getColor(context, R.color.green)
            "3" -> ContextCompat.getColor(context, R.color.red)
            else -> ContextCompat.getColor(context, R.color.black)
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
                if (msg03Value != null && msg03Value == otpValue) {
                    otpAttempts.clear()
                    isOtpValidated = true
                    otpDialog?.dismiss()
                    cancelOtpTimer()
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
                    findViewById<EditText>(R.id.otpDigit1)?.error = "OTP salah"
                }
            } else {
                val messageBody = screen?.let { createMessageBody(it) }
                val sendOTPComponent = screen?.comp?.find { it.id == "OTP09" }
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
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("KODE_CABANG", null)
    }

    private fun saveKodeCabangToPreferences(kodeCabang: String) {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("KODE_CABANG", kodeCabang)
        editor.apply() // atau editor.commit()
    }



    private fun createMessageBody(screen: Screen): JSONObject? {
        return try {
            val msg = JSONObject()
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val savedNorekening = sharedPreferences.getString("norekening", "") ?: ""
            val savedNamaAgen = sharedPreferences.getString("fullname", "") ?: ""
            val savedKodeAgen = sharedPreferences.getString("merchant_id", "")?: ""
            val username = "lakupandai"
            Log.e("FormActivity", "Saved Username: $username")
            Log.e("FormActivity", "Saved Norekening: $savedNorekening")
            Log.e("FormActivity", "Saved Agen: $savedKodeAgen")
            Log.e("FormActivity", "Saved Nama Agen: $savedNamaAgen")
            val branchid = getKodeCabangFromPreferences()
            Log.e("FormActivity", "Saved Kode Cabang: $branchid")

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
            var msgSi = screen.actionUrl

            Log.d("FormActivity", "msgSi: $msgSi")

            Log.d("FormActivity", "PICK OTP: $pickOTP")
            if (screen.comp.any { it.id == "PIL03" } && pickOTP == "SMS") {
                when(screen.id){
                    // cek saldo
                    "CS00004"->{
                        msgSi = "N00002"
                    }
                    // cek mutasi
                    "MB81124"->{
                        msgSi = "E81122"
                    }
                    "TF00002"->{
                        msgSi = "T00003"
                    }
                    "CCIF001"->{
                        msgSi = "CC0002"
                    }
                    "RT001"->{
                        msgSi = "RTT002"
                    }
                    "RS001"->{
                        msgSi = "OTN002"
                    }
                    else->{
                        msgSi = screen.actionUrl
                    }
                }
            }

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
                    component.type == 1 && component.label == "Kode Cabang" -> {
                        val branchid = getKodeCabangFromPreferences()
                        componentValues[component.id] = branchid ?: ""
                        Log.d(
                            "FormActivity",
                            "Updated componentValues with branchid : ${branchid}"
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
            val unf03Value = componentValues["UNF03"] ?: ""
            val unf01Value = componentValues["AG009"] ?: ""
            val unf04Value = componentValues["SET10"] ?: ""
            val rnr02Value = componentValues["RNR02"] ?: ""
            when (screen.id) {
                "RCS0001" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$unf01Value, dengan nomor rekening: $unf03Value. Sisa saldo anda adalah $rnr02Value."
                }
                "TF00003", "BR001", "BS001" -> {
                    componentValues["MSG05"] = "Nasabah Yth.$unf01Value, dengan nomor rekening: $unf04Value.Transaksi berhasil dilakukan."
                }
                else -> {
                    componentValues["MSG05"] = "Pesan tidak diketahui."
                }
            }
            var msgDt = ""
            Log.d("Screen", "SCREEN CREATE MESSAGE : ${screen.id}")
            if(screen.id == "AU00001"){
                msgDt = "$username|$savedNorekening|$savedNamaAgen|null"
            }else{
                msgDt = screen.comp.filter { it.type != 7 && it.type != 15 && it.id != "MSG03" && it.id != "PIL03"}
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

    private fun loginUser() {
        lifecycleScope.launch(Dispatchers.IO) {
            val formBodyBuilder = FormBody.Builder()
            var username: String? = null
            var password: String? = null

            // Populate form data
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


//                    val msg_ui = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
//                    Log.d("FormActivity", "msg_ui: $msg_ui")
//                    val imeiFromTerminalData = terminalData?.optString("imei")
//
//                    if (imeiFromTerminalData == msg_ui) {
//                        Log.d("FormActivity", "IMEI sesuai dengan msg_ui.")
//                    } else {
//                        Log.d("FormActivity", "IMEI tidak sesuai dengan msg_ui.")
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@FormActivity, "Perangkat yang digunakan tidak sesuai dengan yang didaftarkan", Toast.LENGTH_SHORT).show()
//                        }
//                        return@launch
//                    }

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
                            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()

                            editor.putString("username", username)
                            editor.putString("token", token)
                            editor.putString("fullname", fullname)
                            editor.putString("id", id)
                            val terminalData = merchantData?.optJSONObject("terminal")
                            if (terminalData != null) {
                                editor.putString("tid", terminalData.optString("tid"))
                            }

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

                            if (terminalData != null) {
                                editor.putString("tid", terminalData.optString("tid"))
                            }

                            editor.putInt("login_attempts", 0)
                            editor.apply()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@FormActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                                navigateToScreen()
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
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
                    val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
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
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
//            // Dapatkan foto yang diambil
//            photo = data?.extras?.get("data") as? Bitmap
//
//            // Tampilkan foto yang diambil di ImageView yang sesuai
//            when (photoType) {
//                "KTP" -> imageViewKTP.setImageBitmap(photo)
//                "Orang" -> imageViewOrang.setImageBitmap(photo)
//            }
//        }
//    }

    private fun openCameraIntent() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            currentImageView?.setImageBitmap(photo)  // Update the correct ImageView
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
}