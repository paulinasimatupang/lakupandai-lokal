package id.co.bankntbsyariah.lakupandai.iface

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import id.co.bankntbsyariah.lakupandai.api.ArrestCaller
import okhttp3.OkHttpClient
import id.co.bankntbsyariah.lakupandai.common.Constants
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException

class ArrestCallerImpl(override val client: OkHttpClient = OkHttpClient()) : ArrestCaller {

    private val TAG = "ArrestCallerImpl"

    override fun fetchVersion(): ResponseBody? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}ver")
            .build()

        return try {
            client.newCall(request).execute().use {
                if (!it.isSuccessful) {
                    Log.e(TAG, "Failed to fetch version: ${it.message}")
                    null
                } else it.body
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching version", e)
            null
        }
    }

    override fun fetchRootMenuId(): ResponseBody? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}menu")
            .build()

        return try {
            client.newCall(request).execute().use {
                if (!it.isSuccessful) {
                    Log.e(TAG, "Failed to fetch root menu ID: ${it.message}")
                    null
                } else it.body
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching root menu ID", e)
            null
        }
    }

    override fun fetchScreen(id: String): String? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}screen?id=$id")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch screen for ID $id: ${response.message}")
                    null
                } else {
                    val responseBody = response.body?.string()
                    responseBody?.let { body ->
                        Log.i(TAG, "Screen Response Body: $body") // Log the full response body
                        val jsonObject = JSONObject(body)
                        val actionUrl = jsonObject.optJSONObject("screen")?.optString("action_url")
                        Log.i(TAG, "Action URL for screen ID $id: $actionUrl")
                    }
                    return responseBody
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching screen for ID $id", e)
            null
        }
    }


    override fun fetchImage(id: String): Bitmap? {
        Log.i(TAG, "Fetching image with ID: $id")
        val request = Request.Builder()
//            .url("http://16.78.84.90:8081/ARRest/static/$id")
            .url("https://lakupandai.bankntbsyariah.co.id/static/$id")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch: ${response.message} (Code: ${response.code})")
                    null
                } else {
                    Log.e(TAG, "Success to fetch: ${response.message} (Code: ${response.code})")
                    response.body?.byteStream()?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.also {
                            Log.i(TAG, "Image fetched and decoded successfully")
                        } ?: run {
                            Log.e(TAG, "Failed to decode bitmap")
                            null
                        }
                    } ?: run {
                        Log.e(TAG, "InputStream is null")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while fetching image", e)
            null
        }
    }

    override fun requestPost(msg: JSONObject, callback: (String?) -> Unit) {
        val mediaType = "text/plain".toMediaTypeOrNull()

        val requestBody = RequestBody.create(mediaType, msg.toString())
        val request = Request.Builder()
//            .url("http://16.78.84.90:8080/ARRest/api/")
            .url("https://lakupandai.bankntbsyariah.co.id/ARRest/api/")
            .addHeader("Content-Type", "text/plain")
            .post(requestBody)
            .build()

        Log.d(TAG, "Posting request with body: $msg")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to post message", e)
                callback(null)
                Log.d(TAG, "Callback null telah dipanggil")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response message: ${response.message}")
                Log.d(TAG, "Response body: $responseBody")
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to post message: ${response.message}")
                    callback(null)
                } else {
                    Log.i(TAG, "Message posted successfully")
                    callback(responseBody)
                }
            }
        })
    }

}