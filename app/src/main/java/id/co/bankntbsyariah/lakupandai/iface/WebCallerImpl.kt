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
}
