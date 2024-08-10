package id.co.bankntbsyariah.lakupandai.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.content.Context

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    // Set client with extended timeout
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_form_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d(TAG, "Login button clicked with username: $username")
            loginUser(username, password)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun loginUser(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build()

            val request = Request.Builder()
                .url("http://api.selada.id/api/auth/login") // menggunakan HTTP
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
                    val merchantData = jsonResponse.optJSONObject("data")?.optJSONObject("merchant")

                    if (token.isNotEmpty() && merchantData != null) {
                        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        // Menyimpan data pengguna
                        editor.putString("username", username)
                        editor.putString("token", token)

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
                        // Lanjutkan menyimpan field lainnya yang diperlukan

                        editor.apply()

                        val savedUsername = sharedPreferences.getString("username", "defaultUsername")
                        val savedMerchantName = sharedPreferences.getString("merchant_name", "defaultMerchant")
                        val savedNorekening = sharedPreferences.getString("norekening", "defaultNorekening")

                        Log.d(TAG, "Username yang disimpan: $savedUsername")
                        Log.d(TAG, "Merchant Name yang disimpan: $savedMerchantName")
                        Log.d(TAG, "Norekening yang disimpan: $savedNorekening")

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                            navigateToMenuActivity()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Token atau data merchant tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Username atau password salah", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception occurred while logging in", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToMenuActivity() {
        var id = intent.extras?.getString(Constants.KEY_FORM_ID) ?: Constants.DEFAULT_ROOT_ID
        val intent = Intent(this, MenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, id)
        }
        startActivity(intent)
        finish()
    }
}
