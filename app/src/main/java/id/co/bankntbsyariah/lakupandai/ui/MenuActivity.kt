package id.co.bankntbsyariah.lakupandai.ui

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
import org.json.JSONObject

class MenuActivity : AppCompatActivity() {

    private var menuId = Constants.DEFAULT_ROOT_ID
    private val menuList = ArrayList<MenuItem>()
    private var backToExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent.extras?.getString(Constants.KEY_MENU_ID)?.let {
            menuId = it
        } ?: run {
            menuId = Constants.DEFAULT_ROOT_ID
        }

        if (menuId == "HMB0000") {
            setContentView(R.layout.hamburger)
        } else if (menuId == Constants.DEFAULT_ROOT_ID) {
            setContentView(R.layout.dashboard_layout)
        } else {
            setContentView(R.layout.activity_menu)
        }

        val mainView: View? = findViewById(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } ?: Log.e("MenuActivity", "Main view is null")

        lifecycleScope.launch {
            var menuValue = StorageImpl(applicationContext).fetchMenu(menuId)
            if (menuValue.isNullOrEmpty()) {
                withContext(Dispatchers.IO) {
                    menuValue = ArrestCallerImpl(OkHttpClient()).fetchScreen(menuId)
                }
            }

            if (menuValue.isNullOrEmpty()) {
                findViewById<TextView>(R.id.error_message).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.menu_container).visibility = View.GONE
            } else {
                val screenJson = JSONObject(menuValue)
                val screen: Screen = ScreenParser.parseJSON(screenJson)
                val sType = screen.type

                when (sType) {
                    Constants.SCREEN_TYPE_FORM -> {
                        finish()
                        startActivity(Intent(this@MenuActivity, FormActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra(Constants.KEY_FORM_ID, menuId)
                        })
                    }
                    Constants.SCREEN_TYPE_POPUP_GAGAL,
                    Constants.SCREEN_TYPE_POPUP_SUKSES,
                    Constants.SCREEN_TYPE_POPUP_LOGOUT -> {
                    }
                    else -> {
                    }
                }

                val menuContainer = findViewById<RecyclerView>(R.id.menu_container)
                val spacing = (resources.displayMetrics.density * 8).toInt()
                menuContainer.addItemDecoration(SpacingItemDecorator(spacing))
                menuList.clear()
                val menuAdapter = RecyclerViewMenuAdapter(menuList, this@MenuActivity, menuId == "HMB0000")
                menuContainer.adapter = menuAdapter

                screen.comp.forEach { comp ->
                    menuList.add(MenuItem(comp.icon, comp.label, comp.label, comp.desc, comp.action))
                }
                menuAdapter.notifyDataSetChanged()
            }
        }

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
                        finish()
                        startActivity(Intent(this@MenuActivity, MenuActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                    }
                }
            }
        })

        findViewById<View>(R.id.hamburger_button)?.setOnClickListener {
            showHamburgerMenu()
        }
    }

    private fun showHamburgerMenu() {
        startActivity(Intent(this, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_MENU_ID, "HMB0000")
        })
    }

    fun onMenuItemClick(position: Int) {
        finish()
        startActivity(Intent(this, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_MENU_ID, menuList[position].value)
        })
    }

    fun onShowPopupotpClick(view: View) {
        val builder = AlertDialog.Builder(this)
        val popupView = LayoutInflater.from(this).inflate(R.layout.pop_up_otp, null)
        builder.setView(popupView)
        val alertDialog = builder.create()
        popupView.findViewById<Button>(R.id.verifyButton).setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
}

