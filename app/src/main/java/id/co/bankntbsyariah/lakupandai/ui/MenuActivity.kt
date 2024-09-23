@file:Suppress("DEPRECATION")

package id.co.bankntbsyariah.lakupandai.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.common.MenuItem
import id.co.bankntbsyariah.lakupandai.common.Screen
import id.co.bankntbsyariah.lakupandai.common.BannerItem
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.iface.StorageImpl
import id.co.bankntbsyariah.lakupandai.ui.adapter.RecyclerViewMenuAdapter
import id.co.bankntbsyariah.lakupandai.ui.adapter.ImageSliderAdapter
import id.co.bankntbsyariah.lakupandai.utils.ScreenParser
import id.co.bankntbsyariah.lakupandai.utils.SpacingItemDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import android.widget.ImageButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.NumberFormat
import java.util.Locale
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date

class MenuActivity : AppCompatActivity() {

    private var menuId:String = Constants.DEFAULT_ROOT_ID
    private val menuList = ArrayList<MenuItem>()
    private var backToExit = false
    private lateinit var imageSlider: ViewPager2
    private lateinit var sliderAdapter: ImageSliderAdapter
    private var isSaldoVisible = false

    private val NOTIFICATION_PERMISSION_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkNotificationPermission()

        // Get the menu ID from the Intent or use the default
        menuId = intent.getStringExtra(Constants.KEY_MENU_ID) ?: Constants.DEFAULT_ROOT_ID

        Log.d("MenuActivity", "Received menuId: $menuId")

        // Directly check if we need to navigate to form activity
        if (menuId == "FORM") {
            navigateToFormActivity()
            return
        }

        // Set the appropriate layout based on menuId
        setContentView(
            when (menuId) {
                "PP00001" -> R.layout.profile_layout
                "KOM0000" , "PKOM001"-> R.layout.komplain_menu
                "LOG0001","HMB0000" -> R.layout.hamburger
                "MN00001" , "MN00002" -> R.layout.activity_menu_lainnya
                Constants.DEFAULT_ROOT_ID, "MN00000" -> R.layout.dashboard_layout
                else -> R.layout.activity_menu
            }
        )

        val userGreetingTextView: TextView? = findViewById(R.id.user_greeting)
        if (userGreetingTextView != null) {
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val Userfullname = sharedPreferences.getString("fullname", "") ?: ""
            Log.d("MenuActivity", "Nama User : $Userfullname")
            userGreetingTextView.text = "HI, $Userfullname!"
        }

        // Mendapatkan referensi ke TextView dan ImageView
        val namaDepanTextView: TextView? = findViewById(R.id.namaDepanTextView)
        val userTextView: TextView? = findViewById(R.id.user)
        val usernameTextView: TextView? = findViewById(R.id.username)
        val emailTextView: TextView? = findViewById(R.id.email)
        val nomorTeleponTextView: TextView? = findViewById(R.id.nomor_telepon) // Referensi untuk nomor telepon
        val formText: TextView? = findViewById(R.id.text)
        val formImage: ImageView? = findViewById(R.id.image)
        val alamatTextView: TextView? = findViewById(R.id.alamat)

        if (userTextView != null && namaDepanTextView != null && usernameTextView != null && emailTextView != null && nomorTeleponTextView != null && alamatTextView != null) {

            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val userFullName = sharedPreferences.getString("fullname", "") ?: ""
            val username = sharedPreferences.getString("username", "-") ?: "-"
            val email = sharedPreferences.getString("email", "-") ?: "-"
            val nomorTelepon = sharedPreferences.getString("phone", "-") ?: "-"
            val alamat = sharedPreferences.getString("address", "-") ?: "-" // Default alamat


            val namaDepan = userFullName.split(" ").firstOrNull() ?: ""

            val namaDepanDuaHuruf = if (namaDepan.length >= 2) {
                namaDepan.substring(0, 2)
            } else {
                namaDepan
            }


            userTextView.text = userFullName

            namaDepanTextView.text = namaDepanDuaHuruf

            usernameTextView.text = username

            emailTextView.text = email

            nomorTeleponTextView.text = nomorTelepon

            alamatTextView.text = if (alamat.isEmpty() || alamat == "-") "-" else alamat

            // Log untuk debugging
            Log.d("MenuActivity", "Nama User: $userFullName")
            Log.d("MenuActivity", "Nama Depan Dua Huruf: $namaDepanDuaHuruf")
            Log.d("MenuActivity", "Username: $username")
            Log.d("MenuActivity", "Email: $email")
            Log.d("MenuActivity", "Nomor Telepon: $nomorTelepon")
            Log.d("MenuActivity", "Alamat: $alamat")
        }

