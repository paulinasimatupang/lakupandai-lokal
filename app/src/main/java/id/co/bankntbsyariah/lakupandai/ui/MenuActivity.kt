@file:Suppress("DEPRECATION")

package id.co.bankntbsyariah.lakupandai.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.common.MenuItem
import id.co.bankntbsyariah.lakupandai.common.Screen
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.iface.StorageImpl
import id.co.bankntbsyariah.lakupandai.ui.adapter.RecyclerViewMenuAdapter
import id.co.bankntbsyariah.lakupandai.utils.ScreenParser
import id.co.bankntbsyariah.lakupandai.utils.SpacingItemDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import android.widget.ImageButton

class MenuActivity : AppCompatActivity() {

    private var menuId:String = Constants.DEFAULT_ROOT_ID
    private val menuList = ArrayList<MenuItem>()
    private var backToExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                "HMB0000" -> R.layout.hamburger
                "MN00001" , "MN00002" -> R.layout.activity_menu_lainnya
                Constants.DEFAULT_ROOT_ID, "MN00000" -> R.layout.dashboard_layout
                else -> R.layout.activity_menu
            }
        )

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
        findViewById<ImageButton>(R.id.check_saldo_button)?.setOnClickListener {
            checkSaldo()
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
            Constants.SCREEN_TYPE_POPUP_GAGAL,
            Constants.SCREEN_TYPE_POPUP_SUKSES,
            Constants.SCREEN_TYPE_POPUP_LOGOUT -> {
                // Handle popups or alerts
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

    private fun navigateToFormActivity() {
        startActivity(Intent(this@MenuActivity, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, menuId)
        })
        finish() // Close MenuActivity
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupMenuRecyclerView(screen: Screen) {
        val menuContainer = findViewById<RecyclerView>(R.id.menu_container)
        val spacing = (resources.displayMetrics.density * 8).toInt()
        menuContainer?.addItemDecoration(SpacingItemDecorator(spacing))
        menuList.clear()
        val menuAdapter = RecyclerViewMenuAdapter(menuList, this@MenuActivity, menuId == "HMB0000")
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
        val targetScreenId = menuList[position].value
        if (targetScreenId.isNullOrEmpty()) {
            return
        }
        finish()
        navigateToScreen(targetScreenId)
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
            val savedUsername = sharedPreferences.getString("username", "") ?: ""
            val norekening = sharedPreferences.getString("norekening", "") ?: "" // Mengambil norekening dari SharedPreferences
            val msg = JSONObject()
            val msgId = "353471045058692200995" //stan + timestamp
            val msgUi = "353471045058692"
            val msgSi = "N00001"
            val username = savedUsername
            val accountNumber = norekening // Menggunakan norekening sebagai accountNumber
            val msgDt = "$username|$accountNumber"

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
                                    val value = comp.optJSONObject("comp_values")?.optJSONArray("comp_value")?.optJSONObject(0)?.optString("value")

                                    if (label == "No Rekening") {
                                        accountNumber = value
                                    } else if (label == "Saldo Akhir") {
                                        saldo = value
                                    }
                                }

                                runOnUiThread {
                                    findViewById<TextView>(R.id.account_number_text)?.text = "No Rekening: $accountNumber"
                                    findViewById<TextView>(R.id.saldo_text)?.text = "Saldo: $saldo"
                                    Log.d("MenuActivity", "Updated No Rekening text: $accountNumber")
                                    Log.d("MenuActivity", "Updated Saldo text: $saldo")
                                }
                            }
                        } else {
                            Log.e("MenuActivity", "screen object is null in JSON response")
                            runOnUiThread {
                                Toast.makeText(this, "Failed to fetch saldo", Toast.LENGTH_SHORT).show()
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
}
