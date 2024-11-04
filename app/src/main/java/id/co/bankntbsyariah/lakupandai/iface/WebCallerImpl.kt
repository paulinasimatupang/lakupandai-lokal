package id.co.bankntbsyariah.lakupandai.iface

import android.annotation.SuppressLint
import android.util.Log
import id.co.bankntbsyariah.lakupandai.api.WebCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class WebCallerImpl(override val client: OkHttpClient = OkHttpClient()) : WebCaller {

    private val TAG = "WebCallerImpl"

    override fun fetchNasabahList(kode_agen: String, token: String): ResponseBody? {
        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/nasabah/list/$kode_agen")
            .addHeader("Authorization", "Bearer $token")
            .get() // Menggunakan GET tanpa request body
            .build()

        Log.d("Token", "Token : $token")
        Log.d("Kode Agen", "Kode Agen : $kode_agen")

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch nasabah list: ${response.message}")
                    null
                } else {
                    response.body // Return the response body without using it here
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching nasabah list", e)
            null
        }
    }

    override fun fetchHistory(terminalId: String, token: String): ResponseBody? {
        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/history?terminal_id=$terminalId")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch history list: ${response.message}")
                    null
                } else {
                    response.body
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching history list", e)
            null
        }
    }
    override fun fetchHistoryDetail(terminalId: String, messageId: String, token: String): ResponseBody? {
        val url = "http://reportntbs.selada.id/api/history/detail?terminal_id=$terminalId&message_id=$messageId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch history detail: ${response.message}")
                    null
                } else {
                    response.body
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching history detail", e)
            null
        }
    }

    override fun changePassword(id: String, old_password: String, new_password: String, token: String): ResponseBody? {
        val formBody = FormBody.Builder()
            .add("id", id)
            .add("old_password", old_password)
            .add("new_password", new_password)
            .build()

        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/auth/changePassword")
            .addHeader("Authorization", "Bearer $token")
            .post(formBody)
            .build()

            return try {
                client.newCall(request).execute().let { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to change password: ${response.message}")
                        null
                    } else {
                        response.body
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception occurred while changing password", e)
                null
            }
    }

    override fun changePin(id: String, old_pin: String, new_pin: String, token: String): ResponseBody? {
        // Create form body with the parameters
        val formBody = FormBody.Builder()
            .add("id", id)
            .add("old_pin", old_pin)
            .add("new_pin", new_pin)
            .build()

        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/auth/changePin")
            .addHeader("Authorization", "Bearer $token")
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to change password: ${response.message}")
                    null
                } else {
                    response.body
                }
            }
    } catch (e: Exception) {
        Log.e(TAG, "Exception occurred while changing password", e)
        null
    }
}

    override fun forgotPassword(username: String, newPassword: String, uid: String, callback: (String?) -> Unit) {
        val url = "http://reportntbs.selada.id/api/reset/password?username=$username&new_password=$newPassword&uid=$uid"
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteArray(0))) // Empty body for POST request
            .build()

        Log.d(TAG, "Forgot Password URL: $url")

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Exception occurred while requesting forgot password", e)
                callback("Request failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Password reset successful for user: $username")
                    callback(null) // Pass null to indicate success
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Failed to reset password: ${response.code} - ${response.message}")

                    // Try to extract message from error JSON if available
                    val errorMessage = try {
                        val jsonObject = JSONObject(errorBody)
                        jsonObject.getString("message")
                    } catch (e: Exception) {
                        "Failed to reset password: ${response.message}"
                    }
                    callback(errorMessage)
                }
            }
        })
    }


    override fun getPhoneByUsername(username: String): String? {
        val formBody = FormBody.Builder()
            .add("username", username)
            .build()

        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/get/phone") // Ensure the URL is correct
            .post(formBody)
            .build()

        return try {
            Log.d(TAG, "Sending request to get phone by username: $username")

            client.newCall(request).execute().let { response ->
                val responseBody = response.body
                val responseString = responseBody?.string() // Convert body to string for easier handling

                Log.d(TAG, "Response for username $username: $responseString")

                responseString // Return the string instead of ResponseBody
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while requesting phone by username: $username", e)
            null
        }
    }

    override fun blockAgen(id: String, token: String): ResponseBody? {
        // Create form body with the parameters
        val formBody = FormBody.Builder()
            .add("id", id)
            .build()

        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/agen/block")
            .addHeader("Authorization", "Bearer $token")
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to change password: ${response.message}")
                    null
                } else {
                    response.body
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while changing password", e)
            null
        }
    }

    override fun blockAgenLogin(username: String): ResponseBody? {
        // Create form body with the parameters
        val formBody = FormBody.Builder()
            .add("username", username)
            .build()

        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/agen/block/login")
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to change password: ${response.message}")
                    null
                } else {
                    response.body
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while changing password", e)
            null
        }
    }

    override fun historyPengaduan(mid: String, token: String): ResponseBody? {
        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/pengaduan/history?mid=$mid")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch history list: ${response.message}")
                    null
                } else {
                    response.body
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching history list", e)
            null
        }
    }

    override fun getParam2(serviceId: String, token: String): JSONObject? {
        val url = "http://reportntbs.selada.id/api/service/param2/$serviceId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        Log.d("GETPARAM2", "Token: $token")
        Log.d("GETPARAM2", "Service ID: $serviceId")

        return try {
            client.newCall(request).execute().let { response ->
                if (!response.isSuccessful) {
                    Log.e("GETPARAM2", "Failed to fetch param2: ${response.message}, Code: ${response.code}")
                    null
                } else {
                    val responseBody = response.body?.string() // Ambil body sebagai string
                    Log.d("GETPARAM2", "Raw response body: $responseBody") // Tambahkan log untuk melihat raw response

                    // Cek apakah respons body adalah JSON yang valid
                    return try {
                        JSONObject(responseBody) // Parse JSON
                    } catch (jsonException: JSONException) {
                        Log.e("GETPARAM2", "Response is not a valid JSON: ${jsonException.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GETPARAM2", "Exception occurred while fetching param2", e)
            null
        }
    }

    override fun forgotPin(mid: String, token: String, callback: (Boolean, String?) -> Unit) {
        val formBody = FormBody.Builder()
            .add("mid", mid)
            .build()

        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/auth/forgot_pin")
            .addHeader("Authorization", "Bearer $token")
            .post(formBody)
            .build()

        // Pemanggilan API tanpa lifecycleScope
        Thread {
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "")
                    val successMessage = jsonResponse.optString("success")

                    if (successMessage.isNotEmpty()) {
                        callback(true, successMessage)
                    } else {
                        callback(false, "Terjadi kesalahan saat memproses respons.")
                    }
                } else {
                    val errorBody = response.body?.string()
                    val jsonResponse = JSONObject(errorBody ?: "")
                    val errorMessage = jsonResponse.optString("error", "Merchant tidak ditemukan.")
                    callback(false, errorMessage)
                }
            } catch (e: Exception) {
                Log.e("WebCallerImpl", "Exception occurred while changing password", e)
                callback(false, "Terjadi kesalahan dalam memproses permintaan.")
            }
        }.start() // Menjalankan dalam thread terpisah untuk menghindari blocking UI thread
    }

}