        formText?.setOnClickListener {
            navigateToScreen("MN00000")
        }

        formImage?.setOnClickListener {
            navigateToScreen("MN00000")
        }


        // Initialize Image Slider only if the current layout contains the image slider
        val imageSliderView: View? = findViewById(R.id.imageSlider)
        if (imageSliderView != null) {
            imageSlider = imageSliderView as ViewPager2
            val imageUrlBase = "http://108.137.154.8:8081/ARRest/static"
            val imageList = listOf(
                BannerItem("banner1.png"),
                BannerItem("banner2.png"),
                BannerItem("banner3.png")
            )

            sliderAdapter = ImageSliderAdapter(imageList, this, imageUrlBase)
            imageSlider.adapter = sliderAdapter

            // Optional: Set up auto-slide
            val handler = Handler()
            val runnable = object : Runnable {
                var currentItem = 0

                override fun run() {
                    if (currentItem >= imageList.size) {
                        currentItem = 0
                    }
                    Log.d("MenuActivity", "Sliding to item: $currentItem")
                    imageSlider.setCurrentItem(currentItem++, true)
                    handler.postDelayed(this, 3000) // Auto-slide every 3 seconds
                }
            }
            handler.postDelayed(runnable, 3000)
        } else {
            Log.d("MenuActivity", "No image slider found")
        }

        val someTextView: TextView? = findViewById(R.id.title)
        if (someTextView != null) {
            // Do something with the view
        } else {
            Log.e("MenuActivity", "some_text_view is null")
        }

        // Set up edge-to-edge insets for the main view
        val mainView: View? = findViewById(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } ?: run {
            Log.e("MenuActivity", "Main view is null")
        }

        // Initialize the menu container and hide it initially
        val menuContainer = findViewById<RecyclerView>(R.id.menu_container)
        menuContainer?.visibility = View.GONE

        lifecycleScope.launch {
            try {
                var menuValue: String? = StorageImpl(applicationContext).fetchMenu(menuId)
                Log.d("MenuActivity", "Fetched menu value from storage: $menuValue")

                if (menuValue.isNullOrEmpty()) {
                    menuValue = withContext(Dispatchers.IO) {
                        ArrestCallerImpl(OkHttpClient()).fetchScreen(menuId)
                    }
                    Log.d("MenuActivity", "Fetched menu value from server: $menuValue")
                }

                if (menuValue.isNullOrEmpty()) {
                    showError("Menu value is null or empty for menuId: $menuId")
                } else {
                    val screenJson = JSONObject(menuValue)
                    val screen: Screen = ScreenParser.parseJSON(screenJson)
                    handleScreen(screen)
                }
            } catch (e: JSONException) {
                Log.e("MenuActivity", "Error parsing menu JSON: ${e.message}", e)
                showError("Error parsing menu data.")
            } catch (e: Exception) {
                Log.e("MenuActivity", "Error fetching menu: ${e.message}", e)
                showError("Error fetching menu.")
            } finally {
                findViewById<RecyclerView>(R.id.menu_container)?.visibility = View.VISIBLE
            }
        }

        // Handle the back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backToExit) {
                    finish()
                } else {
                    if (menuId == Constants.DEFAULT_ROOT_ID) {
                        backToExit = true
                        Handler(mainLooper).postDelayed({ backToExit = false }, 2000)
                        Toast.makeText(this@MenuActivity, getString(R.string.back_to_exit_info), Toast.LENGTH_SHORT).show()
                    } else {
                        restartMenuActivity()
                    }
                }
            }
        })

        // Set up the hamburger button click listener
        findViewById<View>(R.id.hamburger_button)?.setOnClickListener {
            showHamburgerMenu()
        }

        // Set up check saldo button click listener
