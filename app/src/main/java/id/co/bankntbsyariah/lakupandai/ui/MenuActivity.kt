package id.co.bankntbsyariah.lakupandai.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
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
import androidx.appcompat.app.AlertDialog
import android.widget.Button
import android.view.LayoutInflater


class MenuActivity : AppCompatActivity() {

    private var menuId = Constants.DEFAULT_ROOT_ID
    private val menuList = ArrayList<MenuItem>()
    private var backToExit = false

    private var formId = Constants.DEFAULT_ROOT_ID
    private var isForm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ambil menuId dari Intent sebelum menentukan layout
        intent.extras?.getString(Constants.KEY_MENU_ID)?.let {
            menuId = it
        } ?: run {
            menuId = Constants.DEFAULT_ROOT_ID
        }

        // Tentukan layout berdasarkan menuId
        if (menuId == Constants.DEFAULT_ROOT_ID) {
            setContentView(R.layout.dashboard_layout)
        } else {
            setContentView(R.layout.activity_menu)
        }

        val mainView: View? = findViewById(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } else {
            Log.e("MenuActivity", "Main view is null")

        if (formId.equals("HMB0000")) {
            setContentView(R.layout.hamburger)
        } else {
            setContentView(R.layout.activity_menu)
        }

        val mainView: View? = findViewById(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } else {
            Log.e("MenuActivity", "Main view is null")
        }

        intent.extras?.getString(Constants.KEY_MENU_ID).let {
            menuId = it ?: Constants.DEFAULT_ROOT_ID
        }

        lifecycleScope.launch {
            var menuValue = StorageImpl(applicationContext).fetchMenu(menuId)
            if (menuValue == null || menuValue == "") {
                withContext(Dispatchers.IO) {
                    val mv = ArrestCallerImpl(OkHttpClient()).fetchScreen(menuId)
                    menuValue = mv
                }
            }

            if (menuValue == null || menuValue == "") {
                findViewById<TextView>(R.id.error_message).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.menu_container).visibility = View.GONE
            } else {
                val screenJson = JSONObject(menuValue)
                val screen: Screen = ScreenParser.parseJSON(screenJson)
                val sType = screen.type

                when (sType) {
                    Constants.SCREEN_TYPE_FORM -> {
                        finish()
                        startActivity(
                            Intent(this@MenuActivity, FormActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .putExtra(Constants.KEY_FORM_ID, menuId)
                        )
                    }
                    Constants.SCREEN_TYPE_POPUP_GAGAL,
                    Constants.SCREEN_TYPE_POPUP_SUKSES,
                    Constants.SCREEN_TYPE_POPUP_LOGOUT -> {
                        // Show popup
                    }
                    else -> {
                        // Pass menu
                    }
                }

                val menuContainer = findViewById<RecyclerView>(R.id.menu_container)
                val x = (resources.displayMetrics.density * 8).toInt()
                menuContainer.addItemDecoration(SpacingItemDecorator(x))
                menuList.clear()
                val menuAdapter = RecyclerViewMenuAdapter(menuList, this@MenuActivity)
                menuContainer.adapter = menuAdapter

                for (i in 0 until screen.comp.size) {
                    val comp = screen.comp[i]
                    menuList.add(
                        MenuItem(
                            comp.icon,
                            comp.label,
                            comp.label,
                            comp.desc,
                            comp.action
                        )
                    )
                }
                menuAdapter.notifyItemInserted(0)
            }
        }

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backToExit) {
                    finish()
                } else {
                    if (menuId == Constants.DEFAULT_ROOT_ID) {
                        backToExit = true
                        Handler(mainLooper).postDelayed({
                            backToExit = false
                        }, 2000)
                        Toast.makeText(
                            this@MenuActivity,
                            getString(R.string.back_to_exit_info),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        finish()
                        startActivity(
                            Intent(this@MenuActivity, MenuActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                    }
                }
            }

        })
    }

    fun onMenuItemClick(position: Int) {
        finish()
        startActivity(
            Intent(this, MenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(Constants.KEY_MENU_ID, menuList[position].value)
        )
    }

    fun onShowPopupotpClick(view: View) {
        // Membuat builder untuk AlertDialog
        val builder = AlertDialog.Builder(this)

        // Meng-inflate layout untuk pop-up
        val popupView = LayoutInflater.from(this).inflate(R.layout.pop_up_otp, null)

        // Mengatur tampilan untuk AlertDialog
        builder.setView(popupView)

        // Membuat AlertDialog
        val alertDialog = builder.create()

        // Menemukan tombol di dalam layout pop-up dan mengatur onClickListener
        popupView.findViewById<Button>(R.id.verifyButton).setOnClickListener {
            alertDialog.dismiss()
        }

        // Menampilkan AlertDialog
        alertDialog.show()
    }
}
