package id.co.bankntbsyariah.lakupandai.iface

import android.annotation.SuppressLint
import android.util.Log
import id.co.bankntbsyariah.lakupandai.api.WebCaller
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

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

    override fun forgotPassword(username: String, newPassword: String): ResponseBody? {
        // Build the request body
        val formBody = FormBody.Builder()
            .add("username", username)
            .add("new_password", newPassword)
            .build()

        // Create the request
        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/reset/password") // Ensure this is the correct URL
            .post(formBody)
            .build()

        Log.d(TAG, "Username FORGOT: $username")
        Log.d(TAG, "New Password FORGOT: $newPassword")

        return try {
            // Execute the request
            client.newCall(request).execute().let { response ->
                // Log the response details
                Log.d(TAG, "Response Code FORGOT: ${response.code}")
                Log.d(TAG, "Response Message FORGOT: ${response.message}")

                if (response.isSuccessful) {
                    // Log success and return the response body
                    Log.d(TAG, "Password reset successful for user: $username")
                    response.body
                } else {
                    // Log error if the response is not successful
                    Log.e(TAG, "Failed to forgot password: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            // Log exception details
            Log.e(TAG, "Exception occurred while requesting forgot password", e)
            null
        }
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
}