//        findViewById<ImageButton>(R.id.check_saldo_button)?.setOnClickListener {
        checkSaldo()
//        }

        findViewById<ImageButton>(R.id.dashboard_nav)?.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.putExtra("formId", "MN00000")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.history_nav)?.setOnClickListener {
            val intent = Intent(this@MenuActivity, FormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_FORM_ID, "HY00001")
            }
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.history_bsa_nav)?.setOnClickListener {
            val intent = Intent(this@MenuActivity, FormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_FORM_ID, "HR00001")
            }
            startActivity(intent)
        }
    }

    private fun showError(message: String) {
        Log.e("MenuActivity", message)
        findViewById<TextView>(R.id.error_message)?.visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.menu_container)?.visibility = View.GONE
    }

    private fun handleScreen(screen: Screen) {
        when (screen.type) {
            Constants.SCREEN_TYPE_FORM -> navigateToFormActivity()
            Constants.SCREEN_TYPE_POPUP_GAGAL -> {
                when (screen.id) {
                    "000000F" -> {
                        // Handle failure case
                        val failureMessage = screen.comp.firstOrNull { it.id == "0000A" }
                            ?.compValues?.compValue?.firstOrNull()?.value ?: "Unknown error"
                        val intent = Intent(this@MenuActivity, PopupActivity::class.java).apply {
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
                        val intent = Intent(this@MenuActivity, PopupActivity::class.java).apply {
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
                showLogoutPopup()
            }
            Constants.SCREEN_TYPE_POPUP_OTP-> {
                navigateToFormActivity()
            }
            else -> {
                setupMenuRecyclerView(screen)
                val titleTextView: TextView? = findViewById(R.id.title)
                titleTextView?.let {
                    it.text = screen.title
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showLogoutPopup() {
        val dialogView = layoutInflater.inflate(R.layout.pop_up_logout, null)
        val logoutDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        logoutDialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.5f)  // Set dim amount to 0
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val buttonYa = dialogView.findViewById<Button>(R.id.popup_button_ya)
        val buttonTidak = dialogView.findViewById<Button>(R.id.popup_button_tidak)

        buttonYa.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            editor.clear()
            editor.apply()

            startActivity(Intent(this, FormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Constants.KEY_FORM_ID, "AU00001")
            })

            logoutDialog.dismiss()
        }

        buttonTidak.setOnClickListener {
            logoutDialog.dismiss()
        }

        logoutDialog.show()
    }


    private fun navigateToFormActivity() {
        startActivity(Intent(this@MenuActivity, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, menuId)
        })
        finish() // Close MenuActivity
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupMenuRecyclerView(screen: Screen) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("screen_awal", screen.id)
        editor.apply()

        val menuContainer = findViewById<RecyclerView>(R.id.menu_container)
        val spacing = (resources.displayMetrics.density * 8).toInt()
        menuContainer?.addItemDecoration(SpacingItemDecorator(spacing))
        menuList.clear()
        val menuAdapter = RecyclerViewMenuAdapter(menuList, this@MenuActivity, menuId == "HMB0000",menuId == "PP00001", menuId == "PR00000",  menuId== "KOM0000", menuId=="PKOM001")
        menuContainer?.adapter = menuAdapter

        screen.comp.forEach { comp ->
            if (!comp.visible) return@forEach
            menuList.add(MenuItem(comp.icon, comp.label, comp.label, comp.desc, comp.action))
        }
        Log.d("MenuActivity", "Menu item count: ${menuList.size}")
        menuAdapter.notifyDataSetChanged()

        Log.d("MenuActivity", "Loaded ${menuList.size} menu items into RecyclerView")
    }

    private fun showHamburgerMenu() {
        // Navigate to the hamburger layout
        startActivity(Intent(this, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_MENU_ID, "HMB0000")
        })
    }

    fun onMenuItemClick(position: Int) {
        if (position > menuList.size) {
            Log.e("MenuActivity", "Invalid menu item click. Position: $position, MenuList size: ${menuList.size}")
            return
        }

        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val screenAwal = sharedPreferences.getString("screen_awal", null)

        val targetScreenId = menuList[position].value
        Log.d("Menu", "Value = $targetScreenId")
        if (targetScreenId.isNullOrEmpty()) {
            return
        }
        if(targetScreenId == "LOG0001"){
            showLogoutPopup()
        }else if(targetScreenId == "MN00002") {
            showMenuInBottomSheet("MN00002")
        }else if(screenAwal == "PKOM001"){
            val editor = sharedPreferences.edit()
            editor.putString("kategori", menuList[position].title)
            editor.apply()
            Log.d("Menu", "Title Target = ${menuList[position].title}")
            finish()
            navigateToScreen(targetScreenId)
        }else{
            finish()
            navigateToScreen(targetScreenId)
        }
    }

    private fun showMenuInBottomSheet(menuId: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.activity_menu_lainnya, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val originalMenuList = ArrayList(menuList)

        menuList.clear()
        val menuContainer = bottomSheetView.findViewById<RecyclerView>(R.id.menu_container)
        menuContainer?.let {
            setupMenuRecyclerViewForBottomSheet(menuId, it)
        }

        bottomSheetDialog.setOnDismissListener {
            menuList.clear()
            menuList.addAll(originalMenuList)

            // navigateToScreen("MN00000")
        }

        bottomSheetDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupMenuRecyclerViewForBottomSheet(menuId: String, recyclerView: RecyclerView) {
        lifecycleScope.launch {
            try {
                var menuValue: String? = StorageImpl(applicationContext).fetchMenu(menuId)
                if (menuValue.isNullOrEmpty()) {
                    menuValue = withContext(Dispatchers.IO) {
                        ArrestCallerImpl(OkHttpClient()).fetchScreen(menuId)
                    }
                    Log.d("MenuActivity", "Fetched menu value from server: $menuValue")
                }

                val screenJson = JSONObject(menuValue)
                val screen: Screen = ScreenParser.parseJSON(screenJson)

                // Debug output for parsed data
                Log.d("BOTTOM1", "Parsed screen JSON: ${screenJson.toString()}")
                Log.d("BOTTOM1", "Parsed screen object: ${screen.toString()}")

                menuList.clear()
                screen.comp.forEach { comp ->
                    if (comp.visible) {
                        menuList.add(MenuItem(comp.icon, comp.label, comp.label, comp.desc, comp.action))
                    }
                }

                Log.d("BOTTOM1", "MENULIST BOTTOM: $menuList")

                val menuAdapter = RecyclerViewMenuAdapter(menuList, this@MenuActivity, false, isProfile = false, isList = false, isKomplain=false, isKomplain2 = false)
                recyclerView.adapter = menuAdapter

                menuAdapter.notifyDataSetChanged()
                Log.d("BOTTOM1", "Loaded ${menuList.size} menu items into BottomSheet RecyclerView")

            } catch (e: JSONException) {
                Log.e("BOTTOM1", "Error parsing menu JSON: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("BOTTOM1", "Error fetching menu: ${e.message}", e)
            }
        }
    }

    private fun navigateToScreen(screenId: String) {
        lifecycleScope.launch {
            val screenJson = withContext(Dispatchers.IO) {
                ArrestCallerImpl(OkHttpClient()).fetchScreen(screenId)
            }
            if (screenJson.isNullOrEmpty()) {
                Toast.makeText(this@MenuActivity, "Error loading screen", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val screen = ScreenParser.parseJSON(JSONObject(screenJson))
            when (screen.type) {
                Constants.SCREEN_TYPE_FORM -> {
                    startActivity(Intent(this@MenuActivity, FormActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra(Constants.KEY_FORM_ID, screenId)
                    })
                }
                else -> {
                    // Assume default navigation to MenuActivity for other types
                    startActivity(Intent(this@MenuActivity, MenuActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra(Constants.KEY_MENU_ID, screenId)
                    })
                }
            }
        }
    }


    fun onShowPopupotpClick(view: View) {
        val builder = AlertDialog.Builder(this)
        val popupView = LayoutInflater.from(this).inflate(R.layout.pop_up_otp, null)
        builder.setView(popupView)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun restartMenuActivity() {
        finish()
        startActivity(Intent(this@MenuActivity, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun createMessageBody(): JSONObject? {
        return try {
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val norekening = sharedPreferences.getString("norekening", "") ?: ""
            val merchant_name = sharedPreferences.getString("merchant_name", "") ?: ""
            val username = "lakupandai"
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

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    private fun checkSaldo() {
        val messageBody = createMessageBody()
        if (messageBody != null) {
            Log.d("MenuActivity", "Requesting saldo with message: $messageBody")

            ArrestCallerImpl(OkHttpClient()).requestPost(messageBody) { responseBody ->
                responseBody?.let {
                    Log.d("MenuActivity", "Received response: $it")
                    try {
                        val jsonResponse = JSONObject(it)
                        Log.d("MenuActivity", "Parsed JSON response: $jsonResponse")
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
                                        val saldoNumeric = saldo?.toDoubleOrNull() ?: 0.0
                                        saldo = formatRupiah(saldoNumeric)
                                    }
                                }

                                runOnUiThread {
                                    // Menampilkan nomor rekening secara langsung
                                    findViewById<TextView>(R.id.account_number_text)?.text =
                                        accountNumber ?: ""
                                    Log.d(
                                        "MenuActivity",
                                        "Updated No Rekening text: $accountNumber"
                                    )

                                    // Tambahkan click listener untuk tombol saldo
                                    val checkSaldoButton = findViewById<ImageButton>(R.id.check_saldo_button)
                                    val saldoTextView = findViewById<TextView>(R.id.saldo_text)

                                    checkSaldoButton?.setOnClickListener {
                                        isSaldoVisible = !isSaldoVisible

                                        // Tampilkan atau sembunyikan saldo
                                        saldoTextView?.text = if (isSaldoVisible) saldo ?: "" else "XXXXXXXXX"

                                        // Ganti ikon berdasarkan status saldo
                                        checkSaldoButton.setImageResource(
                                            if (isSaldoVisible) R.drawable.eye_open else R.drawable.eye_closed
                                        )

                                        Log.d(
                                            "MenuActivity",
                                            "Updated Saldo text: ${if (isSaldoVisible) saldo else "XXXXXXXXX"}, isSaldoVisible: $isSaldoVisible"
                                        )
                                    }
                                }
                            }
                        } else {
                            Log.e("MenuActivity", "screen object is null in JSON response")
                            runOnUiThread {
                                Toast.makeText(this, "Failed to fetch saldo", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MenuActivity", "Failed to parse response", e)
                        runOnUiThread {
                            Toast.makeText(this, "Failed to fetch saldo", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Log.e("MenuActivity", "Failed to fetch saldo, responseBody is null")
                    runOnUiThread {
                        Toast.makeText(this, "Failed to fetch saldo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.e("MenuActivity", "Failed to create message body, request not sent")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Jika izin belum diberikan, minta izin
                Log.d("MenuActivity", "Izin notifikasi belum diberikan. Meminta izin...")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                Log.d("MenuActivity", "Izin notifikasi sudah diberikan.")
            }
        } else {
            // Tidak memerlukan izin untuk versi Android di bawah TIRAMISU
            Log.d("MenuActivity", "Tidak memerlukan izin untuk Android versi di bawah 13.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MenuActivity", "Izin notifikasi telah diberikan.")
                Toast.makeText(this, "Izin notifikasi diberikan", Toast.LENGTH_SHORT).show()

                // Panggil fungsi untuk mengirim notifikasi jika diperlukan
                sendNotification("Notifikasi Diberikan", "Izin telah diberikan, dan ini notifikasi contoh.")
            } else {
                Log.e("MenuActivity", "Izin notifikasi ditolak.")
                Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun sendNotification(title: String, message: String) {
        val intent = Intent(this, MenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "notification_channel")
            .setSmallIcon(R.mipmap.logo_aja_ntbs)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(message)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notification_channel",
                "id.co.bankntbsyariah.lakupandai",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
