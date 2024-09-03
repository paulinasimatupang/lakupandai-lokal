package id.co.bankntbsyariah.lakupandai.iface

import android.util.Log
import id.co.bankntbsyariah.lakupandai.api.WebCaller
import okhttp3.OkHttpClient
import okhttp3.Request
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
        val request = Request.Builder()
            .url("http://reportntbs.selada.id/api/history/$terminalId/$messageId")
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
            Log.e(TAG, "Exception occurred while fetching history detail", e)
            null
        }
    }
}
