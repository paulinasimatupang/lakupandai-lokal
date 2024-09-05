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

    override fun fetchNasabahList(branchId: String, token: String): ResponseBody? {
        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/nasabah/list/$branchId")
            .addHeader("Authorization", "Bearer $token")
            .get() // Menggunakan GET tanpa request body
            .build()

        Log.d("Token", "Token : $token")
        Log.d("Branchid", "Branchid : $branchId")

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
}
