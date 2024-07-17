package id.co.bankntbsyariah.lakupandai.api
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import id.co.bankntbsyariah.lakupandai.common.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

interface ArrestCaller {
    val client: OkHttpClient

    fun fetchVersion() : ResponseBody? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}ver")
            .build()

        client.newCall(request).execute().use {
            if (!it.isSuccessful) return null
            else return it.body
        }
    }

    fun fetchRootMenuId() : ResponseBody? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}menu")
            .build()

        client.newCall(request).execute().use {
            if (!it.isSuccessful) return null
            else return it.body
        }
    }

    fun fetchScreen(id: String) : String? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}screen?id=$id")
            .build()

        client.newCall(request).execute().use {
            if (!it.isSuccessful) return null
            else return it.body?.string()
        }
    }

    fun fetchImage(id: String) : Bitmap? {
        val request = Request.Builder()
            .url("${Constants.BASE_API}static/$id")
            .build()

        client.newCall(request).execute().use {
            if (!it.isSuccessful) return null
            else return BitmapFactory.decodeStream(it.body?.byteStream())
        }
    }

    fun requestPost() {

    }
}